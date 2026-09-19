package com.cash.guide.feature.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.R
import com.cash.guide.ui.notebook.resolveJournalFont
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val ReminderColors = listOf(
    "BLUE" to Color(0xFF38BDF8),
    "INDIGO" to Color(0xFF6366F1),
    "PURPLE" to Color(0xFFA855F7),
    "PINK" to Color(0xFFF43F5E),
    "CORAL" to Color(0xFFFB7185),
    "ORANGE" to Color(0xFFFB923C),
    "AMBER" to Color(0xFFF59E0B),
    "GREEN" to Color(0xFF10B981),
    "TEAL" to Color(0xFF14B8A6),
    "SLATE" to Color(0xFF64748B)
)

// Days of week mapping (Calendar.MONDAY = 2, ..., Calendar.SUNDAY = 1)
private val WeekDaysList = listOf(
    Pair(Calendar.MONDAY, "Lun" to "الإثنين"),
    Pair(Calendar.TUESDAY, "Mar" to "الثلاثاء"),
    Pair(Calendar.WEDNESDAY, "Mer" to "الأربعاء"),
    Pair(Calendar.THURSDAY, "Jeu" to "الخميس"),
    Pair(Calendar.FRIDAY, "Ven" to "الجمعة"),
    Pair(Calendar.SATURDAY, "Sam" to "السبت"),
    Pair(Calendar.SUNDAY, "Dim" to "الأحد")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewReminderSheet(
    initialReminder: ReminderEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        targetEpochMs: Long,
        recurrenceType: String,
        repeatDays: String,
        timeHour: Int,
        timeMinute: Int,
        colorTag: String
    ) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isEditing = initialReminder != null

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var isTitleFocused by remember { mutableStateOf(false) }
    var title by remember(initialReminder) { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember(initialReminder) { mutableStateOf(initialReminder?.description ?: "") }
    var selectedRecurrence by remember(initialReminder) {
        mutableStateOf(
            initialReminder?.recurrenceType?.let {
                try { ReminderRecurrence.valueOf(it) } catch (e: Exception) { ReminderRecurrence.ONCE }
            } ?: ReminderRecurrence.ONCE
        )
    }
    var selectedDays by remember(initialReminder) {
        mutableStateOf(
            if (!initialReminder?.repeatDays.isNullOrBlank()) {
                initialReminder!!.repeatDays.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
            } else {
                setOf(Calendar.MONDAY, Calendar.FRIDAY)
            }
        )
    }
    var selectedColor by remember(initialReminder) { mutableStateOf(initialReminder?.colorTag ?: "BLUE") }

    val initCal = remember(initialReminder) {
        Calendar.getInstance().apply {
            if (initialReminder != null) {
                timeInMillis = initialReminder.targetEpochMs
            }
        }
    }
    var targetYear by remember(initialReminder) { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var targetMonth by remember(initialReminder) { mutableIntStateOf(initCal.get(Calendar.MONTH)) }
    var targetDay by remember(initialReminder) { mutableIntStateOf(initCal.get(Calendar.DAY_OF_MONTH)) }
    var targetHour by remember(initialReminder) { mutableIntStateOf(initialReminder?.timeHour ?: 9) }
    var targetMinute by remember(initialReminder) { mutableIntStateOf(initialReminder?.timeMinute ?: 0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }

    val dateFormatter = remember(isRtl) {
        SimpleDateFormat(if (isRtl) "EEEE d MMMM yyyy" else "EEEE d MMMM yyyy", if (isRtl) Locale.forLanguageTag("ar") else Locale.FRENCH)
    }

    val displayDate = remember(targetYear, targetMonth, targetDay) {
        val cal = Calendar.getInstance().apply {
            set(targetYear, targetMonth, targetDay)
        }
        dateFormatter.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    val displayTime = remember(targetHour, targetMinute) {
        String.format(Locale.US, "%02d:%02d", targetHour, targetMinute)
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
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Title + Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dotColor = ReminderColors.firstOrNull { it.first == selectedColor }?.second ?: Color(0xFF38BDF8)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )
                    val sheetTitle = if (isEditing) stringResource(R.string.reminders_edit_title) else stringResource(R.string.reminders_create_new)
                    Text(
                        text = sheetTitle,
                        fontFamily = resolveJournalFont(sheetTitle, isRtl),
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
                        contentDescription = stringResource(R.string.action_close),
                        tint = JournalMutedInk,
                        size = 14.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Titre du rappel (Input)
            val titleLabel = stringResource(R.string.reminders_title_label)
            Text(
                text = titleLabel,
                fontFamily = resolveJournalFont(titleLabel, isRtl),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .border(1.dp, JournalInk.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (title.isBlank()) {
                    val titleHint = stringResource(R.string.reminders_title_hint)
                    Text(
                        text = titleHint,
                        fontFamily = resolveJournalFont(titleHint, isRtl),
                        fontSize = 15.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .onFocusChanged { isTitleFocused = it.isFocused },
                    textStyle = TextStyle(
                        fontFamily = resolveJournalFont(title, isRtl),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        platformStyle = NoFontPadding
                    ),
                    cursorBrush = SolidColor(JournalInk),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Date et Heure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date picker trigger button
                Column(modifier = Modifier.weight(1.3f)) {
                    val dateLabel = stringResource(R.string.reminders_date_label)
                    Text(
                        text = dateLabel,
                        fontFamily = resolveJournalFont(dateLabel, isRtl),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalMutedInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HighlighterBlue.copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                keyboardController?.hide()
                                showDatePicker = true
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Calendar,
                                contentDescription = null,
                                tint = JournalWritingInk,
                                size = 15.dp
                            )
                            Text(
                                text = displayDate,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Time picker trigger button
                Column(modifier = Modifier.weight(0.9f)) {
                    val timeLabel = stringResource(R.string.reminders_time_label)
                    Text(
                        text = timeLabel,
                        fontFamily = resolveJournalFont(timeLabel, isRtl),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalMutedInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HighlighterYellow.copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFFEAB308).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                keyboardController?.hide()
                                showTimePicker = true
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Clock,
                                contentDescription = null,
                                tint = JournalWritingInk,
                                size = 15.dp
                            )
                            Text(
                                text = displayTime,
                                fontFamily = PatrickHandFamily,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Fréquence / Récurrence
            val freqLabel = stringResource(R.string.reminders_freq_label)
            Text(
                text = freqLabel,
                fontFamily = resolveJournalFont(freqLabel, isRtl),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk
            )
            Spacer(modifier = Modifier.height(6.dp))

            val recurrenceOptions = listOf(
                ReminderRecurrence.ONCE to stringResource(R.string.reminders_rec_once),
                ReminderRecurrence.DAILY to stringResource(R.string.reminders_rec_daily),
                ReminderRecurrence.WEEKLY to stringResource(R.string.reminders_rec_weekly),
                ReminderRecurrence.MONTHLY to stringResource(R.string.reminders_rec_monthly),
                ReminderRecurrence.EVERY_3_MONTHS to stringResource(R.string.reminders_rec_3months),
                ReminderRecurrence.EVERY_6_MONTHS to stringResource(R.string.reminders_rec_6months),
                ReminderRecurrence.YEARLY to stringResource(R.string.reminders_rec_yearly)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recurrenceOptions.forEach { (type, label) ->
                    val isSelected = selectedRecurrence == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) HighlighterPink.copy(alpha = 0.40f)
                                else Color.White.copy(alpha = 0.60f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFE27B97) else JournalInk.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedRecurrence = type }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = resolveJournalFont(label, isRtl),
                            fontSize = if (isRtl) 12.5.sp else 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) JournalWritingInk else JournalMutedInk
                        )
                    }
                }
            }

            // If Weekly selected: Show day chips (L, M, M, J, V, S, D)
            if (selectedRecurrence == ReminderRecurrence.WEEKLY) {
                Spacer(modifier = Modifier.height(10.dp))
                val repeatDaysLabel = stringResource(R.string.reminders_repeat_days_label)
                Text(
                    text = repeatDaysLabel,
                    fontFamily = resolveJournalFont(repeatDaysLabel, isRtl),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeekDaysList.forEach { (calDay, labels) ->
                        val isDaySelected = selectedDays.contains(calDay)
                        val dayText = if (isRtl) labels.second.take(2) else labels.first
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDaySelected) HighlighterGreen.copy(alpha = 0.50f)
                                    else Color.White.copy(alpha = 0.60f)
                                )
                                .border(
                                    width = if (isDaySelected) 1.5.dp else 1.dp,
                                    color = if (isDaySelected) Color(0xFF10B981) else JournalInk.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedDays = if (isDaySelected) {
                                        if (selectedDays.size > 1) selectedDays - calDay else selectedDays
                                    } else {
                                        selectedDays + calDay
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayText,
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 12.5.sp,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDaySelected) JournalWritingInk else JournalMutedInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Marqueur de couleur (10 curated colors in horizontal scroll)
            Column(modifier = Modifier.fillMaxWidth()) {
                val colorLabel = stringResource(R.string.reminders_color_label)
                Text(
                    text = colorLabel,
                    fontFamily = resolveJournalFont(colorLabel, isRtl),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReminderColors.forEach { (tag, color) ->
                        val isPicked = selectedColor == tag
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isPicked) 2.5.dp else 0.dp,
                                    color = if (isPicked) JournalWritingInk else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = tag }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Bouton Enregistrer (Highlighter Yellow / Gold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (title.isNotBlank()) HighlighterYellow.copy(alpha = 0.85f) else Color.LightGray.copy(alpha = 0.35f))
                    .clickable(
                        enabled = title.isNotBlank(),
                        role = Role.Button,
                        onClick = {
                            val targetCal = Calendar.getInstance().apply {
                                set(targetYear, targetMonth, targetDay, targetHour, targetMinute, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val repeatDaysStr = if (selectedRecurrence == ReminderRecurrence.WEEKLY) {
                                selectedDays.joinToString(",")
                            } else ""

                            onSave(
                                title.trim(),
                                description.trim(),
                                targetCal.timeInMillis,
                                selectedRecurrence.name,
                                repeatDaysStr,
                                targetHour,
                                targetMinute,
                                selectedColor
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Check,
                        contentDescription = null,
                        tint = if (title.isNotBlank()) JournalWritingInk else JournalMutedInk,
                        size = 16.dp
                    )
                    val saveBtnText = if (isEditing) stringResource(R.string.reminders_save_changes_btn) else stringResource(R.string.reminders_save_btn)
                    Text(
                        text = saveBtnText,
                        fontFamily = resolveJournalFont(saveBtnText, isRtl),
                        fontSize = if (isRtl) 15.sp else 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (title.isNotBlank()) JournalWritingInk else JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Material 3 Pure Compose DatePickerDialog (Avoids BadTokenException)
        if (showDatePicker) {
            val initialEpoch = remember(targetYear, targetMonth, targetDay) {
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, targetMonth)
                    set(Calendar.DAY_OF_MONTH, targetDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                utcCal.timeInMillis
            }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialEpoch
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val picked = datePickerState.selectedDateMillis
                            if (picked != null) {
                                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = picked
                                }
                                targetYear = utcCal.get(Calendar.YEAR)
                                targetMonth = utcCal.get(Calendar.MONTH)
                                targetDay = utcCal.get(Calendar.DAY_OF_MONTH)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_ok),
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = JournalMutedInk,
                            fontSize = 15.sp
                        )
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = JournalPaper
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = JournalPaper,
                        titleContentColor = JournalMutedInk,
                        headlineContentColor = Color(0xFF0284C7),
                        weekdayContentColor = JournalMutedInk,
                        subheadContentColor = JournalInk,
                        navigationContentColor = Color(0xFF0284C7),
                        yearContentColor = JournalInk,
                        currentYearContentColor = Color(0xFF0284C7),
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color(0xFF0284C7),
                        dayContentColor = JournalInk,
                        disabledDayContentColor = JournalMutedInk.copy(alpha = 0.35f),
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color(0xFF0284C7),
                        todayContentColor = Color(0xFF0284C7),
                        todayDateBorderColor = Color(0xFF0284C7),
                        dividerColor = JournalInk.copy(alpha = 0.15f)
                    )
                )
            }
        }

        // Pure Compose Keyboard TimePicker Dialog (Simple, numeric keyboard input, avoids dial confusion)
        if (showTimePicker) {
            KeyboardTimePickerDialog(
                initialHour = targetHour,
                initialMinute = targetMinute,
                isRtl = isRtl,
                onConfirm = { h, m ->
                    targetHour = h
                    targetMinute = m
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    isRtl: Boolean,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                val okText = stringResource(R.string.action_ok)
                Text(
                    text = okText,
                    color = JournalWritingInk,
                    fontFamily = resolveJournalFont(okText, isRtl),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                val cancelText = stringResource(R.string.action_cancel)
                Text(
                    text = cancelText,
                    color = JournalMutedInk,
                    fontFamily = resolveJournalFont(cancelText, isRtl),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            val titleText = stringResource(R.string.reminders_time_dialog_title)
            Text(
                text = titleText,
                fontFamily = resolveJournalFont(titleText, isRtl),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = JournalWritingInk
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = HighlighterBlue.copy(alpha = 0.22f),
                        selectorColor = Color(0xFF38BDF8),
                        containerColor = JournalPaper,
                        periodSelectorBorderColor = Color(0xFF38BDF8),
                        periodSelectorSelectedContainerColor = HighlighterBlue.copy(alpha = 0.22f),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = JournalWritingInk,
                        periodSelectorUnselectedContentColor = JournalMutedInk,
                        timeSelectorSelectedContainerColor = HighlighterBlue.copy(alpha = 0.22f),
                        timeSelectorUnselectedContainerColor = Color.Transparent,
                        timeSelectorSelectedContentColor = JournalWritingInk,
                        timeSelectorUnselectedContentColor = JournalMutedInk
                    )
                )
            }
        },
        containerColor = JournalPaper,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp
    )
}
