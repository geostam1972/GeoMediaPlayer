package gr.geo.mediaplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import gr.geo.mediaplayer.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class QueueEntry(
    val key: String,      // stable key for the list (mediaId + position)
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
)

data class PlayerUiState(
    val hasMedia: Boolean = false,
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = 0,
)

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build()
    )
    .build()

/** The UI's remote control for the PlaybackService. */
object PlayerConnection {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish()
        }
    }

    private val connectLock = Mutex()

    suspend fun connect(context: Context) = connectLock.withLock {
        if (controller != null) return@withLock
        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val c = MediaController.Builder(app, token).buildAsync().await()
        c.addListener(listener)
        controller = c
        publish()
    }

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    val currentPosition: Long get() = controller?.currentPosition ?: 0L
    val bufferedPosition: Long get() = controller?.bufferedPosition ?: 0L

    // ---------- actions ----------
    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.shuffleModeEnabled = shuffle
        val start = if (shuffle) songs.indices.random() else startIndex.coerceIn(0, songs.size - 1)
        c.setMediaItems(songs.map { it.toMediaItem() }, start, 0L)
        c.prepare()
        c.play()
    }

    /** Plays a file opened from another app (e.g. My Files). */
    fun playUri(uri: Uri, title: String) {
        val c = controller ?: return
        val item = MediaItem.Builder()
            .setMediaId("ext:$uri")
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    fun playNext(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        if (c.mediaItemCount == 0) return playSongs(songs)
        c.addMediaItems(c.currentMediaItemIndex + 1, songs.map { it.toMediaItem() })
    }

    fun addToQueue(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        if (c.mediaItemCount == 0) return playSongs(songs)
        c.addMediaItems(songs.map { it.toMediaItem() })
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(c.currentMediaItemIndex, 0)
            c.play()
        }
    }

    fun next() = controller?.seekToNext()
    fun previous() = controller?.seekToPrevious()
    fun seekTo(ms: Long) = controller?.seekTo(ms)
    fun seekBack() = controller?.seekBack()
    fun seekForward() = controller?.seekForward()

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playQueueIndex(index: Int) {
        val c = controller ?: return
        c.seekTo(index, 0L)
        c.play()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to) return
        c.moveMediaItem(from, to)
    }

    fun removeQueueItem(index: Int) = controller?.removeMediaItem(index)

    fun clearQueue() {
        controller?.stop()
        controller?.clearMediaItems()
    }

    /** Randomly reorders everything after the current song. */
    fun shuffleQueue() {
        val c = controller ?: return
        val cur = c.currentMediaItemIndex
        val count = c.mediaItemCount
        if (count - cur <= 2) return
        // Fisher–Yates using moves on the upcoming part of the queue.
        for (i in count - 1 downTo cur + 2) {
            val j = (cur + 1..i).random()
            if (j != i) {
                c.moveMediaItem(j, i)
                c.moveMediaItem(i - 1, j)
            }
        }
    }

    private fun publish() {
        val c = controller ?: run { _state.value = PlayerUiState(); return }
        val item = c.currentMediaItem
        val md = item?.mediaMetadata
        val queue = (0 until c.mediaItemCount).map { i ->
            val it = c.getMediaItemAt(i)
            QueueEntry(
                key = "${it.mediaId}#$i",
                mediaId = it.mediaId,
                title = it.mediaMetadata.title?.toString() ?: "",
                artist = it.mediaMetadata.artist?.toString() ?: "",
                artworkUri = it.mediaMetadata.artworkUri,
            )
        }
        _state.value = PlayerUiState(
            hasMedia = item != null,
            mediaId = item?.mediaId,
            title = md?.title?.toString() ?: "",
            artist = md?.artist?.toString() ?: "",
            artworkUri = md?.artworkUri,
            isPlaying = c.isPlaying,
            durationMs = c.duration.takeIf { it > 0 } ?: 0L,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            queue = queue,
            currentIndex = c.currentMediaItemIndex,
        )
    }
}
