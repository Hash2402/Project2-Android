package com.example.shotanalyse

class TrajectoryTracker {
    private val ballTrajectory = mutableListOf<Pair<Float, Float>?>() // List of ball positions
    private val hoopPositions = mutableListOf<Pair<Float, Float>>() // List of hoop positions
    private val ballPositions = mutableListOf<Pair<Float, Float>?>()

    var hoopPosition: Pair<Float, Float>? = null
        private set

    /**
     * Tracks the ball and hoop positions from the model output.
     * @param output The output array from the model.
     */
    fun track(output: Array<Array<FloatArray>>) {
        if (output.isEmpty() || output[0].isEmpty()) {
            println("Invalid or empty output from model.")
            return
        }

        // Filter and track ball detections
        val ballDetections = output[0].filter { it[0].toInt() == 0 && it[1] > 0.5 }
        ballDetections.firstOrNull()?.let {
            val ballCenter = Pair((it[2] + it[4]) / 2, (it[3] + it[5]) / 2)
            ballTrajectory.add(ballCenter)
        }

        // Filter and track hoop detections
        val hoopDetections = output[0].filter { it[0].toInt() == 1 && it[1] > 0.5 }
        hoopDetections.firstOrNull()?.let {
            val hoopCenter = Pair((it[2] + it[4]) / 2, (it[3] + it[5]) / 2)
            hoopPositions.add(hoopCenter)
            hoopPosition = hoopCenter // Update latest hoop position
        }

        println("Tracked Ball: ${ballTrajectory.lastOrNull() ?: "None"}")
        println("Tracked Hoop: $hoopPosition")
    }

    /**
     * Returns the full trajectory of the ball.
     * @return List of ball positions as (x, y) coordinates.
     */
    fun getTrajectory(): List<Pair<Float, Float>> = ballTrajectory

    /**
     * Returns the most recent ball position.
     * @return Latest ball position or null if no positions are tracked.
     */
    fun getLastBallPosition(): Pair<Float, Float>? = ballTrajectory.lastOrNull()

    /**
     * Clears all tracked data.
     */
    fun clear() {
        ballTrajectory.clear()
        hoopPositions.clear()
        hoopPosition = null
    }

    fun updateHoopPosition(position: Pair<Float, Float>) {
        hoopPosition = position
    }

    fun updateTrajectory(position: Pair<Float, Float>) {
        ballTrajectory.add(position)
    }


    fun filterBoxSets(boxSets: List<List<BoundingBox>>): List<List<BoundingBox?>> {
        val ballBoxSets = boxSets.map { it.filter { it.classId == 0 } }
        val hoopBoxSets = boxSets.map { it.filter { it.classId == 1 } }
        // Boxes are in cx, cy, w, h format
        // Select the highest confidence box for each set
        val filteredBallBoxSets = ballBoxSets.map { it.maxByOrNull { it.confidence } }
        val filteredHoopBoxSets = hoopBoxSets.map { it.maxByOrNull { it.confidence } }

        return listOf(filteredBallBoxSets, filteredHoopBoxSets)
    }

    // Transform the bounding boxes into positions and store them
    // Requires there to be at least one detection
    fun processBoxes(ballBoxes: List<BoundingBox?>, hoopBoxes: List<BoundingBox?>) {
        ballPositions.clear()
        hoopPositions.clear()
        // Extract hoop positions
        val incompleteHoopPositions = mutableListOf<Pair<Float, Float>?>()
        for (i in hoopBoxes.indices) {
            val hoop = hoopBoxes[i]
            if (hoop != null) {
                val x = hoop.x
                val y = hoop.y
                incompleteHoopPositions.add(Pair(x, y))
            } else {
                incompleteHoopPositions.add(null)
            }
        }

        // Find first non-null hoop position
        var firstHoopPosition: Pair<Float, Float>? = null
        for (i in hoopPositions.indices) {
            if (incompleteHoopPositions[i] != null) {
                firstHoopPosition = incompleteHoopPositions[i]
                break
            }
        }
        // Backfill missing positions for hoop
        for (i in incompleteHoopPositions.indices) {
            if (incompleteHoopPositions[i] == null) {
                incompleteHoopPositions[i] = firstHoopPosition
            } else {
                break
            }
        }
        // Identify the last non-null position
        var lastHoopPosition: Pair<Float, Float>? = null
        var lastHoopIndex = 0
        for (i in incompleteHoopPositions.indices) {
            if (incompleteHoopPositions[i] != null) {
                lastHoopPosition = incompleteHoopPositions[i]
                lastHoopIndex = incompleteHoopPositions.indexOf(incompleteHoopPositions[i])
                break
            }
        }
        // Forward fill missing positions at the end
        for (i in hoopBoxes.size - 1 downTo lastHoopIndex) {
            incompleteHoopPositions[i] = lastHoopPosition
        }
        // Interpolate missing positions and add to hoopPositions
        // We know the first and last are non-null due to the filling
        for (i in incompleteHoopPositions.indices) {
            if (incompleteHoopPositions[i] == null) {
                // Find the most recent and next non-null position
                var interpolationFirst = 0
                var interpolationLast = ballBoxes.size - 1
                for (j in i downTo 0) {
                    if (incompleteHoopPositions[j] != null) {
                        interpolationFirst = j
                        break
                    }
                }
                for (j in i until ballBoxes.size) {
                    if (incompleteHoopPositions[j] != null) {
                        interpolationLast = j
                        break
                    }
                }
                // Interpolate
                val interpolationFirstPosition = incompleteHoopPositions[interpolationFirst]!!
                val interpolationLastPosition = incompleteHoopPositions[interpolationLast]!!

                val interpolationSteps = interpolationLast - interpolationFirst
                val interpolationDiffX = interpolationLastPosition.first - interpolationFirstPosition.first
                val interpolationDiffY = interpolationLastPosition.second - interpolationFirstPosition.second

                val interpolationStepX = interpolationDiffX / interpolationSteps
                val interpolationStepY = interpolationDiffY / interpolationSteps

                val numSteps = i - interpolationFirst

                val interpolationX = interpolationFirstPosition.first + (numSteps * interpolationStepX)
                val interpolationY = interpolationFirstPosition.second + (numSteps * interpolationStepY)
                val interpolation = Pair(interpolationX, interpolationY)
                hoopPositions.add(interpolation)
            } else {
                hoopPositions.add(incompleteHoopPositions[i]!!)
            }
        }
        for (i in ballBoxes.indices) {
            val ball = ballBoxes[i]
            if (ball != null) {
                val x = ball.x
                val y = ball.y
                ballPositions.add(Pair(x, y))
            } else {
                ballPositions.add(null)
            }
        }
        // Now we have a full list of hoop positions
        // Fill the trajectory
        for (i in hoopPositions.indices) {
            val hoopPosition = hoopPositions[i]
            val ballPosition = ballPositions[i]
            if (ballPosition != null) {
                val x = hoopPosition.first - ballPosition.first
                val y = hoopPosition.second - ballPosition.second
                ballTrajectory.add(Pair(x, y))
            }
        }
    }

    // TODO: Take the points in ballTrajectory and fit a curve
    fun fitTrajectory() {

    }

}