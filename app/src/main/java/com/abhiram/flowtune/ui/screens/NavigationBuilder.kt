package com.abhiram.flowtune.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.abhiram.flowtune.BuildConfig
import com.abhiram.flowtune.ui.screens.artist.ArtistItemsScreen
import com.abhiram.flowtune.ui.screens.artist.ArtistScreen
import com.abhiram.flowtune.ui.screens.artist.ArtistSongsScreen
import com.abhiram.flowtune.ui.screens.library.LibraryScreen
import com.abhiram.flowtune.ui.screens.playlist.AutoPlaylistScreen
import com.abhiram.flowtune.ui.screens.playlist.CachePlaylistScreen
import com.abhiram.flowtune.ui.screens.playlist.LocalPlaylistScreen
import com.abhiram.flowtune.ui.screens.playlist.OnlinePlaylistScreen
import com.abhiram.flowtune.ui.screens.playlist.TopPlaylistScreen
import com.abhiram.flowtune.ui.screens.search.OnlineSearchResult
import com.abhiram.flowtune.ui.screens.settings.AboutScreen
import com.abhiram.flowtune.ui.screens.settings.AccountSettings
import com.abhiram.flowtune.ui.screens.settings.AppearanceSettings
import com.abhiram.flowtune.ui.screens.settings.BackupAndRestore
import com.abhiram.flowtune.ui.screens.settings.ContentSettings
import com.abhiram.flowtune.ui.screens.settings.DiscordLoginScreen
import com.abhiram.flowtune.ui.screens.settings.DiscordSettings
import com.abhiram.flowtune.ui.screens.settings.PlayerSettings
import com.abhiram.flowtune.ui.screens.settings.PrivacySettings
import com.abhiram.flowtune.ui.screens.settings.ProblemSolverScreen
import com.abhiram.flowtune.ui.screens.settings.SettingsScreen
import com.abhiram.flowtune.ui.screens.settings.StorageSettings


// Animation constants
private const val ANIMATION_DURATION = 300
private val ANIMATION_SPEC = tween<IntOffset>(ANIMATION_DURATION) // Changed to IntOffset

// Common transitions - now using IntOffset instead of Float
private val slideInFromRight = slideInHorizontally(animationSpec = ANIMATION_SPEC) { fullWidth -> fullWidth }
private val slideInFromLeft = slideInHorizontally(animationSpec = ANIMATION_SPEC) { fullWidth -> -fullWidth }
private val slideOutToRight = slideOutHorizontally(animationSpec = ANIMATION_SPEC) { fullWidth -> fullWidth }
private val slideOutToLeft = slideOutHorizontally(animationSpec = ANIMATION_SPEC) { fullWidth -> -fullWidth }

// Fade animations remain the same (they use Float)
private val fadeIn = fadeIn(animationSpec = tween(ANIMATION_DURATION))
private val fadeOut = fadeOut(animationSpec = tween(ANIMATION_DURATION))


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    
    
    composable(
    Screens.Home.route,
    enterTransition = { fadeIn + slideInFromLeft },
    exitTransition = { fadeOut + slideOutToLeft },
    popEnterTransition = { fadeIn + slideInFromRight },
    popExitTransition = { fadeOut + slideOutToRight }
) {
    HomeScreen(navController)
}

    composable(
        Screens.Library.route,
        enterTransition = { fadeIn + slideInFromRight },
        exitTransition = { fadeOut + slideOutToRight },
        popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
    LibraryScreen(navController)
}

    composable(
        Screens.Explore.route,
        enterTransition = { fadeIn + slideInFromRight },
        exitTransition = { fadeOut + slideOutToRight },
        popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
    ExploreScreen(navController)
}
    
    
    
    
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("mood_and_genres") {
        MoodAndGenresScreen(navController, scrollBehavior)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("settings/problem_solver") {
        ProblemSolverScreen(navController)
    }





    composable(
        route = "search/{query}",
        arguments =
            listOf(
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getString("artistId")!!
        if (artistId.startsWith("LA")) {
            ArtistSongsScreen(navController, scrollBehavior)
        } else {
            ArtistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }



    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }


    composable(
    "settings",
    enterTransition = { fadeIn + slideInFromLeft },
    exitTransition = { fadeOut + slideOutToLeft },
    popEnterTransition = { fadeIn + slideInFromRight },
    popExitTransition = { fadeOut + slideOutToRight }
) {
    val latestVersion by mutableLongStateOf(BuildConfig.VERSION_CODE.toLong())
    SettingsScreen(latestVersion, navController, scrollBehavior)
}

    composable(
        "settings/appearance",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
    AppearanceSettings(navController, scrollBehavior)
}

    composable(
        "settings/account",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        AccountSettings(navController, scrollBehavior)
    }
    
    composable("settings/content",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/player",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/storage",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
    composable("settings/about",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        AboutScreen(navController, scrollBehavior)
    }
    composable("login",
         enterTransition = { fadeIn + slideInFromRight },
         exitTransition = { fadeOut + slideOutToRight },
         popEnterTransition = { fadeIn + slideInFromLeft },
        popExitTransition = { fadeOut + slideOutToLeft }
) {
        LoginScreen(navController)
    }
}

