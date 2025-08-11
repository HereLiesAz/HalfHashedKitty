package com.example.hashcatgui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Input", "Wordlist", "Mask", "Attack", "Output", "Terminal")

    Column {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    text = { Text(text = title) }
                )
            }
        }
        when (tabIndex) {
            0 -> InputTab(viewModel)
            1 -> WordlistTab(viewModel)
            2 -> MaskTab()
            3 -> AttackTab(viewModel)
            4 -> OutputTab(viewModel)
            5 -> TerminalTab(viewModel)
        }
    }
}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun InputTab(viewModel: MainViewModel) {
    var hashInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = hashInput,
            onValueChange = { hashInput = it },
            label = { Text("Enter hash") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                if (hashInput.isNotBlank()) {
                    viewModel.addHash(hashInput)
                    hashInput = ""
                }
            }) {
                Text("Add Hash")
            }
            Button(onClick = { /* TODO: Implement file picker */ }) {
                Text("Load from File")
            }
        }
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(viewModel.hashes) { hash ->
                Text(text = hash.hash, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WordlistTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { /* TODO: Implement file picker */ }) {
            Text("Select Wordlist")
        }
        Text(text = "Selected wordlist: ${viewModel.wordlistPath.value}")
    }
}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { /* TODO: Implement mask creator */ }) {
            Text("Create Mask")
        }
        Button(onClick = { /* TODO: Implement file picker */ }) {
            Text("Select Mask File")
        }
    }
}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AttackTab(viewModel: MainViewModel) {
    val command = "python cracker.py <hash> ${viewModel.wordlistPath.value} <algorithm>"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = command,
            onValueChange = { },
            readOnly = true,
            label = { Text("Command") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        Button(onClick = { viewModel.startAttack() }) {
            Text("Start Attack")
        }
    }
}

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CrackedHash(val hash: String, val type: String, val password: String)

@Composable
fun OutputTab(viewModel: MainViewModel) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Hash", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Type", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Password", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }
        }
        items(viewModel.hashes.filter { it.password != null }) { cracked ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(cracked.hash, modifier = Modifier.weight(1f))
                Text(cracked.verifiedHashType?.name ?: "", modifier = Modifier.weight(1f))
                Text(cracked.password ?: "", modifier = Modifier.weight(1f))
            }
        }
    }
}

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun TerminalTab(viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        items(viewModel.terminalOutput) { line ->
            Text(
                text = line,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
