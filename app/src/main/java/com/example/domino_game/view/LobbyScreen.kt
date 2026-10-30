package com.example.domino_game.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // Variable de estado para controlar la visibilidad del diálogo de confirmación
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Cuando el host inicia la partida, TODOS navegan al juego
    LaunchedEffect(room?.status) {
        val id = roomId
        if (room?.status == "PLAYING" && id != null) onGameStart(id)
    }

    // ---------- Diálogo de Alerta Estilizado en Tema Oscuro ----------
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.Black,
            title = {
                Text(
                    text = "⚠️ Eliminar Cuenta",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "¿Estás completamente seguro de que deseas borrar tu usuario? Esta acción es permanente, se desvinculará de la base de datos y perderás todo tu progreso.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount(
                            onSuccess = { onLogout() }
                        )
                    }
                ) {
                    Text(
                        text = "Eliminar definitivamente",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = "Cancelar",
                        color = Color.LightGray
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
            .padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (roomId == null) {
            // ---------- Cabecera de Usuario Fijo (Estilo Top Bar) ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E24))
                        .border(2.dp, Color(0xFFFFC107), CircleShape)
                ) {
                    Text(text = state.userAvatar, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = state.userName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "DOMINÓ CONQUISTA",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.weight(0.3f))

            // ---------- SECCIÓN DE MODOS DE JUEGO ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    GameModeCard(
                        title = "CREAR SALA",
                        subtitle = "Multijugador",
                        emoji = "👥",
                        containerColor = Color(0xFFF2A900),
                        enabled = !state.loading,
                        onClick = { viewModel.createRoom() }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    GameModeCard(
                        title = "PRÁCTICA",
                        subtitle = "Solo / Bot",
                        emoji = "🤖",
                        containerColor = Color(0xFFD61C4E),
                        enabled = !state.loading,
                        onClick = { viewModel.startSoloGame() }
                    )
                }
            }

            Spacer(Modifier.weight(0.3f))

            // ---------- Sección de Unirse con Código ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "o únete con un código",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(8.dp))

                var code by remember { mutableStateOf("") }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Código de sala", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFC107),
                        unfocusedBorderColor = Color(0xFF2B2B36)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.joinRoom(code) },
                    enabled = !state.loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Unirme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // CAMBIAMOS EL WEIGHT POR ESTE HEIGHT FIJO PARA SUBIR LOS BOTONES
            Spacer(Modifier.height(24.dp))

            // ---------- Botones de control de sesión en la parte inferior ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp), // Margen inferior para despegarlos de la barra del sistema
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onLogout) {
                    Text("Cerrar sesión", color = Color.LightGray, fontSize = 14.sp)
                }

                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("Borrar cuenta", color = Color(0xFFD32F2F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

        } else {
            // ---------- Sala de espera ----------
            val isHost = room?.hostId == viewModel.uid

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Código de la sala", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text(room?.code ?: "...", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("Jugadores (${players.size}/4)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                players.forEach { p ->
                    val tag = if (p.uid == room?.hostId) " (host)" else if (p.isAi) " (Bot)" else ""
                    Text("• ${p.name}$tag", color = Color.LightGray, fontSize = 16.sp)
                }
                Spacer(Modifier.height(24.dp))

                if (isHost) {
                    if (players.size < 4) {
                        OutlinedButton(
                            onClick = { viewModel.addBot() },
                            enabled = !state.loading,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
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
                        Spacer(Modifier.height(8.dp))
                        Text("Al iniciar solo, se agregará 1 Bot automáticamente.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Text("Esperando a que el host inicie la partida...", color = Color(0xFFFFC107))
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.leaveRoom() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salir de la sala") }
            }
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

// ---------- COMPONENTE ESTÉTICO: TARJETAS VERTICALES DE MODO DE JUEGO ----------
@Composable
fun GameModeCard(
    title: String,
    subtitle: String,
    emoji: String,
    containerColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 44.sp,
                    color = if (enabled) Color.Unspecified else Color.Gray
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
