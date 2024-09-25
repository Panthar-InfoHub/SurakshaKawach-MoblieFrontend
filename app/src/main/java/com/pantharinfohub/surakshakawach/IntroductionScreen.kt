package com.pantharinfohub.surakshakawach

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pantharinfohub.surakshakawach.ui.theme.PurpleGradient
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IntroductionScreen(modifier: Modifier, currentPage: Int = 0, totalDots: Int = 2) {
    val context = LocalContext.current // Get the current context to create an Intent
    var currentPage by remember { mutableIntStateOf(0) }

    val descriptions = listOf(
        "Suraksha Kawach ensures your personal safety",
        "Quick emergency alerts and live sharing"
    )

    LaunchedEffect(key1 = currentPage) {
        delay(3000L)
        currentPage = (currentPage + 1) % descriptions.size
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Suraksha Kawach Logo
            Image(
                painter = painterResource(id = R.drawable.suraksha_kawach_logo), // Replace with your image resource
                contentDescription = "Suraksha Kawach Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(25.dp))

            // Middle carousel with animated text
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = descriptions[currentPage],
                    transitionSpec = {
                        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) using
                                SizeTransform(clip = false)
                    },
                    label = ""
                ) { description ->
                    Text(
                        text = description,
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Row container for pagination dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Iterate through totalDots and apply dynamic changes based on the currentPage
                repeat(totalDots) { index ->
                    // Change color based on whether the dot is the currently selected one or not
                    val color by animateColorAsState(
                        targetValue = if (index == currentPage) Color.White else Color.Gray,
                        label = ""
                    )

                    // Change size based on whether it's the focused (current) dot or not
                    val width = if (index == currentPage) 36.dp else 24.dp
                    val height = 6.dp // Keep height the same

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .width(width)
                            .height(height)
                            .background(
                                color = color,
                                shape = RoundedCornerShape(3.dp) // Rounded corners for rectangle shape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Get Started Button
            Button(
                onClick = {
                    // Navigate to the LoginScreen when the button is clicked
                    val intent = Intent(context, LoginScreen::class.java)
                    context.startActivity(intent) // Start the login screen
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp) // Rounded button for better aesthetics
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun IntroductionScreenPreview() {
    IntroductionScreen(modifier = Modifier)
}