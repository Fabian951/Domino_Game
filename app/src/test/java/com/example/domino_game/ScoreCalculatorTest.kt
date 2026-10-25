package com.example.domino_game

import com.example.domino_game.model.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {

    @Test
    fun testBonusCellPoints() {
        val pts = ScoreCalculator.calculateCellPoints("BONUS", 0)
        assertEquals(4, pts)
    }

    @Test
    fun testTreasureCellPoints() {
        val pts = ScoreCalculator.calculateCellPoints("TREASURE", 0)
        assertEquals(6, pts)
    }

    @Test
    fun testTrapCellPoints() {
        val pts = ScoreCalculator.calculateCellPoints("TRAP", 0)
        assertEquals(-1, pts)
    }

    @Test
    fun testFortressConquestPoints() {
        val ptsFirstConquest = ScoreCalculator.calculateCellPoints("FORTRESS", 0)
        val ptsSecondConquest = ScoreCalculator.calculateCellPoints("FORTRESS", 1)

        assertEquals(0, ptsFirstConquest)
        assertEquals(3, ptsSecondConquest)
    }

    @Test
    fun testNonNegativeScoreCoercion() {
        val pts = ScoreCalculator.calculateMovePoints("TRAP", 0, "TRAP", 0) // -1 + -1 = -2
        val newScore = (0 + pts).coerceAtLeast(0)
        assertEquals(0, newScore)
    }
}
