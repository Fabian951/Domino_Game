package com.example.domino_game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domino_game.model.AuthRepository
import com.example.domino_game.model.Cell
import com.example.domino_game.model.GameRepository
import com.example.domino_game.model.Move
import com.example.domino_game.model.MoveValidator
import com.example.domino_game.model.Orientation
import com.example.domino_game.model.Player
import com.example.domino_game.model.Room
import com.example.domino_game.model.RoomRepository
import com.example.domino_game.model.ScoreCalculator
import com.example.domino_game.model.Tile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameUiState(
    val selectedTile: Tile? = null,
    val validMovesForSelectedTile: List<Move> = emptyList(),
    val previewMove: Move? = null,
    val validPlacementsAtAnchor: List<Move> = emptyList(),
    val placementIndex: Int = 0,
    val isRelocatingPortal: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(
    private val gameRepo: GameRepository = GameRepository(),
    private val roomRepo: RoomRepository = RoomRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    val currentUid: String = authRepo.currentUserUid ?: ""

    private val _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var botJob: Job? = null
    
    // Conjunto de fichas jugables cacheadas para rendimiento
    private val _playableTiles = MutableStateFlow<Set<Tile>>(emptySet())
    val playableTiles: StateFlow<Set<Tile>> = _playableTiles.asStateFlow()

    // ---------- OBSERVADORES ----------
    val room: StateFlow<Room?> = _roomId
        .flatMapLatest { id -> if (id == null) flowOf(null) else roomRepo.observeRoom(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val players: StateFlow<List<Player>> = _roomId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else roomRepo.observePlayers(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val board: StateFlow<List<Cell>> = _roomId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else gameRepo.observeBoard(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pool: StateFlow<List<Tile>> = _roomId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else gameRepo.observePool(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setRoomId(id: String) {
        _roomId.value = id
        observeBotTurnCombined()
        observePlayableTiles()
    }

    private fun observePlayableTiles() {
        viewModelScope.launch {
            combine(board, players) { b, p ->
                val myHand = p.firstOrNull { it.uid == currentUid }?.hand ?: emptyList()
                val boardMap = b.associateBy { Pair(it.row, it.col) }
                myHand.filter { MoveValidator.getValidMoves(boardMap, it).isNotEmpty() }.toSet()
            }.collect {
                _playableTiles.value = it
            }
        }
    }

    // ---------- ACCIONES DEL JUGADOR ----------
    fun selectTile(tile: Tile) {
        if (_uiState.value.selectedTile == tile) {
            _uiState.value = _uiState.value.copy(selectedTile = null, validMovesForSelectedTile = emptyList(), previewMove = null)
        } else {
            val boardMap = board.value.associateBy { Pair(it.row, it.col) }
            val validMoves = MoveValidator.getValidMoves(boardMap, tile)
            
            // Si está reubicando un portal, simular tablero liberado temporalmente
            val movesToUse = if (_uiState.value.isRelocatingPortal) {
                // Simplificación UI: solo mostramos todos los movimientos regulares, el repo hará la validación estricta
                validMoves 
            } else validMoves
            
            val errMessage = if (movesToUse.isEmpty()) "Esta ficha no tiene movimientos válidos" else null
            _uiState.value = _uiState.value.copy(
                selectedTile = tile,
                validMovesForSelectedTile = movesToUse,
                previewMove = null,
                error = errMessage
            )
        }
    }

    fun rotate() {
        cyclePlacement { current, candidate -> current.orientation != candidate.orientation }
    }

    fun flip() {
        cyclePlacement { current, candidate -> current.orientation == candidate.orientation && current.flipped != candidate.flipped }
    }

    private fun cyclePlacement(condition: (Move, Move) -> Boolean) {
        val placements = _uiState.value.validPlacementsAtAnchor
        val currentPreview = _uiState.value.previewMove ?: return
        
        val nextIdx = placements.indexOfFirst { condition(currentPreview, it) }
        if (nextIdx != -1) {
            _uiState.value = _uiState.value.copy(
                placementIndex = nextIdx,
                previewMove = placements[nextIdx]
            )
        }
    }

    fun onCellTapped(row: Int, col: Int) {
        val tile = _uiState.value.selectedTile
        if (tile == null) {
            _uiState.value = _uiState.value.copy(error = "Selecciona una ficha de tu mano primero")
            return
        }

        val boardMap = board.value.associateBy { Pair(it.row, it.col) }
        val placements = MoveValidator.getValidPlacementsAt(boardMap, tile, row, col)

        if (placements.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No puedes colocar la ficha en esta casilla")
            return
        }

        val currentPreview = _uiState.value.previewMove
        if (currentPreview != null && ((currentPreview.row == row && currentPreview.col == col) || (currentPreview.row2 == row && currentPreview.col2 == col))) {
            confirmCurrentMove()
        } else {
            val firstMove = placements.first()
            _uiState.value = _uiState.value.copy(
                previewMove = firstMove,
                validPlacementsAtAnchor = placements,
                placementIndex = 0,
                error = null
            )
        }
    }

    fun confirmCurrentMove() {
        val preview = _uiState.value.previewMove ?: return
        val currentRoomId = _roomId.value ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                if (_uiState.value.isRelocatingPortal) {
                    gameRepo.resolvePortal(currentRoomId, currentUid, preview)
                } else {
                    gameRepo.playMove(currentRoomId, currentUid, preview)
                }
                _uiState.value = GameUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message ?: "Movimiento inválido")
            }
        }
    }

    fun autoMove() {
        val boardMap = board.value.associateBy { Pair(it.row, it.col) }
        val myPlayer = players.value.firstOrNull { it.uid == currentUid } ?: return

        val tilesToEvaluate = _uiState.value.selectedTile?.let { listOf(it) } ?: myPlayer.hand
        val allValidMoves = tilesToEvaluate.flatMap { MoveValidator.getValidMoves(boardMap, it) }

        if (allValidMoves.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No hay jugadas automáticas disponibles")
            return
        }

        val bestMove = allValidMoves.maxByOrNull { move ->
            val cell1 = boardMap[Pair(move.row, move.col)] ?: Cell()
            val cell2 = boardMap[Pair(move.row2, move.col2)] ?: Cell()
            ScoreCalculator.calculateMovePoints(cell1.type, cell1.conquestCount, cell2.type, cell2.conquestCount)
        } ?: allValidMoves.first()

        _uiState.value = _uiState.value.copy(previewMove = bestMove)
        confirmCurrentMove()
    }

    fun draw() {
        val currentRoomId = _roomId.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                gameRepo.drawTile(currentRoomId, currentUid)
                _uiState.value = GameUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun pass() {
        val currentRoomId = _roomId.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                gameRepo.passTurn(currentRoomId, currentUid)
                _uiState.value = GameUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun abandon(onComplete: () -> Unit) {
        val currentRoomId = _roomId.value ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            try {
                gameRepo.abandon(currentRoomId, currentUid)
            } catch (_: Exception) {}
            _uiState.value = _uiState.value.copy(loading = false)
            onComplete()
        }
    }

    fun restart() {
        val currentRoomId = _roomId.value ?: return
        viewModelScope.launch {
            try { gameRepo.restart(currentRoomId) } catch (_: Exception) {}
        }
    }

    fun startPortalRelocation() {
        val currentRoomId = _roomId.value ?: return
        val boardMap = board.value.associateBy { Pair(it.row, it.col) }
        val currentCells = boardMap.values.filter { it.placedBy == currentUid && it.value != null }
        
        if (currentCells.isNotEmpty()) {
            val lastTilePlaced = Tile(currentCells.first().value ?: 0, currentCells.last().value ?: 0)
            _uiState.value = GameUiState(
                isRelocatingPortal = true,
                selectedTile = lastTilePlaced,
                error = "Selecciona dónde reubicar tu ficha."
            )
        } else {
            resolvePortalStay()
        }
    }

    fun resolvePortalStay() {
        val currentRoomId = _roomId.value ?: return
        viewModelScope.launch {
            gameRepo.resolvePortal(currentRoomId, currentUid, null)
            _uiState.value = GameUiState()
        }
    }

    // ---------- EJECUCIÓN COMBINADA PARA BOT (IA) ----------
    private fun observeBotTurnCombined() {
        viewModelScope.launch {
            combine(room, players, pool) { r, p, _ -> Pair(r, p) }.collect { (currentRoom, activePlayers) ->
                if (currentRoom == null || currentRoom.status != "PLAYING") return@collect
                val isHost = currentRoom.hostId == currentUid
                if (!isHost) return@collect

                val currentTurnUid = currentRoom.currentTurnUid
                val isBot = currentTurnUid.startsWith("bot_") ||
                        activePlayers.firstOrNull { it.uid == currentTurnUid }?.isAi == true
                        
                val pendingPortalUid = currentRoom.pendingPortalUid
                val isBotPortal = pendingPortalUid != null && (pendingPortalUid.startsWith("bot_") || 
                        activePlayers.firstOrNull { it.uid == pendingPortalUid }?.isAi == true)

                if (isBot || isBotPortal) {
                    val targetUid = pendingPortalUid ?: currentTurnUid
                    botJob?.cancel()
                    botJob = viewModelScope.launch {
                        delay(1200)
                        if (room.value?.currentTurnUid == currentTurnUid || room.value?.pendingPortalUid == pendingPortalUid) {
                            try {
                                gameRepo.executeBotTurn(currentRoom.id, targetUid)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}
