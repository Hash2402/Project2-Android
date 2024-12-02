package com.example.shotanalyse

data class BoundingBox(
    val classId: Int,
    val className: String,
    val confidence: Float,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

fun extractBoundingBoxes(
    output: Array<Array<FloatArray>>, // Model output
    confidenceThreshold: Float = 0.3f, // Confidence threshold
    classNames: Array<String>, // Array of class names (e.g., ["Ball", "Hoop"])
    frameWidth: Int, // Frame width
    frameHeight: Int // Frame height
): List<BoundingBox> {
    println("extracting bounding boxes")
    val boundingBoxes = mutableListOf<BoundingBox>()

    // Transpose the output[0] array (from [7, 8400] to [8400, 7])
    val transposedOutput = Array(8400) { FloatArray(7) }
    for (i in output[0].indices) {
        for (j in output[0][i].indices) {
            transposedOutput[j][i] = output[0][i][j]
        }
    }

    // Iterate through the transposed predictions
    for (prediction in transposedOutput) {
        // Extract bounding box coordinates (center X, center Y, width, height)
        val centerX = prediction[0]
        val centerY = prediction[1]
        val width = prediction[2]
        val height = prediction[3]

        // Extract class probabilities
        val classProbabilities = prediction.sliceArray(4..6)
        val maxProbability = classProbabilities.maxOrNull() ?: 0f
        val classId = classProbabilities.toList().indexOf(maxProbability)

        // Only include the detection if the probability exceeds the threshold
        if (maxProbability > confidenceThreshold && classId in classNames.indices) {
            // Convert to absolute coordinates (top-left corner format)
            val absoluteX = (centerX - width / 2) * frameWidth
            val absoluteY = (centerY - height / 2) * frameHeight
            val absoluteWidth = width * frameWidth
            val absoluteHeight = height * frameHeight

            boundingBoxes.add(
                BoundingBox(
                    classId = classId,
                    className = classNames[classId],
                    confidence = maxProbability,
                    x = absoluteX,
                    y = absoluteY,
                    width = absoluteWidth,
                    height = absoluteHeight
                )
            )


        }
    }

    return boundingBoxes
}






