package com.cash.guide.ui.notebook

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.cash.guide.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditDueDateDialog(
    initialDueDateEpochMs: Long? = null,
    initialReminderEnabled: Boolean = false,
    initialReminderTimeEpochMs: Long? = null,
    onSave: (dueDateEpochMs: Long?, reminderEnabled: Boolean, reminderTimeEpochMs: Long?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    var selectedDueDate by remember {
        mutableStateOf<Long?>(initialDueDateEpochMs ?: run {
            // Default to tomorrow 00:00
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        })
    }

    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    // Selected reminder hour: 9 (09:00), 14 (14:00), 18 (18:00)
    var selectedHour by remember {
        val defaultHour = if (initialReminderTimeEpochMs != null) {
            val c = Calendar.getInstance().apply { timeInMillis = initialReminderTimeEpochMs }
            c.get(Calendar.HOUR_OF_DAY)
        } else 9
        mutableIntStateOf(if (defaultHour in listOf(9, 14, 18)) defaultHour else 9)
    }

    var showCustomDatePicker by remember { mutableStateOf(false) }

    // Notification permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            reminderEnabled = true
        } else {
            reminderEnabled = false
        }
    }

    fun computeReminderEpochMs(): Long? {
        if (!reminderEnabled || selectedDueDate == null) return null
        val c = Calendar.getInstance().apply {
            timeInMillis = selectedDueDate!!
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides layoutDirection
        ) {
            // Dimmed Scrim Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                // Authentic Ruled Notebook Card - Spacious & Clean
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 390.dp)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = 1.2.dp,
                            color = JournalRule.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks */ },
                    color = JournalPaper,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                    ) {
                        // 1. Header: Title + Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val dialogTitle = stringResource(R.string.due_date_dialog_title)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFC2410C), CircleShape)
                                )
                                Text(
                                    text = dialogTitle,
                                    fontFamily = resolveJournalFont(dialogTitle, isRtl),
                                    fontSize = if (isRtl) 17.5.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }

                            // Sketch '✕' close button
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(JournalInk.copy(alpha = 0.05f))
                                    .clickable(role = Role.Button) { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    fontFamily = JournalHandFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalMutedInk
                                )
                            }
                        }

                        // 2. Subtitle: Short guidance
                        val subtitleText = stringResource(R.string.due_date_dialog_subtitle)
                        Text(
                            text = subtitleText,
                            fontFamily = resolveJournalFont(subtitleText, isRtl),
                            fontSize = 13.sp,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                        )

                        // 3. Spacious "Choisir une date" Card
                        val formattedSelectedDate = remember(selectedDueDate) {
                            if (selectedDueDate != null) {
                                val sdf = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.getDefault())
                                sdf.format(Date(selectedDueDate!!))
                            } else null
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFC2410C).copy(alpha = 0.07f))
                                .border(
                                    width = 1.2.dp,
                                    color = Color(0xFFC2410C).copy(alpha = 0.30f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(role = Role.Button) {
                                    showCustomDatePicker = true
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Calendar Icon badge
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFC2410C).copy(alpha = 0.14f))
                                            .border(
                                                width = 1.dp,
                                                color = Color(0xFFC2410C).copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "📅", fontSize = 20.sp)
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (formattedSelectedDate != null) {
                                            Text(
                                                text = formattedSelectedDate,
                                                fontFamily = resolveJournalFont(formattedSelectedDate, isRtl),
                                                fontSize = if (isRtl) 15.sp else 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC2410C),
                                                style = TextStyle(platformStyle = NoFontPadding)
                                            )
                                            Text(
                                                text = stringResource(R.string.due_date_select_date_hint),
                                                fontFamily = resolveJournalFont(stringResource(R.string.due_date_select_date_hint), isRtl),
                                                fontSize = 12.sp,
                                                color = JournalMutedInk,
                                                style = TextStyle(platformStyle = NoFontPadding)
                                            )
                                        } else {
                                            Text(
                                                text = stringResource(R.string.due_date_pick_date),
                                                fontFamily = resolveJournalFont(stringResource(R.string.due_date_pick_date), isRtl),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC2410C),
                                                style = TextStyle(platformStyle = NoFontPadding)
                                            )
                                            Text(
                                                text = stringResource(R.string.due_date_select_date_hint),
                                                fontFamily = resolveJournalFont(stringResource(R.string.due_date_select_date_hint), isRtl),
                                                fontSize = 12.sp,
                                                color = JournalMutedInk,
                                                style = TextStyle(platformStyle = NoFontPadding)
                                            )
                                        }
                                    }
                                }

                                // Subtle action badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFC2410C).copy(alpha = 0.12f))
                                        .border(
                                            width = 0.8.dp,
                                            color = Color(0xFFC2410C).copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (formattedSelectedDate != null) "✎" else "+",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2410C)
                                    )
                                }
                            }
                        }

                        // Divider between Date Card & Reminder Section
                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = JournalRule.copy(alpha = 0.45f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Notification Reminder Toggle Row
                        val toggleReminder: () -> Unit = {
                            if (!reminderEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPerm) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        reminderEnabled = true
                                    }
                                } else {
                                    reminderEnabled = true
                                }
                            } else {
                                reminderEnabled = false
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Switch
                                ) { toggleReminder() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val reminderLabel = stringResource(R.string.due_date_reminder_label)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (reminderEnabled) "🔔" else "🔕",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = reminderLabel,
                                    fontFamily = resolveJournalFont(reminderLabel, isRtl),
                                    fontSize = if (isRtl) 13.5.sp else 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JournalInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }

                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { toggleReminder() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFC2410C),
                                    uncheckedThumbColor = JournalMutedInk,
                                    uncheckedTrackColor = JournalRule.copy(alpha = 0.40f)
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }

                        // 5. Reminder Time Selection (visible when reminder is active)
                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val timeLabel = stringResource(R.string.due_date_time_label)
                            Text(
                                text = timeLabel,
                                fontFamily = resolveJournalFont(timeLabel, isRtl),
                                fontSize = 12.5.sp,
                                color = JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetDateChip(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.due_date_time_morning),
                                    isSelected = selectedHour == 9,
                                    onClick = { selectedHour = 9 }
                                )
                                PresetDateChip(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.due_date_time_afternoon),
                                    isSelected = selectedHour == 14,
                                    onClick = { selectedHour = 14 }
                                )
                                PresetDateChip(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.due_date_time_evening),
                                    isSelected = selectedHour == 18,
                                    onClick = { selectedHour = 18 }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 6. Bottom Actions: "Supprimer" (if set) & "Enregistrer"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (initialDueDateEpochMs != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(JournalActionDelete.copy(alpha = 0.10f))
                                        .border(1.dp, JournalActionDelete.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .clickable(role = Role.Button) {
                                            onClear()
                                            onDismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val clearText = stringResource(R.string.due_date_clear)
                                    Text(
                                        text = clearText,
                                        fontFamily = resolveJournalFont(clearText, isRtl),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JournalActionDelete,
                                        style = TextStyle(platformStyle = NoFontPadding)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFC2410C))
                                    .border(1.dp, Color(0xFF9A3412), RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) {
                                        val reminderEpoch = computeReminderEpochMs()
                                        onSave(selectedDueDate, reminderEnabled, reminderEpoch)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val saveText = stringResource(R.string.due_date_save)
                                Text(
                                    text = saveText,
                                    fontFamily = resolveJournalFont(saveText, isRtl),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog - Themed to match Journal Notebook
    if (showCustomDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis
                        if (picked != null) {
                            // DatePicker returns UTC millis; convert UTC components to local midnight
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = picked
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedDueDate = localCal.timeInMillis
                        }
                        showCustomDatePicker = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_ok),
                        color = Color(0xFFC2410C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
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
                    headlineContentColor = Color(0xFFC2410C),
                    weekdayContentColor = JournalMutedInk,
                    subheadContentColor = JournalInk,
                    navigationContentColor = Color(0xFFC2410C),
                    yearContentColor = JournalInk,
                    currentYearContentColor = Color(0xFFC2410C),
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = Color(0xFFC2410C),
                    dayContentColor = JournalInk,
                    disabledDayContentColor = JournalMutedInk.copy(alpha = 0.35f),
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = Color(0xFFC2410C),
                    todayContentColor = Color(0xFFC2410C),
                    todayDateBorderColor = Color(0xFFC2410C),
                    dividerColor = JournalRule.copy(alpha = 0.50f)
                )
            )
        }
    }
}

@Composable
private fun PresetDateChip(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFFC2410C).copy(alpha = 0.12f)
                else JournalInk.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFFC2410C) else JournalRule.copy(alpha = 0.70f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = resolveJournalFont(label, isRtl),
            fontSize = if (isRtl) 12.sp else 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFC2410C) else JournalInk,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(platformStyle = NoFontPadding)
        )
    }
}
