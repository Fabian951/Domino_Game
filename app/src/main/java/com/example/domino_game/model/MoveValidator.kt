package com.example.domino_game.model

object MoveValidator {

    fun isValid(board: Map<Pair<Int, Int>, Cell>, move: Move): Boolean {
        val r1 = move.row
        val c1 = move.col
        val r2 = move.row2
        val c2 = move.col2

        // 1. Limites de la cuadricula (10x10: 0..9)
        if (r1 !in 0..9 || c1 !in 0..9 || r2 !in 0..9 || c2 !in 0..9) {
            return false
        }

        // 2. Verificar que ambas casillas esten disponibles (libres o FORTRESS con conquestCount == 1)
        val cell1 = board[Pair(r1, c1)] ?: Cell(row = r1, col = c1)
        val cell2 = board[Pair(r2, c2)] ?: Cell(row = r2, col = c2)

        if (!isCellPlayable(cell1) || !isCellPlayable(cell2)) {
            return false
        }

        // 3. Regla de adyacencia Estricta del Dominó
        val isBoardEmpty = board.values.none { it.value != null }
        if (isBoardEmpty) {
            return true // Primer turno, tablero vacio
        }

        val v1 = move.val1
        val v2 = move.val2

        val pos1 = Pair(r1, c1)
        val pos2 = Pair(r2, c2)

        val neighbors1 = getOrthogonalNeighbors(r1, c1).filter { it != pos2 }
        val neighbors2 = getOrthogonalNeighbors(r2, c2).filter { it != pos1 }

        var connectsToExisting = false

        // Verificar vecinos de la mitad 1
        for (pos in neighbors1) {
            val neighborValue = board[pos]?.value
            if (neighborValue != null) {
                if (neighborValue != v1) return false // Regla Rota: choca con un número distinto
                connectsToExisting = true
            }
        }

        // Verificar vecinos de la mitad 2
        for (pos in neighbors2) {
            val neighborValue = board[pos]?.value
            if (neighborValue != null) {
                if (neighborValue != v2) return false // Regla Rota: choca con un número distinto
                connectsToExisting = true
            }
        }

        // Debe conectar con al menos 1 ficha, y NINGUNA ficha adyacente debe tener un número distinto
        return connectsToExisting
    }

    private fun isCellPlayable(cell: Cell): Boolean {
        if (cell.value == null) return true
        if (cell.type.uppercase() == "FORTRESS" && cell.conquestCount == 1) return true
        return false
    }

    private fun getOrthogonalNeighbors(r: Int, c: Int): List<Pair<Int, Int>> {
        return listOf(
            Pair(r - 1, c),
            Pair(r + 1, c),
            Pair(r, c - 1),
            Pair(r, c + 1)
        ).filter { it.first in 0..9 && it.second in 0..9 }
    }

    fun getValidMoves(board: Map<Pair<Int, Int>, Cell>, tile: Tile): List<Move> {
        val validMoves = mutableListOf<Move>()
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                for (orientation in Orientation.entries) {
                    for (flipped in listOf(false, true)) {
                        val move = Move(tile, r, c, orientation, flipped)
                        if (isValid(board, move)) {
                            validMoves.add(move)
                        }
                    }
                }
            }
        }
        return validMoves
    }

    fun getValidPlacementsAt(board: Map<Pair<Int, Int>, Cell>, tile: Tile, targetRow: Int, targetCol: Int): List<Move> {
        val allValidMoves = getValidMoves(board, tile)
        return allValidMoves.filter { move ->
            (move.row == targetRow && move.col == targetCol) ||
                    (move.row2 == targetRow && move.col2 == targetCol)
        }
    }

    fun hasAnyValidMove(board: Map<Pair<Int, Int>, Cell>, hand: List<Tile>): Boolean {
        return hand.any { tile -> getValidMoves(board, tile).isNotEmpty() }
    }
}
