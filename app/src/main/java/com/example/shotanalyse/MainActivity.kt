// MainActivity.kt
import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.basketballshotdetector.R

class MainActivity : AppCompatActivity() {

    private lateinit var uploadButton: Button
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var uploadRecordedButton: Button
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uploadButton = findViewById(R.id.uploadButton)
        startRecordingButton = findViewById(R.id.startRecordingButton)
        stopRecordingButton = findViewById(R.id.stopRecordingButton)
        uploadRecordedButton = findViewById(R.id.uploadRecordedButton)
        imageView = findViewById(R.id.imageView)

        uploadButton.setOnClickListener {
            // Open gallery to pick an image
            openGallery()
        }

        startRecordingButton.setOnClickListener {
            // Start video recording logic
            startRecording()
        }

        stopRecordingButton.setOnClickListener {
            // Stop video recording logic
            stopRecording()
        }

        uploadRecordedButton.setOnClickListener {
            // Logic for uploading the recorded video
            uploadRecordedVideo()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val selectedImageUri: Uri? = result.data?.data
            imageView.setImageURI(selectedImageUri)
        }
    }

    private fun startRecording() {
        // Start recording implementation
    }

    private fun stopRecording() {
        // Stop recording implementation
    }

    private fun uploadRecordedVideo() {
        // Upload recorded video implementation
    }
}