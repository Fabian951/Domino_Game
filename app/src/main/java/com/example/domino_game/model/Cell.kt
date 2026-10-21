package com.example.domino_game.model

data class Cell(
    val id: String = "",
    val row: Int = 0,
    val col: Int = 0,
    val type: String = "NORMAL",
    val ownerId: String? = null,
    val conquestCount: Int = 0,
    val value: Int? = null,
    val pairRow: Int? = null,
    val pairCol: Int? = null,
    val placedBy: String? = null
)
