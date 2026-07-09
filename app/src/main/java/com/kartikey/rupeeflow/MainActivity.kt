package com.kartikey.rupeeflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kartikey.rupeeflow.UI_Screens.HomeScreen
import com.kartikey.rupeeflow.UI_Screens.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Phone ki permanent memory read karna
        val sharedPreferences = getSharedPreferences("RupeeFlowPrefs", Context.MODE_PRIVATE)
        val savedLoginState = sharedPreferences.getBoolean("isLoggedIn", false)
        val savedUsername = sharedPreferences.getString("username", "") ?: ""

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isLoggedIn by remember { mutableStateOf(savedLoginState) }
                    var currentUser by remember { mutableStateOf(savedUsername) }

                    if (isLoggedIn && currentUser.isNotEmpty()) {
                        HomeScreen(
                            username = currentUser,
                            onLogout = {
                                // Logout dabane par memory saaf hogi
                                sharedPreferences.edit().clear().apply()
                                isLoggedIn = false
                                currentUser = ""
                            }
                        )
                    } else {
                        LoginScreen(onLoginSuccess = { username -> 
                            // Login hone par memory me save kar lena
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
