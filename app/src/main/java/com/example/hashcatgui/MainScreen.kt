package com.example.hashcatgui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hashcatgui.ui.theme.HashcatGUITheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainViewModel: MainViewModel, hashtopolisViewModel: HashtopolisViewModel) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Input", "Wordlist", "Mask", "Command Builder", "Attack", "Output", "Terminal", "Hashtopolis", "Setup")

    HashcatGUITheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Hashcat GUI") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).animateContentSize()) {
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
                    0 -> InputTab(mainViewModel)
                    1 -> WordlistTab(mainViewModel)
                    2 -> MaskTab()
                    3 -> CommandBuilderTab(mainViewModel)
                    4 -> AttackTab(mainViewModel)
                    5 -> OutputTab(mainViewModel)
                    6 -> TerminalTab(mainViewModel)
                    7 -> HashtopolisTab(hashtopolisViewModel)
                    8 -> SetupTab()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandBuilderTab(viewModel: MainViewModel) {
    var attackModeExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = viewModel.selectedAttackMode.value.second,
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
                        text = { Text(text = "${attackMode.first} - ${attackMode.second}") },
                        onClick = {
                            viewModel.selectedAttackMode.value = attackMode
                            attackModeExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.rulesFile.value,
            onValueChange = { viewModel.rulesFile.value = it },
            label = { Text("Rules File") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = viewModel.customMask.value,
            onValueChange = { viewModel.customMask.value = it },
            label = { Text("Custom Mask") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Checkbox(
                checked = viewModel.force.value,
                onCheckedChange = { viewModel.force.value = it }
            )
            Text("Force")
        }
    }
}

@Composable
fun HashtopolisTab(viewModel: HashtopolisViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
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
            items(viewModel.agents) { agent ->
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen(MainViewModel(Application()), HashtopolisViewModel(HashtopolisApiClient()))
}

@OptIn(ExperimentalMaterial3Api::class)
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
