package com.example.domino_game.model

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RoomRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) {
    private val rooms get() = db.collection("rooms")

    // ---------- CREAR SALA ----------
    suspend fun createRoom(uid: String, name: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..5).map { chars.random() }.joinToString("")

        val roomRef = rooms.document()   // ID automático

        val room = mapOf(
            "code" to code,
            "hostId" to uid,
            "status" to "WAITING",
            "currentTurnUid" to "",
            "turnOrder" to listOf(uid),
            "round" to 1,
            "maxRounds" to 30,
            "winnerId" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.runBatch { batch ->
            batch.set(roomRef, room)
            batch.set(roomRef.collection("players").document(uid), Player(uid = uid, name = name))
        }.await()

        return roomRef.id
    }

    // ---------- AGREGAR BOT (para pruebas / modo solo) ----------
    suspend fun addBot(roomId: String) {
        val roomRef = rooms.document(roomId)
        val playersRef = roomRef.collection("players")
        val currentPlayers = playersRef.get().await().documents
        if (currentPlayers.size >= 4) throw Exception("La sala está llena (máximo 4 jugadores)")

        val botIndex = currentPlayers.count { it.id.startsWith("bot_") } + 1
        val botUid = "bot_$botIndex"
        val botName = "Bot $botIndex"

        playersRef.document(botUid).set(
            Player(uid = botUid, name = botName, isAi = true)
        ).await()
        roomRef.update("turnOrder", FieldValue.arrayUnion(botUid)).await()
    }

    // ---------- UNIRSE A SALA ----------
    suspend fun joinRoom(code: String, uid: String, name: String): String {
        val query = rooms
            .whereEqualTo("code", code.trim().uppercase())
            .whereEqualTo("status", "WAITING")
            .get().await()

        val roomDoc = query.documents.firstOrNull()
            ?: throw Exception("Sala no encontrada o ya iniciada")

        val roomRef = roomDoc.reference
        val playersRef = roomRef.collection("players")

        val alreadyIn = playersRef.document(uid).get().await().exists()
        if (!alreadyIn) {
            val count = playersRef.get().await().size()
            if (count >= 4) throw Exception("La sala está llena")

            playersRef.document(uid).set(Player(uid = uid, name = name)).await()
            roomRef.update("turnOrder", FieldValue.arrayUnion(uid)).await()
        }
        return roomRef.id
    }

    // ---------- ESCUCHAR EN TIEMPO REAL ----------
    fun observeRoom(roomId: String): Flow<Room?> = callbackFlow {
        val reg = rooms.document(roomId).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(Room::class.java)?.copy(id = snap.id))
        }
        awaitClose { reg.remove() }
    }

    fun observePlayers(roomId: String): Flow<List<Player>> = callbackFlow {
        val reg = rooms.document(roomId).collection("players")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.toObjects(Player::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ---------- INICIAR PARTIDA ----------
    suspend fun startGame(roomId: String) {
        val roomRef = rooms.document(roomId)

        var players = roomRef.collection("players").get().await().documents
        if (players.isEmpty()) throw Exception("No hay jugadores en la sala")

        // Si solo hay 1 jugador, agregar un Bot automáticamente para poder probar/jugar
        if (players.size == 1) {
            val botUid = "bot_1"
            val botName = "Bot 1"
            roomRef.collection("players").document(botUid).set(
                Player(uid = botUid, name = botName, isAi = true)
            ).await()
            roomRef.update("turnOrder", FieldValue.arrayUnion(botUid)).await()
            players = roomRef.collection("players").get().await().documents
        }

        // 28 fichas del dominó, mezcladas
        val tiles = mutableListOf<Tile>()
        for (i in 0..6) for (j in i..6) tiles.add(Tile(i, j))
        tiles.shuffle()

        // Tablero 10x10 con ~25% de casillas especiales (sin value)
        val specials = listOf("BONUS", "TREASURE", "TRAP", "FORTRESS", "PORTAL")
        val board = mutableListOf<Cell>()
        for (r in 0 until 10) {
            for (c in 0 until 10) {
                val type = if ((0..99).random() < 25) specials.random() else "NORMAL"
                board.add(Cell(id = "${r}_${c}", row = r, col = c, type = type, value = null, conquestCount = 0, ownerId = null))
            }
        }

        val firstTurn = players.first().id

        db.runBatch { batch ->
            // 7 fichas a cada jugador
            players.forEach { p ->
                val hand = List(7) { tiles.removeAt(0) }
                batch.update(p.reference, mapOf("hand" to hand, "handCount" to 7, "score" to 0, "territories" to 0))
            }

            // Pozo
            batch.set(roomRef.collection("pool").document("state"), mapOf("tiles" to tiles))

            // Tablero 8x8
            board.forEach { cell ->
                batch.set(roomRef.collection("board").document(cell.id), cell)
            }

            // Activar partida
            batch.update(roomRef, mapOf(
                "status" to "PLAYING",
                "currentTurnUid" to firstTurn,
                "round" to 1,
                "winnerId" to null,
                "pendingPortalUid" to null,
                "lastMessage" to "Partida iniciada"
            ))
        }.await()
    }

    // ---------- SALIR DE LA SALA ----------
    suspend fun leaveRoom(roomId: String, uid: String) {
        val roomRef = rooms.document(roomId)
        roomRef.collection("players").document(uid).delete().await()
        roomRef.update("turnOrder", FieldValue.arrayRemove(uid)).await()

        val remaining = roomRef.collection("players").get().await().documents
        if (remaining.isEmpty()) {
            roomRef.delete().await()
        } else {
            val room = roomRef.get().await().toObject(Room::class.java)
            if (room?.hostId == uid) {
                roomRef.update("hostId", remaining.first().id).await()
            }
        }
    }
}
