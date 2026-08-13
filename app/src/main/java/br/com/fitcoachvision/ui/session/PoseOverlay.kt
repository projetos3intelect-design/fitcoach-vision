package br.com.fitcoachvision.ui.session

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import br.com.fitcoachvision.pose.PoseConnections
import br.com.fitcoachvision.pose.PoseFrame
import br.com.fitcoachvision.pose.PoseLandmarkIndex
import br.com.fitcoachvision.ui.theme.Accent
import br.com.fitcoachvision.ui.theme.Info
import br.com.fitcoachvision.ui.theme.Warn

/**
 * Desenha o esqueleto sobre a imagem da camera.
 *
 * Mapeamento de coordenadas: os landmarks vem normalizados (0..1) em relacao a
 * imagem ANALISADA, que tem resolucao e proporcao diferentes da area exibida.
 * O PreviewView usa FILL_CENTER, ou seja, escala pelo maior fator e corta as
 * sobras. Este overlay repete exatamente essa conta, senao o esqueleto aparece
 * deslocado em relacao ao corpo.
 *
 * O espelhamento da camera frontal acontece SOMENTE aqui. A imagem enviada para
 * a deteccao nao e espelhada, para que esquerda e direita anatomicas continuem
 * significando a mesma coisa nas fases seguintes.
 */
@Composable
fun PoseOverlay(
    frame: PoseFrame,
    mirrored: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (!frame.hasPose || frame.imageWidth <= 0 || frame.imageHeight <= 0) return@Canvas

        val scale = maxOf(
            size.width / frame.imageWidth,
            size.height / frame.imageHeight
        )
        val offsetX = (size.width - frame.imageWidth * scale) / 2f
        val offsetY = (size.height - frame.imageHeight * scale) / 2f

        fun project(index: Int): Offset? {
            val lm = frame.landmarks.getOrNull(index) ?: return null
            if (lm.visibility < MIN_VISIBILITY) return null
            val px = lm.x * frame.imageWidth * scale + offsetX
            val py = lm.y * frame.imageHeight * scale + offsetY
            return Offset(if (mirrored) size.width - px else px, py)
        }

        fun drawBones(bones: List<Pair<Int, Int>>, color: Color, width: Float) {
            for ((a, b) in bones) {
                val pa = project(a) ?: continue
                val pb = project(b) ?: continue
                drawLine(
                    color = color,
                    start = pa,
                    end = pb,
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }

        val boneWidth = (size.minDimension * 0.011f).coerceIn(4f, 12f)
        val jointRadius = boneWidth * 0.75f

        drawBones(PoseConnections.HEAD, Warn.copy(alpha = 0.65f), boneWidth * 0.55f)
        drawBones(PoseConnections.TORSO, Accent, boneWidth)
        drawBones(PoseConnections.ARMS, Info, boneWidth * 0.85f)
        drawBones(PoseConnections.LEGS, Info, boneWidth * 0.85f)

        // Articulacoes que importam para exercicios de corpo inteiro
        for (index in PoseLandmarkIndex.FULL_BODY_ESSENTIAL) {
            val point = project(index) ?: continue
            drawCircle(color = Accent, radius = jointRadius, center = point)
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = jointRadius,
                center = point,
                style = Stroke(width = boneWidth * 0.18f)
            )
        }
    }
}

private const val MIN_VISIBILITY = 0.4f
