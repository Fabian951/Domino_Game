package com.example.domino_game.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domino_game.viewmodel.AuthUiState

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Obtenemos el contexto y el gestor de portapapeles nativo del celular
    val context = LocalContext.current
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
                // 1. Enviamos los flujos a la lógica de Firebase
                if (isRegisterMode) {
                    onRegister(name, email, password)
                } else {
                    onLogin(email, password)
                }

                // 2. Guardamos automáticamente en el portapapeles del dispositivo si los campos no están vacíos
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    val datosAGuardar = "Correo: $email | Clave: $password"
                    val clip = ClipData.newPlainText("Credenciales Domino", datosAGuardar)
                    clipboardManager.setPrimaryClip(clip)

                    // 3. Pequeño mensaje emergente de confirmación en el celular
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
    }
}
