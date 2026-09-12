package com.jxdiem.diemgeo.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.jxdiem.diemgeo.db.AiLabel
import kotlinx.coroutines.tasks.await

/**
 * On-device, offline image labeling (spec point 8). Uses ML Kit's bundled
 * base model, so no network call and no API key are involved — trades
 * generic labels (e.g. "Tree", "Building", "Road") for full offline
 * operation and zero cost, which is the point of this classification.
 */
object PhotoLabeler {

    private const val MIN_CONFIDENCE = 0.6f

    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(MIN_CONFIDENCE)
                .build()
        )
    }

    suspend fun label(bitmap: Bitmap): List<AiLabel> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val results = labeler.process(image).await()
        return results.map { AiLabel(text = it.text, confidence = it.confidence) }
            .sortedByDescending { it.confidence }
    }
}
