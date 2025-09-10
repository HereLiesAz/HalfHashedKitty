package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.background
<<<<<<< HEAD
import androidx.compose.foundation.layout.Box
=======
import androidx.compose.foundation.layout.Column
>>>>>>> origin/feature/build-pc-app
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

@Composable
fun TerminalTab(viewModel: MainViewModel) {
<<<<<<< HEAD
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (viewModel.terminalOutput.isEmpty()) {
            Text("Terminal is not active.", color = Color.Gray)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.terminalOutput) { line ->
                    Text(
                        text = line,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
=======
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("This tab shows the raw output from the tools that are being run.")
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
>>>>>>> origin/feature/build-pc-app
            }
        }
    }
}
