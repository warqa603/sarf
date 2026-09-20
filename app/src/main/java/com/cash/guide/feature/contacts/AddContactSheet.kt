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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.ContactEntity
import com.cash.guide.ui.notebook.*
import com.cash.guide.ui.theme.*

private val ContactBrandPink = Color(0xFFBE185D)

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
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val font = if (isRtl) TajawalFamily else PatrickHandFamily

    var name by remember { mutableStateOf(editingContact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(editingContact?.phoneNumber ?: "") }
    var secondaryPhone by remember { mutableStateOf(editingContact?.secondaryPhone ?: "") }
    var note by remember { mutableStateOf(editingContact?.note ?: "") }
    var selectedGroupId by remember { mutableStateOf(editingContact?.groupId) }
    var selectedColorTag by remember { mutableStateOf(editingContact?.colorTag ?: "PINK") }

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
                    .width(42.dp)
                    .height(4.5.dp)
                    .background(JournalInk.copy(alpha = 0.20f), RoundedCornerShape(2.dp))
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
            // Header Row: Icon + Title & Subtitle on Start, Import Button on End
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HighlighterPink.copy(alpha = 0.40f)),
                        contentAlignment = Alignment.Center
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Contacts,
                            contentDescription = null,
                            tint = ContactBrandPink,
                            size = 20.dp
                        )
                    }

                    Column {
                        val titleText = if (editingContact != null) {
                            stringResource(R.string.contact_edit_title)
                        } else {
                            stringResource(R.string.contact_new_title)
                        }
                        Text(
                            text = titleText,
                            fontFamily = font,
                            fontSize = if (isRtl) 19.sp else 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        Text(
                            text = if (editingContact != null) "Coordonnées & détails" else "Nouveau contact dans le carnet",
                            fontFamily = font,
                            fontSize = 12.sp,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }

                // Import from Phone Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(HighlighterYellow.copy(alpha = 0.50f))
                        .border(1.dp, JournalInk.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) {
                            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            phonePickerLauncher.launch(intent)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Contacts,
                            contentDescription = null,
                            tint = JournalInk,
                            size = 13.dp
                        )
                        Text(
                            text = stringResource(R.string.contact_import_phone),
                            fontFamily = font,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 1. Nom complet ou surnom (Required)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(
                        text = stringResource(R.string.contact_name_hint),
                        fontFamily = font,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Contacts,
                        contentDescription = null,
                        tint = if (name.isNotBlank()) ContactBrandPink else JournalMutedInk.copy(alpha = 0.6f),
                        size = 18.dp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.85f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.45f),
                    focusedBorderColor = ContactBrandPink,
                    unfocusedBorderColor = JournalRule.copy(alpha = 0.75f),
                    focusedLabelColor = ContactBrandPink,
                    unfocusedLabelColor = JournalMutedInk,
                    focusedTextColor = JournalWritingInk,
                    unfocusedTextColor = JournalWritingInk,
                    cursorColor = ContactBrandPink
                ),
                textStyle = TextStyle(
                    fontFamily = font,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    platformStyle = NoFontPadding
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Numéro de téléphone (Required)
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = {
                    Text(
                        text = stringResource(R.string.contact_phone_hint),
                        fontFamily = font,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Phone,
                        contentDescription = null,
                        tint = if (phoneNumber.isNotBlank()) ContactBrandPink else JournalMutedInk.copy(alpha = 0.6f),
                        size = 18.dp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.85f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.45f),
                    focusedBorderColor = ContactBrandPink,
                    unfocusedBorderColor = JournalRule.copy(alpha = 0.75f),
                    focusedLabelColor = ContactBrandPink,
                    unfocusedLabelColor = JournalMutedInk,
                    focusedTextColor = ContactBrandPink,
                    unfocusedTextColor = ContactBrandPink,
                    cursorColor = ContactBrandPink
                ),
                textStyle = TextStyle(
                    fontFamily = PatrickHandFamily,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    platformStyle = NoFontPadding
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Numéro secondaire (Optionnel)
            OutlinedTextField(
                value = secondaryPhone,
                onValueChange = { secondaryPhone = it },
                label = {
                    Text(
                        text = stringResource(R.string.contact_secondary_phone_hint),
                        fontFamily = font,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Phone,
                        contentDescription = null,
                        tint = JournalMutedInk.copy(alpha = 0.6f),
                        size = 17.dp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.85f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.45f),
                    focusedBorderColor = ContactBrandPink,
                    unfocusedBorderColor = JournalRule.copy(alpha = 0.75f),
                    focusedLabelColor = ContactBrandPink,
                    unfocusedLabelColor = JournalMutedInk,
                    focusedTextColor = JournalWritingInk,
                    unfocusedTextColor = JournalWritingInk,
                    cursorColor = ContactBrandPink
                ),
                textStyle = TextStyle(
                    fontFamily = PatrickHandFamily,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = NoFontPadding
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Note ou métier (ex: Plombier, Maarif)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text(
                        text = stringResource(R.string.contact_note_hint),
                        fontFamily = font,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    HisabiSketchIcon(
                        symbol = HisabiSymbol.Pencil,
                        contentDescription = null,
                        tint = JournalMutedInk.copy(alpha = 0.6f),
                        size = 17.dp
                    )
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.85f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.45f),
                    focusedBorderColor = ContactBrandPink,
                    unfocusedBorderColor = JournalRule.copy(alpha = 0.75f),
                    focusedLabelColor = ContactBrandPink,
                    unfocusedLabelColor = JournalMutedInk,
                    focusedTextColor = JournalWritingInk,
                    unfocusedTextColor = JournalWritingInk,
                    cursorColor = ContactBrandPink
                ),
                textStyle = TextStyle(
                    fontFamily = font,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = NoFontPadding
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 5. Couleur de la pastille
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.contact_color_tag_title),
                    fontFamily = font,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalMutedInk,
                    style = TextStyle(platformStyle = NoFontPadding)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colorOptions = listOf(
                        "PINK" to ContactBrandPink,
                        "BLUE" to Color(0xFF0EA5E9),
                        "GREEN" to Color(0xFF16A34A),
                        "YELLOW" to Color(0xFFF59E0B),
                        "PURPLE" to Color(0xFF8B5CF6)
                    )
                    colorOptions.forEach { (tag, color) ->
                        val isSelected = selectedColorTag == tag
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, JournalInk, CircleShape)
                                    } else {
                                        Modifier.border(0.5.dp, JournalInk.copy(alpha = 0.15f), CircleShape)
                                    }
                                )
                                .clickable(role = Role.RadioButton) { selectedColorTag = tag },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                HisabiSketchIcon(
                                    symbol = HisabiSymbol.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    size = 16.dp
                                )
                            }
                        }
                    }
                }
            }

            // 6. Choix du groupe (avec puces tactiles du carnet)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.contact_group_title),
                        fontFamily = font,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                    Text(
                        text = stringResource(R.string.contact_new_group_btn),
                        fontFamily = font,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContactBrandPink,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier
                            .clickable(role = Role.Button) { showNewGroupDialog = true }
                            .padding(4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Sans groupe"
                    val isNoneSelected = selectedGroupId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isNoneSelected) HighlighterYellow.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.60f)
                            )
                            .then(
                                if (isNoneSelected) Modifier.border(1.2.dp, JournalInk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                else Modifier.border(0.8.dp, JournalRule.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                            )
                            .clickable(role = Role.RadioButton) { selectedGroupId = null }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.contact_no_group),
                            fontFamily = font,
                            fontSize = 13.5.sp,
                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isNoneSelected) JournalInk else JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }

                    groups.forEach { group ->
                        val isGroupSelected = selectedGroupId == group.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isGroupSelected) HighlighterYellow.copy(alpha = 0.55f)
                                    else Color.White.copy(alpha = 0.60f)
                                )
                                .then(
                                    if (isGroupSelected) Modifier.border(1.2.dp, JournalInk.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    else Modifier.border(0.8.dp, JournalRule.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                                )
                                .clickable(role = Role.RadioButton) { selectedGroupId = group.id }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = group.name,
                                fontFamily = font,
                                fontSize = 13.5.sp,
                                fontWeight = if (isGroupSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGroupSelected) JournalInk else JournalMutedInk,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Action Buttons (Annuler & Enregistrer)
            val canSave = name.isNotBlank() && phoneNumber.isNotBlank()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Annuler
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.65f))
                        .border(1.dp, JournalRule.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        fontFamily = font,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = JournalMutedInk,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }

                // Enregistrer (Vibrant Contact Brand Pink, with checkmark, NEVER pitch black!)
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (canSave) ContactBrandPink
                            else ContactBrandPink.copy(alpha = 0.35f)
                        )
                        .clickable(
                            enabled = canSave,
                            role = Role.Button,
                            onClick = {
                                if (canSave) {
                                    onSave(
                                        editingContact?.id,
                                        name.trim(),
                                        phoneNumber.trim(),
                                        secondaryPhone.trim().ifBlank { null },
                                        note.trim().ifBlank { null },
                                        selectedGroupId,
                                        selectedColorTag
                                    )
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        HisabiSketchIcon(
                            symbol = HisabiSymbol.Check,
                            contentDescription = null,
                            tint = if (canSave) Color.White else Color.White.copy(alpha = 0.70f),
                            size = 17.dp
                        )
                        Text(
                            text = stringResource(R.string.common_save),
                            fontFamily = font,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canSave) Color.White else Color.White.copy(alpha = 0.70f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }
        }
    }

    // New Group Dialog
    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            containerColor = JournalPaper,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = stringResource(R.string.contact_create_group_dialog_title),
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk
                )
            },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text(stringResource(R.string.contact_group_name_hint), fontFamily = font) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ContactBrandPink,
                        unfocusedBorderColor = JournalRule,
                        focusedTextColor = JournalWritingInk,
                        unfocusedTextColor = JournalWritingInk
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            onCreateGroup(newGroupName, "#89B5D8")
                            newGroupName = ""
                            showNewGroupDialog = false
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.common_create),
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = if (newGroupName.isNotBlank()) ContactBrandPink else JournalMutedInk
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        fontFamily = font,
                        color = JournalMutedInk
                    )
                }
            }
        )
    }
}

