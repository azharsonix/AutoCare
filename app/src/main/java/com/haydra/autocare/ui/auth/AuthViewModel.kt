package com.haydra.autocare.ui.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haydra.autocare.data.local.SessionStore
import com.haydra.autocare.data.model.User
import com.haydra.autocare.data.repository.AuthRepository
import com.haydra.autocare.data.repository.FakeAuthRepository
import com.haydra.autocare.data.validation.AuthValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val correoError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class RegisterUiState(
    val nombre: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmacion: String = "",
    val nombreError: String? = null,
    val correoError: String? = null,
    val passwordError: String? = null,
    val confirmacionError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@Immutable
data class ProfileUiState(
    val nombre: String = "",
    val correo: String = "",
    val nombreError: String? = null,
    val correoError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@Immutable
data class ChangePasswordUiState(
    val actual: String = "",
    val nueva: String = "",
    val confirmacion: String = "",
    val actualError: String? = null,
    val nuevaError: String? = null,
    val confirmacionError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@Immutable
data class ForgotPasswordUiState(
    val correo: String = "",
    val correoError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Correo al que se envió el enlace; null mientras no se ha enviado. */
    val enviadoA: String? = null
)

/**
 * ViewModel compartido por las pantallas de Login y Registro (HU01, HU02).
 * Administra el estado de ambos formularios mediante StateFlow, según la
 * arquitectura MVVM + Repository descrita en AUTOCARE.txt (secciones 29-31).
 *
 * Con [sessionStore] mantiene la sesión iniciada entre aperturas de la app:
 * al arrancar intenta restaurarla, y la guarda o borra al iniciar/cerrar sesión.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = FakeAuthRepository(),
    private val sessionStore: SessionStore? = null
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private val _profileState = MutableStateFlow(ProfileUiState())
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _changePasswordState = MutableStateFlow(ChangePasswordUiState())
    val changePasswordState: StateFlow<ChangePasswordUiState> = _changePasswordState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordState: StateFlow<ForgotPasswordUiState> = _forgotPasswordState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(authRepository.currentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _sessionChecked = MutableStateFlow(sessionStore == null)
    val sessionChecked: StateFlow<Boolean> = _sessionChecked.asStateFlow()

    init {
        if (sessionStore != null) {
            viewModelScope.launch {
                val savedUserId = sessionStore.sessionUserId.first()
                if (savedUserId != null) {
                    authRepository.restoreSession(savedUserId)
                        .onSuccess { _currentUser.value = it }
                        .onFailure { sessionStore.clearSession() }
                }
                _sessionChecked.value = true
            }
        }
    }

    fun onLoginCorreoChange(correo: String) {
        _loginState.update { it.copy(correo = correo, correoError = null, errorMessage = null) }
    }

    fun onLoginPasswordChange(password: String) {
        _loginState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun login() {
        val estado = _loginState.value
        val correoError = AuthValidator.validateCorreo(estado.correo)
        val passwordError = AuthValidator.validatePassword(estado.password)
        if (correoError != null || passwordError != null) {
            _loginState.update { it.copy(correoError = correoError, passwordError = passwordError) }
            return
        }

        _loginState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val resultado = authRepository.login(estado.correo, estado.password)
            resultado.onSuccess { usuario ->
                _currentUser.value = usuario
                _loginState.value = LoginUiState()
                sessionStore?.saveSession(usuario.id)
            }.onFailure { error ->
                _loginState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun onRegisterNombreChange(nombre: String) {
        _registerState.update { it.copy(nombre = nombre, nombreError = null, errorMessage = null) }
    }

    fun onRegisterCorreoChange(correo: String) {
        _registerState.update { it.copy(correo = correo, correoError = null, errorMessage = null) }
    }

    fun onRegisterPasswordChange(password: String) {
        _registerState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun onRegisterConfirmacionChange(confirmacion: String) {
        _registerState.update { it.copy(confirmacion = confirmacion, confirmacionError = null, errorMessage = null) }
    }

    fun register() {
        val estado = _registerState.value
        val nombreError = AuthValidator.validateNombre(estado.nombre)
        val correoError = AuthValidator.validateCorreo(estado.correo)
        val passwordError = AuthValidator.validatePassword(estado.password)
        val confirmacionError = AuthValidator.validateConfirmacion(estado.password, estado.confirmacion)
        if (nombreError != null || correoError != null || passwordError != null || confirmacionError != null) {
            _registerState.update {
                it.copy(
                    nombreError = nombreError,
                    correoError = correoError,
                    passwordError = passwordError,
                    confirmacionError = confirmacionError
                )
            }
            return
        }

        _registerState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val resultado = authRepository.register(estado.nombre, estado.correo, estado.password)
            resultado.onSuccess { usuario ->
                _currentUser.value = usuario
                _registerState.value = RegisterUiState()
                sessionStore?.saveSession(usuario.id)
            }.onFailure { error ->
                _registerState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
        viewModelScope.launch { sessionStore?.clearSession() }
    }

    fun startEditingProfile() {
        val usuario = _currentUser.value ?: return
        _profileState.value = ProfileUiState(nombre = usuario.nombre, correo = usuario.correo)
    }

    fun onProfileNombreChange(nombre: String) {
        _profileState.update { it.copy(nombre = nombre, nombreError = null, errorMessage = null, successMessage = null) }
    }

    fun onProfileCorreoChange(correo: String) {
        _profileState.update { it.copy(correo = correo, correoError = null, errorMessage = null, successMessage = null) }
    }

    fun saveProfile() {
        val usuario = _currentUser.value ?: return
        val estado = _profileState.value
        val nombreError = AuthValidator.validateNombre(estado.nombre)
        val correoError = AuthValidator.validateCorreo(estado.correo)
        if (nombreError != null || correoError != null) {
            _profileState.update { it.copy(nombreError = nombreError, correoError = correoError) }
            return
        }

        _profileState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            authRepository.updateProfile(usuario.id, estado.nombre, estado.correo)
                .onSuccess { actualizado ->
                    _currentUser.value = actualizado
                    _profileState.value = ProfileUiState(
                        nombre = actualizado.nombre,
                        correo = actualizado.correo,
                        successMessage = "Perfil actualizado"
                    )
                }
                .onFailure { error -> _profileState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    fun onChangePasswordActualChange(value: String) {
        _changePasswordState.update { it.copy(actual = value, actualError = null, errorMessage = null, successMessage = null) }
    }

    fun onChangePasswordNuevaChange(value: String) {
        _changePasswordState.update { it.copy(nueva = value, nuevaError = null, errorMessage = null, successMessage = null) }
    }

    fun onChangePasswordConfirmacionChange(value: String) {
        _changePasswordState.update { it.copy(confirmacion = value, confirmacionError = null, errorMessage = null, successMessage = null) }
    }

    fun changePassword() {
        val usuario = _currentUser.value ?: return
        val estado = _changePasswordState.value
        val actualError = AuthValidator.validatePassword(estado.actual)
        val nuevaError = AuthValidator.validatePassword(estado.nueva)
        val confirmacionError = AuthValidator.validateConfirmacion(estado.nueva, estado.confirmacion)
        if (actualError != null || nuevaError != null || confirmacionError != null) {
            _changePasswordState.update {
                it.copy(actualError = actualError, nuevaError = nuevaError, confirmacionError = confirmacionError)
            }
            return
        }

        _changePasswordState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            authRepository.changePassword(usuario.id, estado.actual, estado.nueva)
                .onSuccess { _changePasswordState.value = ChangePasswordUiState(successMessage = "Contraseña actualizada") }
                .onFailure { error -> _changePasswordState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    fun onForgotCorreoChange(correo: String) {
        _forgotPasswordState.update { it.copy(correo = correo, correoError = null, errorMessage = null) }
    }

    /** Pide a Firebase que envíe el enlace para restablecer la contraseña. */
    fun sendPasswordReset() {
        val estado = _forgotPasswordState.value
        val correoError = AuthValidator.validateCorreo(estado.correo)
        if (correoError != null) {
            _forgotPasswordState.update { it.copy(correoError = correoError) }
            return
        }

        _forgotPasswordState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(estado.correo)
                .onSuccess { _forgotPasswordState.value = ForgotPasswordUiState(enviadoA = estado.correo.trim()) }
                .onFailure { error -> _forgotPasswordState.update { it.copy(isSaving = false, errorMessage = error.message) } }
        }
    }

    fun clearForgotPassword() {
        _forgotPasswordState.value = ForgotPasswordUiState()
    }
}
