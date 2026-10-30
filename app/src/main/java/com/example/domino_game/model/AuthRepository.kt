package com.example.domino_game.model

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    // CORRECCIÓN: Ahora acepta el cuarto parámetro avatarEmoji
    suspend fun register(name: String, email: String, password: String, avatarEmoji: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid

        // Guardamos el emoji elegido dentro del documento del usuario en Firestore
        db.collection("users").document(uid).set(
            mapOf(
                "name" to name,
                "email" to email,
                "avatar" to avatarEmoji, // <--- Guardado en base de datos
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

    // Función auxiliar opcional por si necesitas recuperar el emoji en el juego más adelante
    suspend fun getUserAvatar(): String {
        val uid = currentUserUid ?: return "🦄"
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("avatar") ?: "🦄"
    }

    suspend fun loginWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid ?: return

        val userDoc = db.collection("users").document(uid).get().await()

        if (!userDoc.exists()) {
            val name = result.user?.displayName ?: "Jugador de Google"
            val email = result.user?.email ?: ""

            db.collection("users").document(uid).set(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "avatar" to "😎", // <--- Le asignamos un emoji por defecto a los registros de Google
                    "gamesPlayed" to 0,
                    "gamesWon" to 0
                )
            ).await()
        }
    }
}
