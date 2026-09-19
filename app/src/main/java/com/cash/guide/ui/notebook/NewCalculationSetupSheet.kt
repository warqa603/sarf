package com.cash.guide.ui.notebook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.TemplateRepository
import com.cash.guide.domain.MoneyUnit
import kotlinx.coroutines.launch

/**
 * Modern Notebook Sheet for New Calculation Setup:
 * - Slides up as ModalBottomSheet matching NewReminderSheet aesthetics.
 * - Single solid pastel backgrounds without harsh outlines.
 * - Text sitting precisely on the baseline without clipping.
 * - Elegant tactile horizontal scroll row of templates replacing the clunky dropdown.
 * - Clean title input with native keyboard and clear '✕' button.
 * - Single solid background choice chips for Type and Currency.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewCalculationSetupSheet(
    defaultCurrency: MoneyUnit = MoneyUnit.DIRHAM,
    templateRepository: TemplateRepository? = null,
    onConfirm: (title: String, calcType: String, currency: MoneyUnit, templateId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val repo = templateRepository ?: remember { TemplateRepository.getInstance(context) }
    val customTemplates by repo.customTemplates.collectAsState(emptyList())
    val builtInTemplates = remember(isRtl) { repo.getBuiltInTemplates(isRtl) }

    var isTitleFocused by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("PERSONNEL") } // "PERSONNEL" or "CREDIT"
    var selectedCurrency by remember { mutableStateOf(defaultCurrency) }
    var selectedModelName by remember { mutableStateOf<String?>(null) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    val titleFocusRequester = remember { FocusRequester() }

    fun submit() {
        onConfirm(title.trim(), selectedType, selectedCurrency, selectedTemplateId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
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
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Dot Indicator + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dotColor = if (selectedType == "CREDIT") Color(0xFFEA580C) else HighlighterBlue
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )
                    Text(
                        text = stringResource(R.string.new_calc_sheet_title),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 18.sp else 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
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

            // 1. Modèles de calcul (Elegant Horizontal Scrolling Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📋",
                        fontSize = 13.sp
                    )
                    Text(
                        text = stringResource(R.string.new_calc_templates_label),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                if (selectedTemplateId != null || title.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.new_calc_clear_selection),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalActionDelete.copy(alpha = 0.85f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                selectedTemplateId = null
                                selectedModelName = null
                                title = ""
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Vierge / Personnalisé" chip
                val isViergeSelected = selectedTemplateId == null && title.isBlank()
                TemplateChip(
                    title = stringResource(R.string.new_calc_template_blank),
                    isSelected = isViergeSelected,
                    isRtl = isRtl,
                    onClick = {
                        selectedTemplateId = null
                        selectedModelName = null
                        title = ""
                    }
                )

                // Custom user templates (if any)
                customTemplates.forEach { tpl ->
                    val isSelected = selectedTemplateId == tpl.id || (selectedModelName == tpl.title && title == tpl.title)
                    TemplateChip(
                        title = tpl.title,
                        isSelected = isSelected,
                        isRtl = isRtl,
                        isCustom = true,
                        onDeleteCustom = {
                            coroutineScope.launch {
                                repo.deleteCustomTemplate(tpl.id)
                                if (selectedTemplateId == tpl.id) {
                                    selectedTemplateId = null
                                    selectedModelName = null
                                }
                            }
                        },
                        onClick = {
                            selectedTemplateId = tpl.id
                            selectedModelName = tpl.title
                            title = tpl.title
                            selectedType = tpl.calcType
                            selectedCurrency = runCatching { MoneyUnit.valueOf(tpl.currency) }.getOrDefault(selectedCurrency)
                        }
                    )
                }

                // Built-in templates
                builtInTemplates.forEach { tpl ->
                    val isSelected = selectedTemplateId == tpl.id || (selectedModelName == tpl.title && title == tpl.title)
                    TemplateChip(
                        title = tpl.title,
                        isSelected = isSelected,
                        isRtl = isRtl,
                        onClick = {
                            selectedTemplateId = tpl.id
                            selectedModelName = tpl.title
                            title = tpl.title
                            selectedType = tpl.calcType
                            selectedCurrency = runCatching { MoneyUnit.valueOf(tpl.currency) }.getOrDefault(selectedCurrency)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Nom du calcul ou client (Input field)
            Text(
                text = stringResource(R.string.new_calc_name_label),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .border(1.dp, JournalInk.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (title.isBlank()) {
                    Text(
                        text = stringResource(R.string.new_calc_name_placeholder),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(titleFocusRequester)
                            .onFocusChanged { isTitleFocused = it.isFocused },
                        textStyle = TextStyle(
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk,
                            platformStyle = NoFontPadding
                        ),
                        cursorBrush = SolidColor(JournalInk),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )
                    if (title.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    title = ""
                                    selectedTemplateId = null
                                    selectedModelName = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalMutedInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Type de calcul (Personnel vs Crédit) - Single Solid Pastel Color, NO Harsh Outline
            Text(
                text = stringResource(R.string.new_calc_type_label),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isPersonnel = selectedType == "PERSONNEL"
                SetupChoiceChip(
                    label = stringResource(R.string.calc_type_personnel),
                    isSelected = isPersonnel,
                    selectedBgColor = HighlighterBlue.copy(alpha = 0.35f),
                    selectedTextColor = JournalWritingInk,
                    isRtl = isRtl,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        if (isPersonnel) {
                            Text(
                                text = "✓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk
                            )
                        }
                    },
                    onClick = { selectedType = "PERSONNEL" }
                )

                val isCredit = selectedType == "CREDIT"
                SetupChoiceChip(
                    label = stringResource(R.string.calc_type_credit),
                    isSelected = isCredit,
                    selectedBgColor = Color(0xFFFFEDD5),
                    selectedTextColor = Color(0xFF9A3412),
                    isRtl = isRtl,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        if (isCredit) {
                            Text(
                                text = "✓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412)
                            )
                        }
                    },
                    onClick = { selectedType = "CREDIT" }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Devise (DH vs Rial) - Single Solid Pastel Color, NO Harsh Outline
            Text(
                text = stringResource(R.string.new_calc_currency_label),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isDh = selectedCurrency == MoneyUnit.DIRHAM
                SetupChoiceChip(
                    label = if (isRtl) "درهم (DH)" else "Dirham (DH)",
                    isSelected = isDh,
                    selectedBgColor = HighlighterGreen.copy(alpha = 0.35f),
                    selectedTextColor = JournalWritingInk,
                    isRtl = isRtl,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        if (isDh) {
                            Text(
                                text = "✓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk
                            )
                        }
                    },
                    onClick = { selectedCurrency = MoneyUnit.DIRHAM }
                )

                val isRial = selectedCurrency == MoneyUnit.RIAL
                SetupChoiceChip(
                    label = if (isRtl) "ريال (Rial)" else "Rial (Rial)",
                    isSelected = isRial,
                    selectedBgColor = HighlighterGreen.copy(alpha = 0.35f),
                    selectedTextColor = JournalWritingInk,
                    isRtl = isRtl,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        if (isRial) {
                            Text(
                                text = "✓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk
                            )
                        }
                    },
                    onClick = { selectedCurrency = MoneyUnit.RIAL }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 5. Action Button: "Commencer" / "ابدأ الحساب"
            val startLabel = stringResource(R.string.new_calc_start_action)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedType == "CREDIT") Color(0xFFEA580C)
                        else HighlighterPink.copy(alpha = 0.85f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selectedType == "CREDIT") Color(0xFFC2410C) else JournalInk.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            keyboardController?.hide()
                            submit()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Check,
                        contentDescription = null,
                        tint = if (selectedType == "CREDIT") Color.White else JournalWritingInk,
                        size = 16.dp
                    )
                    Text(
                        text = startLabel,
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedType == "CREDIT") Color.White else JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Clean Choice Chip for ModalBottomSheet:
 * - When selected: Solid pastel wash background, ZERO harsh outline.
 * - When unselected: Clean soft white background with delicate border.
 * - Text is firmly anchored with NoFontPadding.
 */
@Composable
private fun SetupChoiceChip(
    label: String,
    isSelected: Boolean,
    selectedBgColor: Color,
    selectedTextColor: Color = JournalWritingInk,
    isRtl: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) selectedBgColor
                else Color.White.copy(alpha = 0.55f)
            )
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = JournalInk.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    // Single solid color background, no harsh outline!
                    Modifier
                }
            )
            .clickable(
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = label,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 14.sp else 14.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedTextColor else JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}

/**
 * Tactile Template Chip for Horizontal Scrolling Row:
 * - Easily discoverable, 1-tap template selection.
 * - Highlights with gentle yellow wash without harsh outline.
 */
@Composable
private fun TemplateChip(
    title: String,
    isSelected: Boolean,
    isRtl: Boolean,
    isCustom: Boolean = false,
    onDeleteCustom: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) HighlighterYellow.copy(alpha = 0.60f)
                else Color.White.copy(alpha = 0.65f)
            )
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = JournalInk.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 12.5.sp else 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) JournalWritingInk else JournalMutedInk,
                style = TextStyle(platformStyle = NoFontPadding),
                maxLines = 1
            )
            if (isCustom && onDeleteCustom != null) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable { onDeleteCustom() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 10.sp,
                        color = JournalActionDelete.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
