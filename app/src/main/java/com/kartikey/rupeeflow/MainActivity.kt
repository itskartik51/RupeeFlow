package com.kartikey.rupeeflow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kartikey.rupeeflow.UI_Screens.MainScreen
import com.kartikey.rupeeflow.UI_Screens.LoginScreen
import com.kartikey.rupeeflow.UI_Screens.RupeeFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPreferences = getSharedPreferences("RupeeFlowPrefs", Context.MODE_PRIVATE)
        val savedLoginState = sharedPreferences.getBoolean("isLoggedIn", false)
        val savedUsername = sharedPreferences.getString("username", "") ?: ""
        val savedThemeMode = sharedPreferences.getInt("theme_mode", 0) // Naya Theme Cache

        setContent {
            var themeMode by remember { mutableIntStateOf(savedThemeMode) }

            RupeeFlowTheme(themeMode = themeMode) { 
                Surface(modifier = Modifier.fillMaxSize()) {
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
