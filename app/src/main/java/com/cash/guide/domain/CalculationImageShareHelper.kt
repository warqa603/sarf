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
import com.cash.guide.data.db.CalculationItemEntity
import com.cash.guide.data.db.CalculationWithItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CalculationImageShareHelper {

    /**
     * Renders and shares a [CalculationWithItems] as a single continuous long notebook image.
     */
    fun shareCalculation(
        context: Context,
        calculationWithItems: CalculationWithItems,
        groupName: String? = null,
        isRtl: Boolean,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        val calc = calculationWithItems.calculation
        val currency = runCatching { MoneyUnit.valueOf(calc.currency) }.getOrDefault(MoneyUnit.DIRHAM)

        shareCalculation(
            context = context,
            calculationId = calc.id,
            title = calc.title.ifBlank { context.getString(R.string.editor_new_title) },
            currency = currency,
            items = calculationWithItems.items.sortedBy { it.position },
            totalCentimes = calculationWithItems.totalCentimes,
            groupName = groupName,
            createdAtEpochMs = calc.createdAtEpochMs,
            isRtl = isRtl,
            coroutineScope = coroutineScope
        )
    }

    /**
     * Renders and shares calculation parameters as a single continuous long notebook image.
     */
    fun shareCalculation(
        context: Context,
        calculationId: String,
        title: String,
        currency: MoneyUnit,
        items: List<CalculationItemEntity>,
        totalCentimes: Long,
        groupName: String? = null,
        createdAtEpochMs: Long = System.currentTimeMillis(),
        isRtl: Boolean,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        coroutineScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    renderLongReceiptBitmap(
                        context = context,
                        title = title,
                        currency = currency,
                        items = items,
                        totalCentimes = totalCentimes,
                        groupName = groupName,
                        createdAtEpochMs = createdAtEpochMs,
                        isRtl = isRtl
                    )
                }

                val uri = withContext(Dispatchers.IO) {
                    saveBitmapToCache(context, bitmap, calculationId)
                }

                launchShareIntent(context, uri, title)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, R.string.share_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Renders the complete calculation into an offscreen 2D Bitmap.
     * The height is dynamically determined so that all items are included seamlessly
     * without any cutoff or clipping.
     */
    fun renderLongReceiptBitmap(
        context: Context,
        title: String,
        currency: MoneyUnit,
        items: List<CalculationItemEntity>,
        totalCentimes: Long,
        groupName: String?,
        createdAtEpochMs: Long,
        isRtl: Boolean
    ): Bitmap {
        val width = 1080
        val ruleSpacing = 74f
        val marginX = 60f

        // Fonts
        val creamFrothFont = runCatching {
            ResourcesCompat.getFont(context, R.font.cream_froth)
        }.getOrNull()
        val majazFont = runCatching {
            ResourcesCompat.getFont(context, R.font.majaz_regular)
        }.getOrNull() ?: Typeface.DEFAULT
        val patrickHandFont = runCatching {
            ResourcesCompat.getFont(context, R.font.patrick_hand_regular)
        }.getOrNull() ?: Typeface.DEFAULT
        val manropeBold = runCatching {
            ResourcesCompat.getFont(context, R.font.manrope_bold)
        }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val manropeMedium = runCatching {
            ResourcesCompat.getFont(context, R.font.manrope_medium)
        }.getOrNull() ?: Typeface.DEFAULT

        val primaryFont = if (isRtl) (creamFrothFont ?: majazFont) else patrickHandFont

        // Colors
        val paperColor = Color.rgb(0xF8, 0xF9, 0xFA)          // #F8F9FA Feuille Blanche (White Notebook Paper)
        val inkColor = Color.rgb(0x24, 0x24, 0x21)            // #242421 Journal Ink
        val writingInkColor = Color.rgb(0x38, 0x38, 0x34)     // #383834
        val mutedInkColor = Color.rgb(0x7A, 0x79, 0x72)       // #7A7972
        val ruleColor = Color.argb(0x80, 0xB8, 0xC7, 0xCC)    // #B8C7CC cool gray-blue
        val pinkWashColor = Color.argb(0x80, 0xF3, 0xA7, 0xB9) // #F3A7B9
        val yellowWashColor = Color.argb(0x5A, 0xF4, 0xD6, 0x6D) // #F4D66D (~35% alpha)
        val blueWashColor = Color.argb(0x80, 0xA8, 0xCF, 0xE3) // #A8CFE3

        // Calculate layout coordinates and heights
        var currentY = 56f

        // 1. App Header: Branding & Date
        val headerY = currentY
        currentY += 80f

        // 2. Group Badge (if present)
        val groupY = if (!groupName.isNullOrBlank()) {
            val y = currentY
            currentY += 56f
            y
        } else null

        // 3. Calculation Title Wash Box
        val titleBoxY = currentY
        currentY += 92f

        // 4. Spacer before items
        currentY += 34f
        val itemsStartY = currentY

        // 5. Items section height
        val displayItemCount = items.size.coerceAtLeast(1)
        val itemsHeight = displayItemCount * ruleSpacing
        currentY += itemsHeight

        // 6. Total Box
        currentY += 36f
        val totalBoxY = currentY
        currentY += 100f

        // 7. Footer Watermark
        currentY += 44f
        val footerY = currentY
        currentY += 80f

        val totalHeight = currentY.toInt()

        // Create Bitmap and Canvas
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw Paper Background
        canvas.drawColor(paperColor)

        // Paints
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ruleColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Draw ruled horizontal lines across items section
        for (i in 0..displayItemCount) {
            val lineY = itemsStartY + i * ruleSpacing
            canvas.drawLine(0f, lineY, width.toFloat(), lineY, rulePaint)
        }

        // --- 1. Draw App Branding & Date ---
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 34f
            color = inkColor
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = patrickHandFont
            textSize = 28f
            color = mutedInkColor
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val brandText = context.getString(R.string.share_brand)
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(createdAtEpochMs))

        if (isRtl) {
            canvas.drawText(brandText, width - marginX, headerY + 36f, brandPaint)
            canvas.drawText(dateStr, marginX, headerY + 36f, datePaint)
        } else {
            canvas.drawText(brandText, marginX, headerY + 36f, brandPaint)
            canvas.drawText(dateStr, width - marginX, headerY + 36f, datePaint)
        }

        // --- 2. Draw Group Badge (if present) ---
        if (groupY != null && !groupName.isNullOrBlank()) {
            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = primaryFont
                textSize = 28f
                color = inkColor
                textAlign = Paint.Align.CENTER
            }
            val textW = badgeTextPaint.measureText(groupName)
            val badgeW = textW + 36f
            val badgeH = 42f
            val badgeLeft = if (isRtl) width - marginX - badgeW else marginX
            val badgeRect = RectF(badgeLeft, groupY, badgeLeft + badgeW, groupY + badgeH)

            washPaint.color = blueWashColor
            canvas.drawRoundRect(badgeRect, 14f, 14f, washPaint)

            canvas.drawText(groupName, badgeRect.centerX(), groupY + 30f, badgeTextPaint)
        }

        // --- 3. Draw Title with Pink Highlighter Wash ---
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 44f
            color = inkColor
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val defaultTitle = context.getString(R.string.editor_new_title)
        val displayTitle = title.ifBlank { defaultTitle }
        val titleWidth = titlePaint.measureText(displayTitle).coerceAtMost(width - marginX * 2 - 40f)
        val titleWashW = (titleWidth + 56f).coerceAtMost(width - marginX * 2)
        val titleWashLeft = (width - titleWashW) / 2f
        val titleWashRect = RectF(titleWashLeft, titleBoxY, titleWashLeft + titleWashW, titleBoxY + 76f)

        washPaint.color = pinkWashColor
        canvas.drawRoundRect(titleWashRect, 20f, 20f, washPaint)

        val truncatedTitle = truncateToWidth(titlePaint, displayTitle, titleWashW - 40f)
        canvas.drawText(truncatedTitle, width / 2f, titleBoxY + 54f, titlePaint)

        // --- 4. Draw Items Rows ---
        val itemNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 30f
            color = mutedInkColor
            textAlign = Paint.Align.CENTER
        }
        val itemLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 34f
            color = writingInkColor
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val itemAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = manropeBold
            textSize = 33f
            color = inkColor
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }
        val itemCurrencyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 25f
            color = mutedInkColor
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val currencySuffix = if (currency == MoneyUnit.DIRHAM) {
            if (isRtl) "درهم" else "DH"
        } else {
            if (isRtl) "ريال" else "rial"
        }

        if (items.isEmpty()) {
            // Empty state row
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = primaryFont
                textSize = 32f
                color = mutedInkColor
                textAlign = Paint.Align.CENTER
            }
            val emptyText = context.getString(R.string.share_empty_items)
            val yBaseline = itemsStartY + ruleSpacing - 1f
            canvas.drawText(emptyText, width / 2f, yBaseline, emptyPaint)
        } else {
            val articlePrefix = context.getString(R.string.share_article_prefix)
            items.forEachIndexed { index, item ->
                val lineY = itemsStartY + (index + 1) * ruleSpacing
                val yBaseline = lineY - 1f

                // Row Number
                val numText = "${index + 1}"
                val numX = if (isRtl) width - marginX - 18f else marginX + 18f
                canvas.drawText(numText, numX, yBaseline, itemNumPaint)

                // Amount Text
                val amountVal = MoneyMath.fromCentimes(item.amountCentimes, currency)
                val amountW = itemAmountPaint.measureText(amountVal)
                val currW = itemCurrencyPaint.measureText(" $currencySuffix")
                val totalAmountW = amountW + currW

                // Label Text
                val rawLabel = item.label.trim().ifBlank {
                    "$articlePrefix ${index + 1}"
                }

                if (isRtl) {
                    // RTL: Amount on Left, Label on Right
                    canvas.drawText(amountVal, marginX, yBaseline, itemAmountPaint)
                    canvas.drawText(currencySuffix, marginX + amountW + 10f, yBaseline, itemCurrencyPaint)

                    val labelRight = width - marginX - 68f
                    val maxLabelW = labelRight - (marginX + totalAmountW + 36f)
                    val labelText = truncateToWidth(itemLabelPaint, rawLabel, maxLabelW)
                    canvas.drawText(labelText, labelRight, yBaseline, itemLabelPaint)
                } else {
                    // LTR: Label on Left, Amount on Right
                    canvas.drawText(amountVal, width - marginX - currW - 8f, yBaseline, itemAmountPaint)
                    canvas.drawText(currencySuffix, width - marginX, yBaseline, itemCurrencyPaint)

                    val labelLeft = marginX + 68f
                    val maxLabelW = (width - marginX - totalAmountW - 36f) - labelLeft
                    val labelText = truncateToWidth(itemLabelPaint, rawLabel, maxLabelW)
                    canvas.drawText(labelText, labelLeft, yBaseline, itemLabelPaint)
                }
            }
        }

        // --- 5. Draw Total Section ---
        val totalBoxW = (width * 0.65f).coerceIn(600f, 720f)
        val totalBoxLeft = (width - totalBoxW) / 2f
        val totalBoxRight = totalBoxLeft + totalBoxW
        val totalBoxH = 86f
        val totalBoxRect = RectF(totalBoxLeft, totalBoxY, totalBoxRight, totalBoxY + totalBoxH)
        washPaint.color = yellowWashColor
        canvas.drawRoundRect(totalBoxRect, 18f, 18f, washPaint)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 38f
            color = inkColor
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val totalAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = manropeBold
            textSize = 44f
            color = inkColor
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }
        val totalCurrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 30f
            color = inkColor
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val totalValStr = MoneyMath.fromCentimes(totalCentimes, currency)
        val totalLabelStr = context.getString(R.string.share_total_label)
        val tCurrW = totalCurrPaint.measureText(currencySuffix)

        val totalBaseline = totalBoxY + 56f

        if (isRtl) {
            canvas.drawText(totalLabelStr, totalBoxRight - 36f, totalBaseline, totalLabelPaint)
            val tAmtW = totalAmountPaint.measureText(totalValStr)
            canvas.drawText(totalValStr, totalBoxLeft + 36f, totalBaseline, totalAmountPaint)
            canvas.drawText(currencySuffix, totalBoxLeft + 36f + tAmtW + 14f, totalBaseline - 2f, totalCurrPaint)
        } else {
            canvas.drawText(totalLabelStr, totalBoxLeft + 36f, totalBaseline, totalLabelPaint)
            canvas.drawText(totalValStr, totalBoxRight - 36f - tCurrW - 10f, totalBaseline, totalAmountPaint)
            canvas.drawText(currencySuffix, totalBoxRight - 36f, totalBaseline - 2f, totalCurrPaint)
        }

        // Double pink underline beneath total
        val underPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkWashColor
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val underY1 = totalBoxY + totalBoxH + 12f
        val underY2 = underY1 + 7f
        canvas.drawLine(totalBoxLeft + 24f, underY1, totalBoxRight - 24f, underY1, underPaint)
        canvas.drawLine(totalBoxLeft + 42f, underY2, totalBoxRight - 42f, underY2, underPaint)

        // --- 6. Draw Footer Watermark ---
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 28f
            color = mutedInkColor
            textAlign = Paint.Align.CENTER
        }
        val watermark = context.getString(R.string.share_watermark)
        canvas.drawText(watermark, width / 2f, footerY + 36f, footerPaint)

        return bitmap
    }

    /**
     * Truncates text to fit within [maxWidth] pixels, appending an ellipsis if truncated.
     */
    private fun truncateToWidth(paint: Paint, text: String, maxWidth: Float): String {
        if (maxWidth <= 0) return ""
        if (paint.measureText(text) <= maxWidth) return text

        val ellipsis = "…"
        val ellipsisW = paint.measureText(ellipsis)
        val availW = maxWidth - ellipsisW
        if (availW <= 0) return ellipsis

        var low = 0
        var high = text.length
        var best = 0

        while (low <= high) {
            val mid = (low + high) / 2
            val sub = text.substring(0, mid)
            if (paint.measureText(sub) <= availW) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return text.substring(0, best) + ellipsis
    }

    /**
     * Saves the rendered bitmap to the cache directory as a PNG file.
     */
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, calculationId: String): Uri {
        val shareDir = File(context.cacheDir, "shared_calculations")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
        val file = File(shareDir, "calc_${calculationId}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Launches the system share chooser for the generated image URI.
     */
    private fun launchShareIntent(context: Context, uri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
