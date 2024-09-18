package com.pantharinfohub.surakshakawach

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SOSButtonScreen {
                // Action when the SOS button is clicked
                Toast.makeText(this, "SOS Button Pressed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun SOSButtonScreen(onSOSClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Round shaped SOS button
        Button(
            onClick = { onSOSClick() },
            modifier = Modifier
                .size(150.dp) // This makes the button round
                .clip(CircleShape)
                .background(Color.Red),
        ) {
            Text(
                text = "SOS",
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SOSButtonPreview() {
    SOSButtonScreen(onSOSClick = {})
}
