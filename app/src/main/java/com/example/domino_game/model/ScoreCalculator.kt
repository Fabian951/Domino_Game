package com.example.domino_game.model

object ScoreCalculator {

    fun calculateCellPoints(cellType: String, currentConquestCount: Int): Int {
        return when (cellType.uppercase()) {
            "BONUS" -> 4          // 1 base + 3 extra
            "TREASURE" -> 6       // 1 base + 5 extra
            "TRAP" -> -1          // 1 base - 2 extra = -1
            "FORTRESS" -> if (currentConquestCount == 0) 0 else 3 // 0 en 1a conquista, 3 en 2a
            else -> 1             // NORMAL, PORTAL -> 1 base
        }
    }

    fun calculateMovePoints(
        cell1Type: String,
        cell1ConquestCount: Int,
        cell2Type: String,
        cell2ConquestCount: Int
    ): Int {
        val pts1 = calculateCellPoints(cell1Type, cell1ConquestCount)
        val pts2 = calculateCellPoints(cell2Type, cell2ConquestCount)
        return pts1 + pts2
    }
}
