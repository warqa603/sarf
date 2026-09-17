package com.cash.guide.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.ChecklistRepository
import com.cash.guide.data.NoteRepository
import com.cash.guide.data.ReminderRepository
import com.cash.guide.data.SavingsRepository
import com.cash.guide.data.SecurityRepository
import com.cash.guide.data.SettingsRepository
import com.cash.guide.data.TemplateRepository
import com.cash.guide.data.backup.BackupManager
import com.cash.guide.data.db.HssabiDatabase
import com.cash.guide.domain.ChecklistLinkHelper
import android.net.Uri
import android.widget.Toast
import com.cash.guide.feature.editor.CalculationEditorViewModel
import com.cash.guide.feature.history.HistoryViewModel
import com.cash.guide.feature.home.HomeViewModel
import com.cash.guide.feature.groups.GroupsViewModel
import com.cash.guide.feature.savings.SavingsViewModel
import com.cash.guide.feature.settings.SettingsViewModel
import com.cash.guide.ui.notebook.JournalLockScreen
import com.cash.guide.ui.notebook.JournalPaper
import com.cash.guide.ui.notebook.JournalTheme
import com.cash.guide.ui.notebook.JournalThemeId
import com.cash.guide.ui.notebook.JournalThemePacks
import com.cash.guide.ui.notebook.LocalJournalTheme
import com.cash.guide.ui.notebook.NotebookBottomNavigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Locale

class LocalizedContextWrapper(
    base: Context,
    val originalActivity: Activity?
) : ContextWrapper(base), ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry
        get() = (originalActivity as? ActivityResultRegistryOwner)?.activityResultRegistry
            ?: (baseContext as? ActivityResultRegistryOwner)?.activityResultRegistry
            ?: error("No ActivityResultRegistry available")
}

@Composable
fun HssabiApp(
    deepLinkUri: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { HssabiDatabase.getInstance(context) }
    val calculationRepository = remember {
        CalculationRepository(database.calculationDao(), database.calculationGroupDao())
    }
    val checklistRepository = remember {
        ChecklistRepository(database.checklistDao())
    }
    val noteRepository = remember {
        NoteRepository(database.noteDao())
    }
    val reminderRepository = remember {
        ReminderRepository(database.reminderDao(), context)
    }
    val savingsRepository = remember {
        SavingsRepository(database.savingsDao(), database.financialProfileDao())
    }
    val settingsRepository = remember { SettingsRepository(context) }
    val securityRepository = remember { SecurityRepository(context) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val isAppLocked by securityRepository.isAppLocked.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (securityRepository.isLockEnabled.first()) {
            securityRepository.lock()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    securityRepository.onAppBackgrounded()
                }
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    coroutineScope.launch {
                        securityRepository.onAppForegrounded()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val calcDao = database.calculationDao()
            if (calcDao.getAllSaved().isEmpty()) {
                com.cash.guide.data.DataSeeder.seedCleanData(context)
            }
        }
    }

    val appLanguage by settingsRepository.appLanguage.collectAsState(initial = "fr")
    val currentThemeId by settingsRepository.journalTheme.collectAsState(initial = JournalThemeId.CLASSIC_YELLOW)
    val currentPalette = remember(currentThemeId) { JournalThemePacks.get(currentThemeId) }
    androidx.compose.runtime.LaunchedEffect(currentPalette) {
        JournalTheme.currentPalette = currentPalette
    }
    val configuration = LocalConfiguration.current

    val isArabicLanguage = appLanguage == "ar" || appLanguage == "dar" || appLanguage.startsWith("ar")
    val loc = remember(appLanguage) {
        when (appLanguage) {
            "dar" -> Locale.forLanguageTag("ar-MA")
            "ar" -> Locale.forLanguageTag("ar")
            "en" -> Locale.forLanguageTag("en")
            else -> Locale.forLanguageTag("fr")
        }
    }
    val localizedConfig = remember(appLanguage, configuration) {
        Configuration(configuration).apply {
            setLocale(loc)
            setLayoutDirection(loc)
        }
    }
    androidx.compose.runtime.LaunchedEffect(loc, localizedConfig) {
        Locale.setDefault(loc)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(localizedConfig, context.resources.displayMetrics)
        (context as? Activity)?.let { act ->
            @Suppress("DEPRECATION")
            act.resources.updateConfiguration(localizedConfig, act.resources.displayMetrics)
        }
    }
    val localizedContext = remember(appLanguage, context) {
        LocalizedContextWrapper(
            context.createConfigurationContext(localizedConfig),
            context as? Activity
        )
    }
    val layoutDirection = if (isArabicLanguage) LayoutDirection.Rtl else LayoutDirection.Ltr

    val templateRepository = remember { TemplateRepository.getInstance(context) }
    val homeViewModel = viewModel {
        HomeViewModel(
            repository = calculationRepository,
            settingsRepository = settingsRepository,
            checklistRepository = checklistRepository,
            noteRepository = noteRepository,
            reminderRepository = reminderRepository
        )
    }
    val groupsViewModel = viewModel { GroupsViewModel(calculationRepository, noteRepository, checklistRepository) }
    val historyViewModel = viewModel { HistoryViewModel(calculationRepository) }
    val savingsViewModel = viewModel { SavingsViewModel(savingsRepository) }
    val backupManager = remember { BackupManager(database) }
    val settingsViewModel = viewModel { SettingsViewModel(settingsRepository, backupManager, calculationRepository, securityRepository) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    androidx.compose.runtime.LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri ?: return@LaunchedEffect
        val parsed = ChecklistLinkHelper.parseDeepLink(uri)
        if (parsed != null) {
            coroutineScope.launch {
                val newId = checklistRepository.importChecklist(parsed.title, parsed.items)
                navController.navigate(AppDestination.Checklist.createRoute(newId))
                Toast.makeText(context, "Checklist importée : ${parsed.title}", Toast.LENGTH_SHORT).show()
                onDeepLinkConsumed()
            }
        }
    }

    val isTopLevel = currentRoute in listOf(
        AppDestination.Home.route,
        AppDestination.Groups.route,
        AppDestination.Savings.route,
        AppDestination.Settings.route
    )

    val currentDestination = when (currentRoute) {
        AppDestination.Groups.route -> AppDestination.Groups
        AppDestination.Savings.route -> AppDestination.Savings
        AppDestination.Settings.route -> AppDestination.Settings
        else -> AppDestination.Home
    }

    val parentRegistryOwner = LocalActivityResultRegistryOwner.current
    val effectiveRegistryOwner = parentRegistryOwner ?: (context as? ActivityResultRegistryOwner) ?: localizedContext

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig,
        LocalLayoutDirection provides layoutDirection,
        LocalActivityResultRegistryOwner provides effectiveRegistryOwner,
        LocalJournalTheme provides currentPalette
    ) {
        Scaffold(
            bottomBar = {
                if (isTopLevel) {
                        androidx.compose.foundation.layout.Column {
                            com.cash.guide.ui.components.NotebookBannerAd()
                            NotebookBottomNavigation(
                                currentDestination = currentDestination,
                                onNavigateTo = { dest ->
                                    if (currentRoute != dest.route) {
                                        navController.navigate(dest.route) {
                                            popUpTo(AppDestination.Home.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JournalPaper)
                    .padding(
                        top = if (isTopLevel) innerPadding.calculateTopPadding() else 0.dp,
                        bottom = if (isTopLevel) innerPadding.calculateBottomPadding() else 0.dp
                    )
            ) {
                HssabiNavHost(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    groupsViewModel = groupsViewModel,
                    historyViewModel = historyViewModel,
                    savingsViewModel = savingsViewModel,
                    settingsViewModel = settingsViewModel,
                    calculationRepository = calculationRepository,
                    checklistRepository = checklistRepository,
                    noteRepository = noteRepository,
                    reminderRepository = reminderRepository,
                    settingsRepository = settingsRepository,
                    editorViewModelFactory = {
                        CalculationEditorViewModel(
                            calculationRepository = calculationRepository,
                            settingsRepository = settingsRepository,
                            templateRepository = templateRepository
                        )
                    }
                )
            }
        }

        if (isAppLocked) {
            JournalLockScreen(
                securityRepository = securityRepository,
                onUnlock = { securityRepository.unlock() }
            )
        }
    }
}
