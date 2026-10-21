package com.example.domino_game.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domino_game.model.Tile

@Composable
fun TileView(
    tile: Tile,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isHorizontal: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = Color(0xFFFFFDD0) // Marfil / Hueso premium
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF3E3E3E)
    val borderWidth = if (isSelected) 3.dp else 1.5.dp

    val shape = RoundedCornerShape(10.dp)

    val contentModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(3.dp)

    if (isHorizontal) {
        Row(
            modifier = contentModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            HalfTileView(number = tile.left)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(30.dp)
                    .background(Color(0xFF555555))
            )
            HalfTileView(number = tile.right)
        }
    } else {
        Column(
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HalfTileView(number = tile.left)
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(30.dp)
                    .background(Color(0xFF555555))
            )
            HalfTileView(number = tile.right)
        }
    }
}

@Composable
fun HalfTileView(number: Int, size: Dp = 30.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1A1A1A)
        )
    }
}

@Composable
fun BackTileView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(30.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(1.5.dp, Color(0xFF0F172A), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color(0xFF334155), CircleShape)
        )
    }
}
