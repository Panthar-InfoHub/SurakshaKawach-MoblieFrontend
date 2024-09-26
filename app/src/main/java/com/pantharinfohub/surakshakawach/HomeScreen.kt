package com.pantharinfohub.surakshakawach

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
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
                IconButton(onClick = { /* Handle profile click */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_profile), // Replace with your icon resource
                        contentDescription = "Profile",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = { /* Handle location click */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location), // Replace with your icon resource
                        contentDescription = "Location",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = {
                    isDrawerVisible = !isDrawerVisible // Toggle the drawer visibility
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings), // Replace with your icon resource
                        contentDescription = "Settings",
                        tint = Color.Black
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
                        .height(460.dp)
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

                        // Handle SOS click, get the current location and send it to the server
                        if (hasLocationPermission) {
                            Log.d("SOS_TICKET", "Permission granted, attempting to get location")

                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    val latitude = it.latitude
                                    val longitude = it.longitude
                                    val timestamp = getCurrentTimestamp()

                                    Log.d("SOS_TICKET", "Location acquired: Latitude: $latitude, Longitude: $longitude")
                                    Log.d("SOS_TICKET", "Preparing to send SOS ticket to backend")

                                    // Create SOS ticket and send to backend
                                    coroutineScope.launch {
                                        val api = Api() // Instantiate the API class
                                        Log.d("SOS_TICKET", "Calling API to send SOS ticket")

                                        val success = api.sendSosTicket(
                                            firebaseUID = firebaseUID, // Use the actual Firebase UID from the current session
                                            latitude = latitude.toString(),
                                            longitude = longitude.toString(),
                                            timestamp = timestamp
                                        )

                                        if (success) {
                                            Log.d("SOS_TICKET", "SOS ticket created successfully")
                                        } else {
                                            Log.e("SOS_TICKET", "Failed to create SOS ticket")
                                        }
                                    }
                                } ?: run {
                                    Log.e("SOS_TICKET", "Location is null")
                                }
                            }.addOnFailureListener { exception ->
                                Log.e("SOS_TICKET", "Failed to get location: ${exception.message}")
                            }
                        } else {
                            Log.e("SOS_TICKET", "Permission not granted")
                            // Handle permission request if necessary
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-30).dp)
                        .height(90.dp)
                        .width(110.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "SOS", color = Color.White)
                }
            }

            // Favourites section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Favourites", color = Color.Black)
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_profile), // Replace with your icon resource
                            contentDescription = "Favorite 1",
                            modifier = Modifier.size(60.dp),
                            tint = Color.Magenta
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_profile), // Replace with your icon resource
                            contentDescription = "Favorite 2",
                            modifier = Modifier.size(60.dp),
                            tint = Color.Cyan
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_profile), // Replace with your icon resource
                            contentDescription = "Favorite 3",
                            modifier = Modifier.size(60.dp),
                            tint = Color.Black
                        )
                    }
                }
            }

            // Bottom navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { /* Handle home click */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home), // Replace with your icon resource
                        contentDescription = "Home",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = { /* Handle another screen click */ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home), // Replace with your icon resource
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
                        Text(text = "Add Emergency Contacts", color = Color.White)
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
