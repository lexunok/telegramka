package ru.jarvis.telegramka.ui.chat

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.domain.model.Message
import ru.jarvis.telegramka.ui.theme.TelegramkaTheme
import ru.jarvis.telegramka.ui.utils.UserAvatar
import java.text.SimpleDateFormat
import java.util.*

private const val LoadingMoreItemKey = "loading-more"

@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String?,
    userId: String?,
    name: String,
    nickname: String,
    currentUserId: String,
    avatarUrl: String?,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val successState = uiState as? ChatUiState.Success
    val conversationKey = chatId ?: "user:${userId.orEmpty()}"
    val chatItems = remember(successState?.messages) {
        successState?.messages?.toChatListItems().orEmpty()
    }
    var messageText by remember { mutableStateOf("") }
    val listState = if (successState != null) {
        remember(conversationKey) {
            LazyListState(
                firstVisibleItemIndex = chatItems.lastIndex.coerceAtLeast(0)
            )
        }
    } else {
        rememberLazyListState()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var previousMessageCount by remember(conversationKey) { mutableIntStateOf(0) }
    var wasAtBottom by remember(conversationKey) { mutableStateOf(true) }
    var lastMessageId by remember(conversationKey) { mutableStateOf<String?>(null) }
    var paginationAnchor by remember(conversationKey) { mutableStateOf<PaginationAnchor?>(null) }

    LaunchedEffect(chatId, userId, currentUserId) {
        viewModel.initialize(chatId, userId, currentUserId)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isAtBottom() }
            .collect { atBottom -> wasAtBottom = atBottom }
    }

    LaunchedEffect(listState, successState?.hasMore, successState?.isLoadingMore, chatItems.size) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (firstVisibleItemIndex, firstVisibleItemScrollOffset) ->
                val state = uiState as? ChatUiState.Success ?: return@collect
                if (
                    firstVisibleItemIndex <= 3 &&
                    state.hasMore &&
                    !state.isLoadingMore &&
                    state.messages.isNotEmpty()
                ) {
                    val anchorItem = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull()
                        ?: return@collect
                    paginationAnchor = PaginationAnchor(
                        itemKey = anchorItem.key,
                        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                        oldestMessageId = state.messages.firstOrNull()?.id
                    )
                    viewModel.loadOlderMessages()
                }
            }
    }

    SideEffect {
        val anchor = paginationAnchor ?: return@SideEffect
        val oldestMessageId = successState?.messages?.firstOrNull()?.id
        if (oldestMessageId != null && oldestMessageId != anchor.oldestMessageId) {
            val targetIndex = buildChatItemKeys(chatItems).indexOf(anchor.itemKey)
            if (targetIndex >= 0) {
                listState.requestScrollToItem(
                    index = targetIndex,
                    scrollOffset = anchor.firstVisibleItemScrollOffset
                )
            }
            paginationAnchor = null
        } else if (successState?.isLoadingMore == false) {
            paginationAnchor = null
        }
    }

    LaunchedEffect(successState?.messages?.lastOrNull()?.id) {
        val messages = successState?.messages.orEmpty()
        val messageCount = messages.size
        val latestMessage = messages.lastOrNull()
        if (messageCount > 0) {
            val lastIndex = chatItems.lastIndex
            if (previousMessageCount == 0) {
                listState.scrollToItem(lastIndex)
            } else if (
                latestMessage != null &&
                latestMessage.id != lastMessageId &&
                (wasAtBottom || latestMessage.senderId == currentUserId)
            ) {
                listState.animateScrollToItem(lastIndex)
            }
        }
        previousMessageCount = messageCount
        lastMessageId = latestMessage?.id
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ChatUiState.Error) {
            snackbarHostState.showSnackbar(state.message)
            viewModel.consumeErrorMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ChatHeader(
                name = name,
                nickname = nickname,
                avatarUrl = avatarUrl,
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            MessageInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0A0A0F) // Match the original background color
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A0A0F),
                            Color(0xFF12121A),
                            Color(0xFF1A1630),
                            Color(0xFF0A0A0F)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 2000f)
                    )
                )
        ) {
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChatUiState.Error -> {
                    // Error is shown in snackbar, content area can be empty or show a placeholder
                }
                is ChatUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                        ) {
                            if (chatItems.isEmpty()) {
                                item {
                                    EmptyChatPlaceholder()
                                }
                            } else {
                                itemsIndexed(
                                    items = chatItems,
                                    key = { _, item ->
                                        when (item) {
                                            is ChatListItem.Date -> dateItemKey(item.timestamp)
                                            is ChatListItem.MessageItem -> item.message.id
                                        }
                                    }
                                ) { _, item ->
                                    when (item) {
                                        is ChatListItem.Date -> DateSeparator(timestamp = item.timestamp)
                                        is ChatListItem.MessageItem -> MessageItem(
                                            message = item.message,
                                            currentUserId = currentUserId
                                        )
                                    }
                                }
                            }
                        }
                        if (state.isLoadingMore) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PaginationAnchor(
    val itemKey: Any,
    val firstVisibleItemScrollOffset: Int,
    val oldestMessageId: String?
)

private fun LazyListState.isAtBottom(): Boolean {
    val totalItemsCount = layoutInfo.totalItemsCount
    if (totalItemsCount == 0) return true
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return true
    return lastVisibleItemIndex >= totalItemsCount - 2
}

private sealed interface ChatListItem {
    data class Date(val timestamp: Long) : ChatListItem
    data class MessageItem(val message: Message) : ChatListItem
}

private fun List<Message>.toChatListItems(): List<ChatListItem> {
    if (isEmpty()) return emptyList()

    val result = mutableListOf<ChatListItem>()
    forEachIndexed { index, message ->
        val prevMessage = getOrNull(index - 1)
        val showDate = prevMessage == null || !isSameDay(prevMessage.timestamp, message.timestamp)
        if (showDate) {
            result += ChatListItem.Date(message.timestamp)
        }
        result += ChatListItem.MessageItem(message)
    }
    return result
}

private fun buildChatItemKeys(items: List<ChatListItem>): List<Any> {
    return items.map { item ->
        when (item) {
            is ChatListItem.Date -> dateItemKey(item.timestamp)
            is ChatListItem.MessageItem -> item.message.id
        }
    }
}

private fun dateItemKey(timestamp: Long): String = "date-${startOfDay(timestamp)}"

private fun startOfDay(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

@Composable
fun ChatHeader(name: String, nickname: String, avatarUrl: String?, onBack: () -> Unit) {
    val baseUrl = BuildConfig.API_BASE_URL
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(text = "@$nickname", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                UserAvatar(
                    avatarUrl = avatarUrl,
                    name = name,
                    baseUrl = baseUrl,
                    size = 40.dp
                )
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, currentUserId: String) {
    val isCurrentUser = message.senderId == currentUserId
    val horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    val contentColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val metaColor = contentColor.copy(alpha = 0.7f)
    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        val messageBubbleModifier = if (isCurrentUser) {
            Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                ),
                shape = bubbleShape
            )
        } else {
            Modifier
                .background(MaterialTheme.colorScheme.surface, shape = bubbleShape)
                .border(0.5.dp, MaterialTheme.colorScheme.outline, bubbleShape)
        }

        Box(
            modifier = messageBubbleModifier
                .clip(bubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box {
                Text(
                    text = message.text,
                    color = contentColor,
                    modifier = Modifier.padding(
                        end = if (isCurrentUser) 56.dp else 40.dp
                    )
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = metaColor
                    )
                    if (isCurrentUser) {
                        val statusTint = when {
                            message.isFailed -> MaterialTheme.colorScheme.error
                            else -> metaColor
                        }
                        Icon(
                            imageVector = when {
                                message.isFailed -> Icons.Default.ErrorOutline
                                message.isPending -> Icons.Default.Schedule
                                else -> Icons.Default.Check
                            },
                            contentDescription = when {
                                message.isFailed -> "Не отправлено"
                                message.isPending -> "Отправляется"
                                else -> "Доставлено"
                            },
                            modifier = Modifier.size(14.dp),
                            tint = statusTint
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MessageInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp),
                    maxLines = 5,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .heightIn(max = 120.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .defaultMinSize(minHeight = 24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text("Напишите сообщение...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
fun EmptyChatPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mood,
                contentDescription = "No Messages",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Начните разговор",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DateSeparator(timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = SimpleDateFormat("d MMMM", Locale.forLanguageTag("ru-RU")).format(Date(timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}


private fun formatMessageTime(timestamp: Long): String {
    val date = Date(timestamp)
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance()
    cal1.timeInMillis = timestamp1
    val cal2 = Calendar.getInstance()
    cal2.timeInMillis = timestamp2
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}


@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    TelegramkaTheme {
        ChatScreen(
            navController = rememberNavController(),
            chatId = "1",
            userId = null,
            name = "Test User",
            nickname = "testuser",
            currentUserId = "myUserId",
            avatarUrl = null
        )
    }
}
