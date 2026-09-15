package com.cash.guide.feature.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily

private val QuickAmounts = listOf(100L, 200L, 500L, 1000L, 2000L)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddDepositSheet(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onConfirmDeposit: (amountCentimes: Long, note: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isAmountFocused by remember { mutableStateOf(false) }
    var isNoteFocused by remember { mutableStateOf(false) }

    val currentDh = goal.currentAmountCentimes / 100.0
    val targetDh = goal.targetAmountCentimes / 100.0
    val progressPercent = if (targetDh > 0) ((currentDh / targetDh) * 100).toInt().coerceIn(0, 100) else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetDefaults.properties(shouldDismissOnBackPress = false),
        containerColor = JournalPaper,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        val isImeVisible = WindowInsets.isImeVisible

        BackHandler {
            if (isImeVisible) {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            } else {
                onDismiss()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = if (isRtl) "إضافة مبلغ للتوفير" else "Ajouter une épargne",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 18.sp else 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Close,
                        contentDescription = "Fermer",
                        tint = JournalMutedInk,
                        size = 14.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Goal summary card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighlighterYellow.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .border(1.dp, JournalInk.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = goal.title,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isRtl) {
                                "المجموع: ${formatSavingsMoney(currentDh, true)} من ${formatSavingsMoney(targetDh, true)}"
                            } else {
                                "Actuel: ${formatSavingsMoney(currentDh, false)} / ${formatSavingsMoney(targetDh, false)}"
                            },
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 13.sp,
                            color = JournalMutedInk
                        )
                        Text(
                            text = "\u200E$progressPercent%\u200E",
                            fontFamily = PatrickHandFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Amount Buttons
            Text(
                text = if (isRtl) "مبالغ سريعة" else "Montants rapides",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAmounts.forEach { amt ->
                    val isSelected = amountStr == amt.toString()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) HighlighterGreen.copy(alpha = 0.45f)
                                else JournalInk.copy(alpha = 0.06f)
                            )
                            .clickable {
                                amountStr = amt.toString()
                                focusManager.clearFocus()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isRtl) "\u200E+${amt}\u200E درهم" else "+$amt DH",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom Amount Input
            Text(
                text = if (isRtl) "المبلغ المراد إضافته (درهم) *" else "Montant à ajouter (DH) *",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (isAmountFocused) JournalInk.copy(alpha = 0.6f) else JournalInk.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (amountStr.isBlank()) {
                    Text(
                        text = if (isRtl) "أدخل المبلغ..." else "Entrez le montant...",
                        fontFamily = PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                BasicTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PatrickHandFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        platformStyle = NoFontPadding
                    ),
                    cursorBrush = SolidColor(JournalWritingInk),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isAmountFocused = it.isFocused }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Note Input
            Text(
                text = if (isRtl) "ملاحظة (اختياري)" else "Note (optionnel)",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (isNoteFocused) JournalInk.copy(alpha = 0.6f) else JournalInk.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (note.isBlank()) {
                    Text(
                        text = if (isRtl) "مثال: توفير من راتب هذا الشهر..." else "Ex: Épargne du salaire...",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 14.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 14.sp,
                        color = JournalWritingInk,
                        platformStyle = NoFontPadding
                    ),
                    cursorBrush = SolidColor(JournalWritingInk),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isNoteFocused = it.isFocused }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Button
            val amountVal = amountStr.toDoubleOrNull() ?: 0.0
            val isValid = amountVal > 0.0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isValid) HighlighterGreen.copy(alpha = 0.55f)
                        else JournalMutedInk.copy(alpha = 0.15f)
                    )
                    .clickable(enabled = isValid) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        val centimes = (amountVal * 100).toLong()
                        onConfirmDeposit(centimes, note.trim())
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Plus,
                        contentDescription = null,
                        tint = if (isValid) JournalWritingInk else JournalMutedInk.copy(alpha = 0.5f),
                        size = 16.dp
                    )
                    Text(
                        text = if (isRtl) "تأكيد إضافة المبلغ" else "Confirmer l'ajout",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) JournalWritingInk else JournalMutedInk.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
