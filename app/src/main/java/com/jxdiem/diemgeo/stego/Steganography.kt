package com.jxdiem.diemgeo.stego

import android.graphics.Bitmap
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * LSB steganography used as a tamper-evidence watermark (spec point 13):
 * the capture metadata (coordinates, time, sensor trust) is hashed and the
 * hash + payload are hidden in the low bits of the image pixels themselves,
 * not just in EXIF (which any editor can rewrite freely).
 *
 * This only survives lossless re-encoding, so the app always stores the
 * watermarked photo as PNG. If a photo is later re-compressed (e.g. resent
 * as a JPEG by a chat app) or edited, the hidden payload no longer decodes
 * or its checksum no longer matches — which is exactly the tamper signal
 * point 13 asks for.
 */
object Steganography {

    private const val MAGIC = "DGEO"

    fun embed(source: Bitmap, payload: String): Bitmap {
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        val checksum = crc32Of(payloadBytes)
        val header = MAGIC.toByteArray(Charsets.US_ASCII) // 4 bytes
        val lengthBytes = intToBytes(payloadBytes.size) // 4 bytes
        val checksumBytes = intToBytes(checksum) // 4 bytes
        val fullPayload = header + lengthBytes + checksumBytes + payloadBytes
        val bits = bytesToBits(fullPayload)

        require(bits.size <= source.width * source.height) {
            "Immagine troppo piccola per contenere la firma nascosta"
        }

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        var bitIndex = 0
        outer@ for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                if (bitIndex >= bits.size) break@outer
                val pixel = result.getPixel(x, y)
                val bit = bits[bitIndex]
                val newPixel = (pixel and 0xFFFFFFFE.toInt()) or bit
                result.setPixel(x, y, newPixel)
                bitIndex++
            }
        }
        return result
    }

    sealed class VerificationResult {
        data class Valid(val payload: String) : VerificationResult()
        object ChecksumMismatch : VerificationResult()
        object NoSignatureFound : VerificationResult()
    }

    fun extractAndVerify(bitmap: Bitmap): VerificationResult {
        val headerBits = readBits(bitmap, 0, (4 + 4 + 4) * 8)
        val headerBytes = bitsToBytes(headerBits)
        val magic = String(headerBytes.copyOfRange(0, 4), Charsets.US_ASCII)
        if (magic != MAGIC) return VerificationResult.NoSignatureFound

        val length = bytesToInt(headerBytes.copyOfRange(4, 8))
        val storedChecksum = bytesToInt(headerBytes.copyOfRange(8, 12))
        if (length <= 0 || length > bitmap.width * bitmap.height) return VerificationResult.NoSignatureFound

        val payloadBits = readBits(bitmap, 12 * 8, length * 8)
        val payloadBytes = bitsToBytes(payloadBits)
        val actualChecksum = crc32Of(payloadBytes)

        return if (actualChecksum == storedChecksum) {
            VerificationResult.Valid(String(payloadBytes, Charsets.UTF_8))
        } else {
            VerificationResult.ChecksumMismatch
        }
    }

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun readBits(bitmap: Bitmap, startBit: Int, count: Int): IntArray {
        val bits = IntArray(count)
        var i = 0
        var pixelIndex = 0
        outer@ for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (pixelIndex < startBit) {
                    pixelIndex++
                    continue
                }
                if (i >= count) break@outer
                val pixel = bitmap.getPixel(x, y)
                bits[i] = pixel and 0x01
                i++
                pixelIndex++
            }
        }
        return bits
    }

    private fun crc32Of(bytes: ByteArray): Int {
        val crc = CRC32()
        crc.update(bytes)
        return crc.value.toInt()
    }

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte()
    )

    private fun bytesToInt(bytes: ByteArray): Int =
        ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)

    private fun bytesToBits(bytes: ByteArray): IntArray {
        val bits = IntArray(bytes.size * 8)
        for (i in bytes.indices) {
            for (b in 0 until 8) {
                bits[i * 8 + b] = (bytes[i].toInt() shr (7 - b)) and 0x01
            }
        }
        return bits
    }

    private fun bitsToBytes(bits: IntArray): ByteArray {
        val bytes = ByteArray(bits.size / 8)
        for (i in bytes.indices) {
            var value = 0
            for (b in 0 until 8) {
                value = (value shl 1) or bits[i * 8 + b]
            }
            bytes[i] = value.toByte()
        }
        return bytes
    }
}
