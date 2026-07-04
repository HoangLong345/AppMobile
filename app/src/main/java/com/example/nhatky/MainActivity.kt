package com.example.nhatky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nhatky.ui.screens.AddEditDiaryScreen
import com.example.nhatky.ui.screens.DiaryListScreen
import com.example.nhatky.ui.screens.LoginScreen
import com.example.nhatky.ui.screens.RegisterScreen
import com.example.nhatky.ui.theme.Theme
import com.example.nhatky.viewmodel.AuthViewModel
import com.example.nhatky.viewmodel.DiaryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Theme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val diaryViewModel: DiaryViewModel = hiltViewModel()
    val user by authViewModel.currentUser.collectAsState()

    val startDestination = if (user == null) "login" else "diary_list"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("diary_list") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("diary_list") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("diary_list") {
            DiaryListScreen(
                authViewModel = authViewModel,
                diaryViewModel = diaryViewModel,
                onAddDiary = {
                    navController.navigate("add_diary")
                },
                onEditDiary = { diaryId ->
                    navController.navigate("edit_diary/$diaryId")
                }
            )
        }
        
        composable("add_diary") {
            AddEditDiaryScreen(
                diaryId = null,
                authViewModel = authViewModel,
                diaryViewModel = diaryViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = "edit_diary/{diaryId}",
            arguments = listOf(navArgument("diaryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val diaryId = backStackEntry.arguments?.getString("diaryId")
            AddEditDiaryScreen(
                diaryId = diaryId,
                authViewModel = authViewModel,
                diaryViewModel = diaryViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
