package com.haydra.autocare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.haydra.autocare.navigation.AutoCareNavHost
import com.haydra.autocare.ui.inicio.IntroAnimada
import com.haydra.autocare.ui.theme.AutoCareTheme

class MainActivity : ComponentActivity() {

    /** Vehículo a abrir al tocar la notificación de un recordatorio. */
    private val vehiculoPendiente = mutableStateOf<String?>(null)
    private val pestanaPendiente = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) leerVehiculo(intent)
        setContent {
            AutoCareTheme {
                // La animación del logotipo solo se ve al abrir la app, no al girar la pantalla.
                var mostrarIntro by rememberSaveable { mutableStateOf(true) }
                // La app se compone cuando el logotipo ya está quieto, no mientras se anima.
                var logoCompleto by rememberSaveable { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (logoCompleto || !mostrarIntro) AutoCareNavHost(
                            vehiculoAAbrir = vehiculoPendiente.value,
                            pestanaAAbrir = pestanaPendiente.value,
                            onVehiculoAbierto = {
                                vehiculoPendiente.value = null
                                pestanaPendiente.value = null
                            }
                        )
                    }
                    if (mostrarIntro) {
                        IntroAnimada(onLogoCompleto = { logoCompleto = true }, onTerminar = { mostrarIntro = false })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerVehiculo(intent)
    }

    private fun leerVehiculo(intent: Intent?) {
        intent?.getStringExtra(EXTRA_VEHICULO_ID)?.let {
            pestanaPendiente.value = intent.getStringExtra(EXTRA_PESTANA)
            vehiculoPendiente.value = it
        }
    }

    companion object {
        const val EXTRA_VEHICULO_ID = "vehiculoId"
        const val EXTRA_PESTANA = "pestana"
    }
}
