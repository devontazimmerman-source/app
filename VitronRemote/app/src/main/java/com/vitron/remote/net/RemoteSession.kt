package com.vitron.remote.net

import java.io.IOException
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Control connection (port 6466): sends key presses to an already-paired TV. */
class RemoteSession(private val host: String, private val onClosed: () -> Unit) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val ready = CompletableDeferred<Unit>()
    private val writeLock = Any()
    private var socket: SSLSocket? = null

    @Volatile
    var isReady = false
        private set

    suspend fun connect() {
        val s = withContext(Dispatchers.IO) { TvSocket.open(host, TvSocket.REMOTE_PORT) }
        socket = s
        scope.launch {
            try {
                readLoop(s)
            } catch (_: Exception) {
            } finally {
                isReady = false
                ready.completeExceptionally(IOException("Connection closed"))
                onClosed()
            }
        }
        withTimeout(8000) { ready.await() }
    }

    private fun readLoop(s: SSLSocket) {
        val input = s.inputStream
        while (true) {
            val msg = ProtoMessage.parse(Frames.read(input))
            when {
                msg.has(1) -> {
                    // TV announces itself; answer with our features and device info.
                    send(ProtoWriter().message(1) {
                        int(1, 622)
                        message(2) {
                            string(1, "Vitron Remote")
                            string(2, "Vitron Remote App")
                            int(3, 1)
                            string(4, "1")
                            string(5, "com.vitron.remote")
                            string(6, "1.0.0")
                        }
                    }.toByteArray())
                }
                msg.has(2) -> {
                    send(ProtoWriter().message(2) { int(1, 622) }.toByteArray())
                    isReady = true
                    ready.complete(Unit)
                }
                msg.has(8) -> {
                    val v = msg.message(8)?.long(1)?.toInt() ?: 0
                    send(ProtoWriter().message(9) { int(1, v) }.toByteArray())
                }
                msg.has(3) -> throw IOException("TV reported an error")
            }
        }
    }

    fun sendKey(code: Int) {
        // direction 3 = SHORT press
        send(ProtoWriter().message(10) { int(1, code); int(2, 3) }.toByteArray())
    }

    fun launchApp(link: String) {
        send(ProtoWriter().message(90) { string(1, link) }.toByteArray())
    }

    private fun send(payload: ByteArray) {
        val s = socket ?: throw IOException("Not connected")
        synchronized(writeLock) { Frames.write(s.outputStream, payload) }
    }

    fun close() {
        isReady = false
        try { socket?.close() } catch (_: Exception) {}
        scope.cancel()
    }
}
