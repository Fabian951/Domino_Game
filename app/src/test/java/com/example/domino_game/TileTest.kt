package com.example.domino_game

import com.example.domino_game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileTest {

    @Test
    fun testTileTotalPips() {
        val tile = Tile(left = 3, right = 5)
        assertEquals(8, tile.totalPips)
    }

    @Test
    fun testTileIsDouble() {
        val doubleTile = Tile(left = 6, right = 6)
        val normalTile = Tile(left = 2, right = 4)

        assertTrue(doubleTile.isDouble)
        assertFalse(normalTile.isDouble)
    }

    @Test
    fun testTileContains() {
        val tile = Tile(left = 4, right = 1)

        assertTrue(tile.contains(4))
        assertTrue(tile.contains(1))
        assertFalse(tile.contains(6))
    }

    @Test
    fun testTileFlipped() {
        val tile = Tile(left = 2, right = 6)
        val flipped = tile.flipped()

        assertEquals(6, flipped.left)
        assertEquals(2, flipped.right)
    }
}
