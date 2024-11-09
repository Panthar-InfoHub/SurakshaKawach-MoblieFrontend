package com.nextlevelprogrammers.surakshakawach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextlevelprogrammers.surakshakawach.ui.theme.SurakshaKawachTheme

class WatchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SurakshaKawachTheme {
                WatchScreen(modifier = Modifier.background(Color(0xFF121212)), onBackPress = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(modifier: Modifier = Modifier, onBackPress: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "January 2024", color = Color.White, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E2E))
            )
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
                    FitnessSummaryCard()
                    Spacer(modifier = Modifier.height(16.dp))
                    HealthTrackerCard()
                    Spacer(modifier = Modifier.height(16.dp))
                    HealthGoalsCard()
                }
            }
        }
    )
}

@Composable
fun FitnessSummaryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row with date and dropdown arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Jan 2024",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_drop_down),
                    contentDescription = "Dropdown",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large circular progress chart
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                CircularProgressIndicatorComponent(0.6f, Color.Blue, 12f) // Outer ring for Calories
                CircularProgressIndicatorComponent(0.4f, Color(0xFFFF9800), 12f) // Middle ring for Move minutes
                CircularProgressIndicatorComponent(0.2f, Color.Yellow, 12f) // Inner ring for Weekly Goal
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom row with icons, values, and labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    icon = R.drawable.ic_calories, // Replace with your calorie icon
                    value = "30/50",
                    label = "Calories",
                    color = Color.Blue
                )
                MetricItem(
                    icon = R.drawable.ic_move_minutes, // Replace with your move minutes icon
                    value = "20/50",
                    label = "Move minutes",
                    color = Color(0xFFFF9800)
                )
                MetricItem(
                    icon = R.drawable.weekly, // Replace with your weekly goal icon
                    value = "15/50",
                    label = "Weekly Goal",
                    color = Color.Yellow
                )
            }
        }
    }
}

@Composable
fun CircularProgressIndicatorComponent(f: Float, blue: Color, f1: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp) // Adjust size as needed
    ) {
        // Outer ring for Calories
        SingleArc(progress = 0.6f, color = Color.Blue, size = 120.dp, strokeWidth = 12f)
        // Middle ring for Move Minutes with a slight size reduction to create a gap
        SingleArc(progress = 0.4f, color = Color(0xFFFF9800), size = 100.dp, strokeWidth = 12f)
        // Inner ring for Weekly Goal with further size reduction for another gap
        SingleArc(progress = 0.2f, color = Color.Yellow, size = 80.dp, strokeWidth = 12f)
    }
}

@Composable
fun SingleArc(progress: Float, color: Color, size: Dp, strokeWidth: Float) {
    Canvas(modifier = Modifier.size(size)) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360 * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MetricItem(icon: Int, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(text = value, fontSize = 14.sp, color = Color.White)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun HealthTrackerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Title and subtitle
            Text(
                text = "Health Tracker",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Last 7 days",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Achieved progress text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "3/7",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF42A5F5) // Blue color for progress
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Achieved",
                    fontSize = 14.sp,
                    color = Color(0xFF42A5F5)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days of the week with status indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val days = listOf("F", "S", "S", "M", "T", "W", "T")
                val achievedStatus = listOf(true, true, false, true, true, false, true) // Example status for each day

                days.forEachIndexed { index, day ->
                    DayStatusIndicator(day = day, isAchieved = achievedStatus[index])
                }
            }
        }
    }
}

@Composable
fun DayStatusIndicator(day: String, isAchieved: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(30.dp) // Adjust size if needed
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                drawCircle(
                    color = if (isAchieved) Color(0xFF42A5F5) else Color(0xFF212121), // Blue for achieved, dark gray for not achieved
                    radius = size.minDimension / 2
                )
            }
            if (isAchieved) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check), // Replace with your check icon
                    contentDescription = "Achieved",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = Color(0xFFFF9800), radius = size.minDimension / 2) // Orange inner circle for not achieved
                }
            }
        }
        Text(day, fontSize = 10.sp, color = Color.LightGray)
    }
}

@Composable
fun HealthGoalsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Your Health Goals",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            GoalBar(label = "Calories", percentage = 75)
            GoalBar(label = "Steps Taken", percentage = 80)
            GoalBar(label = "Active Minutes", percentage = 65)
        }
    }
}

@Composable
fun GoalBar(label: String, percentage: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text("$percentage%", color = Color.White, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            val progressWidth = size.width * (percentage / 100f)
            drawRoundRect(
                color = Color.Gray,
                size = Size(size.width, 8f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = Color(0xFFFF9800),
                size = Size(progressWidth, 8f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}