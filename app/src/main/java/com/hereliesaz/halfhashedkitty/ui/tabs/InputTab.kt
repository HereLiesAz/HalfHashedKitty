package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

import kotlinx.serialization.InternalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun InputTab(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadZipFile(context, it)
        }
    }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { viewModel.identifyHash() }) {
                Text("Auto-detect Hash Type")
            }
            Button(onClick = { launcher.launch("application/zip") }) {
                Text("Upload ZIP File")
            }
        }


        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = viewModel.selectedHashMode.value?.name ?: "Select Hash Mode",
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
                        text = { Text(text = "${hashMode.id} - ${hashMode.name}") },
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
