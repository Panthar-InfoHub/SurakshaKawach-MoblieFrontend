package com.pantharinfohub.surakshakawach

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.pantharinfohub.surakshakawach.api.Api

@Composable
fun EmergencyContactsScreen() {
    val context = LocalContext.current

    // Get the logged-in user's Firebase UID
    val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid

    // If firebaseUID is null, we assume the user is not logged in
    if (firebaseUID == null) {
        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        return
    }

    // State to handle showing the dialog
    var showDialog by remember { mutableStateOf(false) }

    // State for the input fields in the dialog
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    // Coroutine scope to handle background tasks
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Emergency Contacts",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Button to add a new contact, which will open the popup
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = "Add Emergency Contact")
        }

        // Popup dialog for adding a new contact
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Emergency Contact") },
                text = {
                    Column {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Send the contact to the backend using Ktor
                            coroutineScope.launch {
                                try {
                                    val isSuccess = Api().sendEmergencyContactToServer(
                                        firebaseUID,
                                        name,
                                        email,
                                        phoneNumber
                                    )
                                    Log.d("API_RESPONSE", "Success: $isSuccess")

                                    if (isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "Contact added successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Reset fields and close the dialog
                                        name = ""
                                        email = ""
                                        phoneNumber = ""
                                        showDialog = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to add contact",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Error: ${e.localizedMessage}")
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
