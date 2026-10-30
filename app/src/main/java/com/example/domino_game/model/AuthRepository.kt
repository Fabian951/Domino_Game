package com.example.domino_game.model

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore
) {

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    suspend fun register(name: String, email: String, password: String, avatarEmoji: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid

        db.collection("users").document(uid).set(
            mapOf(
                "name" to name,
                "email" to email,
                "avatar" to avatarEmoji,
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
                    "avatar" to "",
                    "gamesPlayed" to 0,
                    "gamesWon" to 0
                )
            ).await()
        }
    }

    suspend fun updateUserEmoji(uid: String, emoji: String) {
        if (uid.isNotBlank()) {
            db.collection("users").document(uid).update("avatar", emoji).await()
        }
    }

    suspend fun getUserEmoji(uid: String): String? {
        if (uid.isBlank()) return null
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("avatar")
    }

    // FUNCIÓN DE BORRADO OPTIMIZADA Y USANDO LAS INSTANCIAS DE TU CLASE
    suspend fun deleteUserAccountPermanently() {
        val user = auth.currentUser ?: throw Exception("No hay ningún usuario con sesión activa")
        val uid = user.uid

        try {
            // 1. Eliminamos el documento del usuario en Firestore utilizando la instancia 'db'
            db.collection("users").document(uid).delete().await()

            // 2. Eliminamos el usuario de Firebase Authentication utilizando la instancia 'auth'
            user.delete().await()

        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            // Capturamos de forma precisa la excepción de seguridad de Firebase y la traducimos elegantemente
            throw Exception("Por seguridad, al ser una acción crítica, cierra sesión e ingresa nuevamente para poder eliminar tu cuenta.")
        } catch (e: Exception) {
            throw Exception("No se pudo eliminar la cuenta por completo: ${e.localizedMessage}")
        }
    }
}
