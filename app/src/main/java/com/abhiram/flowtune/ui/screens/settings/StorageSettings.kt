package com.abhiram.flowtune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.abhiram.flowtune.LocalPlayerAwareWindowInsets
import com.abhiram.flowtune.LocalPlayerConnection
import com.abhiram.flowtune.R
import com.abhiram.flowtune.constants.MaxImageCacheSizeKey
import com.abhiram.flowtune.constants.MaxSongCacheSizeKey
import com.abhiram.flowtune.extensions.tryOrNull
import com.abhiram.flowtune.ui.component.AnimatedIconButton
import com.abhiram.flowtune.ui.component.EnhancedListPreference
import com.abhiram.flowtune.ui.component.EnhancedPreferenceEntry
import com.abhiram.flowtune.ui.component.PreferenceGroupTitle
import com.abhiram.flowtune.ui.utils.backToMain
import com.abhiram.flowtune.ui.utils.formatFileSize
import com.abhiram.flowtune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )

    var imageCacheSize by remember { mutableStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableStateOf(tryOrNull { playerCache.cacheSpace } ?: 0) }
    var downloadCacheSize by remember { mutableStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showDownloadClearConfirm by remember { mutableStateOf(false) }
    var showSongCacheClearConfirm by remember { mutableStateOf(false) }
    var showImageCacheClearConfirm by remember { mutableStateOf(false) }

    val imageCacheProgress by animateFloatAsState(
        targetValue = (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f),
        label = "",
    )
    val playerCacheProgress by animateFloatAsState(
        targetValue = (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(0f, 1f),
        label = "",
    )
    val downloadCachePercentage = if(downloadCacheSize > 0) {
        (downloadCacheSize.toFloat() / (8192 * 1024 * 1024L)).coerceIn(0f, 1f) * 100
    } else 0f

    // Refresh cache information periodically
    LaunchedEffect(Unit) {
        while (isActive) {
            imageCacheSize = imageDiskCache.size
            playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0
            downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0
            delay(500)
        }
    }

    // Manual refresh with animation
    fun refreshCacheInfo() {
        coroutineScope.launch {
            isRefreshing = true
            withContext(Dispatchers.IO) {
                imageCacheSize = imageDiskCache.size
                playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0
                downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0
                delay(800) // Show refresh animation for a minimum time
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.storage))
                    }
                },
                navigationIcon = {
                    AnimatedIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    AnimatedIconButton(
                        onClick = { refreshCacheInfo() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.replay),
                                contentDescription = null,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()),
        ) {
            // Storage overview card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.storage),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.storage_overview),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    StorageItem(
                        title = stringResource(R.string.downloaded_songs),
                        icon = R.drawable.download,
                        size = downloadCacheSize,
                        progress = downloadCachePercentage,
                        maxSize = null
                    )

                    StorageItem(
                        title = stringResource(R.string.song_cache),
                        icon = R.drawable.music_note,
                        size = playerCacheSize,
                        progress = playerCacheProgress * 100,
                        maxSize = if (maxSongCacheSize != -1) maxSongCacheSize * 1024 * 1024L else null
                    )

                    StorageItem(
                        title = stringResource(R.string.image_cache),
                        icon = R.drawable.image,
                        size = imageCacheSize,
                        progress = imageCacheProgress * 100,
                        maxSize = imageDiskCache.maxSize
                    )
                }
            }

            // Downloaded Songs Section
            StorageSection(
                title = stringResource(R.string.downloaded_songs),
                icon = R.drawable.download
            ) {
                Text(
                    text = stringResource(R.string.size_used, formatFileSize(downloadCacheSize)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )

                EnhancedPreferenceEntry(
                    title = stringResource(R.string.clear_all_downloads),
                    icon = R.drawable.delete,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showDownloadClearConfirm = true },
                )
            }

            // Song Cache Section
            StorageSection(
                title = stringResource(R.string.song_cache),
                icon = R.drawable.music_note
            ) {
                if (maxSongCacheSize == -1) {
                    Text(
                        text = stringResource(R.string.size_used, formatFileSize(playerCacheSize)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { playerCacheProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        text = stringResource(
                            R.string.size_used,
                            "${formatFileSize(playerCacheSize)} / ${
                                formatFileSize(
                                    maxSongCacheSize * 1024 * 1024L,
                                )
                            }",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }

                EnhancedListPreference(
                    title = stringResource(R.string.max_cache_size),
                    icon = R.drawable.settings,
                    selectedValue = maxSongCacheSize,
                    values = listOf(128, 256, 512, 1024, 2048, 4096, 8192, -1),
                    valueText = {
                        if (it == -1) stringResource(R.string.unlimited) else formatFileSize(it * 1024 * 1024L)
                    },
                    onValueSelected = {
                        onMaxSongCacheSizeChange(it)
                        refreshCacheInfo()
                    },
                )

                EnhancedPreferenceEntry(
                    title = stringResource(R.string.clear_song_cache),
                    icon = R.drawable.delete,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showSongCacheClearConfirm = true },
                )
            }

            // Image Cache Section
            StorageSection(
                title = stringResource(R.string.image_cache),
                icon = R.drawable.image
            ) {
                LinearProgressIndicator(
                    progress = { imageCacheProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = stringResource(
                        R.string.size_used,
                        "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )

                EnhancedListPreference(
                    title = stringResource(R.string.max_cache_size),
                    icon = R.drawable.settings,
                    selectedValue = maxImageCacheSize,
                    values = listOf(128, 256, 512, 1024, 2048, 4096, 8192),
                    valueText = { formatFileSize(it * 1024 * 1024L) },
                    onValueSelected = {
                        onMaxImageCacheSizeChange(it)
                        refreshCacheInfo()
                    },
                )

                EnhancedPreferenceEntry(
                    title = stringResource(R.string.clear_image_cache),
                    icon = R.drawable.delete,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showImageCacheClearConfirm = true },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Dialogs
    if (showDownloadClearConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_all_downloads),
            message = stringResource(R.string.clear_downloads_confirmation),
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    downloadCache.keys.forEach { key ->
                        downloadCache.removeResource(key)
                    }
                    refreshCacheInfo()
                }
                showDownloadClearConfirm = false
            },
            onDismiss = { showDownloadClearConfirm = false }
        )
    }

    if (showSongCacheClearConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_song_cache),
            message = stringResource(R.string.clear_song_cache_confirmation),
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    playerCache.keys.forEach { key ->
                        playerCache.removeResource(key)
                    }
                    refreshCacheInfo()
                }
                showSongCacheClearConfirm = false
            },
            onDismiss = { showSongCacheClearConfirm = false }
        )
    }

    if (showImageCacheClearConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_image_cache),
            message = stringResource(R.string.clear_image_cache_confirmation),
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    imageDiskCache.clear()
                    refreshCacheInfo()
                }
                showImageCacheClearConfirm = false
            },
            onDismiss = { showImageCacheClearConfirm = false }
        )
    }
}

@Composable
fun StorageSection(
    title: String,
    icon: Int,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            onClick = { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Icon(
                    painter = painterResource(if (expanded) R.drawable.expand_less else R.drawable.expand_more),
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun StorageItem(
    title: String,
    icon: Int,
    size: Long,
    progress: Float,
    maxSize: Long?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (maxSize != null) {
                    "${formatFileSize(size)} / ${formatFileSize(maxSize)}"
                } else {
                    formatFileSize(size)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${progress.toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}