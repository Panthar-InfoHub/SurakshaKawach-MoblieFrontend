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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.pantharinfohub.surakshakawach.api.Api
import com.pantharinfohub.surakshakawach.ui.theme.PurpleGradient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginScreen : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Set up Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set the UI content
        setContent {
            ProvideWindowInsets {
                LoginScreenUI(
                    onGenderSelected = { gender ->
                        selectedGender = gender
                    },
                    onLoginClick = {
                        if (selectedGender == null) {
                            Toast.makeText(this, "Please select a gender before proceeding", Toast.LENGTH_SHORT).show()
                        } else {
                            signInWithGoogle()
                        }
                    }
                )
            }
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task.result)
        } else {
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userName = account.displayName
                        val userEmail = account.email
                        sendUserDataToBackend(userName, userEmail, selectedGender!!)
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun sendUserDataToBackend(name: String?, email: String?, gender: String) {
        // Get Firebase UID
        val firebaseUID = auth.currentUser?.uid

        if (firebaseUID != null && name != null && email != null) {
            // Call the API to create the user on the backend
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val api = Api()
                    val success = api.createUser(firebaseUID, name, email, gender)

                    // Handle response on the main thread
                    withContext(Dispatchers.Main) {
                        if (success) {
                            // If user creation is successful, store the flag in SharedPreferences
                            UserPreferences.setUserCreated(this@LoginScreen, true)

                            // Navigate to the next screen
                            val intent = Intent(this@LoginScreen, PermissionActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Handle failure (e.g., show a toast message)
                            Toast.makeText(this@LoginScreen, "Failed to create user. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // Handle exception on the main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginScreen, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            // Handle null cases for firebaseUID, name, or email
            Toast.makeText(this, "Invalid user data.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun LoginScreenUI(onGenderSelected: (String) -> Unit, onLoginClick: () -> Unit) {
    var selectedGender by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleGradient)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.suraksha_kawach_logo),
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

        // Gender Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    selectedGender = "male"
                    onGenderSelected("male")
                },
                modifier = Modifier.weight(1f).padding(8.dp),
                colors = if (selectedGender == "male") ButtonDefaults.buttonColors(containerColor = Color.Blue)
                else ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(text = "Male", color = Color.White)
            }

            Button(
                onClick = {
                    selectedGender = "female"
                    onGenderSelected("female")
                },
                modifier = Modifier.weight(1f).padding(8.dp),
                colors = if (selectedGender == "female") ButtonDefaults.buttonColors(containerColor = Color.Blue)
                else ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text(text = "Female", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "Continue with Google")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}