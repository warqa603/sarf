package com.cash.guide.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.CalculationRepository
import com.cash.guide.domain.GroupCategory
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.HisabiSketchIcon
import com.cash.guide.ui.notebook.HisabiSymbol
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalRule
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.isArabicScript
import com.cash.guide.ui.notebook.resolveJournalFont
import kotlinx.coroutines.launch

@Composable
fun AssignToGroupDialog(
    category: GroupCategory,
    currentGroupId: String?,
    calculationRepository: CalculationRepository,
    onDismiss: () -> Unit,
    onAssignGroup: (String?) -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val allGroups by calculationRepository.observeAllGroups().collectAsState(initial = emptyList())
    val groups = remember(allGroups, category) {
        allGroups.filter { it.category == category.name }
    }

    val categoryBadgeColor = when (category) {
        GroupCategory.CALCULATIONS -> HighlighterPink.copy(alpha = 0.50f)
        GroupCategory.NOTES -> HighlighterYellow.copy(alpha = 0.55f)
        GroupCategory.CHECKLISTS -> HighlighterGreen.copy(alpha = 0.50f)
        GroupCategory.CONTACTS -> HighlighterBlue.copy(alpha = 0.50f)
    }

    val categorySymbol = when (category) {
        GroupCategory.CALCULATIONS -> HisabiSymbol.Calculator
        GroupCategory.NOTES -> HisabiSymbol.Page
        GroupCategory.CHECKLISTS -> HisabiSymbol.Check
        GroupCategory.CONTACTS -> HisabiSymbol.Contacts
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        title = {
            val titleText = stringResource(R.string.group_assign_dialog_title)
            Text(
                text = titleText,
                fontFamily = resolveJournalFont(titleText, isRtl),
                fontSize = if (isRtl) 16.5.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                color = JournalInk
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    // Option 1: None (ungrouped / retirer du groupe)
                    item {
                        val noneText = stringResource(R.string.group_none)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) {
                                    onAssignGroup(null)
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(JournalRule.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                HisabiSketchIcon(
                                    symbol = HisabiSymbol.Close,
                                    contentDescription = null,
                                    tint = JournalMutedInk,
                                    size = 14.dp
                                )
                            }
                            Text(
                                text = noneText,
                                fontFamily = resolveJournalFont(noneText, isRtl),
                                fontSize = if (isRtl) 14.5.sp else 15.sp,
                                color = if (currentGroupId == null) JournalInk else JournalMutedInk,
                                fontWeight = if (currentGroupId == null) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // Existing groups for this category
                    if (groups.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.groups_empty_title),
                                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                    fontSize = 14.sp,
                                    color = JournalMutedInk
                                )
                            }
                        }
                    } else {
                        items(groups, key = { it.id }) { group ->
                            val isCurrent = group.id == currentGroupId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button) {
                                        onAssignGroup(group.id)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(categoryBadgeColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    HisabiSketchIcon(
                                        symbol = categorySymbol,
                                        contentDescription = null,
                                        tint = JournalInk,
                                        size = 17.5.dp
                                    )
                                }
                                Text(
                                    text = group.name,
                                    fontFamily = resolveJournalFont(group.name, isRtl),
                                    fontSize = if (isArabicScript(group.name) || isRtl) 14.5.sp else 15.sp,
                                    color = JournalInk,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent) {
                                    HisabiSketchIcon(
                                        symbol = HisabiSymbol.Check,
                                        contentDescription = null,
                                        tint = JournalInk,
                                        size = 14.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            val cancelText = stringResource(R.string.group_dialog_cancel)
            TextButton(onClick = onDismiss) {
                Text(
                    text = cancelText,
                    fontFamily = resolveJournalFont(cancelText, isRtl),
                    fontSize = if (isRtl) 13.5.sp else 14.sp,
                    color = JournalMutedInk
                )
            }
        }
    )
}

/**
 * Backward compatibility overload specifically for Calculations.
 */
@Composable
fun AssignToGroupDialog(
    calculationId: String,
    currentGroupId: String?,
    calculationRepository: CalculationRepository,
    onDismiss: () -> Unit,
    onAssigned: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    AssignToGroupDialog(
        category = GroupCategory.CALCULATIONS,
        currentGroupId = currentGroupId,
        calculationRepository = calculationRepository,
        onDismiss = onDismiss,
        onAssignGroup = { groupId ->
            coroutineScope.launch {
                calculationRepository.assignCalculationToGroup(calculationId, groupId)
                onAssigned()
            }
        }
    )
}
