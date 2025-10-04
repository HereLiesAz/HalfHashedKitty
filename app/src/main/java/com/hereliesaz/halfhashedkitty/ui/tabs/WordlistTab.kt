package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

@Composable
fun WordlistTab(viewModel: MainViewModel, onShowInstructions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ScreenTitle("Wordlist", onShowInstructions)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("This tab is for specifying the path to the wordlist file on the remote server.")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.wordlistPath.value,
                onValueChange = { viewModel.wordlistPath.value = it },
                label = { Text("Remote Wordlist Path") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "The full path to the wordlist file on the remote server.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}