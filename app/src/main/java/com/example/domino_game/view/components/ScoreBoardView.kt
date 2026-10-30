package com.example.domino_game.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domino_game.model.Player

@Composable
fun ScoreBoardView(
    players: List<Player>,
    currentTurnUid: String,
    poolCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF16161E) // Cambio 1: Fondo de la barra superior a negro premium
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MARCADOR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White // Textos del título en blanco
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF23232E)) // Fondo oscuro para la burbuja del pozo
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "POZO: $poolCount fichas",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107) // Resaltado dorado para el número de fichas en el pozo
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                players.forEach { player ->
                    PlayerScoreCard(
                        player = player,
                        isCurrentTurn = player.uid == currentTurnUid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerScoreCard(
    player: Player,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    // Cambio 2: Borde dorado brillante para el jugador en turno, de lo contrario transparente u oscuro
    val borderColor = if (isCurrentTurn) Color(0xFFFFC107) else Color(0xFF2B2B36)
    val backgroundColor = if (isCurrentTurn) {
        Color(0xFF23232E) // Tono un poco más claro para el jugador activo
    } else {
        Color(0xFF0D0D12) // Fondo negro puro para los oponentes inactivos
    }

    val playerTag = if (player.isAi) " (Bot)" else ""

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${player.name}$playerTag",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                color = Color.White // Nombre en blanco
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${player.score} pts",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCurrentTurn) Color(0xFFFFC107) else Color.White // Resaltado dorado si es su turno
            )
            Text(
                text = "🏰 ${player.territories} ter.",
                fontSize = 10.sp,
                color = Color.LightGray // Subtextos legibles en gris claro
            )
            Text(
                text = "🎴 ${player.handCount} fichas",
                fontSize = 10.sp,
                color = Color.LightGray // Subtextos legibles en gris claro
            )
        }
    }
}
