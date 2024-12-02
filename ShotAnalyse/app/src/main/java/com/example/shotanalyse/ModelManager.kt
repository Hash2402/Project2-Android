

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ModelManager private constructor(context: Context) {

    // Declare interpreters for each model
    val model1Interpreter: Interpreter
    //val model2Interpreter: Interpreter

    init {
        model1Interpreter = Interpreter(loadModelFile(context, "YOLO_actual_final.tflite"))
        //model2Interpreter = Interpreter(loadModelFile(context, "hoop_big_detector.tflite"))
    }

    // Function to load a model file from the assets folder
    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Clean up the interpreters to avoid memory leaks
    fun close() {
        model1Interpreter.close()

    }

    companion object {
        // Singleton instance to ensure only one instance of ModelManager is used throughout the app
        @Volatile
        private var INSTANCE: ModelManager? = null

        fun getInstance(context: Context): ModelManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModelManager(context).also { INSTANCE = it }
            }
        }
    }
}
