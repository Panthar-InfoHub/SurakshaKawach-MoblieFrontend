package com.pantharinfohub.surakshakawach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.pantharinfohub.surakshakawach.ui.theme.SurakshaKawachTheme
import kotlin.random.Random

class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SurakshaKawachTheme {
                WatchScreen(modifier = Modifier.background(Color.White))
            }
        }
    }
}

// Function to generate mock heart rate data for the day
fun generateMockHeartRateData(): List<Int> {
    return List(10) { Random.nextInt(60, 100) } // Mock heart rate data between 60 and 100 BPM
}

// Function to generate mock steps data for the day
fun generateMockStepsData(): List<Int> {
    return List(10) { Random.nextInt(3000, 10000) } // Mock steps data between 3000 and 10000 steps
}

// Function to generate mock location data for the heatmap in India
fun generateMockLocationData(): List<LatLng> {
    return listOf(
        LatLng(28.6139, 77.2090), // New Delhi
        LatLng(19.0760, 72.8777), // Mumbai
        LatLng(22.5726, 88.3639), // Kolkata
        LatLng(13.0827, 80.2707), // Chennai
        LatLng(12.9716, 77.5946), // Bangalore
        LatLng(17.3850, 78.4867), // Hyderabad
        LatLng(23.0225, 72.5714), // Ahmedabad
        LatLng(26.9124, 75.7873), // Jaipur
        LatLng(21.1702, 72.8311), // Surat
        LatLng(15.2993, 74.1240)  // Goa
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(modifier: Modifier = Modifier) {
    val heartRateData by remember { mutableStateOf(generateMockHeartRateData()) }
    val stepsData by remember { mutableStateOf(generateMockStepsData()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Health Watch Data", color = Color.Black) })
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "Heart Rate",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "BPM: ${heartRateData.last()}",
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BarChart(data = heartRateData, barColor = Color.Red)

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Steps",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Total Steps: ${stepsData.sum()}",
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BarChart(data = stepsData, barColor = Color.Green)

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Crowd Location Heatmap",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    HeatMapOverlay(points = generateMockLocationData())
                }
            }
        }
    )
}

// BarChart composable function
@Composable
fun BarChart(
    data: List<Int>,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Blue,
    maxBarHeight: Float = 200f,  // Max height of the bars
    barWidth: Float = 40f,  // Width of the bars
    barSpacing: Float = 16f // Spacing between the bars
) {
    val maxValue = data.maxOrNull() ?: 0

    // Canvas to draw the bar chart
    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(200.dp)) {

        data.forEachIndexed { index, value ->
            // Calculate the height of the bar relative to the maximum value
            val barHeight = (value.toFloat() / maxValue * maxBarHeight)
            val xOffset = index * (barWidth + barSpacing)

            // Draw each bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = xOffset, y = size.height - barHeight),
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Optionally, draw value labels at the top of each bar
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    value.toString(),
                    xOffset + (barWidth / 4), // Center text over the bar
                    size.height - barHeight - 10, // Adjust position of text above the bar
                    android.graphics.Paint().apply {
                        textSize = 30f
                        color = android.graphics.Color.BLACK
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )
            }
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun HeatMapOverlay(points: List<LatLng>) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 4.5f) // Centered in India
    }

    // Create weighted LatLngs for heatmap
    val weightedLatLngs = points.map { WeightedLatLng(it, Random.nextDouble(0.5, 1.5)) }
    val heatmapTileProvider = remember(weightedLatLngs) {
        HeatmapTileProvider.Builder()
            .weightedData(weightedLatLngs)
            .radius(50)  // Adjust radius
            .build()
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        cameraPositionState = cameraPositionState,
        onMapClick = {
            // Placeholder for map click, if needed
        },
        onMapLongClick = {
            // Placeholder for map long-click, if needed
        }
    ) {
        // Add heatmap tile overlay to GoogleMap
        MapEffect(heatmapTileProvider) { map ->
            map.addTileOverlay(TileOverlayOptions().tileProvider(heatmapTileProvider))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SurakshaKawachTheme {
        WatchScreen(modifier = Modifier.background(Color.White))
    }
}