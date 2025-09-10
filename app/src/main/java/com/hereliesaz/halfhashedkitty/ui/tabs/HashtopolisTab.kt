package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.HashtopolisViewModel

@Composable
fun HashtopolisTab(viewModel: HashtopolisViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("This tab is for connecting to a Hashtopolis server to manage your hash cracking agents.")
        OutlinedTextField(
            value = viewModel.serverUrl.value,
            onValueChange = { viewModel.serverUrl.value = it },
            label = { Text("Hashtopolis Server URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = viewModel.apiKey.value,
            onValueChange = { viewModel.apiKey.value = it },
            label = { Text("API Key") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Button(
            onClick = { viewModel.getAgents() },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Get Agents")
        }
        viewModel.errorMessage.value?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(viewModel.agents, key = { agent -> agent.id }) { agent ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = agent.name, fontWeight = FontWeight.Bold)
                        Text(text = "Status: ${agent.status}")
                        Text(text = "Last Activity: ${agent.lastActivity}")
                    }
                }
            }
        }
    }
}
