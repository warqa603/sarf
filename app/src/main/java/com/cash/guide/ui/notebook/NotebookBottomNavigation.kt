package com.cash.guide.ui.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cash.guide.R
import com.cash.guide.app.AppDestination

@Composable
fun NotebookBottomNavigation(
    currentDestination: AppDestination,
    onNavigateTo: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = JournalDockBg,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                color = JournalRule.copy(alpha = 0.6f),
                thickness = 0.65.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Tab 1: Home
                BottomNavItem(
                    label = stringResource(R.string.nav_home),
                    symbol = HisabiSymbol.Home,
                    isSelected = currentDestination == AppDestination.Home,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onNavigateTo(AppDestination.Home)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Groups
                BottomNavItem(
                    label = stringResource(R.string.nav_groups),
                    symbol = HisabiSymbol.Folder,
                    isSelected = currentDestination == AppDestination.Groups,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onNavigateTo(AppDestination.Groups)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Tab 3: Savings
                BottomNavItem(
                    label = stringResource(R.string.nav_savings),
                    symbol = HisabiSymbol.Coin,
                    isSelected = currentDestination == AppDestination.Savings,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onNavigateTo(AppDestination.Savings)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Tab 4: Settings
                BottomNavItem(
                    label = stringResource(R.string.nav_settings),
                    symbol = HisabiSymbol.Gear,
                    isSelected = currentDestination == AppDestination.Settings,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onNavigateTo(AppDestination.Settings)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    symbol: HisabiSymbol,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDesc = if (isSelected) stringResource(R.string.cd_selected) else stringResource(R.string.cd_not_selected)

    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) HighlighterPink.copy(alpha = 0.35f) else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics {
                contentDescription = "$label, $selectedDesc"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            HisabiSketchIcon(
                symbol = symbol,
                contentDescription = null,
                tint = if (isSelected) JournalInk else JournalMutedInk,
                size = 19.dp
            )

            Text(
                text = label,
                fontFamily = if (isRtl) TajawalFamily else PatrickHandFamily,
                fontSize = if (isRtl) 11.5.sp else 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) JournalInk else JournalMutedInk,
                maxLines = 1
            )
        }
    }
}

