package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel
import kotlinx.serialization.InternalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun CommandBuilderTab(viewModel: MainViewModel) {
    var attackModeExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = viewModel.selectedAttackMode.value.name,
                onValueChange = {},
                label = { Text("Attack Mode") },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        Modifier.clickable { attackModeExpanded = !attackModeExpanded }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = attackModeExpanded,
                onDismissRequest = { attackModeExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.attackModes.forEach { attackMode ->
                    DropdownMenuItem(
                        text = { Text(text = "${attackMode.id} - ${attackMode.name}") },
                        onClick = {
                            viewModel.selectedAttackMode.value = attackMode
                            attackModeExpanded = false
                        }
                    )
                }
            }
        }
        Text("Select the hashcat attack mode.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.rulesFile.value,
            onValueChange = { viewModel.rulesFile.value = it },
            label = { Text("Rules File") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Specify a rules file for the attack.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.customMask.value,
            onValueChange = { viewModel.customMask.value = it },
            label = { Text("Custom Mask") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Enter a custom mask for mask attacks.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = viewModel.force.value,
                onCheckedChange = { viewModel.force.value = it }
            )
            Text("Force")
        }
        Text("Force the attack, ignoring any warnings.", style = MaterialTheme.typography.bodySmall)
    }
}
