package com.example.domino_game

import com.example.domino_game.model.Player
import com.example.domino_game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTest {

    @Test
    fun testEmptyPlayerHand() {
        val p = Player(uid = "p1", hand = emptyList())
        assertTrue(p.hasEmptyHand)
        assertEquals(0, p.totalHandPips)
    }

    @Test
    fun testPlayerHandWithTiles() {
        val p = Player(
            uid = "p2",
            hand = listOf(Tile(3, 4), Tile(6, 6))
        )
        assertFalse(p.hasEmptyHand)
        assertEquals(19, p.totalHandPips)
    }
}
