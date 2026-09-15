package com.cash.guide.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cash.guide.feature.editor.CalculationEditorScreen
import com.cash.guide.feature.editor.CalculationEditorViewModel
import com.cash.guide.feature.history.HistoryScreen
import com.cash.guide.feature.history.HistoryViewModel
import com.cash.guide.feature.home.HomeScreen
import com.cash.guide.feature.home.HomeViewModel
import com.cash.guide.feature.settings.SettingsScreen
import com.cash.guide.feature.settings.SettingsViewModel
import com.cash.guide.feature.cashregister.CashRegisterScreen
import com.cash.guide.feature.cashregister.CashRegisterViewModel
import com.cash.guide.feature.calculs.CalculsScreen
import com.cash.guide.feature.calculs.CalculsViewModel

import com.cash.guide.feature.groups.GroupsScreen
import com.cash.guide.feature.groups.GroupsViewModel
import com.cash.guide.feature.groups.GroupDetailScreen
import com.cash.guide.feature.groups.GroupDetailViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.ChecklistRepository
import com.cash.guide.data.NoteRepository
import com.cash.guide.data.ReminderRepository
import com.cash.guide.data.SettingsRepository
import com.cash.guide.feature.checklist.ChecklistScreen
import com.cash.guide.feature.checklist.ChecklistViewModel
import com.cash.guide.feature.checklist.ChecklistsOverviewScreen
import com.cash.guide.feature.checklist.ChecklistsOverviewViewModel
import com.cash.guide.feature.note.NotesOverviewScreen
import com.cash.guide.feature.note.NotesOverviewViewModel
import com.cash.guide.feature.note.NoteEditorScreen
import com.cash.guide.feature.note.NoteViewModel
import com.cash.guide.feature.reminders.RemindersOverviewScreen
import com.cash.guide.feature.reminders.RemindersViewModel
import com.cash.guide.feature.savings.SavingsScreen
import com.cash.guide.feature.savings.SavingsViewModel
import com.cash.guide.feature.history.MonthCalculationsScreen
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HssabiNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    groupsViewModel: GroupsViewModel,
    historyViewModel: HistoryViewModel,
    savingsViewModel: SavingsViewModel,
    settingsViewModel: SettingsViewModel,
    calculationRepository: CalculationRepository,
    checklistRepository: ChecklistRepository,
    noteRepository: NoteRepository,
    reminderRepository: ReminderRepository,
    settingsRepository: SettingsRepository,
    editorViewModelFactory: () -> CalculationEditorViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = modifier
    ) {
        composable(AppDestination.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNewCalculation = { navController.navigate(AppDestination.NewCalculation.route) },
                onNewCalculationWithParams = { title, calcType, currency, templateId ->
                    navController.navigate(
                        AppDestination.NewCalculation.createRoute(
                            type = calcType,
                            currency = currency.name,
                            title = title,
                            templateId = templateId
                        )
                    )
                },
                onOpenCalculation = { id -> navController.navigate("calculation/$id") },
                onOpenHistory = { navController.navigate(AppDestination.History.route) },
                onOpenMonthCalculations = { year, month ->
                    navController.navigate("month_calculations/$year/$month")
                },
                onOpenStyleShowcase = { navController.navigate(AppDestination.StyleShowcase.route) },
                onOpenCalculs = { navController.navigate(AppDestination.Calculs.route) },
                onOpenCashRegister = { navController.navigate(AppDestination.CashRegister.route) },
                onOpenGroups = { navController.navigate(AppDestination.Groups.route) },
                onOpenGroup = { groupId -> navController.navigate("group/$groupId") },
                onOpenChecklist = { navController.navigate(AppDestination.Checklist.route) },
                onOpenChecklistWithId = { id ->
                    navController.navigate(AppDestination.ChecklistDetail.createRoute(id))
                },
                onOpenNotes = { navController.navigate(AppDestination.Notes.route) },
                onOpenNote = { id ->
                    navController.navigate(AppDestination.NoteDetail.createRoute(id))
                },
                onNewChecklist = {
                    navController.navigate(AppDestination.Checklist.route)
                },
                onNewNote = {
                    navController.navigate(AppDestination.NoteDetail.createRoute(java.util.UUID.randomUUID().toString()))
                },
                onOpenReminders = {
                    navController.navigate(AppDestination.Reminders.route)
                },
                onNewReminder = {
                    navController.navigate(AppDestination.Reminders.createRoute(openCreate = true))
                }
            )
        }

        composable(AppDestination.Groups.route) {
            GroupsScreen(
                viewModel = groupsViewModel,
                settingsRepository = settingsRepository,
                onOpenGroup = { groupId -> navController.navigate("group/$groupId") }
            )
        }

        composable(
            route = AppDestination.GroupDetail.ROUTE_PATTERN,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupDetailViewModel: GroupDetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                key = "group_detail_$groupId"
            ) {
                GroupDetailViewModel(groupId, calculationRepository, noteRepository, checklistRepository)
            }
            GroupDetailScreen(
                viewModel = groupDetailViewModel,
                settingsRepository = settingsRepository,
                onBack = { navController.popBackStack() },
                onOpenCalculation = { id -> navController.navigate("calculation/$id") },
                onNewCalculationInGroup = { gid ->
                    coroutineScope.launch {
                        calculationRepository.createDraftInGroup(gid)
                        navController.navigate(AppDestination.NewCalculation.routeForGroup(gid))
                    }
                },
                onOpenNote = { noteId ->
                    navController.navigate(AppDestination.NoteDetail.createRoute(noteId))
                },
                onNewNoteInGroup = {
                    coroutineScope.launch {
                        val newNoteId = groupDetailViewModel.createNoteInGroup()
                        navController.navigate(AppDestination.NoteDetail.createRoute(newNoteId))
                    }
                },
                onOpenChecklist = { checklistId ->
                    navController.navigate(AppDestination.ChecklistDetail.createRoute(checklistId))
                },
                onNewChecklistInGroup = { checklistId ->
                    navController.navigate(AppDestination.ChecklistDetail.createRoute(checklistId))
                }
            )
        }

        composable(AppDestination.StyleShowcase.route) {
            com.cash.guide.feature.showcase.TestNotebookShowcaseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppDestination.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onOpenCalculation = { id -> navController.navigate("calculation/$id") },
                onNewCalculation = { navController.navigate(AppDestination.NewCalculation.route) },
                onOpenMonthCalculations = { year, month ->
                    navController.navigate("month_calculations/$year/$month")
                }
            )
        }

        composable(AppDestination.Savings.route) {
            SavingsScreen(
                viewModel = savingsViewModel
            )
        }

        composable(AppDestination.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel
            )
        }

        composable(AppDestination.Calculs.route) { backStackEntry ->
            val calculsViewModel: CalculsViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            ) {
                CalculsViewModel(calculationRepository, settingsRepository)
            }
            CalculsScreen(
                viewModel = calculsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenCalculation = { id -> navController.navigate("calculation/$id") },
                onOpenCashRegister = { navController.navigate(AppDestination.CashRegister.route) },
                onNewCalculationWithParams = { title, calcType, currency, templateId ->
                    navController.navigate(
                        AppDestination.NewCalculation.createRoute(
                            type = calcType,
                            currency = currency.name,
                            title = title,
                            templateId = templateId
                        )
                    )
                }
            )
        }

        composable(AppDestination.CashRegister.route) { backStackEntry ->
            val cashRegisterViewModel: CashRegisterViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            )
            CashRegisterScreen(
                viewModel = cashRegisterViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppDestination.Checklists.route) { backStackEntry ->
            val overviewViewModel: ChecklistsOverviewViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            ) {
                ChecklistsOverviewViewModel(checklistRepository)
            }
            ChecklistsOverviewScreen(
                viewModel = overviewViewModel,
                calculationRepository = calculationRepository,
                onOpenChecklist = { checklistId ->
                    navController.navigate(AppDestination.ChecklistDetail.createRoute(checklistId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppDestination.ChecklistDetail.ROUTE_PATTERN,
            arguments = listOf(navArgument("checklistId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val checklistId = backStackEntry.arguments?.getString("checklistId")
            val checklistViewModel: ChecklistViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                key = "checklist_${checklistId ?: "default"}"
            ) {
                ChecklistViewModel(checklistRepository, checklistId)
            }
            ChecklistScreen(
                viewModel = checklistViewModel,
                calculationRepository = calculationRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppDestination.Notes.route) { backStackEntry ->
            val overviewViewModel: NotesOverviewViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            ) {
                NotesOverviewViewModel(noteRepository)
            }
            NotesOverviewScreen(
                viewModel = overviewViewModel,
                calculationRepository = calculationRepository,
                onOpenNote = { noteId ->
                    navController.navigate(AppDestination.NoteDetail.createRoute(noteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppDestination.Reminders.ROUTE_PATTERN,
            arguments = listOf(navArgument("openCreate") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val openCreate = backStackEntry.arguments?.getBoolean("openCreate") ?: false
            val remindersViewModel: RemindersViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            ) {
                RemindersViewModel(reminderRepository)
            }
            LaunchedEffect(openCreate) {
                if (openCreate) {
                    remindersViewModel.openCreateDialog()
                }
            }
            RemindersOverviewScreen(
                viewModel = remindersViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppDestination.NoteDetail.ROUTE_PATTERN,
            arguments = listOf(navArgument("noteId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val context = androidx.compose.ui.platform.LocalContext.current
            val noteViewModel: NoteViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                key = "note_${noteId ?: "new"}"
            ) {
                NoteViewModel(noteRepository, noteId, context)
            }
            NoteEditorScreen(
                viewModel = noteViewModel,
                calculationRepository = calculationRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppDestination.NewCalculation.ROUTE_PATTERN,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("type") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "PERSONNEL"
                },
                navArgument("currency") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "DIRHAM"
                },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialGroupId = backStackEntry.arguments?.getString("groupId")
            val initialType = backStackEntry.arguments?.getString("type") ?: "PERSONNEL"
            val initialCurrency = backStackEntry.arguments?.getString("currency") ?: "DIRHAM"
            val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
            val initialTitle = runCatching { java.net.URLDecoder.decode(rawTitle, "UTF-8") }.getOrDefault(rawTitle)
            val templateId = backStackEntry.arguments?.getString("templateId")

            val editorViewModel: CalculationEditorViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                key = "new_calculation_${initialGroupId ?: "root"}_${initialType}_${initialCurrency}_${templateId ?: "none"}"
            ) {
                editorViewModelFactory()
            }
            CalculationEditorScreen(
                viewModel = editorViewModel,
                calculationId = null,
                initialGroupId = initialGroupId,
                initialType = initialType,
                initialCurrency = initialCurrency,
                initialTitle = initialTitle,
                templateId = templateId,
                onNavigateBack = { navController.popBackStack() },
                onOpenCalculation = { id -> navController.navigate("calculation/$id") }
            )
        }

        composable(
            route = AppDestination.EditCalculation.ROUTE_PATTERN,
            arguments = listOf(navArgument("calculationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val calcId = backStackEntry.arguments?.getString("calculationId")
            val editorViewModel: CalculationEditorViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                key = "edit_calculation_$calcId"
            ) {
                editorViewModelFactory()
            }
            CalculationEditorScreen(
                viewModel = editorViewModel,
                calculationId = calcId,
                onNavigateBack = { navController.popBackStack() },
                onOpenCalculation = { id -> navController.navigate("calculation/$id") }
            )
        }

        composable(
            route = AppDestination.MonthCalculations.ROUTE_PATTERN,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: Calendar.getInstance().get(Calendar.YEAR)
            val month = backStackEntry.arguments?.getInt("month") ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
            MonthCalculationsScreen(
                year = year,
                month = month,
                repository = calculationRepository,
                onOpenCalculation = { id -> navController.navigate("calculation/$id") },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
