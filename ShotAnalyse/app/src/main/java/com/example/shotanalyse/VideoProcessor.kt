package com.example.shotanalyse

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.pow


object BitmapHolder {
    var bitmap: ByteArray? = null
}

class VideoProcessor(
    private val context: Context,
    private val model: Interpreter
) {
    private val trajectoryTracker = TrajectoryTracker()
    private val physicsCalculator = PhysicsCalculator()
    private val hoopDetector = HoopDetector()

    private val boxSets = mutableListOf<List<BoundingBox>>()
    private var filteredBallBoxes = mutableListOf<BoundingBox?>()
    private var filteredHoopBoxes = mutableListOf<BoundingBox?>()


    // a, b, and c terms of the parabola
    private var parameters = listOf(0f, 0f, 0f)


    private val _FRAMERATE = 5

    private var firstFrame: Bitmap? = null


    fun processVideoFrames(videoUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val retriever = MediaMetadataRetriever()
            // Clear current data
            boxSets.clear()
            try {
                retriever.setDataSource(context, videoUri)


                val videoDuration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                val frameInterval = 1000000 / _FRAMERATE


                val classNames = arrayOf("Ball", "Hoop")
                var frameWidth = 1920
                var frameHeight = 1080


                for (timeUs in 0 until (videoDuration * 1000).toInt() step frameInterval) {

//                    println("extracting boxes (${timeUs / frameInterval}/${(videoDuration * 1000).toInt() / frameInterval})")
                    val frame = retriever.getFrameAtTime(timeUs.toLong(), MediaMetadataRetriever.OPTION_CLOSEST)
                    if (timeUs == 0) {
                        firstFrame = frame
                    }
                    frame?.let {
                        frameWidth = frame.width
                        frameHeight = frame.height
                        val processedFrame = preprocessFrame(it)
                        val output = runModel(processedFrame)

                        val boundingBoxes = extractBoundingBoxes(output, 0.5f, classNames, frameWidth, frameHeight)

                        boxSets.add(boundingBoxes)
                    } ?: println("Frame at $timeUs could not be retrieved.")
                }


                calculateTrajectory()
                analyzeTrajectory()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }


    private fun calculateTrajectory() {
        // Filter the box sets
        val filterResults = trajectoryTracker.filterBoxSets(boxSets)
        filteredBallBoxes = filterResults[0].toMutableList()
        filteredHoopBoxes = filterResults[1].toMutableList()
        // Process the box sets and store them in the tracker
        trajectoryTracker.processBoxes(filteredBallBoxes, filteredHoopBoxes)
        // Calculate the final trajectory
        parameters = trajectoryTracker.fitTrajectory()
    }


    private fun preprocessFrame(frame: Bitmap): ByteBuffer {
        val resizedFrame = Bitmap.createScaledBitmap(frame, 640, 640, true)
        return BitmapUtils.convertBitmapToByteBuffer(resizedFrame)
    }


    private fun runModel(frameBuffer: ByteBuffer): Array<Array<FloatArray>> {
        val output = Array(1) { Array(7) { FloatArray(8400) } }
        model.run(frameBuffer, output)
        //println(output.contentDeepToString()) // Debugging output
        return output
    }








    // take the parabola and check if scored
    // and calculate optimal path if needed
    private fun analyzeTrajectory() {
        val trajectory = trajectoryTracker.getTrajectory()
        val hoopPosition = trajectoryTracker.hoopPosition


        if (trajectory.isEmpty() || hoopPosition == null) {
            println("Not enough data for analysis. Trajectory or hoop position missing.")
            return
        }


        val hasScored = hoopDetector.calculateHasScored(parameters, trajectoryTracker.getHoopPositions())


        val optimalValues = physicsCalculator.getOptimalPath(
            parameters,
            trajectoryTracker.getBallTrajectory().firstOrNull() ?: Pair(0f, 0f),
            trajectoryTracker.getHoopPositions().firstOrNull() ?: Pair(0f, 0f)
        )

        var optimalAngleResult = ""

        var optimalParams: List<Float>? = null
        if (optimalValues != null) {
            optimalParams = listOf(optimalValues[4], optimalValues[5], optimalValues[6])
            optimalAngleResult = "${optimalValues[7]} degrees"
        } else {
            println("No optimal parameters found.")
            optimalAngleResult = "In order to make the shot, the release velocity must be changed"
        }

        val launchVelocity = physicsCalculator.getLaunchSpeed(parameters).toString()

        val startPosition = trajectoryTracker.getStartPosition()
        val angleResult = abs(physicsCalculator.getAnalysisOfPath(startPosition, parameters)).toString()

        // Retrieve the first frame
        val firstFrame = firstFrame ?: Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)

        val ballLocations = trajectoryTracker.getBallTrajectory()
        val overlayedBitmap = drawParabolas(firstFrame, parameters, optimalParams, ballLocations)

        // Pass data to ResultsActivity
        CoroutineScope(Dispatchers.Main).launch {

            BitmapHolder.bitmap = BitmapUtils.bitmapToByteArray(overlayedBitmap)
            val intent = Intent(context, ResultsActivity::class.java).apply {
                putExtra("RESULT_MESSAGE", if (hasScored) "Scored!" else "Missed")
                putExtra("OPTIMAL_ANGLE", if (hasScored) "---" else optimalAngleResult)
                putExtra("LAUNCH_ANGLE", angleResult)
                putExtra("LAUNCH_VELOCITY", "$launchVelocity m/s")
            }
            context.startActivity(intent)
        }
    }

    private fun drawParabolas(
        frame: Bitmap,
        originalParams: List<Float>,
        optimalParams: List<Float>?,
        positions: List<Pair<Float, Float>> // List of (x, y) pairs for green dots
    ): Bitmap {
        val mutableBitmap = frame.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paintRed = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val paintGreen = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL // For green dots
            strokeWidth = 10f
        }

        // Get the height of the frame to scale the parabola
        val frameHeight = mutableBitmap.height.toFloat()

        // Find the minimum and maximum y-values of the parabola for scaling
        val (minY, maxY) = getMinMaxY(originalParams, mutableBitmap.width)

        // Calculate the scale factor to map the parabola y-values to the frame height
        val scaleY = frameHeight / (maxY - minY)

        // Calculate the offset to ensure the parabola fits within the frame vertically
        val offsetY = -minY * scaleY

        // Draw original parabola (red)
        for (x in 0 until mutableBitmap.width) {
            val y = originalParams[0] * x.toFloat().pow(2) +
                    originalParams[1] * x +
                    originalParams[2]

            // Scale the y-value to fit the frame height
            val scaledY = y * scaleY + offsetY

            // Ensure that the scaled y-value is within the bounds of the frame
            if (scaledY in 0f..frameHeight) {
                canvas.drawPoint(x.toFloat(), scaledY, paintRed)
            }
        }

        // Draw optimal parabola (green)
        if (optimalParams != null) {
            for (x in 0 until mutableBitmap.width) {
                val y = optimalParams[0] * x.toFloat().pow(2) +
                        optimalParams[1] * x +
                        optimalParams[2]

                // Scale the y-value to fit the frame height
                val scaledY = y * scaleY + offsetY

                // Ensure that the scaled y-value is within the bounds of the frame
                if (scaledY in 0f..frameHeight) {
                    canvas.drawPoint(x.toFloat(), scaledY, paintGreen)
                }
            }
        }

        // Plot green dots for given positions
        for ((x, y) in positions) {
            // Ensure the x and y values are within the bounds of the frame
            if (x > 0 && x < mutableBitmap.width && y > 0 && y < mutableBitmap.height) {
                canvas.drawCircle(x, y, 10f, paintGreen) // Adjust radius as needed
            }
        }

        return mutableBitmap
    }

    // Helper function to find the min and max y-values of a parabola for scaling
    private fun getMinMaxY(params: List<Float>, width: Int): Pair<Float, Float> {
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        for (x in 0 until width) {
            val y = params[0] * x.toFloat().pow(2) +
                    params[1] * x +
                    params[2]
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
        }
        return Pair(minY, maxY)
    }
}
