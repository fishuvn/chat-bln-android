package nhom3.chatbln.android.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nhom3.chatbln.android.data.local.SessionPreferences
import nhom3.chatbln.android.data.model.MessageDto
import nhom3.chatbln.android.data.model.PresenceEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Native RFC 6455 WebSocket + STOMP 1.2 client connecting to `/ws-native`.
 */
class StompRealtimeClient(
    private val okHttpClient: OkHttpClient,
    private val sessionPreferences: SessionPreferences,
    private val gson: Gson = Gson()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<MessageDto>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MessageDto> = _incomingMessages.asSharedFlow()

    private val _presenceEvents = MutableSharedFlow<PresenceEvent>(extraBufferCapacity = 64)
    val presenceEvents: SharedFlow<PresenceEvent> = _presenceEvents.asSharedFlow()

    private val desiredTopics = ConcurrentHashMap<String, String>()
    private val activeTopics = ConcurrentHashMap.newKeySet<String>()
    private val subCounter = AtomicInteger(1)
    private val isConnecting = AtomicBoolean(false)

    @Volatile
    private var shouldReconnect: Boolean = false

    @Volatile
    private var activeUsername: String? = null

    @Volatile
    private var webSocket: WebSocket? = null

    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val receiveBuffer = StringBuilder()

    fun connect(username: String) {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty()) return

        activeUsername = cleanUser
        shouldReconnect = true

        ensureTopicRegistered(TOPIC_PRESENCE)
        ensureTopicRegistered("/topic/users/$cleanUser")

        if (_isConnected.value || !isConnecting.compareAndSet(false, true)) {
            return
        }

        reconnectJob?.cancel()
        openSocket()
    }

    fun reconnectWithLatestEndpoint() {
        val user = activeUsername ?: return
        closeCurrentSocket(sendDisconnectFrame = true)
        isConnecting.set(false)
        _isConnected.value = false
        connect(user)
    }

    fun subscribeConversation(conversationId: Long) {
        if (conversationId <= 0L) return
        val topic = "/topic/conversations/$conversationId"
        val subId = ensureTopicRegistered(topic)
        if (_isConnected.value && activeTopics.add(topic)) {
            sendSubscribeFrame(subId = subId, destination = topic)
        }
    }

    fun syncConversationSubscriptions(conversationIds: Collection<Long>) {
        val targetTopics = conversationIds
            .filter { it > 0L }
            .map { "/topic/conversations/$it" }
            .toSet()

        // Remove stale conversation topics
        val existingConvTopics = desiredTopics.keys.filter { it.startsWith("/topic/conversations/") }
        for (oldTopic in existingConvTopics) {
            if (oldTopic !in targetTopics) {
                val subId = desiredTopics.remove(oldTopic)
                activeTopics.remove(oldTopic)
                if (_isConnected.value && subId != null) {
                    sendUnsubscribeFrame(subId)
                }
            }
        }

        // Subscribe to newly added conversation topics
        for (topic in targetTopics) {
            val subId = ensureTopicRegistered(topic)
            if (_isConnected.value && activeTopics.add(topic)) {
                sendSubscribeFrame(subId = subId, destination = topic)
            }
        }
    }

    fun disconnect() {
        shouldReconnect = false
        activeUsername = null
        reconnectJob?.cancel()
        reconnectJob = null
        desiredTopics.clear()
        activeTopics.clear()
        closeCurrentSocket(sendDisconnectFrame = true)
        isConnecting.set(false)
        _isConnected.value = false
    }

    private fun ensureTopicRegistered(topic: String): String {
        return desiredTopics.computeIfAbsent(topic) { "sub-${subCounter.getAndIncrement()}" }
    }

    private fun openSocket() {
        val token = sessionPreferences.currentToken
        if (token.isNullOrBlank()) {
            isConnecting.set(false)
            return
        }

        val wsUrl = sessionPreferences.buildWebSocketUrl()
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $token")
            .build()

        synchronized(receiveBuffer) {
            receiveBuffer.setLength(0)
        }
        activeTopics.clear()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val latestToken = sessionPreferences.currentToken ?: token
                val connectFrame = encodeFrame(
                    command = "CONNECT",
                    headers = linkedMapOf(
                        "accept-version" to "1.1,1.2",
                        "heart-beat" to "10000,10000",
                        "Authorization" to "Bearer $latestToken"
                    )
                )
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingChunk(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleSocketDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleSocketDisconnected()
            }
        })
    }

    private fun handleIncomingChunk(chunk: String) {
        val completeFrames = mutableListOf<String>()
        synchronized(receiveBuffer) {
            receiveBuffer.append(chunk)
            while (true) {
                val nullIdx = receiveBuffer.indexOf('\u0000')
                if (nullIdx < 0) {
                    // Trim pure heartbeat newlines when no frame is buffered
                    if (receiveBuffer.all { it == '\n' || it == '\r' }) {
                        receiveBuffer.setLength(0)
                    }
                    break
                }
                val rawFrame = receiveBuffer.substring(0, nullIdx)
                receiveBuffer.delete(0, nullIdx + 1)
                val trimmedLeading = rawFrame.trimStart('\r', '\n')
                if (trimmedLeading.isNotEmpty()) {
                    completeFrames.add(trimmedLeading)
                }
            }
        }

        for (rawFrame in completeFrames) {
            parseFrame(rawFrame)?.let(::dispatchFrame)
        }
    }

    private fun dispatchFrame(frame: StompFrame) {
        when (frame.command) {
            "CONNECTED" -> {
                isConnecting.set(false)
                _isConnected.value = true
                startHeartbeatLoop()
                resubscribeAllDesiredTopics()
            }

            "MESSAGE" -> {
                val destination = frame.headers["destination"].orEmpty()
                val payload = frame.body.trim()
                if (payload.isEmpty()) return

                when {
                    destination == TOPIC_PRESENCE -> {
                        runCatching { gson.fromJson(payload, PresenceEvent::class.java) }
                            .getOrNull()
                            ?.let { _presenceEvents.tryEmit(it) }
                    }

                    destination.startsWith("/topic/users/") ||
                        destination.startsWith("/topic/conversations/") -> {
                        runCatching { gson.fromJson(payload, MessageDto::class.java) }
                            .getOrNull()
                            ?.let { _incomingMessages.tryEmit(it) }
                    }
                }
            }

            "ERROR" -> {
                handleSocketDisconnected()
            }
        }
    }

    private fun resubscribeAllDesiredTopics() {
        activeTopics.clear()
        for ((topic, subId) in desiredTopics.entries) {
            if (activeTopics.add(topic)) {
                sendSubscribeFrame(subId = subId, destination = topic)
            }
        }
    }

    private fun sendSubscribeFrame(subId: String, destination: String) {
        val frame = encodeFrame(
            command = "SUBSCRIBE",
            headers = linkedMapOf(
                "id" to subId,
                "destination" to destination,
                "ack" to "auto"
            )
        )
        webSocket?.send(frame)
    }

    private fun sendUnsubscribeFrame(subId: String) {
        val frame = encodeFrame(
            command = "UNSUBSCRIBE",
            headers = linkedMapOf("id" to subId)
        )
        webSocket?.send(frame)
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _isConnected.value) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (_isConnected.value) {
                    webSocket?.send("\n")
                }
            }
        }
    }

    private fun closeCurrentSocket(sendDisconnectFrame: Boolean) {
        heartbeatJob?.cancel()
        heartbeatJob = null
        val socket = webSocket
        webSocket = null
        if (socket != null) {
            if (sendDisconnectFrame && _isConnected.value) {
                runCatching {
                    socket.send(encodeFrame(command = "DISCONNECT", headers = emptyMap()))
                }
            }
            runCatching { socket.close(1000, "Client closing") }
        }
    }

    private fun handleSocketDisconnected() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        activeTopics.clear()
        isConnecting.set(false)
        _isConnected.value = false
        webSocket = null

        if (shouldReconnect && !sessionPreferences.currentToken.isNullOrBlank()) {
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            val user = activeUsername
            if (shouldReconnect && !_isConnected.value && !user.isNullOrBlank()) {
                connect(user)
            }
        }
    }

    private data class StompFrame(
        val command: String,
        val headers: Map<String, String>,
        val body: String
    )

    private fun encodeFrame(
        command: String,
        headers: Map<String, String>,
        body: String = ""
    ): String {
        val sb = StringBuilder()
        sb.append(command).append('\n')
        val escapeHeaders = command != "CONNECT" && command != "CONNECTED"
        for ((key, value) in headers) {
            val encodedKey = if (escapeHeaders) escapeHeaderValue(key) else key
            val encodedVal = if (escapeHeaders) escapeHeaderValue(value) else value
            sb.append(encodedKey).append(':').append(encodedVal).append('\n')
        }
        sb.append('\n')
        sb.append(body)
        sb.append('\u0000')
        return sb.toString()
    }

    private fun parseFrame(raw: String): StompFrame? {
        val normalized = raw.replace("\r\n", "\n")
        val firstNewline = normalized.indexOf('\n')
        if (firstNewline <= 0) {
            return StompFrame(command = normalized.trim(), headers = emptyMap(), body = "")
        }

        val command = normalized.substring(0, firstNewline).trim()
        val headerBodySplit = normalized.indexOf("\n\n", startIndex = firstNewline)

        val headerBlock: String
        val body: String
        if (headerBodySplit >= 0) {
            headerBlock = normalized.substring(firstNewline + 1, headerBodySplit)
            body = normalized.substring(headerBodySplit + 2)
        } else {
            headerBlock = normalized.substring(firstNewline + 1)
            body = ""
        }

        val unescape = command != "CONNECT" && command != "CONNECTED"
        val headers = LinkedHashMap<String, String>()
        for (line in headerBlock.split('\n')) {
            if (line.isEmpty()) continue
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val rawKey = line.substring(0, colonIdx)
                val rawValue = line.substring(colonIdx + 1)
                val key = if (unescape) unescapeHeaderValue(rawKey) else rawKey
                val value = if (unescape) unescapeHeaderValue(rawValue) else rawValue
                // Per STOMP 1.2 spec, only the first occurrence of duplicate header keys wins
                headers.putIfAbsent(key, value)
            }
        }

        return StompFrame(command = command, headers = headers, body = body)
    }

    private fun escapeHeaderValue(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                ':' -> append("\\c")
                else -> append(ch)
            }
        }
    }

    private fun unescapeHeaderValue(value: String): String = buildString(value.length) {
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (val next = value[i + 1]) {
                    'n' -> append('\n')
                    'r' -> append('\r')
                    'c' -> append(':')
                    '\\' -> append('\\')
                    else -> {
                        append(ch)
                        append(next)
                    }
                }
                i += 2
            } else {
                append(ch)
                i += 1
            }
        }
    }

    companion object {
        const val TOPIC_PRESENCE = "/topic/presence"
        private const val HEARTBEAT_INTERVAL_MS = 10_000L
        private const val RECONNECT_DELAY_MS = 3_000L
    }
}
