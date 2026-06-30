package com.example.nhatky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    // Determine starting destination
    val startDestination = if (user == null) "login" else "diary_list"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("diary_list") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            ) {
                navController.navigate("register")
            }
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("diary_list") {
                        popUpTo("login") { inclusive = true }
                    }
                },
            ) {
                navController.popBackStack()
            }
        }
        composable("diary_list") {
            DiaryListScreen(
                authViewModel = authViewModel,
                diaryViewModel = diaryViewModel,
                onAddDiary = {
                    navController.navigate("add_edit_diary")
                },
                onEditDiary = { diaryId ->
                    navController.navigate("add_edit_diary?diaryId=$diaryId")
                }
            )
        }
        composable(
            route = "add_edit_diary?diaryId={diaryId}",
            arguments = listOf(
                navArgument("diaryId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
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
