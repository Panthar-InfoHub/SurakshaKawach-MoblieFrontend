package com.pantharinfohub.surakshakawach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pantharinfohub.surakshakawach.ui.theme.SurakshaKawachTheme

class PermissionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the Compose UI content
        setContent {
            SurakshaKawachTheme {
                PermissionScreen()
            }
        }
    }

    @Composable
    fun PermissionScreen() {
        var showDialog by remember { mutableStateOf(false) }

        // Check if permissions are granted and navigate to Home if they are
        if (checkPermissions()) {
            navigateToHome()
        } else {
            // Trigger the dialog to request permissions
            LaunchedEffect(Unit) {
                showDialog = true
            }
        }

        if (showDialog) {
            PermissionDialog(
                onAllow = {
                    requestPermissions()
                    showDialog = false
                },
                onCancel = {
                    showDialog = false
                }
            )
        }
    }

    private fun checkPermissions(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val contactPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        return locationPermission == PackageManager.PERMISSION_GRANTED &&
                contactPermission == PackageManager.PERMISSION_GRANTED &&
                cameraPermission == PackageManager.PERMISSION_GRANTED &&
                audioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldShowRationaleForLocation = ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.ACCESS_FINE_LOCATION)
        val shouldShowRationaleForContacts = ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.READ_CONTACTS)
        val shouldShowRationaleForCamera = ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.CAMERA)
        val shouldShowRationaleForAudio = ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.RECORD_AUDIO)

        if (shouldShowRationaleForLocation || shouldShowRationaleForContacts ||
            shouldShowRationaleForCamera || shouldShowRationaleForAudio) {
            // In Compose, we'll handle this by showing the dialog
        } else {
            // Request permissions directly
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ))
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val isContactGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
            val isCameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val isAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

            if (isLocationGranted && isContactGranted && isCameraGranted && isAudioGranted) {
                // Permissions granted, navigate to HomeActivity
                navigateToHome()
            } else {
                // Handle permission denial
                // We can show a dialog here if necessary
            }
        }

    private fun navigateToHome() {
        // Navigate to HomeActivity
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()  // Finish PermissionActivity so it's removed from the back stack
    }

    @Composable
    fun PermissionDialog(onAllow: () -> Unit, onCancel: () -> Unit) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = {
                Text(text = "Permissions Required")
            },
            text = {
                Text("We need precise location, contacts, camera, and audio access to provide the best experience.")
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