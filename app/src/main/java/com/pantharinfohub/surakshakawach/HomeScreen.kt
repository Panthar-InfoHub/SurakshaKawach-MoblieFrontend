package com.pantharinfohub.surakshakawach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.auth.FirebaseAuth
import com.pantharinfohub.surakshakawach.api.Api
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, fusedLocationClient: FusedLocationProviderClient) {
    var currentLocation by remember { mutableStateOf(LatLng(25.4484, 78.5685)) } // Default to Jhansi
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isDrawerVisible by remember { mutableStateOf(false) } // Control the visibility of the drawer
    val coroutineScope = rememberCoroutineScope()

    // State for modal dialog
    var showModal by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(3) }

    // Handler for periodic location updates
    val handler = remember { Handler(Looper.getMainLooper()) }

    // Get the current user's Firebase UID
    val firebaseAuth = FirebaseAuth.getInstance()
    val firebaseUID = firebaseAuth.currentUser?.uid

    if (firebaseUID == null) {
        Log.e("SOS_TICKET", "User not logged in or Firebase UID is null")
        return // Early exit if the user is not logged in
    }

    // Request location permission if not granted
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        } else {
            // Handle asking for location permission
        }
    }

    // Define the camera position based on current location
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 14f) // Set zoom level to 14
    }

    // Function to start sending location every 5 seconds
    val startSendingLocation = {
        val locationRunnable = object : Runnable {
            override fun run() {
                if (hasLocationPermission) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val latitude = it.latitude
                            val longitude = it.longitude
                            val timestamp = getCurrentTimestamp()

                            Log.d("SOS_TICKET", "Location acquired: Latitude: $latitude, Longitude: $longitude")

                            // Create SOS ticket and send to backend
                            coroutineScope.launch {
                                val api = Api() // Instantiate the API class
                                val success = api.sendSosTicket(
                                    firebaseUID = firebaseUID,
                                    latitude = latitude.toString(),
                                    longitude = longitude.toString(),
                                    timestamp = timestamp
                                )

                                if (success) {
                                    Log.d("SOS_TICKET", "SOS ticket sent successfully")
                                } else {
                                    Log.e("SOS_TICKET", "Failed to send SOS ticket")
                                }
                            }
                        } ?: run {
                            Log.e("SOS_TICKET", "Location is null")
                        }
                    }.addOnFailureListener { exception ->
                        Log.e("SOS_TICKET", "Failed to get location: ${exception.message}")
                    }
                }

                // Schedule the next location update after 5 seconds
                handler.postDelayed(this, 5000)
            }
        }

        // Start the location updates immediately
        handler.post(locationRunnable)
    }

    // Function to send SOS and navigate to SOS activity
    val sendSOSTicketAndOpenActivity = {
        startSendingLocation()
        context.startActivity(Intent(context, SOSActivity::class.java))
    }


    // Timer function for the modal
    fun startTimer() {
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                sendSOSTicketAndOpenActivity()
            }
        }.start()
    }

    // Function to stop sending location
    val stopSendingLocation = {
        handler.removeCallbacksAndMessages(null)
        Log.d("SOS_TICKET", "Stopped sending location updates")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            // Top bar with icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { // Navigate to the DashboardScreen using NavController
                    navController.navigate("dashboard")  }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_profile), // Replace with your icon resource
                        contentDescription = "Profile",
                        tint = Color.Black,
                        modifier = Modifier.size(58.dp)
                    )
                }
                Text(text = "Suraksha Kawach", color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.headlineSmall )
                IconButton(onClick = {
                    isDrawerVisible = !isDrawerVisible // Toggle the drawer visibility
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings), // Replace with your icon resource
                        contentDescription = "Settings",
                        tint = Color.Black,
                        modifier = Modifier.size(58.dp)
                    )
                }
            }

            // Google Map with SOS button overlay
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(410.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    )
                }

                // SOS Button, positioned at the bottom of the map
                Button(
                    onClick = {
                        Log.d("SOS_TICKET", "SOS button clicked")
                        if (hasLocationPermission) {
                            // Show the modal and start the timer
                            showModal = true
                            startTimer()
                        } else {
                            Log.e("SOS_TICKET", "Permission not granted")
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-20).dp)
                        .height(90.dp)
                        .width(150.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "SOS", color = Color.White)
                }
            }

            // Modal Dialog for SOS Confirmation
            if (showModal) {
                AlertDialog(
                    onDismissRequest = { /* Do nothing when clicking outside */ },
                    title = { Text("Send SOS?") },
                    text = {
                        Column {
                            Text("SOS will be sent in $countdown seconds.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Do you want to cancel or confirm?")
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            // Confirm: Send SOS and navigate to SOS activity
                            sendSOSTicketAndOpenActivity()
                        }) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            // Cancel: Just close the modal
                            showModal = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Favourites section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Favourites", color = Color.Black)
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.avatar1), // Replace with your avatar resource
                            contentDescription = "Avatar 1",
                            modifier = Modifier.size(100.dp).padding(end = 20.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.avatar2), // Replace with your avatar resource
                            contentDescription = "Avatar 2",
                            modifier = Modifier.size(100.dp).padding(end = 20.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.avatar3), // Replace with your avatar resource
                            contentDescription = "Avatar 3",
                            modifier = Modifier.size(100.dp).padding(start = 16.dp)
                        )
                    }
                }
            }

            // Bottom navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { /* Handle home click */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home), // Replace with your icon resource
                        contentDescription = "Home",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = {
                    val intent = Intent(context, WatchActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_watch), // Replace with your icon resource
                        contentDescription = "Another",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = { /* Handle settings click */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home), // Replace with your icon resource
                        contentDescription = "Settings",
                        tint = Color.Black
                    )
                }
            }
        }

        // Conditional rendering of the drawer
        if (isDrawerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color.Gray)
                    .padding(16.dp)
                    .align(Alignment.CenterStart)
            ) {
                Column {
                    TextButton(onClick = { /* Handle Profile click */ }) {
                        Text(text = "Profile", color = Color.White)
                    }
                    TextButton(onClick = {
                        coroutineScope.launch {
                            navController.navigate("emergency_contacts") // Navigate to EmergencyContactsScreen
                        }
                    }) {
                        Text(text = "Emergency Contacts", color = Color.White)
                    }
                    TextButton(onClick = { /* Handle Logout click */ }) {
                        Text(text = "Logout", color = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { /* Handle Help click */ }) {
                        Text(text = "Help", color = Color.White)
                    }
                }
            }
        }
    }
}

// Function to get the current timestamp
fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return sdf.format(Date())
}