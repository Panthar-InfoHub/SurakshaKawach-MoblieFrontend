package com.nextlevelprogrammers.surakshakawach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
                Toast.makeText(this, "All permissions are required.", Toast.LENGTH_LONG).show()
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

    // Check device permissions
    private fun checkDevicePermissions(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val contactPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        return locationPermission == PackageManager.PERMISSION_GRANTED &&
                contactPermission == PackageManager.PERMISSION_GRANTED &&
                cameraPermission == PackageManager.PERMISSION_GRANTED &&
                audioPermission == PackageManager.PERMISSION_GRANTED
    }

    // Request device permissions
    private fun requestAllPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    @Composable
    fun PermissionDialog(onAllow: () -> Unit, onCancel: () -> Unit) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = {
                Text(text = "Permissions Required")
            },
            text = {
                Text("We need access to location, contacts, camera, and audio to provide the best experience.")
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