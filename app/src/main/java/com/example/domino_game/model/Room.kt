package com.example.domino_game.model

data class Room(
    val id: String = "",
    val code: String = "",
    val hostId: String = "",
    val status: String = "WAITING",
    val currentTurnUid: String = "",
    val turnOrder: List<String> = emptyList(),
    val round: Int = 1,
    val maxRounds: Int = 30,
    val winnerId: String? = null,
    val pendingPortalUid: String? = null,
    val lastMessage: String = ""
)
