package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MaskTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("This tab is for creating and selecting masks for hash cracking attacks.")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* TODO: Implement mask creator */ }) {
            Text("Create Mask")
        }
        Text("Create a new mask for mask attacks.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { /* TODO: Implement file picker */ }) {
            Text("Select Mask File")
        }
        Text("Select a mask file from your device.", style = MaterialTheme.typography.bodySmall)
    }
}
