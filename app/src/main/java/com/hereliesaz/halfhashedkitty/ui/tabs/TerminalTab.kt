package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hereliesaz.halfhashedkitty.MainViewModel

@Composable
fun TerminalTab(viewModel: MainViewModel) {
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
            }
        }
    }
}
