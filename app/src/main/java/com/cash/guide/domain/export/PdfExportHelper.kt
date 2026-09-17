package com.cash.guide.domain.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.cash.guide.R
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyUnit
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    private const val PAGE_WIDTH = 595 // Standard A4 points at 72dpi
    private const val PAGE_HEIGHT = 842 // Standard A4 points at 72dpi
    private const val MARGIN_X = 36f
    private const val MARGIN_Y = 40f
    private const val FOOTER_HEIGHT = 35f
    private const val MAX_CONTENT_Y = PAGE_HEIGHT - MARGIN_Y - FOOTER_HEIGHT

    /**
     * Exports a single calculation to a beautifully styled A4 notebook PDF document.
     */
    fun exportSingleCalculationPdf(
        context: Context,
        calculationWithItems: CalculationWithItems,
        groupName: String? = null,
        isRtl: Boolean
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val calc = calculationWithItems.calculation
        val currency = runCatching { MoneyUnit.valueOf(calc.currency) }.getOrDefault(MoneyUnit.DIRHAM)
        val currencySuffix = if (currency == MoneyUnit.DIRHAM) {
            if (isRtl) "درهم" else "DH"
        } else {
            if (isRtl) "ريال" else "rial"
        }

        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanTitle = calc.title.trim().replace(Regex("[^a-zA-Z0-9\\p{IsArabic}]"), "_").take(25)
            .ifBlank { "calculation" }
        val file = File(exportDir, "hssabi_${cleanTitle}_${dateSuffix}.pdf")

        // Load fonts
        val primaryFont = resolvePrimaryFont(context, isRtl)
        val digitFont = resolveDigitFont(context)

        val doc = PdfDocument()
        var pageNumber = 1
        var currentPage = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = currentPage.canvas

        drawPageBackground(canvas)

        var currentY = MARGIN_Y

        // 1. App Header: Branding & Date
        drawBrandingHeader(context, canvas, primaryFont, calc.createdAtEpochMs, currentY, isRtl)
        currentY += 34f

        // 2. Group Badge (if present)
        if (!groupName.isNullOrBlank()) {
            drawGroupBadge(canvas, primaryFont, groupName, currentY, isRtl)
            currentY += 26f
        }

        // 3. Calculation Title Wash Box
        val titleText = calc.title.ifBlank { context.getString(R.string.editor_new_title) }
        drawTitleBanner(canvas, primaryFont, titleText, currentY)
        currentY += 46f

        // 4. Note (if present)
        if (!calc.note.isNullOrBlank()) {
            currentY += 4f
            drawNoteBox(canvas, primaryFont, calc.note, currentY, isRtl)
            currentY += 32f
        }

        currentY += 14f

        // 5. Items Table Header
        drawTableHeader(context, canvas, primaryFont, currentY, isRtl)
        currentY += 24f

        // 6. Draw Items
        val items = calculationWithItems.items.sortedBy { it.position }
        val itemRowHeight = 26f

        if (items.isEmpty()) {
            drawEmptyItemsRow(context, canvas, primaryFont, currentY)
            currentY += itemRowHeight
        } else {
            val articlePrefix = context.getString(R.string.share_article_prefix)
            items.forEachIndexed { index, item ->
                // Check if row fits on current page
                if (currentY + itemRowHeight > MAX_CONTENT_Y) {
                    drawFooter(context, canvas, primaryFont, pageNumber, isRtl)
                    doc.finishPage(currentPage)

                    pageNumber++
                    currentPage = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = currentPage.canvas
                    drawPageBackground(canvas)

                    currentY = MARGIN_Y
                    drawContinuationHeader(context, canvas, primaryFont, titleText, currentY, isRtl)
                    currentY += 30f
                    drawTableHeader(context, canvas, primaryFont, currentY, isRtl)
                    currentY += 24f
                }

                val amountStr = MoneyMath.fromCentimes(item.amountCentimes, currency)
                val label = item.label.trim().ifBlank { "$articlePrefix ${index + 1}" }
                drawItemRow(canvas, primaryFont, digitFont, index + 1, label, item.rawExpression, amountStr, currencySuffix, currentY, isRtl)
                currentY += itemRowHeight
            }
        }

        // 7. Total Box
        val totalBoxHeight = 58f
        if (currentY + totalBoxHeight > MAX_CONTENT_Y) {
            drawFooter(context, canvas, primaryFont, pageNumber, isRtl)
            doc.finishPage(currentPage)

            pageNumber++
            currentPage = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = currentPage.canvas
            drawPageBackground(canvas)
            currentY = MARGIN_Y + 16f
        } else {
            currentY += 16f
        }

        val totalAmountStr = MoneyMath.fromCentimes(calculationWithItems.totalCentimes, currency)
        drawTotalBox(context, canvas, primaryFont, digitFont, totalAmountStr, currencySuffix, currentY, isRtl)

        // Footer on the last page
        drawFooter(context, canvas, primaryFont, pageNumber, isRtl)
        doc.finishPage(currentPage)

        FileOutputStream(file).use { out ->
            doc.writeTo(out)
            out.flush()
        }
        doc.close()

        return file
    }

    /**
     * Exports all calculations into a comprehensive summary statement PDF report.
     */
    fun exportAllCalculationsPdf(
        context: Context,
        calculations: List<CalculationWithItems>,
        groupMap: Map<String, String> = emptyMap(),
        isRtl: Boolean
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "hssabi_report_${dateSuffix}.pdf")

        val primaryFont = resolvePrimaryFont(context, isRtl)
        val digitFont = resolveDigitFont(context)

        val doc = PdfDocument()
        var pageNumber = 1
        var currentPage = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = currentPage.canvas

        drawPageBackground(canvas)

        var currentY = MARGIN_Y

        // 1. Report Master Header
        val reportTitle = context.getString(R.string.export_report_title)
        drawReportMasterHeader(context, canvas, primaryFont, reportTitle, currentY, isRtl)
        currentY += 46f

        // 2. Summary KPI Box
        val totalCalcs = calculations.size
        val totalItems = calculations.sumOf { it.items.size }
        val totalDirhamsCentimes = calculations
            .filter { it.calculation.currency == MoneyUnit.DIRHAM.name }
            .sumOf { it.totalCentimes }
        val totalRialsCentimes = calculations
            .filter { it.calculation.currency == MoneyUnit.RIAL.name }
            .sumOf { it.totalCentimes }

        drawKpiCards(context, canvas, primaryFont, digitFont, totalCalcs, totalItems, totalDirhamsCentimes, totalRialsCentimes, currentY, isRtl)
        currentY += 66f

        // 3. Iterating through all calculations
        calculations.forEachIndexed { calcIdx, calcWithItems ->
            val calc = calcWithItems.calculation
            val currency = runCatching { MoneyUnit.valueOf(calc.currency) }.getOrDefault(MoneyUnit.DIRHAM)
            val currencySuffix = if (currency == MoneyUnit.DIRHAM) {
                if (isRtl) "درهم" else "DH"
            } else {
                if (isRtl) "ريال" else "rial"
            }
            val groupName = calc.groupId?.let { groupMap[it] }

            val neededHeight = 44f + (calcWithItems.items.size.coerceAtLeast(1) * 22f) + 16f
            if (currentY + neededHeight > MAX_CONTENT_Y) {
                drawFooter(context, canvas, primaryFont, pageNumber, isRtl)
                doc.finishPage(currentPage)

                pageNumber++
                currentPage = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = currentPage.canvas
                drawPageBackground(canvas)
                currentY = MARGIN_Y
            }

            // Calculation Mini Header
            val title = calc.title.ifBlank { context.getString(R.string.editor_new_title) }
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(calc.createdAtEpochMs))
            val totalStr = "${MoneyMath.fromCentimes(calcWithItems.totalCentimes, currency)} $currencySuffix"

            drawCalculationSectionHeader(canvas, primaryFont, digitFont, calcIdx + 1, title, groupName, dateStr, totalStr, currentY, isRtl)
            currentY += 24f

            // Compact Items
            val items = calcWithItems.items.sortedBy { it.position }
            if (items.isEmpty()) {
                drawCompactEmptyRow(context, canvas, primaryFont, currentY)
                currentY += 20f
            } else {
                items.forEachIndexed { index, item ->
                    if (currentY + 22f > MAX_CONTENT_Y) {
                        drawFooter(context, canvas, primaryFont, pageNumber, isRtl)
                        doc.finishPage(currentPage)

                        pageNumber++
                        currentPage = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                        canvas = currentPage.canvas
                        drawPageBackground(canvas)
                        currentY = MARGIN_Y
                    }
                    val amountStr = MoneyMath.fromCentimes(item.amountCentimes, currency)
                    drawCompactItemRow(canvas, primaryFont, digitFont, index + 1, item.label, item.rawExpression, amountStr, currencySuffix, currentY, isRtl)
                    currentY += 20f
                }
            }
            currentY += 14f
        }

        drawFooter(context, canvas, primaryFont, pageNumber, isRtl)
        doc.finishPage(currentPage)

        FileOutputStream(file).use { out ->
            doc.writeTo(out)
            out.flush()
        }
        doc.close()

        return file
    }

    // --- Drawing Subroutines ---

    private fun drawPageBackground(canvas: Canvas) {
        val bgPaint = Paint().apply {
            color = Color.rgb(0xFD, 0xFC, 0xF7) // Elegant Warm Paper
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // Outer subtle border
        val borderPaint = Paint().apply {
            color = Color.argb(0x30, 0xAA, 0xB8, 0xC0)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(16f, 16f, PAGE_WIDTH - 16f, PAGE_HEIGHT - 16f, borderPaint)
    }

    private fun drawBrandingHeader(
        context: Context,
        canvas: Canvas,
        font: Typeface,
        createdAtEpochMs: Long,
        topY: Float,
        isRtl: Boolean
    ) {
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 15f
            color = Color.rgb(0x22, 0x22, 0x20)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 11f
            color = Color.rgb(0x78, 0x78, 0x72)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val brandText = "SARF • ${context.getString(R.string.share_brand)}"
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(createdAtEpochMs))

        val leftX = MARGIN_X
        val rightX = PAGE_WIDTH - MARGIN_X
        val baseline = topY + 14f

        if (isRtl) {
            canvas.drawText(brandText, rightX, baseline, brandPaint)
            canvas.drawText(dateStr, leftX, baseline, datePaint)
        } else {
            canvas.drawText(brandText, leftX, baseline, brandPaint)
            canvas.drawText(dateStr, rightX, baseline, datePaint)
        }

        // Header divider rule
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x60, 0xB0, 0xC0, 0xC8)
            strokeWidth = 1f
        }
        canvas.drawLine(leftX, baseline + 8f, rightX, baseline + 8f, rulePaint)
    }

    private fun drawGroupBadge(
        canvas: Canvas,
        font: Typeface,
        groupName: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 11f
            color = Color.rgb(0x1B, 0x4D, 0x6E)
            textAlign = Paint.Align.CENTER
        }
        val textWidth = badgePaint.measureText(groupName)
        val badgeWidth = textWidth + 20f
        val badgeHeight = 18f
        val badgeX = if (isRtl) PAGE_WIDTH - MARGIN_X - badgeWidth else MARGIN_X

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x40, 0x90, 0xC8, 0xEA)
            style = Paint.Style.FILL
        }
        val rect = RectF(badgeX, topY, badgeX + badgeWidth, topY + badgeHeight)
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
        canvas.drawText(groupName, rect.centerX(), topY + 13f, badgePaint)
    }

    private fun drawTitleBanner(
        canvas: Canvas,
        font: Typeface,
        title: String,
        topY: Float
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 19f
            color = Color.rgb(0x20, 0x20, 0x1E)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val maxAvailableWidth = PAGE_WIDTH - MARGIN_X * 2 - 32f
        val truncated = truncateToWidth(titlePaint, title, maxAvailableWidth)
        val textWidth = titlePaint.measureText(truncated)

        val bannerWidth = (textWidth + 40f).coerceIn(160f, PAGE_WIDTH - MARGIN_X * 2)
        val bannerHeight = 36f
        val bannerLeft = (PAGE_WIDTH - bannerWidth) / 2f
        val bannerRect = RectF(bannerLeft, topY, bannerLeft + bannerWidth, topY + bannerHeight)

        val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x55, 0xF5, 0xA8, 0xBA) // Soft Pink Wash
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bannerRect, 10f, 10f, washPaint)
        canvas.drawText(truncated, PAGE_WIDTH / 2f, topY + 25f, titlePaint)
    }

    private fun drawNoteBox(
        canvas: Canvas,
        font: Typeface,
        note: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 11f
            color = Color.rgb(0x55, 0x55, 0x50)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val boxRect = RectF(MARGIN_X, topY, PAGE_WIDTH - MARGIN_X, topY + 24f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x35, 0xF8, 0xE7, 0xA0) // Subtle Yellow Wash
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, 6f, 6f, bgPaint)

        val textX = if (isRtl) PAGE_WIDTH - MARGIN_X - 10f else MARGIN_X + 10f
        val truncated = truncateToWidth(notePaint, note, boxRect.width() - 20f)
        canvas.drawText(truncated, textX, topY + 16f, notePaint)
    }

    private fun drawTableHeader(
        context: Context,
        canvas: Canvas,
        font: Typeface,
        topY: Float,
        isRtl: Boolean
    ) {
        val barRect = RectF(MARGIN_X, topY, PAGE_WIDTH - MARGIN_X, topY + 20f)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x30, 0x88, 0xA5, 0xB5)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(barRect, 4f, 4f, barPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 10.5f
            color = Color.rgb(0x3A, 0x48, 0x50)
            isFakeBoldText = true
        }

        val colNum = context.getString(R.string.export_col_number)
        val colItem = context.getString(R.string.export_col_item)
        val colExp = context.getString(R.string.export_col_expression)
        val colAmt = context.getString(R.string.export_col_amount)
        val baseline = topY + 14f

        if (isRtl) {
            headerTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(colNum, PAGE_WIDTH - MARGIN_X - 20f, baseline, headerTextPaint)

            headerTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(colItem, PAGE_WIDTH - MARGIN_X - 50f, baseline, headerTextPaint)

            headerTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(colExp, MARGIN_X + 130f, baseline, headerTextPaint)
            canvas.drawText(colAmt, MARGIN_X + 12f, baseline, headerTextPaint)
        } else {
            headerTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(colNum, MARGIN_X + 20f, baseline, headerTextPaint)

            headerTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(colItem, MARGIN_X + 50f, baseline, headerTextPaint)

            headerTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(colExp, PAGE_WIDTH - MARGIN_X - 130f, baseline, headerTextPaint)
            canvas.drawText(colAmt, PAGE_WIDTH - MARGIN_X - 12f, baseline, headerTextPaint)
        }
    }

    private fun drawItemRow(
        canvas: Canvas,
        primaryFont: Typeface,
        digitFont: Typeface,
        index: Int,
        label: String,
        expression: String?,
        amountStr: String,
        currencySuffix: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val baseline = topY + 18f

        // Underline rule
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x40, 0xC0, 0xCC, 0xD4)
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_X, topY + 24f, PAGE_WIDTH - MARGIN_X, topY + 24f, linePaint)

        // Paints
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 10.5f
            color = Color.rgb(0x7A, 0x79, 0x72)
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 12.5f
            color = Color.rgb(0x28, 0x28, 0x24)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val expPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = digitFont
            textSize = 10f
            color = Color.rgb(0x8A, 0x89, 0x82)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }
        val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = digitFont
            textSize = 12f
            color = Color.rgb(0x1C, 0x1C, 0x1A)
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }
        val currPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 9.5f
            color = Color.rgb(0x60, 0x60, 0x5C)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val fullAmtStr = "$amountStr $currencySuffix"

        if (isRtl) {
            canvas.drawText(index.toString(), PAGE_WIDTH - MARGIN_X - 20f, baseline, numPaint)

            val maxLabelW = (PAGE_WIDTH - MARGIN_X - 50f) - (MARGIN_X + 220f)
            val truncatedLabel = truncateToWidth(labelPaint, label, maxLabelW)
            canvas.drawText(truncatedLabel, PAGE_WIDTH - MARGIN_X - 50f, baseline, labelPaint)

            if (!expression.isNullOrBlank()) {
                val truncatedExp = truncateToWidth(expPaint, expression, 85f)
                canvas.drawText(truncatedExp, MARGIN_X + 130f, baseline, expPaint)
            }
            canvas.drawText(fullAmtStr, MARGIN_X + 12f, baseline, amtPaint)
        } else {
            canvas.drawText(index.toString(), MARGIN_X + 20f, baseline, numPaint)

            val maxLabelW = (PAGE_WIDTH - MARGIN_X - 220f) - (MARGIN_X + 50f)
            val truncatedLabel = truncateToWidth(labelPaint, label, maxLabelW)
            canvas.drawText(truncatedLabel, MARGIN_X + 50f, baseline, labelPaint)

            if (!expression.isNullOrBlank()) {
                val truncatedExp = truncateToWidth(expPaint, expression, 85f)
                canvas.drawText(truncatedExp, PAGE_WIDTH - MARGIN_X - 130f, baseline, expPaint)
            }
            canvas.drawText(fullAmtStr, PAGE_WIDTH - MARGIN_X - 12f, baseline, amtPaint)
        }
    }

    private fun drawEmptyItemsRow(context: Context, canvas: Canvas, font: Typeface, topY: Float) {
        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 12f
            color = Color.rgb(0x8A, 0x89, 0x82)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.share_empty_items), PAGE_WIDTH / 2f, topY + 18f, emptyPaint)
    }

    private fun drawTotalBox(
        context: Context,
        canvas: Canvas,
        primaryFont: Typeface,
        digitFont: Typeface,
        totalStr: String,
        currencySuffix: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val boxWidth = 260f
        val boxHeight = 44f
        val boxLeft = if (isRtl) MARGIN_X else PAGE_WIDTH - MARGIN_X - boxWidth
        val boxRight = boxLeft + boxWidth
        val boxRect = RectF(boxLeft, topY, boxRight, topY + boxHeight)

        val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x60, 0xF6, 0xDC, 0x75) // Yellow Wash
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, 8f, 8f, washPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 13.5f
            color = Color.rgb(0x22, 0x22, 0x20)
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val totalAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = digitFont
            textSize = 17f
            color = Color.rgb(0x1C, 0x1C, 0x18)
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }
        val totalCurrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 11.5f
            color = Color.rgb(0x40, 0x40, 0x3C)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val baseline = topY + 28f
        val labelStr = context.getString(R.string.share_total_label)

        if (isRtl) {
            canvas.drawText(labelStr, boxRight - 14f, baseline, labelPaint)
            val fullText = "$totalStr $currencySuffix"
            canvas.drawText(fullText, boxLeft + 14f, baseline, totalAmountPaint)
        } else {
            canvas.drawText(labelStr, boxLeft + 14f, baseline, labelPaint)
            val fullText = "$totalStr $currencySuffix"
            canvas.drawText(fullText, boxRight - 14f, baseline, totalAmountPaint)
        }

        // Double underline
        val underPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x80, 0xF5, 0xA8, 0xBA)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val u1 = topY + boxHeight + 4f
        val u2 = u1 + 3.5f
        canvas.drawLine(boxLeft + 10f, u1, boxRight - 10f, u1, underPaint)
        canvas.drawLine(boxLeft + 18f, u2, boxRight - 18f, u2, underPaint)
    }

    private fun drawFooter(
        context: Context,
        canvas: Canvas,
        font: Typeface,
        pageNumber: Int,
        isRtl: Boolean
    ) {
        val y = PAGE_HEIGHT - MARGIN_Y
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x40, 0xB0, 0xC0, 0xC8)
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, rulePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 9.5f
            color = Color.rgb(0x8A, 0x88, 0x82)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 9.5f
            color = Color.rgb(0x8A, 0x88, 0x82)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val watermark = "WARQA • ${context.getString(R.string.share_footer_watermark)}"
        val pageStr = if (isRtl) "صفحة $pageNumber" else "Page $pageNumber"

        if (isRtl) {
            canvas.drawText(watermark, PAGE_WIDTH - MARGIN_X, y + 15f, footerPaint)
            canvas.drawText(pageStr, MARGIN_X, y + 15f, pagePaint)
        } else {
            canvas.drawText(watermark, MARGIN_X, y + 15f, footerPaint)
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN_X, y + 15f, pagePaint)
        }
    }

    private fun drawContinuationHeader(
        context: Context,
        canvas: Canvas,
        font: Typeface,
        title: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 12f
            color = Color.rgb(0x55, 0x55, 0x50)
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val text = "$title (…)"
        val x = if (isRtl) PAGE_WIDTH - MARGIN_X else MARGIN_X
        canvas.drawText(text, x, topY + 14f, paint)
    }

    private fun drawReportMasterHeader(
        context: Context,
        canvas: Canvas,
        font: Typeface,
        reportTitle: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 19f
            color = Color.rgb(0x1F, 0x24, 0x28)
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 11f
            color = Color.rgb(0x70, 0x75, 0x7A)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val baseline = topY + 20f

        if (isRtl) {
            canvas.drawText(reportTitle, PAGE_WIDTH - MARGIN_X, baseline, titlePaint)
            canvas.drawText(dateStr, MARGIN_X, baseline, datePaint)
        } else {
            canvas.drawText(reportTitle, MARGIN_X, baseline, titlePaint)
            canvas.drawText(dateStr, PAGE_WIDTH - MARGIN_X, baseline, datePaint)
        }

        // Underline divider
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x80, 0x5B, 0x9E, 0xC9)
            strokeWidth = 1.5f
        }
        canvas.drawLine(MARGIN_X, baseline + 10f, PAGE_WIDTH - MARGIN_X, baseline + 10f, rulePaint)
    }

    private fun drawKpiCards(
        context: Context,
        canvas: Canvas,
        primaryFont: Typeface,
        digitFont: Typeface,
        totalCalcs: Int,
        totalItems: Int,
        totalDirhamsCentimes: Long,
        totalRialsCentimes: Long,
        topY: Float,
        isRtl: Boolean
    ) {
        val cardWidth = (PAGE_WIDTH - MARGIN_X * 2 - 20f) / 3f
        val cardHeight = 48f

        // Card 1: Total Calculations
        val rect1 = RectF(MARGIN_X, topY, MARGIN_X + cardWidth, topY + cardHeight)
        drawSingleKpiCard(canvas, primaryFont, digitFont, context.getString(R.string.export_options_title), totalCalcs.toString(), rect1, Color.argb(0x35, 0x90, 0xC8, 0xEA))

        // Card 2: Total Items
        val x2 = MARGIN_X + cardWidth + 10f
        val rect2 = RectF(x2, topY, x2 + cardWidth, topY + cardHeight)
        drawSingleKpiCard(canvas, primaryFont, digitFont, context.getString(R.string.calculation_lines_count, totalItems), totalItems.toString(), rect2, Color.argb(0x35, 0xF5, 0xA8, 0xBA))

        // Card 3: Total Money in DH
        val x3 = x2 + cardWidth + 10f
        val rect3 = RectF(x3, topY, x3 + cardWidth, topY + cardHeight)
        val dhTotalStr = "${MoneyMath.fromCentimes(totalDirhamsCentimes, MoneyUnit.DIRHAM)} DH"
        drawSingleKpiCard(canvas, primaryFont, digitFont, context.getString(R.string.share_total_label), dhTotalStr, rect3, Color.argb(0x40, 0xF6, 0xDC, 0x75))
    }

    private fun drawSingleKpiCard(
        canvas: Canvas,
        primaryFont: Typeface,
        digitFont: Typeface,
        label: String,
        value: String,
        rect: RectF,
        bgColor: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = digitFont
            textSize = 14f
            color = Color.rgb(0x1C, 0x1C, 0x18)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 9.5f
            color = Color.rgb(0x5A, 0x58, 0x54)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(value, rect.centerX(), rect.top + 20f, valPaint)
        canvas.drawText(label, rect.centerX(), rect.top + 36f, labelPaint)
    }

    private fun drawCalculationSectionHeader(
        canvas: Canvas,
        primaryFont: Typeface,
        digitFont: Typeface,
        index: Int,
        title: String,
        groupName: String?,
        dateStr: String,
        totalStr: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val rect = RectF(MARGIN_X, topY, PAGE_WIDTH - MARGIN_X, topY + 20f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x28, 0x7F, 0xA8, 0x5B) // Soft Sage Green
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 4f, 4f, bgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 11.5f
            color = Color.rgb(0x22, 0x22, 0x20)
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = digitFont
            textSize = 11.5f
            color = Color.rgb(0x2A, 0x50, 0x22)
            isFakeBoldText = true
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val groupSuffix = if (!groupName.isNullOrBlank()) " [$groupName]" else ""
        val fullTitle = "$index. $title$groupSuffix ($dateStr)"
        val baseline = topY + 14f

        if (isRtl) {
            val truncated = truncateToWidth(titlePaint, fullTitle, PAGE_WIDTH - MARGIN_X * 2 - 120f)
            canvas.drawText(truncated, PAGE_WIDTH - MARGIN_X - 8f, baseline, titlePaint)
            canvas.drawText(totalStr, MARGIN_X + 8f, baseline, totalPaint)
        } else {
            val truncated = truncateToWidth(titlePaint, fullTitle, PAGE_WIDTH - MARGIN_X * 2 - 120f)
            canvas.drawText(truncated, MARGIN_X + 8f, baseline, titlePaint)
            canvas.drawText(totalStr, PAGE_WIDTH - MARGIN_X - 8f, baseline, totalPaint)
        }
    }

    private fun drawCompactItemRow(
        canvas: Canvas,
        primaryFont: Typeface,
        digitFont: Typeface,
        index: Int,
        label: String,
        expression: String?,
        amountStr: String,
        currencySuffix: String,
        topY: Float,
        isRtl: Boolean
    ) {
        val baseline = topY + 14f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0x25, 0xC0, 0xCC, 0xD4)
            strokeWidth = 0.5f
        }
        canvas.drawLine(MARGIN_X + 12f, topY + 18f, PAGE_WIDTH - MARGIN_X - 12f, topY + 18f, linePaint)

        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 9.5f
            color = Color.rgb(0x8A, 0x88, 0x80)
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = primaryFont
            textSize = 10.5f
            color = Color.rgb(0x2C, 0x2C, 0x28)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val expPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = digitFont
            textSize = 9f
            color = Color.rgb(0x8A, 0x89, 0x82)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }
        val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = digitFont
            textSize = 10.5f
            color = Color.rgb(0x1C, 0x1C, 0x1A)
            textAlign = if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        val fullAmt = "$amountStr $currencySuffix"

        if (isRtl) {
            canvas.drawText(index.toString(), PAGE_WIDTH - MARGIN_X - 22f, baseline, numPaint)
            val truncatedLabel = truncateToWidth(labelPaint, label, 240f)
            canvas.drawText(truncatedLabel, PAGE_WIDTH - MARGIN_X - 44f, baseline, labelPaint)
            if (!expression.isNullOrBlank()) {
                canvas.drawText(truncateToWidth(expPaint, expression, 80f), MARGIN_X + 110f, baseline, expPaint)
            }
            canvas.drawText(fullAmt, MARGIN_X + 16f, baseline, amtPaint)
        } else {
            canvas.drawText(index.toString(), MARGIN_X + 22f, baseline, numPaint)
            val truncatedLabel = truncateToWidth(labelPaint, label, 240f)
            canvas.drawText(truncatedLabel, MARGIN_X + 44f, baseline, labelPaint)
            if (!expression.isNullOrBlank()) {
                canvas.drawText(truncateToWidth(expPaint, expression, 80f), PAGE_WIDTH - MARGIN_X - 110f, baseline, expPaint)
            }
            canvas.drawText(fullAmt, PAGE_WIDTH - MARGIN_X - 16f, baseline, amtPaint)
        }
    }

    private fun drawCompactEmptyRow(context: Context, canvas: Canvas, font: Typeface, topY: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = font
            textSize = 10f
            color = Color.rgb(0x8A, 0x88, 0x80)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.share_empty_items), PAGE_WIDTH / 2f, topY + 14f, paint)
    }

    private fun resolvePrimaryFont(context: Context, isRtl: Boolean): Typeface {
        return if (isRtl) {
            runCatching { ResourcesCompat.getFont(context, R.font.cream_froth) }.getOrNull()
                ?: runCatching { ResourcesCompat.getFont(context, R.font.majaz_regular) }.getOrNull()
                ?: Typeface.DEFAULT
        } else {
            runCatching { ResourcesCompat.getFont(context, R.font.patrick_hand_regular) }.getOrNull()
                ?: Typeface.DEFAULT
        }
    }

    private fun resolveDigitFont(context: Context): Typeface {
        return runCatching { ResourcesCompat.getFont(context, R.font.manrope_bold) }.getOrNull()
            ?: Typeface.DEFAULT_BOLD
    }

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
}
