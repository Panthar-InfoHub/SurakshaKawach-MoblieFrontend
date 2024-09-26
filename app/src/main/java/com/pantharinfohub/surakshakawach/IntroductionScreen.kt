package com.pantharinfohub.surakshakawach

import android.content.Intent
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.pantharinfohub.surakshakawach.api.Api
import com.pantharinfohub.surakshakawach.ui.theme.PurpleGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IntroductionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val api = Api()
    var isUserExists by remember { mutableStateOf<Boolean?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    val descriptions = listOf(
        "Suraksha Kawach ensures your personal safety",
        "Quick emergency alerts and live sharing"
    )
    val scope = rememberCoroutineScope()

    // Check if user is already created using SharedPreferences
    val isUserCreatedInPrefs = remember {
        UserPreferences.isUserCreated(context)
    }

    // If user is already created, directly navigate to HomeActivity
    LaunchedEffect(key1 = Unit) {
        if (isUserCreatedInPrefs) {
            Log.d("IntroductionScreen", "User already created. Navigating to HomeActivity")
            context.startActivity(Intent(context, HomeActivity::class.java))
        } else {
            // Get the UID from Firebase session if available
            val firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUID = firebaseAuth.currentUser?.uid

            if (firebaseUID != null) {
                Log.d("IntroductionScreen", "Firebase UID found: $firebaseUID")
                try {
                    // Check if the user exists in the backend
                    val userExists = api.checkIfUserExists(firebaseUID)
                    Log.d("IntroductionScreen", "User exists: $userExists")

                    isUserExists = userExists

                    if (userExists) {
                        // Set the flag in SharedPreferences
                        UserPreferences.setUserCreated(context, true)
                        // Navigate to HomeActivity
                        context.startActivity(Intent(context, HomeActivity::class.java))
                    }
                } catch (e: Exception) {
                    Log.e("IntroductionScreen", "Error checking user existence: ${e.message}")
                    isUserExists = false
                }
            } else {
                Log.d("IntroductionScreen", "Firebase UID is null. User not logged in.")
                isUserExists = false // User is not logged in
            }
        }
    }

    LaunchedEffect(key1 = currentPage) {
        delay(3000L)
        currentPage = (currentPage + 1) % descriptions.size
    }

    // Show the introduction screen if the user doesn't exist in the backend or is not authenticated
    if (isUserExists == false || !isUserCreatedInPrefs) {
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
                    repeat(2) { index ->
                        val color by animateColorAsState(
                            targetValue = if (index == currentPage) Color.White else Color.Gray,
                            label = ""
                        )
                        val width = if (index == currentPage) 36.dp else 24.dp
                        val height = 6.dp

                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .width(width)
                                .height(height)
                                .background(
                                    color = color,
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Get Started Button
                Button(
                    onClick = {
                        // Navigate to the LoginScreen if user doesn't exist
                        val intent = Intent(context, LoginScreen::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp)
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
}
