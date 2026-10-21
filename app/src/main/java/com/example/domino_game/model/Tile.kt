package com.example.domino_game.model

data class Tile(
    val left: Int = 0,
    val right: Int = 0
) {
    val sideA: Int get() = left
    val sideB: Int get() = right

    val totalPips: Int
        get() = left + right

    val isDouble: Boolean
        get() = left == right

    fun contains(value: Int): Boolean {
        return left == value || right == value
    }

    fun flipped(): Tile {
        return Tile(right, left)
    }

    override fun toString(): String {
        return "[$left|$right]"
    }
}
