package com.haydra.autocare

import com.haydra.autocare.data.repository.FakeAuthRepository
import com.haydra.autocare.ui.auth.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de integración ViewModel + Repository (AUTOCARE.txt, sección 35:
 * "Login → ViewModel → Repository") para el flujo de registro e inicio de
 * sesión (HU01, HU02), usando FakeAuthRepository como sustituto de Supabase.
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
        viewModel.register()
        advanceUntilIdle()

        val usuario = viewModel.currentUser.value
        assertNotNull(usuario)
        assertEquals("pablo@autocare.com", usuario?.correo)
    }

    @Test
    fun `registro con campos invalidos no crea sesion y marca errores`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onRegisterNombreChange("")
        viewModel.onRegisterCorreoChange("no-es-correo")
        viewModel.onRegisterPasswordChange("123")
        viewModel.onRegisterConfirmacionChange("456")
        viewModel.register()

        assertNull(viewModel.currentUser.value)
        val estado = viewModel.registerState.value
        assertNotNull(estado.nombreError)
        assertNotNull(estado.correoError)
        assertNotNull(estado.passwordError)
        assertNotNull(estado.confirmacionError)
    }

    @Test
    fun `login con credenciales correctas inicia sesion`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onRegisterNombreChange("Ana")
        viewModel.onRegisterCorreoChange("ana@autocare.com")
        viewModel.onRegisterPasswordChange("secreta1")
        viewModel.onRegisterConfirmacionChange("secreta1")
        viewModel.register()
        advanceUntilIdle()
        viewModel.logout()
        assertNull(viewModel.currentUser.value)

        viewModel.onLoginCorreoChange("ana@autocare.com")
        viewModel.onLoginPasswordChange("secreta1")
        viewModel.login()
        advanceUntilIdle()

        assertEquals("ana@autocare.com", viewModel.currentUser.value?.correo)
    }

    @Test
    fun `login con contrasena incorrecta muestra error y no inicia sesion`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onRegisterNombreChange("Ana")
        viewModel.onRegisterCorreoChange("ana@autocare.com")
        viewModel.onRegisterPasswordChange("secreta1")
        viewModel.onRegisterConfirmacionChange("secreta1")
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
}
