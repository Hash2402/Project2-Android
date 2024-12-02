package com.example.shotanalyse


import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView


class ResultsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)


        // Retrieve data from Intent
        val resultMessage = intent.getStringExtra("RESULT_MESSAGE") ?: "No result"
        val optimalAngle = intent.getStringExtra("OPTIMAL_ANGLE") ?: "No optimal angle"
        val bitmapBytes = intent.getByteArrayExtra("OVERLAYED_BITMAP")
        val launchAngle = intent.getStringExtra("LAUNCH_ANGLE")
        val launchVelocity = intent.getStringExtra("LAUNCH_VELOCITY")


        val bitmap = BitmapUtils.byteArrayToBitmap(bitmapBytes)


        // Set data to views
        findViewById<TextView>(R.id.analysisTitle).text = resultMessage
        findViewById<TextView>(R.id.launchAngleValue).text = launchAngle
        findViewById<TextView>(R.id.expectedLaunchAngleValue).text = optimalAngle
        findViewById<TextView>(R.id.launchVelocityValue).text = launchVelocity


        // Set the image with parabolas to ImageView
        findViewById<ImageView>(R.id.videoPreview).setImageBitmap(bitmap)
    }


}
