package com.example.domino_game

import com.example.domino_game.model.Cell
import com.example.domino_game.model.GameEngine
import com.example.domino_game.model.Move
import com.example.domino_game.model.Orientation
import com.example.domino_game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Test

class GameEngineTest {

    @Test
    fun testTurnAdvancementSameRound() {
        val turnOrder = listOf("p1", "p2", "p3")
        val (nextTurn, newRound) = GameEngine.calculateNextTurn("p1", turnOrder, 1)

        assertEquals("p2", nextTurn)
        assertEquals(1, newRound)
    }

    @Test
    fun testTurnAdvancementIncrementsRoundWhenReturningToFirst() {
        val turnOrder = listOf("p1", "p2", "p3")
        val (nextTurn, newRound) = GameEngine.calculateNextTurn("p3", turnOrder, 1)

        assertEquals("p1", nextTurn)
        assertEquals(2, newRound)
    }

    @Test
    fun testTurnAdvancementSkipsAbandonedPlayers() {
        val turnOrder = listOf("p1", "p3")
        val (nextTurn, newRound) = GameEngine.calculateNextTurn("p1", turnOrder, 1)

        assertEquals("p3", nextTurn)
        assertEquals(1, newRound)
    }

    @Test
    fun testApplyMovePairsCellsCorrectly() {
        val boardMap = mapOf(
            Pair(0, 0) to Cell(row = 0, col = 0),
            Pair(0, 1) to Cell(row = 0, col = 1)
        )
        val move = Move(Tile(3, 4), 0, 0, Orientation.HORIZONTAL)
        
        val (updatedMap, _) = GameEngine.applyMove(boardMap, move, "uid1")
        
        val cell1 = updatedMap[Pair(0, 0)]!!
        val cell2 = updatedMap[Pair(0, 1)]!!
        
        assertEquals(0, cell1.pairRow)
        assertEquals(1, cell1.pairCol)
        assertEquals(3, cell1.value)
        assertEquals("uid1", cell1.placedBy)
        
        assertEquals(0, cell2.pairRow)
        assertEquals(0, cell2.pairCol)
        assertEquals(4, cell2.value)
        assertEquals("uid1", cell2.placedBy)
    }
}
