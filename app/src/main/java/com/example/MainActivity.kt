package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.ui.FinanceViewModel
import com.example.ui.FinanceViewModelFactory
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Register permissions launcher
    private val requestSmsPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false
        if (smsGranted && readGranted) {
            Toast.makeText(
                this,
                "Доступ к СМС получен! Теперь транзакции СМС будут поступать автоматически.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                "Доступ не получен. Но вы можете симулировать СМС во вкладке 'СМС Банк'!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = FinanceRepository(database.financeDao())

        // 2. Obtain FinanceViewModel with Factory
        val factory = FinanceViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        viewModel = viewModel,
                        onRequestSmsPermission = { triggerPermissionRequest() }
                    )
                }
            }
        }
    }

    private fun triggerPermissionRequest() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestSmsPermissionsLauncher.launch(permissions.toTypedArray())
    }
}
