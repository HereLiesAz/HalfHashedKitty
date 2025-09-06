package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InstructionsOverlay(selectedId: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Text(
                text = "Instructions for ${selectedId}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(text = getInstructionsForTab(selectedId))
        }
    }
}

fun getInstructionsForTab(tabId: String): String {
    return when (tabId) {
        "Input" -> "Enter a hash or upload a ZIP file containing a hash. You can also upload a PCAPNG file to extract a hash. Use the 'Detect Hash' button to automatically identify the hash type."
        "Attack" -> "Configure the attack mode and options. Select the hash type and provide the necessary inputs for the attack."
        "Wordlist" -> "Manage your wordlists. You can add, remove, or select wordlists to be used in the attack."
        "Mask" -> "Create and manage masks for your attacks. Masks are used to specify a pattern for the hash cracking process."
        "Capture" -> "This screen will display live data from network captures. Currently, no capture is in progress."
        "Terminal" -> "This screen will show the output from the hash cracking process. The terminal is not currently active."
        "Output" -> "View the results of your completed hash cracking tasks. Cracked hashes and other information will be displayed here."
        "Hashtopolis" -> "Connect to a Hashtopolis server to manage your tasks and agents."
        "Pi Control" -> "Control and manage a Raspberry Pi that is set up for hash cracking."
        "PC Connect" -> "Connect to a PC to offload hash cracking tasks."
        else -> "No instructions available for this screen."
    }
}
