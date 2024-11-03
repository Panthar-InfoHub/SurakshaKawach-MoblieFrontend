package com.nextlevelprogrammers.surakshakawach

import Api
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme
import kotlinx.coroutines.launch

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
    var ticketStatus by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch ticket status when the composable loads
    LaunchedEffect(ticketId, firebaseUID) {
        if (ticketId != null && firebaseUID != null) {
            coroutineScope.launch {
                ticketStatus = Api().fetchTicketStatus(firebaseUID, ticketId)
                Log.d("EmergencyDashboardScreen", "Ticket Status fetched: $ticketStatus") // Log status in composable
            }
        }
    }

    // Determine card color based on ticket status
    val cardColor = when (ticketStatus) {
        "closed" -> Color.Red
        "active" -> Color.Green
        else -> MaterialTheme.colorScheme.primaryContainer // Default color
    }

    // Adjust text color for readability
    val textColor = if (cardColor == Color.Red || cardColor == Color.Green) Color.White else MaterialTheme.colorScheme.onSurface

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
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    tint = textColor // Set icon color to match text color for visibility
                )

                // Display ticketId and firebaseUID
                if (ticketId != null && firebaseUID != null) {
                    Text(text = "Ticket ID: $ticketId", style = MaterialTheme.typography.bodyLarge, color = textColor)
                    Text(text = "Firebase UID: $firebaseUID", style = MaterialTheme.typography.bodyLarge, color = textColor)
                    Text(text = "Status: ${ticketStatus ?: "Loading..."}", style = MaterialTheme.typography.bodyLarge, color = textColor)
                } else {
                    Text(text = "No active emergency", style = MaterialTheme.typography.bodyLarge, color = textColor)
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