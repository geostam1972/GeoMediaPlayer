package gr.geo.mediaplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Reads songs, videos and GIFs from the phone's media library. */
object MediaRepository {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _gifs = MutableStateFlow<List<GifItem>>(emptyList())
    val gifs: StateFlow<List<GifItem>> = _gifs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun songById(id: Long): Song? = _songs.value.firstOrNull { it.id == id }

    suspend fun refresh(context: Context) {
        _loading.value = true
        try {
            withContext(Dispatchers.IO) {
                _songs.value = runCatching { loadSongs(context) }.getOrDefault(_songs.value)
                _videos.value = runCatching { loadVideos(context) }.getOrDefault(_videos.value)
                _gifs.value = runCatching { loadGifs(context) }.getOrDefault(_gifs.value)
            }
        } finally {
            _loading.value = false
        }
    }

    private fun loadSongs(context: Context): List<Song> {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        // Skip ringtones / notification sounds and very short clips.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val result = ArrayList<Song>()
        context.contentResolver.query(
            collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val rawArtist = c.getString(artistCol)
                val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Άγνωστος καλλιτέχνης" else rawArtist
                val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: c.getString(nameCol)?.substringBeforeLast('.') ?: "Χωρίς τίτλο"
                result += Song(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = title,
                    artist = artist,
                    album = c.getString(albumCol) ?: "",
                    albumId = c.getLong(albumIdCol),
                    durationMs = c.getLong(durCol),
                    dateAddedSec = c.getLong(dateCol),
                )
            }
        }
        return result
    }

    private fun loadVideos(context: Context): List<VideoItem> {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        val result = ArrayList<VideoItem>()
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result += VideoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = c.getString(nameCol)?.substringBeforeLast('.') ?: "Βίντεο",
                    durationMs = c.getLong(durCol),
                    sizeBytes = c.getLong(sizeCol),
                    dateAddedSec = c.getLong(dateCol),
                    folder = c.getString(bucketCol) ?: "",
                )
            }
        }
        return result
    }

    private fun loadGifs(context: Context): List<GifItem> {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val result = ArrayList<GifItem>()
        context.contentResolver.query(
            collection, projection,
            "${MediaStore.Images.Media.MIME_TYPE} = ?", arrayOf("image/gif"),
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result += GifItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = c.getString(nameCol) ?: "GIF",
                    dateAddedSec = c.getLong(dateCol),
                )
            }
        }
        return result
    }
}
