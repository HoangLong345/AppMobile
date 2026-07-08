package com.example.nhatky.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import androidx.compose.runtime.Composable

@Composable
fun rememberDrivePermissionLauncher(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

fun checkAndRequestDrivePermission(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    onHasPermission: () -> Unit
) {
    val account = GoogleSignIn.getLastSignedInAccount(context)
    val driveScope = Scope(DriveScopes.DRIVE_FILE)
    
    if (account != null && GoogleSignIn.hasPermissions(account, driveScope)) {
        onHasPermission()
    } else {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        launcher.launch(client.signInIntent)
    }
}
