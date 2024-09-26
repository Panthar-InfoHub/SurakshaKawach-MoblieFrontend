package com.pantharinfohub.surakshakawach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                LineGraph(data = heartRateData, lineColor = Color.Red, strokeColor = Color.DarkGray)

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
                LineGraph(data = stepsData, lineColor = Color.Green, strokeColor = Color.DarkGray)
            }
        }
    )
}

@Composable
fun LineGraph(
    data: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.Blue,
    strokeColor: Color = Color.Black, // Stroke color for the border
    strokeWidth: Float = 4f,
    strokeBorderWidth: Float = 6f // Stroke border width
) {
    val maxValue = data.maxOrNull() ?: 0
    val minValue = data.minOrNull() ?: 0

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(200.dp)
        .background(Color.LightGray)) {

        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * (size.width / (data.size - 1))
                val y = size.height - (value.toFloat() / maxValue * size.height)

                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        // Draw stroke border
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(
                width = strokeBorderWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw main line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SurakshaKawachTheme {
        WatchScreen(modifier = Modifier.background(Color.White))
    }
}