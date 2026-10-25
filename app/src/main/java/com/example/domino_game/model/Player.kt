package com.example.domino_game.model

data class Player(
    val uid: String = "",
    val name: String = "",
    val score: Int = 0,
    val territories: Int = 0,
    val hand: List<Tile> = emptyList(),
    val handCount: Int = 0,
    val isAi: Boolean = false,
    val connected: Boolean = true
) {
    val id: String get() = uid

    val hasEmptyHand: Boolean get() = hand.isEmpty()

    val totalHandPips: Int get() = hand.sumOf { it.totalPips }

    fun hasPlayableTile(board: Map<Pair<Int, Int>, Cell>): Boolean = hand.any { MoveValidator.getValidMoves(board, it).isNotEmpty() }
}
