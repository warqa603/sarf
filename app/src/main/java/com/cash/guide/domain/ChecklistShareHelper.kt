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
import com.cash.guide.data.db.ChecklistItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChecklistShareHelper {

    fun shareAsImage(
        context: Context,
        checklistId: String,
        title: String,
        items: List<ChecklistItemEntity>,
        isRtl: Boolean,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        coroutineScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    renderChecklistBitmap(context, title, items, isRtl)
                }
                val uri = withContext(Dispatchers.IO) {
                    saveBitmapToCache(context, bitmap, checklistId)
                }
                launchImageShareIntent(context, uri, title)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, R.string.share_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareAsTextAndLink(
        context: Context,
        title: String,
        items: List<ChecklistItemEntity>
    ) {
        val deepLink = ChecklistLinkHelper.createDeepLink(title, items)
        val sb = StringBuilder()

        sb.append("📋 *Checklist: ${title.ifBlank { "Liste" }}*\n\n")

        if (items.isEmpty()) {
            sb.append("_(Aucun élément)_\n")
        } else {
            items.forEach { item ->
                val mark = if (item.isChecked) "☑" else "☐"
                sb.append("$mark ${item.text}\n")
            }
        }

        sb.append("\n🔗 افتح القائمة وتكوشيها في تطبيق ورقة:\n")
        sb.append(deepLink)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, "Partager via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareAsWhatsAppTextAndLink(
        context: Context,
        title: String,
        items: List<ChecklistItemEntity>
    ) = shareAsTextAndLink(context, title, items)

    private fun isArabicScript(text: String): Boolean {
        return text.any { c ->
            c in '\u0600'..'\u06FF' ||
            c in '\u0750'..'\u077F' ||
            c in '\u08A0'..'\u08FF' ||
            c in '\uFB50'..'\uFDFF' ||
            c in '\uFE70'..'\uFEFF'
        }
    }

    private fun truncateToWidth(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return if (truncated.isEmpty()) "" else "$truncated..."
    }

    private fun renderChecklistBitmap(
        context: Context,
        title: String,
        items: List<ChecklistItemEntity>,
        isRtl: Boolean
    ): Bitmap {
        val width = 1080
        val ruleSpacing = 80f
        val marginX = 54f

        // 1. Script & RTL detection based on actual checklist content
        val hasArabic = isArabicScript(title) || items.any { isArabicScript(it.text) }
        val hasLatin = title.any { (it in 'a'..'z') || (it in 'A'..'Z') } || items.any { it.text.any { c -> (c in 'a'..'z') || (c in 'A'..'Z') } }
        val effectiveRtl = when {
            hasArabic && !hasLatin -> true
            hasLatin && !hasArabic -> false
            hasArabic -> true
            else -> isRtl
        }

        // 2. Bundled Fonts matching the app screen
        val creamFrothFont = runCatching {
            ResourcesCompat.getFont(context, R.font.cream_froth_regular)
                ?: ResourcesCompat.getFont(context, R.font.cream_froth)
        }.getOrNull()
        val creamFrothBold = runCatching {
            ResourcesCompat.getFont(context, R.font.cream_froth_bold)
        }.getOrNull()
        val majazFont = runCatching {
            ResourcesCompat.getFont(context, R.font.majaz_regular)
        }.getOrNull() ?: Typeface.DEFAULT
        val patrickHandFont = runCatching {
            ResourcesCompat.getFont(context, R.font.patrick_hand_regular)
        }.getOrNull() ?: Typeface.DEFAULT

        val primaryFont = if (effectiveRtl) (creamFrothFont ?: majazFont) else patrickHandFont
        val boldFont = if (effectiveRtl) (creamFrothBold ?: creamFrothFont ?: majazFont) else patrickHandFont

        // 3. Colors matching JournalTheme
        val paperColor = Color.rgb(0xF8, 0xF9, 0xFA)          // Feuille Blanche (White Notebook Paper)
        val inkColor = Color.rgb(0x24, 0x24, 0x21)            // Journal Ink
        val writingInkColor = Color.rgb(0x38, 0x38, 0x34)     // Writing Ink
        val mutedInkColor = Color.rgb(0x75, 0x75, 0x70)       // Muted Ink
        val checkedTextColor = Color.argb(140, 0x75, 0x75, 0x70)
        val lineRuleColor = Color.argb(45, 0x1E, 0x5C, 0xA8)  // Ruled Notebook Blue
        val checkGreenColor = Color.rgb(0x1B, 0x7A, 0x4B)     // Emerald Ink
        val pinkPillColor = Color.argb(90, 0xF4, 0x8F, 0xB1)   // Highlighter Pink (0.35f alpha)
        val rowDotColors = listOf(
            Color.rgb(0x3B, 0x82, 0xB6),
            Color.rgb(0xD6, 0x5D, 0x82),
            Color.rgb(0x5E, 0x8C, 0x3B),
            Color.rgb(0xC7, 0x88, 0x1E),
            Color.rgb(0x7E, 0x5A, 0xA8),
            Color.rgb(0xCC, 0x67, 0x3B)
        )

        // 4. Header Dimensions (Exact replica of app screen top bar)
        val statusBarH = 74f
        val titleRowH = 106f
        val subRowH = 76f
        val headerH = statusBarH + titleRowH + subRowH

        // Full Phone Screen Dimensions (min 2280px high, expands if items exceed screen)
        val minScreenHeight = 2280f
        val contentRequiredHeight = headerH + (items.size + 4) * ruleSpacing
        val height = maxOf(minScreenHeight, contentRequiredHeight).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill paper background across the entire screen
        canvas.drawColor(paperColor)

        // 5. Draw horizontal blue ruled lines from headerBottom all the way down to bottom of screen
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineRuleColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        var curLineY = headerH + ruleSpacing
        while (curLineY < height - 30f) {
            canvas.drawLine(0f, curLineY, width.toFloat(), curLineY, linePaint)
            curLineY += ruleSpacing
        }

        // 6. Header Top Bar Elements:
        // A) Title in Watercolor Pink Pill (Centered)
        val headerTitle = if (title.isBlank()) (if (effectiveRtl) "قائمة المهام" else "Checklist") else title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldFont
            color = writingInkColor
            textSize = 46f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val pillY = statusBarH + 14f
        val pillH = 76f
        val rawTitleW = titlePaint.measureText(headerTitle).coerceAtMost(width - marginX * 2 - 60f)
        val pillW = (rawTitleW + 80f).coerceAtMost(width - marginX * 2)
        val pillRect = RectF(
            (width - pillW) / 2f,
            pillY,
            (width + pillW) / 2f,
            pillY + pillH
        )
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkPillColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, 32f, 32f, pillPaint)
        val truncatedTitle = truncateToWidth(titlePaint, headerTitle, pillW - 40f)
        canvas.drawText(truncatedTitle, width / 2f, pillY + 52f, titlePaint)

        // B) Sub-row: Progress count on one side, Date on other side
        val subRowY = statusBarH + titleRowH
        val subBaseline = subRowY + 50f
        val completedCount = items.count { it.isChecked }
        val totalCount = items.size
        val isAllDone = totalCount > 0 && completedCount == totalCount

        val countText = if (effectiveRtl) {
            if (isAllDone) "$completedCount / $totalCount مكتمل ✓"
            else "$completedCount / $totalCount مكتمل"
        } else {
            if (isAllDone) "$completedCount / $totalCount faits ✓"
            else "$completedCount / $totalCount faits"
        }

        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldFont
            textSize = 34f
            color = if (isAllDone) checkGreenColor else writingInkColor
            textAlign = if (effectiveRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }

        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateSdf.format(Date())
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = patrickHandFont
            textSize = 30f
            color = mutedInkColor
            textAlign = if (effectiveRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        if (effectiveRtl) {
            canvas.drawText(countText, width - marginX, subBaseline, countPaint)
            canvas.drawText(dateStr, marginX, subBaseline, datePaint)
        } else {
            canvas.drawText(countText, marginX, subBaseline, countPaint)
            canvas.drawText(dateStr, width - marginX, subBaseline, datePaint)
        }

        // C) Divider separating header from ruled paper
        val headerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0x1E, 0x5C, 0xA8)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, headerH, width.toFloat(), headerH, headerDividerPaint)

        // 7. Checklist Items Section sitting on the Ruled Paper
        val checkboxSize = 42f
        val checkStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val checkFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = checkGreenColor
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        val itemTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 48f
        }

        val itemNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldFont
            textSize = 44f
            isFakeBoldText = true
        }

        if (items.isEmpty()) {
            val emptyLineY = headerH + ruleSpacing
            val emptyBaseline = emptyLineY - 1f
            val emptyText = if (effectiveRtl) "(لا توجد عناصر حالياً في القائمة)" else "(Aucun élément dans la checklist)"
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = primaryFont
                color = mutedInkColor
                textSize = 36f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(emptyText, width / 2f, emptyBaseline, emptyPaint)
        } else {
            items.forEachIndexed { index, item ->
                val lineY = headerH + (index + 1) * ruleSpacing
                val baselineY = lineY - 1f
                val checkboxY = lineY - checkboxSize - 3f

                val dotColor = rowDotColors[index % rowDotColors.size]
                itemNumPaint.color = dotColor

                if (effectiveRtl) {
                    // RTL: Number on Right, Text flowing leftwards, Checkbox on Left
                    val numX = width - marginX - 10f
                    itemNumPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("${index + 1}.", numX, baselineY, itemNumPaint)

                    // Checkbox on far Left
                    val boxX = marginX + 10f
                    val rect = RectF(boxX, checkboxY, boxX + checkboxSize, checkboxY + checkboxSize)
                    canvas.drawRoundRect(rect, 8f, 8f, checkStrokePaint)

                    if (item.isChecked) {
                        val p1x = rect.left + checkboxSize * 0.22f
                        val p1y = rect.top + checkboxSize * 0.54f
                        val p2x = rect.left + checkboxSize * 0.44f
                        val p2y = rect.top + checkboxSize * 0.78f
                        val p3x = rect.left + checkboxSize * 0.82f
                        val p3y = rect.top + checkboxSize * 0.22f
                        canvas.drawLine(p1x, p1y, p2x, p2y, checkFillPaint)
                        canvas.drawLine(p2x, p2y, p3x, p3y, checkFillPaint)
                    }

                    // Item text
                    val textRight = width - marginX - 74f
                    val textLeft = boxX + checkboxSize + 28f
                    val maxW = textRight - textLeft

                    val itemPaint = Paint(itemTextPaint).apply {
                        color = if (item.isChecked) checkedTextColor else inkColor
                        textAlign = Paint.Align.RIGHT
                    }
                    val displayText = truncateToWidth(itemPaint, item.text, maxW)
                    canvas.drawText(displayText, textRight, baselineY, itemPaint)

                    // Strikethrough if completed
                    if (item.isChecked) {
                        val textW = itemPaint.measureText(displayText)
                        val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(120, 0x24, 0x24, 0x21)
                            strokeWidth = 2.5f
                            strokeCap = Paint.Cap.ROUND
                        }
                        val strikeY = baselineY - 15f
                        canvas.drawLine(textRight - textW - 4f, strikeY, textRight + 4f, strikeY, strikePaint)
                    }
                } else {
                    // LTR: Number on Left, Text flowing rightwards, Checkbox on Right
                    val numX = marginX + 10f
                    itemNumPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText("${index + 1}.", numX, baselineY, itemNumPaint)

                    // Checkbox on far Right
                    val boxX = width - marginX - checkboxSize - 10f
                    val rect = RectF(boxX, checkboxY, boxX + checkboxSize, checkboxY + checkboxSize)
                    canvas.drawRoundRect(rect, 8f, 8f, checkStrokePaint)

                    if (item.isChecked) {
                        val p1x = rect.left + checkboxSize * 0.22f
                        val p1y = rect.top + checkboxSize * 0.54f
                        val p2x = rect.left + checkboxSize * 0.44f
                        val p2y = rect.top + checkboxSize * 0.78f
                        val p3x = rect.left + checkboxSize * 0.82f
                        val p3y = rect.top + checkboxSize * 0.22f
                        canvas.drawLine(p1x, p1y, p2x, p2y, checkFillPaint)
                        canvas.drawLine(p2x, p2y, p3x, p3y, checkFillPaint)
                    }

                    // Item text
                    val textLeft = marginX + 74f
                    val textRight = boxX - 28f
                    val maxW = textRight - textLeft

                    val itemPaint = Paint(itemTextPaint).apply {
                        color = if (item.isChecked) checkedTextColor else inkColor
                        textAlign = Paint.Align.LEFT
                    }
                    val displayText = truncateToWidth(itemPaint, item.text, maxW)
                    canvas.drawText(displayText, textLeft, baselineY, itemPaint)

                    // Strikethrough if completed
                    if (item.isChecked) {
                        val textW = itemPaint.measureText(displayText)
                        val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb(120, 0x24, 0x24, 0x21)
                            strokeWidth = 2.5f
                            strokeCap = Paint.Cap.ROUND
                        }
                        val strikeY = baselineY - 15f
                        canvas.drawLine(textLeft - 4f, strikeY, textLeft + textW + 4f, strikeY, strikePaint)
                    }
                }
            }
        }

        // 8. Subtle Notebook Bottom Signature
        val footerY = height - 60f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            color = mutedInkColor
            alpha = 140
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Warqa • ورقة وستيلو فـ جيبك", width / 2f, footerY, footerPaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, checklistId: String): Uri {
        val shareDir = File(context.cacheDir, "shared_checklists")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        val file = File(shareDir, "checklist_${checklistId}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun launchImageShareIntent(context: Context, uri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Partager la Checklist").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
