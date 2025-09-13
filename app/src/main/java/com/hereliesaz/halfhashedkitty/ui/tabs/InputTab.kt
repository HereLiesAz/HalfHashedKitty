package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel
import android.util.Log

import kotlinx.serialization.InternalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun InputTab(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val zipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadZipFile(context, it)
        }
    }

    val pcapngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadPcapngFile(context, it)
        }
    }

    val evidenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.processEvidenceFile(context, uri)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("This tab is for providing the input for the hash cracking process. You can either enter the hash directly, or upload a ZIP or PCAPNG file to extract the hash from it.")

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.serverUrl.value,
            onValueChange = { viewModel.serverUrl.value = it },
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("The URL of your remote hashcat server.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = viewModel.hashToCrack.value,
            onValueChange = { viewModel.hashToCrack.value = it },
            label = { Text("Enter hash") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("The hash you want to crack.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { viewModel.identifyHash() },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
            ) {
                Text("Detect Hash")
            }
            OutlinedButton(
                onClick = { zipLauncher.launch("application/zip") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("Upload Zip")
            }
            OutlinedButton(
                onClick = { pcapngLauncher.launch("*/*") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
            ) {
                Text("Upload PCAPNG")
            }
        }
        Text("Use these buttons to detect the hash type, or upload a file containing the hash.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { evidenceLauncher.launch(arrayOf("image/*", "audio/*", "video/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Evidence (Image/Audio/Video)")
        }
        Text("Select an image, audio, or video file to extract text from.", style = MaterialTheme.typography.bodySmall)


        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
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
        Text("Select the hash mode for the cracking session.", style = MaterialTheme.typography.bodySmall)
    }
}
