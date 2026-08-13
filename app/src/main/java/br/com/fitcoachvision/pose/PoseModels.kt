package br.com.fitcoachvision.pose

/**
 * Modelos de pose — Kotlin puro, sem dependencia de Android nem de MediaPipe.
 *
 * Este arquivo e a fronteira da camada de analise. Tudo acima dele (vision/)
 * conhece MediaPipe e Android; tudo abaixo (analise de exercicio, geometria,
 * maquina de estados) trabalha apenas com estes tipos.
 *
 * Consequencia pratica: os analisadores de exercicio serao testaveis em JUnit
 * comum, sem emulador, alimentados por sequencias de PoseFrame gravadas em JSON.
 */

/**
 * Um ponto do corpo.
 *
 * Em [PoseFrame.landmarks] as coordenadas sao normalizadas (0..1) em relacao a
 * imagem analisada, e servem para desenhar na tela.
 *
 * Em [PoseFrame.worldLandmarks] as coordenadas sao metricas (metros), com origem
 * no centro do quadril, e servem para calcular angulos — sao muito menos
 * sensiveis a perspectiva.
 */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
)

/**
 * Resultado de um frame de deteccao.
 *
 * @param imageWidth  largura da imagem que foi analisada (ja rotacionada)
 * @param imageHeight altura da imagem que foi analisada (ja rotacionada)
 * @param inferenceMs tempo entre o envio do frame e a chegada do resultado
 */
data class PoseFrame(
    val landmarks: List<Landmark>,
    val worldLandmarks: List<Landmark>,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
    val inferenceMs: Long
) {
    val hasPose: Boolean get() = landmarks.size == PoseLandmarkIndex.COUNT

    fun visibilityOf(index: Int): Float = landmarks.getOrNull(index)?.visibility ?: 0f

    /** Media de visibilidade dos pontos que importam para exercicios de corpo inteiro. */
    val bodyConfidence: Float
        get() {
            if (!hasPose) return 0f
            val essential = PoseLandmarkIndex.FULL_BODY_ESSENTIAL
            var sum = 0f
            for (i in essential) sum += visibilityOf(i)
            return sum / essential.size
        }

    companion object {
        val EMPTY = PoseFrame(emptyList(), emptyList(), 0, 0, 0L, 0L)
    }
}

/**
 * Indices dos 33 pontos entregues pelo MediaPipe Pose Landmarker.
 *
 * ATENCAO — limite do modelo: nao existe nenhum ponto de coluna, escapula ou
 * pescoco. Qualquer avaliacao que dependesse desses pontos e impossivel e nao
 * deve ser aproximada nem nomeada como tal.
 */
object PoseLandmarkIndex {
    const val COUNT = 33

    const val NOSE = 0
    const val LEFT_EYE_INNER = 1
    const val LEFT_EYE = 2
    const val LEFT_EYE_OUTER = 3
    const val RIGHT_EYE_INNER = 4
    const val RIGHT_EYE = 5
    const val RIGHT_EYE_OUTER = 6
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val MOUTH_LEFT = 9
    const val MOUTH_RIGHT = 10
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_PINKY = 17
    const val RIGHT_PINKY = 18
    const val LEFT_INDEX = 19
    const val RIGHT_INDEX = 20
    const val LEFT_THUMB = 21
    const val RIGHT_THUMB = 22
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32

    /** Pontos exigidos por exercicios de corpo inteiro (agachamento, afundo...). */
    val FULL_BODY_ESSENTIAL = intArrayOf(
        LEFT_SHOULDER, RIGHT_SHOULDER,
        LEFT_HIP, RIGHT_HIP,
        LEFT_KNEE, RIGHT_KNEE,
        LEFT_ANKLE, RIGHT_ANKLE
    )
}

/** Ligacoes desenhadas no overlay de esqueleto. */
object PoseConnections {
    private val I = PoseLandmarkIndex

    val TORSO: List<Pair<Int, Int>> = listOf(
        I.LEFT_SHOULDER to I.RIGHT_SHOULDER,
        I.LEFT_SHOULDER to I.LEFT_HIP,
        I.RIGHT_SHOULDER to I.RIGHT_HIP,
        I.LEFT_HIP to I.RIGHT_HIP
    )

    val ARMS: List<Pair<Int, Int>> = listOf(
        I.LEFT_SHOULDER to I.LEFT_ELBOW,
        I.LEFT_ELBOW to I.LEFT_WRIST,
        I.RIGHT_SHOULDER to I.RIGHT_ELBOW,
        I.RIGHT_ELBOW to I.RIGHT_WRIST
    )

    val LEGS: List<Pair<Int, Int>> = listOf(
        I.LEFT_HIP to I.LEFT_KNEE,
        I.LEFT_KNEE to I.LEFT_ANKLE,
        I.LEFT_ANKLE to I.LEFT_HEEL,
        I.LEFT_HEEL to I.LEFT_FOOT_INDEX,
        I.RIGHT_HIP to I.RIGHT_KNEE,
        I.RIGHT_KNEE to I.RIGHT_ANKLE,
        I.RIGHT_ANKLE to I.RIGHT_HEEL,
        I.RIGHT_HEEL to I.RIGHT_FOOT_INDEX
    )

    val HEAD: List<Pair<Int, Int>> = listOf(
        I.LEFT_EAR to I.LEFT_EYE,
        I.LEFT_EYE to I.NOSE,
        I.NOSE to I.RIGHT_EYE,
        I.RIGHT_EYE to I.RIGHT_EAR
    )
}
