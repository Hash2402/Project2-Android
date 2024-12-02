package com.example.shotanalyse

import kotlin.math.*

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
}
