package com.nextlevelprogrammers.surakshakawach.ui

import Api
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
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
import com.nextlevelprogrammers.surakshakawach.VoiceRecognitionService
import com.nextlevelprogrammers.surakshakawach.WatchActivity
import com.nextlevelprogrammers.surakshakawach.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, fusedLocationClient: FusedLocationProviderClient,homeViewModel: HomeViewModel = viewModel() ) {
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showModal by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    var isSOSCanceled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var countdownTimer: CountDownTimer? = null
    var isLoading by remember { mutableStateOf(false) }
    var isProcessCompleted by remember { mutableStateOf(true) }
    var isRequestSent by remember { mutableStateOf(false) }
    var isSOSButtonEnabled by remember { mutableStateOf(true) }
    val triggerSOS by homeViewModel.triggerSOS.collectAsState()

    val markerState = remember { MarkerState(position = LatLng(0.0, 0.0)) }
    var showMapTypeSelector by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
//    var isMapLoaded by remember { mutableStateOf(true) } // Track map loading state
    val cameraPositionState = rememberCameraPositionState()

    // Firebase UID
    val firebaseAuth = FirebaseAuth.getInstance()
    val firebaseUID = firebaseAuth.currentUser?.uid

    // Store ticket ID after the first ticket is created
    var sosTicketId: String? = null
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Request permission for location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { currentLocation = LatLng(it.latitude, it.longitude) }
                val intent = Intent(context, VoiceRecognitionService::class.java)
                context.startService(intent)
//                isMapLoaded = true // Set map as loaded once location is retrieved
            }
        } else {
            Log.e("LocationPermission", "Permission denied")
            Log.e("VoiceRecognitionService", "RECORD_AUDIO permission denied.")
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
        currentLocation?.let {
            markerState.position = it
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
        }
    }

    // Launch permission request
    LaunchedEffect(Unit) {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            // Start service directly if permission already granted
            val intent = Intent(context, VoiceRecognitionService::class.java)
            context.startService(intent)
        }
    }

    DisposableEffect(Unit) {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.firstOrNull()?.let { location ->
                    currentLocation = LatLng(location.latitude, location.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            },
            locationCallback,
            Looper.getMainLooper()
        )

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
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
                latitude = currentLocation?.latitude.toString(),
                longitude = currentLocation?.longitude.toString(),
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

    LaunchedEffect(triggerSOS) {
        if (triggerSOS) {
            if (currentLocation != null) {
                homeViewModel.resetSOS()
                startTimer()
            } else {
                Log.e("SOS", "Location not available.")
            }
        }
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
            coroutineScope.launch {
                val intent = Intent(context, SOSActivity::class.java)
                context.startActivity(intent)
                sendSOS()
                isRequestSent = false // Reset after sending SOS
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onProfileClicked = { /* Handle Profile click */ },
                onEmergencyContactsClicked = {
                    coroutineScope.launch {
                        navController.navigate("emergency_contacts")
                        drawerState.close()
                    }
                },
                onLogoutClicked = { /* Handle Logout click */ },
                onHelpClicked = { /* Handle Help click */ },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Suraksha Kawach") },
                    actions = {
                        IconButton(onClick = { navController.navigate("dashboard") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_profile),
                                contentDescription = "User Profile",
                                tint = Color.Black
                            )
                        }
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Settings",
                                tint = Color.Black
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(navController)
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .weight(1f)
                ) {
                    if (currentLocation != null) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(currentLocation!!, 15f)
                            },
                            uiSettings = MapUiSettings(zoomControlsEnabled = false),
                            properties = MapProperties(mapType = mapType)
                        ) {
                            Marker(
                                state = markerState,
                                title = "You are here",
                                snippet = "Real-time Location"
                            )
                        }
                    } else {
                        ShimmerEffect()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { showMapTypeSelector = true },
                            modifier = Modifier
                                .background(Color.White, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.map_type),
                                contentDescription = "Map Type",
                                tint = Color.Black
                            )
                        }
                    }

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

                                MapTypeOption("Normal View") { mapType = MapType.NORMAL; showMapTypeSelector = false }
                                MapTypeOption("Satellite View") { mapType = MapType.SATELLITE; showMapTypeSelector = false }
                                MapTypeOption("Terrain View") { mapType = MapType.TERRAIN; showMapTypeSelector = false }
                                MapTypeOption("Hybrid View") { mapType = MapType.HYBRID; showMapTypeSelector = false }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (hasLocationPermission) {
                            showModal = true
                            startTimer()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 24.dp)
                        .size(150.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = CircleShape
                ) {
                    Text(text = "SOS", color = Color.White, fontSize = 40.sp)
                }

                if (showModal && !isRequestSent && isProcessCompleted) {
                    SOSConfirmationDialog(
                        countdown = countdown,
                        isLoading = isLoading,
                        onConfirm = {
                            isLoading = true
                            isProcessCompleted = false
                            sendSOSManually() // Trigger SOS request
                            sendSOS()
                        },
                        onDismiss = {
                            showModal = false
                            isLoading = false
                            isProcessCompleted = true
                            countdownTimer?.cancel()
                            isRequestSent = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SOSConfirmationDialog(
    countdown: Int,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send SOS?") },
        text = {
            Column {
                Text("SOS will be sent in $countdown seconds.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Do you want to cancel or confirm?")
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DrawerContent(
    onProfileClicked: () -> Unit,
    onEmergencyContactsClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onHelpClicked: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            // Drawer Header with close button
            DrawerHeader(onCloseDrawer)

            DividerItem()

            // Drawer Items
            DrawerItem(text = "Profile", onClick = onProfileClicked)
            DrawerItem(text = "Emergency Contacts", onClick = onEmergencyContactsClicked)
            DrawerItem(text = "Logout", onClick = onLogoutClicked)
            Spacer(modifier = Modifier.weight(1f)) // Pushes the Help item to the bottom
            DrawerItem(text = "Help", onClick = onHelpClicked)
        }
    }
}

@Composable
fun DrawerHeader(onCloseDrawer: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_profile),
            contentDescription = "Profile Icon",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "User's Name",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCloseDrawer) {
            Icon(
                painter = painterResource(id = R.drawable.map_type),
                contentDescription = "Close Drawer"
            )
        }
    }
}

@Composable
fun DrawerItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.map_type), // Replace with appropriate icons
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DividerItem(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val context = LocalContext.current
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognitionService(context)
        } else {
            Log.e("HomeScreen", "Microphone permission denied.")
            Toast.makeText(context, "Microphone permission is required for voice recognition.", Toast.LENGTH_LONG).show()
        }
    }
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
                val hasAudioPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasAudioPermission) {
                    startVoiceRecognitionService(context)
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                    contentDescription = "Start Voice Recognition"
                )
            }
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

@Composable
fun ShimmerEffect() {
    // Shimmer or skeleton effect for loading state
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.3f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.Gray)
    }
}
fun startVoiceRecognitionService(context: Context) {
    val intent = Intent(context, VoiceRecognitionService::class.java)
    context.startService(intent)
    Log.d("HomeScreen", "VoiceRecognitionService started for testing.")
}