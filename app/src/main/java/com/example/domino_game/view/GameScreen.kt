package com.example.domino_game.view

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domino_game.view.components.AbandonConfirmDialog
import com.example.domino_game.view.components.GameOverDialog
import com.example.domino_game.view.components.GridBoardView
import com.example.domino_game.view.components.ScoreBoardView
import com.example.domino_game.view.components.TileView
import com.example.domino_game.viewmodel.GameViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color

@Composable
fun GameScreen(
    roomId: String,
    onLeaveGame: () -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    LaunchedEffect(roomId) {
        viewModel.setRoomId(roomId)
    }

    val room by viewModel.room.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val boardCells by viewModel.board.collectAsStateWithLifecycle()
    val pool by viewModel.pool.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playableTiles by viewModel.playableTiles.collectAsStateWithLifecycle()

    val myPlayer = players.firstOrNull { it.uid == viewModel.currentUid }
    val currentTurnUid = room?.currentTurnUid ?: ""
    val isMyTurn = currentTurnUid == viewModel.currentUid
    val isHost = room?.hostId == viewModel.currentUid

    var showAbandonDialog by remember { mutableStateOf(false) }

    var isHandExpanded by remember { mutableStateOf(false) }

    val winnerPlayer = players.firstOrNull { it.uid == room?.winnerId }

    if (room?.status == "FINISHED") {
        GameOverDialog(
            winner = winnerPlayer,
            players = players,
            isHost = isHost,
            onRestart = { viewModel.restart() },
            onReturnToLobby = {
                onLeaveGame()
            }
        )
    }

    if (showAbandonDialog) {
        AbandonConfirmDialog(
            onConfirm = {
                showAbandonDialog = false
                viewModel.abandon { onLeaveGame() }
            },
            onDismiss = { showAbandonDialog = false }
        )
    }

    // Portal Dialog
    if (room?.pendingPortalUid == viewModel.currentUid && !uiState.isRelocatingPortal) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("🌀 Casilla Portal Activada", fontWeight = FontWeight.Bold) },
            text = { Text("¡Caíste en un Portal! Puedes quedarte en esta posición o resolver el portal reubicando tu ficha en otro lugar válido.") },
            confirmButton = {
                Button(onClick = { viewModel.startPortalRelocation() }) {
                    Text("Reubicar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.resolvePortalStay() }) {
                    Text("Quedarme")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dominó Conquista",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sala: ${room?.code ?: "..."} | Ronda ${room?.round ?: 1}/${room?.maxRounds ?: 30}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = { showAbandonDialog = true }) {
                    Text("Salir", maxLines = 1)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Marcador
            ScoreBoardView(
                players = players,
                currentTurnUid = currentTurnUid,
                poolCount = pool.size
            )

            // Indicador de Turno
            val currentTurnPlayerName = players.firstOrNull { it.uid == currentTurnUid }?.name ?: "..."
            Text(
                text = if (uiState.isRelocatingPortal) "🌀 SELECCIONA NUEVA UBICACIÓN" else if (isMyTurn) "🎯 ¡ES TU TURNO!" else "⏳ Turno de: $currentTurnPlayerName",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isMyTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            // 2. Tablero 8x8 de Conquista
            GridBoardView(
                boardCells = boardCells,
                players = players,
                validMoves = uiState.validMovesForSelectedTile,
                previewMove = uiState.previewMove,
                onCellClick = { row, col ->
                    if (isMyTurn || uiState.isRelocatingPortal) {
                        viewModel.onCellTapped(row, col)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Ultimo Mensaje de la Sala
            if (!room?.lastMessage.isNullOrEmpty()) {
                Text(
                    text = "📢 ${room?.lastMessage}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controles de Rotacion e Informacion de la Ficha Seleccionada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.previewMove != null) {
                    Button(
                        onClick = { viewModel.confirmCurrentMove() },
                        enabled = (isMyTurn || uiState.isRelocatingPortal) && !uiState.loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Colocar ✓", maxLines = 1, fontSize = 11.sp)
                    }
                }

                FilledTonalButton(
                    onClick = { viewModel.rotate() },
                    enabled = (isMyTurn || uiState.isRelocatingPortal) && uiState.previewMove != null && uiState.validPlacementsAtAnchor.size > 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rotar ↺", maxLines = 1, fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { viewModel.flip() },
                    enabled = (isMyTurn || uiState.isRelocatingPortal) && uiState.previewMove != null && uiState.validPlacementsAtAnchor.size > 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Voltear ⇄", maxLines = 1, fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { viewModel.autoMove() },
                    enabled = isMyTurn && !uiState.loading && !uiState.isRelocatingPortal,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auto ⚡", maxLines = 1, fontSize = 11.sp)
                }
            }

            // Mensajes de Error
            uiState.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Mano del Jugador (Menú Desplegable Estilizado)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161E), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHandExpanded = !isHandExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tu Mano (${myPlayer?.hand?.size ?: 0} fichas):",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isHandExpanded) "▼ Ocultar" else "▲ Mostrar Fichas",
                        color = Color(0xFFFFC107),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (isHandExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        myPlayer?.hand?.forEach { tile ->
                            val isPlayable = playableTiles.contains(tile)
                            TileView(
                                tile = tile,
                                isSelected = uiState.selectedTile == tile,
                                modifier = Modifier.alpha(if (isPlayable) 1f else 0.4f),
                                onClick = {
                                    if (isMyTurn && !uiState.isRelocatingPortal) viewModel.selectTile(tile)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Acciones de Fin de Turno Nativas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.draw() },
                    enabled = isMyTurn && pool.isNotEmpty() && !uiState.loading && !uiState.isRelocatingPortal,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Robar (${pool.size})", maxLines = 1, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.pass() },
                    enabled = isMyTurn && !uiState.loading && !uiState.isRelocatingPortal,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pasar Turno", maxLines = 1, fontSize = 12.sp)
                }
            }

            if (uiState.loading) {
                Spacer(modifier = Modifier.height(2.dp))
                CircularProgressIndicator()
            }
        }
    }
}
