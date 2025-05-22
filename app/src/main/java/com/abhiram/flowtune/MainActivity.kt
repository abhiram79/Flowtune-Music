package com.abhiram.flowtune

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.sp 
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import com.abhiram.innertube.YouTube
import com.abhiram.innertube.models.SongItem
import com.abhiram.innertube.models.WatchEndpoint
import com.abhiram.flowtune.constants.AppBarHeight
import com.abhiram.flowtune.constants.DarkModeKey
import com.abhiram.flowtune.constants.DefaultOpenTabKey
import com.abhiram.flowtune.constants.DisableScreenshotKey
import com.abhiram.flowtune.constants.DynamicThemeKey
import com.abhiram.flowtune.constants.MiniPlayerHeight
import com.abhiram.flowtune.constants.NavigationBarAnimationSpec
import com.abhiram.flowtune.constants.NavigationBarHeight
import com.abhiram.flowtune.constants.PauseSearchHistoryKey
import com.abhiram.flowtune.constants.PlayerBackgroundStyle
import com.abhiram.flowtune.constants.PlayerBackgroundStyleKey
import com.abhiram.flowtune.constants.PureBlackKey
import com.abhiram.flowtune.constants.SearchSource
import com.abhiram.flowtune.constants.SearchSourceKey
import com.abhiram.flowtune.constants.SlimNavBarKey
import com.abhiram.flowtune.constants.StopMusicOnTaskClearKey
import com.abhiram.flowtune.db.MusicDatabase
import com.abhiram.flowtune.db.entities.SearchHistory
import com.abhiram.flowtune.extensions.toEnum
import com.abhiram.flowtune.models.toMediaMetadata
import com.abhiram.flowtune.playback.DownloadUtil
import com.abhiram.flowtune.playback.MusicService
import com.abhiram.flowtune.playback.MusicService.MusicBinder
import com.abhiram.flowtune.playback.PlayerConnection
import com.abhiram.flowtune.playback.queues.YouTubeQueue
import com.abhiram.flowtune.ui.component.BottomSheetMenu
import com.abhiram.flowtune.ui.component.IconButton
import com.abhiram.flowtune.ui.component.LocalMenuState
import com.abhiram.flowtune.ui.component.TopSearch
import com.abhiram.flowtune.ui.component.rememberBottomSheetState
import com.abhiram.flowtune.ui.component.shimmer.ShimmerTheme
import com.abhiram.flowtune.ui.menu.YouTubeSongMenu
import com.abhiram.flowtune.ui.player.BottomSheetPlayer
import com.abhiram.flowtune.ui.screens.Screens
import com.abhiram.flowtune.ui.screens.navigationBuilder
import com.abhiram.flowtune.ui.screens.search.LocalSearchScreen
import com.abhiram.flowtune.ui.screens.search.OnlineSearchScreen
import com.abhiram.flowtune.ui.screens.settings.AvatarPreferenceManager
import com.abhiram.flowtune.ui.screens.settings.DarkMode
import com.abhiram.flowtune.ui.component.LocaleManager
import com.abhiram.flowtune.ui.screens.settings.NavigationTab
import com.abhiram.flowtune.ui.theme.ColorSaver
import com.abhiram.flowtune.ui.theme.DefaultThemeColor
import com.abhiram.flowtune.ui.theme.flowtuneTheme
import com.abhiram.flowtune.ui.theme.extractThemeColor
import com.abhiram.flowtune.ui.utils.appBarScrollBehavior
import com.abhiram.flowtune.ui.utils.backToMain
import com.abhiram.flowtune.ui.utils.resetHeightOffset
import com.abhiram.flowtune.utils.SyncUtils
import com.abhiram.flowtune.utils.Updater
import com.abhiram.flowtune.utils.dataStore
import com.abhiram.flowtune.utils.get
import com.abhiram.flowtune.utils.rememberEnumPreference
import com.abhiram.flowtune.utils.rememberPreference
import com.abhiram.flowtune.utils.reportException
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

// El codigo original de la aplicacion pertenece a : Arturo Cervantes Galindo (abhiram) Cualquier parecido es copia y pega de mi codigo original

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    override fun onStart() {
        super.onStart()
        startService(Intent(this, MusicService::class.java))
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    @SuppressLint("ImplicitSamInstance")
    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedLanguage =
            sharedPreferences.getString("app_language", Locale.getDefault().language) ?: "en"

        val localeManager = LocaleManager(newBase)
        localeManager.updateLocale(savedLanguage)



        super.attachBaseContext(newBase)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        intent?.let { handlevideoIdIntent(it) }

        setContent {
            LaunchedEffect(Unit) {
                if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
                    Updater.getLatestVersionName().onSuccess {
                        latestVersionName = it
                    }
                }
            }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = false)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.OFF)

            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    themeColor =
                        if (song != null) {
                            withContext(Dispatchers.IO) {
                                val result =
                                    imageLoader.execute(
                                        ImageRequest
                                            .Builder(this@MainActivity)
                                            .data(song.thumbnailUrl)
                                            .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                                            .build(),
                                    )
                                (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor()
                                    ?: DefaultThemeColor
                            }
                        } else {
                            DefaultThemeColor
                        }
                }
            }

            flowtuneTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
            ) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (previousTab) = rememberSaveable { mutableStateOf("home") }

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val defaultOpenTab =
                        remember {
                            dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                        }
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                ACTION_EXPLORE -> NavigationTab.EXPLORE
                                else -> null
                            }
                        }

                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Explore.route,
                            Screens.Library.route,
                            "settings",
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active) {
                            navBackStackEntry?.destination?.route == null ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                    !active
                        }

                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    val playerAwareWindowInsets =
                        remember(
                            bottomInset,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed
                        ) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar) bottom += NavigationBarHeight
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry) {
                        shouldShowTopBar =
                            !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }
                    DisposableEffect(Unit) {
                        val listener =
                            Consumer<Intent> { intent ->
                                val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)
                                    ?.toUri() ?: return@Consumer
                                when (val path = uri.pathSegments.firstOrNull()) {
                                    "playlist" ->
                                        uri.getQueryParameter("list")?.let { playlistId ->
                                            if (playlistId.startsWith("OLAK5uy_")) {
                                                coroutineScope.launch {
                                                    YouTube
                                                        .albumSongs(playlistId)
                                                        .onSuccess { songs ->
                                                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                                                navController.navigate("album/$browseId")
                                                            }
                                                        }.onFailure {
                                                            reportException(it)
                                                        }
                                                }
                                            } else {
                                                navController.navigate("online_playlist/$playlistId")
                                            }
                                        }

                                    "browse" ->
                                        uri.lastPathSegment?.let { browseId ->
                                            navController.navigate("album/$browseId")
                                        }

                                    "channel", "c" ->
                                        uri.lastPathSegment?.let { artistId ->
                                            navController.navigate("artist/$artistId")
                                        }

                                    else ->
                                        when {
                                            path == "watch" -> uri.getQueryParameter("v")
                                            uri.host == "youtu.be" -> path
                                            else -> null
                                        }?.let { videoId ->
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    YouTube.queue(listOf(videoId))
                                                }.onSuccess {
                                                    playerConnection?.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                                                            it.firstOrNull()?.toMediaMetadata()
                                                        )
                                                    )
                                                }.onFailure {
                                                    reportException(it)
                                                }
                                            }
                                        }
                                }
                            }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val currentTitle = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Explore.route -> R.string.explore
                            Screens.Library.route -> R.string.filter_library
                            else -> null
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                    ) {
                        Scaffold(
                            topBar = {
                                val playerBackground by rememberEnumPreference(
                                    key = PlayerBackgroundStyleKey,
                                    defaultValue = PlayerBackgroundStyle.DEFAULT
                                )
                                val safeSelectedValue = if (
                                    playerBackground == PlayerBackgroundStyle.BLUR &&
                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                                ) {
                                    PlayerBackgroundStyle.DEFAULT // Sin blur en versiones < Android 12 (S)
                                } else {
                                    playerBackground
                                }

                                if (shouldShowTopBar) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        // Capa base con color de fondo siempre visible
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(MaterialTheme.colorScheme.surface)
                                        )

                                        // Solo mostrar blur si safeSelectedValue es BLUR
                                        if (safeSelectedValue == PlayerBackgroundStyle.BLUR) {
                                            val playerConnection = LocalPlayerConnection.current ?: return@Box
                                            val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

                                            mediaMetadata?.thumbnailUrl?.let { imageUrl ->
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.FillBounds,
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .blur(35.dp)
                                                        .alpha(0.6f)
                                                        .drawWithContent {
                                                            drawContent()
                                                            drawRect(
                                                                brush = Brush.verticalGradient(
                                                                    colors = listOf(
                                                                        Color.Black.copy(alpha = 0.5f),
                                                                        Color.Transparent
                                                                    ),
                                                                    startY = 0f,
                                                                    endY = size.height * 0.6f
                                                                ),
                                                                blendMode = BlendMode.DstIn
                                                            )
                                                        }
                                                )
                                            }
                                        }

                                        TopAppBar(
                                            title = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Image(
                                                        painter = painterResource(R.drawable.flowtune),
                                                        contentDescription = "App Logo",
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "FlowTune",
                                                        style = MaterialTheme.typography.displayLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 27.sp
                                                    )
                                                }
                                            },
                                            actions = {
                                                IconButton(onClick = { onActiveChange(true) }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.search),
                                                        contentDescription = stringResource(R.string.search)
                                                    )
                                                }

                                                val context = LocalContext.current

                                                ProfileIconWithUpdateBadge(
                                                    currentVersion = BuildConfig.VERSION_NAME,
                                                    onProfileClick = {
                                                        try {
                                                            navController.navigate("settings")
                                                        } catch (e: Exception) {
                                                            logErrorToDownloads(context, e)
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            },
                                            scrollBehavior = searchBarScrollBehavior,
                                            colors = TopAppBarDefaults.topAppBarColors(
                                                containerColor = Color.Transparent
                                            )
                                        )
                                    }
                                }


                                if (active || navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                                    TopSearch(
                                        query = query,
                                        onQueryChange = onQueryChange,
                                        onSearch = onSearch,
                                        active = active,
                                        onActiveChange = onActiveChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(
                                                    when (searchSource) {
                                                        SearchSource.LOCAL -> R.string.search_library
                                                        SearchSource.ONLINE -> R.string.search_yt_music
                                                    }
                                                ),
                                            )
                                        },
                                        leadingIcon = {
                                            IconButton(
                                                onClick = {
                                                    when {
                                                        active -> onActiveChange(false)
                                                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                            navController.navigateUp()
                                                        }
                                                        else -> onActiveChange(true)
                                                    }
                                                },
                                                onLongClick = {
                                                    when {
                                                        active -> {}
                                                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                            navController.backToMain()
                                                        }
                                                        else -> {}
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painterResource(
                                                        if (active ||
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                        ) {
                                                            R.drawable.arrow_back
                                                        } else {
                                                            R.drawable.search
                                                        },
                                                    ),
                                                    contentDescription = null,
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Row {
                                                if (active) {
                                                    if (query.text.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                onQueryChange(TextFieldValue(""))
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.close),
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            searchSource =
                                                                if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                                        },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                when (searchSource) {
                                                                    SearchSource.LOCAL -> R.drawable.library_music
                                                                    SearchSource.ONLINE -> R.drawable.language
                                                                },
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .focusRequester(searchBarFocusRequester)
                                            .align(Alignment.TopCenter),
                                        focusRequester = searchBarFocusRequester
                                    ) {
                                        Crossfade(
                                            targetState = searchSource,
                                            label = "",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(bottom = if (!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                                .navigationBarsPadding(),
                                        ) { searchSource ->
                                            when (searchSource) {
                                                SearchSource.LOCAL -> LocalSearchScreen(
                                                    query = query.text,
                                                    navController = navController,
                                                    onDismiss = { onActiveChange(false) },
                                                    pureBlack = pureBlack,
                                                )
                                                SearchSource.ONLINE -> OnlineSearchScreen(
                                                    query = query.text,
                                                    onQueryChange = onQueryChange,
                                                    navController = navController,
                                                    onSearch = {
                                                        navController.navigate(
                                                            "search/${URLEncoder.encode(it, "UTF-8")}"
                                                        )
                                                        if (dataStore[PauseSearchHistoryKey] != true) {
                                                            database.query { insert(SearchHistory(query = it)) }
                                                        }
                                                    },
                                                    onDismiss = { onActiveChange(false) },
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                Box {
                                    BottomSheetPlayer(
                                        state = playerBottomSheetState,
                                        navController = navController,
                                    )
                                    NavigationBar(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(15.dp))
                                            .align(Alignment.BottomCenter)
                                            .offset {
                                                if (navigationBarHeight == 0.dp) {
                                                    IntOffset(
                                                        x = 0,
                                                        y = (bottomInset + NavigationBarHeight).roundToPx(),
                                                    )
                                                } else {
                                                    val slideOffset =
                                                        (bottomInset + NavigationBarHeight) *
                                                                playerBottomSheetState.progress.coerceIn(
                                                                    0f,
                                                                    1f,
                                                                )
                                                    val hideOffset =
                                                        (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                                    IntOffset(
                                                        x = 0,
                                                        y = (slideOffset + hideOffset).roundToPx(),
                                                    )
                                                }
                                            },
                                    ) {
                                        var lastTapTime by remember { mutableLongStateOf(0L) }
                                        var lastTappedIcon by remember { mutableStateOf<Int?>(null) }
                                        var navigateToExplore by remember { mutableStateOf(false) }
                                        navigationItems.fastForEach { screen ->
                                            val isSelected =
                                                navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

                                            NavigationBarItem(
                                                selected = isSelected,
                                                icon = {
                                                    Icon(
                                                        painter = painterResource(
                                                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                        ),
                                                        contentDescription = null,
                                                    )
                                                },
                                                label = {
                                                    if (!slimNav) {
                                                        Text(
                                                            text = stringResource(screen.titleId),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    val currentTapTime = System.currentTimeMillis()
                                                    val timeSinceLastTap =
                                                        currentTapTime - lastTapTime
                                                    val isDoubleTap =
                                                        screen.titleId == R.string.explore &&
                                                                lastTappedIcon == R.string.explore &&
                                                                timeSinceLastTap < 300

                                                    lastTapTime = currentTapTime
                                                    lastTappedIcon = screen.titleId

                                                    if (screen.titleId == R.string.explore) {
                                                        if (isDoubleTap) {
                                                            onActiveChange(true)
                                                            navigateToExplore = false
                                                        } else {
                                                            navigateToExplore = true
                                                            coroutineScope.launch {
                                                                delay(300)
                                                                if (navigateToExplore) {
                                                                    navigateToScreen(
                                                                        navController,
                                                                        screen
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        if (isSelected) {
                                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                                "scrollToTop",
                                                                true
                                                            )
                                                            coroutineScope.launch {
                                                                searchBarScrollBehavior.state.resetHeightOffset()
                                                            }
                                                        } else {
                                                            navigateToScreen(navController, screen)
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            var transitionDirection =
                                AnimatedContentTransitionScope.SlideDirection.Left

                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                if (navigationItems.fastAny { it.route == previousTab }) {
                                    val curIndex = navigationItems.indexOf(
                                        navigationItems.fastFirstOrNull {
                                            it.route == navBackStackEntry?.destination?.route
                                        }
                                    )

                                    val prevIndex = navigationItems.indexOf(
                                        navigationItems.fastFirstOrNull {
                                            it.route == previousTab
                                        }
                                    )

                                    if (prevIndex > curIndex)
                                        AnimatedContentTransitionScope.SlideDirection.Right.also {
                                            transitionDirection = it
                                        }
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                    NavigationTab.HOME -> Screens.Home
                                    NavigationTab.EXPLORE -> Screens.Explore
                                    NavigationTab.LIBRARY -> Screens.Library
                                }.route,
                                enterTransition = {
                                    if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                        fadeIn(tween(250))
                                    } else {
                                        fadeIn(tween(250)) + slideInHorizontally { it / 2 }
                                    }
                                },
                                exitTransition = {
                                    if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                        fadeOut(tween(200))
                                    } else {
                                        fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
                                    }
                                },
                                popEnterTransition = {
                                    if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith(
                                            "search/"
                                        ) == true) && targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeIn(tween(250))
                                    } else {
                                        fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
                                    }
                                },
                                popExitTransition = {
                                    if ((initialState.destination.route in topLevelScreens || initialState.destination.route?.startsWith(
                                            "search/"
                                        ) == true) && targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeOut(tween(200))
                                    } else {
                                        fadeOut(tween(200)) + slideOutHorizontally { it / 2 }
                                    }
                                },
                                modifier = Modifier.nestedScroll(
                                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                        navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                    ) {
                                        searchBarScrollBehavior.nestedScrollConnection
                                    } else {
                                        topAppBarScrollBehavior.nestedScrollConnection
                                    }
                                )
                            ) {
                                navigationBuilder(
                                    navController,
                                    topAppBarScrollBehavior,
                                    latestVersionName
                                )
                            }
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun navigateToScreen(
        navController: NavHostController,
        screen: Screens
    ) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun handlevideoIdIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        when {
            uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
            else -> null
        }?.let { videoId ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    YouTube.queue(listOf(videoId))
                }.onSuccess {
                    playerConnection?.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                            it.firstOrNull()?.toMediaMetadata()
                        )
                    )
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.abhiram.flowtune.action.SEARCH"
        const val ACTION_EXPLORE = "com.abhiram.flowtune.action.EXPLORE"
        const val ACTION_LIBRARY = "com.abhiram.flowtune.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }

@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    val checkNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }

    SwitchPreference(
        title = { Text(stringResource(R.string.notification)) },
        icon = {
            Icon(
                painter = painterResource(id = if (permissionGranted) R.drawable.notification_on else R.drawable.notification_off),
                contentDescription = null
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            if (checked && !permissionGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            // Note: We don't update permissionGranted here because it will be updated by the LaunchedEffect
        }
    )
}

@Composable
fun SwitchPreference(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                icon()
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f)) {
                title()
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}


suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/abhiram/flowtune/releases/latest")
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

@Composable
fun ProfileIconWithUpdateBadge(
    currentVersion: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val customAvatarUri by avatarManager.getCustomAvatarUri.collectAsState(initial = null)
    var showUpdateBadge by remember { mutableStateOf(false) }
    val updatedOnClick = rememberUpdatedState(onProfileClick)

    // 
    LaunchedEffect(currentVersion) {
        try {
            val latestVersion = withContext(Dispatchers.IO) { checkForUpdates() }
            showUpdateBadge = latestVersion?.let { isNewerVersion(it, currentVersion) } ?: false
        } catch (e: Exception) {
            Timber.tag("ProfileIcon").e("Error checking for updates: ${e.message}")
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                try {
                    updatedOnClick.value()
                } catch (e: Exception) {
                    logErrorToDownloads(context, e)
                }
            }
    ) {
        // Avatar o ícono predeterminado
        Box(contentAlignment = Alignment.Center) {
            customAvatarUri?.toUri()?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Avatar personalizado",
                    modifier = modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Avatar predeterminado",
                    modifier = modifier
                )
            }

            // Badge de actualización
            if (showUpdateBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.update),
                        contentDescription = "Actualización disponible",
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}


fun logErrorToDownloads(context: Context, e: Exception) {
    val fileName = "error_log_${
        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
    }.txt"
    val logText = buildString {
        append("Exception: ${e.message}\n")
        append("Stacktrace:\n")
        e.stackTrace.forEach { append("$it\n") }
    }

    try {
        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri == null) {
                Toast.makeText(context, "Error al crear el archivo en Descargas", Toast.LENGTH_LONG).show()
                return
            }

            val stream = resolver.openOutputStream(uri)
            if (stream == null) {
                Toast.makeText(context, "No se pudo abrir el archivo para escribir", Toast.LENGTH_LONG).show()
                return
            }

            // Escribimos antes de cambiar IS_PENDING
            stream.use {
                it.write(logText.toByteArray())
            }

            // Marcamos el archivo como disponible
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            // Retornamos null porque ya lo usamos
            null
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }

        // Solo para API < 29
        outputStream?.use {
            it.write(logText.toByteArray())
        }

        Toast.makeText(context, "Log guardado en Descargas como $fileName", Toast.LENGTH_LONG).show()

    } catch (ex: Exception) {
        ex.printStackTrace()
        Toast.makeText(context, "Error al guardar log: ${ex.message}", Toast.LENGTH_LONG).show()
    }
}

