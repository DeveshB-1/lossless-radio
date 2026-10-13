package io.github.deveshb1.losslessradio

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

data class LibraryState(
    val favorites: List<Station> = emptyList(),
    val recents: List<Station> = emptyList(),
) {
    fun isFavorite(station: Station) = favorites.any { it.id == station.id }
}

/** Favourites and recently played stations, stored as one small JSON file. */
class Library private constructor(private val file: File) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    fun toggleFavorite(station: Station) = edit { s ->
        if (s.isFavorite(station)) s.copy(favorites = s.favorites.filterNot { it.id == station.id })
        else s.copy(favorites = s.favorites + station)
    }

    fun played(station: Station) = edit { s ->
        s.copy(recents = (listOf(station) + s.recents.filterNot { it.id == station.id }).take(MAX_RECENTS))
    }

    fun remove(station: Station) = edit { s ->
        s.copy(favorites = s.favorites.filterNot { it.id == station.id }, recents = s.recents.filterNot { it.id == station.id })
    }

    private fun edit(change: (LibraryState) -> LibraryState) {
        _state.update(change)
        save(_state.value)
    }

    private fun load(): LibraryState = runCatching {
        val o = JSONObject(file.readText())
        LibraryState(o.optJSONArray("favorites").stations(), o.optJSONArray("recents").stations())
    }.getOrDefault(LibraryState())

    private fun save(state: LibraryState) {
        val o = JSONObject()
            .put("favorites", JSONArray(state.favorites.map { it.toJson() }))
            .put("recents", JSONArray(state.recents.map { it.toJson() }))
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(file)
    }

    private fun JSONArray?.stations(): List<Station> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { runCatching { Station.fromJson(getJSONObject(it)) }.getOrNull() }

    companion object {
        private const val MAX_RECENTS = 20

        @Volatile private var instance: Library? = null

        fun get(context: Context): Library = instance ?: synchronized(this) {
            instance ?: Library(File(context.applicationContext.filesDir, "library.json")).also { instance = it }
        }
    }
}
