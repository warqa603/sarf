package com.cash.guide.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.cash.guide.R
import com.cash.guide.data.db.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NoteShareHelper {

    fun shareAsImage(
        context: Context,
        note: NoteEntity,
        isRtl: Boolean,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        coroutineScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    renderNoteBitmap(context, note, isRtl)
                }
                val uri = withContext(Dispatchers.IO) {
                    saveBitmapToCache(context, bitmap, note.id)
                }
                launchImageShareIntent(context, uri, note.title.ifBlank { "Note" })
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, R.string.note_share_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareAsText(context: Context, note: NoteEntity) {
        val titleText = note.title.trim()
        val rawBodyText = note.content.trim()
        
        // Clean highlight delimiters "==p:text==" -> "text"
        val hlRegex = Regex("==((?:[pgboy]:)?)(.*?)==", RegexOption.DOT_MATCHES_ALL)
        val bodyText = hlRegex.replace(rawBodyText) { match -> match.groupValues[2] }

        val textToShare = buildString {
            if (titleText.isNotBlank()) {
                append("📌 ").append(titleText).append("\n\n")
            }
            append(bodyText)
            append("\n\n───\nWarqa • ورقة")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textToShare)
            putExtra(Intent.EXTRA_SUBJECT, titleText.ifBlank { "Note" })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Partager la note via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun isArabicScript(text: String): Boolean {
        return text.any { c ->
            c in '\u0600'..'\u06FF' ||
            c in '\u0750'..'\u077F' ||
            c in '\u08A0'..'\u08FF' ||
            c in '\uFB50'..'\uFDFF' ||
            c in '\uFE70'..'\uFEFF'
        }
    }

    private fun renderNoteBitmap(
        context: Context,
        note: NoteEntity,
        systemIsRtl: Boolean
    ): Bitmap {
        val width = 1080
        val lineSpacing = 72f
        val ruleYStart = 430f

        val tajawalBold = ResourcesCompat.getFont(context, R.font.tajawal_bold) ?: Typeface.DEFAULT_BOLD
        val tajawalMedium = ResourcesCompat.getFont(context, R.font.tajawal_medium) ?: Typeface.DEFAULT
        val patrickHand = ResourcesCompat.getFont(context, R.font.patrick_hand_regular) ?: Typeface.create("sans-serif-casual", Typeface.NORMAL)

        val noteText = note.content
        val hasArabic = isArabicScript(note.title) || isArabicScript(noteText)
        val contentIsRtl = if (note.title.isNotBlank() || noteText.isNotBlank()) hasArabic else systemIsRtl

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1E21")
            textSize = 40f
            typeface = if (hasArabic) tajawalMedium else patrickHand
        }

        // Break content into lines fitting between margins
        val marginLeft = 100f
        val marginRight = 100f
        val maxTextWidth = width - marginLeft - marginRight

        val rawLines = noteText.split("\n")
        val wrappedLines = mutableListOf<String>()
        for (raw in rawLines) {
            if (raw.isEmpty()) {
                wrappedLines.add("")
                continue
            }
            var start = 0
            while (start < raw.length) {
                val count = bodyPaint.breakText(raw, start, raw.length, true, maxTextWidth, null)
                wrappedLines.add(raw.substring(start, start + count))
                start += count
            }
        }

        val totalContentLines = wrappedLines.size.coerceAtLeast(8)
        val contentHeightNeeded = ruleYStart + (totalContentLines + 6) * lineSpacing + 220f
        val minHeight = 2280
        val height = contentHeightNeeded.toInt().coerceAtLeast(minHeight)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Paper background
        canvas.drawColor(Color.parseColor("#FFFDF8"))

        // Ruled lines
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#90CAF9")
            alpha = 95
            strokeWidth = 2.2f
        }

        var currentLineY = ruleYStart
        while (currentLineY < height - 60f) {
            canvas.drawLine(0f, currentLineY, width.toFloat(), currentLineY, rulePaint)
            currentLineY += lineSpacing
        }

        // Margin line
        val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF9A9A")
            alpha = 110
            strokeWidth = 2.5f
        }
        val marginX = if (contentIsRtl) width - 85f else 85f
        canvas.drawLine(marginX, 0f, marginX, height.toFloat(), marginPaint)

        // Header Top Bar
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FCE4EC")
        }
        val pillRect = RectF(width / 2f - 240f, 65f, width / 2f + 240f, 155f)
        canvas.drawRoundRect(pillRect, 45f, 45f, pillPaint)

        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F48FB1")
            alpha = 140
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(pillRect, 45f, 45f, pillBorderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1E21")
            textSize = 44f
            typeface = if (contentIsRtl) tajawalBold else patrickHand
            textAlign = Paint.Align.CENTER
        }
        val displayTitle = note.title.ifBlank { if (contentIsRtl) "ملاحظة" else "Note" }
        canvas.drawText(displayTitle, width / 2f, 126f, titlePaint)

        // Date subtitle
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 30f
            typeface = if (contentIsRtl) tajawalMedium else patrickHand
            textAlign = Paint.Align.CENTER
        }
        val dateFormatter = SimpleDateFormat("EEEE d MMMM yyyy • HH:mm", if (contentIsRtl) Locale.forLanguageTag("ar-MA") else Locale.FRENCH)
        val dateStr = dateFormatter.format(Date(note.createdAtEpochMs))
        canvas.drawText(dateStr, width / 2f, 205f, datePaint)

        // Divider
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#90CAF9")
            alpha = 130
            strokeWidth = 2.5f
        }
        canvas.drawLine(50f, 245f, width - 50f, 245f, dividerPaint)

        // Highlighter paints for notebook sharing
        val defaultHlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#90FFF176"); style = Paint.Style.FILL }
        val hlColorMap = mapOf(
            "p" to Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#90F3A7B9"); style = Paint.Style.FILL },
            "g" to Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#90C9DDA0"); style = Paint.Style.FILL },
            "b" to Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#90A8CFE3"); style = Paint.Style.FILL },
            "o" to Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#90FFCC80"); style = Paint.Style.FILL },
            "y" to defaultHlPaint
        )
        val hlRegex = Regex("==((?:[pgboy]:)?)(.*?)==")

        // Render body lines on notebook rules
        var lineY = ruleYStart
        bodyPaint.textAlign = if (contentIsRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val textX = if (contentIsRtl) width - marginRight else marginLeft

        for (line in wrappedLines) {
            if (line.isNotEmpty()) {
                if (line.contains("==")) {
                    // Extract highlight spans and compute clean text
                    var cleanText = ""
                    var lastIndex = 0
                    val spans = mutableListOf<Triple<Int, Int, Paint>>() // start in cleanText, end in cleanText, paint

                    for (m in hlRegex.findAll(line)) {
                        cleanText += line.substring(lastIndex, m.range.first)
                        val colorTag = m.groupValues[1].removeSuffix(":").lowercase()
                        val hlContent = m.groupValues[2]
                        val startInClean = cleanText.length
                        cleanText += hlContent
                        val endInClean = cleanText.length
                        val paint = hlColorMap[colorTag] ?: defaultHlPaint
                        spans.add(Triple(startInClean, endInClean, paint))
                        lastIndex = m.range.last + 1
                    }
                    cleanText += line.substring(lastIndex)

                    // Draw highlighter washes behind spans
                    for ((s, e, paint) in spans) {
                        val beforeText = cleanText.substring(0, s)
                        val spanText = cleanText.substring(s, e)
                        val beforeW = bodyPaint.measureText(beforeText)
                        val spanW = bodyPaint.measureText(spanText)

                        val left = if (contentIsRtl) textX - beforeW - spanW - 4f else textX + beforeW - 4f
                        val right = if (contentIsRtl) textX - beforeW + 4f else textX + beforeW + spanW + 4f
                        val top = lineY - 42f
                        val bottom = lineY - 8f
                        canvas.drawRoundRect(RectF(left, top, right, bottom), 8f, 8f, paint)
                    }

                    canvas.drawText(cleanText, textX, lineY - 14f, bodyPaint)
                } else {
                    canvas.drawText(line, textX, lineY - 14f, bodyPaint)
                }
            }
            lineY += lineSpacing
        }

        // Watermark at bottom
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 28f
            typeface = if (contentIsRtl) tajawalMedium else patrickHand
            textAlign = Paint.Align.CENTER
        }
        val watermarkText = if (contentIsRtl) "Warqa • ورقة وستيلو فـ جيبك" else "Warqa • Carnet de Notes"
        canvas.drawText(watermarkText, width / 2f, height - 70f, watermarkPaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, noteId: String): Uri {
        val cachePath = File(context.cacheDir, "shared_notes").apply { mkdirs() }
        val file = File(cachePath, "note_${noteId}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun launchImageShareIntent(context: Context, uri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            this.type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Partager la note via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
