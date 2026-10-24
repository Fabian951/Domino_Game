package com.example.domino_game.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domino_game.viewmodel.AuthUiState
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    onGoogleLogin: (idToken: String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

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
            Spacer(Modifier.height(12.dp))
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

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
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
                    onRegister(name, email, password)
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
                    val credentialManager = androidx.credentials.CredentialManager.create(context)

                    // IMPORTANTE: Pon aquí tu cliente web de tu archivo google-services.json
                    val webClientId = "423565031620-i82o1nd8hdpb8ltbgibmkdd2fis58rrf.apps.googleusercontent.com"

                    val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    coroutineScope.launch {
                        try {
                            val result = credentialManager.getCredential(context = context, request = request)
                            val credential = result.credential
                            val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)

                            onGoogleLogin(googleIdTokenCredential.idToken)

                        } catch (e: Exception) {
                            Toast.makeText(context, "Operación cancelada", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF2F2F2),
                    contentColor = Color.Black
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Continuar con Google",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
