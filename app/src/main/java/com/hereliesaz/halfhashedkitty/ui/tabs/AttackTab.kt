package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

@Composable
fun AttackTab(viewModel: MainViewModel, onShowInstructions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ScreenTitle("Attack", onShowInstructions)
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("This tab is for starting the hash cracking attack on the remote server.")
            Button(onClick = { viewModel.startAttack() }) {
                Text("Start Remote Attack")
            }
            Text(
                "This will start the hash cracking attack on the remote server with the configured settings.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}