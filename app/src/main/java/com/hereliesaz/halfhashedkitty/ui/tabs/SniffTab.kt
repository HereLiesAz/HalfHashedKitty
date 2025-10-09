package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.SniffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SniffTab(
    sniffViewModel: SniffViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Remote Connection Selector ---
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = sniffViewModel.selectedConnection.value?.name ?: "Select a remote target...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Target Device") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    // Don't clear output here, startSniffing will do it.
                    showPasswordDialog = true
                          },
                enabled = sniffViewModel.selectedConnection.value != null
            ) {
                Text("Start Sniffing")
            }
            Button(
                onClick = { sniffViewModel.stopSniffing() },
                enabled = sniffViewModel.selectedConnection.value != null // This could be improved to track running state
            ) {
                Text("Stop Sniffing")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Output Area ---
        Text("Sniffing Output:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sniffViewModel.sniffOutput.value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxSize(),
            label = { Text("Output") }
        )
    }
}