package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

@Composable
fun OutputTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Attack Results", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        if (viewModel.crackedPassword.value != null) {
            Text("Success!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Password Found!", fontWeight = FontWeight.Bold)
            Text(viewModel.crackedPassword.value!!)
        } else {
            Text("No password found yet.", style = MaterialTheme.typography.titleLarge)
            Text("Please monitor the 'Terminal' tab for live progress of the attack.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
