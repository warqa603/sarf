package com.cash.guide.feature.note

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.cash.guide.R
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.domain.NoteShareHelper
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalActionDelete
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.data.CalculationRepository
import com.cash.guide.domain.GroupCategory
import com.cash.guide.feature.groups.AssignToGroupDialog
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalTextKeyboardDock
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.resolveJournalFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    calculationRepository: CalculationRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAssignGroupDialog by remember { mutableStateOf(false) }
    val titleScrollState = rememberScrollState()

    LaunchedEffect(titleScrollState.maxValue, uiState.title.text, uiState.activeInputTarget) {
        if (uiState.activeInputTarget == NoteInputTarget.TITLE) {
            titleScrollState.scrollTo(titleScrollState.maxValue)
        }
    }

    // Auto-save on back navigation
    BackHandler {
        if (uiState.activeInputTarget != NoteInputTarget.NONE) {
            viewModel.closeKeyboard()
        } else {
            viewModel.saveChanges()
            onNavigateBack()
        }
    }

    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy • HH:mm", Locale.getDefault()) }
    val formattedDate = remember(uiState.createdAtEpochMs) {
        val raw = dateFormatter.format(Date(uiState.createdAtEpochMs))
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    // Cursor blink animation (Pink highlighter cursor matching Image 3)
    val infiniteTransition = rememberInfiniteTransition(label = "note_cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor_blink"
    )

    val cursorColor = Color(0xFFE91E63) // Pinkish cursor line like Image 3

    // Suppress system soft keyboard completely so in-app Journal keyboard is used
    CompositionLocalProvider(
        @Suppress("DEPRECATION")
        LocalTextInputService provides null
    ) {
        androidx.compose.ui.platform.InterceptPlatformTextInput(
            interceptor = { _, _ -> kotlinx.coroutines.awaitCancellation() }
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(JournalPaper)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Top Bar: Back arrow on left, Title in highlighter pill in middle, Pin icon on right
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = JournalPaper,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back Button (40dp touch target)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) {
                                        viewModel.closeKeyboard()
                                        viewModel.saveChanges()
                                        onNavigateBack()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                HisabiSketchIcon(
                                    symbol = HisabiSymbol.Back,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = JournalInk,
                                    size = 20.dp
                                )
                            }

                            // Title in soft highlighter pill (matching note color, clickable to focus title)
                            val isTitleActive = uiState.activeInputTarget == NoteInputTarget.TITLE
                            val displayTitle = uiState.title.text

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(getNoteHighlightPillColor(uiState.colorTag))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        keyboardController?.hide()
                                        viewModel.focusTitle()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val containerWidth = maxWidth
                                    val isArabicKeyboard = uiState.keyboardLanguage == com.cash.guide.domain.JournalKeyboardLanguage.ARABIC
                                    CompositionLocalProvider(
                                        LocalLayoutDirection provides if (isArabicKeyboard) LayoutDirection.Rtl else LayoutDirection.Ltr
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = containerWidth)
                                                .horizontalScroll(titleScrollState),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (displayTitle.isEmpty()) {
                                                    Text(
                                                        text = if (isRtl) "عنوان الملاحظة... ✍️" else "Titre de la note...",
                                                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                                        fontSize = if (isRtl) 16.sp else 16.5.sp,
                                                        color = JournalMutedInk.copy(alpha = 0.5f),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        style = TextStyle(platformStyle = NoFontPadding)
                                                    )
                                                } else {
                                                    Text(
                                                        text = displayTitle,
                                                        fontFamily = resolveJournalFont(displayTitle, isRtl),
                                                        fontSize = if (isRtl) 16.5.sp else 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = JournalWritingInk,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        style = TextStyle(platformStyle = NoFontPadding)
                                                    )
                                                }

                                                if (isTitleActive && cursorAlpha > 0f) {
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.dp)
                                                            .height(18.dp)
                                                            .background(cursorColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Pin Button next to Title (toggles pin/unpin directly)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) {
                                        viewModel.togglePin()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isPinned) {
                                    HisabiSketchIcon(
                                        symbol = HisabiSymbol.PinFilled,
                                        contentDescription = "Épinglé",
                                        tint = JournalWritingInk,
                                        size = 24.dp
                                    )
                                } else {
                                    HisabiSketchIcon(
                                        symbol = HisabiSymbol.Pin,
                                        contentDescription = "Épingler",
                                        tint = JournalInk.copy(alpha = 0.65f),
                                        size = 24.dp
                                    )
                                }
                            }
                        }
                    }

                    // Ruled Document Content (Directly on Notebook Paper, NO Cards, NO Shadows)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        JournalRuledDocument(
                            modifier = Modifier.fillMaxSize(),
                            clearFocusOnTap = false
                        ) {
                            val noteLightColor = getNoteHighlightPillColor(uiState.colorTag)

                            // Rule 1 (29dp): Metadata & Actions Row sitting directly on the blue line
                            // Far Left: Date & Time resting on the blue line
                            // Right: Color circle with palette, Share, Trash (resting on the blue line)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(JournalRuleSpacing)
                                    .padding(start = 18.dp, end = 8.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Far Left: Date & Time resting on the blue line
                                Text(
                                    text = formattedDate,
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 13.5.sp,
                                    color = JournalMutedInk.copy(alpha = 0.75f),
                                    style = TextStyle(platformStyle = NoFontPadding),
                                    modifier = Modifier.journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
                                )

                                // Right: Color circle with palette, Share icon, Trash icon (sitting on the blue line)
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Color Picker Circle Button with Dropdown Palette
                                    var showColorPalette by remember { mutableStateOf(false) }

                                    Box(
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(JournalRuleSpacing)
                                                .width(30.dp)
                                                .clip(CircleShape)
                                                .clickable(role = Role.Button) {
                                                    showColorPalette = true
                                                },
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(y = 0.5.dp)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(noteLightColor)
                                                    .border(1.2.dp, JournalInk.copy(alpha = 0.45f), CircleShape)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showColorPalette,
                                            onDismissRequest = { showColorPalette = false },
                                            modifier = Modifier
                                                .background(JournalPaper)
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val rows = NoteColorPalette.chunked(6)
                                                rows.forEach { rowOptions ->
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        rowOptions.forEach { option ->
                                                            val isSelected = uiState.colorTag.equals(option.tag, ignoreCase = true) ||
                                                                    (option.tag == "PEACH" && uiState.colorTag.equals("ORANGE", ignoreCase = true)) ||
                                                                    (option.tag == "SKY" && uiState.colorTag.equals("BLUE", ignoreCase = true))
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .clickable {
                                                                        showColorPalette = false
                                                                        viewModel.setColorTag(option.tag)
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(if (isSelected) 22.dp else 18.dp)
                                                                        .clip(CircleShape)
                                                                        .background(option.lightColor)
                                                                        .then(
                                                                            if (isSelected) {
                                                                                Modifier.border(2.dp, JournalInk, CircleShape)
                                                                            } else {
                                                                                Modifier.border(1.dp, JournalInk.copy(alpha = 0.25f), CircleShape)
                                                                            }
                                                                        )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Group / Folder Button
                                    Box(
                                        modifier = Modifier
                                            .height(JournalRuleSpacing)
                                            .width(32.dp)
                                            .clip(CircleShape)
                                            .clickable(role = Role.Button) {
                                                viewModel.closeKeyboard()
                                                showAssignGroupDialog = true
                                            },
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        HisabiSketchIcon(
                                            symbol = HisabiSymbol.Folder,
                                            contentDescription = "Groupe",
                                            tint = if (uiState.groupId != null) JournalInk else JournalMutedInk.copy(alpha = 0.65f),
                                            size = 18.dp,
                                            modifier = Modifier.offset(y = 2.8.dp)
                                        )
                                    }

                                    // Share Button
                                    Box(
                                        modifier = Modifier
                                            .height(JournalRuleSpacing)
                                            .width(32.dp)
                                            .clip(CircleShape)
                                            .clickable(role = Role.Button) {
                                                viewModel.closeKeyboard()
                                                viewModel.saveChanges()
                                                NoteShareHelper.shareAsImage(
                                                    context = context,
                                                    note = viewModel.getNoteEntity(),
                                                    isRtl = isRtl,
                                                    coroutineScope = coroutineScope
                                                )
                                            },
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        HisabiSketchIcon(
                                            symbol = HisabiSymbol.Share,
                                            contentDescription = "Partager",
                                            tint = JournalInk,
                                            size = 18.dp,
                                            modifier = Modifier.offset(y = 2.8.dp)
                                        )
                                    }

                                    // Trash / Poubelle Button (Black sketch ink, triggers delete confirmation)
                                    Box(
                                        modifier = Modifier
                                            .height(JournalRuleSpacing)
                                            .width(32.dp)
                                            .clip(CircleShape)
                                            .clickable(role = Role.Button) {
                                                showDeleteDialog = true
                                            },
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        HisabiSketchIcon(
                                            symbol = HisabiSymbol.Trash,
                                            contentDescription = "Supprimer",
                                            tint = JournalInk,
                                            size = 18.dp,
                                            modifier = Modifier.offset(y = 2.5.dp)
                                        )
                                    }
                                }
                            }

                            // Rule 2 (29dp): Empty line skipped ("وغادي تنقص السطر عاد غادي عاد الكونتنت غادي يبدا يتكتب")
                            Spacer(modifier = Modifier.height(JournalRuleSpacing))

                            // Rule 3 onwards: Multiline Content directly sitting on the blue lines
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        keyboardController?.hide()
                                        viewModel.focusContent()
                                    }
                            ) {
                                val contentText = uiState.content.text
                                val isContentActive = uiState.activeInputTarget == NoteInputTarget.CONTENT
                                val textMeasurer = rememberTextMeasurer()
                                val availableWidthPx = constraints.maxWidth

                                val isArabicKeyboard = uiState.keyboardLanguage == com.cash.guide.domain.JournalKeyboardLanguage.ARABIC
                                val isContentRtl = isRtl || (contentText.isNotEmpty() && isArabicScript(contentText)) || (contentText.isEmpty() && isArabicKeyboard)

                                val contentStyle = TextStyle(
                                    fontFamily = resolveJournalFont(contentText, isContentRtl),
                                    fontSize = if (isContentRtl) 16.sp else 16.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = JournalInk,
                                    platformStyle = NoFontPadding
                                )

                                CompositionLocalProvider(
                                    LocalLayoutDirection provides if (isContentRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (contentText.isEmpty()) {
                                            // Empty state: "Écrire une note..." sitting on Rule 3
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(JournalRuleSpacing)
                                            ) {
                                                if (isContentActive && cursorAlpha > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomStart)
                                                            .padding(bottom = 0.5.dp)
                                                            .width(2.dp)
                                                            .height(19.dp)
                                                            .background(cursorColor)
                                                    )
                                                }
                                                Text(
                                                    text = if (isContentRtl) "اكتب ملاحظة..." else "Écrire une note...",
                                                    fontFamily = if (isContentRtl) TajawalFamily else PatrickHandFamily,
                                                    fontSize = if (isContentRtl) 15.sp else 16.sp,
                                                    color = JournalMutedInk.copy(alpha = 0.45f),
                                                    style = TextStyle(platformStyle = NoFontPadding),
                                                    modifier = Modifier
                                                        .then(if (isContentActive) Modifier.padding(start = 6.dp) else Modifier)
                                                        .journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
                                                )
                                            }
                                        } else {
                                            // Multiline text broken into paragraphs and wrapped to exact 29dp rules
                                            val paragraphs = remember(contentText) { contentText.split('\n') }
                                            val paragraphStarts = remember(paragraphs) {
                                                val starts = mutableListOf<Int>()
                                                var cur = 0
                                                paragraphs.forEach { p ->
                                                    starts.add(cur)
                                                    cur += p.length + 1
                                                }
                                                starts
                                            }
                                            val cursorPosition = uiState.content.selection.end

                                            paragraphs.forEachIndexed { pIdx, paragraph ->
                                                val currentGlobalCharIndex = paragraphStarts[pIdx]

                                                if (paragraph.isEmpty()) {
                                                    // Empty paragraph takes exactly 1 rule (29dp)
                                                    val isCursorHere = isContentActive && (cursorPosition == currentGlobalCharIndex)
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(JournalRuleSpacing)
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) {
                                                                viewModel.focusContent(currentGlobalCharIndex)
                                                            }
                                                    ) {
                                                        if (isCursorHere && cursorAlpha > 0f) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.BottomStart)
                                                                    .padding(bottom = 0.5.dp)
                                                                    .width(2.dp)
                                                                    .height(19.dp)
                                                                    .background(cursorColor)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    val layoutResult = textMeasurer.measure(
                                                        text = AnnotatedString(paragraph),
                                                        style = contentStyle,
                                                        constraints = Constraints(maxWidth = availableWidthPx)
                                                    )
                                                    val lineCount = layoutResult.lineCount

                                                    for (lineIndex in 0 until lineCount) {
                                                        val lineStartInP = layoutResult.getLineStart(lineIndex)
                                                        val lineEndInP = layoutResult.getLineEnd(lineIndex)
                                                        val lineText = paragraph.substring(lineStartInP, lineEndInP)

                                                        val globalLineStart = currentGlobalCharIndex + lineStartInP
                                                        val globalLineEnd = currentGlobalCharIndex + lineEndInP

                                                        val isCursorInThisLine = isContentActive &&
                                                                (cursorPosition >= globalLineStart &&
                                                                        (cursorPosition < globalLineEnd || (cursorPosition == globalLineEnd && lineIndex == lineCount - 1)))

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(JournalRuleSpacing)
                                                                .clickable(
                                                                    interactionSource = remember { MutableInteractionSource() },
                                                                    indication = null
                                                                ) {
                                                                    viewModel.focusContent(globalLineStart + lineText.length)
                                                                }
                                                        ) {
                                                            Text(
                                                                text = lineText,
                                                                style = contentStyle,
                                                                modifier = Modifier.journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
                                                            )

                                                            if (isCursorInThisLine && cursorAlpha > 0f) {
                                                                val offsetInLine = (cursorPosition - globalLineStart).coerceIn(0, lineText.length)
                                                                val subLayout = textMeasurer.measure(
                                                                    text = AnnotatedString(lineText.take(offsetInLine)),
                                                                    style = contentStyle
                                                                )
                                                                val cursorX = if (isContentRtl) {
                                                                    -subLayout.size.width.toFloat()
                                                                } else {
                                                                    subLayout.size.width.toFloat()
                                                                }

                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomStart)
                                                                        .offset(x = with(LocalDensity.current) { cursorX.toDp() })
                                                                        .padding(bottom = 0.5.dp)
                                                                        .width(2.dp)
                                                                        .height(19.dp)
                                                                        .background(cursorColor)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 4 blank rules at bottom for breathing room
                                        Spacer(modifier = Modifier.height(JournalRuleSpacing * 4))
                                    }
                                }
                            }
                        }
                    }

                    // In-app Journal Keyboard Dock
                    if (uiState.activeInputTarget != NoteInputTarget.NONE) {
                        JournalTextKeyboardDock(
                            language = uiState.keyboardLanguage,
                            shiftMode = uiState.shiftMode,
                            expanded = uiState.keyboardExpanded,
                            onToggleExpand = { viewModel.toggleKeyboardExpanded() },
                            onCycleLanguage = { viewModel.cycleLanguage() },
                            onSelectLanguage = { viewModel.selectLanguage(it) },
                            onToggleShift = { viewModel.toggleShift() },
                            onInsertText = { viewModel.applyTextKey(it) },
                            onBackspace = { viewModel.applyTextBackspace() },
                            onSwitchToNumericMode = { },
                            onConfirm = { viewModel.confirmInput() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(JournalPaper)
                                .navigationBarsPadding()
                        )
                    }
                }

                // Delete Confirmation Dialog
                if (showDeleteDialog) {
                    val noteTitle = uiState.title.text.ifBlank { if (isRtl) "هذه الملاحظة" else "cette note" }
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.note_delete_title),
                                fontFamily = resolveJournalFont(noteTitle, isRtl),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.note_delete_confirm),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 15.sp,
                                color = JournalInk
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    viewModel.deleteNote {
                                        onNavigateBack()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isRtl) "حذف" else "Supprimer",
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalActionDelete
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(
                                    text = if (isRtl) "إلغاء" else "Annuler",
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    color = JournalMutedInk
                                )
                            }
                        },
                        containerColor = JournalPaper
                    )
                }

                if (showAssignGroupDialog) {
                    AssignToGroupDialog(
                        category = GroupCategory.NOTES,
                        currentGroupId = uiState.groupId,
                        calculationRepository = calculationRepository,
                        onDismiss = { showAssignGroupDialog = false },
                        onAssignGroup = { groupId ->
                            viewModel.assignToGroup(groupId)
                            showAssignGroupDialog = false
                        }
                    )
                }
            }
        }
    }
}
