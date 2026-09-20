package com.cash.guide.feature.showcase

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalDensity
import com.cash.guide.ui.notebook.NoFontPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.ui.notebook.BeirutiFamily
import com.cash.guide.ui.notebook.CreamFrothFamily
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.IbmPlexMonoFamily
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.RealTajawalFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.ZainFamily
import com.cash.guide.ui.notebook.AppFontOption
import com.cash.guide.ui.notebook.JournalFontManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.widget.Toast

/**
 * 6 Polices testables pour le carnet Sarf / Warqa.
 */
enum class TestFont(
    val id: String,
    val displayName: String,
    val arabicName: String,
    val family: FontFamily,
    val description: String,
    val supportsArabic: Boolean,
    val badge: String,
    val badgeColor: Color
) {
    BEIRUTI(
        id = "beiruti",
        displayName = "Beiruti",
        arabicName = "بيروتي",
        family = BeirutiFamily,
        description = "خط عربي/فرنسي عصري وأنيق (Boutros Fonts). حروفه مفتوحة ومريحة جداً في القراءة على السطر.",
        supportsArabic = true,
        badge = "Moderne & Aéré ✨",
        badgeColor = HighlighterGreen
    ),
    ZAIN(
        id = "zain",
        displayName = "Zain",
        arabicName = "زين",
        family = ZainFamily,
        description = "خط ناعم وانسيابي بلمسة دافئة قريبة للخط اليدوي العربي اللطيف. مثالي للدفتر الحميمي.",
        supportsArabic = true,
        badge = "Doux & Artisanal ✍️",
        badgeColor = HighlighterPink
    ),
    TAJAWAL(
        id = "tajawal",
        displayName = "Tajawal",
        arabicName = "تجوال",
        family = RealTajawalFamily,
        description = "خط هندسي متوازن ونظيف جداً، احترافي وواضح للغاية في مختلف الأحجام والعناوين.",
        supportsArabic = true,
        badge = "Équilibré & Net 📐",
        badgeColor = HighlighterBlue
    ),
    PATRICK_HAND(
        id = "patrick_hand",
        displayName = "Patrick Hand",
        arabicName = "باتريك هاند",
        family = PatrickHandFamily,
        description = "خط اليد اللاتيني المكتوب بالماركور أو الفوتر. ممتاز للفرنسية والأرقام والرموز.",
        supportsArabic = false,
        badge = "Manuscrit Feutre 🖋️",
        badgeColor = HighlighterYellow
    ),
    CREAM_FROTH(
        id = "cream_froth",
        displayName = "Cream Froth",
        arabicName = "كريم فروث",
        family = CreamFrothFamily,
        description = "خط يدوي حر وعفوي، الخط الحالي المستعمل في عمليات وحسابات الدفتر.",
        supportsArabic = true,
        badge = "Manuscrit Libre 🎨",
        badgeColor = Color(0xFFE9D5FF)
    ),
    IBM_PLEX_MONO(
        id = "ibm_plex_mono",
        displayName = "IBM Plex Mono",
        arabicName = "آي بي إم مونو",
        family = IbmPlexMonoFamily,
        description = "خط بأبعاد متساوية (Monospace). ممتاز جداً للأرقام والمبالغ وأعمدة الحسابات المتقاطعة.",
        supportsArabic = false,
        badge = "Chiffres Monospace 🔢",
        badgeColor = Color(0xFFFED7AA)
    )
}

@Composable
fun FontTesterScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFont by remember { mutableStateOf(TestFont.BEIRUTI) }
    var customText by remember {
        mutableStateOf("Taqdiya d l-khodra 75.00 DH • مطيشة وبصلة 35 درهم")
    }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var isBold by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val quickSnippets = listOf(
        "مطيشة 10 دراهم، بطاطا 15 درهم",
        "كريدي مول الحانوت: 250 درهم",
        "سلف خويا كريم: 500 درهم",
        "Taqdiya ssi Bouchaib 150 DH",
        "Total: 1,250.00 DH / 25,000 ريال",
        "0 1 2 3 4 5 6 7 8 9 + − × ÷ = %"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = JournalPaper
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // --- Top App Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = JournalInk
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Laboratoire Typographique",
                        fontFamily = PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = JournalInk
                    )
                    Text(
                        text = "مختبر الخطوط • 6 Polices au choix",
                        fontFamily = TajawalFamily,
                        fontSize = 12.sp,
                        color = JournalMutedInk
                    )
                }
            }

            // --- Scrollable Content ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {

                // --- 1. Font Selector Chips (Horizontal Scroll) ---
                Text(
                    text = "Choisissez une police à inspecter :",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = JournalWritingInk,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestFont.entries.forEach { font ->
                        val isSelected = font == selectedFont
                        val chipBg = if (isSelected) font.badgeColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)
                        val chipBorder = if (isSelected) JournalInk else JournalRule

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = chipBg,
                            border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, chipBorder),
                            modifier = Modifier.clickable { selectedFont = font }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(JournalInk),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Column {
                                    Text(
                                        text = font.displayName,
                                        fontFamily = font.family,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = JournalInk
                                    )
                                    Text(
                                        text = font.arabicName,
                                        fontFamily = font.family,
                                        fontSize = 11.sp,
                                        color = JournalMutedInk
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- 2. Active Font Description Card ---
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JournalRule),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedFont.displayName} (${selectedFont.arabicName})",
                                fontFamily = selectedFont.family,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = JournalInk
                            )
                            // Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = selectedFont.badgeColor.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = selectedFont.badge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = selectedFont.description,
                            fontFamily = TajawalFamily,
                            fontSize = 13.sp,
                            color = JournalWritingInk,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Compatibility Tag
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedFont.supportsArabic) {
                                Text(
                                    text = "✅ يدعم الحروف العربية والمغربية بالكامل",
                                    fontFamily = TajawalFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1B6334),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "⚠️ الحروف العربية تستعمل خط النظام (يدعم اللاتينية والأرقام)",
                                    fontFamily = TajawalFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val targetAppOption = when (selectedFont) {
                            TestFont.BEIRUTI -> AppFontOption.BEIRUTI
                            TestFont.ZAIN -> AppFontOption.ZAIN
                            TestFont.TAJAWAL -> AppFontOption.TAJAWAL
                            TestFont.PATRICK_HAND -> AppFontOption.DUO_PATRICK_ZAIN
                            TestFont.CREAM_FROTH -> AppFontOption.CREAM_FROTH
                            TestFont.IBM_PLEX_MONO -> AppFontOption.IBM_PLEX
                        }
                        val isGloballyActive = JournalFontManager.currentFontOption == targetAppOption

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    JournalFontManager.setFont(targetAppOption, context)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(
                                        context,
                                        "Police ${selectedFont.displayName} appliquée à toute l'application ✨",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isGloballyActive) Color(0xFF1B6334) else selectedFont.badgeColor.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isGloballyActive) Color(0xFF1B6334) else JournalInk.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isGloballyActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isGloballyActive) {
                                        "✓ هذه هي البوليس المطبقة حالياً في التطبيق كامل"
                                    } else {
                                        "✨ تطبيق ${selectedFont.displayName} على التطبيق كامل ✨"
                                    },
                                    fontFamily = selectedFont.family,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isGloballyActive) Color.White else JournalInk
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. Interactive Live Typing Notebook Paper ("Wri9a d Test") ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 جرّب واكتب بالخط المختار :",
                        fontFamily = TajawalFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = JournalWritingInk
                    )

                    // Controls for Size and Weight
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isBold) HighlighterYellow else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, JournalRule),
                            modifier = Modifier.clickable { isBold = !isBold }
                        ) {
                            Text(
                                text = "B",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = JournalInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, JournalRule),
                            modifier = Modifier.clickable { if (fontSize > 13f) fontSize -= 2f }
                        ) {
                            Text(
                                text = "A−",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalInk,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, JournalRule),
                            modifier = Modifier.clickable { if (fontSize < 28f) fontSize += 2f }
                        ) {
                            Text(
                                text = "A+",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalInk,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ruled Paper Canvas & Input
                val lineGap = 32.dp
                val density = LocalDensity.current
                val lineGapPx = with(density) { lineGap.toPx() }
                var testerBaselinePx by remember { mutableFloatStateOf(0f) }
                val targetBaselinePx = lineGapPx - with(density) { 0.8.dp.toPx() }
                val yShiftPx = if (testerBaselinePx > 0f) targetBaselinePx - testerBaselinePx else 0f
                val yShiftDp = with(density) { yShiftPx.toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFDF8))
                        .border(1.dp, JournalRule, RoundedCornerShape(12.dp))
                ) {
                    // Draw notebook ruled lines and red margin
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var y = lineGapPx
                        while (y < size.height) {
                            drawLine(
                                color = Color(0xFFC7D7E6),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += lineGapPx
                        }
                        // Red vertical margin rule
                        drawLine(
                            color = Color(0xFFF2A39E),
                            start = Offset(40.dp.toPx(), 0f),
                            end = Offset(40.dp.toPx(), size.height),
                            strokeWidth = 1.2f
                        )
                    }

                    // Input field
                    BasicTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        onTextLayout = { layoutResult ->
                            if (layoutResult.lineCount > 0) {
                                testerBaselinePx = layoutResult.getLineBaseline(0)
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = selectedFont.family,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontSize = fontSize.sp,
                            color = JournalWritingInk,
                            lineHeight = 32.sp,
                            platformStyle = NoFontPadding
                        ),
                        cursorBrush = SolidColor(JournalInk),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = yShiftDp)
                            .padding(start = 50.dp, end = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Snippet Chips
                Text(
                    text = "جمل جاهزة للتجربة بنقرة واحدة :",
                    fontFamily = TajawalFamily,
                    fontSize = 11.sp,
                    color = JournalMutedInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSnippets.forEach { snippet ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, JournalRule),
                            modifier = Modifier.clickable { customText = snippet }
                        ) {
                            Text(
                                text = snippet,
                                fontFamily = selectedFont.family,
                                fontSize = 12.sp,
                                color = JournalInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 4. Carnet de Comptes Demo Card ("3la Star") ---
                Text(
                    text = "📊 مثال حسابات على السطر (Carnet de Comptes) :",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = JournalWritingInk
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, JournalRule),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Taqdiya d s-souq (التقدية)",
                                fontFamily = selectedFont.family,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = JournalInk
                            )
                            Text(
                                text = "19 Septembre",
                                fontFamily = selectedFont.family,
                                fontSize = 13.sp,
                                color = JournalMutedInk
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Line 1
                        DemoCalculationRow(
                            number = "1.",
                            title = "Lhem d l-begri (1.5 kg)",
                            amount = "140.00 DH",
                            font = selectedFont
                        )
                        // Line 2
                        DemoCalculationRow(
                            number = "2.",
                            title = "Khoudra mkhallta (خضرة مخلطة)",
                            amount = "45.00 DH",
                            font = selectedFont
                        )
                        // Line 3
                        DemoCalculationRow(
                            number = "3.",
                            title = "7lib & bayd baldi (حليب وبيض)",
                            amount = "22.50 DH",
                            font = selectedFont
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Highlighted Total
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HighlighterYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المجموع / Total :",
                                fontFamily = selectedFont.family,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = JournalInk
                            )
                            Text(
                                text = "207.50 DH",
                                fontFamily = selectedFont.family,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = JournalInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 5. Side-by-Side Direct Comparison Section ---
                Text(
                    text = "🔍 مقارنة مباشرة بين جميع الخطوط الـ 6 :",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = JournalWritingInk
                )
                Text(
                    text = "نفس الجملة معروضة بكل خط لتحديد الأنسب للدفتر :",
                    fontFamily = TajawalFamily,
                    fontSize = 12.sp,
                    color = JournalMutedInk,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                TestFont.entries.forEach { font ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (font == selectedFont) font.badgeColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (font == selectedFont) 1.5.dp else 0.8.dp,
                            if (font == selectedFont) JournalInk else JournalRule
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { selectedFont = font }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${font.displayName} • ${font.arabicName}",
                                    fontFamily = font.family,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = JournalInk
                                )
                                Text(
                                    text = font.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalMutedInk
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Taqdiya d l-kounach: 120 DH • مطيشة 15 درهم",
                                fontFamily = font.family,
                                fontSize = 16.sp,
                                color = JournalWritingInk
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "0 1 2 3 4 5 6 7 8 9 (DH / ريال)",
                                fontFamily = font.family,
                                fontSize = 13.sp,
                                color = JournalMutedInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun DemoCalculationRow(
    number: String,
    title: String,
    amount: String,
    font: TestFont
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number,
            fontFamily = font.family,
            fontSize = 14.sp,
            color = JournalMutedInk,
            modifier = Modifier.width(22.dp)
        )
        Text(
            text = title,
            fontFamily = font.family,
            fontSize = 15.sp,
            color = JournalWritingInk,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = amount,
            fontFamily = font.family,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = JournalInk
        )
    }
}
