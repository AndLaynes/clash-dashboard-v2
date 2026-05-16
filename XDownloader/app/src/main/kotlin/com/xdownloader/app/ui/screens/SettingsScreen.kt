package com.xdownloader.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xdownloader.app.ui.viewmodel.SettingsUiState
import com.xdownloader.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pasteText by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val stream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
        val content = stream.bufferedReader().readText()
        stream.close()
        viewModel.importCookie(content)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Cookie Settings", style = MaterialTheme.typography.titleLarge)

        CookieStatusCard(hasCookie = viewModel.hasCookie, uiState = uiState)

        Button(
            onClick = { filePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Cookie File (cookies.txt)")
        }

        OutlinedTextField(
            value = pasteText,
            onValueChange = { pasteText = it },
            label = { Text("Paste Netscape cookie content") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 8
        )

        Button(
            onClick = {
                viewModel.importCookie(pasteText)
                pasteText = ""
            },
            enabled = pasteText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Pasted Cookie")
        }

        if (viewModel.hasCookie) {
            OutlinedButton(
                onClick = { viewModel.clearCookie() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Cookie")
            }
        }

        when (val state = uiState) {
            is SettingsUiState.Error -> Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            is SettingsUiState.CookieImported -> Text(
                text = "Cookie imported successfully!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            is SettingsUiState.CookieCleared -> Text(
                text = "Cookie cleared.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
            else -> Unit
        }
    }
}

@Composable
private fun CookieStatusCard(hasCookie: Boolean, uiState: SettingsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Status", style = MaterialTheme.typography.labelMedium)
            Text(
                text = if (hasCookie) "Cookie configured" else "No cookie configured",
                style = MaterialTheme.typography.bodyMedium
            )
            if (uiState is SettingsUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }
    }
}
