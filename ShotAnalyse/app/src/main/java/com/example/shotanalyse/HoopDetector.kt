package com.example.shotanalyse

import kotlin.math.pow
import kotlin.math.sqrt

class HoopDetector {
    var hoopPosition: Pair<Float, Float>? = null
//    private var scored: Boolean = false
    private val SCORE_IOU_THRESHOLD = 0.8

//    fun checkIfScored(trajectory: List<Pair<Float, Float>>) {
//        val ballPosition = trajectory.lastOrNull() ?: return
//        hoopPosition?.let {
//            val distanceToHoop = sqrt(
//                (it.first - ballPosition.first).pow(2) + (it.second - ballPosition.second).pow(2)
//            )
//            scored = distanceToHoop <= 30 // Assuming 30 is the radius of the hoop
//        }
//    }

//    fun isScored(): Boolean {
//        return scored
//    }

    // Uses the parabola to determine if we have scored
    fun calculateHasScored(parameters: List<Float>, hoopPositions: List<Pair<Float, Float>>): Boolean {
        if (parameters.size < 3) {
            throw IllegalArgumentException("The parameters list must have at least 3 values (a, b, and c).")
        }

        val a = parameters[0]
        val b = parameters[1]
        val c = parameters[2]
        val thresholdDistance = 10.0f

        for ((hoopX, hoopY) in hoopPositions) {
            // Calculate the y value of the parabola at hoopX
            val parabolaY = a * hoopX * hoopX + b * hoopX + c

            // Calculate the distance to the hoop position
            val distance = kotlin.math.sqrt((parabolaY - hoopY).pow(2) + (hoopX - hoopX).pow(2)) // Only y matters as x coincides

            // Check if the distance is within the threshold
            if (distance <= thresholdDistance) {
                return true
            }
        }

        return false

    }
}

