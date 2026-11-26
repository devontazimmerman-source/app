package com.vitron.remote.net

import android.annotation.SuppressLint
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/**
 * The phone's identity. The TV remembers this certificate after pairing, so it is created once
 * and kept in the Android Keystore.
 */
object ClientIdentity {
    private const val ALIAS = "vitron_remote_client"

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized
    fun ensure() {
        if (keyStore().containsAlias(ALIAS)) return
        val now = System.currentTimeMillis()
        val spec = KeyGenParameterSpec.Builder(
            ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(2048)
            .setDigests(
                KeyProperties.DIGEST_SHA1,
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA384,
                KeyProperties.DIGEST_SHA512
            )
            .setSignaturePaddings(
                KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                KeyProperties.SIGNATURE_PADDING_RSA_PSS
            )
            .setCertificateSubject(X500Principal("CN=atvremote"))
            .setCertificateSerialNumber(BigInteger.ONE)
            .setCertificateNotBefore(Date(now - 24L * 3600 * 1000))
            .setCertificateNotAfter(Date(now + 20L * 365 * 24 * 3600 * 1000))
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
            initialize(spec)
            generateKeyPair()
        }
    }

    fun certificate(): X509Certificate {
        ensure()
        return keyStore().getCertificate(ALIAS) as X509Certificate
    }

    @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
    fun sslContext(): SSLContext {
        ensure()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore(), null)
        // TVs use self-signed certificates, so we accept them. Pairing is what establishes trust.
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLSv1.2").apply {
            init(kmf.keyManagers, arrayOf(trustAll), null)
        }
    }
}

object TvSocket {
    const val PAIRING_PORT = 6467
    const val REMOTE_PORT = 6466

    fun open(host: String, port: Int): SSLSocket {
        val ctx = ClientIdentity.sslContext()
        val raw = Socket()
        raw.connect(InetSocketAddress(host, port), 5000)
        val ssl = ctx.socketFactory.createSocket(raw, host, port, true) as SSLSocket
        ssl.soTimeout = 10000
        ssl.keepAlive = true
        ssl.startHandshake()
        ssl.soTimeout = 0
        return ssl
    }
}
