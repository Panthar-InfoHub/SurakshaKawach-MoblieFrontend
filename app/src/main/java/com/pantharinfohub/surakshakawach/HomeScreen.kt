package com.pantharinfohub.surakshakawach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.pantharinfohub.surakshakawach.api.Api
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, fusedLocationClient: FusedLocationProviderClient) {
    var currentLocation by remember { mutableStateOf(LatLng(25.4484, 78.5685)) } // Default to Jhansi
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isDrawerVisible by remember { mutableStateOf(false) } // Control the visibility of the drawer
    val coroutineScope = rememberCoroutineScope()
    var isUpdatingLocation = false
    var countdownTimer: CountDownTimer? = null
    var isSOSCanceled by remember { mutableStateOf(false) }


    // State for modal dialog
    var showModal by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }

    // Handler for periodic location updates
    val handler = remember { Handler(Looper.getMainLooper()) }
    val locationRequest = LocationRequest.create().apply {
        interval = 5000 // Adjust interval as needed
        fastestInterval = 2000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    // Get the current user's Firebase UID
    val firebaseAuth = FirebaseAuth.getInstance()
    val firebaseUID = firebaseAuth.currentUser?.uid

    // Store ticket ID after the first ticket is created
    var sosTicketId: String? = null

    if (firebaseUID == null) {
        Log.e("SOS_TICKET", "User not logged in or Firebase UID is null")
        return // Early exit if the user is not logged in
    }

    // Request location permission if not granted
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission is granted, get the last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        } else {
            Log.e("LocationPermission", "Location permission denied by the user")
            // Show a message to the user or handle the lack of location permission gracefully
        }
    }

// Check permission and request if not already granted
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            // Permission already granted, get the last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                }
            }
        } else {
            // Request location permission
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }



    // Define the camera position based on current location
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 14f) // Set zoom level to 14
    }

    suspend fun handleSOSTicket(
        firebaseUID: String,
        latitude: String,
        longitude: String,
        timestamp: String
    ) {
        val api = Api()
        sosTicketId = sosTicketId ?: api.checkActiveTicket(firebaseUID)

        if (sosTicketId == null) {
            val ticketId = api.sendSosTicket(
                firebaseUID = firebaseUID,
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
            if (ticketId != null) {
                sosTicketId = ticketId
                Log.d("SOS_TICKET", "New SOS ticket created with ID: $ticketId")
            } else {
                Log.e("SOS_TICKET", "Failed to create a new SOS ticket")
            }
        } else {
            val success = api.updateCoordinates(
                firebaseUID = firebaseUID,
                ticketId = sosTicketId!!,
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
            if (success) {
                Log.d("SOS_TICKET", "Coordinates updated for ticket ID: $sosTicketId")
            } else {
                Log.e("SOS_TICKET", "Failed to update coordinates for ticket ID: $sosTicketId")
            }
        }
    }

    // Initialize the LocationCallback
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                currentLocation = LatLng(location.latitude, location.longitude) // Update current location
                Log.d("SOS_TICKET", "Location update: ${location.latitude}, ${location.longitude}")
                // Send location to the server if SOS is active
                if (isUpdatingLocation) {
                    coroutineScope.launch {
                        handleSOSTicket(firebaseUID ?: return@launch, location.latitude.toString(), location.longitude.toString(), getCurrentTimestamp())
                    }
                }
            }
        }
    }

    // Location updates request
    fun requestLocationUpdates(fusedLocationClient: FusedLocationProviderClient) {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        val timestamp = getCurrentTimestamp()
                        currentLocation = LatLng(it.latitude, it.longitude)
                        if (isUpdatingLocation) {
                            coroutineScope.launch {
                                handleSOSTicket(firebaseUID, it.latitude.toString(), it.longitude.toString(), timestamp)
                            }
                        }
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    // Function to start sending location updates if not already active
    val startSendingLocation = {
        if (!isUpdatingLocation) {
            isUpdatingLocation = true
            requestLocationUpdates(fusedLocationClient) // Start requesting location updates.
        }
    }

    // Function to stop sending location updates
    val stopSendingLocation = {
        if (isUpdatingLocation) {
            isUpdatingLocation = false
            fusedLocationClient.removeLocationUpdates(locationCallback) // Stop location updates.
            Log.d("SOS_TICKET", "Stopped sending location updates")
        }
    }


    // Function to cancel SOS
    val cancelSOS = {
        countdownTimer?.cancel() // Cancel the timer
        stopSendingLocation() // Stop sending location updates
        isSOSCanceled = true // Mark SOS as canceled
        showModal = false // Close the modal
        Log.d("SOS_TICKET", "SOS canceled")
    }


    // Launch SOS activity and start sending location
    val sendSOSTicketAndOpenActivity = {
        if (!isSOSCanceled) { // Only proceed if SOS is not canceled
            startSendingLocation()
            context.startActivity(Intent(context, SOSActivity::class.java))
        }
    }

    // Start the countdown timer
    fun startTimer() {
        countdownTimer?.cancel()
        isSOSCanceled = false // Reset the cancel flag
        countdownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                if (!isSOSCanceled) {
                    sendSOSTicketAndOpenActivity()
                }
            }
        }.start()
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
                            cancelSOS()
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