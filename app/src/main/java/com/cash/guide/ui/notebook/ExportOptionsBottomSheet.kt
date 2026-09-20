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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

private val ExportSheetRuleSpacing: Dp = 44.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsBottomSheet(
    title: String,
    onSaveAsTemplate: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onSetDueDate: (() -> Unit)? = null,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onShareImage: (() -> Unit)? = null,
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
                // Header: Title sitting directly on ruled notebook line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ExportSheetRuleSpacing)
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
                    val resolvedTitle = title.ifBlank { stringResource(R.string.export_options_title) }
                    Text(
                        text = resolvedTitle,
                        fontFamily = resolveJournalFont(resolvedTitle, isRtl),
                        fontSize = if (isArabicScript(resolvedTitle) || isRtl) 16.sp else 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier
                            .fillMaxWidth()
                            .journalBaselineOnRule(lineHeight = ExportSheetRuleSpacing)
                    )
                }

                // Row: Enregistrer comme modèle
                if (onSaveAsTemplate != null) {
                    ExportSheetRuledItem(
                        label = stringResource(R.string.action_save_as_template),
                        symbol = HisabiSymbol.Page,
                        badgeColor = HighlighterPink.copy(alpha = 0.50f),
                        onClick = {
                            onDismiss()
                            onSaveAsTemplate()
                        }
                    )
                }

                // Row: Dupliquer
                if (onDuplicate != null) {
                    ExportSheetRuledItem(
                        label = stringResource(R.string.action_duplicate_editor),
                        symbol = HisabiSymbol.Copy,
                        badgeColor = HighlighterYellow.copy(alpha = 0.55f),
                        onClick = {
                            onDismiss()
                            onDuplicate()
                        }
                    )
                }

                // Row: Date d'échéance & Rappel
                if (onSetDueDate != null) {
                    ExportSheetRuledItem(
                        label = stringResource(R.string.action_set_due_date),
                        symbol = HisabiSymbol.Clock,
                        badgeColor = Color(0xFFC2410C).copy(alpha = 0.22f),
                        onClick = {
                            onDismiss()
                            onSetDueDate()
                        }
                    )
                }

                // Row: PDF Document
                ExportSheetRuledItem(
                    label = stringResource(R.string.export_as_pdf),
                    symbol = HisabiSymbol.Page,
                    badgeColor = HighlighterBlue.copy(alpha = 0.55f),
                    onClick = {
                        onDismiss()
                        onExportPdf()
                    }
                )

                // Row: Excel Spreadsheet
                ExportSheetRuledItem(
                    label = stringResource(R.string.export_as_excel),
                    symbol = HisabiSymbol.Table,
                    badgeColor = HighlighterGreen.copy(alpha = 0.55f),
                    onClick = {
                        onDismiss()
                        onExportExcel()
                    }
                )

                // Row: Image (if applicable)
                if (onShareImage != null) {
                    ExportSheetRuledItem(
                        label = stringResource(R.string.export_as_image),
                        symbol = HisabiSymbol.Share,
                        badgeColor = HighlighterYellow.copy(alpha = 0.55f),
                        onClick = {
                            onDismiss()
                            onShareImage()
                        }
                    )
                }

                // Empty notebook rule for authentic margin
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ExportSheetRuleSpacing)
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

@Composable
private fun ExportSheetRuledItem(
    label: String,
    symbol: HisabiSymbol,
    badgeColor: Color,
    onClick: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ExportSheetRuleSpacing)
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
                tint = JournalInk,
                size = 16.dp
            )
        }

        Text(
            text = label,
            fontFamily = resolveJournalFont(label, isRtl),
            fontSize = if (isRtl) 14.5.sp else 15.sp,
            fontWeight = FontWeight.Normal,
            color = JournalInk,
            style = TextStyle(platformStyle = NoFontPadding),
            modifier = Modifier.journalBaselineOnRule(lineHeight = ExportSheetRuleSpacing)
        )
    }
}
