package br.com.fitcoachvision.ui.session

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import br.com.fitcoachvision.AppPreferences
import br.com.fitcoachvision.pose.PoseFrame
import br.com.fitcoachvision.vision.ComputeDelegate
import br.com.fitcoachvision.vision.PipelineStats
import br.com.fitcoachvision.vision.PoseImageAnalyzer
import br.com.fitcoachvision.vision.PoseLandmarkerSource
import br.com.fitcoachvision.vision.PoseModel
import br.com.fitcoachvision.vision.VisionConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Dono do ciclo de vida da camera e do detector de pose.
 *
 * Escrito como state holder simples em vez de ViewModel: a tela e travada em
 * retrato e nao ha recriacao por rotacao, entao o ViewModel nao entregaria nada
 * alem de mais uma dependencia no primeiro build. Entra na Fase 4, junto com o
 * Hilt.
 *
 * Os campos de estado sao escritos a partir da thread de callback do MediaPipe.
 * Isso e seguro: o sistema de snapshot do Compose e thread-safe para escrita, e
 * a recomposicao e agendada na thread principal.
 */
class SessionController(
    private val context: Context,
    private val prefs: AppPreferences
) {

    var poseFrame by mutableStateOf(PoseFrame.EMPTY)
        private set

    var fps by mutableStateOf(0f)
        private set

    var inferenceMs by mutableStateOf(0f)
        private set

    var delegateLabel by mutableStateOf("—")
        private set

    var modelLabel by mutableStateOf(PoseModel.LITE.label)
        private set

    var analysisResolution by mutableStateOf("—")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isRunning by mutableStateOf(false)
        private set

    var useFrontCamera by mutableStateOf(prefs.useFrontCamera)
        private set

    var useFullModel by mutableStateOf(prefs.useFullModel)
        private set

    var showDiagnostics by mutableStateOf(prefs.showDiagnostics)
        private set

    private val stats = PipelineStats()
    private var executor: ExecutorService? = null
    private var source: PoseLandmarkerSource? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lastHudUpdateMs = 0L

    // ---------------------------------------------------------------- controles

    fun toggleCamera() {
        useFrontCamera = !useFrontCamera
        prefs.useFrontCamera = useFrontCamera
        restartRequested = true
    }

    fun toggleModel() {
        useFullModel = !useFullModel
        prefs.useFullModel = useFullModel
        restartRequested = true
    }

    fun toggleDiagnostics() {
        showDiagnostics = !showDiagnostics
        prefs.showDiagnostics = showDiagnostics
    }

    /** Sinaliza que a tela precisa reconstruir o pipeline (troca de camera/modelo). */
    var restartRequested by mutableStateOf(false)
        private set

    fun consumeRestart() {
        restartRequested = false
    }

    // ------------------------------------------------------------------ pipeline

    fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        stop()
        errorMessage = null
        stats.reset()

        val config = VisionConfig(
            model = if (useFullModel) PoseModel.FULL else PoseModel.LITE,
            preferredDelegate = ComputeDelegate.GPU
        )
        modelLabel = config.model.label

        val poseSource = PoseLandmarkerSource(
            context = context,
            config = config,
            onResult = ::onPoseResult,
            onError = { message -> errorMessage = message }
        )

        if (!poseSource.setup()) return

        source = poseSource
        delegateLabel = poseSource.activeDelegate.label

        val analysisExecutor = Executors.newSingleThreadExecutor()
        executor = analysisExecutor

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            // previewView.viewPort so existe depois que a View foi medida.
            // Sem ele, preview e analise recebem recortes de campo de visao
            // diferentes e o esqueleto aparece deslocado do corpo.
            previewView.post {
                try {
                    val provider = future.get()
                    cameraProvider = provider
                    bindUseCases(provider, lifecycleOwner, previewView, config, analysisExecutor, poseSource)
                    isRunning = true
                } catch (t: Throwable) {
                    Log.e(TAG, "Falha ao iniciar a camera", t)
                    errorMessage = "Não foi possível abrir a câmera: ${t.message}"
                }
            }
        }, context.mainExecutor)
    }

    private fun bindUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        config: VisionConfig,
        analysisExecutor: ExecutorService,
        poseSource: PoseLandmarkerSource
    ) {
        provider.unbindAll()

        val selector = CameraSelector.Builder()
            .requireLensFacing(
                if (useFrontCamera) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK
            )
            .build()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Resolucao de analise reduzida de proposito. A imagem exibida continua
        // nitida; o que cai e apenas o que vai para a deteccao.
        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    config.analysisSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                )
            )
            .build()

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        analysis.setAnalyzer(analysisExecutor, PoseImageAnalyzer(poseSource, stats))

        // O ViewPort faz preview e analise compartilharem o MESMO recorte de
        // campo de visao. E o que torna valido o mapeamento de coordenadas do
        // overlay: sem ele, a analise pode vir em 4:3 enquanto o preview exibe
        // 16:9, e o esqueleto desenhado nao coincide com o corpo na tela.
        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(analysis)
            .apply { previewView.viewPort?.let { setViewPort(it) } }
            .build()

        provider.bindToLifecycle(lifecycleOwner, selector, group)
    }

    private fun onPoseResult(frame: PoseFrame) {
        poseFrame = frame
        stats.recordResult(frame.timestampMs, frame.inferenceMs)

        if (analysisResolution == "—" && frame.imageWidth > 0) {
            analysisResolution = "${frame.imageWidth}×${frame.imageHeight}"
        }

        // O HUD nao precisa de 15 atualizacoes por segundo.
        val now = SystemClock.uptimeMillis()
        if (now - lastHudUpdateMs >= HUD_INTERVAL_MS) {
            lastHudUpdateMs = now
            fps = stats.fps()
            inferenceMs = stats.averageInferenceMs()
        }
    }

    fun stop() {
        isRunning = false
        try {
            cameraProvider?.unbindAll()
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao desvincular a camera: ${t.message}")
        }
        cameraProvider = null

        // Ordem importa: a thread de analise precisa terminar ANTES de fechar o
        // landmarker. Fechar primeiro deixaria uma chamada nativa em voo apontando
        // para um objeto ja destruido — falha de JNI, que nenhum try/catch pega.
        executor?.let { pool ->
            pool.shutdown()
            try {
                pool.awaitTermination(500, TimeUnit.MILLISECONDS)
            } catch (t: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        executor = null

        source?.close()
        source = null

        poseFrame = PoseFrame.EMPTY
    }

    private companion object {
        const val TAG = "SessionController"
        const val HUD_INTERVAL_MS = 400L
    }
}
