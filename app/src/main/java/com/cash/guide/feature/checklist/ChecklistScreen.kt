package com.cash.guide.feature.checklist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.widget.Toast
import com.cash.guide.ui.components.AiVoiceAssistantButton
import com.cash.guide.ui.components.AiVoiceInputTarget
import com.cash.guide.ui.components.AiVoiceRowContainer
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.CalculationRepository
import com.cash.guide.domain.ChecklistShareHelper
import com.cash.guide.domain.GroupCategory
import com.cash.guide.feature.groups.AssignToGroupDialog
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.JournalActionDelete
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.JournalRuleSpacing
import androidx.compose.foundation.lazy.itemsIndexed
import com.cash.guide.ui.notebook.JournalLazyRuledDocument
import com.cash.guide.ui.notebook.JournalTextKeyboardDock
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalTextOnRules
import com.cash.guide.ui.notebook.resolveJournalFont

private val ColorEmerald = Color(0xFF1B7A4B)
private val ColorCoral = Color(0xFFD9534F)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChecklistScreen(
    viewModel: ChecklistViewModel,
    calculationRepository: CalculationRepository,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showAssignGroupDialog by remember { mutableStateOf(false) }

    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    var showShareMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAiVoiceDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible || state.activeInputTarget != ChecklistInputTarget.NONE) {
        focusManager.clearFocus()
        keyboardController?.hide()
        if (state.activeInputTarget != ChecklistInputTarget.NONE) {
            viewModel.hideKeyboard()
        }
    }

    BackHandler(enabled = !isImeVisible && state.activeInputTarget == ChecklistInputTarget.NONE) {
        onNavigateBack()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "checklist_cursor")
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

    val rowDotColors = remember {
        listOf(
            Color(0xFF3B82B6),
            Color(0xFFD65D82),
            Color(0xFF5E8C3B),
            Color(0xFFC7881E),
            Color(0xFF7E5AA8),
            Color(0xFFCC673B)
        )
    }

    val current = state.currentChecklist
    val items = current?.sortedItems ?: emptyList()
    val totalCount = current?.totalCount ?: 0
    val completedCount = current?.completedCount ?: 0
    val isAllCompleted = totalCount > 0 && completedCount == totalCount

    val adMobManager = remember { com.cash.guide.domain.ads.AdMobManager.getInstance(context) }
    var initialLoadDone by remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(state.titleInput.text, current?.sortedItems) {
        if (initialLoadDone) {
            adMobManager.reportModification()
        }
        initialLoadDone = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JournalPaper)
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = JournalPaper,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // --- Row 1: Back button + Title in Watercolor Pink Pill (Centered / Alone) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = {
                                    if (state.activeInputTarget != ChecklistInputTarget.NONE) {
                                        viewModel.hideKeyboard()
                                    }
                                    onNavigateBack()
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Back,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = JournalInk,
                                size = 20.dp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (state.activeInputTarget == ChecklistInputTarget.TITLE) HighlighterPink.copy(alpha = 0.55f)
                                    else HighlighterPink.copy(alpha = 0.35f)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val defaultTitle = if (isRtl) "قائمة" else "Checklist"
                            BasicTextField(
                                value = state.titleInput,
                                onValueChange = { viewModel.updateTitleInput(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            viewModel.focusTitle()
                                        }
                                    },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = resolveJournalFont(state.titleInput.text.ifBlank { defaultTitle }, isRtl),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalWritingInk,
                                    textAlign = TextAlign.Center,
                                    platformStyle = NoFontPadding
                                ),
                                cursorBrush = SolidColor(JournalWritingInk),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done,
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        viewModel.hideKeyboard()
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (state.titleInput.text.isEmpty()) {
                                            Text(
                                                text = defaultTitle,
                                                fontFamily = resolveJournalFont(defaultTitle, isRtl),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = JournalWritingInk.copy(alpha = 0.5f),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = TextStyle(platformStyle = NoFontPadding)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        // Folder / Group button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = {
                                    if (state.activeInputTarget != ChecklistInputTarget.NONE) {
                                        viewModel.hideKeyboard()
                                    }
                                    showAssignGroupDialog = true
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            HisabiSketchIcon(
                                symbol = HisabiSymbol.Folder,
                                contentDescription = "Groupe",
                                tint = if (current?.checklist?.groupId != null) JournalWritingInk else JournalMutedInk,
                                size = 20.dp
                            )
                        }
                    }

                    // --- Row 2: Sub-toolbar (Left: Count & Status, Right: Partager ↗) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val countText = if (isRtl) "$completedCount / $totalCount مكتمل" else "$completedCount / $totalCount faits"
                            Text(
                                text = countText,
                                fontFamily = resolveJournalFont(countText, isRtl),
                                fontSize = if (isRtl) 14.sp else 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllCompleted) ColorEmerald else JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                            if (isAllCompleted) {
                                Text(
                                    text = "✓",
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorEmerald,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }

                        val shareText = if (isRtl) "مشاركة ↗" else "Partager ↗"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(role = Role.Button) {
                                    viewModel.hideKeyboard()
                                    showShareMenu = true
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .drawBehind {
                                    val strokeW = 1.35.dp.toPx()
                                    val y = size.height + 2.5.dp.toPx()
                                    drawLine(
                                        color = HighlighterPink,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shareText,
                                fontFamily = resolveJournalFont(shareText, isRtl),
                                fontSize = if (isRtl) 14.sp else 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )

                            DropdownMenu(
                                expanded = showShareMenu,
                                onDismissRequest = { showShareMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isRtl) "🖼️ مشاركة كصورة (ورقة مذكرة)"
                                            else "🖼️ Partager comme image (Carnet)"
                                        )
                                    },
                                    onClick = {
                                        showShareMenu = false
                                        val cl = state.currentChecklist ?: return@DropdownMenuItem
                                        ChecklistShareHelper.shareAsImage(
                                            context = context,
                                            checklistId = cl.checklist.id,
                                            title = cl.checklist.title,
                                            items = cl.sortedItems,
                                            isRtl = isRtl,
                                            coroutineScope = coroutineScope
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isRtl) "🔗 مشاركة النص والرابط (كافة التطبيقات)"
                                            else "🔗 Partager texte & lien (Toutes les applications)"
                                        )
                                    },
                                    onClick = {
                                        showShareMenu = false
                                        val cl = state.currentChecklist ?: return@DropdownMenuItem
                                        ChecklistShareHelper.shareAsTextAndLink(
                                            context = context,
                                            title = cl.checklist.title,
                                            items = cl.sortedItems
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Divider line separating Header from notebook
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(JournalRule.copy(alpha = 0.35f))
                    )
                }
            }

            JournalLazyRuledDocument(
                listState = listState,
                clearFocusOnTap = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Always have 1 notebook rule at top: either "Supprimer les cochés" or an empty spacer rule
                item(key = "top_rule") {
                    if (completedCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(JournalRuleSpacing)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.End
                        ) {
                            val deleteCheckedText = if (isRtl) "حذف المشطوبين ($completedCount)" else "Supprimer les cochés ($completedCount)"
                            Text(
                                text = deleteCheckedText,
                                fontFamily = resolveJournalFont(deleteCheckedText, isRtl),
                                fontSize = if (isRtl) 13.sp else 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorCoral,
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier
                                    .journalBaselineOnRule()
                                    .clickable { viewModel.deleteCompletedItems() }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(JournalRuleSpacing))
                    }
                }

                if (items.isEmpty()) {
                    item(key = "empty_hint") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(JournalRuleSpacing)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val emptyHint = if (isRtl) "أدخل عنصراً بالأسفل للبدء في كتابة قائمتك ✍️" else "Tapez un article ci-dessous pour commencer votre liste ✍️"
                            Text(
                                text = emptyHint,
                                fontFamily = resolveJournalFont(emptyHint, isRtl),
                                fontSize = if (isRtl) 14.5.sp else 15.sp,
                                color = JournalMutedInk.copy(alpha = 0.50f),
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier.journalBaselineOnRule()
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    val rowNumber = index + 1
                    val dotColor = rowDotColors[index % rowDotColors.size]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = JournalRuleSpacing)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left: Number (sitting directly on Rule 1)
                        Text(
                            text = "$rowNumber",
                            fontFamily = PatrickHandFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = dotColor,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier
                                .widthIn(min = 16.dp)
                                .journalBaselineOnRule()
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Center: Item text (takes all available width, wraps up to 2 lines, sits directly on ruled lines)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.toggleItem(item.id, !item.isChecked)
                                }
                        ) {
                            Text(
                                text = item.text,
                                fontFamily = resolveJournalFont(item.text, isRtl),
                                fontSize = if (isRtl) 17.sp else 17.5.sp,
                                lineHeight = 29.sp,
                                fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.Medium,
                                color = if (item.isChecked) JournalMutedInk.copy(alpha = 0.55f) else JournalInk,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                style = TextStyle(platformStyle = NoFontPadding, lineHeight = 29.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .journalTextOnRules()
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: [Checkbox] on the left, [Trash Icon] on the far right (Reversed!)
                        Row(
                            modifier = Modifier.height(JournalRuleSpacing),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Checkbox (left of trash)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(
                                        role = Role.Checkbox,
                                        onClickLabel = if (item.isChecked) "Décocher" else "Cocher",
                                        onClick = { viewModel.toggleItem(item.id, !item.isChecked) }
                                    )
                                    .journalBaselineOnRule(opticalOffsetFromBottom = 2.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Canvas(
                                    modifier = Modifier.size(19.dp)
                                ) {
                                    val strokeWidth = 1.35.dp.toPx()
                                    val corner = 3.dp.toPx()
                                    val rect = androidx.compose.ui.geometry.RoundRect(
                                        left = strokeWidth / 2f,
                                        top = strokeWidth / 2f,
                                        right = size.width - strokeWidth / 2f,
                                        bottom = size.height - strokeWidth / 2f,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
                                    )
                                    val path = Path().apply { addRoundRect(rect) }

                                    if (item.isChecked) {
                                        // Transparent background (no color fill)
                                        drawPath(
                                            path = path,
                                            color = Color(0xFF16A34A).copy(alpha = 0.85f),
                                            style = Stroke(width = strokeWidth)
                                        )
                                        val checkPath = Path().apply {
                                            moveTo(size.width * 0.20f, size.height * 0.52f)
                                            lineTo(size.width * 0.40f, size.height * 0.74f)
                                            lineTo(size.width * 0.82f, size.height * 0.22f)
                                        }
                                        drawPath(
                                            path = checkPath,
                                            color = Color(0xFF16A34A),
                                            style = Stroke(
                                                width = 2.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    } else {
                                        drawPath(
                                            path = path,
                                            color = JournalInk.copy(alpha = 0.40f),
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                }
                            }

                            // Trash icon (far right)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Supprimer l'élément",
                                        onClick = { viewModel.deleteItem(item.id) }
                                    )
                                    .journalBaselineOnRule(opticalOffsetFromBottom = 2.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                HisabiSketchIcon(
                                    symbol = HisabiSymbol.Trash,
                                    contentDescription = "Supprimer",
                                    tint = JournalActionDelete.copy(alpha = 0.70f),
                                    size = 17.dp
                                )
                            }
                        }
                    }
                }

                // Delete list action at the bottom of the list
                if (items.isNotEmpty()) {
                    item(key = "delete_list_action") {
                        Column {
                            Spacer(modifier = Modifier.height(JournalRuleSpacing))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(JournalRuleSpacing)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val deleteListText = if (isRtl) "حذف هذه القائمة" else "Supprimer cette liste"
                                Text(
                                    text = deleteListText,
                                    fontFamily = resolveJournalFont(deleteListText, isRtl),
                                    fontSize = if (isRtl) 13.sp else 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorCoral.copy(alpha = 0.75f),
                                    style = TextStyle(platformStyle = NoFontPadding),
                                    modifier = Modifier
                                        .journalBaselineOnRule()
                                        .clickable { showDeleteConfirmDialog = true }
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(JournalRuleSpacing * 3))
                }
            }

            AiVoiceRowContainer(
                target = AiVoiceInputTarget.CHECKLIST,
                onChecklistResult = { result ->
                    viewModel.addMultipleItems(result.items)
                    Toast.makeText(
                        context,
                        if (isRtl) "تمت إضافة ${result.items.size} عناصر بالذكاء الاصطناعي 🪄" else "${result.items.size} éléments ajoutés avec l'IA 🪄",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JournalPaper)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(JournalPaper)
                        .border(
                            BorderStroke(
                                width = 1.3.dp,
                                color = if (state.activeInputTarget == ChecklistInputTarget.ITEM_INPUT) JournalInk else JournalMutedInk.copy(alpha = 0.35f)
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "+",
                            fontFamily = PatrickHandFamily,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk
                        )

                        val placeholderText = if (isRtl) "زيد شي حاجة (مثلاً: خبز، حليب...)" else "Ajouter un élément (ex: Pain, Lait...)"
                        BasicTextField(
                            value = state.inputText,
                            onValueChange = { viewModel.updateInputText(it) },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        viewModel.focusItemInput()
                                    }
                                },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = resolveJournalFont(state.inputText.text, isRtl),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = JournalInk,
                                platformStyle = NoFontPadding
                            ),
                            cursorBrush = SolidColor(JournalInk),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (state.inputText.text.isNotBlank()) {
                                        viewModel.addItem()
                                    } else {
                                        keyboardController?.hide()
                                        viewModel.hideKeyboard()
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (state.inputText.text.isEmpty()) {
                                        Text(
                                            text = placeholderText,
                                            fontFamily = resolveJournalFont(placeholderText, isRtl),
                                            fontSize = if (isRtl) 14.sp else 15.sp,
                                            color = JournalMutedInk.copy(alpha = 0.45f),
                                            style = TextStyle(platformStyle = NoFontPadding)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    if (state.inputText.text.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HighlighterPink.copy(alpha = 0.65f))
                                .clickable { viewModel.addItem() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val addBtnText = if (isRtl) "إضافة" else "Ajouter"
                            Text(
                                text = addBtnText,
                                fontFamily = resolveJournalFont(addBtnText, isRtl),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }

    if (showDeleteConfirmDialog) {
        val dialogTitle = if (isRtl) "حذف القائمة ؟" else "Supprimer la checklist ?"
        val dialogMessage = if (isRtl) "هل أنت متأكد من رغبتك في حذف هذه القائمة نهائياً؟ هذا الإجراء لا يمكن التراجع عنه." else "Êtes-vous sûr de vouloir supprimer cette checklist ? Cette action est irréversible."
        val confirmText = if (isRtl) "حذف" else "Supprimer"
        val dismissText = if (isRtl) "إلغاء" else "Annuler"
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = JournalPaper,
            title = {
                Text(
                    text = dialogTitle,
                    fontFamily = resolveJournalFont(dialogTitle, isRtl),
                    fontSize = if (isRtl) 17.5.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorCoral
                )
            },
            text = {
                Text(
                    text = dialogMessage,
                    fontFamily = resolveJournalFont(dialogMessage, isRtl),
                    fontSize = if (isRtl) 14.5.sp else 16.sp,
                    color = JournalInk
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteCurrentChecklist()
                        onNavigateBack()
                    }
                ) {
                    Text(
                        text = confirmText,
                        fontFamily = resolveJournalFont(confirmText, isRtl),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCoral
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = dismissText,
                        fontFamily = resolveJournalFont(dismissText, isRtl),
                        fontSize = 15.sp,
                        color = JournalMutedInk
                    )
                }
            }
        )
    }

    if (showAssignGroupDialog && current != null) {
        AssignToGroupDialog(
            category = GroupCategory.CHECKLISTS,
            currentGroupId = current.checklist.groupId,
            calculationRepository = calculationRepository,
            onDismiss = { showAssignGroupDialog = false },
            onAssignGroup = { groupId ->
                viewModel.assignToGroup(groupId)
                showAssignGroupDialog = false
            }
        )
    }
}
