package com.sol.dopaminetrap.ml

import android.content.Context
import android.util.Log
import com.sol.dopaminetrap.analysis.KeywordMatcher
import com.sol.dopaminetrap.data.ContentCategory

/**
 * Punct unic de acces pentru clasificare text.
 *
 * Dacă assets/model.tflite există → folosește TFLiteClassifier.
 * Altfel → fallback la KeywordMatcher (faza 1).
 *
 * Tranziția la model e automată: bagi fișierele în assets/ și rebuild.
 */
object ModelManager {

    private const val TAG = "ModelManager"
    private const val MODEL_FILE = "model.tflite"

    @Volatile private var classifier: TFLiteClassifier? = null
    @Volatile private var initialized = false

    fun classify(context: Context, text: String): List<ContentCategory> {
        if (!initialized) init(context)
        return classifier?.classify(text) ?: KeywordMatcher.analyze(text)
    }

    fun isUsingAiModel(context: Context): Boolean {
        if (!initialized) init(context)
        return classifier != null
    }

    private fun init(context: Context) {
        initialized = true
        val hasModel = context.assets.list("")?.contains(MODEL_FILE) == true
        if (hasModel) {
            runCatching {
                classifier = TFLiteClassifier(context)
                Log.d(TAG, "Mod AI activ — TFLite model încărcat")
            }.onFailure {
                Log.e(TAG, "Eroare la încărcarea modelului, fallback la keywords: ${it.message}")
                classifier = null
            }
        } else {
            Log.d(TAG, "model.tflite absent — mod keyword matching activ")
        }
    }
}
