package com.pantharinfohub.surakshakawach

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.pantharinfohub.surakshakawach.api.Api
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

    private val handler = Handler(Looper.getMainLooper())
    private var isCapturingImages = false
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val captureInterval: Long = 30000 // Capture every 30 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            val isTicketClosed = remember { mutableStateOf(false) }
            val errorMessage = remember { mutableStateOf<String?>(null) }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
            }

            SOSScreen(
                onCloseTicket = {
                    stopCapturingImages()
                    closeSOSTicket(
                        onSuccess = {
                            isTicketClosed.value = true
                            Log.d("SOS_TICKET", "SOS ticket closed successfully")
                        },
                        onError = { error ->
                            errorMessage.value = error
                            Log.e("SOS_TICKET", "Failed to close SOS ticket: $error")
                        }
                    )
                },
                isTicketClosed = isTicketClosed.value,
                errorMessage = errorMessage.value
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // ImageCapture instance
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1280, 720)) // Set a desired resolution
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

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraX", "Photo captured: ${photoFile.absolutePath}")
                    val compressedFile = compressImage(photoFile)
                    uploadImageToFirebase(compressedFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun compressImage(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val targetSize = 500 * 1024 // 500 KB
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

    private fun uploadImageToFirebase(file: File) {
        val fileUri: Uri = Uri.fromFile(file)
        val storageReference = FirebaseStorage.getInstance()
            .getReference("emergency-images/${file.name}")
        storageReference.putFile(fileUri)
            .addOnSuccessListener {
                Log.d("SOS_TICKET", "Image uploaded successfully to Firebase Storage.")
            }
            .addOnFailureListener {
                Log.e("SOS_TICKET", "Failed to upload image: ${it.message}")
            }
    }

    private fun stopCapturingImages() {
        isCapturingImages = false
        handler.removeCallbacksAndMessages(null)
        Log.d("SOS_TICKET", "Stopped capturing images.")
    }

    private fun closeSOSTicket(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val api = Api()
        val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid
        if (firebaseUID != null) {
            lifecycleScope.launch {
                val activeTicketId = api.checkActiveTicket(firebaseUID)
                if (activeTicketId != null) {
                    val success = api.closeTicket(firebaseUID, activeTicketId)
                    if (success) {
                        onSuccess()
                    } else {
                        onError("Failed to close the ticket.")
                    }
                } else {
                    onError("No active ticket found.")
                }
            }
        } else {
            onError("User is not logged in.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        stopCapturingImages()
    }
}

@Composable
fun SOSScreen(
    onCloseTicket: () -> Unit,
    isTicketClosed: Boolean,
    errorMessage: String?
) {
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

            Button(
                onClick = onCloseTicket,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Stop SOS and Close Ticket")
            }

            Button(
                onClick = { /* Handle action, e.g., navigate back */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = "Go Back to Home")
            }
        }
    }
}