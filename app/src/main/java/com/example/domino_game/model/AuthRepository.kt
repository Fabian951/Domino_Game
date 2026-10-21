package com.example.domino_game.model

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) {

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    suspend fun register(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid

        // Esto crea la colección "users" automáticamente
        db.collection("users").document(uid).set(
            mapOf(
                "name" to name,
                "email" to email,
                "gamesPlayed" to 0,
                "gamesWon" to 0
            )
        ).await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getUserName(): String {
        val uid = currentUserUid ?: return "Jugador"
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("name") ?: "Jugador"
    }
}
