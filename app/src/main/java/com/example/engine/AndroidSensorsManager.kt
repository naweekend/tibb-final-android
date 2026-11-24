package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.core.content.FileProvider
import com.example.data.model.GeoCoords
import com.example.data.model.PatientProfile
import com.example.data.model.SavedPersonReport
import com.example.data.model.StoredAlarm
import com.example.data.model.StoredEpisode
import com.example.data.model.StoredLabRecord
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

object AndroidSensorsManager {

    /**
     * Generates a crisp high-contrast QR bitmap completely offline via ZXing.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 420): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        return bmp
    }

    /**
     * Extracts mean R/G/B values from the central ROI of a YUV_420_888 CameraX ImageProxy.
     */
    fun extractCenterRgbFromYuv(image: ImageProxy): Triple<Double, Double, Double> {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val width = image.width
        val height = image.height
        val startX = width / 4
        val endX = (width * 3) / 4
        val startY = height / 4
        val endY = (height * 3) / 4
        val step = 6

        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        var row = startY
        while (row < endY) {
            var col = startX
            while (col < endX) {
                val yIdx = row * yRowStride + col
                val uvIdx = (row / 2) * uvRowStride + (col / 2) * uvPixelStride
                if (yIdx < yBuf.limit() && uvIdx < uBuf.limit() && uvIdx < vBuf.limit()) {
                    val yVal = (yBuf.get(yIdx).toInt() and 0xFF).toDouble()
                    val uVal = (uBuf.get(uvIdx).toInt() and 0xFF) - 128.0
                    val vVal = (vBuf.get(uvIdx).toInt() and 0xFF) - 128.0
                    val r = (yVal + 1.402 * vVal).coerceIn(0.0, 255.0)
                    val g = (yVal - 0.344136 * uVal - 0.714136 * vVal).coerceIn(0.0, 255.0)
                    val b = (yVal + 1.772 * uVal).coerceIn(0.0, 255.0)
                    sumR += r
                    sumG += g
                    sumB += b
                    count++
                }
                col += step
            }
            row += step
        }
        if (count == 0) return Triple(0.0, 0.0, 0.0)
        return Triple(sumR / count, sumG / count, sumB / count)
    }

    /**
     * Re-encodes any picked/captured image to a clean resized JPEG in private storage, stripping EXIF GPS metadata.
     */
    suspend fun stripExifAndSaveToVault(
        context: Context,
        accountId: String,
        sourceUri: Uri,
        maxDimPx: Int = 1024,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: error("Unable to open image")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            var sampleSize = 1
            while (max(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimPx * 2) {
                sampleSize *= 2
            }
            val decoded = BitmapFactory.decodeByteArray(
                rawBytes,
                0,
                rawBytes.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            ) ?: error("Invalid image file")

            val scale = min(1f, maxDimPx.toFloat() / max(decoded.width, decoded.height).toFloat())
            val finalBmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else decoded

            val dir = File(context.filesDir, "tibb_media/$accountId").apply { mkdirs() }
            val outFile = File(dir, "img_${UUID.randomUUID().toString().take(10)}.jpg")
            FileOutputStream(outFile).use { fos ->
                finalBmp.compress(Bitmap.CompressFormat.JPEG, 82, fos)
            }
            if (finalBmp !== decoded) decoded.recycle()
            outFile.absolutePath
        }
    }

    suspend fun saveBitmapStrippedToVault(
        context: Context,
        accountId: String,
        bitmap: Bitmap,
        maxDimPx: Int = 960,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val scale = min(1f, maxDimPx.toFloat() / max(bitmap.width, bitmap.height).toFloat())
            val finalBmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else bitmap
            val dir = File(context.filesDir, "tibb_media/$accountId").apply { mkdirs() }
            val outFile = File(dir, "cam_${UUID.randomUUID().toString().take(10)}.jpg")
            FileOutputStream(outFile).use { fos ->
                finalBmp.compress(Bitmap.CompressFormat.JPEG, 82, fos)
            }
            outFile.absolutePath
        }
    }

    /**
     * Runs bundled ML Kit OCR completely offline on an image URI or PDF document URI (renders up to 2 pages).
     */
    suspend fun runOfflineLabOcr(context: Context, uri: Uri, isPdf: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                if (!isPdf) {
                    val image = InputImage.fromFilePath(context, uri)
                    suspendCancellableCoroutine { cont ->
                        recognizer.process(image)
                            .addOnSuccessListener { result -> cont.resume(result.text) }
                            .addOnFailureListener { e -> cont.resumeWith(Result.failure(e)) }
                    }
                } else {
                    val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: error("Unable to open PDF")
                    pfd.use {
                        PdfRenderer(it).use { renderer ->
                            val pages = min(2, renderer.pageCount)
                            val sb = StringBuilder()
                            for (i in 0 until pages) {
                                renderer.openPage(i).use { page ->
                                    val bmp = Bitmap.createBitmap(
                                        (page.width * 2).coerceAtMost(1800),
                                        (page.height * 2).coerceAtMost(2400),
                                        Bitmap.Config.ARGB_8888,
                                    )
                                    val canvas = Canvas(bmp)
                                    canvas.drawColor(AndroidColor.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    val inputImg = InputImage.fromBitmap(bmp, 0)
                                    val txt = suspendCancellableCoroutine { cont ->
                                        recognizer.process(inputImg)
                                            .addOnSuccessListener { r -> cont.resume(r.text) }
                                            .addOnFailureListener { e -> cont.resumeWith(Result.failure(e)) }
                                    }
                                    bmp.recycle()
                                    sb.appendLine(txt)
                                }
                            }
                            sb.toString()
                        }
                    }
                }
            }
        }

    /**
     * Scans QR referral token from a bitmap offline using bundled ML Kit Barcode Scanner.
     */
    suspend fun scanQrFromBitmap(bitmap: Bitmap): String? = suspendCancellableCoroutine { cont ->
        val scanner = BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
        scanner.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { codes ->
                cont.resume(codes.firstOrNull()?.rawValue)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * Explicit GPS lookup when user presses "Use Current Location".
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): GeoCoords? =
        suspendCancellableCoroutine { cont ->
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(GeoCoords(loc.latitude, loc.longitude))
                    } else {
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val fallback = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        cont.resume(fallback?.let { GeoCoords(it.latitude, it.longitude) })
                    }
                }
                .addOnFailureListener {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val fallback = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    cont.resume(fallback?.let { GeoCoords(it.latitude, it.longitude) })
                }
        }

    /**
     * Registers real Accelerometer step listener (no dangerous permission needed) using HealthMath bandpass peak/valley detector.
     */
    fun registerAccelerometerStepDetector(
        context: Context,
        sensitive: Boolean,
        onStepDetected: () -> Unit,
    ): () -> Unit {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return {}
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return {}
        val state = HealthMath.StepDetectorState()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val nowMs = System.currentTimeMillis()
                val detected = HealthMath.processMotionSample(
                    state = state,
                    x = event.values[0].toDouble(),
                    y = event.values[1].toDouble(),
                    z = event.values[2].toDouble(),
                    nowMs = nowMs,
                    sensitive = sensitive,
                )
                if (detected) onStepDetected()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        return { sm.unregisterListener(listener) }
    }

    /**
     * Generates a native Android PDF file for either Weekly Family Digest or Active Person Summary and shares it via FileProvider.
     */
    fun exportAndSharePdfReport(
        context: Context,
        title: String,
        lines: List<String>,
        fileNamePrefix: String,
    ): Result<Uri> = runCatching {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val titlePaint = Paint().apply {
            color = AndroidColor.rgb(17, 37, 32)
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = AndroidColor.rgb(34, 48, 43)
            textSize = 11f
        }
        val mutedPaint = Paint().apply {
            color = AndroidColor.rgb(90, 110, 102)
            textSize = 9.5f
        }

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas
        var y = margin + 12f
        canvas.drawText(title, margin, y, titlePaint)
        y += 18f
        canvas.drawText("Generated offline by Tibb · Pakistan Family & BHU Vault", margin, y, mutedPaint)
        y += 22f

        for (rawLine in lines) {
            val wrapped = wrapText(rawLine, 82)
            for (w in wrapped) {
                if (y > pageHeight - margin) {
                    pdf.finishPage(page)
                    pageNum++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = margin + 12f
                }
                canvas.drawText(w, margin, y, bodyPaint)
                y += 16f
            }
            y += 4f
        }
        pdf.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        uri
    }

    fun shareTextOrBackupFile(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String = "application/json",
        chooserTitle: String = "Export Tibb Backup",
    ): Result<Uri> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        uri
    }

    fun buildDigestLines(
        profiles: List<PatientProfile>,
        episodes: List<StoredEpisode>,
        labs: List<StoredLabRecord>,
        alarms: List<StoredAlarm>,
    ): List<String> = buildList {
        add("=== Household Members (${profiles.size}) ===")
        profiles.forEach { p ->
            add("• ${p.name} (${p.relationship.labelEn}, ${p.bloodGroup.label}) | Chronic: ${p.chronicConditions.joinToString().ifEmpty { "None" }}")
        }
        add("")
        add("=== Recent Triage Episodes (${episodes.size}) ===")
        episodes.take(10).forEach { e ->
            add("• [${e.triageTier}] ${e.patientName} — ${e.symptomCategory} | ${e.referralToken} (${e.createdAtIso.take(10)})")
        }
        add("")
        add("=== Lab Records (${labs.size}) ===")
        labs.take(8).forEach { l ->
            add("• ${l.scannedAtIso.take(10)} — Hb: ${l.hemoglobin ?: "--"}, WBC: ${l.wbc ?: "--"}, PLT: ${l.platelets ?: "--"}, Sugar: ${l.fastingGlucose ?: "--"}, HbA1c: ${l.hba1c ?: "--"}")
        }
        add("")
        add("=== Medicine & Checkup Alarms (${alarms.size}) ===")
        alarms.forEach { a ->
            add("• ${a.patientName}: ${a.medicineLabel} at ${HealthMath.formatTime12h(a.time24h)} (${a.frequency}) — Taken ${a.takenCount}x")
        }
    }

    fun buildPersonSummaryLines(
        person: PatientProfile,
        episodes: List<StoredEpisode>,
        labs: List<StoredLabRecord>,
        alarms: List<StoredAlarm>,
        reports: List<SavedPersonReport>,
    ): List<String> = buildList {
        add("Patient: ${person.name} (${person.relationship.labelEn}) | Blood: ${person.bloodGroup.label}")
        val ageStr = if (person.ageMonths < 24) "${person.ageMonths} months" else "${person.ageMonths / 12} years"
        add("Age: $ageStr | Sex: ${person.sex} | Allergies: ${person.allergies.joinToString().ifEmpty { "None" }}")
        add("Chronic Conditions: ${person.chronicConditions.joinToString().ifEmpty { "None" }}")
        if (person.dailyMedicinesNote.isNotBlank()) add("Daily Medicines: ${person.dailyMedicinesNote}")
        if (person.priorLabNotes.isNotBlank()) add("Prior Lab Notes: ${person.priorLabNotes}")
        if (person.emergencyContact.isNotBlank()) add("Emergency Contact: ${person.emergencyContact}")
        add("")
        add("--- Saved Reports & Logs (${reports.size}) ---")
        reports.take(10).forEach { r ->
            add("• [${r.kind}] ${r.title} (${r.createdAtIso.take(10)}): ${r.notes}")
        }
        add("")
        add("--- Triage Episodes (${episodes.size}) ---")
        episodes.take(8).forEach { e ->
            add("• ${e.createdAtIso.take(10)} [${e.triageTier}] ${e.symptomCategory} — ${e.referralToken}")
        }
        add("")
        add("--- Lab Scans (${labs.size}) & Alarms (${alarms.size}) ---")
        labs.take(6).forEach { l ->
            add("• ${l.scannedAtIso.take(10)} Hb:${l.hemoglobin ?: "--"} WBC:${l.wbc ?: "--"} Sugar:${l.fastingGlucose ?: "--"}")
        }
        alarms.forEach { a ->
            add("• Alarm: ${a.medicineLabel} @ ${HealthMath.formatTime12h(a.time24h)} (${a.takenCount} taken)")
        }
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val out = mutableListOf<String>()
        var remaining = text
        while (remaining.length > maxChars) {
            val cut = remaining.lastIndexOf(' ', maxChars).let { if (it <= 0) maxChars else it }
            out.add(remaining.substring(0, cut))
            remaining = remaining.substring(cut).trimStart()
        }
        if (remaining.isNotEmpty()) out.add(remaining)
        return out
    }
}
