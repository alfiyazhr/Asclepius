package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.dicoding.asclepius.ml.CancerClassification
import org.tensorflow.lite.support.image.TensorImage
import java.io.Closeable
import java.io.IOException

class ImageClassifierHelper(
    private val context: Context,
) : Closeable {

    private var model: CancerClassification? = null
    private var isModelSetup = false

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        try {
            if (model == null) {
                model = CancerClassification.newInstance(context)
                isModelSetup = true
                Log.d(TAG, "TFLite model loaded successfully.")
            }
        } catch (e: Exception) {
            isModelSetup = false
            Log.e(TAG, "Error setting up TFLite model: ${e.message}", e)
        }
    }

    fun classifyStaticImage(imageUri: Uri, onSuccess: (String, Float) -> Unit, onError: (String) -> Unit) {
        if (!isModelSetup || model == null) {
            onError("Image classifier is not initialized or failed to initialize.")
            if (!isModelSetup) {
                setupImageClassifier()
                if (!isModelSetup) return
            }
        }

        try {
            val bitmap: Bitmap = getBitmapFromUri(imageUri) ?: run {
                onError("Failed to get bitmap from URI. The image might be corrupted or inaccessible.")
                return
            }

            val tensorImage = TensorImage.fromBitmap(bitmap)
            val outputs = model!!.process(tensorImage)
            val probability = outputs.probabilityAsCategoryList

            val maxCategory = probability.maxByOrNull { it.score }
            val result = maxCategory?.label ?: "Unknown"
            val confidence = maxCategory?.score ?: 0f

            Log.d(TAG, "Classification result: $result, Confidence: $confidence")
            onSuccess(result, confidence)

        } catch (e: Exception) {
            Log.e(TAG, "Error during image classification: ${e.message}", e)
            onError("Error during image classification: ${e.message}")
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error decoding bitmap from URI: ${e.message}", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception, cannot access URI: ${e.message}", e)
            null
        }
    }

    override fun close() {
        try {
            model?.close()
            model = null
            isModelSetup = false
            Log.d(TAG, "TFLite model closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TFLite model: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}
