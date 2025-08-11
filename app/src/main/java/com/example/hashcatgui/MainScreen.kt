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
    val tabs = listOf("Input", "Wordlist", "Mask", "Attack", "Output", "Terminal", "Setup")

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
            6 -> SetupTab()
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
        OutlinedTextField(
            value = viewModel.wordlistPath.value,
            onValueChange = { viewModel.wordlistPath.value = it },
            label = { Text("Remote Wordlist Path") },
            modifier = Modifier.fillMaxWidth()
        )
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { viewModel.startAttack() }) {
            Text("Start Remote Attack")
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

@Composable
fun OutputTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (viewModel.crackedPassword.value != null) {
            Text("Password Found!", fontWeight = FontWeight.Bold)
            Text(viewModel.crackedPassword.value!!)
        } else {
            Text("No password found yet.")
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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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

@Composable
fun SetupTab() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("Remote Hashcat Server Setup Guide", fontWeight = FontWeight.Bold)
        Text(
            """
            To use this app, you need to set up a remote server with hashcat installed.
            This server will listen for requests from the app and run the hashcat commands.

            1. Install hashcat:
               Follow the official instructions to install hashcat on your server.
               https://hashcat.net/hashcat/

            2. Install Python and Flask:
               You will need Python 3 and the Flask web framework.
               pip install Flask

            3. Create the server script:
               Create a file named `server.py` with the following content:
            """.trimIndent()
        )
        Text(
            """
            from flask import Flask, request, jsonify
            import subprocess

            app = Flask(__name__)
            jobs = {}

            @app.route('/attack', methods=['POST'])
            def start_attack():
                data = request.get_json()
                hash_to_crack = data['hash']
                # WARNING: This is a simplified example.
                # In a real application, you must validate and sanitize all inputs.
                job_id = str(len(jobs))
                proc = subprocess.Popen(['hashcat', '-m', '0', '-a', '0', hash_to_crack, '/path/to/your/wordlist.txt'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
                jobs[job_id] = {'process': proc, 'status': 'Running', 'password': None}
                return jsonify({'jobId': job_id, 'status': 'Running'})

            @app.route('/attack/<job_id>', methods=['GET'])
            def get_status(job_id):
                job = jobs.get(job_id)
                if not job:
                    return jsonify({'status': 'Not Found'}), 404

                # This is a simplified status check.
                # A real implementation would need to parse hashcat's output.
                return jsonify({'jobId': job_id, 'status': job['status'], 'crackedPassword': job['password']})

            if __name__ == '__main__':
                app.run(host='0.0.0.0', port=8080)
            """.trimIndent(),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .background(Color.LightGray)
                .padding(8.dp)
        )
        Text(
            """
            4. Run the server:
               python server.py

            5. Configure the app:
               Enter your server's URL in the "Input" tab of the app.
               Make sure your server is accessible from your phone (e.g., on the same Wi-Fi network, or port-forwarded).
            """.trimIndent()
        )
    }
}
