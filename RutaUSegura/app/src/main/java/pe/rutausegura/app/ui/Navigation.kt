package pe.rutausegura.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppRoot(vm: AppViewModel) {
    BackHandler(enabled = vm.screen in listOf(Screen.Register, Screen.StartTrip, Screen.ContactLink, Screen.Login)) { vm.back() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (vm.screen) {
            Screen.Welcome -> WelcomeScreen(vm)
            Screen.Login -> LoginScreen(vm)
            Screen.Register -> RegisterScreen(vm)
            Screen.StudentHome -> StudentHomeScreen(vm)
            Screen.StartTrip -> StartTripScreen(vm)
            Screen.ActiveTrip -> ActiveTripScreen(vm)
            Screen.ContactLink -> ContactLinkScreen(vm)
            Screen.ContactDash -> ContactDashScreen(vm)
        }
    }
}
