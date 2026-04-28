package ru.jarvis.telegramka.ui.chats

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.domain.model.Chat
import ru.jarvis.telegramka.domain.model.User
import ru.jarvis.telegramka.navigation.Screen
import ru.jarvis.telegramka.ui.login.GradientButton
import ru.jarvis.telegramka.ui.theme.AppMotion
import ru.jarvis.telegramka.ui.theme.TelegramkaTheme
import ru.jarvis.telegramka.ui.utils.UserAvatar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatsScreen(
    navController: NavController,
    viewModel: ChatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchingUser by viewModel.isSearchingUser.collectAsStateWithLifecycle()
    val searchUserError by viewModel.searchUserError.collectAsStateWithLifecycle()
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var activeDownloadId by remember { mutableLongStateOf(-1L) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                val byteArray = inputStream.readBytes()
                viewModel.updateAvatar(byteArray)
            }
        }
    }

    LaunchedEffect(appUpdateState.errorMessage) {
        val message = appUpdateState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeAppUpdateError()
    }

    LaunchedEffect(infoMessage) {
        val message = infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        infoMessage = null
    }

    DisposableEffect(context, activeDownloadId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != activeDownloadId || downloadId == -1L) return

                activeDownloadId = -1L
                handleApkDownloadCompleted(
                    context = receiverContext,
                    downloadId = downloadId,
                    onSuccess = {
                        viewModel.onAppUpdateDownloadFinished()
                        infoMessage = "Загрузка завершена"
                    },
                    onInstallerOpened = {
                        infoMessage = "Открываю установщик"
                    },
                    onInstallPermissionRequired = {
                        infoMessage = "Разреши установку приложений для Telegramka"
                    },
                    onFailure = { message ->
                        viewModel.onAppUpdateDownloadFailed(message)
                    }
                )
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(navigationEvent) {
        navigationEvent?.let { event ->
            val uiStateValue = viewModel.uiState.value
            if (uiStateValue is ChatsUiState.Success) {
                when (event) {
                    is ChatsNavigationEvent.NavigateToChat -> {
                        showDialog = false
                        navController.navigate(
                            Screen.Chat.createRoute(
                                id = event.id,
                                name = event.name,
                                nickname = event.nickname,
                                currentUserId = uiStateValue.currentUser.id,
                                avatarUrl = (uiStateValue.chats.find { it.id == event.id })?.avatarUrl
                            )
                        )
                    }
                }
                viewModel.consumeNavigationEvent()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(AppMotion.ContentDuration))) togetherWith (fadeOut(animationSpec = tween(AppMotion.ExitDuration)))
            },
            label = "MainContent",
            modifier = Modifier.padding(paddingValues)
        ) { state ->
            when (state) {
                is ChatsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChatsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ChatsUiState.Success -> {
                    val filteredChats = state.chats.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.nickname.contains(searchQuery, ignoreCase = true)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        TopBar(
                            user = state.currentUser,
                            onAddContact = { showDialog = true },
                            onLogout = {
                                viewModel.onLogout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Chats.route) { inclusive = true }
                                }
                            },
                            onUpdateAvatar = {
                                imagePickerLauncher.launch("image/*")
                            },
                            onUpdateApplication = {
                                val latestVersion = appUpdateState.latestVersion ?: return@TopBar
                                val downloadId = startApkDownload(context, latestVersion)
                                if (downloadId == null) {
                                    viewModel.onAppUpdateDownloadFailed("Не удалось начать загрузку APK")
                                } else {
                                    activeDownloadId = downloadId
                                    viewModel.onAppUpdateDownloadStarted()
                                    infoMessage = "Загрузка обновления началась"
                                }
                            },
                            isUpdateEnabled = appUpdateState.isUpdateAvailable && !appUpdateState.isDownloading,
                            isUpdateChecking = appUpdateState.isChecking,
                            isUpdateDownloading = appUpdateState.isDownloading,
                            latestVersion = appUpdateState.latestVersion
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SearchBar(
                            query = searchQuery,
                            onQueryChanged = { viewModel.onSearchQueryChanged(it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (filteredChats.isEmpty()) {
                            EmptyChatsView()
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredChats, key = { it.id }) { chat ->
                                    ChatListItem(chat = chat) {
                                        navController.navigate(Screen.Chat.createRoute(chat.id, chat.name, chat.nickname, state.currentUser.id, chat.avatarUrl))
                                    }
                                }
                            }
                        }
                    }
                    if (appUpdateState.isDownloading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddContactDialog(
            isSearching = isSearchingUser,
            error = searchUserError,
            onDismiss = {
                showDialog = false
                viewModel.clearSearchError()
            },
            onConfirm = { nickname ->
                viewModel.findUser(nickname)
            }
        )
    }
}

@Composable
fun TopBar(
    user: User,
    onAddContact: () -> Unit,
    onLogout: () -> Unit,
    onUpdateAvatar: () -> Unit,
    onUpdateApplication: () -> Unit,
    isUpdateEnabled: Boolean,
    isUpdateChecking: Boolean,
    isUpdateDownloading: Boolean,
    latestVersion: String?
) {
    var showMenu by remember { mutableStateOf(false) }
    val baseUrl = BuildConfig.API_BASE_URL
    val updateMenuLabel = when {
        isUpdateDownloading -> "Скачивание обновления..."
        isUpdateChecking -> "Проверка обновления..."
        isUpdateEnabled && latestVersion != null -> "Обновить до $latestVersion"
        else -> "Приложение актуально"
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showMenu = true }
                    )
                ) {
                    UserAvatar(
                        avatarUrl = user.avatarUrl,
                        name = user.name,
                        baseUrl = baseUrl,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Telegramka",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${user.nickname}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    DropdownMenuItem(
                        text = { Text("Обновить аватарку", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        onClick = {
                            onUpdateAvatar()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(updateMenuLabel, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        onClick = {
                            onUpdateApplication()
                            showMenu = false
                        },
                        enabled = isUpdateEnabled,
                        colors = MenuDefaults.itemColors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 8.dp), 1.dp, MaterialTheme.colorScheme.outline)
                    DropdownMenuItem(
                        text = { Text("Выход", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        onClick = onLogout,
                        colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                    )
                }
            }


            Spacer(modifier = Modifier.weight(1f))

            Row {
                IconButton(
                    onClick = onAddContact,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Contact")
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChanged: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Поиск чатов...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
    )
}


@Composable
fun EmptyChatsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = "No Chats",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Нет чатов",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Добавьте контакт, чтобы начать общение",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChatListItem(chat: Chat, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseUrl = BuildConfig.API_BASE_URL
    Column {
        Row(
            modifier = Modifier
                .background(if (pressed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarUrl = chat.avatarUrl,
                name = chat.name,
                baseUrl = baseUrl,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    chat.lastMessageTime?.let {
                        Text(
                            text = formatTime(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage ?: chat.nickname,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    chat.unread?.let {
                        if (it > 0) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = it.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val ApkMimeType = "application/vnd.android.package-archive"

private fun startApkDownload(context: Context, version: String): Long? {
    val downloadManager = context.getSystemService(DownloadManager::class.java) ?: return null
    val fileName = "telegramka-$version.apk"
    val request = DownloadManager.Request(
        Uri.parse("${BuildConfig.API_BASE_URL}/release/download/$version.apk")
    )
        .setMimeType(ApkMimeType)
        .setTitle("Telegramka $version")
        .setDescription("Загрузка обновления приложения")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

    return runCatching { downloadManager.enqueue(request) }.getOrNull()
}

private fun handleApkDownloadCompleted(
    context: Context,
    downloadId: Long,
    onSuccess: () -> Unit,
    onInstallerOpened: () -> Unit,
    onInstallPermissionRequired: () -> Unit,
    onFailure: (String) -> Unit
) {
    val downloadManager = context.getSystemService(DownloadManager::class.java)
    if (downloadManager == null) {
        onFailure("DownloadManager недоступен")
        return
    }

    val query = DownloadManager.Query().setFilterById(downloadId)
    val cursor = downloadManager.query(query)
    cursor.use {
        if (!it.moveToFirst()) {
            onFailure("Не удалось получить статус загрузки")
            return
        }

        val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
            if (apkUri == null) {
                onFailure("APK скачан, но не удалось открыть установщик")
                return
            }

            onSuccess()
            installDownloadedApk(
                context = context,
                apkUri = apkUri,
                onInstallerOpened = onInstallerOpened,
                onInstallPermissionRequired = onInstallPermissionRequired,
                onFailure = onFailure
            )
            return
        }

        onFailure("Не удалось скачать обновление")
    }
}

private fun installDownloadedApk(
    context: Context,
    apkUri: Uri,
    onInstallerOpened: () -> Unit,
    onInstallPermissionRequired: () -> Unit,
    onFailure: (String) -> Unit
) {
    if (!context.packageManager.canRequestPackageInstalls()) {
        openUnknownSourcesSettings(context)
        onInstallPermissionRequired()
        return
    }

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, ApkMimeType)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(installIntent)
    }.onSuccess {
        onInstallerOpened()
    }.onFailure {
        onFailure("Не удалось открыть установщик APK")
    }
}

private fun openUnknownSourcesSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
fun AddContactDialog(
    isSearching: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var nickname by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Добавить контакт", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Введите @nickname") },
                        label = { Text("Никнейм") },
                        isError = error != null,
                        supportingText = {
                            if (error != null) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        )
                    )
                    GradientButton(
                        onClick = { onConfirm(nickname) },
                        enabled = nickname.isNotBlank() && !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Добавить")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val today = Calendar.getInstance()
    val cal = Calendar.getInstance()
    cal.time = date

    return if (today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    ) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

@Preview(showBackground = true)
@Composable
fun ChatsScreenPreview() {
    TelegramkaTheme {
        ChatsScreen(navController = rememberNavController())
    }
}
