package com.mika.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mika.app.data.AppPreferences
import com.mika.app.data.db.AppDatabase
import com.mika.app.domain.DeviceActionHandler
import com.mika.app.services.CheckInWorker
import com.mika.app.services.TtsManager
import com.mika.app.ui.screens.AvatarScreen
import com.mika.app.ui.screens.ChatScreen
import com.mika.app.ui.screens.DataScreen
import com.mika.app.ui.screens.FirstRunScreen
import com.mika.app.ui.screens.MemoriesScreen
import com.mika.app.ui.screens.PermissionsAndPrivacyScreen
import com.mika.app.ui.screens.SettingsScreen
import com.mika.app.ui.theme.MikaTheme
import com.mika.app.viewmodel.ChatViewModel
import com.mika.app.viewmodel.DataViewModel
import com.mika.app.viewmodel.MemoriesViewModel
import com.mika.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class AppScreen {
    FIRST_RUN,
    CHAT,
    SETTINGS,
    MEMORIES,
    DATA,
    PERMISSIONS,
    AVATAR
}

class MainActivity : ComponentActivity() {

    private var ttsManager: TtsManager? = null
    private var isInPipModeState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = MikaApplication.instance
        val preferences = app.preferences
        val database = app.database

        ttsManager = TtsManager(
            context = applicationContext,
            onSpeechStart = { },
            onSpeechDone = { }
        )

        scheduleCheckinsIfEnabled(preferences)

        setContent {
            MikaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MikaAppNavigation(
                        preferences = preferences,
                        database = database,
                        ttsManager = ttsManager,
                        isInPipMode = isInPipModeState,
                        onEnterPip = { enterPipMode() },
                        onImportVrm = { uri -> copyVrmToInternalStorage(uri, preferences) },
                        onExecuteAction = { action ->
                            val result = DeviceActionHandler.executeAction(applicationContext, action)
                            Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun scheduleCheckinsIfEnabled(preferences: AppPreferences) {
        if (preferences.checkinsEnabled && !preferences.checkinsPaused) {
            val checkInRequest = PeriodicWorkRequestBuilder<CheckInWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "MikaCheckIns",
                ExistingPeriodicWorkPolicy.KEEP,
                checkInRequest
            )
        } else {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("MikaCheckIns")
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
    }

    private fun copyVrmToInternalStorage(uri: Uri, preferences: AppPreferences) {
        Toast.makeText(this, "Importing avatar...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val avatarDir = File(filesDir, "avatar").apply { if (!exists()) mkdirs() }
                val destinationFile = File(avatarDir, "companion.vrm")

                val input = contentResolver.openInputStream(uri) ?: throw IOException("Could not open file stream")
                input.use { inputStream ->
                    destinationFile.outputStream().buffered().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val sizeMb = String.format("%.1f", destinationFile.length() / (1024f * 1024f))
                preferences.vrmFilePath = destinationFile.name

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Avatar imported ($sizeMb MB)", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Could not import avatar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager?.shutdown()
    }
}

@Composable
fun MikaAppNavigation(
    preferences: AppPreferences,
    database: AppDatabase,
    ttsManager: TtsManager?,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    onImportVrm: (Uri) -> Unit,
    onExecuteAction: (com.mika.app.domain.ActionTag) -> Unit
) {
    var currentScreen by remember {
        mutableStateOf(
            if (preferences.isAgeConfirmed) AppScreen.CHAT else AppScreen.FIRST_RUN
        )
    }

    val chatViewModel: ChatViewModel = viewModel(
        factory = SimpleViewModelFactory { ChatViewModel(database, preferences) }
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SimpleViewModelFactory { SettingsViewModel(preferences) }
    )
    val memoriesViewModel: MemoriesViewModel = viewModel(
        factory = SimpleViewModelFactory { MemoriesViewModel(database) }
    )
    val dataViewModel: DataViewModel = viewModel(
        factory = SimpleViewModelFactory { DataViewModel(database) }
    )

    LaunchedEffect(preferences.speakReplies) {
        if (preferences.speakReplies) {
            chatViewModel.onSpeakReplyRequested = { text ->
                ttsManager?.speak(
                    text = text,
                    speed = preferences.speechSpeed,
                    pitch = preferences.speechPitch
                )
            }
        } else {
            chatViewModel.onSpeakReplyRequested = null
        }
    }

    val vrmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportVrm(it) }
    }

    if (isInPipMode) {
        AvatarScreen(
            chatViewModel = chatViewModel,
            companionName = preferences.companionName,
            vrmFileName = preferences.vrmFilePath,
            onBack = {},
            onOpenSettings = {},
            onEnterPip = {}
        )
    } else {
        when (currentScreen) {
            AppScreen.FIRST_RUN -> {
                FirstRunScreen(
                    onConfirmed = {
                        preferences.isAgeConfirmed = true
                        currentScreen = AppScreen.CHAT
                    }
                )
            }
            AppScreen.CHAT -> {
                ChatScreen(
                    viewModel = chatViewModel,
                    companionName = preferences.companionName,
                    onOpenAvatarScreen = { currentScreen = AppScreen.AVATAR },
                    onOpenSettingsScreen = { currentScreen = AppScreen.SETTINGS },
                    onConfirmAction = onExecuteAction
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { currentScreen = AppScreen.CHAT },
                    onOpenMemories = { currentScreen = AppScreen.MEMORIES },
                    onOpenData = { currentScreen = AppScreen.DATA },
                    onOpenPermissions = { currentScreen = AppScreen.PERMISSIONS },
                    onOpenAvatarPicker = { vrmLauncher.launch(arrayOf("*/*")) }
                )
            }
            AppScreen.MEMORIES -> {
                MemoriesScreen(
                    viewModel = memoriesViewModel,
                    onBack = { currentScreen = AppScreen.SETTINGS }
                )
            }
            AppScreen.DATA -> {
                DataScreen(
                    viewModel = dataViewModel,
                    onBack = { currentScreen = AppScreen.SETTINGS }
                )
            }
            AppScreen.PERMISSIONS -> {
                PermissionsAndPrivacyScreen(
                    preferences = preferences,
                    onBack = { currentScreen = AppScreen.SETTINGS }
                )
            }
            AppScreen.AVATAR -> {
                AvatarScreen(
                    chatViewModel = chatViewModel,
                    companionName = preferences.companionName,
                    vrmFileName = preferences.vrmFilePath,
                    onBack = { currentScreen = AppScreen.CHAT },
                    onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                    onEnterPip = onEnterPip
                )
            }
        }
    }
}

class SimpleViewModelFactory<T : androidx.lifecycle.ViewModel>(
    private val creator: () -> T
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : androidx.lifecycle.ViewModel> create(modelClass: Class<VM>): VM {
        return creator() as VM
    }
}
