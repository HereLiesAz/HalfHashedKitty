package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.ui.theme.TransparentButton

@Composable
fun MaskTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("This tab is for creating and selecting masks for hash cracking attacks.")

            Spacer(modifier = Modifier.height(16.dp))

            TransparentButton(onClick = { /* TODO: Implement mask creator */ }, text = "Create Mask")
            Spacer(modifier = Modifier.height(4.dp))
            Text("Create a new mask for mask attacks.", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))

            TransparentButton(onClick = { /* TODO: Implement file picker */ }, text = "Select Mask File")
            Spacer(modifier = Modifier.height(4.dp))
            Text("Select a mask file from your device.", style = MaterialTheme.typography.bodySmall)
        }
    }
}