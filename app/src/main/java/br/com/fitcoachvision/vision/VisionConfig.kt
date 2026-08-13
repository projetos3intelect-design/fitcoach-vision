package br.com.fitcoachvision.vision

import android.util.Size

/**
 * Perfis de visao computacional.
 *
 * O aparelho alvo deste projeto e de entrada, entao o padrao e o modelo LITE
 * com resolucao de analise reduzida. A imagem exibida na tela continua nitida —
 * o que cai e apenas a resolucao do que vai para a deteccao.
 */
enum class PoseModel(val assetFile: String, val label: String) {
    LITE("pose_landmarker_lite.task", "lite"),
    FULL("pose_landmarker_full.task", "full")
}

enum class ComputeDelegate(val label: String) {
    GPU("GPU"),
    CPU("CPU")
}

data class VisionConfig(
    val model: PoseModel = PoseModel.LITE,
    val preferredDelegate: ComputeDelegate = ComputeDelegate.GPU,
    val analysisSize: Size = Size(640, 480),
    val minPoseDetectionConfidence: Float = 0.5f,
    val minPosePresenceConfidence: Float = 0.5f,
    val minTrackingConfidence: Float = 0.5f
) {
    companion object {
        /** Perfil inicial usado na Fase 1. A selecao automatica por fps entra na Fase 2. */
        val DEFAULT = VisionConfig()
    }
}

/**
 * Medidor de desempenho do pipeline.
 *
 * Existe por um motivo especifico: sem depurador nem logs em tempo real (o APK
 * chega pelo GitHub Releases), este medidor e a unica forma de saber o que o
 * aparelho realmente aguenta. Ele alimenta o HUD de diagnostico na tela.
 */
class PipelineStats(private val windowSize: Int = 30) {

    private val frameTimestamps = ArrayDeque<Long>()
    private val inferenceTimes = ArrayDeque<Long>()

    /** Frames enviados para o detector. */
    @Volatile var submittedFrames: Int = 0
        private set

    /** Resultados efetivamente recebidos de volta. */
    @Volatile var resultFrames: Int = 0
        private set

    fun recordSubmitted() {
        submittedFrames++
    }

    @Synchronized
    fun recordResult(timestampMs: Long, inferenceMs: Long) {
        resultFrames++
        frameTimestamps.addLast(timestampMs)
        inferenceTimes.addLast(inferenceMs)
        while (frameTimestamps.size > windowSize) frameTimestamps.removeFirst()
        while (inferenceTimes.size > windowSize) inferenceTimes.removeFirst()
    }

    /** Quadros por segundo medidos na janela recente. */
    @Synchronized
    fun fps(): Float {
        if (frameTimestamps.size < 2) return 0f
        val span = frameTimestamps.last() - frameTimestamps.first()
        if (span <= 0L) return 0f
        return (frameTimestamps.size - 1) * 1000f / span
    }

    /** Latencia media de inferencia em milissegundos. */
    @Synchronized
    fun averageInferenceMs(): Float {
        if (inferenceTimes.isEmpty()) return 0f
        var sum = 0L
        for (t in inferenceTimes) sum += t
        return sum.toFloat() / inferenceTimes.size
    }

    @Synchronized
    fun reset() {
        frameTimestamps.clear()
        inferenceTimes.clear()
        submittedFrames = 0
        resultFrames = 0
    }
}
