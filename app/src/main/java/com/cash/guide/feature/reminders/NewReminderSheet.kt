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
import androidx.compose.material3.ModalBottomSheetDefaults
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
        SimpleDateFormat(if (isRtl) "EEEE d MMMM yyyy" else "EEEE d MMMM yyyy", if (isRtl) Locale("ar") else Locale.FRENCH)
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
                    Text(
                        text = if (isEditing) {
                            if (isRtl) "تعديل التذكير" else "Modifier le rappel"
                        } else {
                            if (isRtl) "إنشاء تذكير جديد" else "Créer un nouveau rappel"
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

            // 1. Titre du rappel (Input)
            Text(
                text = if (isRtl) "عنوان التذكير" else "Titre du rappel",
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
                    .border(1.dp, JournalInk.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (title.isBlank()) {
                    Text(
                        text = if (isRtl) "مثال: أداء فاتورة، موعد طبيب، سلعة..." else "ex: Payer facture, Rendez-vous, Stock...",
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
                    modifier = Modifier
                        .fillMaxWidth()
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Date et Heure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date picker trigger button
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = if (isRtl) "التاريخ" else "Date",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
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
                    Text(
                        text = if (isRtl) "الوقت" else "Heure",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
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
            Text(
                text = if (isRtl) "التكرار" else "Fréquence / Répétition",
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = JournalMutedInk
            )
            Spacer(modifier = Modifier.height(6.dp))

            val recurrenceOptions = listOf(
                ReminderRecurrence.ONCE to (if (isRtl) "مرة واحدة" else "Une fois"),
                ReminderRecurrence.DAILY to (if (isRtl) "يومياً" else "Chaque jour"),
                ReminderRecurrence.WEEKLY to (if (isRtl) "أسبوعياً" else "Chaque semaine"),
                ReminderRecurrence.MONTHLY to (if (isRtl) "شهرياً" else "Chaque mois"),
                ReminderRecurrence.EVERY_3_MONTHS to (if (isRtl) "كل 3 أشهر" else "Chaque 3 mois"),
                ReminderRecurrence.EVERY_6_MONTHS to (if (isRtl) "كل 6 أشهر" else "Chaque 6 mois"),
                ReminderRecurrence.YEARLY to (if (isRtl) "سنوياً" else "Chaque année")
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
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
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
                Text(
                    text = if (isRtl) "اختر أيام التكرار" else "Jours de répétition",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
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
                Text(
                    text = if (isRtl) "لون العلامة" else "Couleur du marqueur",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
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
                    Text(
                        text = if (isEditing) {
                            if (isRtl) "حفظ التعديلات" else "Enregistrer les modifications"
                        } else {
                            if (isRtl) "حفظ التذكير وتفعيله" else "Enregistrer le rappel"
                        },
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
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
                            text = "OK",
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(
                            text = if (isRtl) "إلغاء" else "Annuler",
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

@Composable
private fun KeyboardTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    isRtl: Boolean,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hourStr by remember(initialHour) { mutableStateOf(String.format(Locale.US, "%02d", initialHour)) }
    var minuteStr by remember(initialMinute) { mutableStateOf(String.format(Locale.US, "%02d", initialMinute)) }
    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        hourFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = JournalPaper,
            border = BorderStroke(1.dp, JournalInk.copy(alpha = 0.15f)),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Clock,
                        contentDescription = null,
                        tint = JournalWritingInk,
                        size = 20.dp
                    )
                    Text(
                        text = if (isRtl) "تحديد الوقت" else "Choisir l'heure",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isRtl) "اكتب الساعة والدقيقة بالكيبورد" else "Entrez l'heure et les minutes au clavier",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 13.sp,
                    color = JournalMutedInk
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Time Inputs Row: [ HH ] : [ MM ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hours Box
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 66.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HighlighterBlue.copy(alpha = 0.22f))
                                .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { hourFocusRequester.requestFocus() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = hourStr,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(2)
                                    val num = digits.toIntOrNull()
                                    if (digits.isEmpty() || (num != null && num in 0..23)) {
                                        hourStr = digits
                                        if (digits.length == 2) {
                                            minuteFocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier.focusRequester(hourFocusRequester),
                                textStyle = TextStyle(
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalWritingInk,
                                    textAlign = TextAlign.Center,
                                    platformStyle = NoFontPadding
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { minuteFocusRequester.requestFocus() }
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(JournalInk)
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = if (isRtl) "الساعات (0-23)" else "Heures (00-23)",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk
                        )
                    }

                    // Colon separator
                    Text(
                        text = ":",
                        fontFamily = PatrickHandFamily,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .offset(y = (-10).dp)
                    )

                    // Minutes Box
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 66.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HighlighterYellow.copy(alpha = 0.28f))
                                .border(1.5.dp, Color(0xFFEAB308).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { minuteFocusRequester.requestFocus() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = minuteStr,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(2)
                                    val num = digits.toIntOrNull()
                                    if (digits.isEmpty() || (num != null && num in 0..59)) {
                                        minuteStr = digits
                                        if (digits.length == 2) {
                                            keyboardController?.hide()
                                        }
                                    }
                                },
                                modifier = Modifier.focusRequester(minuteFocusRequester),
                                textStyle = TextStyle(
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalWritingInk,
                                    textAlign = TextAlign.Center,
                                    platformStyle = NoFontPadding
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(JournalInk)
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = if (isRtl) "الدقائق (0-59)" else "Minutes (00-59)",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk
                        )
                    }
                }

                // Quick presets row
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf("08:00", "09:00", "12:00", "14:00", "18:00", "20:00", "21:30").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.65f))
                                .border(1.dp, JournalInk.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .clickable {
                                    val parts = preset.split(":")
                                    hourStr = parts[0]
                                    minuteStr = parts[1]
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset,
                                fontFamily = PatrickHandFamily,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Annuler / OK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = if (isRtl) "إلغاء" else "Annuler",
                            color = JournalMutedInk,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val h = hourStr.toIntOrNull()?.coerceIn(0, 23) ?: initialHour
                            val m = minuteStr.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            onConfirm(h, m)
                        }
                    ) {
                        Text(
                            text = "OK",
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
