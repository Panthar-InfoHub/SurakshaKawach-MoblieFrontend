package com.nextlevelprogrammers.surakshakawach

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme

class EmergencyDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the deep link data
        val data = intent?.data

        val ticketId = data?.getQueryParameter("ticketId")
        val firebaseUID = data?.getQueryParameter("firebaseUID")

        // Log the parameters or handle them as needed
        Log.d("EmergencyDashboard", "Ticket ID: $ticketId")
        Log.d("EmergencyDashboard", "Firebase UID: $firebaseUID")

        setContent {
            SurakshaKawachTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    EmergencyDashboardScreen(ticketId, firebaseUID)
                }
            }
        }
    }
}

@Composable
fun EmergencyDashboardScreen(ticketId: String?, firebaseUID: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Emergency Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Correct elevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency Icon",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Display ticketId and firebaseUID
                if (ticketId != null && firebaseUID != null) {
                    Text(text = "Ticket ID: $ticketId", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Firebase UID: $firebaseUID", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(text = "No active emergency", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { /* Handle start emergency */ }) {
                Text(text = "Start Emergency")
            }
            Button(
                onClick = { /* Handle end emergency */ },
                enabled = ticketId != null && firebaseUID != null
            ) {
                Text(text = "End Emergency")
            }
        }
    }
}