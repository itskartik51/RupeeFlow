package com.kartikey.rupeeflow

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kartikey.rupeeflow.UI_Screens.MainScreen
import com.kartikey.rupeeflow.UI_Screens.LoginScreen
import com.kartikey.rupeeflow.UI_Screens.RupeeFlowTheme
import com.kartikey.rupeeflow.UI_Screens.bounceClick

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPreferences = getSharedPreferences("RupeeFlowPrefs", Context.MODE_PRIVATE)
        val savedLoginState = sharedPreferences.getBoolean("isLoggedIn", false)
        val savedUsername = sharedPreferences.getString("username", "") ?: ""
        val savedThemeMode = sharedPreferences.getInt("theme_mode", 0)

        setContent {
            var themeMode by remember { mutableIntStateOf(savedThemeMode) }
            var isSecurityLockEnabled by remember { 
                mutableStateOf(sharedPreferences.getBoolean("isSecurityLockEnabled", false)) 
            }
            var isAuthenticated by remember { mutableStateOf(!isSecurityLockEnabled) }

            fun triggerBiometricAuth() {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            isAuthenticated = true
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(this@MainActivity, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(this@MainActivity, "Authentication Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("RupeeFlow Security")
                    .setSubtitle("Unlock using device security")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }

            LaunchedEffect(Unit) {
                if (isSecurityLockEnabled && !isAuthenticated) {
                    triggerBiometricAuth()
                }
            }

            RupeeFlowTheme(themeMode = themeMode) { 
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isSecurityLockEnabled && !isAuthenticated) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "RupeeFlow is Locked",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Unlock using your device security",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { triggerBiometricAuth() },
                                    modifier = Modifier.bounceClick(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Unlock App", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        var isLoggedIn by remember { mutableStateOf(savedLoginState) }
                        var currentUser by remember { mutableStateOf(savedUsername) }

                        if (isLoggedIn && currentUser.isNotEmpty()) {
                            MainScreen(
                                username = currentUser,
                                themeMode = themeMode,
                                onThemeChange = { newMode ->
                                    sharedPreferences.edit().putInt("theme_mode", newMode).apply()
                                    themeMode = newMode
                                },
                                onLogout = {
                                    sharedPreferences.edit().clear().apply()
                                    isLoggedIn = false
                                    currentUser = ""
                                }
                            )
                        } else {
                            LoginScreen(onLoginSuccess = { username -> 
                                sharedPreferences.edit().apply {
                                    putBoolean("isLoggedIn", true)
                                    putString("username", username)
                                    apply()
                                }
                                currentUser = username
                                isLoggedIn = true 
                            })
                        }
                    }
                }
            }
        }
    }
}
