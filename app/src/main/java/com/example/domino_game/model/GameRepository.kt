package com.example.domino_game.model

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GameRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) {
    private val rooms get() = db.collection("rooms")

    fun observeBoard(roomId: String): Flow<List<Cell>> = callbackFlow {
        val reg = rooms.document(roomId).collection("board")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val cells = snap?.toObjects(Cell::class.java) ?: emptyList()
                trySend(cells.sortedWith(compareBy({ it.row }, { it.col })))
            }
        awaitClose { reg.remove() }
    }

    fun observePool(roomId: String): Flow<List<Tile>> = callbackFlow {
        val reg = rooms.document(roomId).collection("pool").document("state")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                @Suppress("UNCHECKED_CAST")
                val rawTiles = snap?.get("tiles") as? List<Map<String, Long>> ?: emptyList()
                val tiles = rawTiles.map {
                    Tile(
                        left = (it["left"] ?: 0L).toInt(),
                        right = (it["right"] ?: 0L).toInt()
                    )
                }
                trySend(tiles)
            }
        awaitClose { reg.remove() }
    }

    private fun finishIfNeeded(
        roomObj: Room,
        allPlayers: List<Player>,
        updatedBoardMap: Map<Pair<Int, Int>, Cell>,
        poolCount: Int,
        transaction: Transaction,
        roomRef: DocumentReference,
        pendingPortalUid: String? = null
    ): Boolean {
        val isGameOver = EndGameDetector.isGameOver(roomObj, allPlayers, updatedBoardMap, poolCount)
        if (isGameOver) {
            val winner = EndGameDetector.getWinner(allPlayers)
            transaction.update(roomRef, mapOf(
                "status" to "FINISHED",
                "winnerId" to winner?.uid,
                "pendingPortalUid" to pendingPortalUid,
                "lastMessage" to if (winner != null) "¡Partida finalizada! Ganador: ${winner.name}" else "¡Partida finalizada en empate!"
            ))
            return true
        }
        return false
    }

    suspend fun playMove(
        roomId: String,
        uid: String,
        move: Move
    ) {
        val roomRef = rooms.document(roomId)
        val playerRef = roomRef.collection("players").document(uid)
        val boardColl = roomRef.collection("board")
        val poolRef = roomRef.collection("pool").document("state")
        val playersColl = roomRef.collection("players")

        db.runTransaction { transaction ->
            val roomSnap = transaction.get(roomRef)
            val status = roomSnap.getString("status") ?: ""
            if (status != "PLAYING") throw IllegalStateException("La partida no está en curso")

            val playerSnap = transaction.get(playerRef)
            val poolSnap = transaction.get(poolRef)

            @Suppress("UNCHECKED_CAST")
            val turnOrder = (roomSnap.get("turnOrder") as? List<String> ?: emptyList())
            val currentTurnUid = roomSnap.getString("currentTurnUid") ?: ""

            if (currentTurnUid != uid) throw IllegalStateException("No es tu turno")

            val allPlayerSnaps = turnOrder.map { transaction.get(playersColl.document(it)) }
            
            val boardSnaps = mutableListOf<DocumentSnapshot>()
            for (r in 0..7) {
                for (c in 0..7) {
                    boardSnaps.add(transaction.get(boardColl.document("${r}_${c}")))
                }
            }

            val boardList = boardSnaps.mapNotNull { it.toObject(Cell::class.java) }
            val boardMap = boardList.associateBy { Pair(it.row, it.col) }.toMutableMap()

            for (r in 0..7) {
                for (c in 0..7) {
                    if (!boardMap.containsKey(Pair(r, c))) {
                        boardMap[Pair(r, c)] = Cell(id = "${r}_${c}", row = r, col = c)
                    }
                }
            }

            if (!MoveValidator.isValid(boardMap, move)) {
                throw IllegalArgumentException("Movimiento inválido según las reglas del juego")
            }

            @Suppress("UNCHECKED_CAST")
            val rawHand = playerSnap.get("hand") as? List<Map<String, Long>> ?: emptyList()
            val currentHand = rawHand.map {
                Tile(left = (it["left"] ?: 0L).toInt(), right = (it["right"] ?: 0L).toInt())
            }.toMutableList()

            // Asegurarnos de que el Move que estamos procesando concuerde exactamente con la ficha de la mano
            val tileIndex = currentHand.indexOfFirst {
                (it.left == move.tile.left && it.right == move.tile.right) ||
                (it.left == move.tile.right && it.right == move.tile.left)
            }
            if (tileIndex == -1) throw IllegalArgumentException("No tienes esta ficha en tu mano")
            
            // Reemplazamos la ficha del Move con la ficha de la mano por si estaba girada en la UI
            // Esto asegura que la base de datos se actualice exactamente con la ficha que posee
            val actualTile = currentHand[tileIndex]
            val adjustedMove = move.copy(tile = actualTile)

            currentHand.removeAt(tileIndex)

            val (updatedBoardMap, totalPts) = GameEngine.applyMove(boardMap, adjustedMove, uid)
            val newCell1 = updatedBoardMap[Pair(adjustedMove.row, adjustedMove.col)]!!
            val newCell2 = updatedBoardMap[Pair(adjustedMove.row2, adjustedMove.col2)]!!

            val currentScore = (playerSnap.getLong("score") ?: 0L).toInt()
            val newScore = (currentScore + totalPts).coerceAtLeast(0)
            val newTerritoryCount = updatedBoardMap.values.count { it.ownerId == uid }

            val isPortalTriggered = newCell1.type.uppercase() == "PORTAL" || newCell2.type.uppercase() == "PORTAL"
            val pendingPortalUid = if (isPortalTriggered) uid else null

            @Suppress("UNCHECKED_CAST")
            val poolTiles = poolSnap.get("tiles") as? List<Any> ?: emptyList()

            val allPlayersList = allPlayerSnaps.mapNotNull { it.toObject(Player::class.java) }.map { p ->
                if (p.uid == uid) p.copy(hand = currentHand, score = newScore, territories = newTerritoryCount)
                else p
            }

            val currentRound = (roomSnap.getLong("round") ?: 1L).toInt()
            val maxRounds = (roomSnap.getLong("maxRounds") ?: 30L).toInt()
            val roomObj = Room(
                id = roomId,
                turnOrder = turnOrder,
                round = currentRound,
                maxRounds = maxRounds
            )

            transaction.set(boardColl.document(newCell1.id), newCell1)
            transaction.set(boardColl.document(newCell2.id), newCell2)

            transaction.update(playerRef, mapOf(
                "hand" to currentHand,
                "handCount" to currentHand.size,
                "score" to newScore,
                "territories" to newTerritoryCount
            ))

            val isGameOver = finishIfNeeded(roomObj, allPlayersList, updatedBoardMap, poolTiles.size, transaction, roomRef, pendingPortalUid)
            
            if (!isGameOver) {
                if (pendingPortalUid != null) {
                    transaction.update(roomRef, mapOf(
                        "pendingPortalUid" to pendingPortalUid,
                        "lastMessage" to "¡$uid cayó en un portal!"
                    ))
                } else {
                    val (nextTurnUid, newRound) = GameEngine.calculateNextTurn(uid, turnOrder, currentRound)
                    transaction.update(roomRef, mapOf(
                        "currentTurnUid" to nextTurnUid,
                        "round" to newRound,
                        "lastMessage" to "Ficha colocada por $uid"
                    ))
                }
            }
        }.await()
    }

    suspend fun drawTile(roomId: String, uid: String) {
        val roomRef = rooms.document(roomId)
        val playerRef = roomRef.collection("players").document(uid)
        val poolRef = roomRef.collection("pool").document("state")
        val boardColl = roomRef.collection("board")

        db.runTransaction { transaction ->
            val roomSnap = transaction.get(roomRef)
            if (roomSnap.getString("status") != "PLAYING") throw IllegalStateException("La partida no está en curso")
            if (roomSnap.getString("currentTurnUid") != uid) throw IllegalStateException("No es tu turno")

            val poolSnap = transaction.get(poolRef)
            val playerSnap = transaction.get(playerRef)
            
            val boardSnaps = mutableListOf<DocumentSnapshot>()
            for (r in 0..7) for (c in 0..7) boardSnaps.add(transaction.get(boardColl.document("${r}_${c}")))

            val boardMap = boardSnaps.mapNotNull { it.toObject(Cell::class.java) }.associateBy { Pair(it.row, it.col) }.toMutableMap()
            for (r in 0..7) {
                for (c in 0..7) {
                    if (!boardMap.containsKey(Pair(r, c))) {
                        boardMap[Pair(r, c)] = Cell(id = "${r}_${c}", row = r, col = c)
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            val rawHand = playerSnap.get("hand") as? List<Map<String, Long>> ?: emptyList()
            val currentHand = rawHand.map { Tile(left = (it["left"] ?: 0L).toInt(), right = (it["right"] ?: 0L).toInt()) }

            if (MoveValidator.hasAnyValidMove(boardMap, currentHand)) {
                throw IllegalStateException("Tienes al menos un movimiento válido, no puedes robar")
            }

            @Suppress("UNCHECKED_CAST")
            val rawTiles = poolSnap.get("tiles") as? List<Map<String, Long>> ?: emptyList()
            if (rawTiles.isEmpty()) throw IllegalStateException("El pozo está vacío")

            val poolTiles = rawTiles.map { Tile(left = (it["left"] ?: 0L).toInt(), right = (it["right"] ?: 0L).toInt()) }.toMutableList()
            val drawnTile = poolTiles.removeAt(0)
            val updatedHand = currentHand + drawnTile

            transaction.set(poolRef, mapOf("tiles" to poolTiles))
            transaction.update(playerRef, mapOf("hand" to updatedHand, "handCount" to updatedHand.size))
            transaction.update(roomRef, mapOf("lastMessage" to "$uid robó una ficha"))
        }.await()
    }

    suspend fun passTurn(roomId: String, uid: String) {
        val roomRef = rooms.document(roomId)
        val playerRef = roomRef.collection("players").document(uid)
        val poolRef = roomRef.collection("pool").document("state")
        val boardColl = roomRef.collection("board")
        val playersColl = roomRef.collection("players")

        db.runTransaction { transaction ->
            val roomSnap = transaction.get(roomRef)
            if (roomSnap.getString("status") != "PLAYING") throw IllegalStateException("La partida no está en curso")
            if (roomSnap.getString("currentTurnUid") != uid) throw IllegalStateException("No es tu turno")

            val playerSnap = transaction.get(playerRef)
            val poolSnap = transaction.get(poolRef)

            @Suppress("UNCHECKED_CAST")
            val turnOrder = (roomSnap.get("turnOrder") as? List<String> ?: emptyList())
            val allPlayerSnaps = turnOrder.map { transaction.get(playersColl.document(it)) }
            val boardSnaps = mutableListOf<DocumentSnapshot>()
            for (r in 0..7) for (c in 0..7) boardSnaps.add(transaction.get(boardColl.document("${r}_${c}")))

            val boardMap = boardSnaps.mapNotNull { it.toObject(Cell::class.java) }.associateBy { Pair(it.row, it.col) }.toMutableMap()
            for (r in 0..7) {
                for (c in 0..7) {
                    if (!boardMap.containsKey(Pair(r, c))) {
                        boardMap[Pair(r, c)] = Cell(id = "${r}_${c}", row = r, col = c)
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            val rawHand = playerSnap.get("hand") as? List<Map<String, Long>> ?: emptyList()
            val currentHand = rawHand.map { Tile(left = (it["left"] ?: 0L).toInt(), right = (it["right"] ?: 0L).toInt()) }

            @Suppress("UNCHECKED_CAST")
            val rawPool = poolSnap.get("tiles") as? List<Any> ?: emptyList()

            if (MoveValidator.hasAnyValidMove(boardMap, currentHand)) {
                throw IllegalStateException("Tienes movimientos válidos, no puedes pasar el turno")
            }
            if (rawPool.isNotEmpty()) {
                throw IllegalStateException("Aún hay fichas en el pozo, debes robar antes de pasar")
            }

            val currentRound = (roomSnap.getLong("round") ?: 1L).toInt()
            val roomObj = Room(
                id = roomId, turnOrder = turnOrder, round = currentRound, maxRounds = (roomSnap.getLong("maxRounds") ?: 30L).toInt()
            )
            val allPlayersList = allPlayerSnaps.mapNotNull { it.toObject(Player::class.java) }

            val isGameOver = finishIfNeeded(roomObj, allPlayersList, boardMap, rawPool.size, transaction, roomRef)
            
            if (!isGameOver) {
                val (nextTurnUid, newRound) = GameEngine.calculateNextTurn(uid, turnOrder, currentRound)
                transaction.update(roomRef, mapOf(
                    "currentTurnUid" to nextTurnUid,
                    "round" to newRound,
                    "lastMessage" to "$uid pasó el turno"
                ))
            }
        }.await()
    }

    suspend fun resolvePortal(roomId: String, uid: String, newMove: Move?) {
        val roomRef = rooms.document(roomId)
        val boardColl = roomRef.collection("board")
        val playersColl = roomRef.collection("players")
        val playerRef = playersColl.document(uid)
        val poolRef = roomRef.collection("pool").document("state")

        db.runTransaction { transaction ->
            val roomSnap = transaction.get(roomRef)
            if (roomSnap.getString("status") != "PLAYING") throw IllegalStateException("La partida no está en curso")
            if (roomSnap.getString("pendingPortalUid") != uid) throw IllegalStateException("No tienes portal pendiente")
            
            @Suppress("UNCHECKED_CAST")
            val turnOrder = (roomSnap.get("turnOrder") as? List<String> ?: emptyList())
            val currentRound = (roomSnap.getLong("round") ?: 1L).toInt()

            if (newMove == null) {
                // Quedarse
                val (nextTurnUid, newRound) = GameEngine.calculateNextTurn(uid, turnOrder, currentRound)
                transaction.update(roomRef, mapOf(
                    "pendingPortalUid" to null,
                    "currentTurnUid" to nextTurnUid,
                    "round" to newRound,
                    "lastMessage" to "$uid decidió quedarse en el portal"
                ))
            } else {
                // Reubicar
                val poolSnap = transaction.get(poolRef)
                @Suppress("UNCHECKED_CAST")
                val poolTiles = poolSnap.get("tiles") as? List<Any> ?: emptyList()
                
                val allPlayerSnaps = turnOrder.map { transaction.get(playersColl.document(it)) }
                val boardSnaps = mutableListOf<DocumentSnapshot>()
                for (r in 0..7) for (c in 0..7) boardSnaps.add(transaction.get(boardColl.document("${r}_${c}")))
                
                val boardMap = boardSnaps.mapNotNull { it.toObject(Cell::class.java) }.associateBy { Pair(it.row, it.col) }.toMutableMap()
                for (r in 0..7) {
                    for (c in 0..7) {
                        if (!boardMap.containsKey(Pair(r, c))) {
                            boardMap[Pair(r, c)] = Cell(id = "${r}_${c}", row = r, col = c)
                        }
                    }
                }
                
                val currentCells = boardMap.values.filter { it.placedBy == uid && it.value != null }
                if (currentCells.isEmpty()) throw IllegalStateException("No se encontraron tus casillas en el tablero")

                val cell1 = currentCells.first { it.type.uppercase() == "PORTAL" || boardMap[Pair(it.pairRow?:-1, it.pairCol?:-1)]?.type?.uppercase() == "PORTAL" }
                val cell2 = boardMap[Pair(cell1.pairRow?:-1, cell1.pairCol?:-1)]!!
                
                // Extraer la ficha ORIGINAL realemente jugada del tablero
                val isFlippedOnBoard = cell1.value != newMove.tile.left
                val realTile = if (isFlippedOnBoard) Tile(cell2.value ?: 0, cell1.value ?: 0) else Tile(cell1.value ?: 0, cell2.value ?: 0)
                
                // Asegurarse de que el newMove tiene la ficha correcta del tablero original
                val adjustedNewMove = newMove.copy(tile = realTile)

                val playerSnap = transaction.get(playerRef)
                val currentScore = (playerSnap.getLong("score") ?: 0L).toInt()
                
                val ptsOld1 = ScoreCalculator.calculateCellPoints(cell1.type, cell1.conquestCount - 1)
                val ptsOld2 = ScoreCalculator.calculateCellPoints(cell2.type, cell2.conquestCount - 1)
                val baseScore = currentScore - (ptsOld1 + ptsOld2)
                
                val (newBoardMap, newPts) = GameEngine.applyPortalRelocation(boardMap, cell1.row, cell1.col, cell2.row, cell2.col, adjustedNewMove, uid)
                val finalScore = (baseScore + newPts).coerceAtLeast(0)
                val finalTerritories = newBoardMap.values.count { it.ownerId == uid }

                transaction.set(boardColl.document("${cell1.row}_${cell1.col}"), newBoardMap[Pair(cell1.row, cell1.col)]!!)
                transaction.set(boardColl.document("${cell2.row}_${cell2.col}"), newBoardMap[Pair(cell2.row, cell2.col)]!!)
                transaction.set(boardColl.document("${adjustedNewMove.row}_${adjustedNewMove.col}"), newBoardMap[Pair(adjustedNewMove.row, adjustedNewMove.col)]!!)
                transaction.set(boardColl.document("${adjustedNewMove.row2}_${adjustedNewMove.col2}"), newBoardMap[Pair(adjustedNewMove.row2, adjustedNewMove.col2)]!!)

                transaction.update(playerRef, mapOf(
                    "score" to finalScore,
                    "territories" to finalTerritories
                ))

                val allPlayersList = allPlayerSnaps.mapNotNull { it.toObject(Player::class.java) }.map { p ->
                    if (p.uid == uid) p.copy(score = finalScore, territories = finalTerritories) else p
                }

                val roomObj = Room(id = roomId, turnOrder = turnOrder, round = currentRound, maxRounds = (roomSnap.getLong("maxRounds") ?: 30L).toInt())
                val isGameOver = finishIfNeeded(roomObj, allPlayersList, newBoardMap, poolTiles.size, transaction, roomRef, null)

                if (!isGameOver) {
                    val (nextTurnUid, newRound) = GameEngine.calculateNextTurn(uid, turnOrder, currentRound)
                    transaction.update(roomRef, mapOf(
                        "pendingPortalUid" to null,
                        "currentTurnUid" to nextTurnUid,
                        "round" to newRound,
                        "lastMessage" to "$uid se reubicó por portal"
                    ))
                }
            }
        }.await()
    }

    suspend fun abandon(roomId: String, uid: String) {
        val roomRef = rooms.document(roomId)
        val playerRef = roomRef.collection("players").document(uid)

        db.runTransaction { transaction ->
            val roomSnap = transaction.get(roomRef)
            @Suppress("UNCHECKED_CAST")
            val turnOrder = (roomSnap.get("turnOrder") as? List<String> ?: emptyList()).toMutableList()

            turnOrder.remove(uid)
            transaction.update(playerRef, mapOf("connected" to false))

            if (turnOrder.size <= 1) {
                val winnerId = turnOrder.firstOrNull()
                transaction.update(roomRef, mapOf(
                    "turnOrder" to turnOrder,
                    "status" to "FINISHED",
                    "winnerId" to winnerId,
                    "lastMessage" to "Partida terminada (abandono masivo)"
                ))
            } else {
                val currentTurnUid = roomSnap.getString("currentTurnUid") ?: ""
                val nextTurnUid = if (currentTurnUid == uid) {
                    val idx = (turnOrder.indexOf(uid) + 1) % turnOrder.size
                    turnOrder[idx]
                } else currentTurnUid

                transaction.update(roomRef, mapOf(
                    "turnOrder" to turnOrder,
                    "currentTurnUid" to nextTurnUid,
                    "lastMessage" to "$uid abandonó la partida"
                ))
            }
        }.await()
    }

    suspend fun restart(roomId: String) {
        val roomRef = rooms.document(roomId)
        val playersColl = roomRef.collection("players")
        val boardColl = roomRef.collection("board")

        val playersSnaps = playersColl.get().await()
        val playerDocs = playersSnaps.documents

        val tiles = mutableListOf<Tile>()
        for (i in 0..6) for (j in i..6) tiles.add(Tile(i, j))
        tiles.shuffle()

        val specials = listOf("BONUS", "TREASURE", "TRAP", "FORTRESS", "PORTAL")
        val board = mutableListOf<Cell>()
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                val type = if ((0..99).random() < 25) specials.random() else "NORMAL"
                board.add(Cell(id = "${r}_${c}", row = r, col = c, type = type, value = null, conquestCount = 0, ownerId = null, pairRow = null, pairCol = null, placedBy = null))
            }
        }

        val firstTurn = playerDocs.firstOrNull()?.id ?: ""

        db.runBatch { batch ->
            playerDocs.forEach { p ->
                val hand = List(7) { tiles.removeAt(0) }
                batch.update(p.reference, mapOf(
                    "hand" to hand,
                    "handCount" to 7,
                    "score" to 0,
                    "territories" to 0
                ))
            }
            batch.set(roomRef.collection("pool").document("state"), mapOf("tiles" to tiles))
            board.forEach { cell -> batch.set(boardColl.document(cell.id), cell) }
            batch.update(roomRef, mapOf(
                "status" to "PLAYING",
                "currentTurnUid" to firstTurn,
                "round" to 1,
                "winnerId" to null,
                "pendingPortalUid" to null,
                "lastMessage" to "Partida reiniciada"
            ))
        }.await()
    }

    suspend fun executeBotTurn(roomId: String, botUid: String) {
        var attempts = 0
        while (attempts < 10) {
            attempts++
            val roomRef = rooms.document(roomId)
            val roomSnap = roomRef.get().await()

            val currentTurnUid = roomSnap.getString("currentTurnUid") ?: ""
            val status = roomSnap.getString("status") ?: ""
            val pendingPortalUid = roomSnap.getString("pendingPortalUid")

            if (status != "PLAYING") break
            
            if (pendingPortalUid == botUid) {
                resolvePortal(roomId, botUid, null) // Bot siempre se queda
                break
            }

            if (currentTurnUid != botUid) break

            val botPlayerRef = roomRef.collection("players").document(botUid)
            val botSnap = botPlayerRef.get().await()

            @Suppress("UNCHECKED_CAST")
            val rawHand = botSnap.get("hand") as? List<Map<String, Long>> ?: emptyList()
            val botHand = rawHand.map { Tile(left = (it["left"] ?: 0L).toInt(), right = (it["right"] ?: 0L).toInt()) }

            val boardSnaps = roomRef.collection("board").get().await()
            val boardMap = boardSnaps.toObjects(Cell::class.java).associateBy { Pair(it.row, it.col) }.toMutableMap()
            for (r in 0..7) {
                for (c in 0..7) {
                    if (!boardMap.containsKey(Pair(r, c))) {
                        boardMap[Pair(r, c)] = Cell(id = "${r}_${c}", row = r, col = c)
                    }
                }
            }

            val allValidMoves = botHand.flatMap { tile -> MoveValidator.getValidMoves(boardMap, tile) }

            if (allValidMoves.isNotEmpty()) {
                val bestMove = allValidMoves.maxByOrNull { move ->
                    val cell1 = boardMap[Pair(move.row, move.col)] ?: Cell()
                    val cell2 = boardMap[Pair(move.row2, move.col2)] ?: Cell()
                    ScoreCalculator.calculateMovePoints(cell1.type, cell1.conquestCount, cell2.type, cell2.conquestCount)
                } ?: allValidMoves.first()

                playMove(roomId, botUid, bestMove)
                break
            } else {
                val poolSnap = roomRef.collection("pool").document("state").get().await()
                @Suppress("UNCHECKED_CAST")
                val rawPool = poolSnap.get("tiles") as? List<Any> ?: emptyList()

                if (rawPool.isNotEmpty()) {
                    drawTile(roomId, botUid)
                } else {
                    passTurn(roomId, botUid)
                    break
                }
            }
        }
    }
}
