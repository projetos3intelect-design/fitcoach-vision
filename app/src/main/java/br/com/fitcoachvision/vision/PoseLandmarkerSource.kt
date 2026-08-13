package br.com.fitcoachvision.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import br.com.fitcoachvision.pose.Landmark
import br.com.fitcoachvision.pose.PoseFrame
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.Landmark as MpWorldLandmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark as MpNormalizedLandmark

/**
 * Encapsula o MediaPipe Pose Landmarker.
 *
 * Esta e a UNICA classe do aplicativo que enxerga um Bitmap de camera. Ela
 * recebe imagem e devolve [PoseFrame] — apenas numeros. Nenhuma camada acima
 * tem acesso a pixels, o que torna impossivel vazar um frame por descuido.
 *
 * Modo LIVE_STREAM: o resultado chega por callback numa thread do MediaPipe,
 * nao na thread que enviou o frame.
 */
class PoseLandmarkerSource(
    private val context: Context,
    private val config: VisionConfig,
    private val onResult: (PoseFrame) -> Unit,
    private val onError: (String) -> Unit
) {

    private var landmarker: PoseLandmarker? = null

    /** Delegate realmente em uso — pode diferir do preferido, se houve fallback. */
    @Volatile
    var activeDelegate: ComputeDelegate = config.preferredDelegate
        private set

    /** Momento de envio de cada frame, para medir a latencia real de inferencia. */
    private val pendingTimestamps = java.util.concurrent.ConcurrentHashMap<Long, Long>()

    /**
     * Cria o landmarker tentando GPU e caindo para CPU se o driver nao suportar.
     *
     * Em aparelhos de entrada o driver de GPU e o ponto mais fragil de todo o
     * pipeline: ele frequentemente existe, e aceita a criacao, e falha depois.
     * Por isso o fallback e explicito e o resultado aparece no HUD.
     */
    fun setup(): Boolean {
        close()

        val order = when (config.preferredDelegate) {
            ComputeDelegate.GPU -> listOf(ComputeDelegate.GPU, ComputeDelegate.CPU)
            ComputeDelegate.CPU -> listOf(ComputeDelegate.CPU)
        }

        for (delegate in order) {
            try {
                landmarker = createLandmarker(delegate)
                activeDelegate = delegate
                Log.i(TAG, "PoseLandmarker criado: modelo=${config.model.label} delegate=${delegate.label}")
                return true
            } catch (t: Throwable) {
                Log.w(TAG, "Falha ao criar com ${delegate.label}: ${t.message}")
            }
        }

        onError(
            "Nao foi possivel iniciar a deteccao de pose. " +
                "Verifique se o modelo ${config.model.assetFile} esta na pasta assets."
        )
        return false
    }

    private fun createLandmarker(delegate: ComputeDelegate): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(config.model.assetFile)
            .setDelegate(
                when (delegate) {
                    ComputeDelegate.GPU -> Delegate.GPU
                    ComputeDelegate.CPU -> Delegate.CPU
                }
            )
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(config.minPoseDetectionConfidence)
            .setMinPosePresenceConfidence(config.minPosePresenceConfidence)
            .setMinTrackingConfidence(config.minTrackingConfidence)
            .setOutputSegmentationMasks(false)
            .setResultListener { result: PoseLandmarkerResult, input: MPImage ->
                handleResult(result, input.width, input.height)
            }
            .setErrorListener { e: RuntimeException ->
                Log.e(TAG, "Erro do MediaPipe", e)
                onError(e.message ?: "erro desconhecido na deteccao")
            }
            .build()

        return PoseLandmarker.createFromOptions(context, options)
    }

    /**
     * Envia um frame para analise. Nao bloqueia: o resultado chega por callback.
     *
     * O bitmap ja deve chegar rotacionado para a orientacao correta.
     */
    fun detect(bitmap: Bitmap, timestampMs: Long) {
        val current = landmarker ?: return
        pendingTimestamps[timestampMs] = System.currentTimeMillis()
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            current.detectAsync(mpImage, timestampMs)
        } catch (t: Throwable) {
            pendingTimestamps.remove(timestampMs)
            Log.w(TAG, "detectAsync falhou: ${t.message}")
        }
    }

    private fun handleResult(result: PoseLandmarkerResult, imageWidth: Int, imageHeight: Int) {
        val timestampMs = result.timestampMs()
        val sentAt = pendingTimestamps.remove(timestampMs)
        val inferenceMs = if (sentAt != null) System.currentTimeMillis() - sentAt else 0L

        // Limpeza defensiva: frames sem callback nao podem acumular indefinidamente.
        if (pendingTimestamps.size > 60) pendingTimestamps.clear()

        val normalized: List<Landmark> = result.landmarks()
            .firstOrNull()
            ?.map { it.toLandmark() }
            ?: emptyList()

        val world: List<Landmark> = result.worldLandmarks()
            .firstOrNull()
            ?.map { it.toLandmark() }
            ?: emptyList()

        onResult(
            PoseFrame(
                landmarks = normalized,
                worldLandmarks = world,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                timestampMs = timestampMs,
                inferenceMs = inferenceMs
            )
        )
    }

    fun close() {
        try {
            landmarker?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao fechar o landmarker: ${t.message}")
        }
        landmarker = null
        pendingTimestamps.clear()
    }

    private companion object {
        const val TAG = "PoseLandmarkerSource"

        /**
         * visibility() e presence() sao Optional<Float> na API Java do MediaPipe
         * e podem vir vazios. Tratar como float sempre presente quebra em runtime.
         * A conversao acontece aqui, uma unica vez, na fronteira da camada de visao.
         */
        fun MpNormalizedLandmark.toLandmark() = Landmark(
            x = x(),
            y = y(),
            z = z(),
            visibility = visibility().orElse(0f)
        )

        fun MpWorldLandmark.toLandmark() = Landmark(
            x = x(),
            y = y(),
            z = z(),
            visibility = visibility().orElse(0f)
        )
    }
}
