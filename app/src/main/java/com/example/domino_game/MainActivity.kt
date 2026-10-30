package com.example.domino_game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.domino_game.ui.theme.Domino_GameTheme
import com.example.domino_game.view.AuthScreen
import com.example.domino_game.view.AvatarScreen
import com.example.domino_game.view.GameScreen
import com.example.domino_game.view.LobbyScreen
import com.example.domino_game.viewmodel.AuthViewModel
import com.example.domino_game.viewmodel.LobbyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Domino_GameTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                    // Determinamos el destino inicial basándonos en si requiere avatar o va al lobby directo
                    val startDestination = if (authState.loggedIn) {
                        if (authState.needsAvatarSelection) "avatar_selection" else "lobby"
                    } else {
                        "auth"
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("auth") {
                            // Escucha limpia que evalúa la bandera para decidir el destino
                            LaunchedEffect(authState.loggedIn, authState.needsAvatarSelection) {
                                if (authState.loggedIn) {
                                    val destination = if (authState.needsAvatarSelection) "avatar_selection" else "lobby"
                                    navController.navigate(destination) {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            }

                            AuthScreen(
                                state = authState,
                                onLogin = { email, password -> authViewModel.login(email, password) },
                                onRegister = { name, email, password, emoji ->
                                    authViewModel.register(name, email, password, emoji)
                                },
                                onGoogleLogin = { token -> authViewModel.loginWithGoogle(token) }
                            )
                        }

                        // NUEVA RUTA INTEGRADA: Pantalla intermedia de selección de Avatar
                        composable("avatar_selection") {
                            AvatarScreen(
                                onAvatarConfirmed = { selectedEmoji ->
                                    authViewModel.updateAvatarEmoji(selectedEmoji)
                                    navController.navigate("lobby") {
                                        popUpTo("avatar_selection") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("lobby") {
                            val lobbyViewModel: LobbyViewModel = viewModel()
                            LobbyScreen(
                                viewModel = lobbyViewModel,
                                onGameStart = { roomId ->
                                    navController.navigate("game/$roomId")
                                },
                                onLogout = {
                                    authViewModel.logout()
                                    navController.navigate("auth") {
                                        popUpTo("lobby") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("game/{roomId}") { backStackEntry ->
                            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                            GameScreen(
                                roomId = roomId,
                                onLeaveGame = {
                                    navController.navigate("lobby") {
                                        popUpTo("lobby") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
