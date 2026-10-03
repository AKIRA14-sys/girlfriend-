package com.mika.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mika.app.data.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsAndPrivacyScreen(
    preferences: AppPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var speakReplies by remember { mutableStateOf(preferences.speakReplies) }
    var checkinsEnabled by remember { mutableStateOf(preferences.checkinsEnabled) }
    var wakeWordEnabled by remember { mutableStateOf(preferences.wakeWordEnabled) }
    var cameraEnabled by remember { mutableStateOf(preferences.cameraEnabled) }
    var recognizePeopleEnabled by remember { mutableStateOf(preferences.recognizePeopleEnabled) }
    var liveScreenEnabled by remember { mutableStateOf(preferences.liveScreenEnabled) }
    var directCallingAllowed by remember { mutableStateOf(preferences.directCallingAllowed) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions & Privacy") },
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
            Text(
                text = "Privacy Guarantee",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "• Every feature is OFF by default and requires your explicit toggle and Android permission.\n" +
                                "• Secrets (API keys & passwords) are encrypted on-device with Android Keystore.\n" +
                                "• Backups NEVER include API keys or passwords.\n" +
                                "• No analytics, trackers, ads, or background camera recording.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Feature Toggles & Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PermissionRow("Voice Output (TTS)", "No Android Permission required", speakReplies) {
                        speakReplies = it
                        preferences.speakReplies = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow("Notifications & Check-ins", "POST_NOTIFICATIONS", checkinsEnabled) {
                        checkinsEnabled = it
                        preferences.checkinsEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow("Hey Mika Wake Word", "RECORD_AUDIO", wakeWordEnabled) {
                        wakeWordEnabled = it
                        preferences.wakeWordEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow("Camera Vision", "CAMERA", cameraEnabled) {
                        cameraEnabled = it
                        preferences.cameraEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow("People Recognition", "CAMERA", recognizePeopleEnabled) {
                        recognizePeopleEnabled = it
                        preferences.recognizePeopleEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow("Live Screen Mode", "MEDIA_PROJECTION", liveScreenEnabled) {
                        liveScreenEnabled = it
                        preferences.liveScreenEnabled = it
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow("Direct Phone Calling", "CALL_PHONE", directCallingAllowed) {
                        directCallingAllowed = it
                        preferences.directCallingAllowed = it
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Android System Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    permissionName: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(text = permissionName, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
