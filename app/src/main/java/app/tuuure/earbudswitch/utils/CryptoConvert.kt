package app.tuuure.earbudswitch.utils

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class CryptoConvert {
    companion object {

        private const val KEY_MAC_SHA1 = "HmacSHA1"
        private const val KEY_MD5 = "MD5"

        @JvmStatic
        fun md5code32(content: String): ByteArray {
            return MessageDigest.getInstance(KEY_MD5)
                .digest(content.toByteArray(StandardCharsets.UTF_8))
        }

        @JvmStatic
        fun randomBytes(length: Int): ByteArray {
            val ng = SecureRandom()
            val bytes = ByteArray(length)
            ng.nextBytes(bytes)
            return bytes
        }

        @JvmStatic
        fun otpGenerater(key: String, salt: ByteArray): ByteArray {
            val sks = SecretKeySpec(key.toByteArray(), KEY_MAC_SHA1)
            val mac = Mac.getInstance(KEY_MAC_SHA1)
            mac.init(sks)
            val hash = mac.doFinal(salt)
            val offset: Int = hash.get(hash.size - 1).toInt() and 0xF
            return hash.copyOfRange(offset, offset + 3)
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