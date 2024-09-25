package com.pantharinfohub.surakshakawach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

class HomeScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    // Define the initial position (latitude, longitude) and zoom level
    val initialPosition = LatLng(25.4484, 78.5685) // Coordinates for Jhansi, India
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 14f) // Set zoom level to 14
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
            IconButton(onClick = { /* Handle settings click */ }) {
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
                .weight(1.5f) // Adjust weight to reduce the height of the map
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp) // Padding around the map
        ) {
            // Google Map with rounded corners
            Box(
                modifier = Modifier
                    .height(460.dp) // Set explicit height for the map
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)) // Add rounded corners to the map
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                )
            }

            // SOS Button, positioned at the bottom of the map, partially cutting through
            Button(
                onClick = { /* Handle SOS click */ },
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align to the bottom center of the map
                    .offset(y = (-30).dp) // Move it down by 50.dp to cut through the bottom of the map
                    .height(90.dp).width(110.dp), // Adjust button size
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp) // Round the button's corners
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
}