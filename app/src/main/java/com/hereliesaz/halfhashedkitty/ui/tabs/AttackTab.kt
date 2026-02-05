package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel
import com.hereliesaz.halfhashedkitty.HashModeInfo

/**
 * Composable function for the "Attack" configuration tab.
 * <p>
 * This screen allows the user to configure the parameters for a Hashcat attack, including:
 * <ul>
 *     <li>Hash File location (placeholder).</li>
 *     <li>Hash Mode (e.g., MD5, NTLM).</li>
 *     <li>Attack Mode (Straight, Brute-force).</li>
 *     <li>Rules file (optional).</li>
 * </ul>
 * </p>
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackTab(viewModel: MainViewModel) {
    // Scroll state for the column could be added if content overflows.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Hash File Input ---
        OutlinedTextField(
            value = viewModel.hashToCrack.value,
            onValueChange = { viewModel.hashToCrack.value = it },
            label = { Text("Hash File Path (on Desktop)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Hash Mode Selection ---
        var expanded by remember { mutableStateOf(false) }

        // Dropdown menu for selecting Hash Mode.
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                // Display the selected mode description or a prompt.
                value = viewModel.selectedHashMode.value?.toString() ?: "Select Hash Mode",
                onValueChange = {},
                readOnly = true,
                label = { Text("Hash Mode") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Iterate over available modes from ViewModel.
                viewModel.hashModes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.toString()) },
                        onClick = {
                            viewModel.selectedHashMode.value = mode
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Attack Mode Selection ---
        var attackModeExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = attackModeExpanded,
            onExpandedChange = { attackModeExpanded = !attackModeExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = viewModel.selectedAttackMode.value.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Attack Mode") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = attackModeExpanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = attackModeExpanded,
                onDismissRequest = { attackModeExpanded = false }
            ) {
                viewModel.attackModes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name) },
                        onClick = {
                            viewModel.selectedAttackMode.value = mode
                            attackModeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Rules File Input ---
        OutlinedTextField(
            value = viewModel.rulesFile.value,
            onValueChange = { viewModel.rulesFile.value = it },
            label = { Text("Rules File (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Start Attack Button ---
        Button(
            onClick = { viewModel.startAttack() },
            // Disable button if an attack is already running.
            enabled = !viewModel.isAttackRunning.value,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (viewModel.isAttackRunning.value) "Attack Running..." else "Start Attack")
        }
    }
}
