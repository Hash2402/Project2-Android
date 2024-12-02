package com.example.shotanalyse

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

object BitmapUtils {
    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 640 // Image size for resizing
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 3 channels (RGB)
        buffer.order(ByteOrder.nativeOrder()) // Ensure native byte order

        // Resize image to the input size expected by the model
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val intValues = IntArray(inputSize * inputSize)

        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        // Normalize pixel values to [0, 1]
        for (pixel in intValues) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // Red channel
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // Green channel
            buffer.putFloat((pixel and 0xFF) / 255.0f)          // Blue channel
        }

        return buffer
    }
}
