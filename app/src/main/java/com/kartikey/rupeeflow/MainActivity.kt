package com.kartikey.rupeeflow

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
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var currentUser by remember { mutableStateOf("") }

                    if (isLoggedIn) {
                        HomeScreen(username = currentUser)
                    } else {
                        LoginScreen(onLoginSuccess = { username -> 
                            currentUser = username
                            isLoggedIn = true 
                        })
                    }
                }
            }
        }
    }
}
