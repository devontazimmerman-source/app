package com.vitron.remote.net

import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLSocket

class PairingException(message: String) : Exception(message)

/**
 * Android TV pairing (port 6467). start() makes the TV show a 6-character code,
 * finish(code) proves we saw it.
 */
class PairingSession(private val host: String) {
    private var socket: SSLSocket? = null
    private lateinit var input: InputStream
    private lateinit var output: OutputStream

    fun start() {
        val s = TvSocket.open(host, TvSocket.PAIRING_PORT)
        socket = s
        input = s.inputStream
        output = s.outputStream

        send(outer { message(10) { string(1, "androidtvremote2"); string(2, "Vitron Remote") } })
        expect(11)
        send(outer {
            message(20) {
                message(1) { int(1, 3); int(2, 6) } // hexadecimal, 6 symbols
                int(3, 1)                           // we are the INPUT side
            }
        })
        expect(20)
        send(outer {
            message(30) {
                message(1) { int(1, 3); int(2, 6) }
                int(2, 1)
            }
        })
        expect(31)
    }

    fun finish(code: String) {
        val s = socket ?: throw PairingException("Pairing was not started")
        if (!Regex("[0-9A-Fa-f]{6}").matches(code)) {
            throw PairingException("The code is 6 characters (0-9 and A-F).")
        }
        val clientKey = ClientIdentity.certificate().publicKey as RSAPublicKey
        val serverCert = s.session.peerCertificates[0] as X509Certificate
        val serverKey = serverCert.publicKey as RSAPublicKey

        val md = MessageDigest.getInstance("SHA-256")
        md.update(clientKey.modulus.toHexBytes())
        md.update(clientKey.publicExponent.toHexBytes())
        md.update(serverKey.modulus.toHexBytes())
        md.update(serverKey.publicExponent.toHexBytes())
        val tail = code.substring(2)
        md.update(ByteArray(2) { tail.substring(2 * it, 2 * it + 2).toInt(16).toByte() })
        val hash = md.digest()

        if (hash[0] != code.substring(0, 2).toInt(16).toByte()) {
            throw PairingException("That code doesn't match. Check the TV screen and try again.")
        }
        s.soTimeout = 10000
        send(outer { message(40) { bytes(1, hash) } })
        expect(41)
    }

    fun close() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    private fun outer(body: ProtoWriter.() -> Unit): ByteArray =
        ProtoWriter().int(1, 2).int(2, 200).apply(body).toByteArray()

    private fun send(payload: ByteArray) = Frames.write(output, payload)

    private fun expect(field: Int): ProtoMessage {
        val m = ProtoMessage.parse(Frames.read(input))
        val status = m.long(2) ?: 0L
        if (status != 200L) throw PairingException("The TV refused pairing (status $status).")
        if (!m.has(field)) throw PairingException("Unexpected reply from the TV.")
        return m
    }

    private fun BigInteger.toHexBytes(): ByteArray {
        var h = toString(16)
        if (h.length % 2 == 1) h = "0$h"
        return ByteArray(h.length / 2) { h.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
    }
}
