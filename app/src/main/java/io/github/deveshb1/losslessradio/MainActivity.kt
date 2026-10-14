package io.github.deveshb1.losslessradio

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.deveshb1.losslessradio.ui.AboutScreen
import io.github.deveshb1.losslessradio.ui.AddStationDialog
import io.github.deveshb1.losslessradio.ui.Background
import io.github.deveshb1.losslessradio.ui.LosslessTheme
import io.github.deveshb1.losslessradio.ui.topGenres
import io.github.deveshb1.losslessradio.ui.Directory
import io.github.deveshb1.losslessradio.ui.DiscoverScreen
import io.github.deveshb1.losslessradio.ui.FullPlayer
import io.github.deveshb1.losslessradio.ui.LibraryScreen
import io.github.deveshb1.losslessradio.ui.MiniPlayer
import io.github.deveshb1.losslessradio.ui.NowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private enum class Tab { LIBRARY, DISCOVER, ABOUT }

private const val EXTRA_STATION = "station"

class MainActivity : ComponentActivity() {

    private lateinit var library: Library
    private lateinit var tipJar: TipJar

    private var now by mutableStateOf(NowState())
    private var tab by mutableStateOf(Tab.LIBRARY)
    private var playerOpen by mutableStateOf(false)
    private var addingStation by mutableStateOf(false)
    private var bitPerfectWanted by mutableStateOf(false)

    private var query by mutableStateOf("")
    private var losslessOnly by mutableStateOf(true)
    private var genre by mutableStateOf<String?>(null)
    private var lossless: Directory by mutableStateOf(Directory.Loading)
    private var searchResults: Directory by mutableStateOf(Directory.Loading)
    private var searchJob: Job? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        library = Library.get(this)
        tipJar = TipJar(this).also { it.start() }
        bitPerfectWanted = AudioPath.bitPerfectWanted(this)
        loadLossless()

        setContent {
            LosslessTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun App() {
        val lib by library.state.collectAsState()
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    Column {
                        // Library already shows a "now playing" card, so the mini player would just repeat it.
                        if (tab != Tab.LIBRARY) MiniPlayer(now, onOpen = { playerOpen = true }, onToggle = ::togglePlayback)
                        NavigationBar(containerColor = Background, tonalElevation = 0.dp) {
                            val itemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Background,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            )
                            NavigationBarItem(tab == Tab.LIBRARY, { tab = Tab.LIBRARY }, { Icon(Icons.Filled.Home, null) }, label = { Text("Library") }, colors = itemColors)
                            NavigationBarItem(tab == Tab.DISCOVER, { tab = Tab.DISCOVER }, { Icon(Icons.Filled.Search, null) }, label = { Text("Discover") }, colors = itemColors)
                            NavigationBarItem(tab == Tab.ABOUT, { tab = Tab.ABOUT }, { Icon(Icons.Filled.Info, null) }, label = { Text("About") }, colors = itemColors)
                        }
                    }
                },
            ) { padding ->
                when (tab) {
                    Tab.LIBRARY -> LibraryScreen(
                        library = lib,
                        now = now,
                        contentPadding = padding,
                        onPlay = ::play,
                        onFavorite = library::toggleFavorite,
                        onDiscover = { tab = Tab.DISCOVER },
                        onAddStation = { addingStation = true },
                        onOpenPlayer = { playerOpen = true },
                        onToggle = ::togglePlayback,
                    )
                    Tab.DISCOVER -> DiscoverScreen(
                        query = query,
                        losslessOnly = losslessOnly,
                        genre = genre,
                        genres = (lossless as? Directory.Loaded)?.let { remember(it) { topGenres(it.stations) } }.orEmpty(),
                        results = if (losslessOnly) filteredLossless() else searchResults,
                        library = lib,
                        now = now,
                        contentPadding = padding,
                        onQuery = ::onQuery,
                        onLosslessOnly = { losslessOnly = it; genre = null; if (!it) search(query, debounce = false) },
                        onGenre = { genre = it },
                        onRetry = { if (losslessOnly) loadLossless() else search(query, debounce = false) },
                        onPlay = ::play,
                        onFavorite = library::toggleFavorite,
                    )
                    Tab.ABOUT -> AboutScreen(
                        tipJar = tipJar,
                        version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty(),
                        contentPadding = padding,
                        onTip = { tipJar.tip(this@MainActivity, it) },
                        onOpen = ::openUrl,
                    )
                }
            }

            AnimatedVisibility(
                visible = playerOpen && now.station != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                BackHandler { playerOpen = false }
                FullPlayer(
                    now = now,
                    favorite = now.station?.let(lib::isFavorite) == true,
                    bitPerfectWanted = bitPerfectWanted,
                    onClose = { playerOpen = false },
                    onToggle = ::togglePlayback,
                    onFavorite = { now.station?.let(library::toggleFavorite) },
                    onSleep = ::setSleepTimer,
                    onBitPerfect = ::setBitPerfect,
                    onHomepage = ::openUrl,
                )
            }
        }

        if (addingStation) {
            AddStationDialog(
                onDismiss = { addingStation = false },
                onAdd = { name, url ->
                    addingStation = false
                    val station = Station.custom(name, url)
                    library.toggleFavorite(station)
                    play(station)
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token)
            .setListener(object : MediaController.Listener {
                override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                    now = now.copy(extras = Bundle(extras))
                }
            })
            .buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture !== future) return@addListener
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = refresh(c)
            })
            now = now.copy(extras = Bundle(c.sessionExtras))
            refresh(c)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        controller = null
        now = now.copy(connected = false)
        super.onStop()
    }

    override fun onDestroy() {
        tipJar.stop()
        super.onDestroy()
    }

    private fun refresh(c: MediaController) {
        val item = c.currentMediaItem
        val station = item?.mediaMetadata?.extras?.getString(EXTRA_STATION)
            ?.let { runCatching { Station.fromJson(JSONObject(it)) }.getOrNull() }
            ?: now.station.takeIf { item == null }
        val md = c.mediaMetadata
        // Streams often send one "Artist - Title" string; the station name means "no song info".
        val (songArtist, songTitle) = NowPlaying.split(md.artist?.toString(), md.title?.toString())
        val title = songTitle?.takeIf { it != station?.name }
        now = now.copy(
            connected = true,
            station = station,
            wantsAudio = c.playWhenReady,
            buffering = c.playWhenReady && c.playbackState == Player.STATE_BUFFERING,
            reconnecting = c.playWhenReady && c.playerError != null,
            title = title,
            artist = if (title != null) songArtist?.takeIf { it != station?.name } else null,
            error = c.playerError?.takeIf { !c.playWhenReady }?.let(::describe)
                ?: if (now.station?.id == station?.id) now.error else null,
        )
    }

    private fun describe(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "This station isn't available right now"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Can't reach the station — check your connection"
        in PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED..PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
            "This stream's format isn't supported"
        else -> "Couldn't play this station"
    }

    private fun play(station: Station) {
        val c = controller ?: return
        if (now.station?.id == station.id && c.playWhenReady) {
            playerOpen = true
            return
        }
        now = now.copy(station = station, wantsAudio = true, buffering = true, error = null)
        lifecycleScope.launch {
            val url = withContext(Dispatchers.IO) { runCatching { Playlists.resolve(station.url) } }.getOrElse {
                now = now.copy(wantsAudio = false, buffering = false, error = "Couldn't open this station's playlist")
                return@launch
            }
            val item = MediaItem.Builder()
                .setMediaId(station.id)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(url)).build())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setStation(station.name)
                        .setArtworkUri(station.favicon.takeIf { it.startsWith("http") }?.let(Uri::parse))
                        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setExtras(bundleOf(EXTRA_STATION to station.toJson().toString()))
                        .build(),
                )
                .build()
            c.setMediaItem(item)
            c.prepare()
            c.play()
            library.played(station)
            if (!station.custom) launch(Dispatchers.IO) { RadioBrowser.registerClick(station.id) }
        }
    }

    private fun togglePlayback() {
        val c = controller ?: return
        if (c.playWhenReady) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    private fun setSleepTimer(minutes: Int) {
        controller?.sendCustomCommand(SessionInfo.SLEEP_COMMAND, bundleOf(SessionInfo.ARG_MINUTES to minutes))
    }

    private fun setBitPerfect(on: Boolean) {
        bitPerfectWanted = on
        AudioPath.setBitPerfectWanted(this, on)
        // The mixer preference only applies to a freshly opened audio track.
        controller?.takeIf { it.playWhenReady }?.run {
            stop()
            prepare()
            play()
        }
    }

    private fun loadLossless() {
        lossless = Directory.Loading
        lifecycleScope.launch {
            lossless = withContext(Dispatchers.IO) {
                runCatching { Directory.Loaded(RadioBrowser.losslessStations()) }
                    .getOrElse { Directory.Failed("Couldn't load stations. Check your connection.") }
            }
        }
    }

    private fun filteredLossless(): Directory {
        val all = lossless as? Directory.Loaded ?: return lossless
        val q = query.trim()
        val g = genre
        if (q.isEmpty() && g == null) return all
        return Directory.Loaded(
            all.stations.filter { s ->
                (q.isEmpty() || listOf(s.name, s.tags, s.countryCode).any { it.contains(q, ignoreCase = true) }) &&
                    (g == null || s.tags.split(',').any { it.trim().lowercase().contains(g) })
            },
        )
    }

    private fun onQuery(text: String) {
        query = text
        if (!losslessOnly) search(text, debounce = true)
    }

    private fun search(text: String, debounce: Boolean) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            if (debounce) delay(400)
            searchResults = Directory.Loading
            searchResults = withContext(Dispatchers.IO) {
                runCatching { Directory.Loaded(RadioBrowser.search(text)) }
                    .getOrElse { Directory.Failed("Search failed. Check your connection.") }
            }
        }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}
