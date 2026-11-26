package com.vitron.remote.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper

data class Tv(val name: String, val host: String)

/** Finds Android TVs on the Wi-Fi network via mDNS (_androidtvremote2._tcp). */
class TvDiscovery(context: Context, private val onFound: (Tv) -> Unit) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val handler = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var lock: WifiManager.MulticastLock? = null
    private var running = false

    private val listener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            handler.post { queue.add(serviceInfo); pump() }
        }
    }

    fun start() {
        if (running) return
        running = true
        lock = wifi.createMulticastLock("vitron-mdns").apply {
            setReferenceCounted(false)
            acquire()
        }
        try {
            nsd.discoverServices("_androidtvremote2._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!running) return
        running = false
        try { nsd.stopServiceDiscovery(listener) } catch (_: Exception) {}
        try { lock?.release() } catch (_: Exception) {}
        lock = null
    }

    @Suppress("DEPRECATION")
    private fun pump() {
        if (resolving) return
        val info = queue.removeFirstOrNull() ?: return
        resolving = true
        try {
            nsd.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    handler.post { resolving = false; pump() }
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress
                    // Skip IPv6 results; the manual IP box covers odd networks.
                    if (host != null && !host.contains(':')) {
                        onFound(Tv(serviceInfo.serviceName, host))
                    }
                    handler.post { resolving = false; pump() }
                }
            })
        } catch (_: Exception) {
            resolving = false
        }
    }
}
