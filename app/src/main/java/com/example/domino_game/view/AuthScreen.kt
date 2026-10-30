package com.example.domino_game.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domino_game.viewmodel.AuthUiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String, avatarEmoji: String) -> Unit,
    onGoogleLogin: (idToken: String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // NUEVO ESTADO: Controla visualmente si se oculta o muestra el texto
    var isPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val listaEmojis = com.example.domino_game.model.AvatarAssets.listaEmojis

    var selectedEmoji by remember { mutableStateOf("🦄") }

    val webClientId = "423565031620-i82o1nd8hdpb8ltbgibmkdd2fis58rrf.apps.googleusercontent.com"

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    onGoogleLogin(idToken)
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Error Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))

        if (isRegisterMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Selecciona tu avatar emoji:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listaEmojis) { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (selectedEmoji == emoji) Color(0xFFE0E0E0) else Color(0xFFF5F5F5))
                            .border(
                                width = if (selectedEmoji == emoji) 2.5.dp else 1.dp,
                                color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = CircleShape
                            )
                            .clickable { selectedEmoji = emoji }
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 28.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { input ->
                if (input.contains("Correo:") && input.contains("Clave:")) {
                    try {
                        val parteCorreo = input.substringAfter("Correo:").substringBefore("|").trim()
                        val parteClave = input.substringAfter("Clave:").trim()
                        email = parteCorreo
                        password = parteClave
                    } catch (e: Exception) {
                        email = input
                    }
                } else {
                    email = input
                }
            },
            label = { Text("Correo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // MODIFICADO EXCLUSIVAMENTE: Añadido el cambio de visualización y el icono interactivo
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val textoIcono = if (isPasswordVisible) "🙈" else "👁️"
                Text(
                    text = textoIcono,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable { isPasswordVisible = !isPasswordVisible }
                        .padding(end = 12.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (isRegisterMode) {
                    onRegister(name, email, password, selectedEmoji)
                } else {
                    onLogin(email, password)
                }

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    val datosAGuardar = "Correo: $email | Clave: $password"
                    val clip = ClipData.newPlainText("Credenciales Domino", datosAGuardar)
                    clipboardManager.setPrimaryClip(clip)

                    Toast.makeText(
                        context,
                        "Datos de acceso guardados en el portapapeles",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(if (isRegisterMode) "Registrarme" else "Entrar")
            }
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate")
        }

        if (!isRegisterMode) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Limpiamos la caché de sesión de Google antes de abrir la ventana flotante
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                enabled = !state.loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sesión con Google")
            }
        }
    }
}
