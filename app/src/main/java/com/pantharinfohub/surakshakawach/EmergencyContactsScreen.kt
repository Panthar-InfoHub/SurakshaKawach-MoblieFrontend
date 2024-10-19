package com.pantharinfohub.surakshakawach

import Api
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

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

    // State to hold emergency contacts
    var emergencyContacts by remember { mutableStateOf(listOf<EmergencyContact>()) }

    // State to handle showing the dialog for adding new contacts
    var showDialog by remember { mutableStateOf(false) }

    // State for the input fields in the add dialog
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }

    // Coroutine scope to handle background tasks
    val coroutineScope = rememberCoroutineScope()

    // Fetch emergency contacts from the server
    LaunchedEffect(Unit) {
        loadContacts(firebaseUID, context, coroutineScope) { contacts ->
            emergencyContacts = contacts
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

            // Display contacts in a list
            LazyColumn {
                items(emergencyContacts.size) { index ->
                    val contact = emergencyContacts[index]
                    ContactCard(contact, onUpdate = { updatedContact ->
                        // Update the contact in the list and refresh the state
                        emergencyContacts = emergencyContacts.map {
                            if (it == contact) updatedContact else it
                        }
                        // Optionally, send the updated contact to the backend API
                        coroutineScope.launch {
                            val isSuccess = Api().updateEmergencyContacts(
                                firebaseUID = firebaseUID,
                                oldContacts = listOf(contact),  // Old contact details
                                newContacts = listOf(updatedContact)  // Updated contact details
                            )
                            if (isSuccess) {
                                Toast.makeText(context, "Contact updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to update contact", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, onDelete = { contactToDelete ->
                        // Remove the contact from the list after deletion
                        coroutineScope.launch {
                            val isSuccess = Api().removeEmergencyContacts(
                                firebaseUID = firebaseUID,
                                contactDetails = listOf(contactToDelete)
                            )
                            if (isSuccess) {
                                emergencyContacts = emergencyContacts.filter { it != contactToDelete }
                                Toast.makeText(context, "${contactToDelete.name} deleted successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete ${contactToDelete.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }
        }

        // FAB for adding a new contact
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Emergency Contact")
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
                            value = mobile,
                            onValueChange = { mobile = it },
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
                                        mobile
                                    )
                                    Log.d("API_RESPONSE", "Success: $isSuccess")

                                    if (isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "Contact added successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Add the new contact to the list and update state
                                        emergencyContacts = emergencyContacts + EmergencyContact(name, email, mobile)
                                        // Reset fields and close the dialog
                                        name = ""
                                        email = ""
                                        mobile = ""
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

// Function to load contacts
private fun loadContacts(
    firebaseUID: String,
    context: Context,
    coroutineScope: CoroutineScope,
    onSuccess: (List<EmergencyContact>) -> Unit
) {
    coroutineScope.launch {
        try {
            val userProfileResponse = Api().getUserProfile(firebaseUID)
            if (userProfileResponse != null) {
                onSuccess(userProfileResponse.data.emergencyContacts ?: emptyList())
            } else {
                Toast.makeText(context, "Failed to load contacts", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error fetching contacts: ${e.localizedMessage}")
            Toast.makeText(context, "Error fetching contacts", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ContactCard(contact: EmergencyContact, onUpdate: (EmergencyContact) -> Unit, onDelete: (EmergencyContact) -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxOffset = 120f
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State to handle showing the dialog for editing contacts
    var showEditDialog by remember { mutableStateOf(false) }
    var updatedName by remember { mutableStateOf(contact.name) }
    var updatedEmail by remember { mutableStateOf(contact.email) }
    var updatedMobile by remember { mutableStateOf(contact.mobile) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount).coerceIn(-maxOffset, 0f)
                }
            }
    ) {
        // Background card with Edit and Delete icons
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterEnd)
                .padding(horizontal = 16.dp)
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button
                IconButton(
                    onClick = { showEditDialog = true },
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF4CAF50))
                }

                // Modal Dialog for editing contact details
                if (showEditDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        title = { Text("Edit Contact") },
                        text = {
                            Column {
                                TextField(
                                    value = updatedName,
                                    onValueChange = { updatedName = it },
                                    label = { Text("Name") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(
                                    value = updatedEmail,
                                    onValueChange = { updatedEmail = it },
                                    label = { Text("Email") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(
                                    value = updatedMobile,
                                    onValueChange = { updatedMobile = it },
                                    label = { Text("Mobile") }
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    // Create an updated contact object and pass to onUpdate
                                    val updatedContact = EmergencyContact(
                                        name = updatedName,
                                        email = updatedEmail,
                                        mobile = updatedMobile
                                    )
                                    onUpdate(updatedContact)
                                    showEditDialog = false
                                }
                            ) {
                                Text("Update")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showEditDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete button with API integration
                IconButton(
                    onClick = {
                        scope.launch {
                            Toast.makeText(context, "Deleting ${contact.name}", Toast.LENGTH_SHORT).show()
                            onDelete(contact) // Call the onDelete function when deleting a contact
                        }
                    },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF44336))
                }
            }
        }

        // Foreground with the contact card, swiping over the buttons
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .offset(x = offsetX.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "Name: ${contact.name}", fontWeight = FontWeight.Bold)
                Text(text = "Email: ${contact.email}")
                Text(text = "Phone: ${contact.mobile}")
            }
        }
    }
}