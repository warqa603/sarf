package com.cash.guide.app

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Groups : AppDestination("groups")
    data object History : AppDestination("history")
    data object Savings : AppDestination("savings")
    data object Settings : AppDestination("settings")
    data object StyleShowcase : AppDestination("style_showcase")
    data object CashRegister : AppDestination("cash_register")
    data object Checklists : AppDestination("checklists") {
        const val ROUTE_PATTERN = "checklists?openCreate={openCreate}"
        fun createRoute(openCreate: Boolean = false): String = "checklists?openCreate=$openCreate"
    }
    data object ChecklistDetail : AppDestination("checklist_detail") {
        const val ROUTE_PATTERN = "checklist_detail/{checklistId}"
        fun createRoute(checklistId: String): String = "checklist_detail/$checklistId"
    }
    data object Checklist : AppDestination("checklists") {
        const val ROUTE_PATTERN = "checklist_detail/{checklistId}"
        fun createRoute(checklistId: String? = null): String {
            return if (checklistId != null) "checklist_detail/$checklistId" else "checklists?openCreate=false"
        }
    }
    data object Calculs : AppDestination("calculs")
    data object Reminders : AppDestination("reminders") {
        const val ROUTE_PATTERN = "reminders?openCreate={openCreate}"
        fun createRoute(openCreate: Boolean = false): String = "reminders?openCreate=$openCreate"
    }
    data object Notes : AppDestination("notes")
    data object NoteDetail : AppDestination("note_detail") {
        const val ROUTE_PATTERN = "note_detail/{noteId}"
        fun createRoute(noteId: String): String = "note_detail/$noteId"
    }
    data object NewCalculation : AppDestination("calculation/new") {
        const val ROUTE_PATTERN = "calculation/new?groupId={groupId}&type={type}&currency={currency}&title={title}&templateId={templateId}"
        fun createRoute(
            groupId: String? = null,
            type: String = "PERSONNEL",
            currency: String = "DIRHAM",
            title: String = "",
            templateId: String? = null
        ): String {
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedTemplateId = templateId?.let { java.net.URLEncoder.encode(it, "UTF-8") }
            return buildString {
                append("calculation/new?")
                if (groupId != null) append("groupId=$groupId&")
                append("type=$type&currency=$currency&title=$encodedTitle")
                if (encodedTemplateId != null) append("&templateId=$encodedTemplateId")
            }
        }
        fun routeForGroup(groupId: String?): String =
            createRoute(groupId = groupId)
    }
    data class EditCalculation(val calculationId: String) : AppDestination("calculation/$calculationId") {
        companion object {
            const val ROUTE_PATTERN = "calculation/{calculationId}"
        }
    }
    data class GroupDetail(val groupId: String) : AppDestination("group/$groupId") {
        companion object {
            const val ROUTE_PATTERN = "group/{groupId}"
        }
    }
    data class MonthCalculations(val year: Int, val month: Int) : AppDestination("month_calculations/$year/$month") {
        companion object {
            const val ROUTE_PATTERN = "month_calculations/{year}/{month}"
        }
    }
}
