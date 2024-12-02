package com.example.shotanalyse
/*
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt
import com.example.shotanalyse.extractBoundingBoxes
import com.example.shotanalyse.BoundingBox


class VideoProcessor(
    private val context: Context,
    private val model: Interpreter
) {
    private val trajectoryTracker = TrajectoryTracker()
    private val physicsCalculator = PhysicsCalculator()
    private val hoopDetector = HoopDetector()

    private val ballPositions = mutableListOf<Pair<Float, Float>>() // Temporary storage for ball positions
    private var hoopPosition: Pair<Float, Float>? = null           // Storage for the hoop position

    private val boxSets = mutableListOf<List<BoundingBox>>()
    private val filteredBallBoxes = mutableListOf<BoundingBox?>()
    private val filteredHoopBoxes = mutableListOf<BoundingBox?>()

    // a, b, and c terms of the parabola
    private var parameters = listOf(0f, 0f, 0f)

    private val _FRAMERATE = 1

    fun processVideoFrames(videoUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val retriever = MediaMetadataRetriever()
            // Clear current data
            boxSets.clear()
            try {
                retriever.setDataSource(context, videoUri)

                val videoDuration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                val frameInterval = 1000000 / _FRAMERATE // 1 second in microseconds

                val classNames = arrayOf("Ball", "Hoop") // Define your class names
                val frameWidth = 1920 // Example frame width
                val frameHeight = 1080 // Example frame height

                for (timeUs in 0 until (videoDuration * 1000).toInt() step frameInterval) {
                    println("extracting boxes (${timeUs / frameInterval}/${(videoDuration * 1000).toInt() / frameInterval})")
                    val frame = retriever.getFrameAtTime(timeUs.toLong(), MediaMetadataRetriever.OPTION_CLOSEST)
                    frame?.let {
                        val processedFrame = preprocessFrame(it)
                        val output = runModel(processedFrame)

                        // Use the updated extractBoundingBoxes function
                        val boundingBoxes = extractBoundingBoxes(output, 0.5f, classNames, frameWidth, frameHeight)

                        // Process bounding boxes for trajectory and hoop detection
                        boxSets.add(boundingBoxes)
//                        boundingBoxes.forEach { box ->
//                            when (box.className) {
//                                "Ball" -> ballPositions.add(Pair(box.x, box.y))
//                                "Hoop" -> hoopPosition = Pair(box.x, box.y)
//                            }
//
//                            println("Detected ${box.className} with confidence ${box.confidence}: " +
//                                    "(${box.x}, ${box.y}, ${box.width}, ${box.height})")
//                        }
                    } ?: println("Frame at $timeUs could not be retrieved.")
                }

                // After processing all frames, analyze the trajectory
                calculateTrajectory()
//                updateTrajectoryTracker()
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
        filteredBallBoxes.clear()
        filteredHoopBoxes.clear()
        val filterResults = trajectoryTracker.filterBoxSets(boxSets)
        for (boxSet in filterResults) {
            filteredBallBoxes.add(boxSet[0])
            filteredHoopBoxes.add(boxSet[1])
        }
        // Process the box sets and store them in the tracker
        trajectoryTracker.processBoxes(filteredBallBoxes, filteredHoopBoxes)
        // Calculate the final trajectory
        println(trajectoryTracker.getBallTrajectory())
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



    private fun updateTrajectoryTracker() {
        // Update trajectory tracker with ball positions
        ballPositions.forEach { position ->
            trajectoryTracker.updateTrajectory(position)
        }
        // Update hoop position using the method
        hoopPosition?.let { trajectoryTracker.updateHoopPosition(it) }
    }


    // TODO: take the parabola and check if scored
    // and calculate optimal path if needed
    private fun analyzeTrajectory() {
        val trajectory = trajectoryTracker.getTrajectory()
        val hoopPosition = trajectoryTracker.hoopPosition

//        if (trajectory.isEmpty() || hoopPosition == null) {
//            println("Not enough data for analysis. Trajectory or hoop position missing.")
//            return
//        }
        // TODO: error checking

        // this should use the boxes
        val hasScored = hoopDetector.calculateHasScored(parameters, trajectoryTracker.getHoopPositions())

        if (hasScored) {
            println("The ball went through the hoop!")
        } else {
            println("The ball missed the hoop. Calculating optimal path...")
//            physicsCalculator.calculateOptimalPath(trajectory, hoopPosition)
            var hoopPosition = trajectoryTracker.getHoopPositions().firstOrNull()
            if (hoopPosition == null) {
//                println("No hoop position found.")
//                return
                hoopPosition = Pair(0f, 0f)
            }
            if (trajectoryTracker.getBallTrajectory().isEmpty()) {
                println("No ball trajectory found.")
                return
            }
            val startPosition = trajectoryTracker.getBallTrajectory().first()
            val optimalParams = physicsCalculator.getOptimalPath(parameters, startPosition, hoopPosition)
            println("Optimal parameters: $optimalParams")
            if (optimalParams == null) {
                println("No optimal parameters found.")
                return
            }
            val optimalAngle = physicsCalculator.getAnalysisOfPath(startPosition, optimalParams)
            println("Optimal angle: $optimalAngle")
        }
    }
}


*/

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.graphics.Canvas
//import androidx.compose.ui.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt
import com.example.shotanalyse.extractBoundingBoxes
import com.example.shotanalyse.BoundingBox




class VideoProcessor(
    private val context: Context,
    private val model: Interpreter
) {
    private val trajectoryTracker = TrajectoryTracker()
    private val physicsCalculator = PhysicsCalculator()
    private val hoopDetector = HoopDetector()


    private val ballPositions = mutableListOf<Pair<Float, Float>>() // Temporary storage for ball positions
    private var hoopPosition: Pair<Float, Float>? = null           // Storage for the hoop position


    private val boxSets = mutableListOf<List<BoundingBox>>()
    private val filteredBallBoxes = mutableListOf<BoundingBox?>()
    private val filteredHoopBoxes = mutableListOf<BoundingBox?>()


    // a, b, and c terms of the parabola
    private var parameters = listOf(0f, 0f, 0f)


    private val _FRAMERATE = 1


    fun processVideoFrames(videoUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val retriever = MediaMetadataRetriever()
            // Clear current data
            boxSets.clear()
            try {
                retriever.setDataSource(context, videoUri)


                val videoDuration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
                val frameInterval = 1000000 / _FRAMERATE // 1 second in microseconds


                val classNames = arrayOf("Ball", "Hoop") // Define your class names
                val frameWidth = 1920 // Example frame width
                val frameHeight = 1080 // Example frame height


                for (timeUs in 0 until (videoDuration * 1000).toInt() step frameInterval) {
                    println("extracting boxes (${timeUs / frameInterval}/${(videoDuration * 1000).toInt() / frameInterval})")
                    val frame = retriever.getFrameAtTime(timeUs.toLong(), MediaMetadataRetriever.OPTION_CLOSEST)
                    frame?.let {
                        val processedFrame = preprocessFrame(it)
                        val output = runModel(processedFrame)


                        // Use the updated extractBoundingBoxes function
                        val boundingBoxes = extractBoundingBoxes(output, 0.5f, classNames, frameWidth, frameHeight)


                        // Process bounding boxes for trajectory and hoop detection
                        boxSets.add(boundingBoxes)
//                        boundingBoxes.forEach { box ->
//                            when (box.className) {
//                                "Ball" -> ballPositions.add(Pair(box.x, box.y))
//                                "Hoop" -> hoopPosition = Pair(box.x, box.y)
//                            }
//
//                            println("Detected ${box.className} with confidence ${box.confidence}: " +
//                                    "(${box.x}, ${box.y}, ${box.width}, ${box.height})")
//                        }
                    } ?: println("Frame at $timeUs could not be retrieved.")
                }


                // After processing all frames, analyze the trajectory
                calculateTrajectory()
//                updateTrajectoryTracker()
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
        filteredBallBoxes.clear()
        filteredHoopBoxes.clear()
        val filterResults = trajectoryTracker.filterBoxSets(boxSets)
        for (boxSet in filterResults) {
            filteredBallBoxes.add(boxSet[0])
            filteredHoopBoxes.add(boxSet[1])
        }
        // Process the box sets and store them in the tracker
        trajectoryTracker.processBoxes(filteredBallBoxes, filteredHoopBoxes)
        // Calculate the final trajectory
        println(trajectoryTracker.getBallTrajectory())
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






    private fun updateTrajectoryTracker() {
        // Update trajectory tracker with ball positions
        ballPositions.forEach { position ->
            trajectoryTracker.updateTrajectory(position)
        }
        // Update hoop position using the method
        hoopPosition?.let { trajectoryTracker.updateHoopPosition(it) }
    }

    // take the parabola and check if scored
    // and calculate optimal path if needed
    private fun analyzeTrajectory() {
        val trajectory = trajectoryTracker.getTrajectory()
        val hoopPosition = trajectoryTracker.hoopPosition


        if (trajectory.isEmpty() || hoopPosition == null) {
            println(trajectory)
            println(hoopPosition)
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
        val angleResult = physicsCalculator.getAnalysisOfPath(startPosition, parameters).toString()
        println(parameters)
        println(optimalParams)
        println(angleResult)

        // Retrieve the first frame
        val firstFrame = boxSets.firstOrNull()?.let {
            Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888) // Replace with actual frame
        }


        if (firstFrame != null) {
            val overlayedBitmap = drawParabolas(firstFrame, parameters, optimalParams)

            // Pass data to ResultsActivity
            CoroutineScope(Dispatchers.Main).launch {
                val intent = Intent(context, ResultsActivity::class.java).apply {
                    putExtra("RESULT_MESSAGE", if (hasScored) "Scored!" else "Missed")
                    putExtra("OPTIMAL_ANGLE", if (hasScored) "---" else optimalAngleResult)
                    putExtra("LAUNCH_ANGLE", angleResult)
                    putExtra("OVERLAYED_BITMAP", BitmapUtils.bitmapToByteArray(overlayedBitmap))
                    putExtra("LAUNCH_VELOCITY", "$launchVelocity m/s")
                }
                context.startActivity(intent)
            }
        }
    }


    private fun drawParabolas(
        frame: Bitmap,
        originalParams: List<Float>,
        optimalParams: List<Float>?
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
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }


        // Draw original parabola (red)
        for (x in 0 until mutableBitmap.width) {
            val y = (originalParams[0] * x.toFloat().pow(2) +
                    originalParams[1] * x +
                    originalParams[2])
            if (y.toInt() in 0 until mutableBitmap.height) {
                canvas.drawPoint(x.toFloat(), y, paintRed)
            }
        }

        if (optimalParams != null) {
            // Draw optimal parabola (green)
            for (x in 0 until mutableBitmap.width) {
                val y = (optimalParams[0] * x.toFloat().pow(2) +
                        optimalParams[1] * x +
                        optimalParams[2])
                if (y.toInt() in 0 until mutableBitmap.height) {
                    canvas.drawPoint(x.toFloat(), y, paintGreen)
                }
            }
        }


        return mutableBitmap
    }
}
