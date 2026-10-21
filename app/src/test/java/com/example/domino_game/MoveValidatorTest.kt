package com.example.domino_game

import com.example.domino_game.model.Cell
import com.example.domino_game.model.Move
import com.example.domino_game.model.MoveValidator
import com.example.domino_game.model.Orientation
import com.example.domino_game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveValidatorTest {

    @Test
    fun testEmptyBoardValidMove() {
        val board = emptyMap<Pair<Int, Int>, Cell>()
        val move = Move(Tile(3, 4), 0, 0, Orientation.HORIZONTAL)

        assertTrue(MoveValidator.isValid(board, move))
    }

    @Test
    fun testVerticalMoveWithoutFlip() {
        val board = emptyMap<Pair<Int, Int>, Cell>()
        val move = Move(Tile(3, 5), 1, 1, Orientation.VERTICAL, flipped = false)

        assertTrue(MoveValidator.isValid(board, move))
        assertEquals(3, move.val1)
        assertEquals(5, move.val2)
        assertEquals(2, move.row2)
        assertEquals(1, move.col2)
    }

    @Test
    fun testVerticalMoveWithFlip() {
        val board = emptyMap<Pair<Int, Int>, Cell>()
        val move = Move(Tile(3, 5), 1, 1, Orientation.VERTICAL, flipped = true)

        assertTrue(MoveValidator.isValid(board, move))
        assertEquals(5, move.val1)
        assertEquals(3, move.val2)
    }

    @Test
    fun testGetValidMovesAndHasAnyValidMove() {
        val board = emptyMap<Pair<Int, Int>, Cell>()
        val tile = Tile(2, 4)
        val validMoves = MoveValidator.getValidMoves(board, tile)

        assertTrue(validMoves.isNotEmpty())
        assertTrue(MoveValidator.hasAnyValidMove(board, listOf(tile)))
    }

    @Test
    fun testOccupiedCellIsInvalid() {
        val occupiedCell = Cell(row = 0, col = 0, value = 4)
        val board = mapOf(Pair(0, 0) to occupiedCell)

        val move = Move(Tile(4, 1), 0, 0, Orientation.HORIZONTAL)
        assertFalse(MoveValidator.isValid(board, move))
    }

    @Test
    fun testFortressWithConquestCountTwoIsRejected() {
        val fortressCell = Cell(row = 1, col = 1, type = "FORTRESS", conquestCount = 2, value = 3)
        val board = mapOf(Pair(1, 1) to fortressCell)

        val move = Move(Tile(3, 2), 1, 1, Orientation.HORIZONTAL)
        assertFalse(MoveValidator.isValid(board, move))
    }

    @Test
    fun testStrictAdjacencyRule() {
        // Tablero con un 6 en (0,0) y un 3 en (0,3)
        val board = mapOf(
            Pair(0, 0) to Cell(row = 0, col = 0, value = 6),
            Pair(0, 3) to Cell(row = 0, col = 3, value = 3)
        )

        // Intento colocar [6|2] en (0,1) y (0,2). 
        // Mitad 1 (con valor 6) toca el 6 en (0,0) -> OK
        // Mitad 2 (con valor 2) toca el 3 en (0,3) -> ERROR (chocan los numeros)
        val move = Move(Tile(6, 2), 0, 1, Orientation.HORIZONTAL)
        assertFalse(MoveValidator.isValid(board, move))
    }
}
