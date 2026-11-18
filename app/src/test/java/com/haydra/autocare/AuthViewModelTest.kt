package com.haydra.autocare

import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.repository.FakeAuthRepository
import com.haydra.autocare.ui.auth.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de integración ViewModel + Repository (AUTOCARE.txt, sección 35:
 * "Login → ViewModel → Repository") para el flujo de registro e inicio de
 * sesión (HU01, HU02), el aviso de privacidad y el borrado de cuenta, usando
 * FakeAuthRepository como sustituto de Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `registro exitoso deja al usuario con sesion iniciada`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onRegisterNombreChange("Pablo")
        viewModel.onRegisterCorreoChange("pablo@autocare.com")
        viewModel.onRegisterPasswordChange("123456")
        viewModel.onRegisterConfirmacionChange("123456")
        viewModel.onRegisterAvisoChange(true)
        viewModel.register()
        advanceUntilIdle()

        val usuario = viewModel.currentUser.value
        assertNotNull(usuario)
        assertEquals("pablo@autocare.com", usuario?.correo)
    }

    @Test
    fun `login con contrasena incorrecta muestra error y no inicia sesion`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onRegisterNombreChange("Ana")
        viewModel.onRegisterCorreoChange("ana@autocare.com")
        viewModel.onRegisterPasswordChange("secreta1")
        viewModel.onRegisterConfirmacionChange("secreta1")
        viewModel.onRegisterAvisoChange(true)
        viewModel.register()
        advanceUntilIdle()
        viewModel.logout()

        viewModel.onLoginCorreoChange("ana@autocare.com")
        viewModel.onLoginPasswordChange("incorrecta")
        viewModel.login()
        advanceUntilIdle()

        assertNull(viewModel.currentUser.value)
        assertTrue(viewModel.loginState.value.errorMessage != null)
    }

    private fun TestScope.iniciarSesionDemo(viewModel: AuthViewModel) {
        viewModel.onLoginCorreoChange(DemoData.CORREO)
        viewModel.onLoginPasswordChange(DemoData.PASSWORD)
        viewModel.login()
        advanceUntilIdle()
    }

    @Test
    fun `registro sin aceptar el aviso no crea la cuenta`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onRegisterNombreChange("Daniela Flores")
        viewModel.onRegisterCorreoChange("daniela@correo.com")
        viewModel.onRegisterPasswordChange("Daniela2026")
        viewModel.onRegisterConfirmacionChange("Daniela2026")
        viewModel.register()
        advanceUntilIdle()

        assertNotNull(viewModel.registerState.value.avisoError)
        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun `borrar cuenta cierra la sesion y ya no deja entrar`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository())
        iniciarSesionDemo(viewModel)

        viewModel.onDeleteAccountPasswordChange(DemoData.PASSWORD)
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertNull(viewModel.currentUser.value)
        assertFalse(viewModel.deleteAccountState.value.isDeleting)

        iniciarSesionDemo(viewModel)
        assertNull(viewModel.currentUser.value)
        assertNotNull(viewModel.loginState.value.errorMessage)
    }
}
