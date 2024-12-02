package com.example.shotanalyse

//import VideoProcessor
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.shotanalyse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var selectVideoButton: Button
    private lateinit var startRecordingButton: Button
    private lateinit var stopRecordingButton: Button
    private lateinit var uploadButton: Button
    private lateinit var imageView: ImageView

    private var videoUri: Uri? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectVideoButton = findViewById(R.id.selectVideoButton)
        startRecordingButton = findViewById(R.id.startRecordingButton)
        stopRecordingButton = findViewById(R.id.stopRecordingButton)
        uploadButton = findViewById(R.id.uploadButton)
        imageView = findViewById(R.id.videoPlaceholder)

        checkPermissions()

        selectVideoButton.setOnClickListener {
            openGalleryForVideo()
        }

        startRecordingButton.setOnClickListener {
            startRecording()
        }

        stopRecordingButton.setOnClickListener {
            stopRecording()
        }

        uploadButton.setOnClickListener {
            videoUri?.let {
                uploadVideo(it)
            } ?: Toast.makeText(this, "No video selected or recorded.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun openGalleryForVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        galleryVideoLauncher.launch(intent)
    }

    private val galleryVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            videoUri = result.data?.data
            setVideoThumbnail(videoUri)
            Toast.makeText(this, "Video selected: $videoUri", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startRecording() {
        if (!isRecording) {
            val videoFile = File(getExternalFilesDir(null), "recorded_video.mp4")
            videoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", videoFile)
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            videoRecordingLauncher.launch(intent)
            isRecording = true
            Toast.makeText(this, "Recording started.", Toast.LENGTH_SHORT).show()
        }
    }

    private val videoRecordingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Recording saved: $videoUri", Toast.LENGTH_SHORT).show()
            setVideoThumbnail(videoUri)
            isRecording = false
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            isRecording = false
            Toast.makeText(this, "Recording stopped.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setVideoThumbnail(uri: Uri?) {
        uri?.let {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri) // Set the video Uri as data source
                val thumbnail = retriever.getFrameAtTime(0) // Get the first frame
                if (thumbnail != null) {
                    imageView.setImageBitmap(thumbnail) // Set the thumbnail in the ImageView
                } else {
                    Toast.makeText(this, "Unable to retrieve thumbnail.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error retrieving thumbnail.", Toast.LENGTH_SHORT).show()
            } finally {
                retriever.release() // Always release the retriever to free resources
            }
        }
    }

    private fun initializeVideoProcessor(context: Context): VideoProcessor {
        // Get a singleton instance of ModelManager
        val modelManager = ModelManager.getInstance(context)

        // Create and return a VideoProcessor instance using ModelManager's interpreters
        return VideoProcessor(
            context,
            modelManager.model1Interpreter,

        )
    }


    private fun uploadVideo(uri: Uri) {
        val videoProcessor = initializeVideoProcessor(this)

        CoroutineScope(Dispatchers.Default).launch {
            videoProcessor.processVideoFrames(uri)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
            Toast.makeText(this, "Permissions are required for the app to function.", Toast.LENGTH_SHORT).show()
        }
    }


}


