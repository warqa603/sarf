package com.cash.guide.ui.notebook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cash.guide.R
import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyPiece
import com.cash.guide.domain.MoneyUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal object BanknoteImageCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun decode(context: Context, path: String): ImageBitmap? {
        val existing = cache[path]
        if (existing != null) return existing
        return runCatching {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }.getOrNull()?.also { cache[path] = it }
    }
}

@Composable
internal fun rememberBanknoteImage(path: String?): ImageBitmap? {
    val context = LocalContext.current
    if (path == null) return null
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        val loaded = withContext(Dispatchers.IO) {
            BanknoteImageCache.decode(context, path)
        }
        value = loaded
    }
    return bitmapState.value
}

internal fun getRialEquivalent(denominationCentimes: Long): String? = when (denominationCentimes) {
    20_000L -> "4 000 ريال"
    10_000L -> "2 000 ريال"
    5_000L -> "1 000 ريال"
    2_000L -> "400 ريال"
    1_000L -> "200 ريال"
    500L -> "100 ريال"
    200L -> "40 ريال"
    100L -> "20 ريال"
    50L -> "10 ريال"
    20L -> "4 ريال"
    10L -> "2 ريال"
    5L -> "ريال واحد"
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyBreakdownSheet(
    totalCentimes: Long,
    onDismiss: () -> Unit
) {
    val pieces = remember(totalCentimes) { MoneyMath.breakdown(totalCentimes) }
    val remainderCentimes = remember(totalCentimes, pieces) {
        val accounted = pieces.sumOf { it.denomination.valueCentimes * it.count }
        (totalCentimes - accounted).coerceAtLeast(0)
    }

    val dirhamFormatted = MoneyMath.fromCentimes(totalCentimes, MoneyUnit.DIRHAM)
    val rialFormatted = MoneyMath.fromCentimes(totalCentimes, MoneyUnit.RIAL)

    val banknotes = remember(pieces) { pieces.filter { it.denomination.valueCentimes >= 2_000L } }
    val coins = remember(pieces) { pieces.filter { it.denomination.valueCentimes < 2_000L } }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides layoutDirection
        ) {
            // Full screen overlay - click anywhere in the void ("في الخواء") to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Centered Notebook Slip ("ورقة الكناش")
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 410.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Consume click so tapping on paper does not dismiss
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = JournalPaper,
                    border = BorderStroke(1.dp, JournalInk.copy(alpha = 0.45f)),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // 1. Subtle paper grain flecks
                                val dotColor = JournalInk.copy(alpha = 0.022f)
                                var px = 18f
                                while (px < size.width) {
                                    var py = 22f
                                    while (py < size.height) {
                                        drawCircle(dotColor, radius = 0.9f, center = Offset(px, py))
                                        py += 64f
                                    }
                                    px += 48f
                                }

                                // 2. Continuous Moroccan blue ruled lines (السطور الزرقين)
                                val spacingPx = JournalRuleSpacing.roundToPx().toFloat()
                                var y = spacingPx
                                val stroke = 0.65.dp.toPx()
                                val lineColor = JournalRule.copy(alpha = 0.40f)
                                while (y <= size.height) {
                                    drawLine(
                                        color = lineColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = stroke
                                    )
                                    y += spacingPx
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 580.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 14.dp)
                        ) {
                            // Row 1: Close button '✕' (exactly 1 rule = 29dp, NO title badge)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(JournalRuleSpacing)
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            role = Role.Button,
                                            onClick = onDismiss
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✕",
                                        fontFamily = PatrickHandFamily,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JournalMutedInk
                                    )
                                }
                            }

                            // Row 2: Total Conversion resting squarely on ruled line (Line 2 at y = 58dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(JournalRuleSpacing)
                                    .journalBaselineOnRule(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val dhSuffix = stringResource(R.string.currency_dirham)
                                Text(
                                    text = "$dirhamFormatted $dhSuffix",
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorOrange,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )

                                Text(
                                    text = "  =  ",
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalMutedInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )

                                val rialSuffix = stringResource(R.string.currency_rial)
                                Text(
                                    text = "$rialFormatted $rialSuffix",
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorOrange,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }

                            // Section 1: Banknotes (sitting directly on ruled lines)
                            if (banknotes.isNotEmpty()) {
                                val headingBanknotes = stringResource(R.string.breakdown_heading_banknotes)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(JournalRuleSpacing),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HisabiSketchIcon(
                                        symbol = HisabiSymbol.Wallet,
                                        contentDescription = null,
                                        tint = JournalWritingInk,
                                        size = 16.dp
                                    )
                                    Text(
                                        text = headingBanknotes,
                                        fontFamily = resolveJournalFont(headingBanknotes, isRtl),
                                        color = JournalWritingInk,
                                        fontSize = if (isRtl) 13.5.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(platformStyle = NoFontPadding),
                                        modifier = Modifier.journalBaselineOnRule()
                                    )
                                }

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    banknotes.forEach { piece ->
                                        BanknoteDisplayItem(piece = piece)
                                    }
                                }
                            }

                            // Section 2: Coins (sitting directly on ruled lines)
                            if (coins.isNotEmpty()) {
                                val headingCoins = stringResource(R.string.breakdown_heading_coins)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(JournalRuleSpacing),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🪙", fontSize = 13.sp)
                                    Text(
                                        text = headingCoins,
                                        fontFamily = resolveJournalFont(headingCoins, isRtl),
                                        color = JournalWritingInk,
                                        fontSize = if (isRtl) 13.sp else 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(platformStyle = NoFontPadding),
                                        modifier = Modifier.journalBaselineOnRule()
                                    )
                                }

                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    coins.forEach { piece ->
                                        CoinDisplayItem(piece = piece)
                                    }
                                }
                            }

                            // Remainder if any
                            if (remainderCentimes > 0) {
                                val remText = "${stringResource(R.string.breakdown_remainder)} (< 10 centimes): $remainderCentimes c"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(JournalRuleSpacing),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = remText,
                                        fontFamily = resolveJournalFont(remText, isRtl),
                                        color = JournalMutedInk,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        style = TextStyle(platformStyle = NoFontPadding),
                                        modifier = Modifier.journalBaselineOnRule()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Authentic Moroccan notebook cash tray ("صينية الفلوس")
 * Displaying banknotes and coins neatly arranged directly on the blue ruled lines.
 * ZERO heavy cards or solid backgrounds covering the ruled lines.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalDenominationsBoard(
    pieces: List<MoneyPiece>,
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    headingBanknotes: String = stringResource(R.string.cash_register_heading_banknotes),
    headingCoins: String = stringResource(R.string.cash_register_heading_coins)
) {
    val banknotes = remember(pieces) { pieces.filter { it.denomination.valueCentimes >= 2_000L } }
    val coins = remember(pieces) { pieces.filter { it.denomination.valueCentimes < 2_000L } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Section: Banknotes (L-Wra9)
        if (banknotes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Banknote,
                    contentDescription = null,
                    tint = JournalWritingInk,
                    size = 21.dp,
                    modifier = Modifier.journalVisualOnRule(lineHeight = JournalRuleSpacing, gapAboveRule = 4.dp)
                )
                Text(
                    text = headingBanknotes,
                    fontFamily = resolveJournalFont(headingBanknotes, isRtl),
                    color = JournalWritingInk,
                    fontSize = if (isRtl) 14.sp else 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            Spacer(Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                banknotes.forEach { piece ->
                    BanknoteDisplayItem(piece = piece)
                }
            }
        }

        if (banknotes.isNotEmpty() && coins.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
        }

        // Section: Coins (L-Coins / D-Drahem)
        if (coins.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Coin,
                    contentDescription = null,
                    tint = JournalWritingInk,
                    size = 20.dp,
                    modifier = Modifier.journalVisualOnRule(lineHeight = JournalRuleSpacing, gapAboveRule = 4.dp)
                )
                Text(
                    text = headingCoins,
                    fontFamily = resolveJournalFont(headingCoins, isRtl),
                    color = JournalWritingInk,
                    fontSize = if (isRtl) 13.5.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            Spacer(Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                coins.forEach { piece ->
                    CoinDisplayItem(piece = piece)
                }
            }
        }
    }
}

/**
 * Clean Banknote representation:
 * - Real BAM banknote transparent image
 * - NO shadow, NO artificial black halo underneath
 * - Proportional ~1.79:1 aspect ratio
 * - Height: 58dp (exactly 2 ruled lines of 29dp), bottom sitting directly on the blue line!
 * - Prominent, bold count badge (e.g. 2×) in top corner if count > 1
 * - No redundant denomination text
 */
@Composable
fun BanknoteDisplayItem(
    piece: MoneyPiece,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberBanknoteImage(piece.denomination.assetPath)
    val count = piece.count

    Box(
        modifier = modifier
            .width(104.dp)
            .height(58.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
            )
        }

        // Prominent Count Badge (e.g. "2×")
        if (count > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HighlighterPink)
                    .border(BorderStroke(1.2.dp, JournalPaper), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = "${count}×",
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

/**
 * Clean Coin representation:
 * - Real BAM coin transparent circular image
 * - Height: 58dp (2 ruled lines of 29dp), bottom-aligned so the coin rests squarely on the blue line!
 * - Prominent count badge (e.g. 2×) in top corner if count > 1
 * - No redundant denomination text
 */
@Composable
fun CoinDisplayItem(
    piece: MoneyPiece,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberBanknoteImage(piece.denomination.assetPath)
    val count = piece.count
    val sizeDp = when (piece.denomination.valueCentimes) {
        1_000L -> 76.dp // 10 DH (bimetallic, large)
        500L -> 71.dp   // 5 DH (bimetallic)
        200L -> 67.dp   // 2 DH
        100L -> 63.dp   // 1 DH
        50L -> 58.dp    // 50c
        20L -> 55.dp    // 20c
        10L -> 52.dp    // 10c
        else -> 60.dp
    }
    val containerHeight = 87.dp // exactly 3 ruled lines (29dp * 3)

    Box(
        modifier = modifier
            .size(width = sizeDp + 12.dp, height = containerHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(sizeDp)
                    .clip(CircleShape)
            )
        }

        // Prominent Count Badge (e.g. "2×")
        if (count > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (containerHeight - sizeDp - 6.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(HighlighterYellow)
                    .border(BorderStroke(1.dp, JournalPaper), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = "${count}×",
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

