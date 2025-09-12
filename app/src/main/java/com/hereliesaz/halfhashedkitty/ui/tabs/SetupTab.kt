package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun SetupTab() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("Remote Hashcat Server Setup Guide", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Introduction", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            """
            To use this app, you need to set up a remote server with hashcat installed.
            This server will listen for requests from the app and run the hashcat commands.
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Server Requirements", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            """
            1. Install hashcat:
               Follow the official instructions to install hashcat on your server.
               https://hashcat.net/hashcat/

            2. Install Python and Flask:
               You will need Python 3 and the Flask web framework.
               pip install Flask
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Server Script", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Create a file named `server.py` with the following content:")
        Spacer(modifier = Modifier.height(8.dp))
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
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Running and Configuration", style = MaterialTheme.typography.titleLarge)
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            """
            1. Run the server:
               python server.py

            2. Configure the app:
               Enter your server's URL in the "Input" tab of the app.
               Make sure your server is accessible from your phone (e.g., on the same Wi-Fi network, or port-forwarded).
            """.trimIndent()
        )
    }
}
