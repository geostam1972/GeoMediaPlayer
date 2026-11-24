package gr.geo.mediaplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import gr.geo.mediaplayer.data.LibraryStore
import gr.geo.mediaplayer.data.MediaRepository
import gr.geo.mediaplayer.playback.PlayerConnection
import gr.geo.mediaplayer.ui.AppRoot
import gr.geo.mediaplayer.ui.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionsGranted = MutableStateFlow(false)
    private var pendingAudioUri: Uri? = null

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onPermissionsResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        LibraryStore.init(this)

        lifecycleScope.launch {
            runCatching { PlayerConnection.connect(this@MainActivity) }
            pendingAudioUri?.let { playExternal(it) }
            pendingAudioUri = null
        }

        handleIntent(intent)
        onPermissionsResult()
        if (!permissionsGranted.value) requestPermissions.launch(neededPermissions())

        setContent {
            AppTheme {
                AppRoot(
                    permissionsGranted = permissionsGranted,
                    onRequestPermissions = { requestPermissions.launch(neededPermissions()) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Pick up new files that were downloaded while we were in the background.
        if (permissionsGranted.value) lifecycleScope.launch { MediaRepository.refresh(this@MainActivity) }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            lifecycleScope.launch {
                runCatching { PlayerConnection.connect(this@MainActivity) }
                playExternal(uri)
            }
        }
    }

    private fun playExternal(uri: Uri) {
        val name = runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "Ήχος"
        PlayerConnection.playUri(uri, name.substringBeforeLast('.'))
    }

    private fun neededPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            list += Manifest.permission.READ_MEDIA_AUDIO
            list += Manifest.permission.READ_MEDIA_VIDEO
            list += Manifest.permission.READ_MEDIA_IMAGES
            list += Manifest.permission.POST_NOTIFICATIONS
        } else {
            list += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return list.toTypedArray()
    }

    private fun onPermissionsResult() {
        val audioPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        val granted = ContextCompat.checkSelfPermission(this, audioPerm) == PackageManager.PERMISSION_GRANTED
        permissionsGranted.value = granted
        if (granted) lifecycleScope.launch { MediaRepository.refresh(this@MainActivity) }
    }
}
