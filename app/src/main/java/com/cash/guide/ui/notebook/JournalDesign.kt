package com.cash.guide.ui.notebook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R

// --- Color Palette: French Bullet-Journal (Dynamic Pack Linked) ---
val JournalPaper: Color get() = JournalTheme.currentPalette.paper
val JournalDockBg: Color get() = JournalTheme.currentPalette.dockBg
val JournalInk: Color get() = JournalTheme.currentPalette.ink
val JournalWritingInk: Color get() = JournalTheme.currentPalette.writingInk
val JournalMutedInk: Color get() = JournalTheme.currentPalette.mutedInk
val JournalRule: Color get() = JournalTheme.currentPalette.rule
val JournalAccent: Color get() = JournalTheme.currentPalette.accent

// Pastel Marker Highlighters
val HighlighterPink = Color(0xFFF3A7B9)    // Category, '=' button, '+' dab, double underline
val HighlighterYellow = Color(0xFFF4D66D)  // 'Total' heading, '−' dab
val HighlighterGreen = Color(0xFFC9DDA0)   // '×' dab
val HighlighterBlue = Color(0xFFA8CFE3)    // '÷' dab
val HighlighterPurple = Color(0xFFDDD6FE)  // Contacts

// Row Action Colors (Handwritten × and ✓)
val JournalActionDelete = Color(0xFFD66860)   // Restrained muted coral for delete '×'
val JournalActionConfirm = Color(0xFF3E8A52)  // Restrained muted green for confirm '✓'
val JournalCreditRed = Color(0xFFD66860)      // Coral red for credit amounts and warnings
val JournalErrorRed = Color(0xFFD66860)       // Muted coral for error text and badges

// Action Icon Geometry (Unclipped, anchored directly on top of notebook paper rule like text)
val JournalActionIconSize = 12.dp
val JournalActionIconRuleGap = 0.dp

// Offline Handwritten Fonts
val JournalHandFamily = FontFamily(
    Font(R.font.patrick_hand_regular, FontWeight.Normal)
)

// Authoritative geometry token for ruled paper rhythm and rows
val JournalRuleSpacing = 29.dp

val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

// --- Migrated Notebook & Theme Design System ---
val Paper: Color get() = JournalPaper
val PaperWarm: Color get() = JournalDockBg
val Ink: Color get() = JournalInk
val WritingInk: Color get() = JournalWritingInk
val MutedInk: Color get() = JournalMutedInk
val Rule: Color get() = JournalRule

val ColorCoral = Color(0xFFF05B48)
val ColorOrange = Color(0xFFE38D2C)

enum class InkTone(val color: Color) {
    Ink(Color(0xFF3B3C39)),
    Coral(Color(0xFFF05B48)),
    Teal(Color(0xFF2CA5A2)),
    Green(Color(0xFF59A85A)),
    Blue(Color(0xFF3377AD)),
    Purple(Color(0xFF8869B2)),
    Orange(Color(0xFFE38D2C))
}

object HisabiMetrics {
    val Grid = 29.dp
    val WritingBaseline = Grid
    val MetadataBaseline = Grid
    val Gutter = 44.dp
    val TopBarHeight = 54.dp
    val IconSize = 20.dp
    val KeypadDockHeight = 260.dp
}

object NotebookMetrics {
    val ruleSpacing = JournalRuleSpacing // 29.dp
    // Text geometry is now baseline-driven. Keep both values at zero so no
    // locale or screen can drift away from the physical notebook rule.
    val baselineOffset = 0.dp
    val baselineOffsetRtl = 0.dp
    val ruleStroke = 0.6.dp
    val verticalGuideThickness = 1.5.dp
}

@Composable
fun notebookBaselineOffset(isRtl: Boolean): Dp =
    if (isRtl) NotebookMetrics.baselineOffsetRtl else NotebookMetrics.baselineOffset

// --- Bundled Offline Font Families ---
val PatrickHandFamily = JournalHandFamily

val MajazFamily = FontFamily(
    Font(R.font.majaz_regular, FontWeight.Normal)
)

val CreamFrothFamily = FontFamily(
    Font(R.font.cream_froth_light, FontWeight.Light),
    Font(R.font.cream_froth_regular, FontWeight.Normal),
    Font(R.font.cream_froth_regular, FontWeight.Medium),
    Font(R.font.cream_froth_bold, FontWeight.SemiBold),
    Font(R.font.cream_froth_bold, FontWeight.Bold)
)

// Primary Arabic handwriting font used across the entire app (testing Cream Froth)
val ArabicFamily: FontFamily = CreamFrothFamily

// Alias maintaining 100% compatibility with untouched CalculationEditorScreen
val TajawalFamily: FontFamily = CreamFrothFamily

val ManropeFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold)
)

// --- Script Detection & Typography Helpers ---
fun isArabicScript(text: String): Boolean {
    return text.any { c ->
        c in '\u0600'..'\u06FF' ||
        c in '\u0750'..'\u077F' ||
        c in '\u08A0'..'\u08FF' ||
        c in '\uFB50'..'\uFDFF' ||
        c in '\uFE70'..'\uFEFF'
    }
}

/**
 * Universal script- and locale-aware handwritten font resolver.
 * French/English/Latin -> PatrickHandFamily
 * Arabic -> CreamFrothFamily
 */
fun resolveJournalFont(text: String = "", isRtl: Boolean = false): FontFamily {
    return if (isArabicScript(text) || (text.isBlank() && isRtl)) {
        CreamFrothFamily
    } else {
        PatrickHandFamily
    }
}

/**
 * Formats digits and quantities inside a text string with FontWeight.Bold
 * so numbers (e.g. 1 kg, 2 gousses, 10 DH, 2 كيلو) stand out clearly.
 */
fun highlightNumbersInText(text: String, baseColor: Color = JournalInk): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    val regex = Regex("""[\d\u0660-\u0669]+([.,][\d\u0660-\u0669]+)?""")
    if (!regex.containsMatchIn(text)) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }
            val hasAsciiDigits = match.value.any { it in '0'..'9' }
            withStyle(
                SpanStyle(
                    fontFamily = if (hasAsciiDigits) PatrickHandFamily else CreamFrothFamily,
                    fontWeight = FontWeight.Bold,
                    fontSynthesis = FontSynthesis.Weight,
                    color = baseColor
                )
            ) {
                append(text.substring(start, end))
            }
            lastIndex = end
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

/**
 * VisualTransformation that renders numeric digits in bold inside BasicTextField
 * without altering cursor position or string contents.
 */
object NumberBoldVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlightNumbersInText(text.text)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
fun arabicWritingStyle(
    color: Color = WritingInk,
    sizeSp: Float = 14.5f,
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = TajawalFamily,
    fontSize = sizeSp.sp,
    lineHeight = with(LocalDensity.current) { HisabiMetrics.Grid.toSp() },
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    ),
    fontWeight = weight,
    color = color
)

@Composable
fun amountWritingStyle(
    color: Color = Ink,
    sizeSp: Float = 15.5f,
    weight: FontWeight = FontWeight.Medium
) = TextStyle(
    fontFamily = PatrickHandFamily,
    fontSize = sizeSp.sp,
    lineHeight = with(LocalDensity.current) { HisabiMetrics.Grid.toSp() },
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    ),
    fontWeight = weight,
    color = color
)

// --- Typography Styles ---
@Composable
fun journalCategoryStyle() = TextStyle(
    fontFamily = JournalHandFamily,
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,
    color = JournalInk,
    lineHeight = 19.sp,
    platformStyle = NoFontPadding
)

@Composable
fun journalRowNumberStyle(color: Color = JournalMutedInk) = TextStyle(
    fontFamily = JournalHandFamily,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = color,
    lineHeight = 18.sp,
    platformStyle = NoFontPadding
)

@Composable
fun journalTitleStyle(text: String = "", color: Color = JournalInk): TextStyle {
    val isArabic = isArabicScript(text)
    return TextStyle(
        fontFamily = if (isArabic) CreamFrothFamily else JournalHandFamily,
        fontSize = if (isArabic) 14.5.sp else 15.sp,
        fontWeight = FontWeight.Normal,
        color = color,
        lineHeight = 20.sp,
        platformStyle = NoFontPadding
    )
}

@Composable
fun journalAmountStyle(color: Color = JournalInk) = TextStyle(
    fontFamily = JournalHandFamily,
    fontSize = 15.5.sp,
    fontWeight = FontWeight.Normal,
    color = color,
    lineHeight = 20.sp,
    platformStyle = NoFontPadding
)

@Composable
fun journalSuffixStyle() = TextStyle(
    fontFamily = JournalHandFamily,
    fontSize = 13.5.sp,
    fontWeight = FontWeight.Normal,
    color = JournalMutedInk,
    lineHeight = 16.sp,
    platformStyle = NoFontPadding
)

@Composable
fun journalTotalLabelStyle() = TextStyle(
    fontFamily = JournalHandFamily,
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,
    color = JournalInk,
    lineHeight = 18.sp,
    platformStyle = NoFontPadding
)

@Composable
fun journalPrimaryTotalStyle(color: Color = JournalInk) = TextStyle(
    fontFamily = JournalHandFamily,
    fontSize = 18.5.sp,
    fontWeight = FontWeight.Bold,
    color = color,
    lineHeight = 22.sp,
    platformStyle = NoFontPadding
)

@Composable
fun journalSecondaryTotalStyle() = TextStyle(
    fontFamily = JournalHandFamily,
    fontSize = 13.5.sp,
    fontWeight = FontWeight.Normal,
    color = JournalMutedInk,
    lineHeight = 17.sp,
    platformStyle = NoFontPadding
)

val JournalKeyDigitStyle: TextStyle
    get() = TextStyle(
        fontFamily = JournalHandFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        color = JournalInk,
        platformStyle = NoFontPadding
    )

val JournalKeyOperatorStyle: TextStyle
    get() = TextStyle(
        fontFamily = JournalHandFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        color = JournalInk,
        platformStyle = NoFontPadding
    )

// --- Deterministic Organic Highlighter Modifiers ---
/**
 * Organic highlighter stroke that visually centers around the text bounds
 * with balanced margin above and below, while preserving underlying baseline alignment.
 */
fun Modifier.journalHighlighter(
    color: Color,
    alpha: Float = 0.40f,
    horizontalPadding: Dp = 6.dp,
    verticalPadding: Dp = 1.0.dp,
    seedVariant: Int = 0
): Modifier = drawBehind {
    val hPad = horizontalPadding.toPx()
    val vPad = verticalPadding.toPx()
    val left = -hPad
    val top = -vPad
    val right = size.width + hPad
    val bottom = size.height + vPad
    val w = right - left
    val h = bottom - top

    // Deterministic organic path with tilted chisel entry/exit and subtle irregular upper/lower edges
    val p1 = if (seedVariant % 2 == 0) 0.6f else -0.6f
    val p2 = if (seedVariant % 3 == 0) -0.8f else 0.8f

    val path = Path().apply {
        moveTo(left + 2f, top + 1.2f)
        cubicTo(
            left + w * 0.28f, top - 1.0f + p1,
            left + w * 0.68f, top + 0.6f + p2,
            right - 1.5f, top + 1.2f
        )
        cubicTo(
            right + 2.0f, top + h * 0.45f,
            right + 1.0f, top + h * 0.85f,
            right - 1.8f, bottom - 0.9f
        )
        cubicTo(
            left + w * 0.72f, bottom + 1.0f + p1,
            left + w * 0.32f, bottom - 0.7f + p2,
            left + 2.2f, bottom - 1.2f
        )
        cubicTo(
            left - 1.2f, top + h * 0.65f,
            left - 0.6f, top + h * 0.35f,
            left + 2f, top + 1.2f
        )
        close()
    }

    drawPath(path = path, color = color.copy(alpha = alpha))
}

// Subtle active field underline
fun Modifier.journalActiveUnderline(
    color: Color,
    strokeWidth: Dp = 1.35.dp
): Modifier = drawBehind {
    val strokeW = strokeWidth.toPx()
    val y = size.height + 2.dp.toPx()
    drawLine(
        color = color,
        start = Offset(-2.dp.toPx(), y),
        end = Offset(size.width + 2.dp.toPx(), y),
        strokeWidth = strokeW,
        cap = StrokeCap.Round
    )
}

// Small pastel operator dab for keypad keys
fun Modifier.journalOperatorDab(
    color: Color,
    alpha: Float = 0.75f,
    widthDp: Dp = 32.dp,
    heightDp: Dp = 16.dp
): Modifier = drawBehind {
    val w = widthDp.toPx()
    val h = heightDp.toPx()
    val left = (size.width - w) / 2f
    val top = (size.height - h) / 2f
    val right = left + w
    val bottom = top + h

    val path = Path().apply {
        moveTo(left + 2f, top + 1.5f)
        quadraticTo(left + w * 0.5f, top - 1f, right - 1.5f, top + 1f)
        quadraticTo(right + 2.5f, top + h * 0.5f, right - 1f, bottom - 1f)
        quadraticTo(left + w * 0.5f, bottom + 1.5f, left + 1.5f, bottom - 1f)
        quadraticTo(left - 2.5f, top + h * 0.5f, left + 2f, top + 1.5f)
        close()
    }
    drawPath(path = path, color = color.copy(alpha = alpha))
}

// Double Pink Pen Underline beneath Total
@Composable
fun JournalDoubleUnderline(
    modifier: Modifier = Modifier,
    color: Color = HighlighterPink,
    width: Dp = 150.dp
) {
    Canvas(modifier = modifier.width(width).height(8.dp)) {
        val strokeW = 1.4.dp.toPx()
        // Upper line
        drawLine(
            color = color.copy(alpha = 0.88f),
            start = Offset(2.dp.toPx(), 2.dp.toPx()),
            end = Offset(size.width - 2.dp.toPx(), 2.dp.toPx()),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Lower line
        drawLine(
            color = color.copy(alpha = 0.88f),
            start = Offset(5.dp.toPx(), 5.8.dp.toPx()),
            end = Offset(size.width - 5.dp.toPx(), 5.8.dp.toPx()),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

// Sketched dashed border for notebook CTA / add row buttons
fun Modifier.journalDashedBorder(
    color: Color = JournalInk.copy(alpha = 0.45f),
    strokeWidth: Dp = 1.2.dp,
    cornerRadius: Dp = 12.dp,
    intervals: FloatArray = floatArrayOf(14f, 10f)
): Modifier = drawBehind {
    val sw = strokeWidth.toPx()
    val halfSw = sw / 2f
    val stroke = Stroke(
        width = sw,
        pathEffect = PathEffect.dashPathEffect(intervals, 0f)
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(halfSw, halfSw),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke
    )
}
