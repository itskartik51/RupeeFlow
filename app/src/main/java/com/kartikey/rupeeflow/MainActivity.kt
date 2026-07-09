package com.kartikey.rupeeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
// Yahan humne aapki banayi hui UI_Screens folder se HomeScreen ko bula liya
import com.kartikey.rupeeflow.UI_Screens.HomeScreen 

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Ab app khulte hi seedha aapka banaya hua Dashboard/Form dikhega!
                    HomeScreen()
                }
            }
        }
    }
}
