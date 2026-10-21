package com.example.domino_game.model

object EndGameDetector {

    fun isGameOver(
        room: Room,
        players: List<Player>,
        board: Map<Pair<Int, Int>, Cell>,
        poolCount: Int
    ): Boolean {
        // (d) Un solo jugador restante en la sala
        if (room.turnOrder.size <= 1) return true

        // (a) Alguien se quedo sin fichas
        if (players.any { it.hand.isEmpty() && room.turnOrder.contains(it.uid) }) return true

        // (c) Se supero el maximo de rondas
        if (room.round > room.maxRounds) return true

        // (b) Pozo vacio y nadie tiene jugada valida
        if (poolCount == 0) {
            val activePlayers = players.filter { room.turnOrder.contains(it.uid) }
            val anyHasMove = activePlayers.any { player ->
                MoveValidator.hasAnyValidMove(board, player.hand)
            }
            if (!anyHasMove) return true
        }

        return false
    }

    fun getWinner(players: List<Player>): Player? {
        if (players.isEmpty()) return null

        val sorted = players.sortedWith(
            compareByDescending<Player> { it.score }
                .thenByDescending { it.territories }
        )

        val top = sorted[0]
        if (sorted.size > 1) {
            val second = sorted[1]
            if (top.score == second.score && top.territories == second.territories) {
                return null // Empate persistente
            }
        }
        return top
    }
}
