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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily

val GoalColors = listOf(
    "BLUE" to Color(0xFF38BDF8),
    "INDIGO" to Color(0xFF6366F1),
    "GREEN" to Color(0xFF10B981),
    "AMBER" to Color(0xFFF59E0B),
    "PURPLE" to Color(0xFFA855F7),
    "PINK" to Color(0xFFF43F5E),
    "TEAL" to Color(0xFF14B8A6)
)

private val QuickGoalSuggestions = listOf(
    "صندوق الطوارئ",
    "شراء سيارة",
    "تسبيق السكن",
    "عمرة / سفر",
    "تجهيز وزواج",
    "مشروع تجاري"
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NewSavingsGoalSheet(
    initialGoal: SavingsGoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        targetAmountCentimes: Long,
        initialAmountCentimes: Long,
        monthlyContributionCentimes: Long,
        targetDateEpochMs: Long?,
        colorTag: String
    ) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isEditing = initialGoal != null && initialGoal.id.isNotBlank()

    var title by remember(initialGoal) { mutableStateOf(initialGoal?.title ?: "") }
    var targetAmountStr by remember(initialGoal) {
        mutableStateOf(
            if (initialGoal != null) {
                val dh = initialGoal.targetAmountCentimes / 100.0
                if (dh % 1.0 == 0.0) dh.toLong().toString() else dh.toString()
            } else ""
        )
    }
    var initialAmountStr by remember(initialGoal) {
        mutableStateOf(
            if (initialGoal != null && initialGoal.currentAmountCentimes > 0) {
                val dh = initialGoal.currentAmountCentimes / 100.0
                if (dh % 1.0 == 0.0) dh.toLong().toString() else dh.toString()
            } else ""
        )
    }
    var monthlyContributionStr by remember(initialGoal) {
        mutableStateOf(
            if (initialGoal != null && initialGoal.monthlyContributionCentimes > 0) {
                val dh = initialGoal.monthlyContributionCentimes / 100.0
                if (dh % 1.0 == 0.0) dh.toLong().toString() else dh.toString()
            } else ""
        )
    }
    var selectedColor by remember(initialGoal) { mutableStateOf(initialGoal?.colorTag ?: "BLUE") }

    var isTitleFocused by remember { mutableStateOf(false) }
    var isTargetFocused by remember { mutableStateOf(false) }
    var isInitialFocused by remember { mutableStateOf(false) }
    var isMonthlyFocused by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }

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
            // Header Row: Dot + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dotColor = GoalColors.firstOrNull { it.first == selectedColor }?.second ?: Color(0xFF38BDF8)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = if (isEditing) {
                            if (isRtl) "تعديل هدف التوفير" else "Modifier l'objectif"
                        } else {
                            if (isRtl) "هدف توفير جديد" else "Nouvel objectif d'épargne"
                        },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestions Chips for Quick Goal Title
            if (!isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickGoalSuggestions.forEach { suggestion ->
                        val isSelected = title == suggestion
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) HighlighterBlue.copy(alpha = 0.45f)
                                    else JournalInk.copy(alpha = 0.05f)
                                )
                                .clickable {
                                    title = suggestion
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = suggestion,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) JournalWritingInk else JournalMutedInk
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 1. Goal Title Input
            Text(
                text = if (isRtl) "اسم الهدف أو المشروع *" else "Nom de l'objectif *",
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
                        if (isTitleFocused) JournalInk.copy(alpha = 0.6f) else JournalInk.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (title.isBlank()) {
                    Text(
                        text = if (isRtl) "مثال: 100,000 درهم لشراء شقة..." else "Ex: 100 000 DH pour avance...",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalWritingInk,
                        platformStyle = NoFontPadding
                    ),
                    cursorBrush = SolidColor(JournalWritingInk),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .onFocusChanged { isTitleFocused = it.isFocused }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Target Amount Input
            Text(
                text = if (isRtl) "المبلغ المطلوب (درهم) *" else "Montant cible (DH) *",
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
                        if (isTargetFocused) JournalInk.copy(alpha = 0.6f) else JournalInk.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (targetAmountStr.isBlank()) {
                    Text(
                        text = if (isRtl) "مثال: 100000" else "Ex: 100000",
                        fontFamily = PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                BasicTextField(
                    value = targetAmountStr,
                    onValueChange = { targetAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PatrickHandFamily,
                        fontSize = 16.sp,
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
                        .onFocusChanged { isTargetFocused = it.isFocused }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Initial Amount Input (Only when creating)
            if (!isEditing) {
                Text(
                    text = if (isRtl) "المبلغ المتوفر حالياً للبدء (اختياري)" else "Montant déjà épargné (optionnel)",
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
                            if (isInitialFocused) JournalInk.copy(alpha = 0.6f) else JournalInk.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (initialAmountStr.isBlank()) {
                        Text(
                            text = if (isRtl) "0 درهم" else "0 DH",
                            fontFamily = PatrickHandFamily,
                            fontSize = 15.sp,
                            color = JournalMutedInk.copy(alpha = 0.5f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    BasicTextField(
                        value = initialAmountStr,
                        onValueChange = { initialAmountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = PatrickHandFamily,
                            fontSize = 15.sp,
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
                            .onFocusChanged { isInitialFocused = it.isFocused }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Monthly Target Contribution (optional)
            Text(
                text = if (isRtl) "المساهمة الشهرية المخططة (درهم / شهر)" else "Épargne mensuelle prévue (DH / mois)",
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
                        if (isMonthlyFocused) JournalInk.copy(alpha = 0.6f) else JournalInk.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (monthlyContributionStr.isBlank()) {
                    Text(
                        text = if (isRtl) "مثال: 2000" else "Ex: 2000",
                        fontFamily = PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                BasicTextField(
                    value = monthlyContributionStr,
                    onValueChange = { monthlyContributionStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalWritingInk,
                        platformStyle = NoFontPadding
                    ),
                    cursorBrush = SolidColor(JournalWritingInk),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
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
                        .onFocusChanged { isMonthlyFocused = it.isFocused }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Color Tag Picker
            Text(
                text = if (isRtl) "لون التمييز" else "Couleur du badge",
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoalColors.forEach { (tag, col) ->
                    val isSelected = selectedColor == tag
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(col)
                            .clickable { selectedColor = tag }
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, JournalWritingInk, CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Check,
                                contentDescription = null,
                                tint = Color.White,
                                size = 16.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Action Button
            val isFormValid = title.isNotBlank() && (targetAmountStr.toDoubleOrNull() ?: 0.0) > 0.0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isFormValid) HighlighterGreen.copy(alpha = 0.55f)
                        else JournalMutedInk.copy(alpha = 0.15f)
                    )
                    .clickable(enabled = isFormValid) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        val targetCentimes = ((targetAmountStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        val initCentimes = ((initialAmountStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        val monthlyCentimes = ((monthlyContributionStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        onSave(
                            initialGoal?.id?.takeIf { it.isNotBlank() },
                            title.trim(),
                            targetCentimes,
                            initCentimes,
                            monthlyCentimes,
                            null,
                            selectedColor
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Check,
                        contentDescription = null,
                        tint = if (isFormValid) JournalWritingInk else JournalMutedInk.copy(alpha = 0.5f),
                        size = 18.dp
                    )
                    Text(
                        text = if (isEditing) {
                            if (isRtl) "حفظ التعديلات" else "Enregistrer les modifications"
                        } else {
                            if (isRtl) "إنشاء الهدف" else "Créer l'objectif"
                        },
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFormValid) JournalWritingInk else JournalMutedInk.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
