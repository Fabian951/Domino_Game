package com.example.domino_game.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.domino_game.model.Cell
import com.example.domino_game.model.Move
import com.example.domino_game.model.Player

@Composable
fun GridBoardView(
    boardCells: List<Cell>,
    players: List<Player>,
    validMoves: List<Move>,
    previewMove: Move?,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Colores de conquista de jugadores con un tono más vibrante y moderno
    val playerColors = listOf(
        Color(0xFF2196F3), // Jugador 1 (Azul Neón)
        Color(0xFF9C27B0), // Jugador 2 (Púrpura Profundo)
        Color(0xFF4CAF50), // Jugador 3 (Verde Esmeralda)
        Color(0xFFFF9800)  // Jugador 4 (Naranja Eléctrico)
    )

    val validAnchorPositions = validMoves.map { Pair(it.row, it.col) }.toSet()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val boardSize = if (maxWidth < maxHeight) maxWidth else maxHeight

        Column(
            modifier = Modifier
                .size(boardSize)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF112614)) // Rejilla de fondo (Verde Sombra muy oscuro)
                .border(3.dp, Color(0xFF09140B), RoundedCornerShape(18.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp) // Espaciado que da efecto 3D
        ) {
            for (r in 0 until 10) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until 10) {
                        val cell = boardCells.firstOrNull { it.row == r && it.col == c }
                            ?: Cell(id = "${r}_${c}", row = r, col = c)

                        val ownerIndex = players.indexOfFirst { it.uid == cell.ownerId }
                        val ownerColor = if (ownerIndex != -1) playerColors[ownerIndex % playerColors.size] else Color.Transparent

                        val isPreview1 = previewMove != null && previewMove.row == r && previewMove.col == c
                        val isPreview2 = previewMove != null && previewMove.row2 == r && previewMove.col2 == c
                        val previewValue = if (isPreview1) previewMove?.val1 else if (isPreview2) previewMove?.val2 else null

                        TerritoryCellView(
                            cell = cell,
                            ownerColor = ownerColor,
                            isValidAnchor = validAnchorPositions.contains(Pair(r, c)),
                            previewValue = previewValue,
                            onClick = { onCellClick(r, c) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerritoryCellView(
    cell: Cell,
    ownerColor: Color,
    isValidAnchor: Boolean,
    previewValue: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDashedFortress = cell.type.uppercase() == "FORTRESS" && cell.conquestCount == 1

    // Bordes de celda sutiles
    val borderColor = when {
        isValidAnchor -> MaterialTheme.colorScheme.primary
        ownerColor != Color.Transparent -> ownerColor
        isDashedFortress -> Color(0xFF90A4AE)
        else -> Color(0xFF153517) // Borde verde oscuro para integrarse elegantemente
    }

    val borderWidth = when {
        isValidAnchor -> 3.dp
        ownerColor != Color.Transparent -> 2.5.dp
        else -> 1.dp
    }

    // NUEVA PALETA: Fondo basado en Verde Oliva Profundo (0xFF1E4620)
    val backgroundColor = when {
        previewValue != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        isValidAnchor -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ownerColor != Color.Transparent -> ownerColor.copy(alpha = 0.35f)
        else -> when (cell.type.uppercase()) {
            "BONUS" -> Color(0xFF3E3516)     // Oro Mate / Baldosa Estrella
            "TREASURE" -> Color(0xFF12373D)  // Azul Océano / Baldosa Diamante
            "TRAP" -> Color(0xFF3F1919)      // Terracota / Baldosa Trampa
            "FORTRESS" -> Color(0xFF2D373C)  // Pizarra / Baldosa Castillo
            "PORTAL" -> Color(0xFF311E44)    // Amatista / Baldosa Portal
            else -> Color(0xFF1E4620)        // NUEVO: Casilla normal Verde Oliva Real 🎲
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp)) // Esquinas redondeadas en las baldosas
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        val displayVal = previewValue ?: cell.value

        if (displayVal != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .background(Color(0xFF0A170B).copy(alpha = 0.75f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayVal.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            if (isDashedFortress) {
                Text(
                    text = "1/2",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                )
            }
        } else {
            val specialTag = when (cell.type.uppercase()) {
                "BONUS" -> "⭐"
                "TREASURE" -> "💎"
                "TRAP" -> "⚠️"
                "FORTRESS" -> if (cell.conquestCount == 1) "1/2" else "🏰"
                "PORTAL" -> "🌀"
                else -> ""
            }

            if (specialTag.isNotEmpty()) {
                Text(
                    text = specialTag,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
