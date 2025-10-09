package com.hereliesaz.halfhashedkitty.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HashcatSetupTab() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Setting Up Hashcat for GPU Cracking",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hashcat uses your Graphics Card (GPU) to crack hashes incredibly fast. To make this work, you need the right drivers installed. If hashcat isn't working, this is the most common reason why.",
            style = MaterialTheme.typography.bodyLarge
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Step 1
        Text(
            text = "Step 1: Install Hashcat",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Download the latest version from the official website: hashcat.net/hashcat/. Extract the archive to a known location on your computer.",
            style = MaterialTheme.typography.bodyLarge
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Step 2
        Text(
            text = "Step 2: Install GPU Drivers",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This is the most important step.\n\n" +
                    "NVIDIA Users:\n" +
                    "You need the latest 'Game Ready' or 'Studio' drivers. Download them from the official NVIDIA website. Hashcat uses the NVIDIA CUDA platform.\n\n" +
                    "AMD Users:\n" +
                    "You need the latest 'Adrenalin Edition' drivers. Download them from the official AMD website. Hashcat uses the OpenCL platform, which is included with these drivers.\n\n" +
                    "Intel GPU Users:\n" +
                    "You need the latest graphics drivers from Intel's website. Hashcat uses the OpenCL platform, which is included with these drivers.",
            style = MaterialTheme.typography.bodyLarge
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Step 3
        Text(
            text = "Step 3: Verify Everything Works",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Open a command prompt or terminal, navigate to your hashcat directory, and run the benchmark command:\n\n" +
                    "hashcat.exe -b\n\n" +
                    "If everything is set up correctly, you will see a list of your GPUs and a benchmark running for various hash types. If you see errors about missing DLLs or no devices being found, it means your GPU drivers are not installed correctly.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}