package com.cash.guide.ui.notebook

import androidx.activity.compose.BackHandler
import com.cash.guide.domain.reminder.CreditStatusHelper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import com.cash.guide.domain.MoneyMath
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import com.cash.guide.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import com.cash.guide.domain.RecentActivityItem
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.cash.guide.domain.BackspaceRepeatController
import com.cash.guide.domain.JournalKeyboardController
import com.cash.guide.domain.JournalKeyboardLanguage
import com.cash.guide.domain.JournalKeyboardMode
import com.cash.guide.domain.JournalKeySpec
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.domain.JournalShiftMode
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.domain.DateGroupHelper
import com.cash.guide.domain.MoneyUnit

/**
 * Ruled document with tactile paper texture and 48dp horizontal rules that scroll with content.
 */
@Composable
fun JournalRuledDocument(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    clearFocusOnTap: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && clearFocusOnTap) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(JournalPaper)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = clearFocusOnTap
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        val minimumHeight = maxHeight
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minimumHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = clearFocusOnTap
                        ) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                        .drawBehind {
                            // 1. Subtle tactile paper grain / flecks
                            val dotColor = JournalInk.copy(alpha = 0.022f)
                            var px = 18f
                            while (px < size.width) {
                                var py = 22f
                                while (py < size.height) {
                                    drawCircle(
                                        color = dotColor,
                                        radius = 0.9f,
                                        center = Offset(px, py)
                                    )
                                    py += 64f
                                }
                                px += 48f
                            }

                            // 2. 29dp Horizontal Rules (matching Style Lab & Home)
                            val rowHeightPx = JournalRuleSpacing.roundToPx().toFloat()
                            var y = rowHeightPx
                            while (y <= size.height) {
                                drawLine(
                                    color = JournalRule.copy(alpha = 0.35f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 0.6.dp.toPx()
                                )
                                y += rowHeightPx
                            }
                        },
                    content = content
                )
            }
        }
    }
}

/**
 * Category header sitting on the paper rule with a dusty pink highlighter stroke.
 * Features a hand-drawn calculator outline icon on the far right.
 */
@Composable
fun JournalCategoryHeader(
    categoryName: String,
    modifier: Modifier = Modifier,
    onOpenCalculator: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing) // exactly 48dp = 1 notebook rule
            .drawBehind {
                val strokeW = 0.55.dp.toPx()
                val y = size.height
                drawLine(
                    color = JournalRule.copy(alpha = 0.52f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        JournalBaselineHighlightedText(
            text = categoryName,
            style = journalCategoryStyle(),
            highlighterColor = HighlighterPink,
            highlighterAlpha = 0.40f,
            horizontalPadding = 5.dp,
            verticalPadding = 1.0.dp,
            seedVariant = 1
        )

        if (onOpenCalculator != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Ouvrir la calculatrice",
                        onClick = onOpenCalculator
                    )
                    .semantics { testTag = "tag_header_calculator_icon" },
                contentAlignment = Alignment.Center
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Calculator,
                    contentDescription = "Ouvrir la calculatrice",
                    tint = JournalInk,
                    size = 20.dp,
                    modifier = Modifier.offset(y = 14.dp)
                )
            }
        }
    }
}

/**
 * Transaction row in authentic Moroccan bullet-journal style.
 * Exactly 1 visible ruled-paper line (29dp height).
 * Both Arabic label and Latin digits rest directly on the ruled line.
 */
@Composable
fun JournalEntryRow(
    rowNumber: Int,
    titleValue: TextFieldValue,
    amountValue: TextFieldValue,
    currencySuffix: String,
    isTitleActive: Boolean,
    isAmountActive: Boolean,
    isValid: Boolean,
    titleFocusRequester: FocusRequester? = null,
    amountFocusRequester: FocusRequester? = null,
    onTitleValueChange: (TextFieldValue) -> Unit,
    onAmountValueChange: (TextFieldValue) -> Unit,
    onTitleFocused: () -> Unit,
    onAmountFocused: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isInvalid = !isValid && amountValue.text.isNotBlank()
    val isRowActive = isTitleActive || isAmountActive

    val amountTextColor = when {
        isInvalid -> ColorCoral
        amountValue.text.isBlank() -> JournalMutedInk.copy(alpha = 0.5f)
        else -> JournalInk
    }

    val titleFocusMod = if (titleFocusRequester != null) {
        Modifier.focusRequester(titleFocusRequester)
    } else {
        Modifier
    }

    val amountFocusMod = if (amountFocusRequester != null) {
        Modifier.focusRequester(amountFocusRequester)
    } else {
        Modifier
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var titleLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var amountLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "journal_row_cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor_blink"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing) // 29.dp
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Rotating watercolor palette for row numbers
        val rowDotColors = remember {
            listOf(
                Color(0xFF3B82B6), // Row 1: Rich Sky Blue
                Color(0xFFD65D82), // Row 2: Rich Rose Pink
                Color(0xFF5E8C3B), // Row 3: Rich Sage Green
                Color(0xFFC7881E), // Row 4: Warm Amber
                Color(0xFF7E5AA8), // Row 5: Deep Lavender
                Color(0xFFCC673B)  // Row 6: Warm Peach Coral
            )
        }
        val dotColor = rowDotColors[(rowNumber - 1).coerceAtLeast(0) % rowDotColors.size]

        // Right side (RTL start): Colored Row Number + Title Input
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Handwritten colored row number sitting directly on the blue notebook line
            Text(
                text = "$rowNumber",
                fontFamily = PatrickHandFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = dotColor,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier
                    .widthIn(min = 14.dp)
                    .journalBaselineOnRule()
            )

            BasicTextField(
                value = titleValue,
                onValueChange = onTitleValueChange,
                modifier = titleFocusMod
                    .weight(1f, fill = false)
                    .offset(y = 6.7.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onTitleFocused()
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        if (isTitleActive) {
                            // Pink active underline
                            val sw = 1.3.dp.toPx()
                            val y = size.height - 2.dp.toPx()
                            drawLine(
                                color = HighlighterPink.copy(alpha = 0.85f),
                                start = Offset(0f, y),
                                end = Offset(size.width.coerceAtLeast(60.dp.toPx()), y),
                                strokeWidth = sw,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    .semantics { testTag = "tag_row_title_$rowNumber" },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = resolveJournalFont(titleValue.text, isRtl),
                    fontSize = if (isArabicScript(titleValue.text) || isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = JournalInk,
                    platformStyle = NoFontPadding,
                    textDirection = if (isArabicScript(titleValue.text)) TextDirection.Rtl else TextDirection.ContentOrLtr
                ),
                cursorBrush = SolidColor(JournalInk),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onAmountFocused() }
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (titleValue.text.isEmpty()) {
                            val placeholderText = stringResource(R.string.editor_item_placeholder)
                            Text(
                                text = placeholderText,
                                fontFamily = resolveJournalFont(placeholderText, isRtl),
                                fontSize = if (isArabicScript(placeholderText) || isRtl) 14.5.sp else 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = JournalMutedInk.copy(alpha = if (isTitleActive) 0.50f else 0.40f),
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Left side (RTL end): Amount + Suffix + Action Icon
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Editable Amount
            BasicTextField(
                value = amountValue,
                onValueChange = onAmountValueChange,
                modifier = amountFocusMod
                    .widthIn(min = 28.dp)
                    .offset(y = 5.7.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onAmountFocused()
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        if (isAmountActive) {
                            val sw = 1.2.dp.toPx()
                            val y1 = size.height - 3.dp.toPx()
                            val y2 = size.height - 0.5.dp.toPx()
                            drawLine(
                                color = HighlighterPink.copy(alpha = 0.85f),
                                start = Offset(0f, y1),
                                end = Offset(size.width, y1),
                                strokeWidth = sw,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = HighlighterPink.copy(alpha = 0.85f),
                                start = Offset(0f, y2),
                                end = Offset(size.width, y2),
                                strokeWidth = sw,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    .semantics { testTag = "tag_row_amount_$rowNumber" },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = PatrickHandFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = amountTextColor,
                    platformStyle = NoFontPadding
                ),
                cursorBrush = SolidColor(JournalInk),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onConfirm()
                    }
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (amountValue.text.isEmpty()) {
                            Text(
                                text = "0",
                                fontFamily = PatrickHandFamily,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = JournalMutedInk.copy(alpha = if (isAmountActive) 0.50f else 0.45f),
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Suffix ("درهم" / "ريال" / "DH")
            val isLatinSuffix = currencySuffix.contains(Regex("[a-zA-Z]"))
            Text(
                text = currencySuffix,
                fontFamily = if (isLatinSuffix) PatrickHandFamily else TajawalFamily,
                fontSize = if (isLatinSuffix) 13.5.sp else 12.sp,
                fontWeight = FontWeight.Normal,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.offset(y = if (isLatinSuffix) 5.8.dp else 5.0.dp)
            )

            // Action: Checkmark ✓ when active, Cross ✕ when inactive
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = JournalRuleSpacing)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isRowActive) "Valider la ligne" else "Supprimer la ligne",
                        onClick = if (isRowActive) onConfirm else onDelete
                    )
                    .semantics {
                        testTag = if (isRowActive) "tag_row_confirm_$rowNumber" else "tag_row_delete_$rowNumber"
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                if (isRowActive) {
                    NotebookCheckmark(
                        size = 16.dp,
                        modifier = Modifier.offset(y = 1.7.dp)
                    )
                } else {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Trash,
                        contentDescription = stringResource(R.string.editor_delete_row),
                        tint = JournalActionDelete.copy(alpha = 0.85f),
                        size = 16.dp,
                        modifier = Modifier.offset(y = 1.7.dp)
                    )
                }
            }
        }
    }
}

/**
 * Centered handwritten '+' button to add a new transaction row manually.
 * Occupies exactly 1 rule line (29dp).
 * Sitting directly ON the line immediately below the last entry row with NO background box.
 */
@Composable
fun JournalAddRowButton(
    onAddRow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing) // exactly 29dp = 1 notebook rule
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.editor_add_row),
                onClick = onAddRow
            )
            .semantics { testTag = "tag_add_row_button" },
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 0.dp)
        ) {
            // Handwritten sketch '+' symbol centered with handwriting text
            Canvas(
                modifier = Modifier
                    .size(width = 13.dp, height = 13.dp)
                    .offset(y = (-4.0).dp)
            ) {
                val strokeW = 1.5.dp.toPx()
                val ink = JournalWritingInk.copy(alpha = 0.8f)
                val midX = size.width / 2f
                val midY = size.height / 2f

                // Horizontal stroke
                drawLine(
                    color = ink,
                    start = Offset(1.dp.toPx(), midY),
                    end = Offset(size.width - 1.dp.toPx(), midY),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                // Vertical stroke
                drawLine(
                    color = ink,
                    start = Offset(midX, 1.dp.toPx()),
                    end = Offset(midX, size.height - 1.dp.toPx()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.editor_add_row),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 15.sp else 16.5.sp,
                fontWeight = FontWeight.Medium,
                color = JournalWritingInk.copy(alpha = 0.8f),
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }
    }
}

/**
 * A saved calculation row on the Home Page, occupying exactly 1 ruled line (29dp).
 * Strictly follows the Calculation Page model (JournalEntryRow):
 * - Left: Bullet dot tangent to line
 * - Title: Calculation name resting directly on line
 * - Right: Formatted total amount + currency suffix resting on line + More options icon (⋮)
 */
@Composable
fun JournalCalculationRow(
    index: Int,
    title: String,
    totalAmount: String,
    currencySuffix: String,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColorOverride: Color? = null
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val isLatinSuffix = currencySuffix.contains(Regex("[a-zA-Z]"))

    // Rotating soft pastel watercolor bullet dot colors per row (used when dotColorOverride is null)
    val rowDotColors = remember {
        listOf(
            Color(0xFF5B9EC9), // Soft Sky Blue
            Color(0xFFE27B97), // Soft Rose Pink
            Color(0xFF7FA85B), // Soft Sage Green
            Color(0xFFE0B038), // Soft Warm Amber
            Color(0xFF9878C8), // Soft Lavender
            Color(0xFFE28862)  // Soft Peach Coral
        )
    }
    val defaultDotColor = rowDotColors[index.coerceAtLeast(0) % rowDotColors.size]
    val dotColor = dotColorOverride ?: defaultDotColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing) // exactly 29.dp = 1 notebook rule
            .padding(horizontal = 14.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Start: Bullet dot on line + Title on line
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Little colored dot resting directly on the ruled line (single solid color, no outline)
            Canvas(
                modifier = Modifier
                    .size(7.5.dp)
                    .offset(y = if (isRtl) (-0.5).dp else 0.dp)
            ) {
                drawCircle(color = dotColor)
            }

            Text(
                text = title.ifBlank { stringResource(R.string.editor_new_title) },
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 16.5.sp else 17.5.sp,
                fontWeight = FontWeight.Normal,
                color = JournalInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }

        // Subtle connecting dotted line directly on the blue notebook line between Title and Amount
        Box(
            modifier = Modifier
                .weight(1f)
                .height(JournalRuleSpacing)
                .padding(horizontal = 6.dp)
                .drawBehind {
                    val strokeW = 0.85.dp.toPx()
                    val y = size.height
                    drawLine(
                        color = JournalWritingInk.copy(alpha = 0.28f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeW,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                    )
                }
        )

        // End: Amount + Suffix + More Options (⋮)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = totalAmount,
                fontFamily = PatrickHandFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = JournalInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )

            Text(
                text = currencySuffix,
                fontFamily = if (isLatinSuffix) PatrickHandFamily else TajawalFamily,
                fontSize = if (isLatinSuffix) 15.sp else 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )

            // Three dots icon (⋮)
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = JournalRuleSpacing)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.cd_more_options),
                        onClick = onMoreClick
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.More,
                    contentDescription = null,
                    tint = JournalMutedInk,
                    size = 18.dp,
                    modifier = Modifier.offset(y = 2.0.dp)
                )
            }
        }
    }
}

/**
 * "+ Nouveau calcul" button on the Home Page, sitting on exactly 1 ruled line (29dp).
 * Modeled directly on JournalAddRowButton in the calculation page.
 */
@Composable
fun JournalNewCalculationButton(
    onNewCalculation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing) // exactly 29dp = 1 notebook rule
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.home_new_calculation),
                onClick = onNewCalculation
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 13.dp, height = 13.dp)
                    .offset(y = (-4.0).dp)
            ) {
                val strokeW = 1.5.dp.toPx()
                val ink = JournalWritingInk.copy(alpha = 0.8f)
                val midX = size.width / 2f
                val midY = size.height / 2f

                // Horizontal stroke
                drawLine(
                    color = ink,
                    start = Offset(1.dp.toPx(), midY),
                    end = Offset(size.width - 1.dp.toPx(), midY),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                // Vertical stroke
                drawLine(
                    color = ink,
                    start = Offset(midX, 1.dp.toPx()),
                    end = Offset(midX, size.height - 1.dp.toPx()),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.home_new_calculation),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 16.sp else 17.5.sp,
                fontWeight = FontWeight.Bold,
                color = JournalWritingInk.copy(alpha = 0.85f),
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }
    }
}

/**
 * Header row for Recent Calculations on the Home Page, sitting on exactly 1 ruled line (29dp).
 * Features an edge-to-edge flush Mildliner pink highlighter bar flanking the uppercase title.
 */
@Composable
fun JournalRecentHeader(
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {}
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val markerColor = HighlighterPink.copy(alpha = 0.55f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        // Highlight starts flush at the left screen edge (weight 1f balances with right side to center the title)
        Box(
            modifier = Modifier
                .weight(1f)
                .offset(y = 0.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 3.dp, bottomEnd = 3.dp))
                .background(markerColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Uppercase Title text sitting directly on the blue ruled line in the center
        Text(
            text = if (isRtl) stringResource(R.string.home_recent_title) else stringResource(R.string.home_recent_title).uppercase(),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 19.sp else 19.5.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.offset(y = if (isRtl) 6.2.dp else 5.8.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Full-width Mildliner line extending all the way to right screen edge sitting on the blue line
        Box(
            modifier = Modifier
                .weight(1f)
                .offset(y = 0.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                .background(markerColor)
        )
    }
}

/**
 * Header row for Pinned / Favorite Calculations on the Home Page, sitting on exactly 1 ruled line (29dp).
 * Features an edge-to-edge flush Mildliner yellow highlighter bar flanking the centered uppercase title.
 */
@Composable
fun JournalFavoritesHeader(
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val markerColor = HighlighterPink.copy(alpha = 0.55f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        // Highlight starts flush at the left screen edge (weight 1f balances with right side to center the title)
        Box(
            modifier = Modifier
                .weight(1f)
                .offset(y = 0.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 3.dp, bottomEnd = 3.dp))
                .background(markerColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Uppercase Title text sitting directly on the blue ruled line in the center
        Text(
            text = if (isRtl) stringResource(R.string.home_favorites_title) else stringResource(R.string.home_favorites_title).uppercase(),
            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
            fontSize = if (isRtl) 19.sp else 19.5.sp,
            fontWeight = FontWeight.Bold,
            color = JournalInk,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.offset(y = if (isRtl) 6.2.dp else 5.8.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Full-width Mildliner line extending all the way to right screen edge sitting on the blue line
        Box(
            modifier = Modifier
                .weight(1f)
                .offset(y = 0.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                .background(markerColor)
        )
    }
}

/**
 * Timeline styling tokens corresponding to date sections.
 */
data class DateTimelineStyle(
    val washColor: Color,
    val dotColor: Color,
    val lineColor: Color = Color.Transparent
)

/**
 * Maps date header titles ("Aujourd'hui", "Hier", "Cette semaine", etc.)
 * to their respective pastel wash and matching solid dot colors.
 */
fun getDateTimelineStyle(
    title: String,
    todayText: String,
    yesterdayText: String
): DateTimelineStyle {
    return when {
        title.contains(todayText, ignoreCase = true) || title.contains("اليوم") -> {
            DateTimelineStyle(
                washColor = HighlighterYellow.copy(alpha = 0.60f),
                dotColor = Color(0xFFE0B038) // Warm Amber matching yellow date wash
            )
        }
        title.contains(yesterdayText, ignoreCase = true) || title.contains("أمس") || title.contains("البارح") -> {
            DateTimelineStyle(
                washColor = HighlighterGreen.copy(alpha = 0.55f),
                dotColor = Color(0xFF7FA85B) // Soft Sage Green matching green date wash
            )
        }
        else -> {
            DateTimelineStyle(
                washColor = HighlighterPink.copy(alpha = 0.50f),
                dotColor = Color(0xFFD66860) // Soft coral/rose matching pink date wash
            )
        }
    }
}

/**
 * Date separator on the Home Page, sitting on exactly 1 ruled line (29dp).
 * Left-aligned (Start), with larger font and soft highlighter wash behind the text bounds.
 */
@Composable
fun JournalDateRuleBand(
    title: String,
    modifier: Modifier = Modifier
) {
    val todayText = stringResource(R.string.date_today)
    val yesterdayText = stringResource(R.string.date_yesterday)

    val timelineStyle = getDateTimelineStyle(title, todayText, yesterdayText)
    val highlighterWashColor = timelineStyle.washColor

    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .drawBehind {
                    // Soft highlighter wash centered vertically around the date text,
                    // with the blue notebook rule passing through the lower portion
                    val h = size.height
                    val w = size.width
                    val washHeight = 21.dp.toPx()
                    val washCenterY = h - 6.5.dp.toPx()
                    val washY = washCenterY - (washHeight / 2f)
                    val padH = 8.dp.toPx()
                    drawRoundRect(
                        color = highlighterWashColor,
                        topLeft = Offset(-padH, washY),
                        size = Size(w + padH * 2, washHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
        ) {
            Text(
                text = title,
                fontFamily = resolveJournalFont(title, isRtl),
                fontSize = if (isArabicScript(title) || isRtl) 14.5.sp else 15.sp,
                fontWeight = FontWeight.Normal,
                color = JournalInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }
    }
}

/**
 * Primary call-to-action button: "+ حساب جديد" / "+ Nouveau calcul"
 * Renders as a soft rounded pink pill button spanning 2 notebook lines (58dp)
 * to maintain strict notebook line grid alignment.
 */
@Composable
fun JournalPrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.home_new_calculation)
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2) // exactly 2 notebook rules = 58dp
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(HighlighterPink.copy(alpha = 0.35f))
                .clickable(
                    role = Role.Button,
                    onClickLabel = text,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Canvas(modifier = Modifier.size(13.dp)) {
                    val strokeW = 1.9.dp.toPx()
                    val ink = JournalInk
                    val midX = size.width / 2f
                    val midY = size.height / 2f
                    drawLine(ink, Offset(1.dp.toPx(), midY), Offset(size.width - 1.dp.toPx(), midY), strokeW, StrokeCap.Round)
                    drawLine(ink, Offset(midX, 1.dp.toPx()), Offset(midX, size.height - 1.dp.toPx()), strokeW, StrokeCap.Round)
                }

                Text(
                    text = text,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 16.5.sp else 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.offset(y = if (isRtl) (-0.5).dp else 0.dp)
                )
            }
        }
    }
}

/**
 * Ruled-line inline search row matching Mockups 1 & 2.
 * Renders cleanly sitting on exactly 1 ruled line (29dp).
 * In RTL: text input on right (Start), search icon on left (End).
 * In LTR: search icon on left (Start), text input on right (End).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalInlineSearchRow(
    query: String,
    onQueryChange: ((String) -> Unit)?,
    placeholder: String = stringResource(R.string.home_search_input_placeholder),
    onClick: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null,
    isDateFiltered: Boolean = false,
    showUnderline: Boolean = true,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Automatically clear focus and hide cursor if the keyboard is hidden
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && isFocused) {
            focusManager.clearFocus()
        }
    }

    // Intercept back button when search is focused: clear focus and hide keyboard
    if (isFocused) {
        BackHandler {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // The Search Box (Grey highlight capsule touching both top and bottom blue lines)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(JournalRuleSpacing)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF6B7067).copy(alpha = 0.16f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (onQueryChange != null) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    onClick?.invoke()
                }
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Search Icon inside the box
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Search,
                    contentDescription = null,
                    tint = JournalInk.copy(alpha = 0.70f),
                    size = 16.dp
                )

                // Search Input with cursor and disappearing placeholder on focus
                if (onQueryChange != null) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = true,
                        cursorBrush = if (isFocused) SolidColor(JournalInk) else SolidColor(Color.Transparent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        textStyle = TextStyle(
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 15.sp else 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalInk,
                            platformStyle = NoFontPadding
                        ),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty() && !isFocused) {
                                Text(
                                    text = placeholder,
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    fontSize = if (isRtl) 13.5.sp else 14.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = JournalWritingInk.copy(alpha = 0.60f),
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                            innerTextField()
                        }
                    )
                } else {
                    Text(
                        text = placeholder,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 13.5.sp else 14.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = JournalWritingInk.copy(alpha = 0.60f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Clear button inside the box
                if (query.isNotEmpty() && onQueryChange != null) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(role = Role.Button, onClick = {
                                onQueryChange("")
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontFamily = PatrickHandFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk
                        )
                    }
                }
            }
        }

        // Calendar Icon Button, touching both top and bottom lines
        if (onOpenCalendar != null) {
            // Generous breathing space before the calendar icon
            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(JournalRuleSpacing)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDateFiltered) HighlighterYellow.copy(alpha = 0.50f) else Color.Transparent)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.home_pick_date),
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onOpenCalendar()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Calendar,
                    contentDescription = stringResource(R.string.home_pick_date),
                    tint = if (isDateFiltered) JournalInk else JournalInk.copy(alpha = 0.85f),
                    size = 22.dp
                )
            }
        }
    }
}

/**
 * Notebook ledger primary action button positioned at the bottom-right.
 * Circular button with handwritten '+' in notebook sketch style with soft pink highlighter.
 */
@Composable
fun JournalFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.home_new_calculation)
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = JournalInk.copy(alpha = 0.22f),
                spotColor = JournalInk.copy(alpha = 0.28f)
            )
            .clip(CircleShape)
            .background(HighlighterPink.copy(alpha = 0.80f))
            .border(
                width = 1.dp,
                color = JournalInk.copy(alpha = 0.30f),
                shape = CircleShape
            )
            .clickable(
                role = Role.Button,
                onClickLabel = text,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val strokeW = 2.4.dp.toPx()
            val ink = JournalInk
            val midX = size.width / 2f
            val midY = size.height / 2f
            drawLine(ink, Offset(1.5.dp.toPx(), midY), Offset(size.width - 1.5.dp.toPx(), midY), strokeW, StrokeCap.Round)
            drawLine(ink, Offset(midX, 1.5.dp.toPx()), Offset(midX, size.height - 1.5.dp.toPx()), strokeW, StrokeCap.Round)
        }
    }
}

/**
 * Line-anchored 2-line calculation entry matching mockups 1 & 2.
 * Spans exactly 2 ruled lines (58dp):
 * Line 1 (29dp): Watercolor bullet dot, bold title, bold amount, currency, and 3-dots action icon (⋮).
 * Line 2 (29dp): Contextual subtitle (time, item count, or snippet) resting neatly on the second line.
 */
@Composable
fun JournalTwoLineCalculationRow(
    index: Int,
    title: String,
    subtitle: String,
    totalAmount: String,
    currencySuffix: String,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    isPinned: Boolean = false,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val isLatinSuffix = currencySuffix.contains(Regex("[a-zA-Z]"))

    // Rotating soft pastel watercolor bullet dot colors per row
    val rowDotColors = remember {
        listOf(
            Color(0xFF5B9EC9), // Soft Sky Blue
            Color(0xFFE27B97), // Soft Rose Pink
            Color(0xFF7FA85B), // Soft Sage Green
            Color(0xFFE0B038), // Soft Warm Amber
            Color(0xFF9878C8), // Soft Lavender
            Color(0xFFE28862)  // Soft Peach Coral
        )
    }
    val dotColor = rowDotColors[index.coerceAtLeast(0) % rowDotColors.size]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2) // exactly 58dp = 2 ruled lines
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
    ) {
        // Line 1: Bullet dot + Title on Start; Connecting dotted line; Amount + Currency + 3-dots on End
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Bullet dot + Title
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(6.5.dp)
                        .offset(y = (-5.5).dp)
                ) {
                    drawCircle(color = dotColor)
                }

                if (isPinned) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Pin,
                        contentDescription = null,
                        tint = JournalInk.copy(alpha = 0.75f),
                        size = 14.dp,
                        modifier = Modifier.offset(y = (-4.0).dp)
                    )
                }

                Text(
                    text = title.ifBlank { stringResource(R.string.editor_new_title) },
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 16.5.sp else 17.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Subtle connecting line directly on the blue notebook line between Title and Amount
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 6.dp)
                    .drawBehind {
                        val strokeW = 0.85.dp.toPx()
                        val y = size.height
                        drawLine(
                            color = JournalWritingInk.copy(alpha = 0.28f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                        )
                    }
            )

            // End: Amount + Suffix + 3 dots (⋮)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = totalAmount,
                    fontFamily = PatrickHandFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                Text(
                    text = currencySuffix,
                    fontFamily = if (isLatinSuffix) PatrickHandFamily else TajawalFamily,
                    fontSize = if (isLatinSuffix) 15.sp else 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = JournalRuleSpacing)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.cd_more_options),
                            onClick = onMoreClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = null,
                        tint = JournalMutedInk,
                        size = 14.dp
                    )
                }
            }
        }

        // Line 2: Subtitle resting on ruled line 2, indented below title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = subtitle,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 13.sp else 13.5.sp,
                fontWeight = FontWeight.Light,
                color = JournalMutedInk.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp)
                    .journalBaselineOnRule()
            )
        }
    }
}

/**
 * Minimal ruled filter bar on Line 3 in HistoryScreen matching Mockup 2.
 * Real, interactive filter pills for "Tout" and "Ce mois".
 */
@Composable
fun JournalFilterBar(
    selectedFilter: com.cash.guide.feature.history.HistoryFilter = com.cash.guide.feature.history.HistoryFilter.ALL,
    onFilterSelect: (com.cash.guide.feature.history.HistoryFilter) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pill 1: Tout / الكل
        val isAll = selectedFilter == com.cash.guide.feature.history.HistoryFilter.ALL
        Box(
            modifier = Modifier
                .offset(y = 2.0.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAll) HighlighterPink.copy(alpha = 0.45f) else Color(0xFF6B7067).copy(alpha = 0.12f))
                .clickable(role = Role.RadioButton, onClick = { onFilterSelect(com.cash.guide.feature.history.HistoryFilter.ALL) })
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.history_filter_all),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 13.5.sp else 14.5.sp,
                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal,
                color = if (isAll) JournalInk else JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }

        // Pill 2: Ce mois / هذا الشهر
        val isMonth = selectedFilter == com.cash.guide.feature.history.HistoryFilter.THIS_MONTH
        Box(
            modifier = Modifier
                .offset(y = 2.0.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isMonth) HighlighterPink.copy(alpha = 0.45f) else Color(0xFF6B7067).copy(alpha = 0.12f))
                .clickable(role = Role.RadioButton, onClick = { onFilterSelect(com.cash.guide.feature.history.HistoryFilter.THIS_MONTH) })
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📅",
                    fontSize = 11.sp
                )
                Text(
                    text = stringResource(R.string.history_filter_month),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 13.5.sp else 14.5.sp,
                    fontWeight = if (isMonth) FontWeight.Bold else FontWeight.Normal,
                    color = if (isMonth) JournalInk else JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Notebook section header band occupying 1 rule line (29dp).
 * Used for "Calculs récents" (centered with flanked highlighters) and Settings section bands (start-aligned pill).
 */
@Composable
fun NotebookSectionBand(
    title: String,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    isCentered: Boolean = false
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.Start
    ) {
        JournalBaselineHighlightedText(
            text = title,
            style = TextStyle(
                fontFamily = resolveJournalFont(title, isRtl),
                fontSize = if (isArabicScript(title) || isRtl) 14.sp else 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = JournalInk,
                platformStyle = NoFontPadding
            ),
            highlighterColor = highlightColor,
            highlighterAlpha = 0.22f,
            horizontalPadding = 7.dp,
            verticalPadding = 0.dp
        )
    }
}

/**
 * Unified segmented choice control for notebook tabs & settings (Filters, Language, Currency, Security, etc.).
 * Style:
 * - Selected option: Subtle grey background (JournalInk alpha 0.09f) with rounded corners + underline indicator (HighlighterPink)
 * - Non-selected option: Transparent background, muted ink text
 * - Separator between options: Light delicate divider line ("tiret خفيفة")
 */
@Composable
fun <T> NotebookSegmentedControl(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelectOption: (T) -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color = JournalAccent
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val padH = if (options.size > 3) 8.dp else 12.dp
    val spacing = if (options.size > 3) 6.dp else 12.dp

    Row(
        modifier = modifier
            .height(JournalRuleSpacing)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selectedOption
            val isOptionArabic = isArabicScript(label)

            Box(
                modifier = Modifier
                    .height(JournalRuleSpacing)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.RadioButton,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onSelectOption(value)
                        }
                    )
                    .drawBehind {
                        if (isSelected) {
                            val strokeW = 1.1.dp.toPx()
                            val halfStroke = strokeW / 2f
                            val pillHeight = 22.dp.toPx()
                            val pillCenterY = size.height - 5.5.dp.toPx()
                            val topY = pillCenterY - (pillHeight / 2f)
                            drawRoundRect(
                                color = indicatorColor,
                                topLeft = Offset(halfStroke, topY),
                                size = Size(size.width - strokeW, pillHeight),
                                cornerRadius = CornerRadius(6.dp.toPx()),
                                style = Stroke(width = strokeW)
                            )
                        }
                    }
                    .padding(horizontal = padH),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = label,
                    fontFamily = if (isOptionArabic) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isOptionArabic) (if (options.size > 3) 12.5.sp else 13.5.sp) else (if (options.size > 3) 13.sp else 14.sp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) JournalInk else JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }
        }
    }
}

/**
 * Full-width pink "+ Nouveau calcul" primary button on Home.
 * Occupies 1 notebook rule (29dp) with text and pink capsule sitting directly on the blue line.
 */
@Composable
fun NotebookPrimaryActionButton(
    text: String = stringResource(R.string.home_new_calculation),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HighlighterPink.copy(alpha = 0.55f))
                .clickable(
                    role = Role.Button,
                    onClickLabel = text,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "+",
                    fontFamily = PatrickHandFamily,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.offset(y = (-0.5).dp)
                )
                Text(
                    text = text,
                    fontFamily = resolveJournalFont(text, isRtl),
                    fontSize = if (isArabicScript(text) || isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.offset(y = if (isRtl) 0.5.dp else 0.dp)
                )
            }
        }
    }
}

/**
 * Notebook quick-access action button for Calculs on HomeScreen (29dp).
 * Respects the 1-rule spacing grid with clean paper background, ink borders and handwritten text.
 */
@Composable
fun NotebookCalculsActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.home_action_calculs)
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(
                    BorderStroke(0.95.dp, JournalWritingInk.copy(alpha = 0.85f)),
                    RoundedCornerShape(8.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = title,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onClick()
                    }
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🧮",
                        fontSize = 15.sp,
                        modifier = Modifier.offset(y = (-0.5).dp)
                    )
                    Text(
                        text = title,
                        fontFamily = resolveJournalFont(title, isRtl),
                        fontSize = if (isArabicScript(title) || isRtl) 14.sp else 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.offset(y = if (isRtl) 0.5.dp else 0.dp)
                    )
                }

                Text(
                    text = if (isRtl) "←" else "→",
                    fontFamily = PatrickHandFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk.copy(alpha = 0.75f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Quick-access action card for Calculs Hub ("Nouveau calcul", "Rendu de monnaie").
 * Features an accent icon on the start, title & subtitle in the center, and chevron on the end.
 * Exactly 2 notebook rules tall (58dp) with 50dp card height.
 */
@Composable
fun NotebookHubActionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(
                    BorderStroke(0.95.dp, JournalWritingInk.copy(alpha = 0.85f)),
                    RoundedCornerShape(8.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = title,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onClick()
                    }
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    icon()

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            fontFamily = resolveJournalFont(title, isRtl),
                            fontSize = if (isArabicScript(title) || isRtl) 15.sp else 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 12.sp else 12.5.sp,
                            color = JournalMutedInk.copy(alpha = 0.80f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }

                Text(
                    text = if (isRtl) "←" else "→",
                    fontFamily = PatrickHandFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk.copy(alpha = 0.70f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Notebook quick-access action button for Caisse & Rendu de monnaie (29dp).
 * Respects the 1-rule spacing grid with clean paper background, ink borders and handwritten text.
 */
@Composable
fun NotebookCashRegisterActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.cash_register_home_card_title)
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(
                    BorderStroke(0.9.dp, JournalRule.copy(alpha = 0.85f)),
                    RoundedCornerShape(8.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = title,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onClick()
                    }
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🧮",
                        fontSize = 15.sp,
                        modifier = Modifier.offset(y = (-0.5).dp)
                    )
                    Text(
                        text = title,
                        fontFamily = resolveJournalFont(title, isRtl),
                        fontSize = if (isArabicScript(title) || isRtl) 14.sp else 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.offset(y = if (isRtl) 0.5.dp else 0.dp)
                    )
                }

                Text(
                    text = if (isRtl) "←" else "→",
                    fontFamily = PatrickHandFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk.copy(alpha = 0.75f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Quick action button on HomeScreen for Checklist.
 * Respects the 1-rule spacing grid with clean paper background, ink borders and handwritten text.
 */
@Composable
fun NotebookChecklistActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Checklist"
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(
                    BorderStroke(0.9.dp, JournalRule.copy(alpha = 0.85f)),
                    RoundedCornerShape(8.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = title,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onClick()
                    }
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "☑️",
                        fontSize = 15.sp,
                        modifier = Modifier.offset(y = (-0.5).dp)
                    )
                    Text(
                        text = title,
                        fontFamily = resolveJournalFont(title, isRtl),
                        fontSize = if (isArabicScript(title) || isRtl) 14.sp else 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.offset(y = if (isRtl) 0.5.dp else 0.dp)
                    )
                }

                Text(
                    text = if (isRtl) "←" else "→",
                    fontFamily = PatrickHandFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk.copy(alpha = 0.75f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Quick action button on HomeScreen for Notes & Ideas.
 * Respects the 1-rule spacing grid with clean paper background, ink borders and handwritten text.
 */
@Composable
fun NotebookNotesActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Notes & Idées"
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(
                    BorderStroke(0.9.dp, JournalRule.copy(alpha = 0.85f)),
                    RoundedCornerShape(8.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = title,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onClick()
                    }
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📝",
                        fontSize = 15.sp,
                        modifier = Modifier.offset(y = (-0.5).dp)
                    )
                    Text(
                        text = title,
                        fontFamily = resolveJournalFont(title, isRtl),
                        fontSize = if (isArabicScript(title) || isRtl) 14.sp else 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.offset(y = if (isRtl) 0.5.dp else 0.dp)
                    )
                }

                Text(
                    text = if (isRtl) "←" else "→",
                    fontFamily = PatrickHandFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk.copy(alpha = 0.75f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}


/**
 * Standard 2-rule notebook calculation row (58dp).
 * Line 1 (29dp): Bullet dot, Title, Amount, Currency, 3-dots menu icon.
 * Line 2 (29dp): Time · count metadata or search snippet.
 */
@Composable
fun NotebookCalculationRow(
    index: Int,
    title: String,
    totalAmount: String,
    currencySuffix: String,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    subtitle: String = "",
    isPinned: Boolean = false,
    dotColorOverride: Color? = null,
    paymentStatus: String = "PAID",
    calcType: String = "PERSONNEL",
    dueDateEpochMs: Long? = null,
    reminderEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val isLatinSuffix = currencySuffix.contains(Regex("[a-zA-Z]"))

    val rowDotColors = remember {
        listOf(
            Color(0xFF5B9EC9), // Soft Sky Blue
            Color(0xFFE27B97), // Soft Rose Pink
            Color(0xFF7FA85B), // Soft Sage Green
            Color(0xFFE0B038), // Soft Warm Amber
            Color(0xFF9878C8), // Soft Lavender
            Color(0xFFE28862)  // Soft Peach Coral
        )
    }
    val defaultDotColor = rowDotColors[index.coerceAtLeast(0) % rowDotColors.size]
    val dotColor = dotColorOverride ?: defaultDotColor

    val hasSubtitle = subtitle.isNotBlank()
    val dueInfo = remember(paymentStatus, calcType, dueDateEpochMs, reminderEnabled) {
        CreditStatusHelper.computeDueInfo(
            context = context,
            paymentStatus = paymentStatus,
            calcType = calcType,
            dueDateEpochMs = dueDateEpochMs,
            reminderEnabled = reminderEnabled
        )
    }
    val showSecondLine = hasSubtitle || dueInfo != null

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (showSecondLine) JournalRuleSpacing * 2 else JournalRuleSpacing)
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onClick()
                }
            )
    ) {
        // Line 1: Bullet dot + Title on Start; Amount + Currency + 3-dots on End
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Bullet dot + Title
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(7.5.dp)
                        .offset(y = if (isRtl) (-0.5).dp else 0.dp)
                ) {
                    drawCircle(color = dotColor)
                }

                val displayTitle = title.ifBlank { stringResource(R.string.editor_new_title) }
                Text(
                    text = displayTitle,
                    fontFamily = resolveJournalFont(displayTitle, isRtl),
                    fontSize = if (isArabicScript(displayTitle) || isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Subtle connecting line directly on the blue notebook line between Title and Amount
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 6.dp)
                    .drawBehind {
                        val strokeW = 0.85.dp.toPx()
                        val y = size.height
                        drawLine(
                            color = JournalWritingInk.copy(alpha = 0.28f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                        )
                    }
            )

            // End: Amount + Suffix (Color-coded for Crédit vs Personnel) + 3 dots (⋮)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = totalAmount,
                    fontFamily = PatrickHandFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                val isCredit = calcType == "CREDIT"
                val isUnpaid = paymentStatus == "UNPAID"
                val suffixColor = when {
                    isCredit && isUnpaid -> Color(0xFFDC2626) // Vivid red for unpaid credit
                    isCredit && !isUnpaid -> Color(0xFF16A34A) // Fresh green for settled credit
                    else -> JournalMutedInk // Normal muted ink for personal
                }
                val suffixWeight = if (isCredit) FontWeight.Bold else FontWeight.Normal

                Text(
                    text = currencySuffix,
                    fontFamily = if (isLatinSuffix) PatrickHandFamily else TajawalFamily,
                    fontSize = if (isLatinSuffix) 13.5.sp else 12.sp,
                    fontWeight = suffixWeight,
                    color = suffixColor,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = JournalRuleSpacing)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.cd_more_options),
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onMoreClick()
                            }
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = null,
                        tint = JournalMutedInk,
                        size = 18.dp,
                        modifier = Modifier.offset(y = 2.0.dp)
                    )
                }
            }
        }

        // Line 2: Credit status badge + Subtitle metadata on Rule 2 (if present)
        if (showSecondLine) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(start = 29.5.dp, end = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dueInfo != null) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-3).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(dueInfo.badgeColor)
                            .border(0.8.dp, dueInfo.textColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = dueInfo.displayText,
                                fontFamily = resolveJournalFont(dueInfo.displayText, isRtl),
                                fontSize = if (isRtl) 11.sp else 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = dueInfo.textColor,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                            if (dueInfo.hasReminder) {
                                Text(
                                    text = "🔔",
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                if (hasSubtitle) {
                    Text(
                        text = subtitle,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 13.5.sp else 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = JournalMutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        style = TextStyle(
                            platformStyle = NoFontPadding,
                            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .journalBaselineOnRule()
                    )
                }
            }
        }
    }
}

/**
 * Date group container with date header and subtle vertical grouping guide
 * connecting all rows strictly within this date group.
 */
@Composable
fun NotebookDateGroupBlock(
    header: String,
    calculations: List<CalculationWithItems>,
    onOpenCalculation: (String) -> Unit,
    onMoreClick: (CalculationWithItems) -> Unit,
    searchQuery: String = "",
    pinnedCalculationIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val todayText = stringResource(R.string.date_today)
    val yesterdayText = stringResource(R.string.date_yesterday)
    val timelineStyle = getDateTimelineStyle(header, todayText, yesterdayText)

    Column(modifier = modifier.fillMaxWidth()) {
        // Line 1: Date separator band (29dp)
        JournalDateRuleBand(title = header)

        // Calculation rows column with vertical grouping guide
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (calculations.size > 1) {
                        val strokeW = 1.5.dp.toPx()
                        val guideX = if (isRtl) size.width - 17.75.dp.toPx() else 17.75.dp.toPx()
                        val rowHeightPx = JournalRuleSpacing.toPx() // 29dp (single notebook rule)
                        val dotCenterY = 25.25.dp.toPx()
                        val startY = dotCenterY
                        val endY = (calculations.size - 1) * rowHeightPx + dotCenterY

                        drawLine(
                            color = timelineStyle.dotColor.copy(alpha = 0.40f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }
        ) {
            calculations.forEachIndexed { idx, calc ->
                val currency = runCatching { MoneyUnit.valueOf(calc.calculation.currency) }.getOrDefault(MoneyUnit.DIRHAM)
                val totalFormatted = JournalLedgerManager.formatTotal(calc.totalCentimes, currency)
                val currencySuffix = if (currency == MoneyUnit.DIRHAM) {
                    stringResource(R.string.currency_dirham)
                } else {
                    stringResource(R.string.currency_rial)
                }

                NotebookCalculationRow(
                    index = idx,
                    title = calc.calculation.title,
                    totalAmount = totalFormatted,
                    currencySuffix = currencySuffix,
                    isPinned = false,
                    dotColorOverride = timelineStyle.dotColor,
                    paymentStatus = calc.calculation.paymentStatus,
                    calcType = calc.calculation.calcType,
                    dueDateEpochMs = calc.calculation.dueDateEpochMs,
                    reminderEnabled = calc.calculation.reminderEnabled,
                    onClick = { onOpenCalculation(calc.calculation.id) },
                    onMoreClick = { onMoreClick(calc) }
                )
            }
        }
    }
}

/**
 * Unified lightweight search field integrated with the notebook ruled grid.
 */
@Composable
fun NotebookSearchField(
    query: String,
    onQueryChange: ((String) -> Unit)?,
    placeholder: String = stringResource(R.string.home_search_placeholder),
    onOpenCalendar: (() -> Unit)? = null,
    isDateFiltered: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    JournalInlineSearchRow(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = placeholder,
        onOpenCalendar = onOpenCalendar,
        isDateFiltered = isDateFiltered,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Notebook Category / Section Badge with unified seamless path (Background "X"):
 * Rounded tab/badge on start seamlessly joined with a 5dp thick ruled line extending across the page width.
 */
@Composable
fun JournalSectionBadge(
    title: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val textStyle = TextStyle(
        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
        fontSize = if (isRtl) 14.5.sp else 15.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = NoFontPadding
    )
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(title, textStyle) {
        textMeasurer.measure(AnnotatedString(title), textStyle)
    }
    val density = LocalDensity.current
    val badgeWidthDp = with(density) {
        textLayoutResult.size.width.toDp() + 24.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(start = 14.dp, end = 0.dp)
            .drawBehind {
                val badgeW = badgeWidthDp.toPx().coerceAtMost(size.width - 16.dp.toPx())
                val totalW = size.width
                val h = size.height
                val r = 6.dp.toPx()
                val lineH = 5.dp.toPx()
                val filletR = 2.5.dp.toPx()

                val unifiedPath = Path().apply {
                    if (!isRtl) {
                        moveTo(0f, r)
                        quadraticTo(0f, 0f, r, 0f)
                        lineTo(badgeW - r, 0f)
                        quadraticTo(badgeW, 0f, badgeW, r)
                        lineTo(badgeW, h - lineH - filletR)
                        quadraticTo(badgeW, h - lineH, badgeW + filletR, h - lineH)
                        lineTo(totalW, h - lineH)
                        lineTo(totalW, h)
                        lineTo(r, h)
                        quadraticTo(0f, h, 0f, h - r)
                        close()
                    } else {
                        val badgeStart = totalW - badgeW
                        moveTo(totalW, r)
                        quadraticTo(totalW, 0f, totalW - r, 0f)
                        lineTo(badgeStart + r, 0f)
                        quadraticTo(badgeStart, 0f, badgeStart, r)
                        lineTo(badgeStart, h - lineH - filletR)
                        quadraticTo(badgeStart, h - lineH, badgeStart - filletR, h - lineH)
                        lineTo(0f, h - lineH)
                        lineTo(0f, h)
                        lineTo(totalW - r, h)
                        quadraticTo(totalW, h, totalW, h - r)
                        close()
                    }
                }

                drawPath(
                    path = unifiedPath,
                    color = badgeColor
                )
            },
        contentAlignment = if (isRtl) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(badgeWidthDp)
                    .height(JournalRuleSpacing),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * 2-line upcoming feature row matching Mockup 3 (Settings Screen).
 * Line 1 (29dp): Feature title on start, clock sketch icon on end.
 * Line 2 (29dp): Feature description on start, "قريباً" / "Bientôt" on end.
 */
@Composable
fun JournalUpcomingFeatureRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2) // exactly 58dp = 2 ruled lines
            .padding(horizontal = 14.dp)
    ) {
        // Line 1: Title on Start, Clock icon on End
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 15.5.sp else 16.5.sp,
                fontWeight = FontWeight.Normal,
                color = JournalInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )

            HisabiSketchIcon(
                symbol = HisabiSymbol.Clock,
                contentDescription = null,
                tint = JournalMutedInk,
                size = 17.dp,
                modifier = Modifier.offset(y = 1.0.dp)
            )
        }

        // Line 2: Description on Start, "قريباً" on End
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = description,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 11.5.sp else 12.sp,
                fontWeight = FontWeight.Light,
                color = JournalMutedInk.copy(alpha = 0.75f),
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )

            Text(
                text = stringResource(R.string.settings_upcoming_badge),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 11.5.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                color = JournalMutedInk.copy(alpha = 0.75f),
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }
    }
}

/**
 * French bullet-journal total result band:
 * - Centered yellow highlighter pill (~66% width) with "TOTAL" / "المجموع" on start and amount on end
 * - Double pink pen underline centered beneath it
 * - Clickable to open denomination breakdown when valid and > 0.
 */
@Composable
fun JournalTotalResultBand(
    amount: String,
    suffix: String,
    hasInvalidRows: Boolean,
    canBreakdown: Boolean,
    onShowBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clickModifier = if (canBreakdown) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = stringResource(R.string.editor_breakdown_money),
            onClick = onShowBreakdown
        )
    } else {
        Modifier
    }

    val totalColor = if (hasInvalidRows) ColorCoral else JournalInk
    val totalLabel = stringResource(R.string.share_total_label)
    val isArabic = isArabicScript(totalLabel)
    val isLatinSuffix = suffix.contains(Regex("[a-zA-Z]"))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.66f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HighlighterYellow.copy(alpha = 0.35f))
                    .then(clickModifier)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .semantics { testTag = "tag_total_result_band" },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = totalLabel,
                        fontFamily = resolveJournalFont(totalLabel, isArabic),
                        fontSize = if (isArabic) 15.sp else 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = amount,
                            fontFamily = PatrickHandFamily,
                            fontSize = 18.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = totalColor,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )

                        Text(
                            text = suffix,
                            fontFamily = if (isLatinSuffix) PatrickHandFamily else TajawalFamily,
                            fontSize = if (isLatinSuffix) 13.5.sp else 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.5.dp))

            // Double Pink Pen Underline matching the card width exactly as in the shared receipt image
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
            ) {
                val strokeW = 1.6.dp.toPx()
                val pinkColor = HighlighterPink.copy(alpha = 0.88f)

                // Upper Line: spans nearly full width of the box (inset by 8.dp on each side)
                val line1Start = 8.dp.toPx()
                val line1End = size.width - 8.dp.toPx()
                val y1 = 2.dp.toPx()
                drawLine(
                    color = pinkColor,
                    start = Offset(line1Start, y1),
                    end = Offset(line1End, y1),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Lower Line: visibly stepped underline (inset by 18.dp on each side)
                val line2Start = 18.dp.toPx()
                val line2End = size.width - 18.dp.toPx()
                val y2 = 6.2.dp.toPx()
                drawLine(
                    color = pinkColor,
                    start = Offset(line2Start, y2),
                    end = Offset(line2End, y2),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            if (canBreakdown) {
                Spacer(modifier = Modifier.height(6.dp))
                val breakdownBtnLabel = stringResource(R.string.editor_breakdown_button)
                val isBreakdownArabic = isArabicScript(breakdownBtnLabel)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(JournalPaper)
                        .border(
                            BorderStroke(0.9.dp, JournalRule.copy(alpha = 0.75f)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(role = Role.Button, onClick = onShowBreakdown)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💵",
                        fontSize = 14.sp
                    )
                    Text(
                        text = breakdownBtnLabel,
                        fontFamily = resolveJournalFont(breakdownBtnLabel, isBreakdownArabic),
                        fontSize = if (isBreakdownArabic) 13.sp else 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}

/**
 * Journal Compact Numeric Dock:
 * - 4 columns x 3 rows:
 *   1 | 2 | 3 | ⌫
 *   4 | 5 | 6 | .
 *   7 | 8 | 9 | 0
 * - "ABC" switch button on the left of top handle bar
 * - Collapse / Expand handle
 * - Extends behind system navigation bar with no gap
 */
/**
 * Shared Surface for custom in-app keyboard docks.
 */
@Composable
fun JournalKeyboardDockSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = JournalDockBg,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp),
            content = content
        )
    }
}

/**
 * Shared Handle Bar for custom keyboards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalKeyboardHandleBar(
    leftText: String,
    onLeftClick: () -> Unit,
    onLeftLongClick: (() -> Unit)? = null,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    centerContent: (@Composable () -> Unit)? = null,
    rightContent: @Composable () -> Unit = { Spacer(modifier = Modifier.size(36.dp)) },
    leftTestTag: String? = null
) {
    val leftClickLabel = when (leftText) {
        "ABC" -> "Basculer vers le clavier texte"
        "123" -> "Basculer vers le clavier numérique"
        else -> "Changer de langue"
    }

    val barHeight = if (expanded) 36.dp else 32.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Slot: Switch mode / language
        val leftClickMod = if (onLeftLongClick != null) {
            Modifier.combinedClickable(
                role = Role.Button,
                onClickLabel = leftClickLabel,
                onClick = onLeftClick,
                onLongClick = onLeftLongClick
            )
        } else {
            Modifier.clickable(
                role = Role.Button,
                onClickLabel = leftClickLabel,
                onClick = onLeftClick
            )
        }

        Box(
            modifier = Modifier
                .widthIn(min = 40.dp)
                .fillMaxHeight()
                .then(leftClickMod)
                .then(if (leftTestTag != null) Modifier.semantics { testTag = leftTestTag } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leftText,
                style = TextStyle(
                    fontFamily = if (leftText == "ع") TajawalFamily else JournalHandFamily,
                    fontSize = if (leftText == "ع") 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
            )
        }

        val centerClickMod = Modifier.clickable(
            role = Role.Button,
            onClickLabel = if (expanded) "Réduire le clavier" else "Développer le clavier",
            onClick = onToggleExpand
        )

        if (centerContent != null) {
            // Flexible center content (e.g. Quick Emoji Bar)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                centerContent()
            }

            // Dedicated Chevron Toggle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .fillMaxHeight()
                    .then(centerClickMod),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(16.dp, 9.dp)) {
                    val strokeW = 1.4.dp.toPx()
                    val inkColor = JournalInk.copy(alpha = 0.85f)
                    if (expanded) {
                        drawLine(
                            color = inkColor,
                            start = Offset(1.dp.toPx(), 2.dp.toPx()),
                            end = Offset(size.width / 2f, size.height - 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = inkColor,
                            start = Offset(size.width / 2f, size.height - 2.dp.toPx()),
                            end = Offset(size.width - 1.dp.toPx(), 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    } else {
                        drawLine(
                            color = inkColor,
                            start = Offset(1.dp.toPx(), size.height - 2.dp.toPx()),
                            end = Offset(size.width / 2f, 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = inkColor,
                            start = Offset(size.width / 2f, 2.dp.toPx()),
                            end = Offset(size.width - 1.dp.toPx(), size.height - 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Right Slot
            Box(
                modifier = Modifier
                    .widthIn(min = 36.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                rightContent()
            }
        } else {
            // Center Slot: Geometrically centered Chevron
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(centerClickMod),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(18.dp, 10.dp)) {
                    val strokeW = 1.4.dp.toPx()
                    val inkColor = JournalInk.copy(alpha = 0.85f)
                    if (expanded) {
                        // Chevron Down
                        drawLine(
                            color = inkColor,
                            start = Offset(1.dp.toPx(), 2.dp.toPx()),
                            end = Offset(size.width / 2f, size.height - 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = inkColor,
                            start = Offset(size.width / 2f, size.height - 2.dp.toPx()),
                            end = Offset(size.width - 1.dp.toPx(), 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    } else {
                        // Chevron Up
                        drawLine(
                            color = inkColor,
                            start = Offset(1.dp.toPx(), size.height - 2.dp.toPx()),
                            end = Offset(size.width / 2f, 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = inkColor,
                            start = Offset(size.width / 2f, 2.dp.toPx()),
                            end = Offset(size.width - 1.dp.toPx(), size.height - 2.dp.toPx()),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Right Slot
            Box(
                modifier = Modifier
                    .widthIn(min = 40.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                rightContent()
            }
        }
    }
}

/**
 * Paper-style Language Chooser Popup Modal.
 */
@Composable
fun JournalLanguageChooserPopup(
    currentLanguage: JournalKeyboardLanguage,
    onSelectLanguage: (JournalKeyboardLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(16, -140),
        onDismissRequest = onDismiss
    ) {
        Surface(
            color = JournalPaper,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, JournalRule.copy(alpha = 0.75f)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 6.dp)
                    .width(130.dp)
            ) {
                JournalKeyboardLanguage.values().forEach { lang ->
                    val isSelected = lang == currentLanguage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clickable {
                                onSelectLanguage(lang)
                                onDismiss()
                            }
                            .drawBehind {
                                if (isSelected) {
                                    drawRect(HighlighterPink.copy(alpha = 0.28f))
                                }
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = lang.displayName,
                            style = TextStyle(
                                fontFamily = if (lang == JournalKeyboardLanguage.ARABIC) TajawalFamily else JournalHandFamily,
                                fontSize = if (lang == JournalKeyboardLanguage.ARABIC) 18.sp else 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = JournalInk
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Anchored Alternatives / Accents Popup using real PopupPositionProvider.
 */
@Composable
fun JournalAnchoredPopup(
    anchorBounds: IntRect,
    alternatives: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val positionProvider = remember(anchorBounds) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBoundsParam: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                return JournalKeyboardController.calculateAnchoredPopupPosition(
                    anchorBounds = anchorBounds,
                    windowSize = windowSize,
                    popupContentSize = popupContentSize
                )
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss
    ) {
        Surface(
            color = JournalPaper,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, JournalRule.copy(alpha = 0.65f)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                alternatives.forEach { alt ->
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 44.dp)
                            .clickable(role = Role.Button) {
                                onSelect(alt)
                                onDismiss()
                            }
                            .drawBehind {
                                if (alt.startsWith("é") || alt.startsWith("É") || alt.startsWith("أ") || alt.startsWith("à")) {
                                    drawRect(HighlighterPink.copy(alpha = 0.22f))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alt,
                            style = TextStyle(
                                fontFamily = if (alt.any { it in '\u0600'..'\u06FF' }) TajawalFamily else JournalHandFamily,
                                fontSize = if (alt.any { it in '\u0600'..'\u06FF' }) 22.sp else 23.sp,
                                color = JournalInk
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Journal Compact Numeric Dock:
 * - 4 columns x 3 rows:
 *   1 | 2 | 3 | ⌫
 *   4 | 5 | 6 | .
 *   7 | 8 | 9 | 0
 * - Shared JournalKeyboardDockSurface & JournalKeyboardHandleBar with "ABC"
 * - Soft pencil dividers instead of heavy black box
 */
@Composable
fun JournalCompactNumericDock(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onKey: (String) -> Unit,
    onSwitchToTextMode: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val backspaceController = remember(coroutineScope, onKey) {
        BackspaceRepeatController(
            scope = coroutineScope,
            onDelete = { onKey("⌫") }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            backspaceController.cancel()
        }
    }

    JournalKeyboardDockSurface(
        modifier = modifier.semantics { testTag = "tag_compact_keypad" }
    ) {
        // Handle bar
        JournalKeyboardHandleBar(
            leftText = "ABC",
            onLeftClick = {
                backspaceController.cancel()
                onSwitchToTextMode()
            },
            expanded = expanded,
            onToggleExpand = {
                backspaceController.cancel()
                onToggleExpand()
            },
            rightContent = {
                if (onConfirm != null) {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 36.dp)
                            .fillMaxHeight()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Confirmer la saisie",
                                onClick = {
                                    backspaceController.cancel()
                                    onConfirm()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        NotebookCheckmark(size = 18.dp)
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            },
            leftTestTag = "tag_switch_to_text_key"
        )

        if (expanded) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .padding(horizontal = 6.dp)
                        .drawBehind {
                            val strokeW = 0.65.dp.toPx()
                            val gridLineColor = JournalRule.copy(alpha = 0.45f)

                            // 2 Internal horizontal dividers
                            val rowH = size.height / 3f
                            for (i in 1..2) {
                                val y = rowH * i
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = strokeW
                                )
                            }

                            // 3 Internal vertical dividers
                            val colW = size.width / 4f
                            for (i in 1..3) {
                                val x = colW * i
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = strokeW
                                )
                            }
                        }
                ) {
                    // Row 1: 1 | 2 | 3 | ⌫
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        JournalKeyCell("1", Modifier.weight(1f)) { onKey("1") }
                        JournalKeyCell("2", Modifier.weight(1f)) { onKey("2") }
                        JournalKeyCell("3", Modifier.weight(1f)) { onKey("3") }
                        JournalBackspaceKeyCell(
                            modifier = Modifier.weight(1f),
                            controller = backspaceController
                        )
                    }

                    // Row 2: 4 | 5 | 6 | .
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        JournalKeyCell("4", Modifier.weight(1f)) { onKey("4") }
                        JournalKeyCell("5", Modifier.weight(1f)) { onKey("5") }
                        JournalKeyCell("6", Modifier.weight(1f)) { onKey("6") }
                        JournalKeyCell(".", Modifier.weight(1f)) { onKey(".") }
                    }

                    // Row 3: 7 | 8 | 9 | 0
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        JournalKeyCell("7", Modifier.weight(1f)) { onKey("7") }
                        JournalKeyCell("8", Modifier.weight(1f)) { onKey("8") }
                        JournalKeyCell("9", Modifier.weight(1f)) { onKey("9") }
                        JournalKeyCell("0", Modifier.weight(1f)) { onKey("0") }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Custom In-App Text Keyboard Supporting French (AZERTY), English (QWERTY), and Arabic.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalTextKeyboardDock(
    language: JournalKeyboardLanguage,
    shiftMode: JournalShiftMode,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onCycleLanguage: () -> Unit,
    onSelectLanguage: (JournalKeyboardLanguage) -> Unit,
    onToggleShift: () -> Unit,
    onInsertText: (String) -> Unit,
    onBackspace: () -> Unit,
    onSwitchToNumericMode: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageChooser by remember { mutableStateOf(false) }
    var isEmojiMode by remember { mutableStateOf(false) }
    var activePopupAnchor by remember { mutableStateOf<IntRect?>(null) }
    var activeAlternatives by remember { mutableStateOf<List<String>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val backspaceController = remember(coroutineScope, onBackspace) {
        BackspaceRepeatController(
            scope = coroutineScope,
            onDelete = onBackspace
        )
    }

    DisposableEffect(language, expanded) {
        onDispose {
            backspaceController.cancel()
            activePopupAnchor = null
            showLanguageChooser = false
            isEmojiMode = false
        }
    }

    JournalKeyboardDockSurface(
        modifier = modifier.semantics { testTag = "tag_text_keypad" }
    ) {
        // Handle bar
        if (expanded) {
            JournalKeyboardHandleBar(
                leftText = if (isEmojiMode) "ABC" else language.label,
                onLeftClick = {
                    if (isEmojiMode) {
                        isEmojiMode = false
                    } else {
                        backspaceController.cancel()
                        activePopupAnchor = null
                        onCycleLanguage()
                    }
                },
                onLeftLongClick = if (!isEmojiMode) {
                    {
                        backspaceController.cancel()
                        activePopupAnchor = null
                        showLanguageChooser = true
                    }
                } else null,
                expanded = true,
                onToggleExpand = {
                    backspaceController.cancel()
                    activePopupAnchor = null
                    onToggleExpand()
                },
                centerContent = null,
                rightContent = {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 36.dp)
                            .fillMaxHeight()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Confirmer la saisie",
                                onClick = {
                                    backspaceController.cancel()
                                    activePopupAnchor = null
                                    onConfirm()
                                }
                            )
                            .semantics { testTag = "tag_keyboard_confirm_top" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK",
                            style = TextStyle(
                                fontFamily = JournalHandFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalActionConfirm
                            )
                        )
                    }
                },
                leftTestTag = if (isEmojiMode) "tag_emoji_to_abc_top" else "tag_language_key_top"
            )
        } else {
            JournalKeyboardHandleBar(
                leftText = "123",
                onLeftClick = {
                    backspaceController.cancel()
                    activePopupAnchor = null
                    onSwitchToNumericMode()
                },
                expanded = false,
                onToggleExpand = {
                    backspaceController.cancel()
                    activePopupAnchor = null
                    onToggleExpand()
                },
                rightContent = {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 36.dp)
                            .fillMaxHeight()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Confirmer la saisie",
                                onClick = {
                                    backspaceController.cancel()
                                    onConfirm()
                                }
                            )
                            .semantics { testTag = "tag_confirm_key_top" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK ✓",
                            style = TextStyle(
                                fontFamily = JournalHandFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalActionConfirm
                            )
                        )
                    }
                },
                leftTestTag = "tag_switch_to_num_key_top"
            )
        }

        if (expanded) {
            if (isEmojiMode) {
                JournalEmojiKeyboardPanel(
                    onInsertEmoji = { emoji -> onInsertText(emoji) },
                    onSwitchToAlphabet = { isEmojiMode = false },
                    onSwitchToNumericMode = {
                        isEmojiMode = false
                        onSwitchToNumericMode()
                    },
                    onBackspace = onBackspace,
                    onConfirm = onConfirm,
                    backspaceController = backspaceController,
                    spaceLabel = when (language) {
                        JournalKeyboardLanguage.FRENCH -> "espace"
                        JournalKeyboardLanguage.ENGLISH -> "space"
                        JournalKeyboardLanguage.ARABIC -> "مسافة"
                    }
                )
            } else {
                val rows = remember(language, shiftMode) {
                    when (language) {
                        JournalKeyboardLanguage.FRENCH -> JournalKeyboardController.getFrenchRows(shiftMode)
                        JournalKeyboardLanguage.ENGLISH -> JournalKeyboardController.getEnglishRows(shiftMode)
                        JournalKeyboardLanguage.ARABIC -> JournalKeyboardController.getArabicRows()
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        // Rows 0 to 3 (Number Row + Letter Rows)
                        for (rowIndex in 0 until (rows.size - 1)) {
                            val row = rows[rowIndex]
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = if (rowIndex == 2 && language == JournalKeyboardLanguage.ENGLISH) 6.dp else 0.dp)
                                 ) {
                                    row.forEachIndexed { keyIndex, keySpec ->
                                        JournalKeySpecCell(
                                            spec = keySpec,
                                            language = language,
                                            shiftMode = shiftMode,
                                            showVerticalDivider = keyIndex < row.size - 1,
                                            modifier = Modifier.weight(keySpec.flexWeight),
                                            onTap = {
                                                if (keySpec.isShift) {
                                                    onToggleShift()
                                                } else {
                                                    onInsertText(keySpec.output)
                                                }
                                            },
                                            onLongPress = { bounds ->
                                                if (keySpec.alternatives.isNotEmpty()) {
                                                    activePopupAnchor = bounds
                                                    activeAlternatives = keySpec.alternatives
                                                }
                                            },
                                            backspaceController = if (keySpec.isBackspace) backspaceController else null
                                        )
                                    }
                                }
                            }
                        }

                        // Row 4 (Utility Row: 123 / Emoji / Space / Punctuation / OK)
                        val utilityRow = rows.last()
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                utilityRow.forEachIndexed { keyIndex, keySpec ->
                                    JournalKeySpecCell(
                                        spec = keySpec,
                                        language = language,
                                        shiftMode = shiftMode,
                                        showVerticalDivider = keyIndex < utilityRow.size - 1,
                                        modifier = Modifier.weight(keySpec.flexWeight),
                                        onTap = {
                                            when {
                                                keySpec.isModeSwitch -> onSwitchToNumericMode()
                                                keySpec.isEmojiSwitch -> isEmojiMode = true
                                                keySpec.isConfirm -> onConfirm()
                                                else -> onInsertText(keySpec.output)
                                            }
                                        },
                                        onLongPress = null,
                                        backspaceController = null
                                    )
                                }
                            }
                        }
                    }

                    // Anchored Popup for Alternatives / Accents
                    if (activePopupAnchor != null && activeAlternatives.isNotEmpty()) {
                        JournalAnchoredPopup(
                            anchorBounds = activePopupAnchor!!,
                            alternatives = activeAlternatives,
                            onSelect = { selectedAlt ->
                                onInsertText(selectedAlt)
                                activePopupAnchor = null
                            },
                            onDismiss = { activePopupAnchor = null }
                        )
                    }

                    // Paper-style Language Chooser Popup
                    if (showLanguageChooser) {
                        JournalLanguageChooserPopup(
                            currentLanguage = language,
                            onSelectLanguage = { selected ->
                                onSelectLanguage(selected)
                                showLanguageChooser = false
                            },
                            onDismiss = { showLanguageChooser = false }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Key cell driven by JournalKeySpec with dedicated gesture policies for Backspace, Shift, and Alternative keys.
 */
@Composable
private fun JournalKeySpecCell(
    spec: JournalKeySpec,
    language: JournalKeyboardLanguage,
    shiftMode: JournalShiftMode,
    modifier: Modifier = Modifier,
    showVerticalDivider: Boolean = false,
    onTap: () -> Unit,
    onLongPress: ((IntRect) -> Unit)?,
    backspaceController: BackspaceRepeatController?
) {
    var keyBounds by remember { mutableStateOf<IntRect?>(null) }
    val haptic = LocalHapticFeedback.current

    val keyTestTag = when {
        spec.isBackspace -> if (language == JournalKeyboardLanguage.ARABIC) "tag_arabic_backspace" else "tag_key_backspace"
        spec.isSpace -> if (language == JournalKeyboardLanguage.ARABIC) "tag_arabic_space" else "tag_key_space"
        spec.isConfirm -> "tag_keyboard_confirm"
        spec.isShift -> "tag_key_shift"
        spec.isModeSwitch -> "tag_switch_to_num_key_top"
        spec.isEmojiSwitch -> "tag_key_emoji_switch"
        spec.label.length == 1 && spec.label[0].isDigit() -> "tag_key_digit_${spec.output}"
        language == JournalKeyboardLanguage.FRENCH -> "tag_key_fr_${spec.output.lowercase()}"
        language == JournalKeyboardLanguage.ENGLISH -> "tag_key_en_${spec.output.lowercase()}"
        language == JournalKeyboardLanguage.ARABIC -> "tag_key_ar_${spec.output}"
        else -> null
    }

    @OptIn(ExperimentalFoundationApi::class)
    val gestureModifier = when {
        spec.isBackspace && backspaceController != null -> {
            val backspaceInteractionSource = remember { MutableInteractionSource() }
            Modifier
                .indication(backspaceInteractionSource, LocalIndication.current)
                .pointerInput(backspaceController) {
                    detectTapGestures(
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            backspaceInteractionSource.emit(press)
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (_: Exception) {}
                            backspaceController.onPointerDown()
                            val released = tryAwaitRelease()
                            if (released) {
                                backspaceInteractionSource.emit(PressInteraction.Release(press))
                                backspaceController.onPointerUp()
                            } else {
                                backspaceInteractionSource.emit(PressInteraction.Cancel(press))
                                backspaceController.cancel()
                            }
                        }
                    )
                }
        }
        else -> {
            Modifier.combinedClickable(
                role = Role.Button,
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                    onTap()
                },
                onLongClick = if (onLongPress != null && spec.alternatives.isNotEmpty()) {
                    {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (_: Exception) {}
                        keyBounds?.let { onLongPress(it) }
                    }
                } else null
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .semantics {
                if (keyTestTag != null) {
                    testTag = keyTestTag
                }
            }
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                keyBounds = IntRect(
                    left = bounds.left.toInt(),
                    top = bounds.top.toInt(),
                    right = bounds.right.toInt(),
                    bottom = bounds.bottom.toInt()
                )
            }
            .then(gestureModifier)
            .drawBehind {
                val strokeW = 0.6.dp.toPx()
                val dividerColor = JournalMutedInk.copy(alpha = 0.18f)
                // Subtle pencil bottom line
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeW
                )

                // Subtle vertical divider between keys (charta sghira w bahta)
                if (showVerticalDivider) {
                    val tickStrokeW = 0.65.dp.toPx()
                    val tickColor = JournalMutedInk.copy(alpha = 0.18f)
                    val startY = size.height * 0.32f
                    val endY = size.height * 0.68f
                    val x = size.width - 0.5f
                    drawLine(
                        color = tickColor,
                        start = Offset(x, startY),
                        end = Offset(x, endY),
                        strokeWidth = tickStrokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Shift mode indicator (Only for Latin Shift)
                if (spec.isShift && language != JournalKeyboardLanguage.ARABIC) {
                    when (shiftMode) {
                        JournalShiftMode.ONE_SHOT -> {
                            drawCircle(
                                color = HighlighterPink.copy(alpha = 0.40f),
                                radius = size.minDimension * 0.35f,
                                center = Offset(size.width / 2f, size.height / 2f)
                            )
                        }
                        JournalShiftMode.CAPS_LOCK -> {
                            drawCircle(
                                color = HighlighterPink.copy(alpha = 0.55f),
                                radius = size.minDimension * 0.38f,
                                center = Offset(size.width / 2f, size.height / 2f)
                            )
                            // Underline bar for Caps Lock
                            drawLine(
                                color = JournalInk,
                                start = Offset(size.width * 0.28f, size.height * 0.82f),
                                end = Offset(size.width * 0.72f, size.height * 0.82f),
                                strokeWidth = 1.6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        JournalShiftMode.OFF -> {}
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Top-right subtle indicator dot for keys with alternatives
        if (spec.alternatives.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 2.5.dp, end = 2.5.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(color = JournalMutedInk.copy(alpha = 0.40f), shape = CircleShape)
                )
            }
        }

        if (spec.isBackspace) {
            Canvas(modifier = Modifier.size(22.dp, 16.dp)) {
                val strokeW = 1.15.dp.toPx()
                val ink = JournalInk.copy(alpha = 0.9f)
                val w = size.width
                val h = size.height

                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.32f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(w * 0.32f, h)
                    lineTo(0f, h / 2f)
                    close()
                }
                drawPath(p, color = ink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))

                val cx = w * 0.64f
                val cy = h / 2f
                val d = 3.0.dp.toPx()
                drawLine(ink, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeW, StrokeCap.Round)
                drawLine(ink, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeW, StrokeCap.Round)
            }
        } else if (spec.isShift) {
            Canvas(modifier = Modifier.size(18.dp, 18.dp)) {
                val strokeW = 1.4.dp.toPx()
                val ink = if (shiftMode != JournalShiftMode.OFF) JournalInk else JournalInk.copy(alpha = 0.85f)
                val w = size.width
                val h = size.height

                // Arrow head
                drawLine(ink, Offset(w * 0.15f, h * 0.48f), Offset(w * 0.5f, h * 0.15f), strokeW, StrokeCap.Round)
                drawLine(ink, Offset(w * 0.85f, h * 0.48f), Offset(w * 0.5f, h * 0.15f), strokeW, StrokeCap.Round)
                // Arrow stem
                drawLine(ink, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), strokeW, StrokeCap.Round)
            }
        } else {
            val isArabicChar = spec.label.any { it in '\u0600'..'\u06FF' }
            val font = if (spec.isEmojiSwitch) null else if (isArabicChar) TajawalFamily else JournalHandFamily
            val textColor = when {
                spec.isConfirm -> JournalActionConfirm
                spec.isSpace -> JournalMutedInk
                else -> JournalInk
            }
            val fontSize = when {
                spec.isEmojiSwitch -> 17.5.sp
                spec.isSpace -> if (isArabicChar) 16.sp else 18.sp
                spec.isConfirm -> 17.sp
                spec.label == "123" -> 18.sp
                spec.label == "." || spec.label == "،" -> 22.sp
                spec.label.length == 1 && spec.label[0].isDigit() -> 19.sp
                isArabicChar -> 19.5.sp
                else -> 21.sp
            }
            val fontWeight = when {
                spec.isConfirm || spec.label == "123" -> FontWeight.Bold
                else -> FontWeight.Normal
            }

            Text(
                text = spec.label,
                style = TextStyle(
                    fontFamily = font,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = textColor
                )
            )
        }
    }
}

/**
 * Dedicated Backspace Key Cell for the Numeric keypad.
 */
@Composable
private fun JournalBackspaceKeyCell(
    modifier: Modifier = Modifier,
    controller: BackspaceRepeatController
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(controller) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } catch (_: Exception) {}
                        controller.onPointerDown()
                        val released = tryAwaitRelease()
                        if (released) {
                            interactionSource.emit(PressInteraction.Release(press))
                            controller.onPointerUp()
                        } else {
                            interactionSource.emit(PressInteraction.Cancel(press))
                            controller.cancel()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp, 16.dp)) {
            val strokeW = 1.15.dp.toPx()
            val ink = JournalInk.copy(alpha = 0.9f)
            val w = size.width
            val h = size.height

            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.32f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(w * 0.32f, h)
                lineTo(0f, h / 2f)
                close()
            }
            drawPath(p, color = ink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))

            val cx = w * 0.64f
            val cy = h / 2f
            val d = 3.0.dp.toPx()
            drawLine(ink, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeW, StrokeCap.Round)
            drawLine(ink, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeW, StrokeCap.Round)
        }
    }
}

/**
 * Contextual calculator popup modal for calculation editor and ledger screens.
 * Redesigned for compact notebook carnet elegance:
 * - Wrap-content height eliminating unnecessary empty space
 * - Title row with hand-drawn mini calculator sketch + close button
 * - Display area: Expression on top ("l2ar9am dial lcalcul lfo9"), blue notebook ruled divider line, Total on bottom ("totoal lta7t") with live preview
 * - Keypad: 5x4 grid with (, ), C, digits, backspace, and pastel operator dabs
 * - Full-width 'Confirmer' action with pink marker stroke
 */
@Composable
fun JournalCalculatorPopup(
    expression: String,
    result: String,
    hasError: Boolean,
    canConfirm: Boolean,
    onKey: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner card modal
        Surface(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // prevent closing when tapping card surface
                )
                .semantics { testTag = "tag_popup_container" },
            color = JournalPaper,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, JournalInk.copy(alpha = 0.75f)),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Subtle tactile paper grain
                Canvas(modifier = Modifier.matchParentSize()) {
                    val dotColor = JournalInk.copy(alpha = 0.022f)
                    var px = 16f
                    while (px < size.width) {
                        var py = 20f
                        while (py < size.height) {
                            drawCircle(dotColor, radius = 0.9f, center = Offset(px, py))
                            py += 64f
                        }
                        px += 48f
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar: Title with mini calculator sketch on left, Close "×" button on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val strokeW = 1.2.dp.toPx()
                                val ink = JournalInk.copy(alpha = 0.7f)
                                drawRoundRect(
                                    color = ink,
                                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
                                )
                                drawLine(ink, Offset(3.dp.toPx(), 5.dp.toPx()), Offset(size.width - 3.dp.toPx(), 5.dp.toPx()), strokeW)
                            }
                            Text(
                                text = "Calculatrice",
                                style = TextStyle(
                                    fontFamily = JournalHandFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalInk.copy(alpha = 0.85f),
                                    platformStyle = NoFontPadding
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Fermer la calculatrice",
                                    onClick = onDismiss
                                )
                                .semantics { testTag = "tag_popup_close_button" },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val strokeW = 1.5.dp.toPx()
                                val ink = JournalInk.copy(alpha = 0.85f)
                                val pad = 2.dp.toPx()
                                drawLine(ink, Offset(pad, pad), Offset(size.width - pad, size.height - pad), strokeW, StrokeCap.Round)
                                drawLine(ink, Offset(size.width - pad, pad), Offset(pad, size.height - pad), strokeW, StrokeCap.Round)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display Area: Expression on top ("l2ar9am dial lcalcul lfo9"), Total on bottom ("totoal lta7t")
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = JournalDockBg.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, JournalInk.copy(alpha = 0.22f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Top Row: Numbers of calculation (formatted expression)
                            val formattedExpr = remember(expression) {
                                if (expression.isNotBlank()) {
                                    JournalLedgerManager.formatDisplayExpression(expression)
                                } else ""
                            }
                            Text(
                                text = formattedExpr.ifEmpty { " " },
                                style = TextStyle(
                                    fontFamily = JournalHandFamily,
                                    fontSize = 19.sp,
                                    color = JournalInk.copy(alpha = 0.85f),
                                    platformStyle = NoFontPadding
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { testTag = "tag_popup_expression_display" }
                            )

                            // Blue notebook ruled divider line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(0.8.dp)
                                    .background(JournalRule.copy(alpha = 0.85f))
                            )

                            // Bottom Row: Total / Result
                            if (hasError) {
                                val errorText = stringResource(R.string.calculator_error)
                                Text(
                                    text = errorText,
                                    style = TextStyle(
                                        fontFamily = resolveJournalFont(errorText),
                                        fontSize = 18.sp,
                                        color = JournalErrorRed,
                                        platformStyle = NoFontPadding
                                    ),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics { testTag = "tag_popup_result_display" }
                                )
                            } else {
                                val livePreview = remember(expression, result) {
                                    if (result.isNotBlank()) {
                                        result
                                    } else if (expression.isNotBlank()) {
                                        MoneyMath.evaluate(expression)?.stripTrailingZeros()?.toPlainString()
                                    } else null
                                }

                                val (totalText, totalColor) = when {
                                    result.isNotBlank() -> Pair(result, JournalInk)
                                    livePreview != null -> Pair(livePreview, JournalMutedInk.copy(alpha = 0.55f))
                                    expression.isBlank() -> Pair("0", JournalMutedInk.copy(alpha = 0.4f))
                                    else -> Pair(" ", JournalInk)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (totalText != " " && totalText != "0") {
                                        Text(
                                            text = "= ",
                                            style = TextStyle(
                                                fontFamily = JournalHandFamily,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = totalColor,
                                                platformStyle = NoFontPadding
                                            )
                                        )
                                    }
                                    Text(
                                        text = totalText,
                                        style = TextStyle(
                                            fontFamily = JournalHandFamily,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = totalColor,
                                            platformStyle = NoFontPadding
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.semantics { testTag = "tag_popup_result_display" }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Arithmetic Keypad: 5x4 Grid + 'Confirmer' Button
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp)
                                .drawBehind {
                                    val strokeW = 0.85.dp.toPx()
                                    val gridLineColor = JournalInk.copy(alpha = 0.65f)

                                    // Outer border
                                    drawRect(
                                        color = gridLineColor,
                                        topLeft = Offset(0f, 0f),
                                        size = size,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW)
                                    )

                                    // 4 horizontal lines for 5 rows
                                    val rowH = size.height / 5f
                                    for (i in 1..4) {
                                        val y = rowH * i
                                        drawLine(
                                            color = gridLineColor,
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = strokeW
                                        )
                                    }

                                    // 3 vertical lines for 4 columns
                                    val colW = size.width / 4f
                                    for (i in 1..3) {
                                        val x = colW * i
                                        drawLine(
                                            color = gridLineColor,
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = strokeW
                                        )
                                    }
                                }
                        ) {
                            // Row 1: C | ( | ) | ÷
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                JournalKeyCell("C", Modifier.weight(1f)) { onKey("C") }
                                JournalKeyCell("(", Modifier.weight(1f)) { onKey("(") }
                                JournalKeyCell(")", Modifier.weight(1f)) { onKey(")") }
                                JournalKeyCell("÷", Modifier.weight(1f), operatorDabColor = HighlighterBlue) { onKey("÷") }
                            }

                            // Row 2: 1 | 2 | 3 | ×
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                JournalKeyCell("1", Modifier.weight(1f)) { onKey("1") }
                                JournalKeyCell("2", Modifier.weight(1f)) { onKey("2") }
                                JournalKeyCell("3", Modifier.weight(1f)) { onKey("3") }
                                JournalKeyCell("×", Modifier.weight(1f), operatorDabColor = HighlighterGreen) { onKey("×") }
                            }

                            // Row 3: 4 | 5 | 6 | −
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                JournalKeyCell("4", Modifier.weight(1f)) { onKey("4") }
                                JournalKeyCell("5", Modifier.weight(1f)) { onKey("5") }
                                JournalKeyCell("6", Modifier.weight(1f)) { onKey("6") }
                                JournalKeyCell("−", Modifier.weight(1f), operatorDabColor = HighlighterYellow) { onKey("−") }
                            }

                            // Row 4: 7 | 8 | 9 | +
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                JournalKeyCell("7", Modifier.weight(1f)) { onKey("7") }
                                JournalKeyCell("8", Modifier.weight(1f)) { onKey("8") }
                                JournalKeyCell("9", Modifier.weight(1f)) { onKey("9") }
                                JournalKeyCell("+", Modifier.weight(1f), operatorDabColor = HighlighterPink) { onKey("+") }
                            }

                            // Row 5: . | 0 | ⌫ | =
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                JournalKeyCell(".", Modifier.weight(1f)) { onKey(".") }
                                JournalKeyCell("0", Modifier.weight(1f)) { onKey("0") }
                                JournalKeyCell("⌫", Modifier.weight(1f), isBackspace = true) { onKey("⌫") }
                                JournalKeyCell(
                                    "=",
                                    Modifier.weight(1f).semantics { testTag = "tag_popup_equals_key" },
                                    operatorDabColor = HighlighterPink
                                ) { onKey("=") }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 'Confirmer' Action Button (Organic Pink Marker Stroke centered around text)
                        val confirmAlpha = if (canConfirm) 0.85f else 0.28f
                        val confirmTextAlpha = if (canConfirm) 1f else 0.35f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable(
                                    enabled = canConfirm,
                                    role = Role.Button,
                                    onClickLabel = "Confirmer le montant",
                                    onClick = onConfirm
                                )
                                .semantics { testTag = "tag_popup_confirm_button" },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.journalHighlighter(
                                    color = HighlighterPink,
                                    alpha = confirmAlpha,
                                    horizontalPadding = 26.dp,
                                    verticalPadding = 2.dp,
                                    seedVariant = 3
                                )
                            ) {
                                Text(
                                    text = "Confirmer",
                                    style = TextStyle(
                                        fontFamily = JournalHandFamily,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JournalInk.copy(alpha = confirmTextAlpha),
                                        platformStyle = NoFontPadding
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalKeyCell(
    text: String,
    modifier: Modifier = Modifier,
    operatorDabColor: Color? = null,
    isBackspace: Boolean = false,
    onClick: () -> Unit
) {
    val dabModifier = if (operatorDabColor != null) {
        Modifier.journalOperatorDab(operatorDabColor, alpha = 0.72f, widthDp = 34.dp, heightDp = 18.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onClick)
            .then(dabModifier),
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            // Hand-drawn backspace symbol
            Canvas(modifier = Modifier.size(22.dp, 16.dp)) {
                val strokeW = 1.15.dp.toPx()
                val ink = JournalInk.copy(alpha = 0.9f)
                val w = size.width
                val h = size.height

                // Pointed tag shape
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.32f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(w * 0.32f, h)
                    lineTo(0f, h / 2f)
                    close()
                }
                drawPath(p, color = ink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW))

                // Inner cross '×'
                val cx = w * 0.64f
                val cy = h / 2f
                val d = 3.2.dp.toPx()
                drawLine(ink, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeW, StrokeCap.Round)
                drawLine(ink, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeW, StrokeCap.Round)
            }
        } else {
            val style = when (text) {
                "=" -> JournalKeyDigitStyle.copy(fontSize = 24.sp)
                "(", ")" -> JournalKeyDigitStyle.copy(fontSize = 20.sp)
                "C" -> JournalKeyDigitStyle.copy(fontWeight = FontWeight.Bold)
                else -> JournalKeyDigitStyle
            }
            Text(
                text = text,
                style = style
            )
        }
    }
}

@Composable
fun DateGroupHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val todayText = androidx.compose.ui.res.stringResource(R.string.date_today)
    val yesterdayText = androidx.compose.ui.res.stringResource(R.string.date_yesterday)

    val (pillColor, lineColor) = when {
        title.contains(todayText, ignoreCase = true) || title.contains("اليوم") -> {
            Pair(HighlighterYellow.copy(alpha = 0.65f), HighlighterPink.copy(alpha = 0.7f))
        }
        title.contains(yesterdayText, ignoreCase = true) || title.contains("أمس") || title.contains("البارح") -> {
            Pair(HighlighterGreen.copy(alpha = 0.55f), HighlighterGreen.copy(alpha = 0.8f))
        }
        else -> {
            Pair(HighlighterBlue.copy(alpha = 0.55f), HighlighterBlue.copy(alpha = 0.8f))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Small dash on the left
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(2.5.dp)
                .background(lineColor, RoundedCornerShape(1.dp))
        )

        // Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(pillColor)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = title,
                fontFamily = PatrickHandFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = JournalInk
            )
        }
    }
}

/**
 * Single-line activity row (29dp) for Activité récente.
 * Supports Calculations (amount on end), Checklists (dashed line + chevron), and Notes (dashed line + chevron).
 */
@Composable
fun NotebookActivityRow(
    activity: RecentActivityItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    searchQuery: String = "",
    showIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val title = activity.title.ifBlank {
        when (activity) {
            is RecentActivityItem.CalculationActivity -> stringResource(R.string.editor_new_title)
            is RecentActivityItem.ChecklistActivity -> stringResource(R.string.checklist_untitled)
            is RecentActivityItem.NoteActivity -> stringResource(R.string.note_untitled)
        }
    }

    // Determine dot color for the timeline on the left
    val dotColor = remember(activity) {
        when (activity) {
            is RecentActivityItem.CalculationActivity -> {
                val tLower = activity.calculationWithItems.calculation.title.lowercase()
                if (tLower.contains("caisse") || tLower.contains("rendu") || tLower.contains("صرف")) {
                    Color(0xFF3B82F6) // Soft Blue
                } else {
                    Color(0xFFEF4444) // Soft Rose Pink
                }
            }
            is RecentActivityItem.ChecklistActivity -> Color(0xFF10B981) // Soft Sage/Emerald Green
            is RecentActivityItem.NoteActivity -> Color(0xFFF59E0B) // Soft Amber Yellow
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onClick()
                }
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Start side: Dot + (Optional Icon) + Title
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.widthIn(max = if (showIcon) 240.dp else 265.dp)
        ) {
            // Dot
            Canvas(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = (-1.5).dp)
            ) {
                drawCircle(color = dotColor)
            }

            if (showIcon) {
                when (activity) {
                    is RecentActivityItem.CalculationActivity -> {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Calculator,
                            contentDescription = null,
                            tint = JournalWritingInk,
                            size = 17.dp,
                            modifier = Modifier.offset(y = 1.8.dp)
                        )
                    }
                    is RecentActivityItem.ChecklistActivity -> {
                        Canvas(
                            modifier = Modifier
                                .size(17.dp)
                                .offset(y = 1.8.dp)
                        ) {
                            val u = size.width / 24f
                            val strokeW = 1.35.dp.toPx()
                            val box = androidx.compose.ui.geometry.Rect(
                                left = 4.5f * u,
                                top = 3.5f * u,
                                right = 19.5f * u,
                                bottom = 21f * u
                            )
                            drawRoundRect(
                                color = JournalWritingInk,
                                topLeft = Offset(box.left, box.top),
                                size = Size(box.width, box.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f * u),
                                style = Stroke(width = strokeW)
                            )
                            val p = androidx.compose.ui.graphics.Path().apply {
                                moveTo(7.5f * u, 12f * u)
                                lineTo(11.5f * u, 16.5f * u)
                                lineTo(17f * u, 7.5f * u)
                            }
                            drawPath(
                                path = p,
                                color = JournalWritingInk,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                        }
                    }
                    is RecentActivityItem.NoteActivity -> {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Page,
                            contentDescription = null,
                            tint = JournalWritingInk,
                            size = 17.dp,
                            modifier = Modifier.offset(y = 1.8.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = title,
                fontFamily = resolveJournalFont(title, isRtl),
                fontSize = if (isArabicScript(title) || isRtl) 15.sp else 15.5.sp,
                fontWeight = FontWeight.Normal,
                color = JournalWritingInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }

        // Connecting dashed line directly on the blue notebook line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(JournalRuleSpacing)
                .padding(horizontal = 6.dp)
                .drawBehind {
                    val strokeW = 0.85.dp.toPx()
                    val y = size.height
                    drawLine(
                        color = JournalWritingInk.copy(alpha = 0.25f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeW,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                    )
                }
        )

        // End side: Amount if calculation, Chevron > if checklist/note, + 3-dots menu
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (activity) {
                is RecentActivityItem.CalculationActivity -> {
                    val calc = activity.calculationWithItems
                    val currency = runCatching { MoneyUnit.valueOf(calc.calculation.currency) }.getOrDefault(MoneyUnit.DIRHAM)
                    val totalFormatted = JournalLedgerManager.formatTotal(calc.totalCentimes, currency)
                    val currencySuffix = if (currency == MoneyUnit.DIRHAM) {
                        stringResource(R.string.currency_dirham)
                    } else {
                        stringResource(R.string.currency_rial)
                    }
                    val isLatinSuffix = currencySuffix.contains(Regex("[a-zA-Z]"))

                    Text(
                        text = totalFormatted,
                        fontFamily = PatrickHandFamily,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = currencySuffix,
                        fontFamily = if (isLatinSuffix) PatrickHandFamily else TajawalFamily,
                        fontSize = if (isLatinSuffix) 14.sp else 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }
                is RecentActivityItem.ChecklistActivity, is RecentActivityItem.NoteActivity -> {
                    Text(
                        text = if (isRtl) "←" else "→",
                        fontFamily = PatrickHandFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalMutedInk.copy(alpha = 0.70f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }
            }

            // 3-dots menu
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = JournalRuleSpacing)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.cd_more_options),
                        onClick = onMoreClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.More,
                    contentDescription = null,
                    tint = JournalMutedInk,
                    size = 14.dp
                )
            }
        }
    }
}

/**
 * Date group container for unified Activité récente.
 * Includes date header pill with subtle vertical guideline connecting all rows.
 */
@Composable
fun NotebookActivityDateGroupBlock(
    header: String,
    items: List<RecentActivityItem>,
    onOpenCalculation: (String) -> Unit,
    onOpenChecklist: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onMoreClick: (RecentActivityItem) -> Unit,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val todayText = stringResource(R.string.date_today)
    val yesterdayText = stringResource(R.string.date_yesterday)
    val timelineStyle = getDateTimelineStyle(header, todayText, yesterdayText)

    Column(modifier = modifier.fillMaxWidth()) {
        // Line 1: Date separator band (29dp)
        JournalDateRuleBand(title = header)

        // Activity rows column with vertical grouping guide
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (items.size > 1) {
                        val strokeW = 1.4.dp.toPx()
                        val guideX = if (isRtl) size.width - 17.5.dp.toPx() else 17.5.dp.toPx()
                        val rowHeightPx = JournalRuleSpacing.toPx() // 29dp (single notebook rule)
                        val dotCenterY = 24.5.dp.toPx()
                        val startY = dotCenterY
                        val endY = (items.size - 1) * rowHeightPx + dotCenterY

                        drawLine(
                            color = timelineStyle.dotColor.copy(alpha = 0.40f),
                            start = Offset(guideX, startY),
                            end = Offset(guideX, endY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }
        ) {
            items.forEach { activity ->
                NotebookActivityRow(
                    activity = activity,
                    searchQuery = searchQuery,
                    onClick = {
                        when (activity) {
                            is RecentActivityItem.CalculationActivity -> onOpenCalculation(activity.id)
                            is RecentActivityItem.ChecklistActivity -> onOpenChecklist(activity.id)
                            is RecentActivityItem.NoteActivity -> onOpenNote(activity.id)
                        }
                    },
                    onMoreClick = { onMoreClick(activity) }
                )
            }
        }
    }
}

/**
 * Unified Activity Timeline Block directly without a date band header.
 * Directly renders items connected by a subtle vertical guide line on the left.
 */
@Composable
fun NotebookActivityTimelineBlock(
    items: List<RecentActivityItem>,
    onOpenCalculation: (String) -> Unit,
    onOpenChecklist: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onMoreClick: (RecentActivityItem) -> Unit,
    searchQuery: String = "",
    showIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (items.size > 1) {
                    val strokeW = 1.4.dp.toPx()
                    val guideX = if (isRtl) size.width - 17.5.dp.toPx() else 17.5.dp.toPx()
                    val rowHeightPx = JournalRuleSpacing.toPx()
                    val dotCenterY = 24.5.dp.toPx()
                    val startY = dotCenterY
                    val endY = (items.size - 1) * rowHeightPx + dotCenterY

                    drawLine(
                        color = JournalRule.copy(alpha = 0.55f),
                        start = Offset(guideX, startY),
                        end = Offset(guideX, endY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
    ) {
        items.forEach { activity ->
            NotebookActivityRow(
                activity = activity,
                searchQuery = searchQuery,
                showIcon = showIcon,
                onClick = {
                    when (activity) {
                        is RecentActivityItem.CalculationActivity -> onOpenCalculation(activity.id)
                        is RecentActivityItem.ChecklistActivity -> onOpenChecklist(activity.id)
                        is RecentActivityItem.NoteActivity -> onOpenNote(activity.id)
                    }
                },
                onMoreClick = { onMoreClick(activity) }
            )
        }
    }
}


/**
 * Speed Dial Floating Action Button for Home screen matching mockups 3, 4, 5.
 * Features a 52dp pink circular button with black '+'.
 * Expands into a stack of cream capsules: Note, Checklist, Calcul.
 */
@Composable
fun NotebookSpeedDialFab(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onNewCalcul: () -> Unit,
    onNewChecklist: () -> Unit,
    onNewNote: () -> Unit,
    onNewReminder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotationDegree by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Speed dial menu capsules (animated)
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 } + androidx.compose.animation.scaleIn(initialScale = 0.85f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 } + androidx.compose.animation.scaleOut(targetScale = 0.85f)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                // Item 1: Rappel
                SpeedDialCapsuleItem(
                    title = stringResource(R.string.speed_dial_rappel),
                    icon = {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Clock,
                            contentDescription = null,
                            tint = JournalWritingInk,
                            size = 17.dp
                        )
                    },
                    onClick = onNewReminder
                )

                // Item 2: Note
                SpeedDialCapsuleItem(
                    title = stringResource(R.string.speed_dial_note),
                    icon = {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Page,
                            contentDescription = null,
                            tint = JournalWritingInk,
                            size = 17.dp
                        )
                    },
                    onClick = onNewNote
                )

                // Item 2: Checklist
                SpeedDialCapsuleItem(
                    title = stringResource(R.string.speed_dial_checklist),
                    icon = {
                        Canvas(modifier = Modifier.size(15.dp)) {
                            val strokeW = 1.6.dp.toPx()
                            drawRoundRect(
                                color = JournalWritingInk,
                                style = Stroke(width = strokeW),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                            )
                            val p = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.22f, size.height * 0.50f)
                                lineTo(size.width * 0.44f, size.height * 0.74f)
                                lineTo(size.width * 0.82f, size.height * 0.26f)
                            }
                            drawPath(
                                path = p,
                                color = JournalWritingInk,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                            )
                        }
                    },
                    onClick = onNewChecklist
                )

                // Item 3: Calcul
                SpeedDialCapsuleItem(
                    title = stringResource(R.string.speed_dial_calcul),
                    icon = {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Calculator,
                            contentDescription = null,
                            tint = JournalWritingInk,
                            size = 18.dp
                        )
                    },
                    onClick = onNewCalcul
                )
            }
        }

        // Main pink FAB circle (52dp)
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    ambientColor = HighlighterPink.copy(alpha = 0.40f),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(CircleShape)
                .background(HighlighterPink.copy(alpha = 0.88f))
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Ajouter",
                    onClick = onToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(rotationZ = rotationDegree)
            ) {
                val strokeW = 2.4.dp.toPx()
                val ink = JournalWritingInk
                val midX = size.width / 2f
                val midY = size.height / 2f
                drawLine(ink, Offset(1.5.dp.toPx(), midY), Offset(size.width - 1.5.dp.toPx(), midY), strokeW, StrokeCap.Round)
                drawLine(ink, Offset(midX, 1.5.dp.toPx()), Offset(midX, size.height - 1.5.dp.toPx()), strokeW, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun SpeedDialCapsuleItem(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Box(
        modifier = Modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = JournalInk.copy(alpha = 0.15f),
                spotColor = JournalInk.copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(JournalPaper)
            .border(
                width = 0.9.dp,
                color = JournalRule.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon()

            Text(
                text = title,
                fontFamily = resolveJournalFont(title, isRtl),
                fontSize = if (isArabicScript(title) || isRtl) 14.5.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}


