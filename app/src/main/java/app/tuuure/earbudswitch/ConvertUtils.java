package app.tuuure.earbudswitch;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.MurmurHash3;

class ConvertUtils {
    private static final String TAG = "ConverUtils";
    private static final int SEED = 1030;

    static byte[] mur32b(String content) {
        byte[] bytes = content.getBytes();
        long i = MurmurHash3.hash64(bytes, 0, bytes.length, SEED);
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(i);
        return buffer.array();
    }

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

    static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[Long.BYTES * 2]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    static long bytesToLong(byte[] bytes) {
        if (bytes == null) {
            return 0;
        }
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();
        return buffer.getLong();
    }

    static UUID bytesToUUID(byte[] byteArray1, byte[] byteArray2) {
        return new UUID(bytesToLong(byteArray1), bytesToLong(byteArray2));
    }


    static UUID bytesToUUID(byte[] inputByteArray) {
        ByteBuffer bb = ByteBuffer.wrap(inputByteArray);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }
}
