package com.example.shotanalyse

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

    private val _FRAMERATE = 3

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
        trajectoryTracker.fitTrajectory()
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

        if (trajectory.isEmpty() || hoopPosition == null) {
            println("Not enough data for analysis. Trajectory or hoop position missing.")
            return
        }

        // Use HoopDetector to check if the ball scored
//        hoopDetector.hoopPosition = hoopPosition
//        hoopDetector.checkIfScored(trajectory)

        // this should use the boxes
        val hasScored = hoopDetector.calculateHasScored(boxSets)

        if (hasScored) {
            println("The ball went through the hoop!")
        } else {
            println("The ball missed the hoop. Calculating optimal path...")
            physicsCalculator.calculateOptimalPath(trajectory, hoopPosition)
        }
    }
}


