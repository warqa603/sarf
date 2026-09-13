package com.cash.guide.ui.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(JournalPaper)
                .border(1.2.dp, JournalRule.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HighlighterYellow.copy(alpha = 0.35f))
                            .border(1.dp, JournalInk.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✏️", fontSize = 20.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.edit_profile_dialog_title),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 17.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                        Text(
                            text = stringResource(R.string.edit_profile_dialog_subtitle),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 12.sp else 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = JournalMutedInk.copy(alpha = 0.85f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }

                // Input Field Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(JournalInk.copy(alpha = 0.04f))
                        .border(1.dp, JournalRule.copy(alpha = 0.70f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (nameText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.edit_profile_placeholder),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 15.sp else 16.sp,
                            color = JournalMutedInk.copy(alpha = 0.50f),
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }

                    BasicTextField(
                        value = nameText,
                        onValueChange = { if (it.length <= 30) nameText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                end = if (!isRtl && nameText.isNotEmpty()) 36.dp else 0.dp,
                                start = if (isRtl && nameText.isNotEmpty()) 36.dp else 0.dp
                            )
                            .focusRequester(focusRequester),
                        singleLine = true,
                        cursorBrush = SolidColor(JournalInk),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onSave(nameText.trim())
                                onDismiss()
                            }
                        ),
                        textStyle = TextStyle(
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 16.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            platformStyle = NoFontPadding
                        )
                    )

                    if (nameText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(if (isRtl) Alignment.CenterStart else Alignment.CenterEnd)
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClick = { nameText = "" }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                fontFamily = PatrickHandFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalMutedInk
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Annuler
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(JournalInk.copy(alpha = 0.05f))
                            .border(1.dp, JournalRule.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.edit_profile_cancel),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 14.sp else 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = JournalMutedInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }

                    // Enregistrer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HighlighterGreen.copy(alpha = 0.35f))
                            .border(1.2.dp, JournalInk.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = {
                                    onSave(nameText.trim())
                                    onDismiss()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.edit_profile_save),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 15.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalInk,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }
            }
        }
    }
}
