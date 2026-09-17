package com.cash.guide.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.border
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextOverflow
import com.cash.guide.ui.notebook.JournalThemeId
import com.cash.guide.ui.notebook.JournalThemePacks
import com.cash.guide.ui.notebook.JournalThemePalette
import com.cash.guide.ui.notebook.ExportOptionsBottomSheet
import com.cash.guide.ui.notebook.SetupPinDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow
import com.cash.guide.ui.notebook.JournalInk
import com.cash.guide.ui.notebook.JournalMutedInk
import com.cash.guide.ui.notebook.JournalWritingInk
import com.cash.guide.ui.notebook.JournalRuleSpacing
import com.cash.guide.ui.notebook.JournalRuledDocument
import com.cash.guide.ui.notebook.NoFontPadding
import com.cash.guide.ui.notebook.NotebookSectionBand
import com.cash.guide.ui.notebook.NotebookSegmentedControl
import com.cash.guide.ui.notebook.PatrickHandFamily
import com.cash.guide.ui.notebook.TajawalFamily
import com.cash.guide.ui.notebook.journalBaselineOnRule
import com.cash.guide.ui.notebook.journalVisualOnRule
import com.cash.guide.ui.notebook.NotebookMetrics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.cash.guide.ui.notebook.JournalUpcomingFeatureRow
import com.cash.guide.ui.notebook.journalBaselineOnRule

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    var showExportOptions by remember { mutableStateOf(false) }
    var showSetupPinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkBiometricAvailability(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(context, uri) { success ->
                val msg = if (success) {
                    context.getString(R.string.backup_toast_export_success)
                } else {
                    context.getString(R.string.backup_toast_error)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadBackupForInspection(context, uri) { success ->
                if (!success) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.backup_toast_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        JournalRuledDocument(modifier = Modifier.fillMaxSize()) {
            // Line 1: Header Band (Compact "Paramètres" / "الإعدادات" + Month Year sitting directly on ruled line 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 16.5.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )

                val context = LocalContext.current
                val currentMonthYear = remember(context) {
                    val locale = context.resources.configuration.locales[0]
                    val sdf = java.text.SimpleDateFormat("LLLL yyyy", locale)
                    val raw = sdf.format(java.util.Date())
                    raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                }

                Text(
                    text = currentMonthYear,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 13.5.sp else 14.sp,
                    fontWeight = FontWeight.Light,
                    color = JournalMutedInk.copy(alpha = 0.85f),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            // Line 2: 1 empty notebook line spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Section 1: Thème & Style du carnet (soft pink band)
            SettingsSectionBadge(
                title = stringResource(R.string.settings_section_theme),
                badgeColor = HighlighterPink.copy(alpha = 0.35f)
            )

            // 1 empty notebook line spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Theme Cards: Row 1 (Classic Yellow & Emerald Registry) - exactly 3 notebook rules (87dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 3)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemePackCard(
                    palette = JournalThemePacks.ClassicYellow,
                    isSelected = state.selectedTheme == JournalThemeId.CLASSIC_YELLOW,
                    onClick = { viewModel.selectTheme(JournalThemeId.CLASSIC_YELLOW) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                ThemePackCard(
                    palette = JournalThemePacks.EmeraldRegistry,
                    isSelected = state.selectedTheme == JournalThemeId.EMERALD_REGISTRY,
                    onClick = { viewModel.selectTheme(JournalThemeId.EMERALD_REGISTRY) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // 1 empty notebook line spacer between card rows
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Theme Cards: Row 2 (White Notebook & Dark Carnet) - exactly 3 notebook rules (87dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 3)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemePackCard(
                    palette = JournalThemePacks.WhiteNotebook,
                    isSelected = state.selectedTheme == JournalThemeId.WHITE_NOTEBOOK,
                    onClick = { viewModel.selectTheme(JournalThemeId.WHITE_NOTEBOOK) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                ThemePackCard(
                    palette = JournalThemePacks.DarkCarnet,
                    isSelected = state.selectedTheme == JournalThemeId.DARK_CARNET,
                    onClick = { viewModel.selectTheme(JournalThemeId.DARK_CARNET) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // 1 empty notebook line spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Section 2: Préférences (soft yellow band)
            SettingsSectionBadge(
                title = stringResource(R.string.settings_section_preferences),
                badgeColor = HighlighterYellow.copy(alpha = 0.45f)
            )

            // Lines 4-5: Setting 1 - Language (58dp = 2 rules)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2)
            ) {
                // Line 1 (29dp): Label, separator & description on rule 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .journalVisualOnRule(gapAboveRule = 2.dp)
                            .size(7.5.dp)
                    ) {
                        drawCircle(color = Color(0xFFE27B97)) // Soft Rose Pink
                    }

                    Text(
                        text = stringResource(R.string.settings_language),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 16.sp else 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = "—",
                        fontFamily = PatrickHandFamily,
                        fontSize = 12.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = stringResource(R.string.settings_language_description),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 11.5.sp else 12.sp,
                        fontWeight = FontWeight.Light,
                        color = JournalMutedInk.copy(alpha = 0.75f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }

                // Line 2 (29dp): Segmented choices sitting on rule 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(start = 22.dp, end = 14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    NotebookSegmentedControl(
                        options = listOf(
                            "dar" to stringResource(R.string.settings_darija),
                            "ar" to stringResource(R.string.settings_arabic),
                            "fr" to stringResource(R.string.settings_french),
                            "en" to stringResource(R.string.settings_english)
                        ),
                        selectedOption = when {
                            state.currentLanguage == "dar" -> "dar"
                            state.currentLanguage == "ar" -> "ar"
                            state.currentLanguage == "en" -> "en"
                            else -> "fr"
                        },
                        onSelectOption = { viewModel.selectLanguage(it) }
                    )
                }
            }

            // Lines 6-7: Setting 2 - Currency (58dp = 2 rules)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2)
            ) {
                // Line 1 (29dp): Label, separator & description on rule 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .journalVisualOnRule(gapAboveRule = 2.dp)
                            .size(7.5.dp)
                    ) {
                        drawCircle(color = Color(0xFF5B9EC9)) // Soft Sky Blue
                    }

                    Text(
                        text = stringResource(R.string.settings_currency),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 16.sp else 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = "—",
                        fontFamily = PatrickHandFamily,
                        fontSize = 12.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = stringResource(R.string.settings_currency_description),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 11.5.sp else 12.sp,
                        fontWeight = FontWeight.Light,
                        color = JournalMutedInk.copy(alpha = 0.75f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }

                // Line 2 (29dp): Segmented choices sitting on rule 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(start = 22.dp, end = 14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    NotebookSegmentedControl(
                        options = listOf(
                            MoneyUnit.DIRHAM to stringResource(R.string.currency_dirham),
                            MoneyUnit.RIAL to stringResource(R.string.currency_rial)
                        ),
                        selectedOption = state.defaultCurrency,
                        onSelectOption = { viewModel.selectDefaultCurrency(it) }
                    )
                }
            }

            // 1 empty notebook line spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Section 3: Sécurité & Confidentialité (soft pink band)
            SettingsSectionBadge(
                title = stringResource(R.string.settings_section_security),
                badgeColor = HighlighterPink.copy(alpha = 0.35f)
            )

            // Setting: Verrouiller le carnet (58dp = 2 rules)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2)
            ) {
                // Line 1 (29dp): Label, separator & description on rule 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .journalVisualOnRule(gapAboveRule = 2.dp)
                            .size(7.5.dp)
                    ) {
                        drawCircle(color = Color(0xFFE27B97)) // Soft Rose Pink
                    }

                    Text(
                        text = stringResource(R.string.settings_security_lock_title),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 16.sp else 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = JournalWritingInk,
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = "—",
                        fontFamily = PatrickHandFamily,
                        fontSize = 12.sp,
                        color = JournalMutedInk.copy(alpha = 0.5f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )

                    Text(
                        text = stringResource(R.string.settings_security_lock_desc),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 11.5.sp else 12.sp,
                        fontWeight = FontWeight.Light,
                        color = JournalMutedInk.copy(alpha = 0.75f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }

                // Line 2 (29dp): Segmented choices sitting on rule 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(start = 22.dp, end = 14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    NotebookSegmentedControl(
                        options = listOf(
                            false to stringResource(R.string.settings_security_badge_inactive),
                            true to stringResource(R.string.settings_security_badge_active)
                        ),
                        selectedOption = state.isLockEnabled,
                        onSelectOption = { enable ->
                            if (enable) {
                                if (state.hasPinSet) {
                                    viewModel.setLockEnabled(true)
                                } else {
                                    showSetupPinDialog = true
                                }
                            } else {
                                viewModel.setLockEnabled(false)
                            }
                        }
                    )
                }
            }

            if (state.isLockEnabled) {
                // Setting: Modifier le code PIN
                JournalActionRow(
                    title = stringResource(R.string.settings_security_pin_change),
                    description = stringResource(R.string.settings_security_pin_change_desc),
                    bulletColor = Color(0xFFE27B97),
                    badgeText = "PIN",
                    onClick = { showSetupPinDialog = true }
                )

                // Setting: Déverrouillage par empreinte (if supported)
                if (state.isBiometricAvailable) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing * 2)
                    ) {
                        // Line 1 (29dp): Label, separator & description on rule 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(JournalRuleSpacing)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .journalVisualOnRule(gapAboveRule = 2.dp)
                                    .size(7.5.dp)
                            ) {
                                drawCircle(color = Color(0xFF5B9EC9))
                            }

                            Text(
                                text = stringResource(R.string.settings_security_biometrics_title),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 16.sp else 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = JournalWritingInk,
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier.journalBaselineOnRule()
                            )

                            Text(
                                text = "—",
                                fontFamily = PatrickHandFamily,
                                fontSize = 12.sp,
                                color = JournalMutedInk.copy(alpha = 0.5f),
                                style = TextStyle(platformStyle = NoFontPadding),
                                modifier = Modifier.journalBaselineOnRule()
                            )

                            Text(
                                text = stringResource(R.string.settings_security_biometrics_desc),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = if (isRtl) 11.5.sp else 12.sp,
                                fontWeight = FontWeight.Light,
                                color = JournalMutedInk.copy(alpha = 0.75f),
                                style = TextStyle(platformStyle = NoFontPadding),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.journalBaselineOnRule()
                            )
                        }

                        // Line 2 (29dp): Segmented choices sitting on rule 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(JournalRuleSpacing)
                                .padding(start = 22.dp, end = 14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            NotebookSegmentedControl(
                                options = listOf(
                                    false to stringResource(R.string.settings_security_badge_inactive),
                                    true to stringResource(R.string.settings_security_badge_active)
                                ),
                                selectedOption = state.useBiometrics,
                                onSelectOption = { viewModel.setUseBiometrics(it) }
                            )
                        }
                    }
                }

                // Setting: Délai de verrouillage (58dp = 2 rules)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing * 2)
                ) {
                    // Line 1 (29dp): Label, separator & description on rule 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .journalVisualOnRule(gapAboveRule = 2.dp)
                                .size(7.5.dp)
                        ) {
                            drawCircle(color = Color(0xFFE5A93C))
                        }

                        Text(
                            text = stringResource(R.string.settings_security_timeout_title),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 16.sp else 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )

                        Text(
                            text = "—",
                            fontFamily = PatrickHandFamily,
                            fontSize = 12.sp,
                            color = JournalMutedInk.copy(alpha = 0.5f),
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )

                        Text(
                            text = stringResource(R.string.settings_security_timeout_desc),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 11.5.sp else 12.sp,
                            fontWeight = FontWeight.Light,
                            color = JournalMutedInk.copy(alpha = 0.75f),
                            style = TextStyle(platformStyle = NoFontPadding),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }

                    // Line 2 (29dp): Segmented choices sitting on rule 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(JournalRuleSpacing)
                            .padding(start = 22.dp, end = 14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        NotebookSegmentedControl(
                            options = listOf(
                                0 to stringResource(R.string.settings_security_timeout_immediately),
                                60 to stringResource(R.string.settings_security_timeout_1min),
                                300 to stringResource(R.string.settings_security_timeout_5min)
                            ),
                            selectedOption = state.lockTimeoutSeconds,
                            onSelectOption = { viewModel.setLockTimeoutSeconds(it) }
                        )
                    }
                }
            }

            // 1 empty notebook line spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Section 4: Données (soft green band)
            SettingsSectionBadge(
                title = stringResource(R.string.settings_section_data),
                badgeColor = HighlighterGreen.copy(alpha = 0.35f)
            )

            // Setting 3 - Local Storage (58dp = 2 rules)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(JournalRuleSpacing * 2)
            ) {
                // Line 1 (29dp): Title on Start, Badge on End
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .journalVisualOnRule(gapAboveRule = 2.dp)
                                .size(7.5.dp)
                        ) {
                            drawCircle(color = Color(0xFF7FA85B)) // Soft Sage Green
                        }

                        Text(
                            text = stringResource(R.string.settings_storage_title),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 16.sp else 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = JournalWritingInk,
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(JournalRuleSpacing)
                            .drawBehind {
                                val h = size.height
                                val w = size.width
                                val washHeight = 20.dp.toPx()
                                val washCenterY = h - 6.dp.toPx()
                                val washY = washCenterY - (washHeight / 2f)
                                val padH = 8.dp.toPx()
                                val radius = CornerRadius(6.dp.toPx())
                                // Background tint
                                drawRoundRect(
                                    color = Color(0xFF7FA85B).copy(alpha = 0.20f),
                                    topLeft = Offset(-padH, washY),
                                    size = Size(w + padH * 2, washHeight),
                                    cornerRadius = radius
                                )
                                // Delicate outline
                                drawRoundRect(
                                    color = Color(0xFF7FA85B).copy(alpha = 0.45f),
                                    topLeft = Offset(-padH, washY),
                                    size = Size(w + padH * 2, washHeight),
                                    cornerRadius = radius,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = stringResource(R.string.settings_storage_badge),
                            fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                            fontSize = if (isRtl) 11.5.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF436B2B),
                            style = TextStyle(platformStyle = NoFontPadding),
                            modifier = Modifier.journalBaselineOnRule()
                        )
                    }
                }

                // Line 2 (29dp): Storage Notice Text sitting on rule 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 29.5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.settings_storage_notice),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 11.5.sp else 12.sp,
                        fontWeight = FontWeight.Light,
                        color = JournalMutedInk.copy(alpha = 0.75f),
                        style = TextStyle(platformStyle = NoFontPadding),
                        modifier = Modifier.journalBaselineOnRule()
                    )
                }
            }

            // Line 12: 1 empty notebook line spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Section 5: Sauvegarde & Restauration (soft blue band)
            SettingsSectionBadge(
                title = stringResource(R.string.settings_section_backup),
                badgeColor = HighlighterBlue.copy(alpha = 0.35f)
            )

            // Setting: Exporter une sauvegarde (.calc)
            JournalActionRow(
                title = stringResource(R.string.settings_backup_action_export),
                description = stringResource(R.string.settings_backup_action_export_desc),
                bulletColor = Color(0xFF5B9EC9),
                badgeText = ".calc",
                onClick = {
                    val dateSuffix = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("sarf_backup_${dateSuffix}.calc")
                }
            )

            // Setting: Partager la sauvegarde (.calc)
            JournalActionRow(
                title = stringResource(R.string.settings_backup_action_share),
                description = stringResource(R.string.settings_backup_action_share_desc),
                bulletColor = Color(0xFF7FA85B),
                badgeText = stringResource(R.string.settings_action_badge_share),
                onClick = {
                    val chooserTitle = context.getString(R.string.backup_toast_share_title)
                    viewModel.shareBackup(context, chooserTitle) { success ->
                        if (!success) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.backup_toast_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )

            // Setting: Restaurer une sauvegarde (.calc)
            JournalActionRow(
                title = stringResource(R.string.settings_backup_action_restore),
                description = stringResource(R.string.settings_backup_action_restore_desc),
                bulletColor = Color(0xFFE27B97),
                badgeText = stringResource(R.string.settings_action_badge_restore),
                onClick = {
                    restoreLauncher.launch(arrayOf("*/*"))
                }
            )

            // Setting: Recharger les exemples
            JournalActionRow(
                title = stringResource(R.string.settings_seed_data_title),
                description = stringResource(R.string.settings_seed_data_desc),
                bulletColor = Color(0xFFE5A93C),
                badgeText = stringResource(R.string.settings_seed_data_badge),
                onClick = {
                    viewModel.reloadSampleData(context) { success ->
                        val msg = if (success) {
                            context.getString(R.string.settings_seed_data_success)
                        } else {
                            context.getString(R.string.backup_toast_error)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // 1 empty notebook line spacer
            Spacer(modifier = Modifier.height(JournalRuleSpacing))

            // Section 6: Options d'exportation (soft yellow band)
            SettingsSectionBadge(
                title = stringResource(R.string.export_options_title),
                badgeColor = HighlighterYellow.copy(alpha = 0.45f)
            )

            // Setting: Exporter PDF / Excel
            JournalActionRow(
                title = stringResource(R.string.settings_export_title),
                description = stringResource(R.string.settings_export_desc),
                bulletColor = Color(0xFFE5A93C),
                badgeText = stringResource(R.string.export_action_share),
                onClick = {
                    showExportOptions = true
                }
            )

            // Bottom Spacers: 5 notebook lines for full scrolling clearance above dock
            Spacer(modifier = Modifier.height(JournalRuleSpacing * 5))
        }

        // Restore confirmation dialog
        state.restoreCandidate?.let { payload ->
            RestoreBackupDialog(
                payload = payload,
                onMerge = {
                    viewModel.confirmRestore(replaceExisting = false) { success ->
                        val msg = if (success) {
                            context.getString(R.string.backup_toast_restore_success)
                        } else {
                            context.getString(R.string.backup_toast_error)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onReplace = {
                    viewModel.confirmRestore(replaceExisting = true) { success ->
                        val msg = if (success) {
                            context.getString(R.string.backup_toast_restore_success)
                        } else {
                            context.getString(R.string.backup_toast_error)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = {
                    viewModel.dismissRestoreDialog()
                }
            )
        }

        // Export Options Bottom Sheet
        if (showExportOptions) {
            ExportOptionsBottomSheet(
                title = stringResource(R.string.export_options_title),
                onExportPdf = {
                    viewModel.exportAllToPdf(context, isRtl) { success, hasCalcs ->
                        if (!hasCalcs) {
                            Toast.makeText(context, R.string.export_no_calculations, Toast.LENGTH_SHORT).show()
                        } else if (!success) {
                            Toast.makeText(context, R.string.export_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onExportExcel = {
                    viewModel.exportAllToExcel(context) { success, hasCalcs ->
                        if (!hasCalcs) {
                            Toast.makeText(context, R.string.export_no_calculations, Toast.LENGTH_SHORT).show()
                        } else if (!success) {
                            Toast.makeText(context, R.string.export_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showExportOptions = false }
            )
        }

        // Setup PIN Dialog
        if (showSetupPinDialog) {
            SetupPinDialog(
                onPinConfirmed = { newPin ->
                    viewModel.savePin(newPin)
                    showSetupPinDialog = false
                    Toast.makeText(context, R.string.pin_set_success, Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showSetupPinDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionBadge(
    title: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val textStyle = TextStyle(
        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
        fontSize = if (isRtl) 15.sp else 15.5.sp,
        fontWeight = FontWeight.Bold,
        platformStyle = NoFontPadding
    )
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(title, textStyle) {
        textMeasurer.measure(AnnotatedString(title), textStyle)
    }
    val density = LocalDensity.current
    val badgeWidthDp = with(density) {
        textLayoutResult.size.width.toDp() + 24.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing)
            .padding(start = 14.dp, end = 0.dp)
            .drawBehind {
                val badgeW = badgeWidthDp.toPx().coerceAtMost(size.width - 16.dp.toPx())
                val totalW = size.width
                val h = size.height
                val r = 6.dp.toPx()
                val lineH = 5.dp.toPx()
                val filletR = 2.5.dp.toPx()

                val unifiedPath = Path().apply {
                    if (!isRtl) {
                        moveTo(0f, r)
                        quadraticTo(0f, 0f, r, 0f)
                        lineTo(badgeW - r, 0f)
                        quadraticTo(badgeW, 0f, badgeW, r)
                        lineTo(badgeW, h - lineH - filletR)
                        quadraticTo(badgeW, h - lineH, badgeW + filletR, h - lineH)
                        lineTo(totalW, h - lineH)
                        lineTo(totalW, h)
                        lineTo(r, h)
                        quadraticTo(0f, h, 0f, h - r)
                        close()
                    } else {
                        val badgeStart = totalW - badgeW
                        moveTo(totalW, r)
                        quadraticTo(totalW, 0f, totalW - r, 0f)
                        lineTo(badgeStart + r, 0f)
                        quadraticTo(badgeStart, 0f, badgeStart, r)
                        lineTo(badgeStart, h - lineH - filletR)
                        quadraticTo(badgeStart, h - lineH, badgeStart - filletR, h - lineH)
                        lineTo(0f, h - lineH)
                        lineTo(0f, h)
                        lineTo(totalW - r, h)
                        quadraticTo(totalW, h, totalW, h - r)
                        close()
                    }
                }

                drawPath(
                    path = unifiedPath,
                    color = badgeColor
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(badgeWidthDp)
                .height(JournalRuleSpacing),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 15.sp else 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = JournalWritingInk,
                style = TextStyle(platformStyle = NoFontPadding)
            )
        }
    }
}

@Composable
private fun JournalActionRow(
    title: String,
    description: String,
    bulletColor: Color,
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(JournalRuleSpacing * 2)
            .clickable(onClick = onClick)
    ) {
        // Line 1 (29dp): Bullet + Title on Start, Badge on End
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Canvas(
                    modifier = Modifier
                        .journalVisualOnRule(gapAboveRule = 2.dp)
                        .size(7.5.dp)
                ) {
                    drawCircle(color = bulletColor)
                }

                Text(
                    text = title,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 16.sp else 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = JournalWritingInk,
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }

            Box(
                modifier = Modifier
                    .height(JournalRuleSpacing)
                    .drawBehind {
                        val h = size.height
                        val w = size.width
                        val washHeight = 20.dp.toPx()
                        val washCenterY = h - 6.dp.toPx()
                        val washY = washCenterY - (washHeight / 2f)
                        val padH = 8.dp.toPx()
                        val radius = CornerRadius(6.dp.toPx())
                        // Soft tinted background
                        drawRoundRect(
                            color = bulletColor.copy(alpha = 0.16f),
                            topLeft = Offset(-padH, washY),
                            size = Size(w + padH * 2, washHeight),
                            cornerRadius = radius
                        )
                        // Delicate outline border
                        drawRoundRect(
                            color = bulletColor.copy(alpha = 0.50f),
                            topLeft = Offset(-padH, washY),
                            size = Size(w + padH * 2, washHeight),
                            cornerRadius = radius,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = badgeText,
                    fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                    fontSize = if (isRtl) 11.5.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = bulletColor.copy(alpha = 0.95f),
                    style = TextStyle(platformStyle = NoFontPadding),
                    modifier = Modifier.journalBaselineOnRule()
                )
            }
        }

        // Line 2 (29dp): Description sitting on rule 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(JournalRuleSpacing)
                .padding(horizontal = 29.5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = description,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 11.5.sp else 12.sp,
                fontWeight = FontWeight.Light,
                color = JournalMutedInk.copy(alpha = 0.75f),
                style = TextStyle(platformStyle = NoFontPadding),
                modifier = Modifier.journalBaselineOnRule()
            )
        }
    }
}

@Composable
private fun ThemePackCard(
    palette: JournalThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.RadioButton, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }),
        shape = RoundedCornerShape(6.dp),
        color = palette.paper,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) palette.accent else palette.cardBorder
        ),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Two miniature ruled lines dividing the 3 rules inside the card (at 29dp and 58dp)
                    val rule1Y = JournalRuleSpacing.toPx()
                    val rule2Y = (JournalRuleSpacing * 2).toPx()
                    val strokeW = 0.6.dp.toPx()
                    val ruleCol = palette.rule.copy(alpha = 0.35f)
                    drawLine(
                        color = ruleCol,
                        start = Offset(0f, rule1Y),
                        end = Offset(size.width, rule1Y),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = ruleCol,
                        start = Offset(0f, rule2Y),
                        end = Offset(size.width, rule2Y),
                        strokeWidth = strokeW
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Row 1 (29dp = 1 rule): Theme name + Selection badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(palette.nameResId),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = if (isRtl) 13.5.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        style = TextStyle(platformStyle = NoFontPadding)
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.accent.copy(alpha = 0.22f))
                                .border(0.8.dp, palette.accent, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_theme_active_badge),
                                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (palette.isDark) palette.accent else palette.ink,
                                style = TextStyle(platformStyle = NoFontPadding)
                            )
                        }
                    } else {
                        // Subtle hollow circle for unselected state
                        Canvas(modifier = Modifier.size(11.dp)) {
                            drawCircle(
                                color = palette.mutedInk.copy(alpha = 0.45f),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }

                // Row 2 (29dp = 1 rule): Miniature writing sample with accent highlighter stroke
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.accent.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isRtl) "١٥٠ د.م ✓" else "150.00 DH ✓",
                            fontFamily = PatrickHandFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.ink,
                            style = TextStyle(platformStyle = NoFontPadding)
                        )
                    }
                }

                // Row 3 (29dp = 1 rule): 3 miniature color dots (paper, ink, rule) + description
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(JournalRuleSpacing)
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Swatch dots
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = palette.ink)
                        }
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = palette.rule)
                        }
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = palette.accent)
                        }
                    }

                    Text(
                        text = stringResource(palette.descResId),
                        fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                        fontSize = 10.sp,
                        color = palette.mutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = NoFontPadding)
                    )
                }
            }
        }
    }
}
