package br.com.fitcoachvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.fitcoachvision.ui.AppRoot
import br.com.fitcoachvision.ui.theme.FitCoachTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitCoachTheme {
                AppRoot()
            }
        }
    }
}
