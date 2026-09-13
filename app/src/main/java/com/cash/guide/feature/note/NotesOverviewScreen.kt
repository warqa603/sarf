package com.cash.guide.feature.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.db.NoteEntity
import com.cash.guide.domain.GroupCategory
import com.cash.guide.feature.groups.AssignToGroupDialog
import androidx.compose.ui.graphics.PathEffect
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.NotebookSearchField
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalSectionBadge
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.MonthPickerDialog
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.resolveJournalFont
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NoteColorOption(
    val tag: String,
    val lightColor: Color,
    val dotColor: Color
)

val NoteColorPalette = listOf(
    NoteColorOption("PINK", Color(0xFFFBCFE8), Color(0xFFE879A8)),       // Soft Rose
    NoteColorOption("PEACH", Color(0xFFFFD8B8), Color(0xFFF59E6B)),      // Soft Peach / Coral
    NoteColorOption("YELLOW", Color(0xFFFEF08A), Color(0xFFEAB308)),     // Soft Lemon Yellow
    NoteColorOption("AMBER", Color(0xFFFED7AA), Color(0xFFF59E0B)),      // Soft Warm Amber
    NoteColorOption("MINT", Color(0xFFBBF7D0), Color(0xFF34D399)),       // Soft Mint Green
    NoteColorOption("GREEN", Color(0xFFC8E6C9), Color(0xFF5E9C47)),      // Soft Sage Green
    NoteColorOption("TEAL", Color(0xFF99F6E4), Color(0xFF14B8A6)),       // Soft Aqua Teal
    NoteColorOption("SKY", Color(0xFFBAE6FD), Color(0xFF38BDF8)),        // Soft Sky Blue
    NoteColorOption("INDIGO", Color(0xFFC7D2FE), Color(0xFF818CF8)),     // Soft Periwinkle
    NoteColorOption("PURPLE", Color(0xFFE9D5FF), Color(0xFFA855F7)),     // Soft Lavender
    NoteColorOption("ROSE", Color(0xFFFCE7F3), Color(0xFFF43F5E)),       // Soft Blush Rose
    NoteColorOption("SAND", Color(0xFFF5E6D3), Color(0xFFD97706))        // Soft Warm Sand
)

/** Color helpers for notes accent dots & highlights */
fun getNoteDotColor(colorTag: String, fallbackIndex: Int = 0): Color {
    val upper = colorTag.uppercase()
    val match = NoteColorPalette.firstOrNull {
        it.tag == upper ||
        (it.tag == "SKY" && upper == "BLUE") ||
        (it.tag == "PEACH" && upper == "ORANGE")
    }
    return match?.dotColor ?: NoteColorPalette[fallbackIndex % NoteColorPalette.size].dotColor
}

fun getNoteHighlightPillColor(colorTag: String, fallbackIndex: Int = 0): Color {
    val upper = colorTag.uppercase()
    val match = NoteColorPalette.firstOrNull {
        it.tag == upper ||
        (it.tag == "SKY" && upper == "BLUE") ||
        (it.tag == "PEACH" && upper == "ORANGE")
    }
    return match?.lightColor ?: NoteColorPalette[fallbackIndex % NoteColorPalette.size].lightColor
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NotesOverviewScreen(
    viewModel: NotesOverviewViewModel,
    calculationRepository: CalculationRepository,
    onNavigateBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showMonthPicker by remember { mutableStateOf(false) }
    var noteToAssignToGroup by remember { mutableStateOf<NoteEntity?>(null) }

    val shortDateFormatter = remember {
        SimpleDateFormat("d MMM", Locale.getDefault())
    }
    val currentMonthHeaderFormatter = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    }
    val currentMonthDisplay = remember {
        val raw = currentMonthHeaderFormatter.format(Date())
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    // Search focus state
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val isImeVisible = WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && isSearchFocused) {
            focusManager.clearFocus()
        }
    }

    if (isSearchFocused) {
        BackHandler {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // Clear focus on open
    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JournalPaper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar: Back button, Pink Highlight "Notes & idées" pill on left, Month/Year on right
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = JournalPaper,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Back button + "Notes & idées" soft yellow pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Back Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onNavigateBack),
                            contentAlignment = Alignment.Center
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Back,
                                contentDescription = "Retour",
                                tint = JournalInk,
                                size = 20.dp
                            )
                        }

                        // Yellow Highlighter Pill with yellow dot: "Notes & idées" / "ملاحظات وأفكار"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HighlighterYellow.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFEAB308), CircleShape)
                                )
                                val titleText = if (isRtl) "ملاحظات وأفكار" else "Notes & idées"
                                Text(
                                    text = titleText,
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    fontSize = if (isRtl) 15.sp else 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalWritingInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    }

                    // Right: Current / Selected Month (e.g. "Septembre 2026")
                    val headerMonthText = if (uiState.selectedMonthKey != null) {
                        uiState.availableMonths.firstOrNull { it.first == uiState.selectedMonthKey }?.second
                            ?: uiState.selectedMonthKey!!
                    } else {
                        currentMonthDisplay
                    }

                    Text(
                        text = headerMonthText,
                        fontFamily = PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalWritingInk.copy(alpha = 0.80f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Ruled Paper Content
            JournalRuledDocument(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                clearFocusOnTap = true
            ) {
                // Line 1: 1 rule spacer
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 2: Search Bar + Calendar Icon directly resting on the ruled blue line (29dp)
                NotebookSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = { query -> viewModel.updateSearchQuery(query) },
                    placeholder = if (isRtl) "بحث في الملاحظات..." else "Rechercher dans vos notes...",
                    onOpenCalendar = { showMonthPicker = true },
                    isDateFiltered = uiState.selectedMonthKey != null
                )

                // Line 3: 1 rule spacer (tna9ez star)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 4: Action Button: "Créer une nouvelle note" (1 rule = 29dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(JournalRuleSpacing)
                            .clip(RoundedCornerShape(6.dp))
                            .background(HighlighterYellow.copy(alpha = 0.35f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = if (isRtl) "إنشاء ملاحظة جديدة" else "Créer une nouvelle note",
                                onClick = {
                                    coroutineScope.launch {
                                        val newId = viewModel.createNewNote()
                                        onOpenNote(newId)
                                    }
                                }
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Plus,
                                contentDescription = null,
                                tint = JournalWritingInk,
                                size = 13.5.dp
                            )
                            val btnText = if (isRtl) "إنشاء ملاحظة جديدة" else "Créer une nouvelle note"
                            Text(
                                text = btnText,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 13.5.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                }

                // Line 5: 1 rule spacer (tna9ez star)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 6: "Notes du mois" badge with unified background "X"
                val notesDuMoisText = if (isRtl) "ملاحظات الشهر" else "Notes du mois"
                JournalSectionBadge(
                    title = notesDuMoisText,
                    badgeColor = HighlighterYellow.copy(alpha = 0.35f),
                    trailingContent = if (uiState.selectedMonthKey != null) {
                        {
                            val activeMonthDisplay = uiState.availableMonths
                                .firstOrNull { it.first == uiState.selectedMonthKey }?.second ?: uiState.selectedMonthKey!!
                            Row(
                                modifier = Modifier
                                    .height(JournalRuleSpacing)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(HighlighterYellow.copy(alpha = 0.50f))
                                    .clickable { viewModel.selectMonth(null) }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$activeMonthDisplay ✕",
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    } else null
                )

                // Line 5+: Content dial notes kif kayn howa daba
                if (uiState.monthGroups.isEmpty() && !uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing * 6)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Page,
                            contentDescription = null,
                            tint = JournalMutedInk.copy(alpha = 0.40f),
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "لا توجد أي ملاحظات حالياً" else "Aucune note trouvée",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRtl) "اضغط على الزر أسفله لكتابة أول ملاحظة ✍️" else "Appuyez sur le bouton ci-dessous pour créer une note ✍️",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 13.5.sp,
                            color = JournalMutedInk.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val allNotes = remember(uiState.monthGroups) {
                        uiState.monthGroups.flatMap { it.notes }
                    }

                    allNotes.forEachIndexed { noteIndex, note ->
                        val dotColor = getNoteDotColor(note.colorTag, noteIndex)
                        val shortDateStr = shortDateFormatter.format(Date(note.updatedAtEpochMs))
                        val isLastInGroup = noteIndex == allNotes.lastIndex

                        NoteRowItem(
                            note = note,
                            dotColor = dotColor,
                            shortDateStr = shortDateStr,
                            isLastInGroup = isLastInGroup,
                            isRtl = isRtl,
                            onOpenNote = { onOpenNote(note.id) },
                            onTogglePin = { viewModel.togglePin(note.id, !note.isPinned) },
                            onAssignToGroup = { noteToAssignToGroup = note },
                            onSetColor = { colorTag -> viewModel.setColorTag(note.id, colorTag) },
                            onDelete = { viewModel.deleteNote(note.id) }
                        )
                    }
                }

                // Bottom extra space above navigation bar
                Spacer(modifier = Modifier.height(JournalRuleSpacing * 3))
            }
        }

        // Month Picker Dialog
        if (showMonthPicker) {
            val cal = remember { Calendar.getInstance() }
            val initialYear = remember(uiState.selectedMonthKey) {
                uiState.selectedMonthKey?.split("-")?.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
            }
            val initialMonth = remember(uiState.selectedMonthKey) {
                uiState.selectedMonthKey?.split("-")?.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)
            }
            MonthPickerDialog(
                initialYear = initialYear,
                initialMonth = initialMonth,
                onDismiss = { showMonthPicker = false },
                onSelectMonth = { year, month ->
                    showMonthPicker = false
                    val key = String.format(Locale.US, "%04d-%02d", year, month)
                    viewModel.selectMonth(key)
                }
            )
        }

        // Assign to Group Dialog
        noteToAssignToGroup?.let { note ->
            AssignToGroupDialog(
                category = GroupCategory.NOTES,
                currentGroupId = note.groupId,
                calculationRepository = calculationRepository,
                onDismiss = { noteToAssignToGroup = null },
                onAssignGroup = { groupId ->
                    viewModel.assignNoteToGroup(note.id, groupId)
                    noteToAssignToGroup = null
                }
            )
        }
    }
}

/**
 * Single Note Item rendered across exactly 2 notebook lines (58dp):
 * Line 1: Guide line with color dot + Title + 📌 (if pinned) + Date + Menu (⋮)
 * Line 2: Guide line continuing down + Content preview
 * Directly on ruled paper with zero card container!
 */
@Composable
private fun NoteRowItem(
    note: NoteEntity,
    dotColor: Color,
    shortDateStr: String,
    isLastInGroup: Boolean,
    isRtl: Boolean,
    onOpenNote: () -> Unit,
    onTogglePin: () -> Unit,
    onAssignToGroup: () -> Unit,
    onSetColor: (String) -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val displayTitle = note.title.ifBlank { if (isRtl) "ملاحظة بدون عنوان" else "Note sans titre" }
    val previewContent = note.content.lines().firstOrNull { it.isNotBlank() } ?: ""

    val guideLineColor = HighlighterPink.copy(alpha = 0.55f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpenNote)
    ) {
        // Line 1 (29dp): Guide line + dot + Title + Pin + Dashed line + Date + Menu (⋮)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Dot + Title + Pin
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                // Little colored dot resting directly on the ruled line
                Canvas(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(y = 0.5.dp)
                ) {
                    drawCircle(color = dotColor)
                }

                Text(
                    text = displayTitle,
                    fontFamily = resolveJournalFont(displayTitle, isRtl),
                    fontSize = if (isArabicScript(displayTitle) || isRtl) 15.sp else 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalWritingInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
                )

                if (note.isPinned) {
                    Box(
                        modifier = Modifier
                            .height(JournalRuleSpacing)
                            .journalBaselineOnRule(opticalOffsetFromBottom = 0.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.PinFilled,
                            contentDescription = "Épinglé",
                            tint = JournalInk.copy(alpha = 0.85f),
                            size = 13.dp,
                            modifier = Modifier.offset(y = 0.5.dp)
                        )
                    }
                }
            }

            // Connecting dashed line directly on the blue notebook line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(JournalRuleSpacing)
                    .padding(start = 5.dp, end = 6.dp)
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

            // End: Short Date (e.g. "10 sept.") + 3-dots Menu Button (⋮)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = shortDateStr,
                    fontFamily = PatrickHandFamily,
                    fontSize = 13.sp,
                    color = JournalMutedInk.copy(alpha = 0.70f),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(role = Role.Button) { menuExpanded = true }
                        .journalBaselineOnRule(opticalOffsetFromBottom = 0.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                Text(
                    text = "⋮",
                    fontFamily = PatrickHandFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk.copy(alpha = 0.85f)
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(JournalPaper)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (note.isPinned) {
                                    if (isRtl) "إلغاء التثبيت" else "Désépingler"
                                } else {
                                    if (isRtl) "تثبيت الملاحظة" else "Épingler"
                                },
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                color = JournalWritingInk
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isRtl) "إضافة إلى مجموعة" else stringResource(R.string.action_add_to_group),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                color = JournalWritingInk
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onAssignToGroup()
                        }
                    )

                    // Color tag picker row (12 soft pastel colors in 2 rows)
                    DropdownMenuItem(
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val rows = NoteColorPalette.chunked(6)
                                rows.forEach { rowColors ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowColors.forEach { opt ->
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(opt.lightColor)
                                                    .border(0.8.dp, JournalInk.copy(alpha = 0.25f), CircleShape)
                                                    .clickable {
                                                        menuExpanded = false
                                                        onSetColor(opt.tag)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onClick = { }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isRtl) "حذف الملاحظة 🗑" else "Supprimer 🗑",
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                color = Color(0xFFE53935)
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

        // Line 2 (29dp): Content preview sitting strictly ON the rule line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(start = 27.dp, end = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = previewContent.ifBlank { if (isRtl) "لا يوجد محتوى..." else "Aucun contenu..." },
                fontFamily = resolveJournalFont(previewContent, isRtl),
                fontSize = if (isArabicScript(previewContent)) 11.5.sp else 12.sp,
                fontWeight = FontWeight.Normal,
                color = JournalMutedInk.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier
                    .fillMaxWidth()
                    .journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
            )
        }
    }
}
