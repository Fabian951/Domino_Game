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

                    NavHost(
                        navController = navController,
                        startDestination = if (authState.loggedIn) "lobby" else "auth"
                    ) {
                        composable("auth") {
                            LaunchedEffect(authState.loggedIn) {
                                if (authState.loggedIn) {
                                    navController.navigate("lobby") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            }
                            AuthScreen(
                                state = authState,
                                onLogin = authViewModel::login,
                                onRegister = authViewModel::register,
                                onGoogleLogin = { token -> authViewModel.loginWithGoogle(token) } // <-- AGREGA ESTA LÍNEA AQUÍ
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
