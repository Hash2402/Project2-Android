// ResultsActivity.kt
package com.example.shotanalyse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        // Example data (replace with actual data passed from the previous screen)
        val launchVelocity = "8 m/s"
        val launchAngle = "40 degrees"
        val airTime = "1.75 s"
        val expectedLaunchAngle = "44 degrees"

        // Set data to the views
        findViewById<TextView>(R.id.launchVelocityValue).text = launchVelocity
        findViewById<TextView>(R.id.launchAngleValue).text = launchAngle
        findViewById<TextView>(R.id.airTimeValue).text = airTime
        findViewById<TextView>(R.id.expectedLaunchAngleValue).text = expectedLaunchAngle
    }
}