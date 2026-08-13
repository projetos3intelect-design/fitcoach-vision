package br.com.fitcoachvision.ui.session

import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.fitcoachvision.AppPreferences
import br.com.fitcoachvision.ui.theme.Danger
import br.com.fitcoachvision.ui.theme.TextMid

/**
 * Tela de camera com deteccao de pose (Fase 1).
 *
 * Nesta fase a tela apenas prova o pipeline: camera → landmarks → esqueleto na
 * tela, com diagnostico medido. Contagem de repeticoes, analise de agachamento
 * e voz entram nas Fases 2 e 3.
 */
@Composable
fun SessionScreen(
    prefs: AppPreferences,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val controller = remember { SessionController(context, prefs) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Mantem a tela acesa durante a sessao.
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Liga o pipeline enquanto a tela existe.
    DisposableEffect(activity) {
        if (activity != null) controller.start(activity, previewView)
        onDispose { controller.stop() }
    }

    // Troca de camera ou de modelo reconstroi o pipeline.
    LaunchedEffect(controller.restartRequested) {
        if (controller.restartRequested && activity != null) {
            controller.consumeRestart()
            controller.start(activity, previewView)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        PoseOverlay(
            frame = controller.poseFrame,
            mirrored = controller.useFrontCamera,
            modifier = Modifier.fillMaxSize()
        )

        // ------------------------------------------------------------ topo
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            if (controller.showDiagnostics) {
                DiagnosticHud(
                    frame = controller.poseFrame,
                    fps = controller.fps,
                    inferenceMs = controller.inferenceMs,
                    delegateLabel = controller.delegateLabel,
                    modelLabel = controller.modelLabel,
                    analysisResolution = controller.analysisResolution
                )
            }
        }

        // ----------------------------------------------------------- centro
        val error = controller.errorMessage
        if (error != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Não foi possível iniciar a análise",
                    style = MaterialTheme.typography.titleLarge,
                    color = Danger
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else if (!controller.poseFrame.hasPose) {
            Text(
                text = "Posicione-se de forma que o corpo inteiro apareça na tela",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // ---------------------------------------------------------- rodape
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Fase 1 — apenas detecção de pose. Sem contagem e sem voz ainda.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMid,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallControl(
                    text = if (controller.useFrontCamera) "Frontal" else "Traseira",
                    modifier = Modifier.weight(1f),
                    onClick = controller::toggleCamera
                )
                SmallControl(
                    text = if (controller.useFullModel) "full" else "lite",
                    modifier = Modifier.weight(1f),
                    onClick = controller::toggleModel
                )
                SmallControl(
                    text = if (controller.showDiagnostics) "HUD on" else "HUD off",
                    modifier = Modifier.weight(1f),
                    onClick = controller::toggleDiagnostics
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onExit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("VOLTAR", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SmallControl(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.16f),
            contentColor = Color.White
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}
