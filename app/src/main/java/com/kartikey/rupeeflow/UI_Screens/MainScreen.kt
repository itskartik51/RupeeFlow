package com.kartikey.rupeeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kartikey.rupeeflow.UI_Screens.MainScreen // Naye Super Boss ka proper import
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
                    // Yahan HomeScreen ki jagah ab direct MainScreen call ho raha hai
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
