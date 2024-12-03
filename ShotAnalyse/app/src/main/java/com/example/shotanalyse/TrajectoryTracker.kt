package com.example.shotanalyse

import kotlin.math.pow

class TrajectoryTracker {
    private var ballTrajectory = mutableListOf<Pair<Float, Float>>() // List of ball positions
    private val hoopPositions = mutableListOf<Pair<Float, Float>>() // List of hoop positions
    private val ballPositions = mutableListOf<Pair<Float, Float>?>()
    private var startPosition = Pair(0f, 0f)

    fun getStartPosition(): Pair<Float, Float> = startPosition

    var hoopPosition: Pair<Float, Float>? = null
        private set

    fun getBallTrajectory(): List<Pair<Float, Float>> = ballTrajectory

    fun getHoopPositions(): List<Pair<Float, Float>> = hoopPositions
    /**
     * Tracks the ball and hoop positions from the model output.
     * @param output The output array from the model.
     */
    fun track(output: Array<Array<FloatArray>>) {
        if (output.isEmpty() || output[0].isEmpty()) {
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

    }

    /**
     * Returns the full trajectory of the ball.
     * @return List of ball positions as (x, y) coordinates.
     */
    fun getTrajectory(): List<Pair<Float, Float>> = ballTrajectory.toList()

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
        // Ensure there is at least one detection
        if (ballBoxes.filterNotNull().size == 0 || hoopBoxes.filterNotNull().size == 0) {
            throw java.lang.IllegalArgumentException("No detections found.")
            // Dummy values
            ballTrajectory = mutableListOf(
                Pair(100f, 600f),
                Pair(320f, 100f),
                Pair(600f, 50f)
            )
            ballPositions.add(Pair(100f, 600f))
            hoopPosition=Pair(640f,50f)
            hoopPosition = Pair(600f, 50f)
            hoopPositions.add(Pair(600f, 55f))
            return
        }
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
        var firstHoopIndex = 0
        for (i in incompleteHoopPositions.indices) {
            if (incompleteHoopPositions[i] != null) {
                firstHoopIndex = i
                break
            }
        }
        // Backfill missing positions for hoop
        for (i in incompleteHoopPositions.indices) {
            if (incompleteHoopPositions[i] == null) {
                incompleteHoopPositions[i] = incompleteHoopPositions[firstHoopIndex]
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
//                val x = hoopPosition.first - ballPosition.first
//                val y = hoopPosition.second - ballPosition.second
                val x = ballPosition.first
                val y = ballPosition.second
                ballTrajectory.add(Pair(x, y))
            }
        }
        // Use first hoop position as start
        hoopPosition = hoopPositions.first()
        // Use first ball position as start as well
        startPosition = ballTrajectory.first()

    }

    fun fitTrajectory(): List<Float> {
        if (ballTrajectory.size < 3) {
            throw IllegalArgumentException("At least 3 points are required to fit a parabola.")
        }

        // Construct the normal equations for least squares
        var sumX = 0.0
        var sumX2 = 0.0
        var sumX3 = 0.0
        var sumX4 = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumX2Y = 0.0

        for ((x, y) in ballTrajectory) {
            val xDouble = x.toDouble()
            val yDouble = y.toDouble()
            sumX += xDouble
            sumX2 += xDouble.pow(2)
            sumX3 += xDouble.pow(3)
            sumX4 += xDouble.pow(4)
            sumY += yDouble
            sumXY += xDouble * yDouble
            sumX2Y += xDouble.pow(2) * yDouble
        }

        // Solve the system of linear equations Ax = B
        val n = ballTrajectory.size.toDouble()

        val aMatrix = arrayOf(
            doubleArrayOf(sumX4, sumX3, sumX2),
            doubleArrayOf(sumX3, sumX2, sumX),
            doubleArrayOf(sumX2, sumX, n)
        )

        val bVector = doubleArrayOf(sumX2Y, sumXY, sumY)

        val coefficients = solveLinearSystem(aMatrix, bVector)

        // Convert the result back to Float
        return coefficients.map { it.toFloat() }
    }

    // Helper function to solve a system of linear equations using Gaussian elimination
    fun solveLinearSystem(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = b.size

        // Augment the matrix A with the vector B
        val augmentedMatrix = Array(n) { i -> a[i] + doubleArrayOf(b[i]) }

        // Perform Gaussian elimination
        for (i in 0 until n) {
            // Pivot for maximum value in column
            var maxRow = i
            for (k in i + 1 until n) {
                if (kotlin.math.abs(augmentedMatrix[k][i]) > kotlin.math.abs(augmentedMatrix[maxRow][i])) {
                    maxRow = k
                }
            }
            val temp = augmentedMatrix[i]
            augmentedMatrix[i] = augmentedMatrix[maxRow]
            augmentedMatrix[maxRow] = temp

            // Make all rows below this one 0 in the current column
            for (k in i + 1 until n) {
                val factor = augmentedMatrix[k][i] / augmentedMatrix[i][i]
                for (j in i until n + 1) {
                    augmentedMatrix[k][j] -= factor * augmentedMatrix[i][j]
                }
            }
        }

        // Solve the upper triangular matrix
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = 0.0
            for (j in i + 1 until n) {
                sum += augmentedMatrix[i][j] * x[j]
            }
            x[i] = (augmentedMatrix[i][n] - sum) / augmentedMatrix[i][i]
        }

        return x
    }


}