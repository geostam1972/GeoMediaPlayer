package gr.geo.mediaplayer.data

import android.content.ContentUris
import android.net.Uri

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
) {
    val artworkUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val folder: String,
)

data class GifItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAddedSec: Long,
)

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long>,
)

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
