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
    val error: String? = null,
    val turnTimeLeft: Int = 60
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
        observeTurnTimer()
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

    private fun getEffectiveBoardMap(): Map<Pair<Int, Int>, Cell> {
        val boardMap = board.value.associateBy { Pair(it.row, it.col) }.toMutableMap()
        if (_uiState.value.isRelocatingPortal) {
            val currentCells = boardMap.values.filter { it.placedBy == currentUid && it.value != null }
            val portalCell1 = currentCells.firstOrNull { cell ->
                cell.type.uppercase() == "PORTAL" ||
                        boardMap[Pair(cell.pairRow ?: -1, cell.pairCol ?: -1)]?.type?.uppercase() == "PORTAL"
            } ?: currentCells.lastOrNull()

            if (portalCell1 != null) {
                val portalCell2 = boardMap[Pair(portalCell1.pairRow ?: -1, portalCell1.pairCol ?: -1)]
                boardMap[Pair(portalCell1.row, portalCell1.col)] = portalCell1.copy(value = null, ownerId = null, conquestCount = maxOf(0, portalCell1.conquestCount - 1))
                if (portalCell2 != null) {
                    boardMap[Pair(portalCell2.row, portalCell2.col)] = portalCell2.copy(value = null, ownerId = null, conquestCount = maxOf(0, portalCell2.conquestCount - 1))
                }
            }
        }
        return boardMap
    }

    // ---------- ACCIONES DEL JUGADOR ----------
    fun selectTile(tile: Tile) {
        if (_uiState.value.selectedTile == tile) {
            _uiState.value = _uiState.value.copy(selectedTile = null, validMovesForSelectedTile = emptyList(), previewMove = null)
        } else {
            val boardMap = getEffectiveBoardMap()
            val validMoves = MoveValidator.getValidMoves(boardMap, tile)
            
            val errMessage = if (validMoves.isEmpty()) "Esta ficha no tiene movimientos válidos" else null
            _uiState.value = _uiState.value.copy(
                selectedTile = tile,
                validMovesForSelectedTile = validMoves,
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
            _uiState.value = _uiState.value.copy(error = "Selecciona una ficha primero")
            return
        }

        val boardMap = getEffectiveBoardMap()
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
        val rawBoardMap = board.value.associateBy { Pair(it.row, it.col) }
        val currentCells = rawBoardMap.values.filter { it.placedBy == currentUid && it.value != null }
        
        val portalCell1 = currentCells.firstOrNull { cell ->
            cell.type.uppercase() == "PORTAL" ||
                    rawBoardMap[Pair(cell.pairRow ?: -1, cell.pairCol ?: -1)]?.type?.uppercase() == "PORTAL"
        } ?: currentCells.lastOrNull()

        if (portalCell1 != null) {
            val portalCell2 = rawBoardMap[Pair(portalCell1.pairRow ?: -1, portalCell1.pairCol ?: -1)]
            val actualPortalTile = if (portalCell2 != null) {
                Tile(portalCell1.value ?: 0, portalCell2.value ?: 0)
            } else {
                Tile(portalCell1.value ?: 0, portalCell1.value ?: 0)
            }

            // Liberar temporalmente las casillas del portal para calcular movimientos válidos correctamente
            val tempBoardMap = rawBoardMap.toMutableMap()
            tempBoardMap[Pair(portalCell1.row, portalCell1.col)] = portalCell1.copy(value = null, ownerId = null, conquestCount = 0)
            if (portalCell2 != null) {
                tempBoardMap[Pair(portalCell2.row, portalCell2.col)] = portalCell2.copy(value = null, ownerId = null, conquestCount = 0)
            }

            val validMoves = MoveValidator.getValidMoves(tempBoardMap, actualPortalTile)

            _uiState.value = GameUiState(
                isRelocatingPortal = true,
                selectedTile = actualPortalTile,
                validMovesForSelectedTile = validMoves,
                error = if (validMoves.isEmpty()) "No hay casillas válidas para reubicar la ficha" else "Selecciona la nueva ubicación para tu ficha."
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

    private var turnTimerJob: Job? = null

    // Temporizador de 1 minuto (60 segundos) por turno con expulsión por inactividad
    private fun observeTurnTimer() {
        viewModelScope.launch {
            combine(room, players) { r, p -> Pair(r, p) }.collect { (currentRoom, activePlayers) ->
                turnTimerJob?.cancel()
                if (currentRoom == null || currentRoom.status != "PLAYING") return@collect

                val turnUid = currentRoom.currentTurnUid
                if (turnUid.isBlank()) return@collect

                val isMyTurn = turnUid == currentUid
                val isHost = currentRoom.hostId == currentUid

                turnTimerJob = viewModelScope.launch {
                    for (seconds in 60 downTo 1) {
                        _uiState.value = _uiState.value.copy(turnTimeLeft = seconds)
                        delay(1000)
                    }
                    _uiState.value = _uiState.value.copy(turnTimeLeft = 0)

                    // Si se agota 1 minuto (60 segundos) sin mover:
                    if (isMyTurn) {
                        abandon { }
                    } else if (isHost && !turnUid.startsWith("bot_")) {
                        try {
                            gameRepo.abandon(currentRoom.id, turnUid)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    // // ---------- EJECUCIÓN COMBINADA PARA BOT (IA) ----------
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
                        // OPTIMIZACIÓN: Reducimos el retardo artificial de 1200ms a 150ms para fluidez inmediata
                        delay(150)
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

