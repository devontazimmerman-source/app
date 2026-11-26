package com.vitron.remote.net

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Tiny protobuf encoder: just enough for the Android TV remote messages. */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun int(field: Int, value: Int): ProtoWriter {
        writeVarint(out, ((field shl 3) or 0).toLong())
        writeVarint(out, value.toLong())
        return this
    }

    fun bytes(field: Int, value: ByteArray): ProtoWriter {
        writeVarint(out, ((field shl 3) or 2).toLong())
        writeVarint(out, value.size.toLong())
        out.write(value)
        return this
    }

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun message(field: Int, build: ProtoWriter.() -> Unit): ProtoWriter =
        bytes(field, ProtoWriter().apply(build).toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()
}

fun writeVarint(out: OutputStream, value: Long) {
    var x = value
    while ((x and 0x7FL.inv()) != 0L) {
        out.write(((x and 0x7F) or 0x80).toInt())
        x = x ushr 7
    }
    out.write(x.toInt())
}

/** Tiny protobuf decoder: keeps varints as Long and length-delimited fields as ByteArray. */
class ProtoMessage(private val fields: Map<Int, List<Any>>) {
    fun has(field: Int) = fields.containsKey(field)
    fun long(field: Int): Long? = fields[field]?.firstOrNull() as? Long
    fun bytes(field: Int): ByteArray? = fields[field]?.firstOrNull() as? ByteArray
    fun message(field: Int): ProtoMessage? = bytes(field)?.let { parse(it) }

    companion object {
        fun parse(data: ByteArray): ProtoMessage {
            val map = HashMap<Int, MutableList<Any>>()
            var pos = 0

            fun readVarint(): Long {
                var result = 0L
                var shift = 0
                while (true) {
                    if (pos >= data.size) throw IOException("Truncated message")
                    val b = data[pos++].toInt() and 0xFF
                    result = result or ((b and 0x7F).toLong() shl shift)
                    if ((b and 0x80) == 0) break
                    shift += 7
                    if (shift > 63) throw IOException("Bad varint")
                }
                return result
            }

            while (pos < data.size) {
                val tag = readVarint().toInt()
                val field = tag ushr 3
                when (tag and 7) {
                    0 -> map.getOrPut(field) { mutableListOf() }.add(readVarint())
                    2 -> {
                        val len = readVarint().toInt()
                        if (len < 0 || pos + len > data.size) throw IOException("Bad length")
                        map.getOrPut(field) { mutableListOf() }.add(data.copyOfRange(pos, pos + len))
                        pos += len
                    }
                    1 -> pos += 8
                    5 -> pos += 4
                    else -> throw IOException("Unsupported wire type")
                }
            }
            return ProtoMessage(map)
        }
    }
}

/** Messages on the wire are prefixed with a varint length. */
object Frames {
    fun write(out: OutputStream, payload: ByteArray) {
        val header = ByteArrayOutputStream()
        writeVarint(header, payload.size.toLong())
        out.write(header.toByteArray())
        out.write(payload)
        out.flush()
    }

    fun read(input: InputStream): ByteArray {
        var len = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b < 0) throw EOFException()
            len = len or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift > 28) throw IOException("Bad frame header")
        }
        val buf = ByteArray(len)
        var off = 0
        while (off < len) {
            val n = input.read(buf, off, len - off)
            if (n < 0) throw EOFException()
            off += n
        }
        return buf
    }
}
