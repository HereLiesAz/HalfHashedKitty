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
        Button(onClick = { /* TODO: Implement mask creator */ }) {
            Text("Create Mask")
        }
        Button(onClick = { /* TODO: Implement file picker */ }) {
            Text("Select Mask File")
        }
    }
}
