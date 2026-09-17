package com.cash.guide.feature.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.ui.notebook.*

/**
 * 4-question lightweight monthly re-diagnostic check-in.
 * Allows user to quickly update their situation without re-doing the full 12-section questionnaire.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCheckInSheet(
    goal: SavingsGoalEntity,
    onSubmit: (MonthlyCheckInAnswers) -> Unit,
    onClose: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    var savedAmountDh by remember { mutableStateOf("") }
    var biggestObstacle by remember { mutableStateOf("NONE") }
    var incomeOrObligationsChanged by remember { mutableStateOf(false) }
    var goalStillSame by remember { mutableStateOf(true) }

    val requiredDh = if (goal.targetMonths > 0) {
        (goal.targetAmountCentimes - goal.currentAmountCentimes).coerceAtLeast(0L) / goal.targetMonths / 100
    } else 0L

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = JournalPaper,
        scrimColor = Color(0x66000000),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(JournalMutedInk.copy(alpha = 0.35f))
            )
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isRtl) "📅 فحص الشهر: كيف مشى الحال؟" else "📅 Check-in du mois : le bilan",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
                Text(
                    text = "✕",
                    fontFamily = TajawalFamily,
                    fontSize = 16.sp,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Q1: Actual saved this month
            Text(
                text = if (isRtl) "1. شحال وفرتي فعلياً هاد الشهر؟" else "1. Combien avez-vous réellement épargné ce mois ?",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isRtl) "القسط المبرمج كان: $requiredDh DH" else "Mensualité prévue : $requiredDh DH",
                fontFamily = TajawalFamily,
                fontSize = 12.sp,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JournalPaper)
                    .border(1.dp, JournalMutedInk.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = savedAmountDh,
                    onValueChange = { savedAmountDh = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = TajawalFamily,
                        fontSize = 15.sp,
                        color = JournalWritingInk,
                        platformStyle = NoFontPadding
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    cursorBrush = SolidColor(JournalInk),
                    decorationBox = { inner ->
                        if (savedAmountDh.isEmpty()) {
                            Text(
                                text = "0",
                                fontFamily = TajawalFamily,
                                fontSize = 14.sp,
                                color = JournalMutedInk.copy(alpha = 0.6f),
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                        inner()
                    }
                )
                Text(
                    text = "DH",
                    fontFamily = TajawalFamily,
                    fontSize = 13.sp,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Q2: Biggest obstacle
            Text(
                text = if (isRtl) "2. شنو اللي عرقل التوفير أكثر؟" else "2. Quel a été le principal obstacle ?",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(Modifier.height(6.dp))
            listOf(
                "NONE" to (if (isRtl) "✅ مشى كلشي مزيان" else "✅ Tout s'est bien passé"),
                "EMERGENCY" to (if (isRtl) "🚨 مصروف طارئ مفاجئ" else "🚨 Dépense urgente imprévue"),
                "OVERSPEND" to (if (isRtl) "💸 خروج عن الميزانية (شوبينغ/قهاوي)" else "💸 Dépassement de budget"),
                "INCOME_LOW" to (if (isRtl) "📉 الدخل كان أقل من المتوقع" else "📉 Revenu inférieur aux attentes"),
                "DEBT" to (if (isRtl) "💳 التزامات أو ديون زادت" else "💳 Dettes ou charges supplémentaires")
            ).forEach { (key, label) ->
                val isSelected = biggestObstacle == key
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) HighlighterYellow.copy(alpha = 0.45f) else JournalPaper)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) JournalWritingInk.copy(alpha = 0.50f) else JournalMutedInk.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { biggestObstacle = key }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontFamily = TajawalFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(14.dp))

            // Q3: Income or obligations change
            Text(
                text = if (isRtl) "3. واش وقع تغيير مهم فالدخل أو المصاريف الثابتة؟" else "3. Changement majeur de revenu ou de charges fixes ?",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    false to (if (isRtl) "لا، الأمور مستقرة" else "Non, stable"),
                    true to (if (isRtl) "نعم، تبدل الوضع" else "Oui, changement")
                ).forEach { (v, label) ->
                    val isSelected = incomeOrObligationsChanged == v
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HighlighterBlue.copy(alpha = 0.35f) else JournalPaper)
                            .border(1.dp, if (isSelected) JournalWritingInk.copy(0.5f) else JournalMutedInk.copy(0.20f), RoundedCornerShape(8.dp))
                            .clickable { incomeOrObligationsChanged = v },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = TajawalFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Q4: Goal still same
            Text(
                text = if (isRtl) "4. واش الهدف والموعد ما زالين هما هما؟" else "4. L'objectif et la date restent-ils les mêmes ?",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    true to (if (isRtl) "نعم، مستمر بنفس الخطة" else "Oui, même plan"),
                    false to (if (isRtl) "خاص تعديل فالهدف/المدة" else "Besoin d'ajustement")
                ).forEach { (v, label) ->
                    val isSelected = goalStillSame == v
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) HighlighterGreen.copy(alpha = 0.35f) else JournalPaper)
                            .border(1.dp, if (isSelected) JournalWritingInk.copy(0.5f) else JournalMutedInk.copy(0.20f), RoundedCornerShape(8.dp))
                            .clickable { goalStillSame = v },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = TajawalFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.5.sp,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Submit button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HighlighterGreen.copy(alpha = 0.50f))
                    .border(1.dp, JournalWritingInk.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) {
                        val dh = savedAmountDh.toLongOrNull() ?: 0L
                        onSubmit(
                            MonthlyCheckInAnswers(
                                actualSavedCentimes = dh * 100,
                                biggestObstacle = biggestObstacle,
                                incomeOrObligationsChanged = incomeOrObligationsChanged,
                                goalStillSame = goalStillSame
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) "تحديث التشخيص المالي 🚀" else "Mettre à jour le diagnostic 🚀",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
