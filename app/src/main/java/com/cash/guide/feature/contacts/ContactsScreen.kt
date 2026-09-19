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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
            Text(
                text = stringResource(R.string.contacts_screen_title),
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 18.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = JournalInk,
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )

            // Button: + Nouveau contact with text and + sitting on the blue rule
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button) { viewModel.openAddContactSheet() }
                    .drawBehind {
                        val washHeight = 24.dp.toPx()
                        val washY = size.height - washHeight + 5.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFFEDE9FE).copy(alpha = 0.80f),
                            topLeft = Offset(-6.dp.toPx(), washY),
                            size = Size(size.width + 12.dp.toPx(), washHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                    .padding(horizontal = 6.dp)
            ) {
                HisabiSketchIcon(
                    symbol = HisabiSymbol.Plus,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    size = 13.dp,
                    modifier = Modifier.offset(y = 2.dp)
                )
                Text(
                    text = stringResource(R.string.contact_btn_new),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 13.5.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }
        }

        // Line 2: 1 rule spacer (نقز السطر - tna9ez star)
        Spacer(modifier = Modifier.height(JournalRuleSpacing))

        // Line 3: Ruled Search Row (Exact match to Home page JournalInlineSearchRow with grey capsule)
        JournalInlineSearchRow(
            query = state.searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            placeholder = stringResource(R.string.contacts_search_hint)
        )

        // Line 4: Group Filter Chips Row sitting on the blue rule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 2.5.dp)
            ) {
                // "Tous"
                item {
                    val isSelected = state.selectedGroupId == null
                    Surface(
                        onClick = { viewModel.selectGroup(null) },
                        shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
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
                val emptyText = when {
                    state.searchQuery.isNotBlank() -> stringResource(R.string.contacts_empty_search)
                    state.selectedGroupId != null -> stringResource(R.string.group_detail_empty_contacts)
                    else -> stringResource(R.string.contacts_empty_list)
                }
                Text(
                    text = emptyText,
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
                    onSms = { ContactActionHelper.sendSms(context, contact.phoneNumber) },
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

private fun formatDisplayPhone(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    val startsWithPlus = rawPhone.trim().startsWith("+")
    return when {
        digits.length == 10 && digits.startsWith("0") -> {
            "${digits.substring(0, 2)} ${digits.substring(2, 4)} ${digits.substring(4, 6)} ${digits.substring(6, 8)} ${digits.substring(8, 10)}"
        }
        digits.length == 12 && digits.startsWith("212") -> {
            val prefix = if (startsWithPlus) "+212" else "212"
            "$prefix ${digits.substring(3, 5)} ${digits.substring(5, 7)} ${digits.substring(7, 9)} ${digits.substring(9, 11)}"
        }
        digits.length == 9 -> {
            "0${digits.substring(0, 1)} ${digits.substring(1, 3)} ${digits.substring(3, 5)} ${digits.substring(5, 7)} ${digits.substring(7, 9)}"
        }
        else -> rawPhone
    }
}

@Composable
fun ContactItemRow(
    contact: ContactEntity,
    isRtl: Boolean,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
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

    val displayPhone = remember(contact.phoneNumber) {
        formatDisplayPhone(contact.phoneNumber)
    }

    // 2 Ruled Notebook Lines = JournalRuleSpacing * 2
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2)
    ) {
        // Line 1: [Pastille + (Pin) + Name] -------- [Phone Number] [3-dots Menu]
        // Note: No clickable on this row! Calling only happens via the call icon.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start: Color Pastille + Pin (if pinned) + Name
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(2.5f, fill = false)
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
                    fontSize = if (isRtl) 15.sp else 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Connecting dotted line on the blue notebook rule spanning up to the phone number
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 4.dp)
                    .drawBehind {
                        val strokeW = 0.85.dp.toPx()
                        val y = size.height
                        drawLine(
                            color = JournalWritingInk.copy(alpha = 0.32f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeW,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()))
                        )
                    }
            )

            // End: Phone number in bold navy ink + 3-dots Menu
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayPhone,
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D28D9),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                // 3-dots Menu button on Line 1 sitting right on the blue rule
                Box(
                    modifier = Modifier
                        .offset(y = 4.5.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { showMenu = true }
                        .padding(2.dp)
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.More,
                        contentDescription = "Options",
                        tint = JournalMutedInk.copy(alpha = 0.85f),
                        size = 18.dp
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

        // Line 2: Exactly on the blue ruled line below!
        // Center: SMS, WhatsApp, Call (closer together, and lower SMS and Call to sit on the blue line)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // SMS Icon (Amber / Orange message bubble, pushed down 2.2dp to touch the blue line)
                Box(
                    modifier = Modifier
                        .offset(y = 2.2.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onSms)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Sms,
                        contentDescription = "SMS",
                        tint = Color(0xFFEA580C),
                        size = 21.dp
                    )
                }

                // WhatsApp Icon (Green WhatsApp bubble, sits on line)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onWhatsApp)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.WhatsApp,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF15803D),
                        size = 21.dp
                    )
                }

                // Direct Call Icon (Phone Call Green, pushed down 2.2dp to touch the blue line)
                Box(
                    modifier = Modifier
                        .offset(y = 2.2.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onCall)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Phone,
                        contentDescription = "Appel",
                        tint = Color(0xFF16A34A),
                        size = 21.dp
                    )
                }
            }
        }
    }
}
