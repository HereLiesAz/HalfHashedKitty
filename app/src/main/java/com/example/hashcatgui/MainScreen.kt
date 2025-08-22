package com.example.hashcatgui

import android.app.Application
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.hashcatgui.ui.tabs.*
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen(MainViewModel(Application(), HashcatApiClient()), HashtopolisViewModel(HashtopolisApiClient()))
}
