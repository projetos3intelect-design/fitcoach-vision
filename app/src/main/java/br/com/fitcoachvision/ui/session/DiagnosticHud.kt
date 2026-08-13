package br.com.fitcoachvision.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.fitcoachvision.pose.PoseFrame
import br.com.fitcoachvision.ui.theme.Accent
import br.com.fitcoachvision.ui.theme.Danger
import br.com.fitcoachvision.ui.theme.TextMid
import br.com.fitcoachvision.ui.theme.Warn
import kotlin.math.roundToInt

/**
 * HUD de diagnostico.
 *
 * Existe porque o APK chega pelo GitHub Releases: nao ha depurador nem logcat
 * no circuito. Este painel e a unica janela para o que o aparelho realmente
 * esta fazendo — e serve para calibrar os padroes do projeto com dado medido,
 * em vez de estimativa.
 */
@Composable
fun DiagnosticHud(
    frame: PoseFrame,
    fps: Float,
    inferenceMs: Float,
    delegateLabel: String,
    modelLabel: String,
    analysisResolution: String,
    modifier: Modifier = Modifier
) {
    val fpsColor = when {
        fps >= 12f -> Accent
        fps >= 8f -> Warn
        else -> Danger
    }

    val confidence = frame.bodyConfidence
    val confidenceColor = when {
        confidence >= 0.7f -> Accent
        confidence >= 0.4f -> Warn
        else -> Danger
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "DIAGNÓSTICO",
            style = MaterialTheme.typography.labelSmall,
            color = TextMid
        )

        HudRow("fps", if (fps > 0f) fps.roundToInt().toString() else "—", fpsColor)
        HudRow(
            "latência",
            if (inferenceMs > 0f) "${inferenceMs.roundToInt()} ms" else "—",
            Color.White
        )
        HudRow(
            "confiança",
            if (frame.hasPose) "${(confidence * 100).roundToInt()}%" else "sem pose",
            confidenceColor
        )
        HudRow("modelo", modelLabel, Color.White)
        HudRow("processamento", delegateLabel, Color.White)
        HudRow("análise", analysisResolution, Color.White)
    }
}

@Composable
private fun HudRow(label: String, value: String, valueColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMid,
            modifier = Modifier.padding(end = 2.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor
        )
    }
}
