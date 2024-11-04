package com.nextlevelprogrammers.surakshakawach.ui

import Api
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import com.nextlevelprogrammers.surakshakawach.api.EmergencyContact
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen() {
    val context = LocalContext.current
    val firebaseUID = FirebaseAuth.getInstance().currentUser?.uid

    if (firebaseUID == null) {
        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        return
    }

    var emergencyContacts by remember { mutableStateOf(listOf<EmergencyContact>()) }
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Define the pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

// Function to refresh contacts from the server
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            loadContacts(firebaseUID, context, coroutineScope) { contacts ->
                emergencyContacts = contacts
                isRefreshing = false
                loading = false
            }
        }
    }

    // Initial load of contacts
    LaunchedEffect(Unit) {
        onRefresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                LazyColumn {
                    items(emergencyContacts.size) { index ->
                        val contact = emergencyContacts[index]
                        ContactCard(
                            contact,
                            onUpdate = { updatedContact ->
                                emergencyContacts = emergencyContacts.map {
                                    if (it == contact) updatedContact else it
                                }
                                coroutineScope.launch {
                                    val isSuccess = Api().updateEmergencyContacts(
                                        firebaseUID = firebaseUID,
                                        oldContacts = listOf(contact),
                                        newContacts = listOf(updatedContact)
                                    )
                                    if (isSuccess) {
                                        onRefresh()
                                        Toast.makeText(context, "Contact updated successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to update contact", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { contactToDelete ->
                                coroutineScope.launch {
                                    val isSuccess = Api().removeEmergencyContacts(
                                        firebaseUID = firebaseUID,
                                        contactDetails = listOf(contactToDelete)
                                    )
                                    if (isSuccess) {
                                        onRefresh()
                                        Toast.makeText(context, "${contactToDelete.name} deleted successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to delete ${contactToDelete.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
                if (isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Emergency Contact")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Emergency Contact") },
                text = {
                    Column {
                        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Phone Number") })
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val isDuplicate = emergencyContacts.any {
                                it.email == email || it.mobile == mobile
                            }

                            if (isDuplicate) {
                                Toast.makeText(
                                    context,
                                    "Contact with this email or phone number already exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            coroutineScope.launch {
                                try {
                                    val isSuccess = Api().sendEmergencyContactToServer(
                                        firebaseUID,
                                        name,
                                        email,
                                        mobile
                                    )
                                    if (isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "Contact added successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onRefresh()
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
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
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
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxOffset = 120f
    val animatedOffsetX by animateDpAsState(targetValue = offsetX.dp)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var updatedName by remember { mutableStateOf(contact.name) }
    var updatedEmail by remember { mutableStateOf(contact.email) }
    var updatedMobile by remember { mutableStateOf(contact.mobile) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val dragSensitivity = 1.5f
                        val scaledDragAmount = dragAmount * dragSensitivity
                        val newOffsetX = (offsetX + scaledDragAmount).coerceIn(-maxOffset * 1.2f, 0f)
                        offsetX = if (newOffsetX < -maxOffset) -maxOffset + (newOffsetX + maxOffset) / 3 else newOffsetX
                    },
                    onDragEnd = { if (offsetX > -maxOffset / 2) offsetX = 0f }
                )
            }
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterEnd)
                .padding(horizontal = 12.dp)
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF4CAF50))
                }

                if (showEditDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditDialog = false },
                        title = { Text("Edit Contact") },
                        text = {
                            Column {
                                TextField(value = updatedName, onValueChange = { updatedName = it }, label = { Text("Name") })
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(value = updatedEmail, onValueChange = { updatedEmail = it }, label = { Text("Email") })
                                Spacer(modifier = Modifier.height(8.dp))
                                TextField(value = updatedMobile, onValueChange = { updatedMobile = it }, label = { Text("Mobile") })
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                onUpdate(EmergencyContact(updatedName, updatedEmail, updatedMobile))
                                showEditDialog = false
                            }) { Text("Update") }
                        },
                        dismissButton = { Button(onClick = { showEditDialog = false }) { Text("Cancel") } }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { scope.launch { onDelete(contact) } }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF44336))
                }
            }
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .offset(x = animatedOffsetX)
                .fillMaxWidth()
                .background(Color.White)
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