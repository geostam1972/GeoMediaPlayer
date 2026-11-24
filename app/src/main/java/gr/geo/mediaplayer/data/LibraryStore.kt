package gr.geo.mediaplayer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores favourites, user playlists, play history and play counts on the phone.
 * Simple JSON in SharedPreferences: small, fast and survives app updates.
 */
object LibraryStore {

    private lateinit var prefs: SharedPreferences

    private val _favorites = MutableStateFlow<List<Long>>(emptyList())
    val favorites: StateFlow<List<Long>> = _favorites

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    /** Most recent first. */
    private val _recent = MutableStateFlow<List<Long>>(emptyList())
    val recent: StateFlow<List<Long>> = _recent

    private val _playCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val playCounts: StateFlow<Map<Long, Int>> = _playCounts

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("library", Context.MODE_PRIVATE)
        _favorites.value = readLongList("favorites")
        _recent.value = readLongList("recent")
        _playCounts.value = readCounts()
        _playlists.value = readPlaylists()
    }

    // ---------- favourites ----------
    fun isFavorite(id: Long) = _favorites.value.contains(id)

    fun toggleFavorite(id: Long) {
        val list = _favorites.value.toMutableList()
        if (!list.remove(id)) list.add(0, id)
        _favorites.value = list
        writeLongList("favorites", list)
    }

    // ---------- history ----------
    fun recordPlay(id: Long) {
        val recent = _recent.value.toMutableList()
        recent.remove(id)
        recent.add(0, id)
        val trimmed = recent.take(100)
        _recent.value = trimmed
        writeLongList("recent", trimmed)

        val counts = _playCounts.value.toMutableMap()
        counts[id] = (counts[id] ?: 0) + 1
        _playCounts.value = counts
        writeCounts(counts)
    }

    fun clearHistory() {
        _recent.value = emptyList()
        writeLongList("recent", emptyList())
    }

    fun clearCounts() {
        _playCounts.value = emptyMap()
        writeCounts(emptyMap())
    }

    // ---------- playlists ----------
    fun createPlaylist(name: String, songIds: List<Long> = emptyList()): Long {
        val id = System.currentTimeMillis()
        _playlists.value = _playlists.value + Playlist(id, name.trim().ifBlank { "Νέα playlist" }, songIds.distinct())
        writePlaylists()
        return id
    }

    fun renamePlaylist(id: Long, name: String) {
        _playlists.value = _playlists.value.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it }
        writePlaylists()
    }

    fun deletePlaylist(id: Long) {
        _playlists.value = _playlists.value.filterNot { it.id == id }
        writePlaylists()
    }

    fun addToPlaylist(id: Long, songIds: List<Long>) {
        _playlists.value = _playlists.value.map { p ->
            if (p.id == id) p.copy(songIds = (p.songIds + songIds).distinct()) else p
        }
        writePlaylists()
    }

    fun removeFromPlaylist(id: Long, songId: Long) {
        _playlists.value = _playlists.value.map { p ->
            if (p.id == id) p.copy(songIds = p.songIds - songId) else p
        }
        writePlaylists()
    }

    fun movePlaylistSong(id: Long, from: Int, to: Int) {
        _playlists.value = _playlists.value.map { p ->
            if (p.id == id && from in p.songIds.indices && to in p.songIds.indices) {
                val l = p.songIds.toMutableList()
                l.add(to, l.removeAt(from))
                p.copy(songIds = l)
            } else p
        }
        writePlaylists()
    }

    // ---------- settings ----------
    fun getInt(key: String, def: Int) = prefs.getInt(key, def)
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getBool(key: String, def: Boolean) = prefs.getBoolean(key, def)
    fun putBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getString(key: String): String? = prefs.getString(key, null)
    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    // ---------- json helpers ----------
    private fun readLongList(key: String): List<Long> = runCatching {
        val arr = JSONArray(prefs.getString(key, "[]"))
        List(arr.length()) { arr.getLong(it) }
    }.getOrDefault(emptyList())

    private fun writeLongList(key: String, list: List<Long>) {
        prefs.edit().putString(key, JSONArray(list).toString()).apply()
    }

    private fun readCounts(): Map<Long, Int> = runCatching {
        val obj = JSONObject(prefs.getString("counts", "{}") ?: "{}")
        obj.keys().asSequence().associate { it.toLong() to obj.getInt(it) }
    }.getOrDefault(emptyMap())

    private fun writeCounts(map: Map<Long, Int>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k.toString(), v) }
        prefs.edit().putString("counts", obj.toString()).apply()
    }

    private fun readPlaylists(): List<Playlist> = runCatching {
        val arr = JSONArray(prefs.getString("playlists", "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("songs")
            Playlist(o.getLong("id"), o.getString("name"), List(ids.length()) { ids.getLong(it) })
        }
    }.getOrDefault(emptyList())

    private fun writePlaylists() {
        val arr = JSONArray()
        _playlists.value.forEach { p ->
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("songs", JSONArray(p.songIds)))
        }
        prefs.edit().putString("playlists", arr.toString()).apply()
    }
}
