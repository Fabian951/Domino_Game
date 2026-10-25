package com.example.domino_game

import com.example.domino_game.model.Cell
import com.example.domino_game.model.EndGameDetector
import com.example.domino_game.model.Player
import com.example.domino_game.model.Room
import com.example.domino_game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndGameDetectorTest {

    @Test
    fun testEmptyHandTriggersGameOver() {
        val p1 = Player(uid = "p1", hand = emptyList())
        val p2 = Player(uid = "p2", hand = listOf(Tile(1, 1)))
        val room = Room(turnOrder = listOf("p1", "p2"))

        assertTrue(EndGameDetector.isGameOver(room, listOf(p1, p2), emptyMap(), 10))
    }

    @Test
    fun testBlockedGameWithEmptyPoolTriggersGameOver() {
        val occupiedCell = Cell(row = 0, col = 0, value = 6)
        val board = mapOf(Pair(0, 0) to occupiedCell)

        val p1 = Player(uid = "p1", hand = listOf(Tile(1, 1)))
        val room = Room(turnOrder = listOf("p1"))

        // p1 no puede jugar [1|1] junta a (0,0) que tiene 6. Pozo vacio (0).
        assertTrue(EndGameDetector.isGameOver(room, listOf(p1), board, poolCount = 0))
    }

    @Test
    fun testGetWinnerByScoreAndTerritories() {
        val p1 = Player(uid = "p1", name = "Jugador 1", score = 25, territories = 5)
        val p2 = Player(uid = "p2", name = "Jugador 2", score = 30, territories = 3)

        val winner = EndGameDetector.getWinner(listOf(p1, p2))
        assertEquals("p2", winner?.uid)
    }

    @Test
    fun testTieBreakerByTerritories() {
        val p1 = Player(uid = "p1", name = "Jugador 1", score = 20, territories = 6)
        val p2 = Player(uid = "p2", name = "Jugador 2", score = 20, territories = 4)

        val winner = EndGameDetector.getWinner(listOf(p1, p2))
        assertEquals("p1", winner?.uid)
    }

    @Test
    fun testPersistentTieReturnsNull() {
        val p1 = Player(uid = "p1", name = "Jugador 1", score = 20, territories = 4)
        val p2 = Player(uid = "p2", name = "Jugador 2", score = 20, territories = 4)

        val winner = EndGameDetector.getWinner(listOf(p1, p2))
        assertNull(winner)
    }
}
