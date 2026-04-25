package com.sol.dopaminetrap.ml

import android.content.Context
import android.util.Log
import com.sol.dopaminetrap.data.ContentCategory
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.Normalizer

/**
 * Rulează inferența TFLite pe text.
 *
 * Necesită în assets/:
 *   - model.tflite  — modelul antrenat
 *   - vocab.json    — {"token": index, ...}
 *   - labels.json   — ["SPORT_MISCARE", "DANS", ...]  (ordinea ieșirilor din model)
 *
 * Input model:  [1, MAX_LEN] float32  (indecși token)
 * Output model: [1, N_LABELS] float32 (scoruri sigmoid per categorie)
 */
class TFLiteClassifier(context: Context) {

    companion object {
        private const val TAG = "TFLiteClassifier"
        private const val MAX_LEN = 64
        private const val THRESHOLD = 0.45f

        private val LABEL_ALIASES = mapOf(
            "GROOMING" to ContentCategory.RISC_GROOMING,
            "CONTINUT_SEXUAL" to ContentCategory.CONTINUT_ADULT,
            "VIOLENTA_EXTREMA" to ContentCategory.VIOLENTA_EXTREMA,
        )
    }

    private val interpreter: Interpreter
    private val vocab: Map<String, Int>
    private val labels: List<ContentCategory?>  // null = label necunoscut, indexul trebuie păstrat

    init {
        interpreter = Interpreter(loadModelFile(context))
        vocab = loadVocab(context)
        labels = loadLabels(context)
        Log.d(TAG, "Model încărcat — ${labels.filterNotNull().size} categorii, vocab ${vocab.size} tokeni")
    }

    fun classify(text: String): List<ContentCategory> {
        val input = tokenize(text)
        val outputSize = interpreter.getOutputTensor(0).shape()[1]
        val output = Array(1) { FloatArray(outputSize) }
        interpreter.run(input, output)
        return labels.mapIndexedNotNull { i, cat ->
            if (cat != null && i < outputSize && output[0][i] >= THRESHOLD) cat else null
        }
    }

    fun close() = interpreter.close()

    // ─── Tokenizare ───────────────────────────────────────────────────────────

    private fun normalizeDiacritics(text: String): String {
        // NFD descompune caracterele (ă → a + combining breve), regex sterge diacriticele combinate
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun tokenize(text: String): Array<FloatArray> {
        val tokens = normalizeDiacritics(text).lowercase()
            .split(Regex("[\\s,!?.;:()\"/\\\\]+"))
            .filter { it.isNotBlank() }
            .take(MAX_LEN)
            .map { (vocab[it] ?: vocab["<OOV>"] ?: 1).toFloat() }

        val input = FloatArray(MAX_LEN) { if (it < tokens.size) tokens[it] else 0f }
        return arrayOf(input)
    }

    // ─── Încărcare assets ─────────────────────────────────────────────────────

    private fun loadModelFile(context: Context): ByteBuffer {
        val fd = context.assets.openFd("model.tflite")
        val stream = FileInputStream(fd.fileDescriptor)
        val channel = stream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            .also { it.order(ByteOrder.nativeOrder()) }
    }

    private fun loadVocab(context: Context): Map<String, Int> {
        val json = context.assets.open("vocab.json").bufferedReader().readText()
        val obj = JSONObject(json)
        return obj.keys().asSequence().associateWith { obj.getInt(it) }
    }

    private fun loadLabels(context: Context): List<ContentCategory?> {
        val json = context.assets.open("labels.json").bufferedReader().readText()
        val arr = org.json.JSONArray(json)
        return (0 until arr.length()).map { i ->
            val name = arr.getString(i)
            LABEL_ALIASES[name] ?: runCatching { ContentCategory.valueOf(name) }.getOrNull()
        }
    }
}
