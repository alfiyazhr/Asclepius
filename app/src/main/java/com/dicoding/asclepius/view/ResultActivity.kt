package com.dicoding.asclepius.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResultBinding
import java.text.NumberFormat

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        displayResult()
    }

    private fun displayResult() {
        val result = intent.getStringExtra(EXTRA_RESULT)
        val confidenceScore = intent.getFloatExtra(EXTRA_CONFIDENCE_SCORE, 0.0f)
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)

        if (result != null && imageUriString != null) {
            binding.resultText.text = result
            val confidencePercentage = NumberFormat.getPercentInstance().format(confidenceScore)
            binding.confidenceText.text = confidencePercentage

            val imageUri = Uri.parse(imageUriString)
            binding.resultImage.setImageURI(imageUri)
            Log.d(TAG, "Displaying result: $result, Confidence: $confidencePercentage, Image: $imageUri")

            val additionalInfo = when (result.equals("Cancer", ignoreCase = true)) {
                true -> "Terdeteksi potensi kanker. Segera konsultasikan dengan dokter untuk diagnosis dan penanganan lebih lanjut."
                false -> "Tidak terdeteksi potensi kanker. Tetap jaga kesehatan kulit Anda dan lakukan pemeriksaan rutin."
            }
            binding.additionalInfoText.text = additionalInfo

        } else {
            showToast(getString(R.string.no_result_available))
            Log.e(TAG, "Result data is incomplete: Result=$result, ImageURI=$imageUriString")
            binding.resultText.text = getString(R.string.no_result_available)
            binding.confidenceText.text = "-"
            binding.resultImage.setImageResource(R.drawable.ic_image_placeholder)
            binding.additionalInfoText.text = getString(R.string.analysis_error)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
        const val EXTRA_CONFIDENCE_SCORE = "extra_confidence_score"
        private const val TAG = "ResultActivity"
    }
}
