package com.cash.guide.feature.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ModalBottomSheetProperties
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalSectionBadge
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.TajawalFamily

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SavingsWizardSheet(
    formState: WizardFormState,
    onPresetSelected: (String, Double) -> Unit,
    onCustomTitleChanged: (String) -> Unit,
    onTargetAmountChanged: (Double) -> Unit,
    onDurationChanged: (Int) -> Unit,
    onSalarySelected: (String, Double) -> Unit,
    onCustomSalaryChanged: (Double) -> Unit,
    onEssentialsSelected: (String) -> Unit,
    onLeisureSelected: (String) -> Unit,
    onLeakDailyCostChanged: (Double) -> Unit,
    onLeakDaysPerWeekChanged: (Int) -> Unit,
    onSavingsStyleSelected: (String) -> Unit,
    onInitialAmountChanged: (Double) -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BackHandler(enabled = true) {
        if (isImeVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        } else {
            onPrevStep()
        }
    }

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
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header with Steps progress
            WizardHeader(
                currentStep = formState.step,
                totalSteps = 7,
                onPrev = onPrevStep,
                onClose = onClose
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step Content
            when (formState.step) {
                1 -> Step1GoalAndAmount(
                    formState = formState,
                    isRtl = isRtl,
                    onPreset = onPresetSelected,
                    onCustomTitle = onCustomTitleChanged,
                    onAmount = onTargetAmountChanged
                )
                2 -> Step2Duration(
                    currentMonths = formState.durationMonths,
                    onSelect = onDurationChanged
                )
                3 -> Step3Salary(
                    salaryBracket = formState.salaryBracket,
                    salaryAmount = formState.salaryAmount,
                    isRtl = isRtl,
                    onSelect = onSalarySelected,
                    onCustomAmount = onCustomSalaryChanged
                )
                4 -> Step4Essentials(
                    currentBracket = formState.essentialBracket,
                    onSelect = onEssentialsSelected
                )
                5 -> Step5Leisure(
                    currentCategory = formState.leisureCategory,
                    dailyCost = formState.leakDailyCost,
                    daysPerWeek = formState.leakDaysPerWeek,
                    isRtl = isRtl,
                    onSelect = onLeisureSelected,
                    onDailyCostChanged = onLeakDailyCostChanged,
                    onDaysPerWeekChanged = onLeakDaysPerWeekChanged
                )
                6 -> Step6SavingsStyle(
                    currentStyle = formState.savingsStyle,
                    onSelect = onSavingsStyleSelected
                )
                7 -> Step7InitialAmountAndSummary(
                    formState = formState,
                    isRtl = isRtl,
                    onInitialAmount = onInitialAmountChanged
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Navigation Button (Height: 52dp, soft pastel, subtle border)
            val nextArrow = if (isRtl) "←" else "→"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (formState.step == 7) HighlighterGreen.copy(alpha = 0.45f) else HighlighterYellow.copy(alpha = 0.50f))
                    .border(1.dp, JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onNextStep()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (formState.step == 7) {
                            if (isRtl) "تحليل واعتماد الخطة 🚀" else "Analyser et valider le plan 🚀"
                        } else {
                            if (isRtl) "التالي $nextArrow" else "Suivant $nextArrow"
                        },
                        fontFamily = TajawalFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WizardHeader(
    currentStep: Int,
    totalSteps: Int,
    onPrev: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(29.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button
        Text(
            text = "إلغاء",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable(role = Role.Button, onClick = onClose)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Step Dots
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "سؤال $currentStep من $totalSteps",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            for (i in 1..totalSteps) {
                val isActive = i <= currentStep
                Box(
                    modifier = Modifier
                        .size(if (i == currentStep) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isActive) JournalInk else JournalMutedInk.copy(alpha = 0.3f))
                )
            }
        }

        // Back button
        if (currentStep > 1) {
            Text(
                text = "السابق",
                fontFamily = TajawalFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = JournalInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClick = onPrev)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }
    }
}

// -------------------------------------------------------------
// Step 1: Goal & Target Amount
// -------------------------------------------------------------
@Composable
private fun Step1GoalAndAmount(
    formState: WizardFormState,
    isRtl: Boolean,
    onPreset: (String, Double) -> Unit,
    onCustomTitle: (String) -> Unit,
    onAmount: (Double) -> Unit
) {
    val presets = listOf(
        Triple("CAR", "🚗 شراء سيارة", 80000.0),
        Triple("HOUSE", "🏠 تسبيق سكن / دار", 100000.0),
        Triple("EMERGENCY", "🛡️ صندوق الطوارئ", 25000.0),
        Triple("PROJECT", "💼 مشروع تجاري", 50000.0),
        Triple("EVENT", "✈️ مناسبة / سفر", 35000.0),
        Triple("CUSTOM", "🎯 هدف مخصص", 50000.0)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "شنو هو الهدف المالي اللي باغي توفر ليه؟",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Text(
            text = "اختار هدفك باش التطبيق يقاد ليك خطة واضحة بالدرهم:",
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset cards
        presets.forEach { (presetKey, title, defaultAmount) ->
            val isSelected = formState.goalPreset == presetKey
            NotebookChoiceCard(
                title = title,
                subtitle = if (presetKey != "CUSTOM") "المقترح: ${formatSavingsMoney(defaultAmount, isRtl)}" else "اكتب الاسم والمبلغ اللي بغيتي",
                isSelected = isSelected,
                onClick = { onPreset(presetKey, defaultAmount) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (formState.goalPreset == "CUSTOM") {
            Spacer(modifier = Modifier.height(6.dp))
            NotebookInputField(
                label = "اسم الهدف المخصص:",
                value = formState.customTitle,
                onValueChange = onCustomTitle,
                placeholder = "مثال: تجهيز مكتبة، عرس، دراجة..."
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Target Amount Input
        NotebookAmountField(
            label = "المبلغ المطلوب (درهم):",
            amount = formState.targetAmount,
            onAmountChange = onAmount
        )
    }
}

// -------------------------------------------------------------
// Step 2: Duration
// -------------------------------------------------------------
@Composable
private fun Step2Duration(
    currentMonths: Int,
    onSelect: (Int) -> Unit
) {
    val durations = listOf(
        Pair(6, "⚡ 6 أشهر (تحدي سريع ومركز)"),
        Pair(12, "🗓️ سنة واحدة - 12 شهر (المدة النموذجية)"),
        Pair(24, "⏳ سنتين - 24 شهر (توازن ممتاز للأهداف الكبيرة)"),
        Pair(36, "🎯 3 سنوات - 36 شهر (أريحية تامة باقتطاع خفيف)")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "فاشمن مدة زمنية بغيتي تحقق هاد الهدف؟",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Text(
            text = "كلما كانت المدة أطول كيكون القسط الشهري خفيف، والعكس صحيح:",
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(14.dp))

        durations.forEach { (months, label) ->
            val isSelected = currentMonths == months
            NotebookChoiceCard(
                title = label,
                subtitle = when (months) {
                    6 -> "باغي توصل بالزربة ومستعد تزير راسك"
                    12 -> "سنة كاملة ومناسبة لأغلب الأهداف"
                    24 -> "أحسن مدة للمشاريع والسكن والسيارات"
                    else -> "اقتطاع شهري مريح جداً بلا ضغط"
                },
                isSelected = isSelected,
                onClick = { onSelect(months) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// -------------------------------------------------------------
// Step 3: Salary
// -------------------------------------------------------------
@Composable
private fun Step3Salary(
    salaryBracket: String,
    salaryAmount: Double,
    isRtl: Boolean,
    onSelect: (String, Double) -> Unit,
    onCustomAmount: (Double) -> Unit
) {
    val brackets = listOf(
        Triple("LOW", "🟢 أقل من \u200E4\u00A0000\u200E درهم (بداية المسار / السميك)", 3500.0),
        Triple("MED", "🔵 بين \u200E4\u00A0000\u200E و \u200E7\u00A0000\u200E درهم (دخل متوسط معتاد)", 5500.0),
        Triple("GOOD", "🟣 بين \u200E7\u00A0000\u200E و \u200E12\u00A0000\u200E درهم (دخل متوسط جيد)", 9000.0),
        Triple("HIGH", "🟡 أكثر من \u200E12\u00A0000\u200E درهم (دخل مرتاح)", 15000.0)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "شحال الصالير أو المدخول الشهري الصافي ديالك؟",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Text(
            text = "هاد المعلومة كتبقى محفوظة عندك فالهاتف فقط باش نحسبو الخطة الواقعية:",
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(14.dp))

        brackets.forEach { (key, title, defaultAmount) ->
            val isSelected = salaryBracket == key
            NotebookChoiceCard(
                title = title,
                subtitle = "المتوسط التقريبي: ${formatSavingsMoney(defaultAmount, isRtl)}",
                isSelected = isSelected,
                onClick = { onSelect(key, defaultAmount) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        NotebookAmountField(
            label = "ضبط الصالير الشهري بالدرهم:",
            amount = salaryAmount,
            onAmountChange = onCustomAmount
        )
    }
}

// -------------------------------------------------------------
// Step 4: Essentials
// -------------------------------------------------------------
@Composable
private fun Step4Essentials(
    currentBracket: String,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        Triple("LOW", "🟢 خفيفة (أقل من 40% من الصالير)", "ساكن مع العائلة أو كراء رخيص، بدون ديون أو كريديات."),
        Triple("MEDIUM", "🔵 متوسطة ومضبوطة (حوالي 50% من الصالير)", "كراء معقول، فواتير الماء والضوء، ومصروف البيت العادي."),
        Triple("HIGH", "🔴 ضاغطة وكبيرة (أكثر من 70% من الصالير)", "كراء غالي، كريديات سابقة، أو التزامات عائلية كبيرة.")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "حجم المصاريف الإجبارية والالتزامات الثابتة؟",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Text(
            text = "(الكراء، الفواتير، التقضية الأساسية، القروض والكريديات):",
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(14.dp))

        options.forEach { (key, title, desc) ->
            val isSelected = currentBracket == key
            NotebookChoiceCard(
                title = title,
                subtitle = desc,
                isSelected = isSelected,
                onClick = { onSelect(key) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// -------------------------------------------------------------
// Step 5: Leisure & Spending Leak Audit
// -------------------------------------------------------------
@Composable
private fun Step5Leisure(
    currentCategory: String,
    dailyCost: Double,
    daysPerWeek: Int,
    isRtl: Boolean,
    onSelect: (String) -> Unit,
    onDailyCostChanged: (Double) -> Unit,
    onDaysPerWeekChanged: (Int) -> Unit
) {
    val leakInfo = SavingsKnowledgeBase.getLeak(currentCategory)
    val shock = SavingsKnowledgeBase.computeShockNumbers(dailyCost, daysPerWeek)

    val options = listOf(
        Triple("CAFE", if (isRtl) "☕ القهاوي، المطاعم والماكلة برا (سناك، طاكوس، بيتزا)" else "☕ Cafés, snacks, restos (Tacos, Pizza)", if (isRtl) "استنزاف متكرر بالدرهم الصغير كيتجمع فآخر الشهر بدون ما تشعر." else "Petites dépenses quotidiennes qui s'accumulent."),
        Triple("SHOPPING", if (isRtl) "🛍️ الشوبينغ العشوائي، الملابس، والإلكترونيات" else "🛍️ Shopping impulsif, vêtements & gadgets", if (isRtl) "شراء رغبات لحظية وكماليات استهلاكية ممكن تأجيلها." else "Achats coup de cœur et extras non planifiés."),
        Triple("OUTINGS", if (isRtl) "🚗 الخرجات، الكازوال، وسفريات الويكاند" else "🚗 Sorties, carburant & week-ends", if (isRtl) "مصاريف نهاية الأسبوع والتنقل والأنشطة الترفيهية." else "Frais de loisirs du week-end et trajets imprévus."),
        Triple("SUBSCRIPTIONS", if (isRtl) "📱 اشتراكات وفورفيات زائدة ومصاريف متفرقة" else "📱 Abonnements dormants & petits extras", if (isRtl) "أنترنت زائد، تطبيقات، واشتراكات مهجورة كتقطع شهرياً." else "Forfaits surdimensionnés et abonnements oubliés.")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isRtl) "فين كتمشي أغلب فلوس الرفاهية والنشاط؟" else "Où passe la majeure partie de vos extras ?",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Text(
            text = if (isRtl) "اختار الباب اللي باغي نركزو عليه فالتحليل وننقصو منو:" else "Sélectionnez le poste de dépense à auditer :",
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(14.dp))

        options.forEach { (key, title, desc) ->
            val isSelected = currentCategory == key
            NotebookChoiceCard(
                title = title,
                subtitle = desc,
                isSelected = isSelected,
                onClick = { onSelect(key) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- SUB-QUESTION 1: Daily cost ---
        Text(
            text = if (isRtl) "شحال تقريباً كتقام عليك فاليوم؟ (قهوة، سناك، أو مشتريات)" else "Combien dépensez-vous environ par jour ?",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            leakInfo.quickCostOptionsDh.forEachIndexed { index, cost ->
                val label = if (isRtl) leakInfo.quickCostLabelsAr.getOrElse(index) { "${cost.toInt()} DH" }
                            else leakInfo.quickCostLabelsFr.getOrElse(index) { "${cost.toInt()} DH" }
                val isCostSelected = dailyCost == cost
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCostSelected) HighlighterYellow.copy(alpha = 0.50f) else JournalPaper)
                        .border(1.dp, if (isCostSelected) JournalWritingInk.copy(alpha = 0.6f) else JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable { onDailyCostChanged(cost) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = TajawalFamily,
                        fontWeight = if (isCostSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = JournalWritingInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- SUB-QUESTION 2: Frequency days per week ---
        Text(
            text = if (isRtl) "شحال من يوم فالسيمانة كيتكرر هاد الصرف؟" else "Combien de jours par semaine ?",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val daysOptions = listOf(
            3 to (if (isRtl) "3 أيام (ويكاند)" else "3j (Week-end)"),
            5 to (if (isRtl) "5 أيام (الخدمة)" else "5j (Boulot)"),
            6 to (if (isRtl) "6 أيام (ديما)" else "6j (Presque tout)"),
            7 to (if (isRtl) "7 أيام (كل نهار)" else "7j (Tous les jours)")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            daysOptions.forEach { (days, label) ->
                val isDaysSelected = daysPerWeek == days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDaysSelected) HighlighterGreen.copy(alpha = 0.45f) else JournalPaper)
                        .border(1.dp, if (isDaysSelected) JournalWritingInk.copy(alpha = 0.6f) else JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable { onDaysPerWeekChanged(days) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = TajawalFamily,
                        fontWeight = if (isDaysSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = JournalWritingInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- LIVE SHOCK CALCULATION CARD (Stamped Paper Box) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HighlighterPink.copy(alpha = 0.18f))
                .border(1.dp, HighlighterPink.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💥",
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isRtl) "صدمة الحساب الحقيقي (Le Choc des Montants):" else "Le calcul choc de cette habitude :",
                        fontFamily = TajawalFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isRtl) {
                        "هاد العادة بوحدها كتكلفك تقريباً \u200E${shock.formatYearlyDrain()}\u200E درهم فالعام (\u200E${shock.formatMonthlyDrain()}\u200E DH شهرياً)!\nإلى خفضتيها غير للنصف، غادي توفر \u200E+${shock.formatHalfCutYearly()}\u200E درهم كل عام فجيبك."
                    } else {
                        "Cette habitude vous coûte environ \u200E${shock.formatYearlyDrain()}\u200E DH/an (\u200E${shock.formatMonthlyDrain()}\u200E DH/mois) !\nEn la réduisant de moitié, vous économisez \u200E+${shock.formatHalfCutYearly()}\u200E DH/an."
                    },
                    fontFamily = TajawalFamily,
                    fontSize = 12.5.sp,
                    color = JournalWritingInk,
                    lineHeight = 18.sp,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Step 6: Savings Style
// -------------------------------------------------------------
@Composable
private fun Step6SavingsStyle(
    currentStyle: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "شنو هو الأسلوب اللي باغي تمشي عليه فالتوفير؟",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Text(
            text = "الأسلوب هو اللي غادي يحدد نسبة التزيار ونوع النصائح:",
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(14.dp))

        NotebookChoiceCard(
            title = "🔥 أسلوب التقشف السريع (Mode Turbo)",
            subtitle = "مستعد نزير راسي ونقطع أقصى حد من الكماليات باش نوصل لهدفي فـ أسرع وقت ممكن.",
            isSelected = currentStyle == "TURBO",
            onClick = { onSelect("TURBO") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        NotebookChoiceCard(
            title = "🌿 أسلوب متوازن ومريح (Mode Équilibré)",
            subtitle = "باغي نوفر بانتظام ولكن بلا ما نخنق راسي، مع الحفاظ على جزء معقول من الخروجات والراحة.",
            isSelected = currentStyle == "BALANCED",
            onClick = { onSelect("BALANCED") }
        )
    }
}

// -------------------------------------------------------------
// Step 7: Initial Amount & Summary
// -------------------------------------------------------------
@Composable
private fun Step7InitialAmountAndSummary(
    formState: WizardFormState,
    isRtl: Boolean,
    onInitialAmount: (Double) -> Unit
) {
    val quickAmounts = listOf(0.0, 1000.0, 2000.0, 5000.0, 10000.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "واش عندك شي بركة واجدة دابا تبدا بيها؟",
            fontFamily = TajawalFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = JournalWritingInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Text(
            text = "إذا كانت عندك شي حاجة مخبية غادي نقصوها مباشرة من الهدف:",
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick amount chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickAmounts.forEach { amt ->
                val isSelected = formState.initialAmount == amt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) HighlighterYellow.copy(alpha = 0.40f) else JournalPaper)
                        .border(1.dp, if (isSelected) JournalWritingInk.copy(alpha = 0.5f) else JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable { onInitialAmount(amt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (amt == 0.0) {
                            if (isRtl) "0 درهم" else "0 DH"
                        } else {
                            "\u200E+${JournalLedgerManager.formatFrenchNumber(amt.toLong().toString())}\u200E"
                        },
                        fontFamily = TajawalFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        NotebookAmountField(
            label = "المبلغ الأولي المتوفر (درهم):",
            amount = formState.initialAmount,
            onAmountChange = onInitialAmount
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Summary Preview Card (Notebook Stamped style)
        val remaining = (formState.targetAmount - formState.initialAmount).coerceAtLeast(0.0)
        val monthlyReq = if (formState.durationMonths > 0) remaining / formState.durationMonths else remaining
        val dailyReq = monthlyReq / 30.0

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HighlighterGreen.copy(alpha = 0.20f))
                .border(1.dp, JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 ملخص خطتك الشخصية:",
                        fontFamily = TajawalFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                    JournalSectionBadge(
                        title = "${formState.durationMonths} شهر",
                        badgeColor = HighlighterYellow.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• الهدف: ${formState.displayTitle} (${formatSavingsMoney(formState.targetAmount, isRtl)})",
                    fontFamily = TajawalFamily,
                    fontSize = 13.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
                Text(
                    text = "• المطلوب توفيره شهرياً: ${formatSavingsMoney(monthlyReq, isRtl)} (أو ~${formatSavingsMoney(dailyReq, isRtl)}/يوم)",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF00796B),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Helper UI Components in Notebook Style
// -------------------------------------------------------------
@Composable
private fun NotebookChoiceCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) HighlighterYellow.copy(alpha = 0.40f) else JournalPaper)
            .border(
                width = if (isSelected) 1.2.dp else 1.dp,
                color = if (isSelected) JournalWritingInk.copy(alpha = 0.6f) else JournalMutedInk.copy(alpha = 0.25f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = TajawalFamily,
                    fontSize = 12.sp,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Checkmark or radio circle
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) JournalInk else Color.Transparent)
                    .border(1.5.dp, if (isSelected) JournalInk else JournalMutedInk.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(1.2.dp, JournalInk, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = TajawalFamily,
                    fontSize = 14.sp,
                    color = JournalMutedInk.copy(alpha = 0.5f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = TajawalFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = JournalWritingInk,
                    platformStyle = NoFontPadding
                ),
                cursorBrush = SolidColor(JournalInk),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NotebookAmountField(
    label: String,
    amount: Double,
    onAmountChange: (Double) -> Unit
) {
    var textValue by remember(amount) { mutableStateOf(if (amount > 0) amount.toInt().toString() else "") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = TajawalFamily,
            fontSize = 13.sp,
            color = JournalMutedInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JournalPaper)
                .border(1.2.dp, JournalInk, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (textValue.isEmpty()) {
                        Text(
                            text = "0",
                            fontFamily = TajawalFamily,
                            fontSize = 16.sp,
                            color = JournalMutedInk.copy(alpha = 0.5f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    BasicTextField(
                        value = textValue,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            textValue = filtered
                            val parsed = filtered.toDoubleOrNull() ?: 0.0
                            onAmountChange(parsed)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = TajawalFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk,
                            platformStyle = NoFontPadding
                        ),
                        cursorBrush = SolidColor(JournalInk),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "درهم",
                    fontFamily = TajawalFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }
    }
}
