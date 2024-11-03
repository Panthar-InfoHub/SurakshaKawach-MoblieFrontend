package com.nextlevelprogrammers.surakshakawach.ui

import Api
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.nextlevelprogrammers.surakshakawach.R
import com.nextlevelprogrammers.surakshakawach.SOSActivity
import com.nextlevelprogrammers.surakshakawach.WatchActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(navController: NavHostController, fusedLocationClient: FusedLocationProviderClient) {
    var currentLocation by remember {
        mutableStateOf(
            LatLng(
                25.4484,
                78.5685
            )
        )
    } // Default to Jhansi
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isDrawerVisible by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    var isSOSCanceled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var countdownTimer: CountDownTimer? = null
    var isLoading by remember { mutableStateOf(false) }
    var isProcessCompleted by remember { mutableStateOf(true) }
    var isRequestSent by remember { mutableStateOf(false) }
    var isSOSButtonEnabled by remember { mutableStateOf(true) }

    val markerState = remember { MarkerState(position = currentLocation) }
    var showMapTypeSelector by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 14f)
    }

    // Firebase UID
    val firebaseAuth = FirebaseAuth.getInstance()
    val firebaseUID = firebaseAuth.currentUser?.uid

    // Store ticket ID after the first ticket is created
    var sosTicketId: String? = null
    val context = LocalContext.current

    // Request permission for location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { currentLocation = LatLng(it.latitude, it.longitude) }
            }
        } else {
            Log.e("LocationPermission", "Permission denied")
        }
    }

    // Check if permission is granted
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(currentLocation) {
        markerState.position = currentLocation
    }

    // Function to handle SOS ticket creation
    suspend fun handleSOSTicket(firebaseUID: String?) {
        Log.d("SOS_TICKET", "Entered handleSOSTicket function.")

        if (firebaseUID.isNullOrEmpty()) {
            Log.e("SOS_TICKET", "Firebase UID is null or empty.")
            return
        }

        val api = Api()
        sosTicketId = api.checkActiveTicket(firebaseUID)

        if (sosTicketId == null) {
            val timestamp = getCurrentTimestamp()
            sosTicketId = api.sendSosTicket(
                firebaseUID = firebaseUID,
                latitude = currentLocation.latitude.toString(),
                longitude = currentLocation.longitude.toString(),
                timestamp = timestamp
            )
            if (sosTicketId != null) {
                Log.d("SOS_TICKET", "New SOS ticket created: $sosTicketId")
            } else {
                Log.e("SOS_TICKET", "Failed to create SOS ticket.")
            }
        } else {
            Log.d("SOS_TICKET", "Active ticket found: $sosTicketId")
        }
    }

    // Start the countdown timer and navigate to SOSActivity
    fun startTimer() {
        Log.d("SOS_TIMER", "Starting timer.")
        countdown = 5 // Reset countdown
        isSOSCanceled = false // Reset cancel flag
        isSOSButtonEnabled = false

        countdownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown = (millisUntilFinished / 1000).toInt()
                Log.d("SOS_TIMER", "Countdown ticking: $countdown seconds remaining.")
            }

            override fun onFinish() {
                Log.d("SOS_TIMER", "Timer finished.")
                if (!isSOSCanceled && !isRequestSent) {
                    Log.d("SOS_TIMER", "SOS request will be sent now.")
                    isRequestSent = true // Set flag to prevent further requests
                    coroutineScope.launch {
                        try {
                            handleSOSTicket(firebaseUID ?: return@launch)
                            // Check if sosTicketId is not null before navigating
                            if (sosTicketId != null) {
                                Log.d("SOS_NAVIGATION", "Navigating to SOSActivity with ticket ID: $sosTicketId")
                                val intent = Intent(context, SOSActivity::class.java)
                                intent.putExtra("sosTicketId", sosTicketId)
                                context.startActivity(intent)
                            } else {
                                Log.e("SOS_TICKET", "SOS Ticket ID is null! Cannot proceed to SOSActivity.")
                                // Optionally notify the user about the failure
                            }
                        } catch (e: Exception) {
                            Log.e("SOS", "Failed to send SOS request", e)
                            // Optionally notify the user about the failure
                        }
                    }
                } else {
                    Log.d("SOS_TIMER", "SOS request canceled or already sent.")
                }
            }
        }.start()
        Log.d("SOS_TIMER", "Timer started.")
    }
    // Unified function to handle SOS sending
    fun sendSOS() {
        coroutineScope.launch {
            try {
                handleSOSTicket(firebaseUID ?: return@launch)
                if (sosTicketId != null) {
                    val intent = Intent(context, SOSActivity::class.java).apply {
                        putExtra("sosTicketId", sosTicketId)
                    }
                    context.startActivity(intent)
                } else {
                    Log.e("SOS_TICKET", "SOS Ticket ID is null! Cannot proceed to SOSActivity.")
                }
            } catch (e: Exception) {
                Log.e("SOS", "Failed to send SOS request", e)
            } finally {
                isSOSButtonEnabled = true // Re-enable the SOS button after the request
                isLoading = false
                isRequestSent = false
                isProcessCompleted = true
                isSOSButtonEnabled = true
            }
        }
    }


    // Function to manually trigger SOS after "Confirm"
    fun sendSOSManually() {
        if (!isRequestSent) {
            isRequestSent = true
            countdownTimer?.cancel()
            sendSOS()
        }
    }

    Box(modifier = Modifier.fillMaxHeight().fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.theme),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Background image and main content
        Column(modifier = Modifier.fillMaxSize()) {

            // Main content layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f) // Takes up remaining space above the BottomNavBar
                    .background(Color.Transparent)
                    .padding(16.dp)
            ) {

                // Google Map with SOS button overlay
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .fillMaxWidth()
                ) {


                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                    ) {

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(zoomControlsEnabled = false),
                            properties = MapProperties(mapType = mapType)
                        ) {
                            // Define the marker state with the current location
                            Marker(
                                state = markerState,
                                title = "You are here",
                                snippet = "Current Location",
                            )
                        }

                        // Top bar with icons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Profile icon aligned on the left
                            IconButton(onClick = { navController.navigate("dashboard") }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_profile),
                                    contentDescription = "Profile",
                                    tint = Color.Black,
                                    modifier = Modifier.background(Color.White, CircleShape).size(48.dp)
                                )
                            }

                            // Column to stack Settings and Map Type Toggle Button, aligned on the right
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { isDrawerVisible = !isDrawerVisible }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_settings),
                                        contentDescription = "Settings",
                                        tint = Color.Black,
                                        modifier = Modifier.background(Color.White, CircleShape).size(48.dp)
                                    )
                                }

                                // Spacer to add padding between the Settings icon and Map Type Toggle Button
                                Spacer(modifier = Modifier.height(8.dp)) // Adjust this height as needed

                                // Icon Button to toggle the Map Type Selector Card
                                IconButton(
                                    onClick = { showMapTypeSelector = true }, // Show the card when clicked
                                    modifier = Modifier
                                        .background(Color.White, CircleShape)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.map_type), // Custom icon for map type
                                        contentDescription = "Map Type",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        // Re-center Button
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f))
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .background(Color.White, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.my_location),
                                contentDescription = "My Location",
                                tint = Color.Black
                            )
                        }

                        // Map Type Selector Card (shown when showMapTypeSelector is true)
                        if (showMapTypeSelector) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                                    .background(Color.White, shape = RoundedCornerShape(12.dp)),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Select Map Type",
                                        fontSize = 18.sp,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Map type options
                                    MapTypeOption("Normal View") {
                                        mapType = MapType.NORMAL
                                        showMapTypeSelector = false
                                    }
                                    MapTypeOption("Satellite View") {
                                        mapType = MapType.SATELLITE
                                        showMapTypeSelector = false
                                    }
                                    MapTypeOption("Terrain View") {
                                        mapType = MapType.TERRAIN
                                        showMapTypeSelector = false
                                    }
                                }
                            }
                        }


                    }

                    // SOS Button, positioned at the bottom of the map
                    Button(
                        onClick = {
                            if (hasLocationPermission) {
                                showModal = true
                                startTimer()
                            } else {
                                Log.e("SOS_TICKET", "Permission not granted")
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-20).dp)
                            .size(150.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = CircleShape,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp, // Adjust this value for more or less elevation
                            pressedElevation = 12.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        Text(text = "SOS", color = Color.White, fontSize = 40.sp)
                    }

                }

                if (showModal && !isRequestSent && isProcessCompleted) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Send SOS?") },
                        text = {
                            Column {
                                Text("SOS will be sent in $countdown seconds.")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Do you want to cancel or confirm?")

                                if (isLoading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CircularProgressIndicator() // Show loading indicator when waiting for response
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isLoading = true
                                    isProcessCompleted = false // Start SOS process
                                    sendSOSManually() // Trigger SOS request
                                },
                                enabled = !isLoading
                            ) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = {
                                    isSOSCanceled = true
                                    showModal = false
                                    countdownTimer?.cancel()
                                    isRequestSent = false
                                    isLoading = false
                                    isProcessCompleted = true // Reset process completion on cancel
                                },
                                enabled = !isLoading
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // Bottom Navigation Bar
            BottomNavBar(navController)
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

@Composable
fun BottomNavBar(navController: NavHostController) {
    val context = LocalContext.current
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { navController.navigate("home") },
            icon = { Icon(
                painter = painterResource(id = R.drawable.home), // Use your custom drawable resource
                contentDescription = "Home"
            ) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                Toast.makeText(context, "Ongoing Work", Toast.LENGTH_SHORT).show()
            },
            icon = { Icon(
                painter = painterResource(id = R.drawable.search), // Use your custom drawable resource
                contentDescription = "Home"
            ) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, WatchActivity::class.java)
                context.startActivity(intent)
            },
            icon = { Icon(
                painter = painterResource(id = R.drawable.watch), // Use your custom drawable resource
                contentDescription = "Home"
            ) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                Toast.makeText(context, "Ongoing Work", Toast.LENGTH_SHORT).show()
            },
            icon = { Icon(
                painter = painterResource(id = R.drawable.notifications), // Use your custom drawable resource
                contentDescription = "Home"
            ) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                Toast.makeText(context, "Ongoing Work", Toast.LENGTH_SHORT).show()
            },
            icon = { Icon(
                painter = painterResource(id = R.drawable.history), // Use your custom drawable resource
                contentDescription = "Home"
            ) }
        )
    }
}

// Function to get current timestamp
fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return sdf.format(Date())
}

@Composable
fun MapTypeOption(name: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = name, fontSize = 16.sp, color = Color.Black)
    }
}