package com.droidphotos

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.droidphotos.ui.theme.DroidPhotosTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var isSignedIn by mutableStateOf(false)
    private var signInError: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Google Sign-In for Photos Library scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if user is already signed in
        isSignedIn = GoogleSignIn.getLastSignedInAccount(this) != null

        // Activity Result launcher for Google Sign-In
        val signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    task.getResult(ApiException::class.java)
                    isSignedIn = true
                    signInError = null
                } catch (e: ApiException) {
                    signInError = "Sign-in failed. Please try again."
                }
            } else {
                signInError = "Sign-in canceled."
            }
        }

        setContent {
            DroidPhotosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isSignedIn) {
                        DashboardPlaceholderScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        SignInScreen(
                            modifier = Modifier.padding(innerPadding),
                            errorMessage = signInError,
                            onSignInClick = {
                                signInError = null
                                signInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardPlaceholderScreen(modifier: Modifier = Modifier) {
    // Simple placeholder until full Dashboard (TASK-019/020)
    Text(
        text = "Signed in! Dashboard placeholder.",
        modifier = modifier.padding(24.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun SignInPreview() {
    DroidPhotosTheme {
        SignInScreen(
            errorMessage = null,
            onSignInClick = {}
        )
    }
}