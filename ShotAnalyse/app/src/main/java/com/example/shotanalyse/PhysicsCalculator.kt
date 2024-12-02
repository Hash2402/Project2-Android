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

    /**
     * Calculates the launch velocity and angle of the ball based on its trajectory.
     * @param trajectory List of ball positions in the form of (x, y) coordinates.
     */
    fun calculatePhysics(trajectory: List<Pair<Float, Float>>) {
        if (trajectory.size < 2) return // Need at least two points to calculate

        val (x1, y1) = trajectory[trajectory.size - 2]
        val (x2, y2) = trajectory.last()

        val deltaX = x2 - x1
        val deltaY = y2 - y1
        val deltaT = 1.0f // Assuming 1 frame per second; adjust based on actual FPS

        val velocityX = deltaX / deltaT
        val velocityY = deltaY / deltaT
        val launchVelocity = sqrt(velocityX * velocityX + velocityY * velocityY)
        val launchAngle = atan2(velocityY, velocityX).toDegrees()

        println("Launch Velocity: $launchVelocity, Launch Angle: $launchAngle")
    }

    /**
     * Simulates the optimal path to reach the hoop.
     * @param trajectory List of ball positions in the form of (x, y) coordinates.
     * @param hoopPosition Position of the hoop as (x, y) coordinates.
     */

    // TODO: Implement with curve parameters
    fun calculateOptimalPath(trajectory: List<Pair<Float, Float>>, hoopPosition: Pair<Float, Float>?) {
        if (hoopPosition == null) return // Can't calculate without hoop position

        val initialPosition = trajectory.first()
        val gravity = 9.8f
        val targetX = hoopPosition.first - initialPosition.first
        val targetY = hoopPosition.second - initialPosition.second

        for (angle in 30..60 step 5) { // Adjust angle range and step as needed
            val radianAngle = Math.toRadians(angle.toDouble())
            val velocity = sqrt((gravity * targetX * targetX) / (2 * targetY * cos(radianAngle) * sin(radianAngle)))

            println("Optimal Velocity: $velocity, Optimal Angle: $angle")
        }
    }

    fun getOptimalPath(
        parameters: List<Float>,
        startPosition: Pair<Float, Float>,
        hoopPosition: Pair<Float, Float>
    ): List<Float>? {
        if (parameters.size < 3) {
            throw IllegalArgumentException("The parameters list must have at least 3 values (a, b, and c).")
        }

        val initialA = parameters[0]
        val initialB = parameters[1]
        val initialC = parameters[2]

        val (x0, y0) = startPosition
        val (xh, yh) = hoopPosition

        // Initial velocity components
        val initialVy = 2 * initialA * x0 + initialB

        // We need to solve for new `a` and `c` while keeping `b` constant
        // System of equations:
        // 1. y0 = a * x0^2 + b * x0 + c (start position constraint)
        // 2. yh = a * xh^2 + b * xh + c (hoop position constraint)

        // Constants for the linear system
        val x0Squared = x0 * x0
        val xhSquared = xh * xh

        // Formulate equations in terms of `a` and `c`
        // Equation 1: c = y0 - a * x0^2 - b * x0
        // Equation 2: yh = a * xh^2 + b * xh + c
        // Substitute c from Equation 1 into Equation 2

        val aNumerator = yh - y0 - initialB * (xh - x0)
        val aDenominator = xhSquared - x0Squared

        if (aDenominator == 0.0f) {
            // This happens if xh == x0, making it impossible to find a new parabola
            return null
        }

        val newA = aNumerator / aDenominator
        val newC = y0 - newA * x0Squared - initialB * x0

        // Return new parameters
        return listOf(newA, initialB, newC)
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
}
