package com.wineselector.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.wineselector.app.ui.WineSelectorApp
import com.wineselector.app.viewmodel.WineSelectorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WineSelectorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { WineSelectorApp(viewModel) }
    }
}
