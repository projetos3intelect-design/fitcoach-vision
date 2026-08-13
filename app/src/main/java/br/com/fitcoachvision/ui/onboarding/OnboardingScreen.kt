package br.com.fitcoachvision.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onReady: () -> Unit) {
    val context = LocalContext.current

    var permissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
        if (granted) onReady()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FitCoach Vision",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Assistente de execução de exercícios",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        InfoCard(
            title = "O que este aplicativo é",
            body = "Um assistente que acompanha a execução dos seus exercícios pela câmera: " +
                "conta repetições, mede amplitude e dá retorno sobre o movimento."
        )

        Spacer(Modifier.height(12.dp))

        InfoCard(
            title = "O que este aplicativo não é",
            body = "Não substitui médico, fisioterapeuta ou profissional de educação física. " +
                "Não faz diagnóstico e não avalia lesões. As pontuações são uma medida " +
                "interna de consistência da execução, para você comparar com você mesmo."
        )

        Spacer(Modifier.height(12.dp))

        InfoCard(
            title = "Privacidade",
            body = "Todo o processamento acontece no seu aparelho. Nenhuma imagem é gravada " +
                "ou enviada para servidores. Este aplicativo sequer possui permissão de " +
                "acesso à internet — você pode verificar isso nas informações do app."
        )

        Spacer(Modifier.height(32.dp))

        if (permissionDenied && !permissionGranted) {
            Text(
                text = "Sem acesso à câmera o aplicativo não consegue analisar os exercícios. " +
                    "Você pode liberar em Configurações do sistema › Aplicativos › FitCoach Vision.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (permissionGranted) onReady() else launcher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (permissionGranted) "Continuar" else "Entendi, permitir a câmera",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
