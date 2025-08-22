package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputTab(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = viewModel.serverUrl.value,
            onValueChange = { viewModel.serverUrl.value = it },
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = viewModel.hashToCrack.value,
            onValueChange = { viewModel.hashToCrack.value = it },
            label = { Text("Enter hash") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = viewModel.selectedHashMode.value?.second ?: "Select Hash Mode",
                onValueChange = {},
                label = { Text("Hash Mode") },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        Modifier.clickable { expanded = !expanded }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.hashModes.forEach { hashMode ->
                    DropdownMenuItem(
                        text = { Text(text = "${hashMode.first} - ${hashMode.second}") },
                        onClick = {
                            viewModel.selectedHashMode.value = hashMode
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
