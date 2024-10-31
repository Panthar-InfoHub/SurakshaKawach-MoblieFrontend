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
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme
import kotlinx.coroutines.launch

class PermissionActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient
    private val requiredHealthPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

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

        // Initialize Health Connect Client with error handling
        try {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Health Connect is not available on this device.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            SurakshaKawachTheme {
                PermissionScreen()
            }
        }
    }

    @Composable
    fun PermissionScreen() {
        var showDialog by remember { mutableStateOf(false) }
        var healthPermissionsGranted by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                // Check both health and device permissions
                healthPermissionsGranted = checkHealthPermissions()
                val allPermissionsGranted = checkPermissions(healthPermissionsGranted)
                if (!allPermissionsGranted) {
                    showDialog = true
                } else {
                    navigateToHome() // If all permissions are granted, navigate to Home
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

    private fun checkPermissions(healthPermissionsGranted: Boolean): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val contactPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        return locationPermission == PackageManager.PERMISSION_GRANTED &&
                contactPermission == PackageManager.PERMISSION_GRANTED &&
                cameraPermission == PackageManager.PERMISSION_GRANTED &&
                audioPermission == PackageManager.PERMISSION_GRANTED &&
                healthPermissionsGranted
    }

    private fun requestAllPermissions() {
        // Request both device and health permissions
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

        // Request health permissions separately
        permissionLauncher.launch(
            requiredHealthPermissions.map { it.toString() }.toTypedArray()
        )
    }

    private suspend fun checkHealthPermissions(): Boolean {
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredHealthPermissions.all { it in grantedPermissions }
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
                Text("We need access to location, contacts, camera, audio, and health data (steps, heart rate) to provide the best experience.")
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