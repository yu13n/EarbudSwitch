package app.tuuure.earbudswitch.utils

import android.graphics.Bitmap
import com.king.zxing.util.CodeUtils

class KeyQRCode {
    companion object {
        private const val prefix = "ebs://"

        @JvmStatic
        suspend fun createQRCode(key: String, invert: Boolean, heightPix: Int): Bitmap {
            val bitmap = CodeUtils.createQRCode(prefix + key, heightPix)
            return processBitmap(bitmap, invert)
        }

        @JvmStatic
        fun deprefix(content: String): String =
            if (content.startsWith(prefix))
                content.replaceFirst(prefix.toRegex(), "")
            else
                ""

        @JvmStatic
        private fun processBitmap(bitmap: Bitmap, invert: Boolean): Bitmap {
            val sWidth = bitmap.width
            val sHeight = bitmap.height
            val sPixels = IntArray(sWidth * sHeight)
            bitmap.getPixels(sPixels, 0, sWidth, 0, 0, sWidth, sHeight)
            var sIndex: Int
            for (sRow in 0 until sHeight) {
                sIndex = sRow * sWidth
                for (sCol in 0 until sWidth) {
                    var sPixel = sPixels[sIndex]
                    var sA = sPixel shr 24 and 0xff
                    var sRGB = sPixel and 0xffffff
                    if (sRGB == 0xffffff) {
                        sA = 0
                    }
                    if (invert) {
                        var sR = sRGB shr 16 and 0xff
                        var sG = sRGB shr 8 and 0xff
                        var sB = sRGB and 0xff
                        sR = 255 - sR
                        sG = 255 - sG
                        sB = 255 - sB
                        sRGB = sR and 0xff shl 16 or (sG and 0xff shl 8) or (sB and 0xff)
                    }
                    sPixel = sA and 0xff shl 24 or (sRGB and 0xffffff)
                    sPixels[sIndex] = sPixel
                    sIndex++
                }
            }
            bitmap.setPixels(sPixels, 0, sWidth, 0, 0, sWidth, sHeight)
            return bitmap
        }
    }
}