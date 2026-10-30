package com.example.domino_game.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domino_game.viewmodel.LobbyViewModel

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onGameStart: (roomId: String) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val roomId by viewModel.roomId.collectAsStateWithLifecycle()
    val room by viewModel.room.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()

    // Cuando el host inicia la partida, TODOS navegan al juego
    LaunchedEffect(room?.status) {
        val id = roomId
        if (room?.status == "PLAYING" && id != null) onGameStart(id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (roomId == null) {
            // ---------- Cabecera de Usuario Fijo ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Text(text = state.userAvatar, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = state.userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // ---------- Crear o unirse ----------
            var code by remember { mutableStateOf("") }

            Text("Dominó Conquista", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.createRoom() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Crear sala multijugador") }

            Spacer(Modifier.height(12.dp))

            FilledTonalButton(
                onClick = { viewModel.startSoloGame() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Modo Práctica (Solo / Bot)") }

            Spacer(Modifier.height(24.dp))
            Text("o únete con un código")
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Código de sala") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.joinRoom(code) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Unirme") }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onLogout) { Text("Cerrar sesión") }

        } else {
            // ---------- Sala de espera ----------
            val isHost = room?.hostId == viewModel.uid

            Text("Código de la sala", style = MaterialTheme.typography.titleMedium)
            Text(room?.code ?: "...", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(16.dp))

            Text("Jugadores (${players.size}/4)", style = MaterialTheme.typography.titleMedium)
            players.forEach { p ->
                val tag = if (p.uid == room?.hostId) " (host)" else if (p.isAi) " (Bot)" else ""
                Text("• ${p.name}$tag")
            }
            Spacer(Modifier.height(24.dp))

            if (isHost) {
                if (players.size < 4) {
                    OutlinedButton(
                        onClick = { viewModel.addBot() },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🤖 Agregar Bot (IA)") }
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = { viewModel.startGame() },
                    enabled = !state.loading && players.size >= 1,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Iniciar partida") }

                if (players.size == 1) {
                    Text("Al iniciar solo, se agregará 1 Bot automáticamente.")
                }
            } else {
                Text("Esperando a que el host inicie la partida...")
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.leaveRoom() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salir de la sala") }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
