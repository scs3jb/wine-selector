package com.wineselector.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.wineselector.app.viewmodel.WineSelectorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this)[WineSelectorViewModel::class.java]

        setContent {
            WineSelectorApp(viewModel = viewModel)
        }
    }
}
