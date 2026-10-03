package com.mika.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mika.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenData: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAvatarPicker: () -> Unit
) {
    val prefs = viewModel.preferences
    val testConnectionState by viewModel.testConnectionState.collectAsState()

    var baseUrl by remember { mutableStateOf(prefs.baseUrl) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    var companionName by remember { mutableStateOf(prefs.companionName) }
    var personalityText by remember { mutableStateOf(prefs.personalityText) }
    var attitudeLevel by remember { mutableStateOf(prefs.attitudeLevel) }

    var speakReplies by remember { mutableStateOf(prefs.speakReplies) }
    var speechSpeed by remember { mutableStateOf(prefs.speechSpeed) }
    var speechPitch by remember { mutableStateOf(prefs.speechPitch) }
    var useFishAudio by remember { mutableStateOf(false) }

    var checkinsEnabled by remember { mutableStateOf(prefs.checkinsEnabled) }
    var wakeWordEnabled by remember { mutableStateOf(prefs.wakeWordEnabled) }
    var cameraEnabled by remember { mutableStateOf(prefs.cameraEnabled) }
    var recognizePeopleEnabled by remember { mutableStateOf(prefs.recognizePeopleEnabled) }
    var liveScreenEnabled by remember { mutableStateOf(prefs.liveScreenEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("AI Provider Settings")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            prefs.baseUrl = it
                        },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = {
                            modelName = it
                            prefs.modelName = it
                        },
                        label = { Text("Model Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.apiKey = it
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key visibility"
                                )
                            }
                        }
                    )

                    Button(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Connection")
                    }

                    testConnectionState?.let { state ->
                        Text(
                            text = state,
                            color = if (state.startsWith("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            SectionHeader("Voice & Speech Settings")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToggleRow("Speak Her Replies", speakReplies) {
                        speakReplies = it
                        prefs.speakReplies = it
                    }

                    if (speakReplies) {
                        Text("Speech Rate (${String.format("%.1f", speechSpeed)}x)")
                        Slider(
                            value = speechSpeed,
                            onValueChange = {
                                speechSpeed = it
                                prefs.speechSpeed = it
                            },
                            valueRange = 0.5f..2.0f
                        )

                        Text("Voice Pitch (${String.format("%.1f", speechPitch)}x)")
                        Slider(
                            value = speechPitch,
                            onValueChange = {
                                speechPitch = it
                                prefs.speechPitch = it
                            },
                            valueRange = 0.5f..2.0f
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ToggleRow("Fish Audio Voice Provider (Coming soon)", useFishAudio) {
                            useFishAudio = it
                        }
                    }
                }
            }

            SectionHeader("Companion Settings")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = companionName,
                        onValueChange = {
                            companionName = it
                            prefs.companionName = it
                        },
                        label = { Text("Companion Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = personalityText,
                        onValueChange = {
                            personalityText = it
                            prefs.personalityText = it
                        },
                        label = { Text("Personality Prompt") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Text("Attitude Level: Sweet (${(attitudeLevel * 100).toInt()}%) Sassy")
                    Slider(
                        value = attitudeLevel,
                        onValueChange = {
                            attitudeLevel = it
                            prefs.attitudeLevel = it
                        },
                        valueRange = 0f..1f
                    )

                    Button(
                        onClick = onOpenAvatarPicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose Avatar (.vrm)")
                    }
                }
            }

            SectionHeader("Features & Permissions")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleRow("Daily Check-ins & Reminders", checkinsEnabled) {
                        checkinsEnabled = it
                        prefs.checkinsEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ToggleRow("Hey Mika Wake Word", wakeWordEnabled) {
                        wakeWordEnabled = it
                        prefs.wakeWordEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ToggleRow("Camera Vision", cameraEnabled) {
                        cameraEnabled = it
                        prefs.cameraEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ToggleRow("Recognize People (Opt-in - Coming soon)", recognizePeopleEnabled) {
                        recognizePeopleEnabled = it
                        prefs.recognizePeopleEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ToggleRow("Live Screen Mode", liveScreenEnabled) {
                        liveScreenEnabled = it
                        prefs.liveScreenEnabled = it
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenPermissions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Permissions & Privacy Dashboard")
                    }
                }
            }

            SectionHeader("Data Management")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onOpenMemories,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Memories")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onOpenData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Data & Backup")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
