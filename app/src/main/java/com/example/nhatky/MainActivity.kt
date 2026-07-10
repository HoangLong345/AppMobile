package com.example.nhatky

import android.net.Uri
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
import com.example.nhatky.ui.screens.CameraScreen
import com.example.nhatky.ui.screens.DiaryListScreen
import com.example.nhatky.ui.screens.LoginScreen
import com.example.nhatky.ui.screens.NoteWallScreen
import com.example.nhatky.ui.screens.PhotoEditScreen
import com.example.nhatky.ui.screens.RegisterScreen
import com.example.nhatky.ui.screens.SettingsScreen
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
                onOpenWall = { dateKey ->
                    navController.navigate("note_wall/$dateKey")
                },
                onTakePhoto = {
                    navController.navigate("camera_screen")
                },
                onPickPhoto = { encodedUri ->
                    navController.navigate("photo_edit_screen/$encodedUri")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                authViewModel = authViewModel,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // SỬA Ở ĐÂY: Xử lý logic chuyển màn hình khi bấm nút Edit
        composable(
            route = "note_wall/{dateKey}",
            arguments = listOf(navArgument("dateKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateKey = backStackEntry.arguments?.getString("dateKey") ?: ""
            NoteWallScreen(
                dateKey = dateKey,
                diaryViewModel = diaryViewModel,
                onEditNote = { note ->
                    // Kiểm tra nếu nhật ký có ảnh -> Chuyển sang chỉnh sửa ảnh
                    if (note.mediaUrls.isNotEmpty()) {
                        val encodedUri = Uri.encode(note.mediaUrls.first())
                        navController.navigate("photo_edit_screen/$encodedUri?diaryId=${note.id}")
                    } else {
                        // Nếu là nhật ký chữ -> Chuyển sang AddEditDiaryScreen
                        navController.navigate("edit_diary/${note.id}")
                    }
                },
                onBack = { navController.popBackStack() }
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

        composable("camera_screen") {
            CameraScreen(
                onImageCaptured = { uri ->
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate("photo_edit_screen/$encodedUri") {
                        popUpTo("camera_screen") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // SỬA Ở ĐÂY: Bổ sung Optional param diaryId cho màn hình sửa ảnh
        composable(
            route = "photo_edit_screen/{imageUri}?diaryId={diaryId}",
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("diaryId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val imageUriStr = backStackEntry.arguments?.getString("imageUri") ?: ""
            val diaryId = backStackEntry.arguments?.getString("diaryId")
            val imageUri = Uri.parse(Uri.decode(imageUriStr))
            PhotoEditScreen(
                imageUri = imageUri,
                diaryId = diaryId,
                authViewModel = authViewModel,
                diaryViewModel = diaryViewModel,
                onSave = {
                    navController.navigate("diary_list") {
                        popUpTo("diary_list") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}