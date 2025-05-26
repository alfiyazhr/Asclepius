package com.dicoding.asclepius.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageClassifierHelper: ImageClassifierHelper

    private var currentImageUri: Uri? = null
    private var uCropResultUri: Uri? = null

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            launchUCrop(uri)
        } else {
            Log.d(TAG, "No media selected from gallery")
            showToast(getString(R.string.no_image_selected))
        }
    }

    private val launcherCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        if (isSuccess) {
            currentImageUri?.let { uri ->
                launchUCrop(uri)
            } ?: run {
                Log.e(TAG, "currentImageUri is null after taking picture")
                showToast(getString(R.string.failed_to_capture_image))
            }
        } else {
            Log.d(TAG, "Failed to capture image from camera")
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            showToast(getString(R.string.permission_denied_camera))
        }
    }


    private val uCropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            if (resultUri != null) {
                uCropResultUri = resultUri
                currentImageUri = resultUri
                showImage(resultUri)
                Log.d(TAG, "Image cropped successfully with UCrop: $resultUri")
            } else {
                showToast(getString(R.string.image_crop_failed))
                Log.e(TAG, "UCrop output URI is null")
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = result.data?.let { UCrop.getError(it) }
            val errorMessage = cropError?.message ?: getString(R.string.image_crop_failed)
            showToast("Error saat cropping: $errorMessage")
            Log.e(TAG, "UCrop cropping error: ", cropError)
        } else {
            Log.d(TAG, "UCrop Canceled or other error. Result code: ${result.resultCode}")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        imageClassifierHelper = ImageClassifierHelper(context = this)

        binding.galleryButton.text = getString(R.string.select_image)
        binding.galleryButton.setOnClickListener {
            showImageSourceDialog()
        }

        binding.cameraButton.visibility = View.GONE

        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf<CharSequence>(
            getString(R.string.gallery),
            getString(R.string.camera)
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_image_source))
        builder.setItems(options) { dialog, item ->
            when (item) {
                0 -> openGallery()
                1 -> checkCameraPermissionAndOpenCamera()
            }
            dialog.dismiss()
        }
        builder.show()
    }

    private fun openGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        currentImageUri = createImageUri()
        currentImageUri?.let { uri ->
            launcherCamera.launch(uri)
        } ?: run {
            Log.e(TAG, "Failed to create URI for camera image")
            showToast("Gagal menyiapkan kamera.")
        }
    }

    private fun createImageUri(): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir("images")
        if (storageDir?.exists() == false) storageDir.mkdirs()
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
        return FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            imageFile
        )
    }


    private fun launchUCrop(sourceUri: Uri) {
        val destinationFileName = "ucrop_cropped_image_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))

        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false)
            setToolbarTitle("Potong Gambar")
            setStatusBarColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_primaryContainer))
            setToolbarColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_primary))
            setToolbarWidgetColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_onPrimary))
        }

        val ucropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .getIntent(this)

        uCropLauncher.launch(ucropIntent)
    }

    private fun showImage(uri: Uri) {
        binding.previewImageView.setImageURI(uri)
        binding.previewImageView.visibility = View.VISIBLE
        Log.d(TAG, "Displaying image: $uri")
    }

    private fun analyzeImage() {
        currentImageUri?.let { uri ->
            binding.progressIndicator.visibility = View.VISIBLE
            imageClassifierHelper.classifyStaticImage(uri,
                onSuccess = { result, confidence ->
                    binding.progressIndicator.visibility = View.GONE
                    moveToResult(result, confidence, uri)
                },
                onError = { errorMessage ->
                    binding.progressIndicator.visibility = View.GONE
                    showToast(errorMessage)
                    Log.e(TAG, "Classification error: $errorMessage")
                }
            )
        } ?: showToast(getString(R.string.empty_image_warning))
    }

    private fun moveToResult(result: String, confidence: Float, uri: Uri) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(ResultActivity.EXTRA_RESULT, result)
            putExtra(ResultActivity.EXTRA_CONFIDENCE_SCORE, confidence)
            putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString())
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        imageClassifierHelper.close()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
