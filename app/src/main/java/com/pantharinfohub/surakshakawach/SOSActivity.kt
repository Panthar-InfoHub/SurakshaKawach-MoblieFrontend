package com.pantharinfohub.surakshakawach

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.pantharinfohub.surakshakawach.api.Api
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SOSActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sosTicketId: String? = null
    private val captureInterval: Long = 60000 // Capture every 60 seconds
    private var isRecordingAudio = false
    private val audioRecordingInterval: Long = 60000 // 60 seconds interval
    private val audioRecordingDuration: Long = 15000 // 15 seconds duration
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var isCapturingImages = false
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var locationCallback: LocationCallback
    private val imageUrls = mutableListOf<String>()
    private var isCameraExecutorInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the SOS ticket ID passed from HomeScreen
        sosTicketId = intent.getStringExtra("sosTicketId")

        // Make sure we have a valid ticket ID
        if (sosTicketId == null) {
            Log.e("SOS_TICKET", "SOS Ticket ID is null! Cannot proceed.")
            finish() // Close the activity if no valid ticket ID
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Update coordinates when the location changes
                    updateCoordinates(location.latitude, location.longitude)
                }
            }
        }

        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        isCameraExecutorInitialized = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkMultiplePermissionsAndStartUpdates()

        // Get the SOS ticket ID passed from HomeScreen
        sosTicketId = intent.getStringExtra("sosTicketId")

        // Start the service in foreground mode for background execution
        startSOSService()

        setContent {
            val isTicketClosed = remember { mutableStateOf(false) }
            val errorMessage = remember { mutableStateOf<String?>(null) }

            SOSScreen(
                onCloseTicket = {
                    stopSOSService(
                        onSuccess = {
                            isTicketClosed.value = true // Set ticket closed state to true on success
                            Log.d("SOSActivity", "SOS ticket closed successfully.")
                        },
                        onError = { error ->
                            errorMessage.value = error // Set error message on failure
                            Log.e("SOSActivity", "Failed to close SOS ticket: $error")
                        }
                    )
                    finish() // Close the activity after stopping the service
                },
                isTicketClosed = isTicketClosed.value, // Pass state to the screen
                errorMessage = errorMessage.value // Pass error message to the screen
            )
        }

        // Start background tasks
        startCamera()
        startAudioRecordingAtIntervals()
        startImageCapture()
        startUpdatingCoordinates()
    }

    private fun checkMultiplePermissionsAndStartUpdates() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

        when {
            permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            } -> {
                // All permissions are granted, proceed
                startUpdatingCoordinates()
                startCamera()
            }
            else -> {
                // Request multiple permissions
                requestMultiplePermissionsLauncher.launch(permissions)
            }
        }
    }

    private fun startSOSService() {
        // Start foreground service for continuous background execution
        val serviceIntent = Intent(this, SOSBackgroundService::class.java)
        serviceIntent.putExtra("sosTicketId", sosTicketId)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopSOSService(onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Stop image capture
        stopCapturingImages()

        // Stop audio recording
        stopAudioRecordingAtIntervals()

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Make API call to close the ticket
        sosTicketId?.let {
            closeSOSTicket(
                onSuccess = {
                    onSuccess() // Call success callback when ticket is closed
                },
                onError = { error ->
                    onError(error) // Call error callback in case of failure
                }
            )
        }

        // Stop the SOS background service
        val stopIntent = Intent(this, SOSBackgroundService::class.java)
        stopService(stopIntent)

        Log.d("SOSActivity", "SOS service stopped.")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // ImageCapture instance
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1080, 1080)) // Set a desired resolution
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind any previous use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to the camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture
                )

                // Start image capture only after the camera is bound
                startImageCapture()

            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startImageCapture() {
        isCapturingImages = true
        val imageCaptureRunnable = object : Runnable {
            override fun run() {
                if (isCapturingImages) {
                    captureImage()
                    handler.postDelayed(this, captureInterval)
                }
            }
        }
        handler.post(imageCaptureRunnable)
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(externalMediaDirs.first(), "IMG_$timestamp.jpg")
        val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraX", "Photo captured: ${photoFile.absolutePath}")
                    val compressedFile = compressImage(photoFile)

                    // Pass the 'firebaseUID' along with the compressed file
                    uploadImageToFirebase(compressedFile, firebaseUID)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun compressImage(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val targetSize = 100 * 1024 // 100 KB
        var quality = 100
        var compressedFile = file

        do {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val compressedData = byteArrayOutputStream.toByteArray()

            compressedFile = File(file.parent, "COMPRESSED_${file.name}")
            FileOutputStream(compressedFile).use {
                it.write(compressedData)
                it.flush()
            }

            quality -= 5
        } while (compressedFile.length() > targetSize && quality > 0)

        Log.d("CameraX", "Image compressed to: ${compressedFile.length() / 1024} KB")
        return compressedFile
    }

    private fun uploadImageToFirebase(file: File, firebaseUID: String) {
        val fileUri: Uri = Uri.fromFile(file)
        val storageReference = FirebaseStorage.getInstance()
            .getReference("emergency-images/${file.name}")

        storageReference.putFile(fileUri)
            .addOnSuccessListener {
                // Get the download URL after a successful upload
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("SOS_TICKET", "Image uploaded successfully: $uri")

                    // Add the URL to the list of image URLs
                    val imageUrl = uri.toString()
                    imageUrls.add(imageUrl) // Add to the list of image URLs

                    // Now send the URL array and the latest image URL to the backend
                    sendImageUrlToBackend(firebaseUID, imageUrl, imageUrls)

                    file.delete() // Optionally delete the file after upload
                }
            }
            .addOnFailureListener {
                Log.e("SOS_TICKET", "Failed to upload image: ${it.message}")
            }
    }

    private fun sendImageUrlToBackend(firebaseUID: String, latestImageUrl: String, imageUrls: List<String>) {
        if (sosTicketId != null) {
            Log.d("SOS_TICKET", "Preparing to send image URLs to backend. SOS Ticket ID: $sosTicketId")

            lifecycleScope.launch {
                try {
                    val success = Api().sendImages(sosTicketId!!, firebaseUID, imageUrls)
                    if (success) {
                        Log.d("SOS_TICKET", "Image URLs sent successfully to the server")
                    } else {
                        Log.e("SOS_TICKET", "Failed to send image URLs for ticket ID: $sosTicketId")
                    }
                } catch (e: Exception) {
                    Log.e("SOS_TICKET", "Error while sending image URLs: ${e.localizedMessage}", e)
                }
            }
        } else {
            Log.e("SOS_TICKET", "Cannot send image URLs. SOS ticket ID is null.")
        }
    }

    private fun startAudioRecordingAtIntervals() {
        val audioRecordingRunnable = object : Runnable {
            override fun run() {
                if (isRecordingAudio) {
                    startAudioRecording()
                    handler.postDelayed({ stopAudioRecording() }, audioRecordingDuration)
                    handler.postDelayed(this, audioRecordingInterval)
                }
            }
        }
        isRecordingAudio = true
        handler.post(audioRecordingRunnable)
    }

    private fun startAudioRecording() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val audioFile = File(externalMediaDirs.first(), "AUDIO_$timestamp.mp3")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(audioFile.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            try {
                prepare()
                start()
                Log.d("SOSActivity", "Audio recording started: ${audioFile.absolutePath}")
            } catch (e: IOException) {
                Log.e("SOSActivity", "Audio recording failed: ${e.message}")
            }
        }

        handler.postDelayed({
            stopAudioRecording()
            uploadAudioToFirebase(audioFile)
        }, audioRecordingDuration)
    }

    private fun uploadAudioToFirebase(audioFile: File) {
        val fileUri: Uri = Uri.fromFile(audioFile)
        val storageReference = FirebaseStorage.getInstance()
            .getReference("emergency-audio/${audioFile.name}")

        storageReference.putFile(fileUri)
            .addOnSuccessListener {
                Log.d("SOS_TICKET", "Audio uploaded successfully to Firebase Storage.")
                audioFile.delete()
            }
            .addOnFailureListener {
                Log.e("SOS_TICKET", "Failed to upload audio: ${it.message}")
            }
    }

    private fun startUpdatingCoordinates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are not granted, so don't start updates
            Log.e("SOSActivity", "Location permissions are not granted.")
            return
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // At least one location permission granted
            startUpdatingCoordinates()
        } else {
            // Location permission was denied
            Log.e("SOSActivity", "Location permissions denied.")
        }
    }

    private fun updateCoordinates(latitude: Double, longitude: Double) {
        val api = Api()

        // Check if the SOS ticket ID exists
        if (sosTicketId != null) {
            // Update the coordinates for the existing ticket
            lifecycleScope.launch {
                val success = api.updateCoordinates(
                    firebaseUID = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch,
                    ticketId = sosTicketId!!,
                    latitude = latitude.toString(),
                    longitude = longitude.toString(),
                    timestamp = getCurrentTimestamp()
                )

                if (success) {
                    Log.d("SOS_TICKET", "Coordinates updated for ticket ID: $sosTicketId")
                } else {
                    Log.e("SOS_TICKET", "Failed to update coordinates for ticket ID: $sosTicketId")
                }
            }
        } else {
            // Log an error if sosTicketId is null, indicating no active SOS ticket
            Log.e("SOS_TICKET", "SOS ticket ID is null. Cannot update coordinates.")
        }
    }

    private fun stopSendingLocation() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("SOSActivity", "Stopped sending coordinates updates.")
    }


    private fun stopAudioRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        Log.d("SOSActivity", "Audio recording stopped.")
    }

    private fun stopCapturingImages() {
        isCapturingImages = false
        handler.removeCallbacksAndMessages(null)
        Log.d("SOSActivity", "Stopped capturing images.")
    }

    private fun stopAudioRecordingAtIntervals() {
        isRecordingAudio = false
        handler.removeCallbacksAndMessages(null)
        stopAudioRecording()
    }


    private fun closeSOSTicket(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val api = Api()
        val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid

        if (firebaseUID != null && sosTicketId != null) { // Ensure we have a ticket ID
            Log.d("SOSActivity", "Attempting to close ticket with ID: $sosTicketId for UID: $firebaseUID")

            lifecycleScope.launch {
                try {
                    // Close the ticket using the stored `sosTicketId`
                    val success = api.closeTicket(firebaseUID, sosTicketId!!)

                    if (success) {
                        onSuccess()
                        Log.d("SOSActivity", "Ticket closed successfully for UID: $firebaseUID, Ticket ID: $sosTicketId")

                        // Stop background processes and go back to the home screen
                        stopSendingLocation()
                        navigateToHome(this@SOSActivity)
                    } else {
                        onError("Failed to close the ticket.")
                        Log.e("SOSActivity", "Failed to close the ticket for UID: $firebaseUID, Ticket ID: $sosTicketId")
                    }
                } catch (e: Exception) {
                    Log.e("SOSActivity", "Error while closing the ticket for UID: $firebaseUID - ${e.localizedMessage}")
                    onError("Error while closing the ticket: ${e.localizedMessage}")
                }
            }
        } else {
            onError("No active ticket found or user is not logged in.")
            Log.e("SOSActivity", "No active ticket found or user is not logged in.")
        }
    }



    override fun onDestroy() {
        super.onDestroy()

        // Stop all background tasks
        stopCapturingImages()
        stopAudioRecordingAtIntervals()
        stopUpdatingCoordinates()

        // Shutdown camera executor
        if (isCameraExecutorInitialized) {
            cameraExecutor.shutdown()
            Log.d("SOSActivity", "Camera executor shutdown successfully.")
        } else {
            Log.e("SOSActivity", "Camera executor was not initialized, skipping shutdown.")
        }

        // Remove all handler callbacks
        handler.removeCallbacksAndMessages(null)
    }

}



@Composable
fun SOSScreen(
    onCloseTicket: () -> Unit,
    isTicketClosed: Boolean,
    errorMessage: String?
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "SOS Sent Successfully!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Help is on the way!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isTicketClosed) {
                Text(
                    text = "SOS Ticket closed successfully.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Button to stop SOS and close the ticket
            Button(
                onClick = onCloseTicket,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Stop SOS and Close Ticket")
            }

            // Button to return to Home
            Button(
                onClick = {
                    stopUpdatingCoordinates()
                    navigateToHome(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = "Go Back to Home")
            }
        }
    }
}


private val coordinateUpdateHandler = Handler(Looper.getMainLooper())

// Function to stop sending coordinate updates when SOS is stopped
private fun stopUpdatingCoordinates() {
    coordinateUpdateHandler.removeCallbacksAndMessages(null)
    Log.d("SOSActivity", "Stopped sending coordinates updates.")
}

// Function to navigate back to the HomeActivity
private fun navigateToHome(context: Context) {
    val intent = Intent(context, HomeActivity::class.java)
    context.startActivity(intent)

    // Finish the current activity to prevent back navigation
    if (context is Activity) {
        context.finish()
    }
}