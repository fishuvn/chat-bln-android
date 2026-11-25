package nhom3.chatbln.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import nhom3.chatbln.android.ui.screens.ChatRoomScreen
import nhom3.chatbln.android.ui.screens.LoginScreen
import nhom3.chatbln.android.ui.screens.MainScaffoldScreen
import nhom3.chatbln.android.ui.screens.RegisterScreen
import nhom3.chatbln.android.ui.theme.ChatBlnTheme
import nhom3.chatbln.android.ui.viewmodel.AuthViewModel
import nhom3.chatbln.android.ui.viewmodel.MainChatViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as ChatBlnApp).container

        setContent {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.factory(appContainer)
            )
            val mainChatViewModel: MainChatViewModel = viewModel(
                factory = MainChatViewModel.factory(appContainer)
            )

            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkEffective = authState.darkModePreference ?: systemDark

            ChatBlnTheme(darkTheme = isDarkEffective) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (authState.isBootstrapping) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ChatBlnRootNavHost(
                            isAuthenticated = authState.isAuthenticated,
                            isDarkEffective = isDarkEffective,
                            authViewModel = authViewModel,
                            mainChatViewModel = mainChatViewModel
                        )
                    }
                }
            }
        }
    }
}

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val CHAT_ROOM = "chat/{conversationId}"

    fun chatRoom(conversationId: Long): String = "chat/$conversationId"
}

@Composable
private fun ChatBlnRootNavHost(
    isAuthenticated: Boolean,
    isDarkEffective: Boolean,
    authViewModel: AuthViewModel,
    mainChatViewModel: MainChatViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isAuthenticated) {
        val currentRoute = navController.currentDestination?.route
        if (isAuthenticated) {
            if (currentRoute == Routes.LOGIN || currentRoute == Routes.REGISTER) {
                navController.navigate(Routes.MAIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            if (currentRoute != null && currentRoute != Routes.LOGIN && currentRoute != Routes.REGISTER) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Routes.MAIN else Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                serverBaseUrl = authState.serverBaseUrl,
                onLogin = authViewModel::login,
                onNavigateToRegister = {
                    authViewModel.clearError()
                    navController.navigate(Routes.REGISTER)
                },
                onUpdateServerUrl = authViewModel::updateServerUrl,
                onClearError = authViewModel::clearError
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                serverBaseUrl = authState.serverBaseUrl,
                onRegister = authViewModel::register,
                onNavigateBack = {
                    authViewModel.clearError()
                    navController.popBackStack()
                },
                onUpdateServerUrl = authViewModel::updateServerUrl,
                onClearError = authViewModel::clearError
            )
        }

        composable(Routes.MAIN) {
            MainScaffoldScreen(
                viewModel = mainChatViewModel,
                isDarkThemeEffective = isDarkEffective,
                onOpenConversation = { conversationId ->
                    navController.navigate(Routes.chatRoom(conversationId))
                },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CHAT_ROOM,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getLong("conversationId") ?: return@composable
            ChatRoomScreen(
                conversationId = convId,
                viewModel = mainChatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConversation = { nextId ->
                    navController.navigate(Routes.chatRoom(nextId)) {
                        popUpTo(Routes.MAIN)
                    }
                }
            )
        }
    }
}
