package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.IrrigationRepository
import com.example.ui.screens.IrrigationScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IrrigationViewModel
import com.example.ui.viewmodel.IrrigationViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local SQLite instance via Room
        val database = AppDatabase.getDatabase(this)
        
        // 2. Wrap operations inside the single source-of-truth Repository
        val repository = IrrigationRepository(database.irrigationDao())
        
        // 3. Construct the View Model with its specific factory
        val viewModel = ViewModelProvider(
            this,
            IrrigationViewModelFactory(repository)
        )[IrrigationViewModel::class.java]

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    IrrigationScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
