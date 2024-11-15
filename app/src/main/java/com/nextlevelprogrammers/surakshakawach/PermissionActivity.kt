package com.nextlevelprogrammers.surakshakawach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme
import kotlinx.coroutines.launch

class PermissionActivity : ComponentActivity() {

    // Device permissions launcher
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.all { it.value }
            if (allPermissionsGranted) {
                navigateToHome()
            } else {
                val permanentlyDenied = permissions.any { !shouldShowRequestPermissionRationale(it.key) && !it.value }
                if (permanentlyDenied) {
                    showSettingsDialog()
                } else {
                    Toast.makeText(this, "All permissions are required.", Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SurakshaKawachTheme {
                PermissionScreen()
            }
        }
    }

    @Composable
    fun PermissionScreen() {
        var showDialog by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                val allPermissionsGranted = checkDevicePermissions()
                if (!allPermissionsGranted) {
                    showDialog = true
                } else {
                    navigateToHome()
                }
            }
        }

        if (showDialog) {
            PermissionDialog(
                onAllow = {
                    requestAllPermissions()
                    showDialog = false
                },
                onCancel = {
                    Toast.makeText(this@PermissionActivity, "Permissions are required", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Check if all required device permissions are granted
    private fun checkDevicePermissions(): Boolean {
        val permissionsToCheck = getRequiredPermissions()
        return permissionsToCheck.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request all necessary permissions, including POST_NOTIFICATIONS for Android 13+
    private fun requestAllPermissions() {
        val permissions = getRequiredPermissions()
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        // Add POST_NOTIFICATIONS permission if on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Add WRITE_EXTERNAL_STORAGE for Android versions <= 32
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        return permissions
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permissions Required")
            .setMessage("Some permissions are permanently denied. Please enable them in the app settings for full functionality.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    @Composable
    fun PermissionDialog(onAllow: () -> Unit, onCancel: () -> Unit) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = {
                Text(text = "Permissions Required")
            },
            text = {
                Column {
                    Text("We need access to the following:")
                    Text("- Location: For tracking your position.")
                    Text("- Contacts: To manage emergency contacts.")
                    Text("- Camera: For capturing images.")
                    Text("- Microphone: For wake word detection.")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Text("- Notifications: For alerting you about critical events.")
                    }
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                        Text("- Storage: To save critical files.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = onAllow) {
                    Text("Allow")
                }
            },
            dismissButton = {
                Button(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        )
    }
}