package app.tuuure.earbudswitch

import android.util.Log
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class CryptoConvert {
    companion object {

        private const val KEY_MAC_SHA1 = "HmacSHA1"
        private const val KEY_MD5 = "MD5"

        private const val PERIOD = 5000

        @JvmStatic
        fun md5code32(content: String): ByteArray {
            return MessageDigest.getInstance(KEY_MD5)
                .digest(content.toByteArray(StandardCharsets.UTF_8))
        }

        @JvmStatic
        fun tOTPGenerater(key: String): Int {
            val sks = SecretKeySpec(key.toByteArray(), KEY_MAC_SHA1)
            val mac = Mac.getInstance(KEY_MAC_SHA1)
            mac.init(sks)
            val time = System.currentTimeMillis() / PERIOD
            Log.d("TIME", time.toString())
            val value = ByteBuffer.allocate(8).putLong(time).array()

            return otpSlicer(mac.doFinal(value))
        }

        @JvmStatic
        suspend fun tOTPChecker(key: String): Collection<Int> {
            val sks = SecretKeySpec(key.toByteArray(), KEY_MAC_SHA1)
            val mac = Mac.getInstance(KEY_MAC_SHA1)
            mac.init(sks)
            val time = System.currentTimeMillis() / PERIOD
            Log.d("TIME", time.toString())
            val value1 = ByteBuffer.allocate(8).putLong(time).array()
            val data1 = otpSlicer(mac.doFinal(value1))

            val value2 = ByteBuffer.allocate(8).putLong(time - 1).array()
            val data2 = otpSlicer(mac.doFinal(value2))

            return listOf(data1, data2)
        }

        @JvmStatic
        private fun otpSlicer(hash: ByteArray): Int {
            val offset: Int = hash.get(hash.size - 1).toInt() and 0xF
            var binary: Int = (hash.get(offset).toInt() and 0x7F) shl 0x18
            binary = binary or (hash.get(offset + 1).toInt() and 0xFF shl 0x10)
            binary = binary or (hash.get(offset + 2).toInt() and 0xFF shl 0x08)
            binary = binary or (hash.get(offset + 3).toInt() and 0xFF)
            return binary
        }

        @JvmStatic
        fun hmacMD5(content: ByteArray, key: String): ByteArray {
            val sks = SecretKeySpec(key.toByteArray(), "HmacMD5")
            val mac = Mac.getInstance("HmacMD5")
            mac.init(sks)
            return mac.doFinal(content)
        }

        @JvmStatic
        fun uuidToBytes(uuid: UUID): ByteArray {
            return ByteBuffer.wrap(ByteArray(16))
                .putLong(uuid.mostSignificantBits)
                .putLong(uuid.leastSignificantBits)
                .array()
        }

        @JvmStatic
        fun bytesToUUID(inputByteArray: ByteArray): UUID {
            val bb = ByteBuffer.wrap(inputByteArray)
            val firstLong = bb.long
            val secondLong = bb.long
            return UUID(firstLong, secondLong)
        }
    }
}