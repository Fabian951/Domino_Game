package com.example.domino_game.model

data class Move(
    val tile: Tile,
    val row: Int,
    val col: Int,
    val orientation: Orientation,
    val flipped: Boolean = false
) {
    val val1: Int get() = if (flipped) tile.right else tile.left
    val val2: Int get() = if (flipped) tile.left else tile.right

    val row2: Int get() = if (orientation == Orientation.VERTICAL) row + 1 else row
    val col2: Int get() = if (orientation == Orientation.HORIZONTAL) col + 1 else col
}
