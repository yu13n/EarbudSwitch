package app.tuuure.earbudswitch;

import android.os.ParcelUuid;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;

public class BluetoothUuid {
    public static final ParcelUuid A2DP_SINK =
            ParcelUuid.fromString("0000110B-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid A2DP_SOURCE =
            ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid ADV_AUDIO_DIST =
            ParcelUuid.fromString("0000110D-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid HSP =
            ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid HSP_AG =
            ParcelUuid.fromString("00001112-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid HFP =
            ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid HFP_AG =
            ParcelUuid.fromString("0000111F-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid AVRCP_CONTROLLER =
            ParcelUuid.fromString("0000110E-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid AVRCP_TARGET =
            ParcelUuid.fromString("0000110C-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid OBEX_OBJECT_PUSH =
            ParcelUuid.fromString("00001105-0000-1000-8000-00805f9b34fb");

    public static final ParcelUuid HID =
            ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb");

    public static final ParcelUuid HOGP =
            ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb");

    public static final ParcelUuid PANU =
            ParcelUuid.fromString("00001115-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid NAP =
            ParcelUuid.fromString("00001116-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid BNEP =
            ParcelUuid.fromString("0000000f-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid PBAP_PCE =
            ParcelUuid.fromString("0000112e-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid PBAP_PSE =
            ParcelUuid.fromString("0000112f-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid MAP =
            ParcelUuid.fromString("00001134-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid MNS =
            ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid MAS =
            ParcelUuid.fromString("00001132-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid SAP =
            ParcelUuid.fromString("0000112D-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid HEARING_AID =
            ParcelUuid.fromString("0000FDF0-0000-1000-8000-00805f9b34fb");

    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

    /**
     * Length of bytes for 16 bit UUID
     *
     * @hide
     */

    public static final int UUID_BYTES_16_BIT = 2;
    /**
     * Length of bytes for 32 bit UUID
     *
     * @hide
     */

    public static final int UUID_BYTES_32_BIT = 4;
    /**
     * Length of bytes for 128 bit UUID
     *
     * @hide
     */

    public static final int UUID_BYTES_128_BIT = 16;
    /**
     * Returns true if there any common ParcelUuids in uuidA and uuidB.
     *
     * @param uuidA - List of ParcelUuids
     * @param uuidB - List of ParcelUuids
     *
     * @hide
     */
    public static boolean containsAnyUuid(@Nullable ParcelUuid[] uuidA,
                                          @Nullable ParcelUuid[] uuidB) {
        if (uuidA == null && uuidB == null) return true;
        if (uuidA == null) {
            return uuidB.length == 0;
        }
        if (uuidB == null) {
            return uuidA.length == 0;
        }
        HashSet<ParcelUuid> uuidSet = new HashSet<ParcelUuid>(Arrays.asList(uuidA));
        for (ParcelUuid uuid : uuidB) {
            if (uuidSet.contains(uuid)) return true;
        }
        return false;
    }
}
