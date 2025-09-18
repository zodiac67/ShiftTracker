package com.example.shifttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shifttracker.ui.MainScreen
import com.example.shifttracker.ui.MainViewModel
import com.example.shifttracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(applicationContext))
                MainScreen(vm)
            }
        }
    }
}
