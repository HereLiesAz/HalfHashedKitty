package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.SniffViewModel

/**
 * Composable function for the "Sniff" tab.
 * <p>
 * This screen allows the user to:
 * <ul>
 *     <li>Select a saved remote connection (e.g., Raspberry Pi).</li>
 *     <li>Enter SSH credentials (password).</li>
 *     <li>Start and Stop the remote sniffing process.</li>
 *     <li>View the real-time output from the remote sniffer.</li>
 * </ul>
 * </p>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SniffTab(
    sniffViewModel: SniffViewModel
) {
    // State for the connection dropdown menu.
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Remote Connection Selector ---
        // Dropdown menu to pick a target.
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                // Display selected name or placeholder.
                value = sniffViewModel.selectedConnection.value?.name ?: "Select a remote target...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Target Device") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Populate menu items from ViewModel.
                sniffViewModel.remoteConnections.value.forEach { connection ->
                    DropdownMenuItem(
                        text = { Text(connection.name) },
                        onClick = {
                            sniffViewModel.onConnectionSelected(connection)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Control Buttons ---
        var showPasswordDialog by remember { mutableStateOf(false) }

        // Password Dialog (Modal).
        if (showPasswordDialog) {
            var password by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text("Enter SSH Password") },
                text = {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password for ${sniffViewModel.selectedConnection.value?.connectionString}") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Start sniffing with the provided password.
                            sniffViewModel.selectedConnection.value?.let {
                                sniffViewModel.startSniffing(it, password)
                            }
                            showPasswordDialog = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Action Buttons Row.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    // Trigger the password dialog instead of starting immediately.
                    showPasswordDialog = true
                },
                // Only enable if a connection is selected.
                enabled = sniffViewModel.selectedConnection.value != null
            ) {
                Text("Start Sniffing")
            }
            Button(
                onClick = { sniffViewModel.stopSniffing() },
                enabled = sniffViewModel.selectedConnection.value != null
            ) {
                Text("Stop Sniffing")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Output Area ---
        Text("Sniffing Output:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        // Read-only text field to display logs.
        OutlinedTextField(
            value = sniffViewModel.sniffOutput.value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxSize(),
            label = { Text("Output") }
        )
    }
}
