package com.vitron.remote

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitron.remote.net.PairingException
import com.vitron.remote.net.PairingSession
import com.vitron.remote.net.RemoteSession
import com.vitron.remote.net.Tv
import com.vitron.remote.net.TvDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class PairPhase { Idle, Connecting, WaitingForCode, Verifying }
enum class Conn { Disconnected, Connecting, Ready }

data class UiState(
    val pairedHost: String? = null,
    val phase: PairPhase = PairPhase.Idle,
    val pairingHost: String = "",
    val message: String? = null,
    val connection: Conn = Conn.Disconnected,
    val found: List<Tv> = emptyList()
)

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("vitron", Context.MODE_PRIVATE)
    private val _ui = MutableStateFlow(UiState(pairedHost = prefs.getString("host", null)))
    val ui: StateFlow<UiState> = _ui

    private var discovery: TvDiscovery? = null
    private var pairing: PairingSession? = null
    private var session: RemoteSession? = null
    private val mutex = Mutex()

    // ---------- discovery ----------
    fun startDiscovery() {
        if (discovery != null) return
        discovery = TvDiscovery(getApplication()) { tv ->
            _ui.update { s ->
                if (s.found.any { it.host == tv.host }) s else s.copy(found = s.found + tv)
            }
        }.also { it.start() }
    }

    fun stopDiscovery() {
        discovery?.stop()
        discovery = null
    }

    // ---------- pairing ----------
    fun startPairing(host: String) {
        val h = host.trim()
        if (h.isEmpty()) return
        _ui.update { it.copy(phase = PairPhase.Connecting, pairingHost = h, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            pairing?.close()
            try {
                val p = PairingSession(h)
                p.start()
                pairing = p
                _ui.update { it.copy(phase = PairPhase.WaitingForCode) }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        phase = PairPhase.Idle,
                        message = "Couldn't start pairing: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun submitCode(code: String) {
        val p = pairing ?: return
        val host = _ui.value.pairingHost
        _ui.update { it.copy(phase = PairPhase.Verifying, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p.finish(code.trim().uppercase())
                p.close()
                pairing = null
                prefs.edit().putString("host", host).apply()
                stopDiscovery()
                _ui.update { it.copy(pairedHost = host, phase = PairPhase.Idle, message = null) }
            } catch (e: PairingException) {
                _ui.update { it.copy(phase = PairPhase.WaitingForCode, message = e.message) }
            } catch (e: Exception) {
                p.close()
                pairing = null
                _ui.update {
                    it.copy(
                        phase = PairPhase.Idle,
                        message = "Pairing failed: ${e.message ?: e.javaClass.simpleName}. Start again."
                    )
                }
            }
        }
    }

    fun cancelPairing() {
        pairing?.close()
        pairing = null
        _ui.update { it.copy(phase = PairPhase.Idle, message = null) }
    }

    fun forgetTv() {
        session?.close()
        session = null
        prefs.edit().remove("host").apply()
        _ui.update {
            it.copy(pairedHost = null, phase = PairPhase.Idle, connection = Conn.Disconnected, message = null, found = emptyList())
        }
    }

    // ---------- remote control ----------
    fun connect() {
        viewModelScope.launch(Dispatchers.IO) { ensureSession() }
    }

    fun press(key: Int) {
        viewModelScope.launch(Dispatchers.IO) { withRetry { it.sendKey(key) } }
    }

    fun launch(link: String) {
        viewModelScope.launch(Dispatchers.IO) { withRetry { it.launchApp(link) } }
    }

    private suspend fun withRetry(block: (RemoteSession) -> Unit) {
        val first = ensureSession() ?: return
        try {
            block(first)
        } catch (e: Exception) {
            first.close()
            session = null
            _ui.update { it.copy(connection = Conn.Disconnected) }
            val second = ensureSession() ?: return
            try { block(second) } catch (_: Exception) {}
        }
    }

    private suspend fun ensureSession(): RemoteSession? = mutex.withLock {
        val host = _ui.value.pairedHost ?: return null
        session?.let { if (it.isReady) return it }
        session?.close()
        session = null
        _ui.update { it.copy(connection = Conn.Connecting, message = null) }
        val s = RemoteSession(host) {
            _ui.update { st -> st.copy(connection = Conn.Disconnected) }
        }
        try {
            s.connect()
            session = s
            _ui.update { it.copy(connection = Conn.Ready) }
            s
        } catch (e: Exception) {
            s.close()
            _ui.update {
                it.copy(
                    connection = Conn.Disconnected,
                    message = "Can't reach the TV. Is it on and on the same Wi-Fi?"
                )
            }
            null
        }
    }

    override fun onCleared() {
        stopDiscovery()
        pairing?.close()
        session?.close()
    }
}
