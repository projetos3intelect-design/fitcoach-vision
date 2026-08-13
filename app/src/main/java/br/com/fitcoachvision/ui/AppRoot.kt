package br.com.fitcoachvision.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import br.com.fitcoachvision.AppPreferences
import br.com.fitcoachvision.ui.home.HomeScreen
import br.com.fitcoachvision.ui.onboarding.OnboardingScreen
import br.com.fitcoachvision.ui.session.SessionScreen

/**
 * Navegacao por estado.
 *
 * Com tres telas, uma variavel de estado faz o mesmo que a Navigation Compose e
 * remove uma dependencia do primeiro build. A troca por um NavHost acontece na
 * Fase 5, quando surgirem as telas de treino, descanso, relatorio e historico.
 */
enum class Screen { ONBOARDING, HOME, SESSION }

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    val hasCameraPermission = {
        context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    var screen by remember {
        mutableStateOf(
            if (prefs.disclaimerAccepted && hasCameraPermission()) Screen.HOME else Screen.ONBOARDING
        )
    }

    when (screen) {
        Screen.ONBOARDING -> OnboardingScreen(
            onReady = {
                prefs.disclaimerAccepted = true
                screen = Screen.HOME
            }
        )

        Screen.HOME -> HomeScreen(
            onStartSession = { screen = Screen.SESSION }
        )

        Screen.SESSION -> SessionScreen(
            prefs = prefs,
            onExit = { screen = Screen.HOME }
        )
    }
}
