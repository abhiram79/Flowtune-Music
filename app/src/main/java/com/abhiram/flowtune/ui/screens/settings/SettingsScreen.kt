package com.abhiram.flowtune.ui.screens.settings

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.abhiram.innertube.utils.parseCookieString
import com.abhiram.flowtune.BuildConfig
import com.abhiram.flowtune.LocalPlayerAwareWindowInsets
import com.abhiram.flowtune.R
import com.abhiram.flowtune.constants.AccountNameKey
import com.abhiram.flowtune.constants.InnerTubeCookieKey
import com.abhiram.flowtune.ui.component.IconButton
import com.abhiram.flowtune.ui.component.PreferenceEntry
import com.abhiram.flowtune.ui.utils.backToMain
import com.abhiram.flowtune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap


@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0) // ✅ Solo en Android 13+
            )
        } else {
            @Suppress("DEPRECATION") // Evita advertencias en compilación
            context.packageManager.getPackageInfo(
                context.packageName,
                0 // ✅ Compatible con Android 12 y versiones anteriores
            )
        }
        packageInfo.versionName ?: "Unknown" // Si versionName es null, retorna "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown" // Si ocurre un error, retorna "Unknown"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VersionCard(uriHandler: UriHandler) {
    val context = LocalContext.current
    val appVersion = remember { getAppVersion(context) }


    Spacer(Modifier.height(25.dp))
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
//            .clip(RoundedCornerShape(38.dp))
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(85.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,

            ),
        shape = RoundedCornerShape(38.dp),
        onClick = { uriHandler.openUri("https://github.com/abhiram/flowtune/releases/latest") }
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(38.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${stringResource(R.string.Version)} $appVersion",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),


                )
        }
    }
}


@Composable
fun UpdateCard(latestVersion: String = "") {
    val context = LocalContext.current
    var showUpdateCard by remember { mutableStateOf(false) }
    var currentLatestVersion by remember { mutableStateOf(latestVersion) }
    var showDownloadDialog by remember { mutableStateOf(false) }



    // Verificar actualizaciones al inicio
    LaunchedEffect(Unit) {
        val newVersion = checkForUpdates()
        if (newVersion != null && isNewerVersion(newVersion, BuildConfig.VERSION_NAME)) {
            showUpdateCard = true
            currentLatestVersion = newVersion
        }
    }

    // Diálogo de descarga
    if (showDownloadDialog) {
        UpdateDownloadDialog(
            latestVersion = currentLatestVersion,
            onDismiss = { showDownloadDialog = false }
        )
    }

    // Card de actualización disponible
    if (showUpdateCard) {
        Spacer(Modifier.height(25.dp))
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(170.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(38.dp),
            onClick = {
                showDownloadDialog = true
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(3.dp))

                val newVersion = stringResource(R.string.NewVersion)
                val tapToUpdate = stringResource(R.string.tap_to_update)
                val warn = stringResource(R.string.warn)

                // Línea principal: "NewVersion: currentLatestVersion"
                Text(
                    text = "$newVersion: $currentLatestVersion",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(Modifier.height(8.dp))

                // Advertencia
                Text(
                    text = "$warn ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.error,
                )

                Spacer(Modifier.height(8.dp))

                // Tap to update (acción)
                Text(
                    text = tapToUpdate,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

        }
    }
}

@Composable
fun UpdateDownloadDialog(
    latestVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatus by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }
    var downloadedApkUri by remember { mutableStateOf<Uri?>(null) }
    val downloadScope = rememberCoroutineScope()

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Cuando vuelve de la pantalla de permisos, verificamos de nuevo si podemos instalar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls() && downloadedApkUri != null) {
                // Ahora que tenemos el permiso, procedemos con la instalación
                installApk(context, downloadedApkUri!!)
            }
        }
    }

    // Dialog para mostrar progreso y opciones
    Dialog(onDismissRequest = {
        if (downloadStatus != DownloadStatus.DOWNLOADING) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.update_version, latestVersion),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (downloadStatus) {
                    DownloadStatus.NOT_STARTED -> {
                        Text(stringResource(R.string.download_question))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(onClick = {
                                downloadStatus = DownloadStatus.DOWNLOADING
                                downloadScope.launch {
                                    downloadedApkUri =
                                        downloadApk(context, latestVersion) { progress ->
                                            downloadProgress = progress
                                            if (progress >= 1f) {
                                                downloadStatus = DownloadStatus.COMPLETED
                                            }
                                        }
                                }
                            }) {
                                Text(stringResource(R.string.download))
                            }
                        }
                    }

                    DownloadStatus.DOWNLOADING -> {
                        Text(stringResource(R.string.downloadingup))
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    DownloadStatus.COMPLETED -> {
                        Text(stringResource(R.string.download_completed))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.close))
                            }
                            // Modifica solo la parte del botón de instalar en el caso COMPLETED
                            Button(onClick = {
                                if (downloadedApkUri != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (!context.packageManager.canRequestPackageInstalls()) {
                                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                .setData("package:${context.packageName}".toUri())

                                            installPermissionLauncher.launch(intent)
                                        } else {
                                            // Pasar directamente el objeto Uri, no una cadena
                                            installApk(context, downloadedApkUri!!)
                                        }
                                    } else {
                                        // Pasar directamente el objeto Uri, no una cadena
                                        installApk(context, downloadedApkUri!!)
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.install))
                            }
                        }
                    }

                    DownloadStatus.ERROR -> {
                        Text(stringResource(R.string.download_errorup))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
}


enum class DownloadStatus {
    NOT_STARTED,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

suspend fun downloadApk(
    context: Context,
    version: String,
    onProgressUpdate: (Float) -> Unit
): Uri? = withContext(Dispatchers.IO) {
    try {
        // URL del APK (ajusta esta URL según donde estén alojados tus archivos APK)
        val apkUrl =
            "https://github.com/abhiram/flowtune/releases/download/$version/app-release.apk"

        // Crear archivo de destino
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadDir, "app-release-$version.apk")

        // Si ya existe, bórralo
        if (apkFile.exists()) {
            apkFile.delete()
        }

        // Configurar el DownloadManager
        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle("Descargando flowtune v$version")
            .setDescription("Descargando actualización...")
            .setDestinationUri(Uri.fromFile(apkFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Monitorear el progreso
        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesDownloadedColumn =
                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalColumn =
                    cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                if (statusColumn != -1 && bytesDownloadedColumn != -1 && bytesTotalColumn != -1) {
                    val status = cursor.getInt(statusColumn)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedColumn)
                    val bytesTotal = cursor.getLong(bytesTotalColumn)

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isDownloading = false
                            onProgressUpdate(1f)
                        }

                        DownloadManager.STATUS_FAILED -> {
                            isDownloading = false
                            onProgressUpdate(0f)
                            return@withContext null
                        }

                        else -> {
                            if (bytesTotal > 0) {
                                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                onProgressUpdate(progress)
                            }
                        }
                    }
                }
            }
            cursor.close()
            delay(100) // Esperar un poco antes de actualizar de nuevo
        }

        // Crear Uri para la instalación con FileProvider
        return@withContext FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

// Función corregida para instalar APK usando directamente la URI
fun installApk(context: Context, apkUri: Uri) {
    // Verificar y solicitar permiso para instalar APKs en Android 8+ (API 26+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val pm = context.packageManager
        val isAllowed = pm.canRequestPackageInstalls()
        if (!isAllowed) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData("package:${context.packageName}".toUri())
            context.startActivity(intent)
            return // Salimos para que el usuario conceda el permiso antes de continuar
        }
    }

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    context.startActivity(installIntent)
}

// Estas funciones ya las tenías
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    latestVersion: Long,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {


    val uriHandler = LocalUriHandler.current


//    var isBetaFunEnabled by remember { mutableStateOf(false) }



    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )
        val context = LocalContext.current
        val avatarManager = remember { AvatarPreferenceManager(context) }
        val customAvatarUri by avatarManager.getCustomAvatarUri.collectAsState(initial = null)
        val accountName by rememberPreference(AccountNameKey, "")
        val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
        val isLoggedIn = remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn) {
                // Avatar circular para usuario
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (customAvatarUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(data = customAvatarUri!!.toUri())
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Avatar con inicial del nombre si no hay imagen
                        Text(
                            text = accountName.first().toString().uppercase(),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = accountName.replace("@", ""),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // Logo para usuario no autenticado
                Icon(
                    painter = painterResource(R.drawable.flowtune_monochrome),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "flowtune",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        PreferenceEntry(
            title = { Text(stringResource(R.string.appearance)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            onClick = { navController.navigate("settings/appearance") },
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.account)) },
            icon = { Icon(painterResource(R.drawable.person), null) },
            onClick = { navController.navigate("settings/account") },
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.content)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            onClick = { navController.navigate("settings/content") },
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.player_and_audio)) },
            icon = { Icon(painterResource(R.drawable.play), null) },
            onClick = { navController.navigate("settings/player") },
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.storage)) },
            icon = { Icon(painterResource(R.drawable.storage), null) },
            onClick = { navController.navigate("settings/storage") },
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.privacy)) },
            icon = { Icon(painterResource(R.drawable.security), null) },
            onClick = { navController.navigate("settings/privacy") },
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.backup_restore)) },
            icon = { Icon(painterResource(R.drawable.restore), null) },
            onClick = { navController.navigate("settings/backup_restore") },
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.about)) },
            icon = { Icon(painterResource(R.drawable.info), null) },
            onClick = { navController.navigate("settings/about") }
        )
//        PreferenceEntry(
//            title = { Text(stringResource(R.string.problem_solver)) },
//            icon = { Icon(painterResource(R.drawable.apps), null) },
//            onClick = { navController.navigate("settings/problem_solver") }
//        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.Donate)) },
            icon = { Icon(painterResource(R.drawable.donate), null) },
            onClick = { uriHandler.openUri("https://buymeacoffee.com/arturocervantes") }
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.Telegramchanel)) },
            icon = { Icon(painterResource(R.drawable.telegram), null) },
            onClick = { uriHandler.openUri("https://t.me/flowtune_chat") }
        )


        TranslatePreference(uriHandler = uriHandler)

        ChangelogButtonWithPopup()



        UpdateCard()
        Spacer(Modifier.height(25.dp))


        VersionCard(uriHandler)

        Spacer(Modifier.height(25.dp))


    }

    TopAppBar(


        title = { Text(stringResource(R.string.settings)) },
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            )

            {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior

    )
}


/**
 * Clase de estado para el changelog que contiene toda la información
 * necesaria para representar los diferentes estados de la UI
 */
data class ChangelogState(
    val changes: String = "", // Ahora guardamos el markdown completo en lugar de solo una lista
    val isLoading: Boolean = true,
    val error: String? = null
)

/** Clase ViewModel para manejar la lógica de negocio y el estado de la UI */
class ChangelogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChangelogState())
    val uiState: StateFlow<ChangelogState> = _uiState.asStateFlow()

    // Cache simple para evitar llamadas repetidas a la API
    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val cacheTimeMs = 30 * 60 * 1000 // 30 minutos

    fun loadChangelog(repoOwner: String, repoName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val cacheKey = "$repoOwner/$repoName"

                // Verificar si hay datos en caché
                val cachedData = getCachedChanges(cacheKey)
                if (cachedData != null) {
                    _uiState.update {
                        it.copy(
                            changes = cachedData,
                            isLoading = false
                        )
                    }
                    return@launch
                }

                // Si no hay caché, hacer la petición
                val markdownContent = fetchReleaseMarkdown(repoOwner, repoName)

                // Guardar en caché
                cacheChanges(cacheKey, markdownContent)

                _uiState.update {
                    it.copy(
                        changes = markdownContent,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ChangelogViewModel", "Error cargando changelog", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al cargar los cambios: ${e.message}"
                    )
                }
            }
        }
    }

    private fun getCachedChanges(key: String): String? {
        val cached = cache[key] ?: return null
        val (content, timestamp) = cached

        // Comprobar si el caché ha expirado
        if (System.currentTimeMillis() - timestamp > cacheTimeMs) {
            cache.remove(key)
            return null
        }

        return content
    }

    private fun cacheChanges(key: String, content: String) {
        cache[key] = content to System.currentTimeMillis()
    }

    /** Función para hacer la petición a la API de GitHub con reintentos */
    private suspend fun fetchReleaseMarkdown(owner: String, repo: String): String =
        withContext(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000 // 15 segundos de timeout
                    connection.readTimeout = 15000
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        if (attempt == 2) throw IOException("Error HTTP: ${connection.responseCode}")
                        delay(1000)

                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    return@withContext jsonObject.optString("body", "")
                } catch (e: Exception) {
                    if (attempt == 2) throw e
                    delay(1000)
                }
            }

            throw IOException("No se pudo obtener la información después de los reintentos")
        }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val lines = markdown.lines()

    Column(modifier = modifier) {
        for (line in lines) {
            val trimmedLine = line.trim()

            // Filtrar líneas que contienen imágenes (Markdown: ![alt text](url))
            if (trimmedLine.matches(Regex("^!\\[.*?\\]\\(.*?\\)\$"))) {
                continue // Omitir líneas que contienen imágenes
            }

            when {
                trimmedLine.startsWith("# ") -> {
                    // Encabezado H1
                    Text(
                        text = trimmedLine.substring(2),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                trimmedLine.startsWith("## ") -> {
                    // Encabezado H2
                    Text(
                        text = trimmedLine.substring(3),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                trimmedLine.startsWith("### ") -> {
                    // Encabezado H3
                    Text(
                        text = trimmedLine.substring(4),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") -> {
                    // Lista no ordenada
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                        )

                        // Procesar el contenido de la lista para darle formato
                        val itemContent = trimmedLine.substring(2)
                        val annotatedString = buildAnnotatedString {
                            var currentIndex = 0

                            // Buscar texto en negrita: **texto** o __texto__
                            val boldPattern = Regex("(\\*\\*|__)(.+?)(\\*\\*|__)")
                            val boldMatches = boldPattern.findAll(itemContent)

                            for (match in boldMatches) {
                                // Añadir texto regular antes del formato
                                if (match.range.first > currentIndex) {
                                    append(itemContent.substring(currentIndex, match.range.first))
                                }

                                // Añadir texto en negrita
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(match.groupValues[2])
                                }

                                currentIndex = match.range.last + 1
                            }

                            // Añadir el resto del texto
                            if (currentIndex < itemContent.length) {
                                append(itemContent.substring(currentIndex))
                            }
                        }

                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                trimmedLine.startsWith("> ") -> {
                    // Cita
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = trimmedLine.substring(2),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                trimmedLine.startsWith("```") -> {
                    // Bloque de código - simplemente lo mostramos como texto monoespaciado
                    if (trimmedLine.length > 3) {
                        Text(
                            text = "Código: " + trimmedLine.substring(3),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                trimmedLine.startsWith("---") -> {
                    // Separador horizontal
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                trimmedLine.isNotEmpty() -> {
                    // Texto normal
                    val annotatedString = buildAnnotatedString {
                        var currentIndex = 0

                        // Procesar negrita: **texto** o __texto__
                        val boldPattern = Regex("(\\*\\*|__)(.+?)(\\*\\*|__)")
                        val boldMatches = boldPattern.findAll(trimmedLine)

                        for (match in boldMatches) {
                            // Añadir texto regular antes del formato
                            if (match.range.first > currentIndex) {
                                append(trimmedLine.substring(currentIndex, match.range.first))
                            }

                            // Añadir texto en negrita
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(match.groupValues[2])
                            }

                            currentIndex = match.range.last + 1
                        }

                        // Procesar cursiva: *texto* o _texto_ (si no está dentro de negrita)
                        if (currentIndex < trimmedLine.length) {
                            val remainingText = trimmedLine.substring(currentIndex)
                            val italicPattern = Regex("(\\*|_)([^*_]+?)(\\*|_)")
                            val italicMatches = italicPattern.findAll(remainingText)

                            var italicIndex = 0
                            for (match in italicMatches) {
                                // Añadir texto regular antes del formato
                                if (match.range.first > italicIndex) {
                                    append(remainingText.substring(italicIndex, match.range.first))
                                }

                                // Añadir texto en cursiva
                                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(match.groupValues[2])
                                }

                                italicIndex = match.range.last + 1
                            }

                            // Añadir el resto del texto
                            if (italicIndex < remainingText.length) {
                                append(remainingText.substring(italicIndex))
                            }
                        }

                        // Si no se ha procesado ningún formato, simplemente añadir el texto original
                        if (length == 0) {
                            append(trimmedLine)
                        }
                    }

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                else -> {
                    // Línea vacía - espacio
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}


/** Botón de preferencia que muestra un modal con el changelog */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogButtonWithPopup(
    viewModel: ChangelogViewModel = viewModel()
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    // Botón en las preferencias
    PreferenceEntry(
        title = { Text(stringResource(R.string.Changelog)) },
        icon = { Icon(painterResource(R.drawable.schedule), null) },
        onClick = {
            showBottomSheet = true
        }
    )

    // Modal de bottom sheet que se muestra al hacer clic
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    ChangelogScreen(viewModel)
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

/** Tarjeta que muestra el changelog con formato Markdown */
@Composable
fun AutoChangelogCard(
    viewModel: ChangelogViewModel,
    repoOwner: String,
    repoName: String
) {
    val uiState by viewModel.uiState.collectAsState()

    // Cargar los cambios cuando el componente se compone por primera vez
    LaunchedEffect(key1 = repoOwner, key2 = repoName) {
        viewModel.loadChangelog(repoOwner, repoName)
    }

    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                stringResource(R.string.changelogs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Column {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            viewModel.loadChangelog(repoOwner, repoName)
                        }) {
                            Text("Reintentar")
                        }
                    }
                }

                uiState.changes.isEmpty() -> {
                    Text(stringResource(R.string.no_changes))
                }

                else -> {
                    // Renderizar el markdown
                    MarkdownText(
                        markdown = uiState.changes,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Pantalla principal de changelog */
@Composable
fun ChangelogScreen(viewModel: ChangelogViewModel = viewModel()) {
    AutoChangelogCard(
        viewModel = viewModel,
        repoOwner = "abhiram",
        repoName = "flowtune"
    )
}

@Composable
fun TranslatePreference(uriHandler: UriHandler) {
    var showDialog by remember { mutableStateOf(false) }

    PreferenceEntry(
        title = { Text(stringResource(R.string.Translate)) },
        icon = { Icon(painterResource(R.drawable.translate), null) },
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.Redirección)) },
            text = { Text(stringResource(R.string.poeditor_redirect)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        uriHandler.openUri("https://poeditor.com/join/project/208BwCVazA")
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}



