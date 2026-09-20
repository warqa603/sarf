package com.cash.guide.ui.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.cash.guide.data.db.CalculationWithItems
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R

/**
 * Line spacing for the Actions Popup sheet.
 * Slightly larger than the homepage (42dp vs 29dp) for comfortable touch targets
 * and badge placement, while maintaining authentic lined notebook paper rhythm.
 */
private val ActionSheetRuleSpacing: Dp = 42.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedCalculationActionsSheet(
    calculationTitle: String,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    paymentStatus: String = "PAID",
    onTogglePaymentStatus: (() -> Unit)? = null,
    calcType: String = "PERSONNEL",
    onToggleCalcType: (() -> Unit)? = null,
    onSetDueDate: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onSaveAsTemplate: (() -> Unit)? = null,
    onShareImage: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onExportExcel: (() -> Unit)? = null,
    onAssignToGroup: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.5.dp)
                        .background(JournalRule.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides layoutDirection
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Row 1: Calculation Title sitting directly on its ruled notebook line (centered)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ActionSheetRuleSpacing)
                        .drawBehind {
                            val strokeW = 0.6.dp.toPx()
                            val y = size.height
                            drawLine(
                                color = JournalRule.copy(alpha = 0.55f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeW
                            )
                        }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val titleText = calculationTitle.ifBlank { stringResource(R.string.editor_new_title) }
                    Text(
                        text = titleText,
                        fontFamily = resolveJournalFont(titleText, isRtl),
                        fontSize = if (isArabicScript(titleText) || isRtl) 16.sp else 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier
                            .fillMaxWidth()
                            .journalBaselineOnRule(lineHeight = ActionSheetRuleSpacing)
                    )
                }

                // Row 2: Épingler / Désépingler (Pin)
                ActionSheetRuledItem(
                    label = stringResource(if (isPinned) R.string.action_unpin else R.string.action_pin),
                    symbol = HisabiSymbol.Pin,
                    badgeColor = HighlighterBlue.copy(alpha = 0.50f),
                    onClick = {
                        onDismiss()
                        onTogglePin()
                    }
                )

                // Row: Marquer comme payé / Marquer comme crédit (if CREDIT)
                if (calcType == "CREDIT" && onTogglePaymentStatus != null) {
                    val isPaid = paymentStatus == "PAID"
                    val label = stringResource(if (isPaid) R.string.action_mark_unpaid else R.string.action_mark_paid)
                    val symbol = if (isPaid) HisabiSymbol.Clock else HisabiSymbol.Check
                    val badgeColor = if (isPaid) HighlighterYellow.copy(alpha = 0.55f) else HighlighterGreen.copy(alpha = 0.55f)
                    ActionSheetRuledItem(
                        label = label,
                        symbol = symbol,
                        badgeColor = badgeColor,
                        onClick = {
                            onDismiss()
                            onTogglePaymentStatus()
                        }
                    )
                }

                // Row: Date d'échéance & Rappel (if CREDIT)
                if (calcType == "CREDIT" && onSetDueDate != null) {
                    ActionSheetRuledItem(
                        label = stringResource(R.string.action_set_due_date),
                        symbol = HisabiSymbol.Clock,
                        badgeColor = Color(0xFFC2410C).copy(alpha = 0.22f),
                        onClick = {
                            onDismiss()
                            onSetDueDate()
                        }
                    )
                }

                // Row: Convert between Personnel and Crédit
                if (onToggleCalcType != null) {
                    val isCredit = calcType == "CREDIT"
                    val label = stringResource(if (isCredit) R.string.action_convert_to_personnel else R.string.action_convert_to_credit)
                    val symbol = if (isCredit) HisabiSymbol.Pencil else HisabiSymbol.Folder
                    val badgeColor = if (isCredit) HighlighterBlue.copy(alpha = 0.50f) else Color(0xFFC2410C).copy(alpha = 0.20f)
                    ActionSheetRuledItem(
                        label = label,
                        symbol = symbol,
                        badgeColor = badgeColor,
                        onClick = {
                            onDismiss()
                            onToggleCalcType()
                        }
                    )
                }

                // Row 3: Modifier (Edit)
                ActionSheetRuledItem(
                    label = stringResource(R.string.action_edit),
                    symbol = HisabiSymbol.Pencil,
                    badgeColor = HighlighterPink.copy(alpha = 0.40f),
                    onClick = {
                        onDismiss()
                        onEdit()
                    }
                )

                // Row 4: Dupliquer (Duplicate)
                ActionSheetRuledItem(
                    label = stringResource(R.string.action_duplicate),
                    symbol = HisabiSymbol.Copy,
                    badgeColor = HighlighterYellow.copy(alpha = 0.55f),
                    onClick = {
                        onDismiss()
                        onDuplicate()
                    }
                )

                // Row: Enregistrer comme modèle (Save as Template)
                if (onSaveAsTemplate != null) {
                    ActionSheetRuledItem(
                        label = stringResource(R.string.action_save_as_template),
                        symbol = HisabiSymbol.Page,
                        badgeColor = HighlighterPink.copy(alpha = 0.50f),
                        onClick = {
                            onDismiss()
                            onSaveAsTemplate()
                        }
                    )
                }

                // Row: Partager en image (Share as Long Image)
                if (onShareImage != null) {
                    ActionSheetRuledItem(
                        label = stringResource(R.string.action_share_image),
                        symbol = HisabiSymbol.Share,
                        badgeColor = HighlighterGreen.copy(alpha = 0.55f),
                        onClick = {
                            onDismiss()
                            onShareImage()
                        }
                    )
                }

                // Row: Exporter en PDF
                if (onExportPdf != null) {
                    ActionSheetRuledItem(
                        label = stringResource(R.string.action_export_pdf),
                        symbol = HisabiSymbol.Page,
                        badgeColor = HighlighterBlue.copy(alpha = 0.50f),
                        onClick = {
                            onDismiss()
                            onExportPdf()
                        }
                    )
                }

                // Row: Exporter en Excel (.csv)
                if (onExportExcel != null) {
                    ActionSheetRuledItem(
                        label = stringResource(R.string.export_as_excel),
                        symbol = HisabiSymbol.Table,
                        badgeColor = HighlighterYellow.copy(alpha = 0.55f),
                        onClick = {
                            onDismiss()
                            onExportExcel()
                        }
                    )
                }

                // Row: Ajouter / Déplacer vers un groupe
                if (onAssignToGroup != null) {
                    ActionSheetRuledItem(
                        label = stringResource(R.string.action_add_to_group),
                        symbol = HisabiSymbol.Folder,
                        badgeColor = HighlighterBlue.copy(alpha = 0.55f),
                        onClick = {
                            onDismiss()
                            onAssignToGroup()
                        }
                    )
                }

                // Row 5: Supprimer (Delete - Destructive)
                ActionSheetRuledItem(
                    label = stringResource(R.string.action_delete),
                    symbol = HisabiSymbol.Trash,
                    badgeColor = JournalActionDelete.copy(alpha = 0.15f),
                    isDestructive = true,
                    onClick = {
                        onDismiss()
                        onDelete()
                    }
                )

                // Row 6: Empty notebook ruled line for authentic bottom margin
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ActionSheetRuleSpacing)
                        .drawBehind {
                            val strokeW = 0.6.dp.toPx()
                            val y = size.height
                            drawLine(
                                color = JournalRule.copy(alpha = 0.55f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeW
                            )
                        }
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

/**
 * An action row inside the bottom sheet sitting directly on 1 notebook ruled line.
 * Features a colored sketch icon badge on the start and handwritten label sitting directly on the line.
 */
@Composable
private fun ActionSheetRuledItem(
    label: String,
    symbol: HisabiSymbol,
    badgeColor: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val tintColor = if (isDestructive) JournalActionDelete else JournalInk

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionSheetRuleSpacing)
            .drawBehind {
                val strokeW = 0.6.dp.toPx()
                val y = size.height
                drawLine(
                    color = JournalRule.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            HisabiSketchIcon(
                symbol = symbol,
                contentDescription = null,
                tint = tintColor,
                size = 16.dp
            )
        }

        Text(
            text = label,
            fontFamily = resolveJournalFont(label, isRtl),
            fontSize = if (isRtl) 14.5.sp else 15.sp,
            fontWeight = FontWeight.Normal,
            color = tintColor,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.journalBaselineOnRule(lineHeight = ActionSheetRuleSpacing)
        )
    }
}

/**
 * Ruled Notebook Action Sheet for Checklists and Notes.
 * Displays Open, Share, and Delete options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookActivityActionsSheet(
    title: String,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onAssignToGroup: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Row 1: Header - Title + Close button sitting on ruled line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ActionSheetRuleSpacing)
                    .drawBehind {
                        val strokeW = 1.0.dp.toPx()
                        val y = size.height
                        drawLine(
                            color = JournalRule.copy(alpha = 0.65f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW
                        )
                    }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.ifBlank { "—" },
                    fontFamily = resolveJournalFont(title, isRtl),
                    fontSize = if (isArabicScript(title) || isRtl) 16.sp else 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalWritingInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .journalBaselineOnRule(lineHeight = ActionSheetRuleSpacing)
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .offset(y = (-7).dp),
                    contentAlignment = Alignment.Center
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Close,
                        contentDescription = null,
                        tint = JournalMutedInk,
                        size = 14.dp
                    )
                }
            }

            // Row 2: Action Ouvrir
            ActionSheetRuledItem(
                label = stringResource(R.string.action_edit),
                symbol = HisabiSymbol.Pencil,
                badgeColor = Color(0xFFE0F2FE),
                onClick = {
                    onDismiss()
                    onOpen()
                }
            )

            // Row 3: Action Partager
            ActionSheetRuledItem(
                label = stringResource(R.string.action_share_image),
                symbol = HisabiSymbol.Share,
                badgeColor = Color(0xFFFEF3C7),
                onClick = {
                    onDismiss()
                    onShare()
                }
            )

            // Row 4: Action Ajouter à un groupe
            if (onAssignToGroup != null) {
                ActionSheetRuledItem(
                    label = stringResource(R.string.action_add_to_group),
                    symbol = HisabiSymbol.Folder,
                    badgeColor = HighlighterBlue.copy(alpha = 0.55f),
                    onClick = {
                        onDismiss()
                        onAssignToGroup()
                    }
                )
            }

            // Row 5: Action Supprimer (Destructive)
            ActionSheetRuledItem(
                label = stringResource(R.string.action_delete),
                symbol = HisabiSymbol.Trash,
                badgeColor = Color(0xFFFEE2E2),
                isDestructive = true,
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * Bottom Sheet displaying upcoming and active credit reminders and due dates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookRemindersSheet(
    reminders: List<CalculationWithItems>,
    onOpenCalculation: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val context = LocalContext.current
    val dateFormat = remember(context) {
        val locale = context.resources.configuration.locales[0]
        SimpleDateFormat("d MMM", locale)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Row 1: Header - Blue highlighted title + Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ActionSheetRuleSpacing)
                    .drawBehind {
                        val strokeW = 1.0.dp.toPx()
                        val y = size.height
                        drawLine(
                            color = JournalRule.copy(alpha = 0.65f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW
                        )
                    }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val sheetTitle = stringResource(R.string.reminders_sheet_title)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                            .journalVisualOnRule(gapAboveRule = 5.dp)
                    )
                    JournalBaselineHighlightedText(
                        text = sheetTitle,
                        style = TextStyle(
                            fontFamily = resolveJournalFont(sheetTitle, isRtl),
                            fontSize = if (isArabicScript(sheetTitle) || isRtl) 16.sp else 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk,
                            platformStyle = NoFontPadding
                        ),
                        highlighterColor = Color(0xFF3B82F6),
                        highlighterAlpha = 0.22f,
                        horizontalPadding = 6.dp,
                        verticalPadding = 0.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .journalVisualOnRule(gapAboveRule = 4.dp)
                        .size(24.dp)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 15.sp,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }

            if (reminders.isEmpty()) {
                // Empty state row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ActionSheetRuleSpacing * 2)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.reminders_empty),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    items(reminders, key = { it.calculation.id }) { calc ->
                        val calcTitle = calc.calculation.title.ifBlank { stringResource(R.string.editor_new_title) }
                        val dueDateStr = calc.calculation.dueDateEpochMs?.let { dateFormat.format(Date(it)) }
                        val totalCentimes = calc.totalCentimes
                        val totalFormatted = if (totalCentimes % 100 == 0L) {
                            "${totalCentimes / 100} ${calc.calculation.currency}"
                        } else {
                            String.format(java.util.Locale.US, "%.2f %s", totalCentimes / 100.0, calc.calculation.currency)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ActionSheetRuleSpacing)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        onDismiss()
                                        onOpenCalculation(calc.calculation.id)
                                    }
                                )
                                .drawBehind {
                                    val strokeW = 0.8.dp.toPx()
                                    val y = size.height
                                    drawLine(
                                        color = JournalRule.copy(alpha = 0.40f),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeW
                                    )
                                }
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF3B82F6), CircleShape)
                                )
                                Text(
                                    text = calcTitle,
                                    fontFamily = resolveJournalFont(calcTitle, isRtl),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = JournalWritingInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                                if (dueDateStr != null) {
                                    Text(
                                        text = "($dueDateStr)",
                                        fontFamily = PatrickHandFamily,
                                        fontSize = 13.sp,
                                        color = JournalMutedInk,
                                        style = TextStyle(platformStyle = NoFontPadding)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (calc.calculation.reminderEnabled) {
                                    Text(
                                        text = "🔔",
                                        fontSize = 12.sp,
                                        style = TextStyle(platformStyle = NoFontPadding)
                                    )
                                }
                                Text(
                                    text = totalFormatted,
                                    fontFamily = PatrickHandFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = JournalWritingInk,
                                    style = TextStyle(platformStyle = NoFontPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


