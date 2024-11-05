package com.nextlevelprogrammers.surakshakawach

import Api
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.nextlevelprogrammers.surakshakawach.api.Coordinates
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme
import kotlinx.coroutines.delay
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
    var userName by remember { mutableStateOf("Unknown User") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var coordinates by remember { mutableStateOf<Coordinates?>(null) }
    var selectedTab by remember { mutableStateOf("Map") }
    val coroutineScope = rememberCoroutineScope()

    // Fetch ticket and user data initially
    LaunchedEffect(ticketId, firebaseUID) {
        if (ticketId != null && firebaseUID != null) {
            coroutineScope.launch {
                val ticketInfo = Api().fetchTicketStatus(firebaseUID, ticketId)
                ticketStatus = ticketInfo?.status ?: "Unknown"
                userName = ticketInfo?.userName ?: "Unknown User"
                coordinates = Api().fetchLatestLocation(firebaseUID, ticketId)
            }
        }
    }

    // Fetch the latest location coordinates every 10 seconds
    LaunchedEffect(ticketId, firebaseUID) {
        if (ticketId != null && firebaseUID != null) {
            while (true) {
                coroutineScope.launch {
                    coordinates = Api().fetchLatestLocation(firebaseUID, ticketId)
                }
                delay(10000L) // 10-second delay
            }
        }
    }

    // Determine the color of the border ring based on ticket status
    val statusColor = when (ticketStatus) {
        "closed" -> Color.Red
        "active" -> Color.Green
        else -> Color.Gray
    }

    // Main layout with full-screen map and overlays
    Box(modifier = Modifier.fillMaxSize()) {
        // Map View or Multimedia View based on the selected tab
        if (selectedTab == "Map") {
            coordinates?.let {
                FullScreenMap(latitude = it.latitude, longitude = it.longitude, userName = userName)
            } ?: Text(
                text = "Location data unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            )
        } else {
            // Placeholder for Multimedia content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Multimedia Content", color = Color.White, fontSize = 20.sp)
            }
        }

        // Top overlay with profile and status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE3F2FD), Color(0xFFC8E6C9))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Profile Image with Status Dot
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    painter = rememberImagePainter(
                        data = profileImageUrl ?: "https://via.placeholder.com/150",
                        builder = {
                            transformations(CircleCropTransformation())
                        }
                    ),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )

                // Status Dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, shape = CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(4.dp, (-4).dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Name
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Bottom Navigation Bar for switching views
        BottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun FullScreenMap(latitude: Double, longitude: Double, userName: String) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 17f) // Default zoom level
    }

    val markerState = remember { MarkerState(position = LatLng(latitude, longitude)) }
    var isZoomInitialized by remember { mutableStateOf(false) } // Track if zoom is already set

    // Update marker position if coordinates change, but keep the initial zoom level
    LaunchedEffect(latitude, longitude) {
        markerState.position = LatLng(latitude, longitude)
        if (!isZoomInitialized) { // Set zoom level only once
            cameraPositionState.position = CameraPosition.fromLatLngZoom(markerState.position, 17f)
            isZoomInitialized = true // Mark zoom as initialized
        } else {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(markerState.position, cameraPositionState.position.zoom)
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true),
        properties = MapProperties(mapType = MapType.NORMAL)
    ) {
        // Add a custom marker at the user's location
        Marker(
            state = markerState,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
            title = userName,
            snippet = "User's location"
        )
    }
}

@Composable
fun BottomNavBar(selectedTab: String, onTabSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
    ) {
        NavigationBarItem(
            selected = selectedTab == "Map",
            onClick = { onTabSelected("Map") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.map_type), // Replace with your actual drawable resource
                    contentDescription = "Map"
                )
            },
            label = { Text("Map") }
        )
        NavigationBarItem(
            selected = selectedTab == "Multimedia",
            onClick = { onTabSelected("Multimedia") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.media), // Replace with your actual drawable resource
                    contentDescription = "Multimedia"
                )
            },
            label = { Text("Multimedia") }
        )
    }
}