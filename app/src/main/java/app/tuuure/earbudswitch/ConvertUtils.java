package app.tuuure.earbudswitch;

import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class ConvertUtils {
    private static final String TAG = "ConverUtils";

    static byte[] hmacMD5(String content, String key) {
        byte[] digest;
        try {
            SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(sks);
            digest = mac.doFinal(content.getBytes());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException");
            digest = null;
        } catch (InvalidKeyException e) {
            Log.e(TAG, "InvalidKeyException " + key);
            digest = null;
        }
        return digest;
    }

    static String md5code32(String content) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(content.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException", e);
        }
        //对生成的16字节数组进行补零操作
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        for (int i = 8; i <= 23; i += 5) {
            hex.insert(i, "-");
        }
        return hex.toString();
    }

    static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    static UUID bytesToUUID(byte[] inputByteArray) {
        ByteBuffer bb = ByteBuffer.wrap(inputByteArray);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }
}
