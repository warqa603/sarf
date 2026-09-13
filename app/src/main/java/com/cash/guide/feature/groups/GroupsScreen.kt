package com.cash.guide.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.SettingsRepository
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.domain.GroupCategory
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.domain.UnifiedGroupItem
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.JournalActionDelete
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.NotebookPrimaryActionButton
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalVisualOnRule
import com.cash.guide.ui.notebook.resolveJournalFont
import com.cash.guide.ui.notebook.isArabicScript

val GroupPalette = listOf(
    // Row 1: Warm & Earthy Pastels
    "#F4D66D", // Pastel Amber Yellow
    "#F7BDAB", // Pastel Peach
    "#F5B093", // Pastel Apricot
    "#F3A7B9", // Pastel Pink
    "#F28AA5", // Pastel Rose
    "#E8B4B8", // Pastel Dusty Rose
    "#E4C3AD", // Pastel Sand / Almond
    "#E8D5B5", // Pastel Cream / Latte
    // Row 2: Cool, Nature & Violet Pastels
    "#C9DDA0", // Pastel Sage Green
    "#A8E6CF", // Pastel Mint
    "#9BD7D5", // Pastel Aqua
    "#A8CFE3", // Pastel Sky Blue
    "#89B5D8", // Pastel Steel Blue
    "#B3C5E7", // Pastel Periwinkle
    "#D3C5E5", // Pastel Lavender
    "#C3A6CB"  // Pastel Lilac / Mauve
)

fun parseGroupColor(hex: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(HighlighterYellow)
}

@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    settingsRepository: SettingsRepository,
    onOpenGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val defaultCurrency by settingsRepository.defaultCurrency.collectAsState(initial = MoneyUnit.DIRHAM)
    val currencySuffix = if (defaultCurrency == MoneyUnit.DIRHAM) {
        stringResource(R.string.currency_dirham)
    } else {
        stringResource(R.string.currency_rial)
    }

    JournalRuledDocument(
        modifier = modifier.fillMaxSize(),
        clearFocusOnTap = true
    ) {
        // Line 1: Header Band ("Mes groupes" / "مجموعاتي" on Start, count on End)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.groups_title),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 16.5.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                color = JournalInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )

            Text(
                text = stringResource(R.string.groups_count_badge, state.groups.size),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 13.5.sp else 14.sp,
                fontWeight = FontWeight.Normal,
                color = JournalMutedInk.copy(alpha = 0.85f),
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }

        // Line 2: 1-rule spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 3: "+ Nouveau groupe" action button
        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
            NotebookPrimaryActionButton(
                text = stringResource(R.string.groups_new_action),
                onClick = { viewModel.openCreateDialog() }
            )
        }

        // Line 4: 1-rule spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 5: Category Filter Chips (Tous, Calculs, Notes, Checklists)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf(
                Pair<GroupCategory?, String>(null, stringResource(R.string.group_filter_all)),
                Pair<GroupCategory?, String>(GroupCategory.CALCULATIONS, stringResource(R.string.group_category_calculations)),
                Pair<GroupCategory?, String>(GroupCategory.NOTES, stringResource(R.string.group_category_notes)),
                Pair<GroupCategory?, String>(GroupCategory.CHECKLISTS, stringResource(R.string.group_category_checklists))
            )

            filters.forEach { (cat, label) ->
                val isSelected = state.filteredCategory == cat
                val count = when (cat) {
                    null -> state.groups.size
                    GroupCategory.CALCULATIONS -> state.calculationsGroupCount
                    GroupCategory.NOTES -> state.notesGroupCount
                    GroupCategory.CHECKLISTS -> state.checklistsGroupCount
                }
                val symbol = when (cat) {
                    null -> HisabiSymbol.Folder
                    GroupCategory.CALCULATIONS -> HisabiSymbol.Calculator
                    GroupCategory.NOTES -> HisabiSymbol.Pencil
                    GroupCategory.CHECKLISTS -> HisabiSymbol.Check
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) HighlighterYellow.copy(alpha = 0.45f)
                            else JournalInk.copy(alpha = 0.04f)
                        )
                        .then(
                            if (isSelected) Modifier.border(1.2.dp, JournalInk, RoundedCornerShape(8.dp))
                            else Modifier.border(0.5.dp, JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        )
                        .clickable { viewModel.setFilterCategory(cat) }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        HisabiSketchIcon(
                            symbol = symbol,
                            contentDescription = null,
                            tint = if (isSelected) JournalInk else JournalMutedInk,
                            size = 11.dp
                        )
                        Text(
                            text = "$label ($count)",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 11.5.sp else 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) JournalInk else JournalMutedInk
                        )
                    }
                }
            }
        }

        // Line 6: 1-rule spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 7+: Groups list or empty state
        val displayedGroups = state.displayedGroups
        if (displayedGroups.isEmpty() && !state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 4)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HisabiSketchIcon(
                    symbol = when (state.filteredCategory) {
                        GroupCategory.CALCULATIONS -> HisabiSymbol.Calculator
                        GroupCategory.NOTES -> HisabiSymbol.Pencil
                        GroupCategory.CHECKLISTS -> HisabiSymbol.Check
                        null -> HisabiSymbol.Folder
                    },
                    contentDescription = null,
                    tint = JournalMutedInk.copy(alpha = 0.50f),
                    size = 32.dp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.groups_empty_title),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk
                )
                Text(
                    text = stringResource(R.string.groups_empty_desc),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 12.5.sp else 13.sp,
                    color = JournalMutedInk.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            displayedGroups.forEachIndexed { index, groupItem ->
                NotebookGroupRow(
                    groupItem = groupItem,
                    currencySuffix = currencySuffix,
                    defaultCurrency = defaultCurrency,
                    onClick = { onOpenGroup(groupItem.group.id) },
                    onEdit = { viewModel.openEditDialog(groupItem.group) },
                    onDelete = { viewModel.promptDeleteGroup(groupItem.group) }
                )
            }
        }

        // Bottom breathing space
        Spacer(modifier = Modifier.height(JournalRuleSpacing * 5))
    }

    // Create / Edit Dialog
    if (state.isCreateOrEditDialogOpen) {
        CreateOrEditGroupDialog(
            isEditing = state.editingGroup != null,
            name = state.groupNameInput,
            onNameChange = { viewModel.updateGroupNameInput(it) },
            selectedColorHex = state.selectedColorHex,
            onColorSelect = { viewModel.selectColorHex(it) },
            selectedCategory = state.selectedCategory,
            onCategorySelect = { viewModel.selectCategory(it) },
            onConfirm = { viewModel.saveGroup() },
            onDismiss = { viewModel.dismissCreateOrEditDialog() }
        )
    }

    // Delete Confirmation Dialog
    state.groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            containerColor = JournalPaper,
            title = {
                Text(
                    text = stringResource(R.string.group_dialog_delete_title),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.group_dialog_delete_body),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 15.sp,
                    color = JournalWritingInk
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteGroup() }) {
                    Text(
                        text = stringResource(R.string.delete_confirm),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontWeight = FontWeight.Bold,
                        color = JournalActionDelete
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(
                        text = stringResource(R.string.delete_cancel),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        color = JournalMutedInk
                    )
                }
            }
        )
    }
}

@Composable
private fun NotebookGroupRow(
    groupItem: UnifiedGroupItem,
    currencySuffix: String,
    defaultCurrency: MoneyUnit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val groupColor = remember(groupItem.group.colorHex) {
        parseGroupColor(groupItem.group.colorHex)
    }

    val totalFormatted = JournalLedgerManager.formatTotal(groupItem.totalCentimes, defaultCurrency)
    val context = LocalContext.current
    val subtitle = remember(groupItem, isRtl, context) {
        val countStr = when (groupItem.category) {
            GroupCategory.CALCULATIONS -> when (groupItem.itemCount) {
                0 -> context.getString(R.string.group_calculations_count_zero)
                1 -> context.getString(R.string.group_calculations_count_single)
                else -> context.getString(R.string.group_calculations_count, groupItem.itemCount)
            }
            GroupCategory.NOTES -> when (groupItem.itemCount) {
                0 -> context.getString(R.string.group_notes_count_zero)
                1 -> context.getString(R.string.group_notes_count_single)
                else -> context.getString(R.string.group_notes_count, groupItem.itemCount)
            }
            GroupCategory.CHECKLISTS -> when (groupItem.itemCount) {
                0 -> context.getString(R.string.group_checklists_count_zero)
                1 -> context.getString(R.string.group_checklists_count_single)
                else -> context.getString(R.string.group_checklists_count, groupItem.itemCount)
            }
        }
        if (groupItem.previewTitles.isNotEmpty()) {
            val joined = groupItem.previewTitles.joinToString(if (isRtl) "، " else ", ")
            "$countStr · $joined"
        } else {
            countStr
        }
    }

    val categorySymbol = when (groupItem.category) {
        GroupCategory.CALCULATIONS -> HisabiSymbol.Calculator
        GroupCategory.NOTES -> HisabiSymbol.Pencil
        GroupCategory.CHECKLISTS -> HisabiSymbol.Check
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2) // 58dp
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        // Line 1 (29dp): Folder badge + Group Name on Start, (Total Amount for Calcs) + 3-dots on End
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Category Icon Badge + Group Name
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Sketched folder badge with group highlight tint & category icon
                Box(
                    modifier = Modifier
                        .journalVisualOnRule(gapAboveRule = 2.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(groupColor.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = categorySymbol,
                        contentDescription = null,
                        tint = JournalInk,
                        size = 14.dp
                    )
                }

                Text(
                    text = groupItem.group.name,
                    fontFamily = resolveJournalFont(groupItem.group.name, isRtl),
                    fontSize = if (isArabicScript(groupItem.group.name) || isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    maxLines = 1,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Subtle connecting line directly on the blue notebook line between Title and End
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
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                        )
                    }
            )

            // End: (Total Amount for Calculations) + 3-dots
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (groupItem.category == GroupCategory.CALCULATIONS) {
                    Text(
                        text = totalFormatted,
                        fontFamily = PatrickHandFamily,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = currencySuffix,
                        fontFamily = if (currencySuffix.contains(Regex("[a-zA-Z]"))) PatrickHandFamily else TajawalFamily,
                        fontSize = if (currencySuffix.contains(Regex("[a-zA-Z]"))) 13.5.sp else 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = JournalRuleSpacing)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.cd_more_options),
                            onClick = { menuExpanded = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = JournalMutedInk,
                        size = 14.dp
                    )

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(JournalPaper)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_edit),
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    color = JournalInk
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_delete),
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    color = JournalActionDelete
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        // Line 2 (29dp): Subtitle (e.g. "3 calculs · Chantier", "2 notes · Idée", "1 checklist · Marché") sitting directly on the rule
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
                fontSize = if (isRtl) 13.sp else 14.5.sp,
                fontWeight = FontWeight.Normal,
                color = JournalMutedInk.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                style = TextStyle(
                    platformStyle = NoFontPadding,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp)
                    .journalBaselineOnRule()
            )
        }
    }
}

@Composable
private fun CreateOrEditGroupDialog(
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    selectedColorHex: String,
    onColorSelect: (String) -> Unit,
    selectedCategory: GroupCategory,
    onCategorySelect: (GroupCategory) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        title = {
            val dialogTitle = if (isEditing) stringResource(R.string.group_dialog_edit_title) else stringResource(R.string.group_dialog_create_title)
            Text(
                text = dialogTitle,
                fontFamily = resolveJournalFont(dialogTitle, isRtl),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = JournalInk
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Selector
                Column {
                    Text(
                        text = stringResource(R.string.group_category_label),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        color = JournalMutedInk
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf(
                            Triple(GroupCategory.CALCULATIONS, stringResource(R.string.group_category_calculations), HisabiSymbol.Calculator),
                            Triple(GroupCategory.NOTES, stringResource(R.string.group_category_notes), HisabiSymbol.Pencil),
                            Triple(GroupCategory.CHECKLISTS, stringResource(R.string.group_category_checklists), HisabiSymbol.Check)
                        )
                        categories.forEach { (cat, label, symbol) ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) HighlighterYellow.copy(alpha = 0.40f)
                                        else JournalInk.copy(alpha = 0.04f)
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(1.5.dp, JournalInk, RoundedCornerShape(8.dp))
                                        else Modifier.border(0.5.dp, JournalMutedInk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    )
                                    .clickable { onCategorySelect(cat) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    HisabiSketchIcon(
                                        symbol = symbol,
                                        contentDescription = null,
                                        tint = if (isSelected) JournalInk else JournalMutedInk,
                                        size = 14.dp
                                    )
                                    Text(
                                        text = label,
                                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                        fontSize = if (isRtl) 12.5.sp else 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) JournalInk else JournalMutedInk,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Name Input with notebook baseline
                Column {
                    Text(
                        text = stringResource(R.string.group_dialog_name_hint),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        color = JournalMutedInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = onNameChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .drawBehind {
                                val strokeW = 1.2.dp.toPx()
                                val y = size.height
                                drawLine(
                                    color = JournalInk.copy(alpha = 0.6f),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = strokeW,
                                    cap = StrokeCap.Round
                                )
                            }
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        cursorBrush = SolidColor(JournalInk),
                        textStyle = TextStyle(
                            fontFamily = resolveJournalFont(name, isRtl),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalInk
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onConfirm() })
                    )
                }

                // Color picker
                Column {
                    Text(
                        text = stringResource(R.string.group_dialog_color_label),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        color = JournalMutedInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GroupPalette.chunked(8).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowColors.forEach { hex ->
                                    val color = parseGroupColor(hex)
                                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(29.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable { onColorSelect(hex) }
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, JournalInk, CircleShape)
                                                else Modifier.border(0.5.dp, JournalMutedInk.copy(alpha = 0.3f), CircleShape)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            HisabiSketchIcon(
                                                symbol = HisabiSymbol.Check,
                                                contentDescription = null,
                                                tint = JournalInk,
                                                size = 12.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.group_dialog_save),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (name.isNotBlank()) JournalInk else JournalMutedInk.copy(alpha = 0.4f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.group_dialog_cancel),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 14.sp,
                    color = JournalMutedInk
                )
            }
        }
    )
}
