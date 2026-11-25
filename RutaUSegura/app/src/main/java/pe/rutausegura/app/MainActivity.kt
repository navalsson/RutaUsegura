package pe.rutausegura.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import pe.rutausegura.app.ui.AppRoot
import pe.rutausegura.app.ui.AppViewModel
import pe.rutausegura.app.ui.RutaTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RutaTheme { AppRoot(vm) } }
    }
}
