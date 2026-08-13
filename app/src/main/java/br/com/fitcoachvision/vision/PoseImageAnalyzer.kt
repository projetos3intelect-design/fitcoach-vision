package br.com.fitcoachvision.vision

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Ponte entre o CameraX e o MediaPipe.
 *
 * Roda numa thread dedicada. Com STRATEGY_KEEP_ONLY_LATEST, frames que chegam
 * enquanto este metodo executa sao descartados pelo proprio CameraX — o que e
 * exatamente o comportamento desejado. Um pipeline que enfileira produz feedback
 * de voz atrasado, o pior defeito possivel neste produto.
 *
 * Nao espelha a imagem da camera frontal de proposito: espelhar trocaria o
 * significado anatomico de esquerda e direita nos landmarks. O espelhamento
 * acontece apenas no desenho do overlay.
 */
class PoseImageAnalyzer(
    private val source: PoseLandmarkerSource,
    private val stats: PipelineStats
) : ImageAnalysis.Analyzer {

    private var lastTimestampMs = 0L

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val upright = if (rotationDegrees == 0) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            // O MediaPipe em LIVE_STREAM exige timestamps estritamente crescentes.
            val now = SystemClock.uptimeMillis()
            val timestamp = if (now > lastTimestampMs) now else lastTimestampMs + 1
            lastTimestampMs = timestamp

            stats.recordSubmitted()
            source.detect(upright, timestamp)
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao processar frame: ${t.message}")
        } finally {
            imageProxy.close()
        }
    }

    private companion object {
        const val TAG = "PoseImageAnalyzer"
    }
}
