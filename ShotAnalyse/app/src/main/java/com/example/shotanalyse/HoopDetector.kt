package com.example.shotanalyse

import kotlin.math.pow
import kotlin.math.sqrt

class HoopDetector {
    var hoopPosition: Pair<Float, Float>? = null
    private var scored: Boolean = false
    private val SCORE_IOU_THRESHOLD = 0.8

    fun checkIfScored(trajectory: List<Pair<Float, Float>>) {
        val ballPosition = trajectory.lastOrNull() ?: return
        hoopPosition?.let {
            val distanceToHoop = sqrt(
                (it.first - ballPosition.first).pow(2) + (it.second - ballPosition.second).pow(2)
            )
            scored = distanceToHoop <= 30 // Assuming 30 is the radius of the hoop
        }
    }

    fun isScored(): Boolean {
        return scored
    }

    fun calculateHasScored(boxSets: List<List<BoundingBox>>): Boolean {
        val ballBoxSets = boxSets.map { it.filter { it.classId == 0 } }
        val hoopBoxSets = boxSets.map { it.filter { it.classId == 1 } }
        // Consider only the highest confidence box for each set
        // Some
        val filteredBallBoxSets = ballBoxSets.map { it.maxByOrNull { it.confidence } }
        val filteredHoopBoxSets = hoopBoxSets.map { it.maxByOrNull { it.confidence } }
        // TODO: We can add additional logic here for more complex filtering

        // Check if any pair of ball and hoop boxes have a certain IoU threshold


        return distanceToHoop <= 30
    }
}

