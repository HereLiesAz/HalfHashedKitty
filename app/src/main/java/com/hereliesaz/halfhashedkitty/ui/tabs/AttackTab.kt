package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel
import com.hereliesaz.halfhashedkitty.ui.theme.TransparentButton

@Composable
fun AttackTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("This tab is for starting the hash cracking attack on the remote server.")
        Spacer(modifier = Modifier.height(16.dp))

        Box(contentAlignment = Alignment.Center) {
            TransparentButton(
                onClick = { viewModel.startAttack() },
                enabled = !viewModel.isAttackRunning.value
            ) {
                if (viewModel.isAttackRunning.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Start Remote Attack")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "This will start the hash cracking attack on the remote server with the configured settings.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}