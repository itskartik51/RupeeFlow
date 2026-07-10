package com.kartikey.rupeeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kartikey.rupeeflow.UI_Screens.MainScreen
import com.kartikey.rupeeflow.ui.theme.RupeeFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RupeeFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        username = "itskartik51", 
                        onLogout = {
                            // Logout functionality yahan handle hogi
                        }
                    )
                }
            }
        }
    }
}
