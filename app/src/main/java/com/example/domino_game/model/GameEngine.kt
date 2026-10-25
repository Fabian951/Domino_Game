package com.example.domino_game.model

object GameEngine {

    fun calculateNextTurn(
        currentTurnUid: String,
        turnOrder: List<String>,
        currentRound: Int
    ): Pair<String, Int> {
        if (turnOrder.isEmpty()) return Pair("", currentRound)

        val currentIndex = turnOrder.indexOf(currentTurnUid)
        if (currentIndex == -1) {
            return Pair(turnOrder.first(), currentRound)
        }

        val nextIndex = (currentIndex + 1) % turnOrder.size
        val nextTurnUid = turnOrder[nextIndex]

        val newRound = if (nextIndex == 0) currentRound + 1 else currentRound
        return Pair(nextTurnUid, newRound)
    }

    /**
     * Aplica el movimiento sobre el tablero en memoria (pura logica).
     * Retorna el mapa del tablero actualizado con pairRow y pairCol correctos, y los puntos ganados.
     */
    fun applyMove(
        boardMap: Map<Pair<Int, Int>, Cell>,
        move: Move,
        uid: String
    ): Pair<Map<Pair<Int, Int>, Cell>, Int> {
        val r1 = move.row
        val c1 = move.col
        val r2 = move.row2
        val c2 = move.col2

        val cell1 = boardMap[Pair(r1, c1)] ?: Cell(id = "${r1}_${c1}", row = r1, col = c1)
        val cell2 = boardMap[Pair(r2, c2)] ?: Cell(id = "${r2}_${c2}", row = r2, col = c2)

        val pts1 = ScoreCalculator.calculateCellPoints(cell1.type, cell1.conquestCount)
        val pts2 = ScoreCalculator.calculateCellPoints(cell2.type, cell2.conquestCount)
        val totalPts = pts1 + pts2

        val newConquest1 = cell1.conquestCount + 1
        val newOwner1 = if (cell1.type.uppercase() == "FORTRESS" && newConquest1 == 1) null else uid
        val newCell1 = cell1.copy(
            value = move.val1,
            conquestCount = newConquest1,
            ownerId = newOwner1,
            placedBy = uid,
            pairRow = r2,
            pairCol = c2
        )

        val newConquest2 = cell2.conquestCount + 1
        val newOwner2 = if (cell2.type.uppercase() == "FORTRESS" && newConquest2 == 1) null else uid
        val newCell2 = cell2.copy(
            value = move.val2,
            conquestCount = newConquest2,
            ownerId = newOwner2,
            placedBy = uid,
            pairRow = r1,
            pairCol = c1
        )

        val updatedBoardMap = boardMap.toMutableMap()
        updatedBoardMap[Pair(r1, c1)] = newCell1
        updatedBoardMap[Pair(r2, c2)] = newCell2

        return Pair(updatedBoardMap, totalPts)
    }

    /**
     * Resuelve un reubicacion por portal liberando las celdas antiguas
     */
    fun applyPortalRelocation(
        boardMap: Map<Pair<Int, Int>, Cell>,
        oldR1: Int, oldC1: Int,
        oldR2: Int, oldC2: Int,
        newMove: Move,
        uid: String
    ): Pair<Map<Pair<Int, Int>, Cell>, Int> {
        
        val tempBoard = boardMap.toMutableMap()
        
        // Liberar 1
        val oldCell1 = tempBoard[Pair(oldR1, oldC1)]!!
        tempBoard[Pair(oldR1, oldC1)] = oldCell1.copy(
            value = null,
            ownerId = null,
            placedBy = null,
            pairRow = null,
            pairCol = null,
            type = if (oldCell1.type.uppercase() == "PORTAL") "NORMAL" else oldCell1.type,
            conquestCount = maxOf(0, oldCell1.conquestCount - 1)
        )
        
        // Liberar 2
        val oldCell2 = tempBoard[Pair(oldR2, oldC2)]!!
        tempBoard[Pair(oldR2, oldC2)] = oldCell2.copy(
            value = null,
            ownerId = null,
            placedBy = null,
            pairRow = null,
            pairCol = null,
            type = if (oldCell2.type.uppercase() == "PORTAL") "NORMAL" else oldCell2.type,
            conquestCount = maxOf(0, oldCell2.conquestCount - 1)
        )

        // Validar el nuevo movimiento contra el tablero liberado
        if (!MoveValidator.isValid(tempBoard, newMove)) {
            throw IllegalArgumentException("Movimiento de reubicación inválido")
        }

        // Aplicar el nuevo movimiento
        val (finalBoard, points) = applyMove(tempBoard, newMove, uid)
        
        return Pair(finalBoard, points)
    }
}
