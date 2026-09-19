package com.cash.guide.feature.reminders

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.data.db.ReminderEntity
import com.cash.guide.data.db.ReminderRecurrence
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
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalSectionBadge
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.MonthPickerDialog
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.NotebookSearchField
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.R
import com.cash.guide.ui.notebook.resolveJournalFont
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalHighlighter
import com.cash.guide.ui.notebook.journalVisualOnRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RemindersOverviewScreen(
    viewModel: RemindersViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    var showMonthPicker by remember { mutableStateOf(false) }

    val currentMonthDisplay = remember(isRtl) {
        val formatter = SimpleDateFormat("MMMM yyyy", if (isRtl) Locale.forLanguageTag("ar") else Locale.FRENCH)
        formatter.format(Date()).replaceFirstChar { it.uppercase() }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val dateFormatter = remember(isRtl) {
        SimpleDateFormat("d MMM", if (isRtl) Locale.forLanguageTag("ar") else Locale.FRENCH)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JournalPaper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
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
                    // Left: Back button + "Rappels & alertes" soft blue pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onNavigateBack),
                            contentAlignment = Alignment.Center
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Back,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = JournalInk,
                                size = 20.dp
                            )
                        }

                        // Blue Highlighter Pill with blue dot: "Rappels & alertes" / "التذكيرات والتنبيهات"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HighlighterBlue.copy(alpha = 0.40f))
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
                                        .background(Color(0xFF38BDF8), CircleShape)
                                )
                                val titleText = stringResource(R.string.title_reminders_alerts)
                                Text(
                                    text = titleText,
                                    fontFamily = resolveJournalFont(titleText, isRtl),
                                    fontSize = if (isRtl) 15.sp else 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalWritingInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    }

                    // Right: Month text
                    val headerMonthText = if (uiState.selectedMonthKey != null) {
                        uiState.availableMonths.firstOrNull { it.first == uiState.selectedMonthKey }?.second
                            ?: uiState.selectedMonthKey!!
                    } else {
                        currentMonthDisplay
                    }

                    Text(
                        text = headerMonthText,
                        fontFamily = resolveJournalFont(headerMonthText, isRtl),
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
                    placeholder = stringResource(R.string.reminders_search_placeholder),
                    onOpenCalendar = { showMonthPicker = true },
                    isDateFiltered = uiState.selectedMonthKey != null
                )

                // Line 3: 1 rule spacer (tna9ez star)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 4: Action Button: "Créer un nouveau rappel" (1 rule = 29dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val createReminderText = stringResource(R.string.reminders_create_new)
                    Box(
                        modifier = Modifier
                            .height(JournalRuleSpacing)
                            .clip(RoundedCornerShape(6.dp))
                            .background(HighlighterBlue.copy(alpha = 0.35f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = createReminderText,
                                onClick = { viewModel.openCreateDialog() }
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
                            Text(
                                text = createReminderText,
                                fontFamily = resolveJournalFont(createReminderText, isRtl),
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

                // Line 6: Section Badge: "Tous les rappels" (using unified seamless path!)
                JournalSectionBadge(
                    title = stringResource(R.string.reminders_section_all, uiState.filteredReminders.size),
                    badgeColor = HighlighterBlue.copy(alpha = 0.30f)
                )

                // Line 7+: Content of reminders
                if (uiState.filteredReminders.isEmpty() && !uiState.isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing * 6)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Clock,
                            contentDescription = null,
                            tint = JournalMutedInk.copy(alpha = 0.40f),
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val emptyTitle = if (uiState.searchQuery.isNotBlank()) {
                            stringResource(R.string.reminders_empty_search)
                        } else {
                            stringResource(R.string.reminders_empty_title)
                        }
                        Text(
                            text = emptyTitle,
                            fontFamily = resolveJournalFont(emptyTitle, isRtl),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val emptySubtitle = stringResource(R.string.reminders_empty_subtitle)
                        Text(
                            text = emptySubtitle,
                            fontFamily = resolveJournalFont(emptySubtitle, isRtl),
                            fontSize = 13.5.sp,
                            color = JournalMutedInk.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    uiState.filteredReminders.forEachIndexed { index, reminder ->
                        ReminderRowItem(
                            reminder = reminder,
                            isRtl = isRtl,
                            onEdit = { viewModel.openEditDialog(reminder) },
                            onToggleEnabled = { viewModel.toggleEnabled(reminder.id, !reminder.isEnabled) },
                            onDelete = { viewModel.deleteReminder(reminder.id) }
                        )
                    }
                }

                // Extra bottom spacing
                Spacer(modifier = Modifier.height(JournalRuleSpacing * 3))
            }
        }

        // New / Edit Reminder Bottom Sheet
        if (uiState.isCreateSheetOpen) {
            NewReminderSheet(
                initialReminder = uiState.editingReminder,
                onDismiss = { viewModel.closeCreateDialog() },
                onSave = { title, desc, targetMs, recType, repDays, hour, min, color ->
                    val editing = uiState.editingReminder
                    if (editing != null) {
                        viewModel.updateReminder(
                            id = editing.id,
                            title = title,
                            description = desc,
                            targetEpochMs = targetMs,
                            recurrenceType = recType,
                            repeatDays = repDays,
                            timeHour = hour,
                            timeMinute = min,
                            colorTag = color
                        )
                    } else {
                        viewModel.createReminder(
                            title = title,
                            description = desc,
                            targetEpochMs = targetMs,
                            recurrenceType = recType,
                            repeatDays = repDays,
                            timeHour = hour,
                            timeMinute = min,
                            colorTag = color
                        )
                    }
                }
            )
        }

        // Month Picker Dialog
        if (showMonthPicker) {
            MonthPickerDialog(
                onSelectMonth = { year, month ->
                    val monthKey = String.format(Locale.US, "%04d-%02d", year, month)
                    viewModel.selectMonth(monthKey)
                    showMonthPicker = false
                },
                onDismiss = { showMonthPicker = false }
            )
        }
    }
}

@Composable
private fun ReminderRowItem(
    reminder: ReminderEntity,
    isRtl: Boolean,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val dotColor = when (reminder.colorTag.uppercase()) {
        "INDIGO" -> Color(0xFF6366F1)
        "PURPLE" -> Color(0xFFA855F7)
        "PINK" -> Color(0xFFF43F5E)
        "CORAL" -> Color(0xFFFB7185)
        "ORANGE" -> Color(0xFFFB923C)
        "AMBER" -> Color(0xFFF59E0B)
        "GREEN" -> Color(0xFF10B981)
        "TEAL" -> Color(0xFF14B8A6)
        "SLATE" -> Color(0xFF64748B)
        else -> Color(0xFF38BDF8) // BLUE
    }

    val recurrenceLabel = when (reminder.recurrenceType) {
        ReminderRecurrence.DAILY.name -> stringResource(R.string.reminders_rec_daily)
        ReminderRecurrence.WEEKLY.name -> stringResource(R.string.reminders_rec_weekly)
        ReminderRecurrence.MONTHLY.name -> stringResource(R.string.reminders_rec_monthly)
        ReminderRecurrence.EVERY_3_MONTHS.name -> stringResource(R.string.reminders_rec_3months)
        ReminderRecurrence.EVERY_6_MONTHS.name -> stringResource(R.string.reminders_rec_6months)
        ReminderRecurrence.YEARLY.name -> stringResource(R.string.reminders_rec_yearly)
        else -> stringResource(R.string.reminders_rec_once)
    }

    val timeStr = String.format(Locale.US, "%02d:%02d", reminder.timeHour, reminder.timeMinute)
    val dateStr = SimpleDateFormat("d MMM", if (isRtl) Locale.forLanguageTag("ar") else Locale.FRENCH).format(Date(reminder.targetEpochMs))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .clickable { onEdit() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Dot + Title
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category Dot
            Canvas(
                modifier = Modifier
                    .journalVisualOnRule(gapAboveRule = 2.dp)
                    .size(7.5.dp)
            ) {
                drawCircle(color = if (reminder.isEnabled) dotColor else JournalMutedInk.copy(alpha = 0.4f))
            }

            // Title
            Text(
                text = reminder.title,
                fontFamily = resolveJournalFont(reminder.title, isRtl),
                fontSize = if (isRtl) 15.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (reminder.isEnabled) JournalWritingInk else JournalMutedInk.copy(alpha = 0.6f),
                style = TextStyle(platformStyle = NoFontPadding),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .journalBaselineOnRule()
            )
        }

        // Right: Recurrence Pill + Time + Actions Menu
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Recurrence & Time with sketch clock icon sitting directly on the ruled line
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Clock,
                    contentDescription = null,
                    tint = if (reminder.isEnabled) JournalMutedInk else JournalMutedInk.copy(alpha = 0.4f),
                    size = 12.dp,
                    modifier = Modifier.journalVisualOnRule(gapAboveRule = 2.dp)
                )
                Text(
                    text = "$recurrenceLabel · $timeStr",
                    fontFamily = resolveJournalFont(recurrenceLabel, isRtl),
                    fontSize = if (isRtl) 13.sp else 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (reminder.isEnabled) JournalWritingInk.copy(alpha = 0.85f) else JournalMutedInk.copy(alpha = 0.5f),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Menu icon (3 dots)
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = JournalRuleSpacing)
                    .clip(CircleShape)
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.More,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = JournalMutedInk,
                    size = 15.dp
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            val editTxt = stringResource(R.string.reminders_edit_title)
                            Text(
                                text = editTxt,
                                fontFamily = resolveJournalFont(editTxt, isRtl),
                                color = JournalWritingInk
                            )
                        },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            val toggleTxt = if (reminder.isEnabled) stringResource(R.string.reminders_action_pause)
                            else stringResource(R.string.reminders_action_activate)
                            Text(
                                text = toggleTxt,
                                fontFamily = resolveJournalFont(toggleTxt, isRtl)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleEnabled()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            val delTxt = stringResource(R.string.action_delete)
                            Text(
                                text = delTxt,
                                fontFamily = resolveJournalFont(delTxt, isRtl),
                                color = Color.Red
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
