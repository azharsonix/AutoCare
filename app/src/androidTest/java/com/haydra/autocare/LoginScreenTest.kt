package com.haydra.autocare

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.haydra.autocare.ui.auth.AuthViewModel
import com.haydra.autocare.ui.auth.LoginScreen
import com.haydra.autocare.ui.theme.AutoCareTheme
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de interfaz (AUTOCARE.txt, sección 35 "Pruebas de interfaz") de la
 * pantalla de inicio de sesión (HU02): verifica que al enviar el formulario
 * vacío se muestren los errores de validación en pantalla.
 */
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun formularioVacioMuestraErroresDeValidacion() {
        composeRule.setContent {
            AutoCareTheme {
                LoginScreen(
                    viewModel = AuthViewModel(),
                    onLoginSuccess = {},
                    onNavigateToRegister = {},
                    onNavigateToForgotPassword = {}
                )
            }
        }

        composeRule.onNodeWithText("Iniciar sesión").performClick()

        composeRule.onNodeWithText("El correo es obligatorio").assertIsDisplayed()
        composeRule.onNodeWithText("La contraseña es obligatoria").assertIsDisplayed()
    }
}
