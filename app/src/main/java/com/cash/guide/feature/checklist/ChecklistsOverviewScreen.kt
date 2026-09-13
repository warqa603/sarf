package com.cash.guide.feature.checklist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.JournalActionDelete
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.JournalSectionBadge
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.JournalInlineSearchRow
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.resolveJournalFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ColorEmerald = Color(0xFF1B7A4B)
private val ColorCoral = Color(0xFFD9534F)
private val ColorOrange = Color(0xFFEA580C)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChecklistsOverviewScreen(
    viewModel: ChecklistsOverviewViewModel,
    onOpenChecklist: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checklists by viewModel.checklists.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
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

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
    }

    val filteredChecklists = remember(checklists, searchQuery) {
        if (searchQuery.isBlank()) {
            checklists
        } else {
            val q = searchQuery.trim()
            checklists.filter { item ->
                item.checklist.title.contains(q, ignoreCase = true) ||
                    item.items.any { it.text.contains(q, ignoreCase = true) }
            }
        }
    }

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

    val shortDateFormatter = remember {
        SimpleDateFormat("d MMM", Locale.getDefault())
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
                    // Start: Back button + "Mes Checklists" soft green pill
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
                                contentDescription = "Retour",
                                tint = JournalInk,
                                size = 20.dp
                            )
                        }

                        // Green Highlighter Pill with green dot: "Mes Checklists" / "قوائم المهام"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HighlighterGreen.copy(alpha = 0.45f))
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
                                        .background(Color(0xFF22C55E), CircleShape)
                                )
                                val titleText = if (isRtl) "قوائم المهام" else "Mes Checklists"
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

                    // End: count badge
                    Text(
                        text = if (isRtl) "${checklists.size} قوائم" else "${checklists.size} listes",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 14.sp else 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalWritingInk.copy(alpha = 0.80f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Notebook ruled list
            JournalRuledDocument(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                clearFocusOnTap = true
            ) {
                // Line 1: 1 exact notebook rule spacer (29dp)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 2: Search Bar directly resting on the ruled blue line (29dp)
                JournalInlineSearchRow(
                    query = searchQuery,
                    onQueryChange = { query -> searchQuery = query },
                    placeholder = if (isRtl) "بحث في قوائم المهام..." else "Rechercher une checklist..."
                )

                // Line 3: 1 rule spacer (tna9ez star)
                Spacer(modifier = Modifier.height(JournalRuleSpacing))

                // Line 4: Action Button: "Créer une nouvelle checklist" (1 rule = 29dp)
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
                            .background(HighlighterGreen.copy(alpha = 0.35f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = if (isRtl) "إنشاء قائمة جديدة" else "Créer une nouvelle checklist",
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
                            val btnText = if (isRtl) "إنشاء قائمة جديدة" else "Créer une nouvelle checklist"
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

                // Line 6: "Toutes les checklists" badge with unified background "X" (1 rule = 29dp)
                val sectionText = if (isRtl) "جميع القوائم" else "Toutes les checklists"
                JournalSectionBadge(
                    title = sectionText,
                    badgeColor = HighlighterGreen.copy(alpha = 0.30f)
                )

                // Line 5+: Content dial checklists
                if (filteredChecklists.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing * 6)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Check,
                            contentDescription = null,
                            tint = JournalMutedInk.copy(alpha = 0.40f),
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                if (isRtl) "لا توجد نتائج للبحث" else "Aucune checklist trouvée"
                            } else {
                                if (isRtl) "لا توجد أي قائمة حالياً" else "Aucune checklist pour l'instant"
                            },
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalMutedInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRtl) "اضغط على الزر أسفله لإنشاء قائمتك الأولى ✍️" else "Appuyez sur le bouton ci-dessous pour créer une liste ✍️",
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 13.5.sp,
                            color = JournalMutedInk.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    filteredChecklists.forEachIndexed { index, item ->
                        val dotColor = rowDotColors[index % rowDotColors.size]
                        val total = item.totalCount
                        val completed = item.completedCount
                        val isDone = total > 0 && completed == total
                        val shortDateStr = shortDateFormatter.format(Date(item.checklist.createdAtEpochMs))

                        ChecklistRowItem(
                            item = item,
                            dotColor = dotColor,
                            shortDateStr = shortDateStr,
                            isDone = isDone,
                            isRtl = isRtl,
                            onOpenChecklist = { onOpenChecklist(item.checklist.id) },
                            onDelete = { viewModel.promptDeleteChecklist(item) }
                        )
                    }
                }

                // Extra breathing room at bottom above navigation bar
                Spacer(modifier = Modifier.height(JournalRuleSpacing * 3))
            }
        }
    }

    // Create Checklist Dialog
    if (uiState.showCreateDialog) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateDialog() },
            containerColor = JournalPaper,
            title = {
                val dialogTitle = if (isRtl) "قائمة جديدة" else "Nouvelle checklist"
                Text(
                    text = dialogTitle,
                    fontFamily = resolveJournalFont(dialogTitle, isRtl),
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRtl) "اسم القائمة (مثال: سخرة، خضار، مقاضي...)" else "Nom de la checklist (ex: Skhra, Supermarché...)",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 13.sp,
                        color = JournalMutedInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = uiState.newChecklistTitle,
                        onValueChange = { viewModel.setNewChecklistTitle(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .drawBehind {
                                val strokeW = 1.2.dp.toPx()
                                val y = size.height
                                drawLine(
                                    color = JournalInk.copy(alpha = 0.6f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = strokeW,
                                    cap = StrokeCap.Round
                                )
                            }
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        cursorBrush = SolidColor(JournalInk),
                        textStyle = TextStyle(
                            fontFamily = resolveJournalFont(uiState.newChecklistTitle, isRtl),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalInk
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.confirmCreateChecklist { newId ->
                                onOpenChecklist(newId)
                            }
                        })
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmCreateChecklist { newId ->
                            onOpenChecklist(newId)
                        }
                    }
                ) {
                    Text(
                        text = if (isRtl) "إنشاء" else "Créer",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorEmerald
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateDialog() }) {
                    Text(
                        text = if (isRtl) "إلغاء" else "Annuler",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalMutedInk
                    )
                }
            }
        )
    }

    // Delete Confirmation Dialog
    val toDelete = uiState.checklistToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            containerColor = JournalPaper,
            title = {
                Text(
                    text = if (isRtl) "حذف القائمة ؟" else "Supprimer la checklist ?",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
            },
            text = {
                Text(
                    text = if (isRtl) {
                        "هل أنت متأكد من رغبتك في حذف « ${toDelete.checklist.title} » وجميع عناصرها نهائياً؟"
                    } else {
                        "Voulez-vous vraiment supprimer « ${toDelete.checklist.title} » et tous ses éléments ?"
                    },
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 14.5.sp,
                    color = JournalInk
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteChecklist() }) {
                    Text(
                        text = if (isRtl) "حذف" else "Supprimer",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCoral
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(
                        text = if (isRtl) "إلغاء" else "Annuler",
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        color = JournalMutedInk
                    )
                }
            }
        )
    }
}

/**
 * Single Checklist Item rendered across exactly 2 notebook lines (58dp):
 * Line 1 (29dp): Colored dot + Title + Dashed line + Status ("En cours" / "Terminé") + Trash icon (🗑)
 * Line 2 (29dp): Subtitle (Date + items preview) sitting strictly ON the blue rule line
 * Directly on ruled paper with zero card container!
 */
@Composable
private fun ChecklistRowItem(
    item: ChecklistWithItems,
    dotColor: Color,
    shortDateStr: String,
    isDone: Boolean,
    isRtl: Boolean,
    onOpenChecklist: () -> Unit,
    onDelete: () -> Unit
) {
    val displayTitle = item.checklist.title.ifBlank { if (isRtl) "قائمة بدون عنوان" else "Checklist sans titre" }
    val itemsSummary = if (item.items.isNotEmpty()) {
        item.sortedItems.joinToString(", ") { it.text }
    } else ""
    val subtitleText = if (itemsSummary.isNotBlank()) {
        "$shortDateStr • $itemsSummary"
    } else {
        if (isRtl) "$shortDateStr • قائمة فارغة..." else "$shortDateStr • Liste vide..."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpenChecklist)
    ) {
        // Line 1 (29dp): Dot + Title + Dashed line + Status + Trash icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Dot + Title
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

            // End: Status ("En cours" / "Terminé") + Trash icon (🗑)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statusText = if (isDone) {
                    if (isRtl) "مكتمل" else "Terminé"
                } else {
                    if (isRtl) "قيد الإنجاز" else "En cours"
                }
                val statusColor = if (isDone) ColorEmerald else ColorOrange

                Text(
                    text = statusText,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = statusColor,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule(opticalOffsetFromBottom = (-0.5).dp)
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Supprimer cette checklist",
                            onClick = onDelete
                        )
                        .journalBaselineOnRule(opticalOffsetFromBottom = 0.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Trash,
                        contentDescription = "Supprimer",
                        tint = JournalActionDelete.copy(alpha = 0.65f),
                        size = 14.5.dp,
                        modifier = Modifier.offset(y = 0.5.dp)
                    )
                }
            }
        }

        // Line 2 (29dp): Subtitle (Date + items preview) sitting strictly ON the rule line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(start = 27.dp, end = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = subtitleText,
                fontFamily = resolveJournalFont(subtitleText, isRtl),
                fontSize = if (isArabicScript(subtitleText)) 11.5.sp else 12.sp,
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

