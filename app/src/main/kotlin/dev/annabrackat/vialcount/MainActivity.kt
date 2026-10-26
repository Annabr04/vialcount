package dev.annabrackat.vialcount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.annabrackat.vialcount.ui.VialcountApp
import dev.annabrackat.vialcount.ui.VialcountTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VialcountTheme {
                VialcountApp()
            }
        }
    }
}
