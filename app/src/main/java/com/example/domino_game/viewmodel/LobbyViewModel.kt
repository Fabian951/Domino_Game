package com.example.domino_game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domino_game.model.AuthRepository
import com.example.domino_game.model.Player
import com.example.domino_game.model.Room
import com.example.domino_game.model.RoomRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LobbyUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val userName: String = "Cargando...", // <- Agregado para el Perfil Fijo
    val userAvatar: String = "🎲"        // <- Agregado para el Perfil Fijo
)

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModel(
    private val roomRepo: RoomRepository = RoomRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    val uid: String = authRepo.currentUserUid ?: ""

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    // ID de la sala actual (null = todavía no estoy en ninguna)
    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    // Cuando cambia roomId, se escucha esa sala en tiempo real
    val room: StateFlow<Room?> = _roomId
        .flatMapLatest { id -> if (id == null) flowOf(null) else roomRepo.observeRoom(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val players: StateFlow<List<Player>> = _roomId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else roomRepo.observePlayers(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bloque de inicialización para cargar el nombre y emoji fijo asíncronamente desde Firestore
    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val name = authRepo.getUserName()
                val avatar = authRepo.getUserAvatar()
                _uiState.update {
                    it.copy(userName = name, userAvatar = avatar)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userName = "Jugador", userAvatar = "😎")
                }
            }
        }
    }

    fun createRoom() = launchAction {
        val name = authRepo.getUserName()
        _roomId.value = roomRepo.createRoom(uid, name)
    }

    fun addBot() = launchAction {
        _roomId.value?.let { roomRepo.addBot(it) }
    }

    fun startSoloGame() = launchAction {
        val name = authRepo.getUserName()
        val newRoomId = roomRepo.createRoom(uid, name)
        _roomId.value = newRoomId
        roomRepo.addBot(newRoomId)
        roomRepo.startGame(newRoomId)
    }

    fun joinRoom(code: String) {
        if (code.isBlank()) {
            _uiState.update { it.copy(error = "Escribe el código de la sala") }
            return
        }
        launchAction {
            val name = authRepo.getUserName()
            _roomId.value = roomRepo.joinRoom(code, uid, name)
        }
    }

    fun startGame() = launchAction {
        _roomId.value?.let { roomRepo.startGame(it) }
    }

    fun leaveRoom() {
        val id = _roomId.value ?: return
        _roomId.value = null
        viewModelScope.launch {
            try { roomRepo.leaveRoom(id, uid) } catch (_: Exception) { }
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                block()
                _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
        }
    }
}
