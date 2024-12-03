package com.example.shotanalyse

import java.lang.Math.toDegrees
import kotlin.math.*
import kotlin.math.atan

/**
 * Extension function to convert radians to degrees.
 */
fun Double.toDegrees(): Double = Math.toDegrees(this)
fun Float.toDegrees(): Float = Math.toDegrees(this.toDouble()).toFloat()


class PhysicsCalculator {

    fun getOptimalPath(
        parameters: List<Float>,
        startPosition: Pair<Float, Float>,
        hoopPosition: Pair<Float, Float>
    ): List<Float>? {
        // Extract the initial quadratic parameters
        val initialA = parameters[0]
        val initialB = parameters[1]
        val initialC = parameters[2]

        // Calculate the initial velocity magnitude
        val initialSpeed = sqrt(initialA * initialA + initialB * initialB)

        // Calculate the horizontal and vertical distance between start and hoop
        val dx = hoopPosition.first - startPosition.first
        val dy = hoopPosition.second - startPosition.second

        // Try different launch angles to find a valid trajectory
        val angleSearchRange = (-Math.PI/2).toInt() .. (Math.PI/2).toInt()
        val angleStep = 1

        for (angle in angleSearchRange.step(angleStep)) {
            // Decompose velocity into x and y components
            val vx = (initialSpeed * cos(angle.toDouble())).toFloat()
            val vy = (initialSpeed * sin(angle.toDouble())).toFloat()

            // Calculate time to reach x-distance
            val timeToReachX = dx / vx

            // Calculate parabola coefficients
            // y = a*x^2 + b*x + c
            val a = -initialA / (2 * vx * vx)
            val b = vy / vx
            val c = startPosition.second

            // Calculate expected y position at this time
            val expectedY = a * dx * dx + b * dx + c

            // Check if the expected y is close enough to the hoop's y position
            if (abs(expectedY - hoopPosition.second) < 10f) {
                // Return the found trajectory and parabola details
                return listOf(
                    vx,            // x-velocity
                    vy,            // y-velocity
                    initialA,      // original gravitational acceleration
                    timeToReachX,  // time of flight
                    a,             // new parabola a coefficient
                    b,             // new parabola b coefficient
                    c,             // new parabola c coefficient
                    angle.toFloat()
                )
            }
        }

        // No valid trajectory found
        return null
    }

    fun getAnalysisOfPath(startPosition: Pair<Float, Float>, parameters: List<Float>): Float {
        if (parameters.size < 3) {
            throw IllegalArgumentException("The parameters list must have at least 3 values (a, b, and c).")
        }

        val a = parameters[0]
        val b = parameters[1]
        // val c = parameters[2] // Not needed for derivative

        val (x0, _) = startPosition

        // Compute the slope (derivative) at x0
        val slope = 2 * a * x0 + b

        // Compute the angle of release in radians
        val angleRadians = atan(slope)

        // Convert the angle to degrees (optional)
        return toDegrees(angleRadians.toDouble()).toFloat()
    }

    fun getLaunchSpeed(parameters: List<Float>): Float {
        if (parameters.size < 2) {
            throw IllegalArgumentException("The parameters list must have at least 2 values (a and b).")
        }
        val initialA = parameters[0]
        val initialB = parameters[1]

        // Calculate the initial velocity magnitude
        return sqrt(initialA * initialA + initialB * initialB)
    }
}
