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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SOSScreen(
                onCloseTicket = {
                    stopSendingLocation() // Stop the location updates
                    closeSOSTicket() // Call the function to close the ticket
                }
            )
        }
    }

    // Function to stop sending location updates
    private fun stopSendingLocation() {
        // Remove all callbacks to stop periodic location updates
        handler.removeCallbacksAndMessages(null)
        Log.d("SOS_TICKET", "Stopped sending location updates")
    }

    // Function to close the SOS ticket
    private fun closeSOSTicket() {
        val api = Api() // Your API instance
        val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid
        val ticketId = sosTicketId // This should be the ID of the active SOS ticket

        if (firebaseUID != null && ticketId != null) {
            // Use lifecycleScope to launch a coroutine in an Activity context
            lifecycleScope.launch {
                val success = api.closeTicket(
                    firebaseUID = firebaseUID,
                    ticketId = ticketId
                )
                if (success) {
                    Log.d("SOS_TICKET", "SOS ticket closed successfully")
                    sosTicketId = null // Clear the stored ticket ID
                } else {
                    Log.e("SOS_TICKET", "Failed to close SOS ticket")
                }
            }
        } else {
            Log.e("SOS_TICKET", "Cannot close ticket. Firebase UID or ticket ID is null.")
        }
    }
}

@Composable
fun SOSScreen(
    onCloseTicket: () -> Unit // Pass this function to handle the close action
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