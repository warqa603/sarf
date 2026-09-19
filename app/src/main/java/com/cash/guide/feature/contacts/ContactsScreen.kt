package com.cash.guide.feature.contacts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.db.ContactEntity
import com.cash.guide.domain.ContactActionHelper
import com.cash.guide.ui.notebook.*
import com.cash.guide.ui.theme.*

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    JournalRuledDocument(modifier = modifier.fillMaxSize(), clearFocusOnTap = true) {
        // Line 1: Header Band (Title + Count + New Contact Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.contacts_screen_title),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 17.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
                Text(
                    text = "",
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Button: Nouveau contact
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(HighlighterYellow.copy(alpha = 0.5f))
                    .clickable(role = Role.Button) { viewModel.openAddContactSheet() }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Plus,
                    contentDescription = null,
                    tint = JournalInk,
                    size = 13.dp
                )
                Text(
                    text = stringResource(R.string.contact_btn_new),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 13.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        }

        // Line 2: 1 rule spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 3: Ruled-Line Search Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HisabiSketchIcon(
                symbol = HisabiSymbol.Search,
                contentDescription = null,
                tint = JournalMutedInk.copy(alpha = 0.6f),
                size = 15.dp,
                modifier = Modifier.offset(y = (-4).dp)
            )

            BasicTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .journalBaselineOnRule(),
                textStyle = TextStyle(
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 15.sp else 16.sp,
                    color = JournalInk,
                    platformStyle = NoFontPadding
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (state.searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.contacts_search_hint),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 14.5.sp else 15.5.sp,
                            color = JournalMutedInk.copy(alpha = 0.5f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                    innerTextField()
                }
            )

            if (state.searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .clip(CircleShape)
                        .clickable { viewModel.setSearchQuery("") }
                        .padding(6.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Close,
                        contentDescription = "Effacer",
                        tint = JournalMutedInk,
                        size = 15.dp
                    )
                }
            }
        }

        // Line 4: Group Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Tous"
                item {
                    val isSelected = state.selectedGroupId == null
                    Surface(
                        onClick = { viewModel.selectGroup(null) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) JournalInk else JournalDockBg,
                        border = androidx.compose.foundation.BorderStroke(
                            0.75.dp,
                            if (isSelected) JournalInk else JournalRule.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.contacts_filter_all),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) JournalPaper else JournalInk,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                // "Sans groupe"
                item {
                    val isSelected = state.selectedGroupId == "UNGROUPED"
                    Surface(
                        onClick = { viewModel.selectGroup("UNGROUPED") },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) JournalInk else JournalDockBg,
                        border = androidx.compose.foundation.BorderStroke(
                            0.75.dp,
                            if (isSelected) JournalInk else JournalRule.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.contact_no_group),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) JournalPaper else JournalInk,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                // Groups list
                items(state.groups, key = { it.id }) { group ->
                    val isSelected = state.selectedGroupId == group.id
                    Surface(
                        onClick = { viewModel.selectGroup(group.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) JournalInk else JournalDockBg,
                        border = androidx.compose.foundation.BorderStroke(
                            0.75.dp,
                            if (isSelected) JournalInk else JournalRule.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = group.name,
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) JournalPaper else JournalInk,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Line 5: 1 rule spacer
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Contact Items List
        if (state.filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 3)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.searchQuery.isNotBlank()) stringResource(R.string.contacts_empty_search) else stringResource(R.string.contacts_empty_list),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = 15.sp,
                    color = JournalMutedInk.copy(alpha = 0.65f),
                    style = TextStyle(platformStyle = NoFontPadding)
                )
            }
        } else {
            state.filteredContacts.forEach { contact ->
                ContactItemRow(
                    contact = contact,
                    isRtl = isRtl,
                    onCall = { ContactActionHelper.dialPhone(context, contact.phoneNumber) },
                    onWhatsApp = { ContactActionHelper.openWhatsApp(context, contact.phoneNumber) },
                    onEdit = { viewModel.openAddContactSheet(contact) },
                    onTogglePin = { viewModel.togglePin(contact.id) },
                    onShare = { ContactActionHelper.shareContact(context, contact) },
                    onDelete = { viewModel.confirmDelete(contact) }
                )
            }
        }

        Spacer(modifier = Modifier.height(JournalRuleSpacing * 4))
    }

    // Add / Edit Sheet
    if (state.isAddSheetOpen) {
        AddContactSheet(
            editingContact = state.editingContact,
            groups = state.groups,
            onDismiss = { viewModel.closeAddContactSheet() },
            onSave = { id, name, phone, secPhone, note, groupId, colorTag ->
                viewModel.saveContact(id, name, phone, secPhone, note, groupId, colorTag)
            },
            onCreateGroup = { name, colorHex ->
                viewModel.createGroup(name, colorHex)
            }
        )
    }

    // Delete Confirmation Dialog
    state.contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text(stringResource(R.string.contact_delete_confirm_title), fontFamily = PatrickHandFamily) },
            text = { Text(stringResource(R.string.contact_delete_confirm_msg, contact.name), fontFamily = PatrickHandFamily) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteContact(contact.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
fun ContactItemRow(
    contact: ContactEntity,
    isRtl: Boolean,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromGroup: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    val dotColor = when (contact.colorTag) {
        "GREEN" -> Color(0xFF10B981)
        "YELLOW" -> Color(0xFFF59E0B)
        "PINK" -> Color(0xFFEC4899)
        "PURPLE" -> Color(0xFF8B5CF6)
        else -> Color(0xFF3B82F6)
    }

    val hasSubtitle = !contact.note.isNullOrBlank()
    val rowHeight = if (hasSubtitle) JournalRuleSpacing * 2 else JournalRuleSpacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable { onCall() }
    ) {
        // Main Row (on blue ruled line)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Bullet + Pin + Name
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1.65f, fill = false)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(6.5.dp)
                        .offset(y = (-5.5).dp)
                ) {
                    drawCircle(color = dotColor)
                }

                if (contact.isPinned) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Pin,
                        contentDescription = null,
                        tint = JournalInk.copy(alpha = 0.75f),
                        size = 12.dp,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }

                Text(
                    text = contact.name,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 14.5.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .journalBaselineOnRule()
                )
            }

            // Connecting dotted line on the blue notebook rule
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 4.dp)
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

            // End: Phone number in distinctive ink + Action buttons (WhatsApp, Call, More)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Distinctive Phone Number Ink (Rich Navy Ink)
                Text(
                    text = contact.phoneNumber,
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E40AF),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                // WhatsApp Quick Action
                Box(
                    modifier = Modifier
                        .offset(y = (-3).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f))
                        .clickable(role = Role.Button, onClick = onWhatsApp)
                        .padding(3.5.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.WhatsApp,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF128C7E),
                        size = 14.dp
                    )
                }

                // Direct Call Quick Action
                Box(
                    modifier = Modifier
                        .offset(y = (-3).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                        .clickable(role = Role.Button, onClick = onCall)
                        .padding(3.5.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Phone,
                        contentDescription = "Appel",
                        tint = Color(0xFF1D4ED8),
                        size = 14.dp
                    )
                }

                // 3-dots Menu
                Box(modifier = Modifier.offset(y = (-3).dp)) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = "Options",
                        tint = JournalMutedInk,
                        size = 15.dp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showMenu = true }
                            .padding(2.5.dp)
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (contact.isPinned) stringResource(R.string.action_unpin) else stringResource(R.string.action_pin)) },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_share)) },
                            onClick = {
                                showMenu = false
                                onShare()
                            }
                        )
                        if (onRemoveFromGroup != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_remove_from_group)) },
                                onClick = {
                                    showMenu = false
                                    onRemoveFromGroup()
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_delete), color = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        // Subtitle line (if note / métier exists)
        if (hasSubtitle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = contact.note ?: "",
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 13.sp else 13.5.sp,
                    color = JournalMutedInk.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }
        }
    }
}
