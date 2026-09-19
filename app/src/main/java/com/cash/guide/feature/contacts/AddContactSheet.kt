package com.cash.guide.feature.contacts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.ContactEntity
import com.cash.guide.ui.notebook.*
import com.cash.guide.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactSheet(
    editingContact: ContactEntity?,
    groups: List<CalculationGroupEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        phoneNumber: String,
        secondaryPhone: String?,
        note: String?,
        groupId: String?,
        colorTag: String
    ) -> Unit,
    onCreateGroup: (name: String, colorHex: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(editingContact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(editingContact?.phoneNumber ?: "") }
    var secondaryPhone by remember { mutableStateOf(editingContact?.secondaryPhone ?: "") }
    var note by remember { mutableStateOf(editingContact?.note ?: "") }
    var selectedGroupId by remember { mutableStateOf(editingContact?.groupId) }
    var selectedColorTag by remember { mutableStateOf(editingContact?.colorTag ?: "BLUE") }

    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    val phonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                try {
                    context.contentResolver.query(
                        contactUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIdx >= 0) {
                                val pickedName = cursor.getString(nameIdx)
                                if (!pickedName.isNullOrBlank()) name = pickedName
                            }
                            if (numberIdx >= 0) {
                                val pickedNumber = cursor.getString(numberIdx)
                                if (!pickedNumber.isNullOrBlank()) phoneNumber = pickedNumber
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JournalPaper,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(JournalInk.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingContact != null) stringResource(R.string.contact_edit_title) else stringResource(R.string.contact_new_title),
                    fontFamily = PatrickHandFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )

                // Import from Phone Button
                Surface(
                    onClick = {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        phonePickerLauncher.launch(intent)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = HighlighterYellow.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JournalRule.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Contacts,
                            contentDescription = null,
                            tint = JournalInk,
                            size = 14.dp
                        )
                        Text(
                            text = stringResource(R.string.contact_import_phone),
                            fontFamily = PatrickHandFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk
                        )
                    }
                }
            }

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.contact_name_hint), fontFamily = PatrickHandFamily) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JournalInk,
                    unfocusedBorderColor = JournalRule,
                    focusedTextColor = JournalInk,
                    unfocusedTextColor = JournalInk
                )
            )

            // Phone Number Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(stringResource(R.string.contact_phone_hint), fontFamily = PatrickHandFamily) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E40AF),
                    unfocusedBorderColor = JournalRule,
                    focusedTextColor = Color(0xFF1E40AF),
                    unfocusedTextColor = Color(0xFF1E40AF)
                )
            )

            // Secondary Phone Number (Optional)
            OutlinedTextField(
                value = secondaryPhone,
                onValueChange = { secondaryPhone = it },
                label = { Text(stringResource(R.string.contact_secondary_phone_hint), fontFamily = PatrickHandFamily) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JournalInk,
                    unfocusedBorderColor = JournalRule,
                    focusedTextColor = JournalInk,
                    unfocusedTextColor = JournalInk
                )
            )

            // Note / Specialty (ex: Plombier, Maarif)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.contact_note_hint), fontFamily = PatrickHandFamily) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JournalInk,
                    unfocusedBorderColor = JournalRule,
                    focusedTextColor = JournalInk,
                    unfocusedTextColor = JournalInk
                )
            )

            // Color Tags
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.contact_color_tag_title),
                    fontFamily = PatrickHandFamily,
                    fontSize = 14.sp,
                    color = JournalMutedInk
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colorOptions = listOf(
                        "BLUE" to Color(0xFF3B82F6),
                        "GREEN" to Color(0xFF10B981),
                        "YELLOW" to Color(0xFFF59E0B),
                        "PINK" to Color(0xFFEC4899),
                        "PURPLE" to Color(0xFF8B5CF6)
                    )
                    colorOptions.forEach { (tag, color) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorTag == tag) 2.5.dp else 0.dp,
                                    color = if (selectedColorTag == tag) JournalInk else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorTag = tag }
                        )
                    }
                }
            }

            // Group Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.contact_group_title),
                        fontFamily = PatrickHandFamily,
                        fontSize = 14.sp,
                        color = JournalMutedInk
                    )
                    Text(
                        text = stringResource(R.string.contact_new_group_btn),
                        fontFamily = PatrickHandFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable { showNewGroupDialog = true }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Sans groupe"
                    FilterChip(
                        selected = selectedGroupId == null,
                        onClick = { selectedGroupId = null },
                        label = { Text(stringResource(R.string.contact_no_group), fontFamily = PatrickHandFamily, maxLines = 1) }
                    )

                    groups.forEach { group ->
                        FilterChip(
                            selected = selectedGroupId == group.id,
                            onClick = { selectedGroupId = group.id },
                            label = { Text(group.name, fontFamily = PatrickHandFamily, maxLines = 1) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.common_cancel), fontFamily = PatrickHandFamily, fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                            onSave(
                                editingContact?.id,
                                name,
                                phoneNumber,
                                secondaryPhone,
                                note,
                                selectedGroupId,
                                selectedColorTag
                            )
                        }
                    },
                    enabled = name.isNotBlank() && phoneNumber.isNotBlank(),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JournalInk,
                        contentColor = JournalPaper
                    )
                ) {
                    Text(stringResource(R.string.common_save), fontFamily = PatrickHandFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // New Group Dialog
    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text(stringResource(R.string.contact_create_group_dialog_title), fontFamily = PatrickHandFamily) },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text(stringResource(R.string.contact_group_name_hint), fontFamily = PatrickHandFamily) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onCreateGroup(newGroupName, "#89B5D8")
                            newGroupName = ""
                            showNewGroupDialog = false
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text(stringResource(R.string.common_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
