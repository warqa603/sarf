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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.cash.guide.R
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.cash.guide.ui.notebook.HighlighterGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteMarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (!raw.contains("==") && !raw.contains("✓") && !raw.contains("☑")) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val builder = AnnotatedString.Builder(raw)
        
        // Highlighting
        val regex = Regex("==((?:[pgboy]:)?)(.*?)==", RegexOption.DOT_MATCHES_ALL)
        for (match in regex.findAll(raw)) {
            val colorPrefix = match.groupValues[1]
            val hlColor = when (colorPrefix.lowercase()) {
                "p:" -> HighlighterPink.copy(alpha = 0.55f)
                "g:" -> HighlighterGreen.copy(alpha = 0.55f)
                "b:" -> HighlighterBlue.copy(alpha = 0.55f)
                "o:" -> Color(0xFFFFD180).copy(alpha = 0.65f)
                else -> HighlighterYellow.copy(alpha = 0.60f)
            }
            val start = match.range.first
            val end = match.range.last + 1
            val contentStart = start + 2 + colorPrefix.length
            val contentEnd = end - 2

            // Dim delimiters so brackets stay visible but unobtrusive
            builder.addStyle(
                SpanStyle(color = JournalMutedInk.copy(alpha = 0.22f)),
                start,
                contentStart
            )
            builder.addStyle(
                SpanStyle(color = JournalMutedInk.copy(alpha = 0.22f)),
                contentEnd,
                end
            )

            // Highlight content
            if (contentStart < contentEnd) {
                builder.addStyle(
                    SpanStyle(
                        background = hlColor,
                        fontWeight = FontWeight.Medium
                    ),
                    contentStart,
                    contentEnd
                )
            }
        }
        
        // Strikethrough completed checklist items
        var currentOffset = 0
        for (line in raw.split("\n")) {
            if (line.startsWith("✓ ") || line.startsWith("☑ ")) {
                builder.addStyle(
                    SpanStyle(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        color = JournalMutedInk.copy(alpha = 0.55f)
                    ),
                    currentOffset,
                    currentOffset + line.length
                )
            }
            currentOffset += line.length + 1
        }
        
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
private fun NoteVoiceDictationBanner(
    uiState: NoteEditorUiState,
    isRtl: Boolean,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.35f at 0
                1.0f at 500
                0.35f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = JournalPaper,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (uiState.dictationError != null) JournalActionDelete.copy(alpha = 0.55f) else HighlighterPink.copy(alpha = 0.65f)
        ),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.dictationError != null) JournalActionDelete
                                else Color(0xFFE91E63).copy(alpha = pulseAlpha)
                            )
                    )

                    val textToShow = when {
                        uiState.dictationError != null -> uiState.dictationError
                        uiState.partialDictation.isNotBlank() -> uiState.partialDictation
                        uiState.isListening -> stringResource(R.string.note_voice_listening)
                        else -> ""
                    }

                    Text(
                        text = textToShow ?: "",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 14.sp,
                        color = if (uiState.dictationError != null) JournalActionDelete else JournalWritingInk,
                        fontWeight = if (uiState.partialDictation.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(JournalMutedInk.copy(alpha = 0.08f))
                            .clickable(role = Role.Button) { onCancel() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.note_voice_dictation_cancel),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.sp,
                            color = JournalMutedInk,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (uiState.partialDictation.isNotBlank() || uiState.isListening) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HighlighterPink.copy(alpha = 0.55f))
                                .clickable(role = Role.Button) { onCommit() }
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.note_voice_dictation_insert),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 12.sp,
                                color = JournalWritingInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
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
    val clipboardManager = LocalClipboardManager.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAssignGroupDialog by remember { mutableStateOf(false) }
    val titleScrollState = rememberScrollState()

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.permission_mic_rationale),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(titleScrollState.maxValue, uiState.title.text, uiState.activeInputTarget) {
        if (uiState.activeInputTarget == NoteInputTarget.TITLE) {
            titleScrollState.scrollTo(titleScrollState.maxValue)
        }
    }

    val adMobManager = remember { com.cash.guide.domain.ads.AdMobManager.getInstance(context) }
    var initialLoadDone by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(uiState.content.text, uiState.title.text) {
        if (initialLoadDone) {
            adMobManager.reportModification()
        }
        initialLoadDone = true
    }

    // Auto-save on back navigation
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible || uiState.activeInputTarget != NoteInputTarget.NONE) {
        focusManager.clearFocus()
        keyboardController?.hide()
        if (uiState.activeInputTarget != NoteInputTarget.NONE) {
            viewModel.closeKeyboard()
        }
    }

    BackHandler(enabled = !isImeVisible && uiState.activeInputTarget == NoteInputTarget.NONE) {
        viewModel.saveChanges()
        onNavigateBack()
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JournalPaper)
            .imePadding()
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
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val placeholderText = stringResource(R.string.note_title_hint)
                                BasicTextField(
                                    value = uiState.title,
                                    onValueChange = { viewModel.updateTitle(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                viewModel.focusTitle()
                                            }
                                        },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = resolveJournalFont(uiState.title.text.ifBlank { placeholderText }, isRtl),
                                        fontSize = if (isRtl) 16.5.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JournalWritingInk,
                                        platformStyle = NoFontPadding
                                    ),
                                    cursorBrush = SolidColor(cursorColor),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next,
                                        capitalization = KeyboardCapitalization.Sentences
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { viewModel.focusContent() }
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (uiState.title.text.isEmpty()) {
                                                Text(
                                                    text = placeholderText,
                                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                                    fontSize = if (isRtl) 16.sp else 16.5.sp,
                                                    color = JournalMutedInk.copy(alpha = 0.5f),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    style = TextStyle(platformStyle = NoFontPadding)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
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

                                    // Share & Actions Button with Dropdown Menu
                                    var showShareMenu by remember { mutableStateOf(false) }

                                    Box(
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(JournalRuleSpacing)
                                                .width(32.dp)
                                                .clip(CircleShape)
                                                .clickable(role = Role.Button) {
                                                    viewModel.closeKeyboard()
                                                    showShareMenu = true
                                                },
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            HisabiSketchIcon(
                                                symbol = HisabiSymbol.Share,
                                                contentDescription = stringResource(R.string.share_chooser_title),
                                                tint = JournalInk,
                                                size = 18.dp,
                                                modifier = Modifier.offset(y = 2.8.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showShareMenu,
                                            onDismissRequest = { showShareMenu = false },
                                            modifier = Modifier
                                                .background(JournalPaper)
                                                .padding(vertical = 4.dp)
                                        ) {
                                            // 1. Share as Image
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = stringResource(R.string.note_share_as_image),
                                                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                                        fontSize = 14.5.sp,
                                                        color = JournalInk
                                                    )
                                                },
                                                leadingIcon = {
                                                    HisabiSketchIcon(
                                                        symbol = HisabiSymbol.Page,
                                                        contentDescription = null,
                                                        tint = JournalInk,
                                                        size = 18.dp
                                                    )
                                                },
                                                onClick = {
                                                    showShareMenu = false
                                                    viewModel.saveChanges()
                                                    NoteShareHelper.shareAsImage(
                                                        context = context,
                                                        note = viewModel.getNoteEntity(),
                                                        isRtl = isRtl,
                                                        coroutineScope = coroutineScope
                                                    )
                                                }
                                            )

                                            // 2. Share as Text
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = stringResource(R.string.note_share_as_text),
                                                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                                        fontSize = 14.5.sp,
                                                        color = JournalInk
                                                    )
                                                },
                                                leadingIcon = {
                                                    HisabiSketchIcon(
                                                        symbol = HisabiSymbol.Share,
                                                        contentDescription = null,
                                                        tint = JournalInk,
                                                        size = 18.dp
                                                    )
                                                },
                                                onClick = {
                                                    showShareMenu = false
                                                    viewModel.saveChanges()
                                                    NoteShareHelper.shareAsText(
                                                        context = context,
                                                        note = viewModel.getNoteEntity()
                                                    )
                                                }
                                            )

                                            HorizontalDivider(
                                                color = JournalRule.copy(alpha = 0.25f),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )

                                            // 3. Copy whole note
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = stringResource(R.string.note_copy_all),
                                                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                                        fontSize = 14.5.sp,
                                                        color = JournalInk
                                                    )
                                                },
                                                leadingIcon = {
                                                    HisabiSketchIcon(
                                                        symbol = HisabiSymbol.Copy,
                                                        contentDescription = null,
                                                        tint = JournalInk,
                                                        size = 18.dp
                                                    )
                                                },
                                                onClick = {
                                                    showShareMenu = false
                                                    viewModel.copy { text ->
                                                        clipboardManager.setText(AnnotatedString(text))
                                                        Toast.makeText(context, R.string.note_toast_copied, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )

                                            // 4. Duplicate note
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = stringResource(R.string.note_duplicate),
                                                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                                        fontSize = 14.5.sp,
                                                        color = JournalInk
                                                    )
                                                },
                                                leadingIcon = {
                                                    HisabiSketchIcon(
                                                        symbol = HisabiSymbol.Plus,
                                                        contentDescription = null,
                                                        tint = JournalInk,
                                                        size = 18.dp
                                                    )
                                                },
                                                onClick = {
                                                    showShareMenu = false
                                                    viewModel.saveChanges()
                                                    val suffix = context.getString(R.string.note_duplicate_suffix)
                                                    viewModel.duplicateNote(suffix) {
                                                        Toast.makeText(context, R.string.note_toast_duplicated, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )

                                            // 5. Clear text
                                            if (uiState.content.text.isNotEmpty()) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = stringResource(R.string.note_clear_content),
                                                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                                            fontSize = 14.5.sp,
                                                            color = JournalActionDelete
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        HisabiSketchIcon(
                                                            symbol = HisabiSymbol.Close,
                                                            contentDescription = null,
                                                            tint = JournalActionDelete,
                                                            size = 18.dp
                                                        )
                                                    },
                                                    onClick = {
                                                        showShareMenu = false
                                                        viewModel.clearContent()
                                                    }
                                                )
                                            }
                                        }
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                            ) {
                                val density = LocalDensity.current
                                val ruleSpacingSp = with(density) { JournalRuleSpacing.toSp() }
                                val rowHeightPx = with(density) { JournalRuleSpacing.toPx() }

                                val contentText = uiState.content.text
                                val isArabicKeyboard = uiState.keyboardLanguage == com.cash.guide.domain.JournalKeyboardLanguage.ARABIC
                                val isContentRtl = isRtl || (contentText.isNotEmpty() && isArabicScript(contentText)) || (contentText.isEmpty() && isArabicKeyboard)
                                val placeholderText = if (isContentRtl) "اكتب ملاحظة..." else "Écrire une note..."

                                val contentTextStyle = remember(isContentRtl, contentText, placeholderText, ruleSpacingSp) {
                                    TextStyle(
                                        fontFamily = resolveJournalFont(contentText.ifBlank { placeholderText }, isContentRtl),
                                        fontSize = if (isContentRtl) 16.sp else 16.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = JournalInk,
                                        lineHeight = ruleSpacingSp,
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Proportional,
                                            trim = LineHeightStyle.Trim.None
                                        ),
                                        platformStyle = NoFontPadding
                                    )
                                }

                                val textMeasurer = rememberTextMeasurer()
                                val measuredBaseline = remember(contentTextStyle, isContentRtl) {
                                    val sampleText = if (isContentRtl) "ملاحظة" else "Ay"
                                    val result = textMeasurer.measure(
                                        text = AnnotatedString(sampleText),
                                        style = contentTextStyle
                                    )
                                    result.getLineBaseline(0)
                                }

                                val yShiftPx = (rowHeightPx - with(density) { 1.0.dp.toPx() }) - measuredBaseline
                                val yShiftDp = with(density) { yShiftPx.toDp() }

                                BasicTextField(
                                    value = uiState.content,
                                    onValueChange = { viewModel.updateContent(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = yShiftDp)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                viewModel.focusContent()
                                            }
                                        },
                                    textStyle = contentTextStyle,
                                    visualTransformation = remember { NoteMarkdownVisualTransformation() },
                                    cursorBrush = SolidColor(cursorColor),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        capitalization = KeyboardCapitalization.Sentences
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = JournalRuleSpacing * 8),
                                            contentAlignment = Alignment.TopStart
                                        ) {
                                            if (contentText.isEmpty()) {
                                                Text(
                                                    text = placeholderText,
                                                    style = contentTextStyle.copy(color = JournalMutedInk.copy(alpha = 0.45f))
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            // 4 blank rules at bottom for breathing room
                            Spacer(modifier = Modifier.height(JournalRuleSpacing * 4))
                        }
                    }

                    if (uiState.isListening || uiState.partialDictation.isNotBlank() || uiState.dictationError != null) {
                        NoteVoiceDictationBanner(
                            uiState = uiState,
                            isRtl = isRtl,
                            onCommit = { viewModel.commitDictation() },
                            onCancel = { viewModel.cancelDictation() }
                        )
                    }

                    NoteAccessoryBar(
                        uiState = uiState,
                        viewModel = viewModel,
                        isRtl = isRtl,
                        onRequestAudioPermission = {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onShowToast = { resId ->
                            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.navigationBarsPadding())
                }

                // Delete Confirmation Dialog
                if (showDeleteDialog) {
                    val noteTitle = uiState.title.text.ifBlank { stringResource(R.string.note_this_note) }
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
                                    text = stringResource(R.string.action_delete),
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalActionDelete
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(
                                    text = stringResource(R.string.action_cancel),
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

@Composable
private fun NoteAccessoryBar(
    uiState: NoteEditorUiState,
    viewModel: NoteViewModel,
    isRtl: Boolean,
    onRequestAudioPermission: () -> Unit,
    onShowToast: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val isListening = uiState.isListening

    val (words, chars) = remember(uiState.content.text) {
        viewModel.getStats()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = JournalPaper,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(JournalRule.copy(alpha = 0.35f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 1. Undo
                AccessoryIconButton(
                    symbol = HisabiSymbol.Undo,
                    contentDescription = stringResource(R.string.note_action_undo),
                    enabled = uiState.canUndo,
                    onClick = { viewModel.undo() }
                )

                // 2. Redo
                AccessoryIconButton(
                    symbol = HisabiSymbol.Redo,
                    contentDescription = stringResource(R.string.note_action_redo),
                    enabled = uiState.canRedo,
                    onClick = { viewModel.redo() }
                )

                AccessoryVerticalDivider()

                // 3. Paste
                AccessoryIconButton(
                    symbol = HisabiSymbol.Paste,
                    contentDescription = stringResource(R.string.note_action_paste),
                    onClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrEmpty()) {
                            viewModel.paste(clipText)
                            onShowToast(R.string.note_toast_pasted)
                        } else {
                            onShowToast(R.string.note_toast_empty_clipboard)
                        }
                    }
                )

                // 4. Copy
                AccessoryIconButton(
                    symbol = HisabiSymbol.Copy,
                    contentDescription = stringResource(R.string.note_action_copy),
                    onClick = {
                        viewModel.copy { textToCopy ->
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            onShowToast(R.string.note_toast_copied)
                        }
                    }
                )

                // 5. Cut
                AccessoryIconButton(
                    symbol = HisabiSymbol.Cut,
                    contentDescription = stringResource(R.string.note_action_cut),
                    onClick = {
                        viewModel.cut { textToCut ->
                            clipboardManager.setText(AnnotatedString(textToCut))
                            onShowToast(R.string.note_toast_cut)
                        }
                    }
                )

                // 6. Select All
                AccessoryTextButton(
                    text = stringResource(R.string.note_select_all_btn),
                    onClick = { viewModel.selectAll() }
                )

                AccessoryVerticalDivider()

                // 7. Highlighter Tool
                var showHighlighterPalette by remember { mutableStateOf(false) }
                val activeHlColor = when (uiState.activeHighlighterColor) {
                    "PINK" -> HighlighterPink
                    "GREEN" -> HighlighterGreen
                    "BLUE" -> HighlighterBlue
                    "ORANGE" -> Color(0xFFFFD180)
                    else -> HighlighterYellow
                }

                Box(contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(activeHlColor.copy(alpha = 0.35f))
                            .clickable(role = Role.Button) {
                                viewModel.toggleHighlight()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Highlighter,
                            contentDescription = stringResource(R.string.note_action_highlight),
                            tint = JournalWritingInk,
                            size = 17.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .clickable { showHighlighterPalette = true },
                            contentAlignment = Alignment.Center
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.ChevronDown,
                                contentDescription = null,
                                tint = JournalMutedInk,
                                size = 9.dp
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showHighlighterPalette,
                        onDismissRequest = { showHighlighterPalette = false },
                        modifier = Modifier
                            .background(JournalPaper)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            val colors = listOf(
                                "YELLOW" to HighlighterYellow,
                                "GREEN" to HighlighterGreen,
                                "PINK" to HighlighterPink,
                                "BLUE" to HighlighterBlue,
                                "ORANGE" to Color(0xFFFFD180)
                            )
                            colors.forEach { (tag, color) ->
                                val isSelected = uiState.activeHighlighterColor == tag
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.setActiveHighlighterColor(tag)
                                            viewModel.toggleHighlight(tag)
                                            showHighlighterPalette = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 22.dp else 18.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.85f))
                                            .then(
                                                if (isSelected) Modifier.border(2.dp, JournalInk, CircleShape)
                                                else Modifier.border(1.dp, JournalInk.copy(alpha = 0.25f), CircleShape)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                AccessoryVerticalDivider()

                // 8. Checkbox
                AccessoryIconButton(
                    symbol = HisabiSymbol.CheckBox,
                    contentDescription = stringResource(R.string.note_action_checkbox),
                    onClick = { viewModel.insertCheckbox() }
                )

                // 9. Bullet
                AccessoryTextButton(
                    text = "•",
                    fontSize = 20.sp,
                    onClick = { viewModel.insertBullet() }
                )

                // 10. Numbered list
                AccessoryTextButton(
                    text = "1.",
                    fontSize = 14.sp,
                    onClick = { viewModel.insertNumberedItem() }
                )

                // 11. Divider
                AccessoryTextButton(
                    text = "───",
                    fontSize = 11.sp,
                    onClick = { viewModel.insertDivider() }
                )

                // 12. Timestamp
                AccessoryIconButton(
                    symbol = HisabiSymbol.Clock,
                    contentDescription = stringResource(R.string.note_action_timestamp),
                    onClick = { viewModel.insertTimestamp() }
                )

                AccessoryVerticalDivider()

                // 13. Mic / Dictation
                val micBg = if (isListening) Color(0xFFFCE4EC) else Color.Transparent
                val micTint = if (isListening) Color(0xFFE91E63) else JournalInk
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(micBg)
                        .clickable(role = Role.Button) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (isListening) {
                                viewModel.commitDictation()
                            } else {
                                if (hasPermission) {
                                    viewModel.startListening()
                                } else {
                                    onRequestAudioPermission()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Microphone,
                        contentDescription = stringResource(R.string.note_voice_hint),
                        tint = micTint,
                        size = 17.dp
                    )
                }

                // 13. Word & Char Stats
                if (chars > 0) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(JournalMutedInk.copy(alpha = 0.08f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.note_stats_format, words, chars),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 11.sp,
                            color = JournalMutedInk.copy(alpha = 0.8f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessoryIconButton(
    symbol: HisabiSymbol,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        HisabiSketchIcon(
            symbol = symbol,
            contentDescription = contentDescription,
            tint = if (enabled) JournalInk else JournalMutedInk.copy(alpha = 0.28f),
            size = 16.dp
        )
    }
}

@Composable
private fun AccessoryTextButton(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = PatrickHandFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = JournalInk,
            style = TextStyle(platformStyle = NoFontPadding)
        )
    }
}

@Composable
private fun AccessoryVerticalDivider() {
    Box(
        modifier = Modifier
            .height(16.dp)
            .width(1.dp)
            .background(JournalRule.copy(alpha = 0.35f))
    )
}
