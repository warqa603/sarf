package com.cash.guide.ui.notebook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Debug overlay toggle for measurable baseline verification
var ShowBaselineDebugOverlay: Boolean = false

/**
 * Positions a composable so its measured FirstBaseline sits directly on the paper rule.
 * Target baseline: ruleY = rowHeight (exact 0dp delta with notebook rule).
 */
fun Modifier.journalBaselineOnRule(
    lineHeight: Dp = JournalRuleSpacing,
    opticalOffsetFromBottom: Dp = 0.dp
): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        )
        val baseline = placeable[FirstBaseline]
        val rowHeight = lineHeight.roundToPx()
        val targetBaseline = rowHeight - opticalOffsetFromBottom.roundToPx()
        val yOffset = if (baseline != AlignmentLine.Unspecified) {
            targetBaseline - baseline
        } else {
            rowHeight - placeable.height
        }
        layout(placeable.width, rowHeight) {
            placeable.placeRelative(0, yOffset)
        }
    }
    .then(
        if (ShowBaselineDebugOverlay) {
            Modifier.drawWithContent {
                drawContent()
                val ruleY = size.height
                val targetY = size.height - opticalOffsetFromBottom.toPx()
                // Paper rule: blue
                drawLine(
                    color = Color.Blue,
                    start = Offset(0f, ruleY),
                    end = Offset(size.width, ruleY),
                    strokeWidth = 1.5f
                )
                // Measured FirstBaseline: red
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, targetY),
                    end = Offset(size.width, targetY),
                    strokeWidth = 1.5f
                )
            }
        } else {
            Modifier
        }
    )

fun Modifier.baselineOnPaperRule(): Modifier = journalBaselineOnRule()

fun Modifier.editableTextOnPaperRules(): Modifier = journalBaselineOnRule()

/**
 * Places a non-text visual (icon, bullet, emoji, sketch) in one notebook slot
 * with its visual bottom resting just above the rule. This is the companion to
 * [journalBaselineOnRule] and avoids screen-specific `offset(y = …)` fixes.
 */
fun Modifier.journalVisualOnRule(
    lineHeight: Dp = JournalRuleSpacing,
    gapAboveRule: Dp = 0.dp,
    opticalBottomShift: Dp = 2.dp
): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
    )
    val rowHeight = lineHeight.roundToPx()
    val yOffset = (rowHeight - gapAboveRule.roundToPx() - placeable.height + opticalBottomShift.roundToPx())
        .coerceAtLeast(0)
    layout(placeable.width, rowHeight) {
        placeable.placeRelative(0, yOffset)
    }
}


/**
 * Aligns single-line or multi-line text (up to 2 lines) directly onto the 29dp notebook rules.
 * - Line 1 FirstBaseline sits on Rule 1 (lineHeight = 29dp).
 * - Line 2 LastBaseline sits on Rule 2 (lineHeight * 2 = 58dp).
 * The measured total height is exactly lineHeight * lineCount (29dp or 58dp).
 */
fun Modifier.journalTextOnRules(
    lineHeight: Dp = JournalRuleSpacing,
    opticalOffsetFromBottom: Dp = 0.dp
): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        )
        val firstBaseline = placeable[FirstBaseline]
        val lastBaseline = placeable[LastBaseline]
        val singleRowHeight = lineHeight.roundToPx()

        val isMultiLine = firstBaseline != AlignmentLine.Unspecified &&
                lastBaseline != AlignmentLine.Unspecified &&
                lastBaseline > firstBaseline + (singleRowHeight * 0.45f)
        val lineCount = if (isMultiLine) 2 else 1
        val totalRowHeight = singleRowHeight * lineCount

        val targetBaseline = singleRowHeight - opticalOffsetFromBottom.roundToPx()
        val yOffset = if (firstBaseline != AlignmentLine.Unspecified) {
            targetBaseline - firstBaseline
        } else {
            singleRowHeight - placeable.height
        }

        layout(placeable.width, totalRowHeight) {
            placeable.placeRelative(0, yOffset)
        }
    }

/**
 * Dedicated component for text with an organic highlighter marker stroke:
 * 1. Measures text at its natural glyph height (with font padding excluded).
 * 2. Positions the measured FirstBaseline directly on the paper rule (rowHeight).
 * 3. Draws the organic marker behind the natural text bounds, centered vertically around glyphs.
 * 4. Stable layout with zero snapshot state writes during measurement.
 * 5. Total layout height is exactly one JournalRuleSpacing (48dp).
 */
@Composable
fun JournalBaselineHighlightedText(
    text: String,
    style: TextStyle,
    highlighterColor: Color,
    highlighterAlpha: Float = 0.72f,
    horizontalPadding: Dp = 8.dp,
    verticalPadding: Dp = 1.0.dp,
    seedVariant: Int = 0,
    modifier: Modifier = Modifier,
    lineHeight: Dp = JournalRuleSpacing,
    opticalOffsetFromBottom: Dp = 0.dp
) {
    val debugOverlayModifier = if (ShowBaselineDebugOverlay) {
        Modifier.drawWithContent {
            drawContent()
            val ruleY = size.height
            val targetY = size.height - opticalOffsetFromBottom.toPx()
            drawLine(
                color = Color.Blue,
                start = Offset(0f, ruleY),
                end = Offset(size.width, ruleY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color.Red,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.5f
            )
        }
    } else {
        Modifier
    }

    Layout(
        content = {
            // Child 0: Organic Highlighter Marker Background
            Canvas(modifier = Modifier) {
                val w = size.width
                val h = size.height
                val p1 = if (seedVariant % 2 == 0) 0.6f else -0.6f
                val p2 = if (seedVariant % 3 == 0) -0.8f else 0.8f

                val path = Path().apply {
                    moveTo(2f, 1.2f)
                    cubicTo(
                        w * 0.28f, -1.0f + p1,
                        w * 0.68f, 0.6f + p2,
                        w - 1.5f, 1.2f
                    )
                    cubicTo(
                        w + 2.0f, h * 0.45f,
                        w + 1.0f, h * 0.85f,
                        w - 1.8f, h - 0.9f
                    )
                    cubicTo(
                        w * 0.72f, h + 1.0f + p1,
                        w * 0.32f, h - 0.7f + p2,
                        2.2f, h - 1.2f
                    )
                    cubicTo(
                        -1.2f, h * 0.65f,
                        -0.6f, h * 0.35f,
                        2f, 1.2f
                    )
                    close()
                }
                drawPath(path = path, color = highlighterColor.copy(alpha = highlighterAlpha))
            }

            // Child 1: Natural Text
            Text(
                text = text,
                style = style
            )
        },
        modifier = modifier.then(debugOverlayModifier)
    ) { measurables, constraints ->
        val textPlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        )
        val baseline = textPlaceable[FirstBaseline]
        val rowHeight = lineHeight.roundToPx()
        val targetBaseline = rowHeight - opticalOffsetFromBottom.roundToPx()
        val textYOffset = if (baseline != AlignmentLine.Unspecified) {
            targetBaseline - baseline
        } else {
            rowHeight - textPlaceable.height
        }
        val hPad = horizontalPadding.roundToPx()
        val vPad = verticalPadding.roundToPx()

        val markerWidth = textPlaceable.width + 2 * hPad
        val markerHeight = textPlaceable.height + 2 * vPad
        val markerPlaceable = measurables[0].measure(
            Constraints.fixed(markerWidth, markerHeight)
        )

        val markerYOffset = textYOffset - vPad
        val totalWidth = markerWidth

        layout(totalWidth, rowHeight) {
            // Draw marker behind text
            markerPlaceable.placeRelative(0, markerYOffset)
            // Place text centered inside marker horizontally, baseline on rule
            textPlaceable.placeRelative(hPad, textYOffset)
        }
    }
}

/**
 * Dedicated component for the centered Total Result with organic pale-yellow highlighter:
 * 1. Measures amount text (prominent, 25sp) and suffix text (smaller, 16.5sp).
 * 2. Positions both measured FirstBaselines directly on the paper rule (rowHeight).
 * 3. Draws the organic pale-yellow highlighter behind the combined natural text bounds.
 * 4. Exact 48dp single-rule slot with zero optical offsets.
 */
@Composable
fun JournalBaselineHighlightedResult(
    amount: String,
    suffix: String,
    amountColor: Color = JournalInk,
    suffixColor: Color = JournalMutedInk,
    highlighterColor: Color = HighlighterYellow,
    highlighterAlpha: Float = 0.38f,
    horizontalPadding: Dp = 8.dp,
    verticalPadding: Dp = 2.dp,
    seedVariant: Int = 2,
    modifier: Modifier = Modifier,
    lineHeight: Dp = JournalRuleSpacing,
    opticalOffsetFromBottom: Dp = 0.dp
) {
    val debugOverlayModifier = if (ShowBaselineDebugOverlay) {
        Modifier.drawWithContent {
            drawContent()
            val ruleY = size.height
            val targetY = size.height - opticalOffsetFromBottom.toPx()
            drawLine(
                color = Color.Blue,
                start = Offset(0f, ruleY),
                end = Offset(size.width, ruleY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color.Red,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.5f
            )
        }
    } else {
        Modifier
    }

    Layout(
        content = {
            // Child 0: Organic Pale-Yellow Highlighter Marker Background
            Canvas(modifier = Modifier) {
                val w = size.width
                val h = size.height
                val padX = 2.dp.toPx()
                val padY = 1.5.dp.toPx()

                val path = Path().apply {
                    val left = padX
                    val top = padY
                    val right = w - padX
                    val bottom = h - padY

                    // Top stroke: gentle organic hand wave
                    moveTo(left + 2f, top + 1.5f)
                    cubicTo(
                        w * 0.30f, top - 1.0f,
                        w * 0.70f, top + 0.8f,
                        right - 1.5f, top + 0.5f
                    )
                    // Right cap: subtle chisel tilt and rounded return
                    cubicTo(
                        right + 2.5f, top + (h * 0.35f),
                        right + 1.8f, bottom - (h * 0.35f),
                        right - 1.5f, bottom - 0.8f
                    )
                    // Bottom stroke: organic hand return wave
                    cubicTo(
                        w * 0.68f, bottom + 1.0f,
                        w * 0.32f, bottom - 0.7f,
                        left + 1.5f, bottom - 1.0f
                    )
                    // Left cap: soft rounded entry
                    cubicTo(
                        left - 2.5f, bottom - (h * 0.35f),
                        left - 1.8f, top + (h * 0.35f),
                        left + 2f, top + 1.5f
                    )
                    close()
                }
                drawPath(path = path, color = highlighterColor.copy(alpha = highlighterAlpha))
            }

            // Child 1: Amount Text
            Text(
                text = amount,
                style = journalPrimaryTotalStyle(color = amountColor)
            )

            // Child 2: Suffix Text
            Text(
                text = suffix,
                style = journalAmountStyle(color = suffixColor)
            )
        },
        modifier = modifier.then(debugOverlayModifier)
    ) { measurables, constraints ->
        val amountPlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        )
        val suffixPlaceable = measurables[2].measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        )

        val rowHeight = lineHeight.roundToPx()
        val targetBaseline = rowHeight - opticalOffsetFromBottom.roundToPx()

        val amountBaseline = amountPlaceable[FirstBaseline]
        val amountYOffset = if (amountBaseline != AlignmentLine.Unspecified) {
            targetBaseline - amountBaseline
        } else {
            rowHeight - amountPlaceable.height
        }

        val suffixBaseline = suffixPlaceable[FirstBaseline]
        val suffixYOffset = if (suffixBaseline != AlignmentLine.Unspecified) {
            targetBaseline - suffixBaseline
        } else {
            rowHeight - suffixPlaceable.height
        }

        val spacing = 6.dp.roundToPx()
        val textContentWidth = amountPlaceable.width + spacing + suffixPlaceable.width
        val textMaxHeight = maxOf(amountPlaceable.height, suffixPlaceable.height)

        val hPad = horizontalPadding.roundToPx()
        val vPad = verticalPadding.roundToPx()

        val markerWidth = textContentWidth + 2 * hPad
        val markerHeight = textMaxHeight + 2 * vPad
        val markerPlaceable = measurables[0].measure(
            Constraints.fixed(markerWidth, markerHeight)
        )

        val minYOffset = minOf(amountYOffset, suffixYOffset)
        val markerYOffset = minYOffset - vPad

        layout(markerWidth, rowHeight) {
            // Draw marker behind
            markerPlaceable.placeRelative(0, markerYOffset)
            // Draw amount
            amountPlaceable.placeRelative(hPad, amountYOffset)
            // Draw suffix
            suffixPlaceable.placeRelative(hPad + amountPlaceable.width + spacing, suffixYOffset)
        }
    }
}

@Composable
fun RuledPaperBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        content()
    }
}

@Composable
fun RuledDocument(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(Paper)) {
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
                        .drawBehind {
                            val grid = HisabiMetrics.Grid.toPx()
                            var y = grid
                            while (y <= size.height) {
                                drawLine(
                                    color = Rule.copy(alpha = 0.54f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 0.55.dp.toPx()
                                )
                                y += grid
                            }
                        },
                    content = content
                )
            }
        }
    }
}
