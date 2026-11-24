package gr.geo.mediaplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import gr.geo.mediaplayer.MainActivity
import gr.geo.mediaplayer.data.LibraryStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Runs the music player as a foreground service, so playback keeps going when you
 * switch apps, press Home or lock the screen. Shows the media notification and
 * lock-screen controls automatically.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())

    private val saveRunnable = object : Runnable {
        override fun run() {
            saveQueue()
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LibraryStore.init(this)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)    // keep CPU awake while playing with screen off
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.toLongOrNull()?.let { LibraryStore.recordPlay(it) }
                saveQueue()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) saveQueue()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioEffects.attach(audioSessionId)
            }
        })

        player.repeatMode = LibraryStore.getInt("repeat", Player.REPEAT_MODE_OFF)
        player.shuffleModeEnabled = LibraryStore.getBool("shuffle", false)

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()

        instance = this
        currentPlayer = player
        AudioEffects.attach(player.audioSessionId)
        restoreQueue(player)
        handler.postDelayed(saveRunnable, 15_000)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away from Recents keeps the music playing.
        // Only stop the service if nothing is playing.
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            saveQueue()
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveQueue()
        handler.removeCallbacksAndMessages(null)
        SleepTimer.cancel()
        AudioEffects.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        currentPlayer = null
        instance = null
        super.onDestroy()
    }

    // ---------- remember the queue between app launches ----------
    private fun saveQueue() {
        val player = session?.player ?: return
        val arr = JSONArray()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val uri = item.localConfiguration?.uri ?: continue
            arr.put(
                JSONObject()
                    .put("id", item.mediaId)
                    .put("uri", uri.toString())
                    .put("title", item.mediaMetadata.title?.toString() ?: "")
                    .put("artist", item.mediaMetadata.artist?.toString() ?: "")
                    .put("art", item.mediaMetadata.artworkUri?.toString() ?: "")
            )
        }
        LibraryStore.putString("queue", arr.toString())
        LibraryStore.putInt("queueIndex", player.currentMediaItemIndex)
        LibraryStore.putString("queuePos", player.currentPosition.toString())
        LibraryStore.putInt("repeat", player.repeatMode)
        LibraryStore.putBool("shuffle", player.shuffleModeEnabled)
    }

    private fun restoreQueue(player: ExoPlayer) {
        val json = LibraryStore.getString("queue") ?: return
        runCatching {
            val arr = JSONArray(json)
            val items = List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val art = o.optString("art")
                MediaItem.Builder()
                    .setMediaId(o.getString("id"))
                    .setUri(Uri.parse(o.getString("uri")))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(o.optString("title"))
                            .setArtist(o.optString("artist"))
                            .setArtworkUri(if (art.isNotEmpty()) Uri.parse(art) else null)
                            .build()
                    )
                    .build()
            }
            if (items.isEmpty()) return
            val index = LibraryStore.getInt("queueIndex", 0).coerceIn(0, items.size - 1)
            val pos = LibraryStore.getString("queuePos")?.toLongOrNull() ?: 0L
            player.setMediaItems(items, index, pos)
            player.prepare()
        }
    }

    companion object {
        var instance: PlaybackService? = null
            private set
        var currentPlayer: ExoPlayer? = null
            private set
    }
}
