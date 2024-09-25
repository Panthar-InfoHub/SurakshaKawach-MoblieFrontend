package com.pantharinfohub.surakshakawach

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pantharinfohub.surakshakawach.ui.theme.PurpleGradient

class LoginScreen : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("LoginScreen", "FirebaseApp initialized: ${FirebaseApp.getInstance().name}")

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()
        Log.d("LoginScreen", "FirebaseAuth initialized")

        // Set up Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d("LoginScreen", "GoogleSignInClient initialized")

        // Set the UI content
        setContent {
            ProvideWindowInsets {
                LoginScreenUI {
                    Log.d("LoginScreen", "Login button clicked, starting Google Sign-In")
                    signInWithGoogle()
                }
            }
        }
    }

    // Launcher for Google Sign-In activity
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "Google Sign-In activity result received")
        if (result.resultCode == RESULT_OK) {
            Log.d("LoginScreen", "Google Sign-In successful, handling result")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task.result)
        } else {
            Log.e("LoginScreen", "Google Sign-In failed, resultCode: ${result.resultCode}")
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Start Google Sign-In process
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        Log.d("LoginScreen", "Launching Google Sign-In intent")
        googleSignInLauncher.launch(signInIntent)
    }

    // Handle the result of the Google Sign-In
    private fun handleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            Log.d("LoginScreen", "Google Sign-In successful, signing in to Firebase with account: ${it.email}")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("LoginScreen", "Firebase authentication successful, navigating to HomeActivity")
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("LoginScreen", "Firebase authentication failed: ${task.exception?.message}")
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        } ?: run {
            Log.e("LoginScreen", "Google account is null, sign-in failed")
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun LoginScreenUI(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleGradient) // Assuming you have a gradient setup
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Suraksha Kawach Logo
        val logoPainter: Painter = painterResource(id = R.drawable.suraksha_kawach_logo) // Replace with your logo
        Image(
            painter = logoPainter,
            contentDescription = "App Logo",
            modifier = Modifier.size(300.dp)
        )

        // Intro Text
        Text(
            text = "Suraksha Kawach is a safety app designed to protect and alert your loved ones during emergencies.",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Subtext
        Text(
            text = "Ensure your safety with just a click of a button",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Google Sign-In Button
        Button(
            onClick = {
                Log.d("LoginScreenUI", "Sign-In button clicked")
                onLoginClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "Continue with Google")
        }

        // Spacer
        Spacer(modifier = Modifier.height(16.dp))

        // Age and Gender Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { /* TODO: Handle Age Click */ },
                modifier = Modifier.weight(1f).padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(text = "AGE", color = Color.White)
            }

            Button(
                onClick = { /* TODO: Handle Gender Click */ },
                modifier = Modifier.weight(1f).padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(text = "GENDER", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreenUI(onLoginClick = {})
}
