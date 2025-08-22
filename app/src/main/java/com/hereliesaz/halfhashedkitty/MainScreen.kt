package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hereliesaz.halfhashedkitty.ui.tabs.AttackTab
import com.hereliesaz.halfhashedkitty.ui.tabs.HashtopolisTab
import com.hereliesaz.halfhashedkitty.ui.tabs.InputTab
import com.hereliesaz.halfhashedkitty.ui.tabs.MaskTab
import com.hereliesaz.halfhashedkitty.ui.tabs.OutputTab
import com.hereliesaz.halfhashedkitty.ui.tabs.SetupTab
import com.hereliesaz.halfhashedkitty.ui.tabs.TerminalTab
import com.hereliesaz.halfhashedkitty.ui.tabs.WordlistTab
import com.hereliesaz.halfhashedkitty.ui.theme.HashcatGUITheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainViewModel: MainViewModel, hashtopolisViewModel: HashtopolisViewModel) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Input", "Wordlist", "Mask", "Attack", "Output", "Terminal", "Hashtopolis", "Setup")

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
                ScrollableTabRow(selectedTabIndex = tabIndex) {
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
                    3 -> AttackTab(mainViewModel)
                    4 -> OutputTab(mainViewModel)
                    5 -> TerminalTab(mainViewModel)
                    6 -> HashtopolisTab(hashtopolisViewModel)
                    7 -> SetupTab()
                }
            }
        }
    }
}

// Preview for MainScreen
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen(MainViewModel(Application(), HashcatApiClient()), HashtopolisViewModel())
}
