package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

@Composable
fun CaptureTab(viewModel: MainViewModel) {
    val listState = rememberLazyListState()

    // Automatically scroll to the bottom when new output arrives
    LaunchedEffect(viewModel.captureOutput.size) {
        if (viewModel.captureOutput.isNotEmpty()) {
            listState.animateScrollToItem(index = viewModel.captureOutput.size - 1)
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("This tab is for capturing wireless network packets to get the handshake for hash cracking.")
        Button(
            onClick = {
                if (viewModel.isCapturing.value) {
                    viewModel.stopCapture()
                } else {
                    viewModel.startCapture()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (viewModel.isCapturing.value) "Stop Capture" else "Start Capture")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Live Output:", style = MaterialTheme.typography.titleMedium)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            LazyColumn(state = listState) {
                items(viewModel.captureOutput) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}
