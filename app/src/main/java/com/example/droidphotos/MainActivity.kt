package com.droidphotos

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.droidphotos.ui.theme.DroidPhotosTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private var isSignedIn by mutableStateOf(false)
    private var signInError: String? by mutableStateOf(null)

    private var hasMediaPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var mediaPermissionError: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Google Sign-In for Photos Library scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/photoslibrary"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initial permission state
        hasMediaPermission = checkMediaPermission()
        hasNotificationPermission = checkNotificationPermission()

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
                    Log.w("MainActivity", "Google sign-in failed", e)
                    signInError = "Sign-in failed. Please try again."
                }
            } else {
                signInError = "Sign-in canceled."
            }
        }

        // Activity Result launcher for media permissions
        val mediaPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            hasMediaPermission = checkMediaPermission()
            mediaPermissionError = if (!hasMediaPermission) {
                "Without photo access, DroidPhotos can't scan or upload your folders."
            } else {
                null
            }
        }

        // Activity Result launcher for notifications (API 33+)
        val notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasNotificationPermission = granted || checkNotificationPermission()
        }

        setContent {
            DroidPhotosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isSignedIn) {
                        Column(modifier = Modifier.padding(innerPadding)) {
                            DashboardPlaceholderScreen(modifier = Modifier.padding(24.dp))

                            if (!hasMediaPermission) {
                                MediaPermissionPrompt(
                                    errorMessage = mediaPermissionError,
                                    onRequestClick = {
                                        val permissions = if (Build.VERSION.SDK_INT >= 33) {
                                            arrayOf(
                                                Manifest.permission.READ_MEDIA_IMAGES,
                                                Manifest.permission.READ_MEDIA_VIDEO
                                            )
                                        } else {
                                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                        mediaPermissionLauncher.launch(permissions)
                                    }
                                )
                            } else if (!hasNotificationPermission && Build.VERSION.SDK_INT >= 33) {
                                NotificationPermissionPrompt(
                                    onRequestClick = {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                )
                            }
                        }
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

    private fun checkMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            val imagesGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val videosGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            imagesGranted && videosGranted
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

@Composable
private fun DashboardPlaceholderScreen(modifier: Modifier = Modifier) {
    // Simple placeholder until full Dashboard (TASK-019/020)
    Text(
        text = "Signed in! Dashboard placeholder.",
        modifier = modifier
    )
}

@Composable
private fun MediaPermissionPrompt(
    errorMessage: String?,
    onRequestClick: () -> Unit
) {
    Text(
        text = "To sync your folders, DroidPhotos needs access to your photos and videos on this device.",
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
    Button(
        onClick = onRequestClick,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(text = "Allow photo access")
    }
    if (errorMessage != null) {
        Text(
            text = errorMessage,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun NotificationPermissionPrompt(
    onRequestClick: () -> Unit
) {
    Text(
        text = "We can notify you when sync completes or if uploads fail.",
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
    Button(
        onClick = onRequestClick,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(text = "Allow notifications")
    }
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