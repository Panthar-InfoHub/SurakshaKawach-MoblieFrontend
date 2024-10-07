package com.pantharinfohub.surakshakawach

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.pantharinfohub.surakshakawach.api.Api
import kotlinx.coroutines.launch

class SOSActivity : ComponentActivity() {

    // Handler for periodic location updates
    private val handler = Handler(Looper.getMainLooper())
    // Store ticket ID after the first ticket is created
    private var sosTicketId: String? = null
    private var isLocationUpdatesActive = false // Flag to manage location updates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // State to track if the ticket is closed successfully
            val isTicketClosed = remember { mutableStateOf(false) }
            val errorMessage = remember { mutableStateOf<String?>(null) }

            SOSScreen(
                onCloseTicket = {
                    stopSendingLocation() // Stop the location updates immediately
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

            startSendingLocationUpdates() // Start location updates when the SOSActivity is created
        }
    }

    // Function to start sending location updates every 5 seconds
    private fun startSendingLocationUpdates() {
        isLocationUpdatesActive = true
        val locationRunnable = object : Runnable {
            override fun run() {
                if (isLocationUpdatesActive) {
                    if (sosTicketId != null) {
                        Log.d("SOS_TICKET", "Sending new coordinates...")

                        // Schedule the next location update after 5 seconds if still active
                        handler.postDelayed(this, 5000)
                    } else {
                        Log.d("SOS_TICKET", "Ticket is closed, stopping location updates.")
                        stopSendingLocation() // Stop if the ticket is closed
                    }
                }
            }
        }

        // Start the location updates
        handler.post(locationRunnable)
    }

    // Function to stop sending location updates
    private fun stopSendingLocation() {
        isLocationUpdatesActive = false // Set the flag to false to stop further updates
        handler.removeCallbacksAndMessages(null) // Remove any pending callbacks
        Log.d("SOS_TICKET", "Stopped sending location updates")
    }

    // Function to close the SOS ticket
    private fun closeSOSTicket(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val api = Api() // Your API instance
        val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid

        // Fetch the active ticket ID before attempting to close it
        if (firebaseUID != null) {
            lifecycleScope.launch {
                val activeTicketId = api.checkActiveTicket(firebaseUID)
                Log.d("SOS_TICKET", "Active ticket ID fetched: $activeTicketId")

                if (activeTicketId != null) {
                    // Now that we have the active ticket ID, proceed to close it
                    val success = api.closeTicket(
                        firebaseUID = firebaseUID,
                        ticketId = activeTicketId
                    )
                    if (success) {
                        Log.d("SOS_TICKET", "SOS ticket closed successfully with ID: $activeTicketId")
                        sosTicketId = null // Clear the stored ticket ID, if any
                        onSuccess()
                    } else {
                        Log.e("SOS_TICKET", "Failed to close SOS ticket with ID: $activeTicketId")
                        onError("Failed to close the ticket.")
                    }
                } else {
                    Log.e("SOS_TICKET", "No active ticket found for Firebase UID: $firebaseUID")
                    onError("No active ticket found.")
                }
            }
        } else {
            Log.e("SOS_TICKET", "Cannot close ticket. Firebase UID is null.")
            onError("User is not logged in.")
        }
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

            // Button to close the ticket and stop sending updates
            Button(
                onClick = onCloseTicket,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Stop SOS and Close Ticket")
            }

            Button(
                onClick = { /* You can define an action, like returning to the home screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = "Go Back to Home")
            }
        }
    }
}