package com.vendorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vendorapp.ui.AppViewModel
import com.vendorapp.ui.VendorApp
import com.vendorapp.ui.theme.VendorAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VendorAppTheme {
                VendorApp(viewModel<AppViewModel>())
            }
        }
    }
}
