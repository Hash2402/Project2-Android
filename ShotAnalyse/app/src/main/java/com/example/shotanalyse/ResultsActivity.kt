package com.example.shotanalyse


import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView


class ResultsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Retrieve data from Intent
        val resultMessage = intent.getStringExtra("RESULT_MESSAGE") ?: "No result"
        val optimalAngle = intent.getStringExtra("OPTIMAL_ANGLE") ?: "No optimal angle"
        val bitmapBytes = BitmapHolder.bitmap
        val launchAngle = intent.getStringExtra("LAUNCH_ANGLE")
        val launchVelocity = intent.getStringExtra("LAUNCH_VELOCITY")


        val bitmap = BitmapUtils.byteArrayToBitmap(bitmapBytes)

        findViewById<TextView>(R.id.analysisTitle).text = resultMessage
        findViewById<TextView>(R.id.launchAngleValue).text = launchAngle
        findViewById<TextView>(R.id.expectedLaunchAngleValue).text = optimalAngle
        findViewById<TextView>(R.id.launchVelocityValue).text = launchVelocity

        findViewById<ImageView>(R.id.videoPreview).setImageBitmap(bitmap)
    }


}
