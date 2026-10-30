package com.example.domino_game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domino_game.model.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
    val needsAvatarSelection: Boolean = false
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(loggedIn = repo.currentUserUid != null)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(name: String, email: String, password: String, avatarEmoji: String) {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            _uiState.value = _uiState.value.copy(
                error = "Completa todos los campos (contraseña mínimo 6 caracteres)"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            try {
                repo.register(name.trim(), email.trim(), password, avatarEmoji)
                // MODIFICADO: Tras registro tradicional exitoso, va directo al Lobby sin pasar por la pantalla extra
                _uiState.value = AuthUiState(loggedIn = true, needsAvatarSelection = false)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message)
            }
        }
    }


    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa correo y contraseña")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            try {
                repo.login(email.trim(), password)
                _uiState.value = AuthUiState(loggedIn = true, needsAvatarSelection = false)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message)
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            try {
                repo.loginWithGoogle(idToken)
                // Exclusivo Google: activa la redirección a la pantalla de avatares
                _uiState.value = AuthUiState(loggedIn = true, needsAvatarSelection = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message)
            }
        }
    }


    fun updateAvatarEmoji(emoji: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                // Concluimos el flujo intermedio quitando la bandera para dar acceso al Lobby
                _uiState.value = _uiState.value.copy(loading = false, needsAvatarSelection = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun logout() {
        repo.logout()
        _uiState.value = AuthUiState()
    }
}
