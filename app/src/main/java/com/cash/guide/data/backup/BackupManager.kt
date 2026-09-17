package com.cash.guide.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.cash.guide.data.db.CalculationDao
import com.cash.guide.data.db.CalculationEntity
import com.cash.guide.data.db.CalculationGroupDao
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.CalculationItemEntity
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.data.db.ChecklistDao
import com.cash.guide.data.db.ChecklistEntity
import com.cash.guide.data.db.ChecklistItemEntity
import com.cash.guide.data.db.ChecklistWithItems
import com.cash.guide.data.db.FinancialProfileDao
import com.cash.guide.data.db.FinancialProfileEntity
import com.cash.guide.data.db.HssabiDatabase
import com.cash.guide.data.db.NoteDao
import com.cash.guide.data.db.NoteEntity
import com.cash.guide.data.db.ReminderDao
import com.cash.guide.data.db.ReminderEntity
import com.cash.guide.data.db.SavingsDao
import com.cash.guide.data.db.SavingsDepositEntity
import com.cash.guide.data.db.SavingsGoalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
    val app: String = "SARF",
    val format: String = "SARF_BACKUP",
    val version: Int = 2,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val groups: List<CalculationGroupEntity> = emptyList(),
    val calculations: List<CalculationWithItems> = emptyList(),
    val checklists: List<ChecklistWithItems> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val savingsDeposits: List<SavingsDepositEntity> = emptyList(),
    val financialProfiles: List<FinancialProfileEntity> = emptyList()
) {
    val totalCalculations: Int get() = calculations.size
    val totalGroups: Int get() = groups.size
    val totalChecklists: Int get() = checklists.size
    val totalNotes: Int get() = notes.size
    val totalReminders: Int get() = reminders.size
    val totalSavingsGoals: Int get() = savingsGoals.size
}

class BackupManager(
    private val calculationDao: CalculationDao,
    private val groupDao: CalculationGroupDao,
    private val checklistDao: ChecklistDao? = null,
    private val noteDao: NoteDao? = null,
    private val reminderDao: ReminderDao? = null,
    private val savingsDao: SavingsDao? = null,
    private val financialProfileDao: FinancialProfileDao? = null
) {
    constructor(database: HssabiDatabase) : this(
        database.calculationDao(),
        database.calculationGroupDao(),
        database.checklistDao(),
        database.noteDao(),
        database.reminderDao(),
        database.savingsDao(),
        database.financialProfileDao()
    )

    suspend fun createBackupPayload(): BackupPayload = withContext(Dispatchers.IO) {
        val groups = groupDao.getAllGroups()
        val calculations = calculationDao.getAllSaved()
        val checklists = checklistDao?.getAll() ?: emptyList()
        val notes = noteDao?.getAllNotes() ?: emptyList()
        val reminders = reminderDao?.getAllRemindersList() ?: emptyList()
        val savingsGoals = savingsDao?.getAllGoalsList() ?: emptyList()
        val savingsDeposits = savingsDao?.getAllDepositsList() ?: emptyList()
        val financialProfiles = financialProfileDao?.getAllProfiles() ?: emptyList()

        BackupPayload(
            version = 2,
            exportedAtEpochMs = System.currentTimeMillis(),
            groups = groups,
            calculations = calculations,
            checklists = checklists,
            notes = notes,
            reminders = reminders,
            savingsGoals = savingsGoals,
            savingsDeposits = savingsDeposits,
            financialProfiles = financialProfiles
        )
    }

    fun serializeToJson(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("app", payload.app)
        root.put("format", payload.format)
        root.put("version", payload.version)
        root.put("exportedAtEpochMs", payload.exportedAtEpochMs)

        // 1. Groups
        val groupsArr = JSONArray()
        for (group in payload.groups) {
            val gObj = JSONObject()
            gObj.put("id", group.id)
            gObj.put("name", group.name)
            gObj.put("colorHex", group.colorHex)
            gObj.put("category", group.category)
            gObj.put("createdAtEpochMs", group.createdAtEpochMs)
            gObj.put("updatedAtEpochMs", group.updatedAtEpochMs)
            groupsArr.put(gObj)
        }
        root.put("groups", groupsArr)

        // 2. Calculations
        val calcsArr = JSONArray()
        for (item in payload.calculations) {
            val calcObj = JSONObject()
            val c = item.calculation
            val cEntity = JSONObject()
            cEntity.put("id", c.id)
            cEntity.put("title", c.title)
            cEntity.put("currency", c.currency)
            cEntity.put("createdAtEpochMs", c.createdAtEpochMs)
            cEntity.put("updatedAtEpochMs", c.updatedAtEpochMs)
            cEntity.put("status", c.status)
            if (c.note != null) cEntity.put("note", c.note)
            if (c.editingCalculationId != null) cEntity.put("editingCalculationId", c.editingCalculationId)
            if (c.groupId != null) cEntity.put("groupId", c.groupId)
            cEntity.put("paymentStatus", c.paymentStatus)
            cEntity.put("calcType", c.calcType)
            if (c.dueDateEpochMs != null) cEntity.put("dueDateEpochMs", c.dueDateEpochMs)
            cEntity.put("reminderEnabled", c.reminderEnabled)
            if (c.reminderTimeEpochMs != null) cEntity.put("reminderTimeEpochMs", c.reminderTimeEpochMs)
            calcObj.put("calculation", cEntity)

            val itemsArr = JSONArray()
            for (row in item.items) {
                val rObj = JSONObject()
                rObj.put("id", row.id)
                rObj.put("calculationId", row.calculationId)
                rObj.put("label", row.label)
                rObj.put("amountCentimes", row.amountCentimes)
                if (row.rawExpression != null) rObj.put("rawExpression", row.rawExpression)
                rObj.put("position", row.position)
                rObj.put("createdAtEpochMs", row.createdAtEpochMs)
                rObj.put("updatedAtEpochMs", row.updatedAtEpochMs)
                itemsArr.put(rObj)
            }
            calcObj.put("items", itemsArr)
            calcsArr.put(calcObj)
        }
        root.put("calculations", calcsArr)

        // 3. Checklists
        val checklistsArr = JSONArray()
        for (cl in payload.checklists) {
            val clObj = JSONObject()
            val c = cl.checklist
            val cEntity = JSONObject()
            cEntity.put("id", c.id)
            cEntity.put("title", c.title)
            cEntity.put("createdAtEpochMs", c.createdAtEpochMs)
            cEntity.put("updatedAtEpochMs", c.updatedAtEpochMs)
            if (c.groupId != null) cEntity.put("groupId", c.groupId)
            clObj.put("checklist", cEntity)

            val itemsArr = JSONArray()
            for (item in cl.items) {
                val itObj = JSONObject()
                itObj.put("id", item.id)
                itObj.put("checklistId", item.checklistId)
                itObj.put("text", item.text)
                itObj.put("isChecked", item.isChecked)
                itObj.put("position", item.position)
                itObj.put("createdAtEpochMs", item.createdAtEpochMs)
                itemsArr.put(itObj)
            }
            clObj.put("items", itemsArr)
            checklistsArr.put(clObj)
        }
        root.put("checklists", checklistsArr)

        // 4. Notes
        val notesArr = JSONArray()
        for (note in payload.notes) {
            val nObj = JSONObject()
            nObj.put("id", note.id)
            nObj.put("title", note.title)
            nObj.put("content", note.content)
            nObj.put("colorTag", note.colorTag)
            nObj.put("isPinned", note.isPinned)
            nObj.put("createdAtEpochMs", note.createdAtEpochMs)
            nObj.put("updatedAtEpochMs", note.updatedAtEpochMs)
            if (note.groupId != null) nObj.put("groupId", note.groupId)
            notesArr.put(nObj)
        }
        root.put("notes", notesArr)

        // 5. Reminders
        val remindersArr = JSONArray()
        for (rem in payload.reminders) {
            val rObj = JSONObject()
            rObj.put("id", rem.id)
            rObj.put("title", rem.title)
            rObj.put("description", rem.description)
            rObj.put("targetEpochMs", rem.targetEpochMs)
            rObj.put("recurrenceType", rem.recurrenceType)
            rObj.put("repeatDays", rem.repeatDays)
            rObj.put("timeHour", rem.timeHour)
            rObj.put("timeMinute", rem.timeMinute)
            rObj.put("isEnabled", rem.isEnabled)
            rObj.put("isCompleted", rem.isCompleted)
            rObj.put("colorTag", rem.colorTag)
            if (rem.calculationId != null) rObj.put("calculationId", rem.calculationId)
            rObj.put("createdAtEpochMs", rem.createdAtEpochMs)
            rObj.put("updatedAtEpochMs", rem.updatedAtEpochMs)
            remindersArr.put(rObj)
        }
        root.put("reminders", remindersArr)

        // 6. Savings Goals
        val goalsArr = JSONArray()
        for (g in payload.savingsGoals) {
            val gObj = JSONObject()
            gObj.put("id", g.id)
            gObj.put("title", g.title)
            gObj.put("targetAmountCentimes", g.targetAmountCentimes)
            gObj.put("currentAmountCentimes", g.currentAmountCentimes)
            gObj.put("monthlyContributionCentimes", g.monthlyContributionCentimes)
            if (g.targetDateEpochMs != null) gObj.put("targetDateEpochMs", g.targetDateEpochMs)
            gObj.put("currency", g.currency)
            gObj.put("colorTag", g.colorTag)
            gObj.put("icon", g.icon)
            gObj.put("isCompleted", g.isCompleted)
            gObj.put("targetMonths", g.targetMonths)
            gObj.put("monthlySalaryCentimes", g.monthlySalaryCentimes)
            gObj.put("essentialBracket", g.essentialBracket)
            gObj.put("leisureCategory", g.leisureCategory)
            gObj.put("savingsStyle", g.savingsStyle)
            gObj.put("initialAmountCentimes", g.initialAmountCentimes)
            gObj.put("leakDailyCostCentimes", g.leakDailyCostCentimes)
            gObj.put("leakDaysPerWeek", g.leakDaysPerWeek)
            gObj.put("createdAtEpochMs", g.createdAtEpochMs)
            gObj.put("updatedAtEpochMs", g.updatedAtEpochMs)
            goalsArr.put(gObj)
        }
        root.put("savingsGoals", goalsArr)

        // 7. Savings Deposits
        val depositsArr = JSONArray()
        for (d in payload.savingsDeposits) {
            val dObj = JSONObject()
            dObj.put("id", d.id)
            dObj.put("goalId", d.goalId)
            dObj.put("amountCentimes", d.amountCentimes)
            dObj.put("note", d.note)
            dObj.put("dateEpochMs", d.dateEpochMs)
            depositsArr.put(dObj)
        }
        root.put("savingsDeposits", depositsArr)

        // 8. Financial Profiles
        val profilesArr = JSONArray()
        for (fp in payload.financialProfiles) {
            val pObj = JSONObject()
            pObj.put("id", fp.id)
            pObj.put("goalId", fp.goalId)
            pObj.put("goalOwnership", fp.goalOwnership)
            pObj.put("dependentsCount", fp.dependentsCount)
            pObj.put("familyCommitmentCentimes", fp.familyCommitmentCentimes)
            pObj.put("netMonthlyIncomeCentimes", fp.netMonthlyIncomeCentimes)
            pObj.put("incomeType", fp.incomeType)
            pObj.put("incomeLowestCentimes", fp.incomeLowestCentimes)
            pObj.put("incomeHighestCentimes", fp.incomeHighestCentimes)
            pObj.put("additionalIncomeSources", fp.additionalIncomeSources)
            pObj.put("additionalIncomeRegularity", fp.additionalIncomeRegularity)
            pObj.put("housingCentimes", fp.housingCentimes)
            pObj.put("utilitiesCentimes", fp.utilitiesCentimes)
            pObj.put("internetPhoneCentimes", fp.internetPhoneCentimes)
            pObj.put("groceriesCentimes", fp.groceriesCentimes)
            pObj.put("workTransportCentimes", fp.workTransportCentimes)
            pObj.put("workTransportType", fp.workTransportType)
            pObj.put("healthCentimes", fp.healthCentimes)
            pObj.put("educationCentimes", fp.educationCentimes)
            pObj.put("insuranceCentimes", fp.insuranceCentimes)
            pObj.put("otherEssentialsCentimes", fp.otherEssentialsCentimes)
            pObj.put("debtType", fp.debtType)
            pObj.put("debtPaymentsCentimes", fp.debtPaymentsCentimes)
            pObj.put("paymentDelayFrequency", fp.paymentDelayFrequency)
            pObj.put("endOfMonthBorrowFrequency", fp.endOfMonthBorrowFrequency)
            pObj.put("emergencyFundCentimes", fp.emergencyFundCentimes)
            pObj.put("emergencyResponse", fp.emergencyResponse)
            pObj.put("emergencyFundLocation", fp.emergencyFundLocation)
            pObj.put("monthEndSituation", fp.monthEndSituation)
            pObj.put("spendingAwareness", fp.spendingAwareness)
            pObj.put("spendingTrackingHabit", fp.spendingTrackingHabit)
            pObj.put("selectedLeaks", fp.selectedLeaks)
            pObj.put("leakDetails", fp.leakDetails)
            pObj.put("seasonalExpenses", fp.seasonalExpenses)
            pObj.put("savingTiming", fp.savingTiming)
            pObj.put("hasStandingTransfer", fp.hasStandingTransfer)
            pObj.put("bonusHandling", fp.bonusHandling)
            pObj.put("comfortSavingCentimes", fp.comfortSavingCentimes)
            pObj.put("minimumSavingCentimes", fp.minimumSavingCentimes)
            pObj.put("purchaseDecisionStyle", fp.purchaseDecisionStyle)
            pObj.put("discountTriggerBuying", fp.discountTriggerBuying)
            pObj.put("usesShoppingList", fp.usesShoppingList)
            pObj.put("paymentMethodThatMakesSpendMore", fp.paymentMethodThatMakesSpendMore)
            pObj.put("userProtectedPreferences", fp.userProtectedPreferences)
            pObj.put("goalImportance", fp.goalImportance)
            pObj.put("deadlineFlexibility", fp.deadlineFlexibility)
            pObj.put("willingToIncreaseIncome", fp.willingToIncreaseIncome)
            pObj.put("willingToCutFlexible", fp.willingToCutFlexible)
            pObj.put("computedTags", fp.computedTags)
            pObj.put("answersVersion", fp.answersVersion)
            pObj.put("createdAtEpochMs", fp.createdAtEpochMs)
            pObj.put("updatedAtEpochMs", fp.updatedAtEpochMs)
            profilesArr.put(pObj)
        }
        root.put("financialProfiles", profilesArr)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): Result<BackupPayload> = runCatching {
        val root = JSONObject(jsonString)
        val app = root.optString("app", "")
        val format = root.optString("format", "")
        if (format != "SARF_BACKUP" && app != "SARF") {
            throw IllegalArgumentException("Format de fichier non reconnu")
        }
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAtEpochMs", System.currentTimeMillis())

        // 1. Groups
        val groupsList = mutableListOf<CalculationGroupEntity>()
        val groupsArr = root.optJSONArray("groups")
        if (groupsArr != null) {
            for (i in 0 until groupsArr.length()) {
                val gObj = groupsArr.getJSONObject(i)
                groupsList.add(
                    CalculationGroupEntity(
                        id = gObj.getString("id"),
                        name = gObj.getString("name"),
                        colorHex = gObj.optString("colorHex", "#E5A93C"),
                        category = gObj.optString("category", "CALCULATIONS"),
                        createdAtEpochMs = gObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        updatedAtEpochMs = gObj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }

        // 2. Calculations
        val calcsList = mutableListOf<CalculationWithItems>()
        val calcsArr = root.optJSONArray("calculations")
        if (calcsArr != null) {
            for (i in 0 until calcsArr.length()) {
                val obj = calcsArr.getJSONObject(i)
                val cObj = obj.getJSONObject("calculation")
                val calcEntity = CalculationEntity(
                    id = cObj.getString("id"),
                    title = cObj.optString("title", ""),
                    currency = cObj.optString("currency", "DIRHAM"),
                    createdAtEpochMs = cObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    updatedAtEpochMs = cObj.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                    status = cObj.optString("status", "SAVED"),
                    note = if (cObj.has("note") && !cObj.isNull("note")) cObj.getString("note") else null,
                    editingCalculationId = if (cObj.has("editingCalculationId") && !cObj.isNull("editingCalculationId")) cObj.getString("editingCalculationId") else null,
                    groupId = if (cObj.has("groupId") && !cObj.isNull("groupId")) cObj.getString("groupId") else null,
                    paymentStatus = cObj.optString("paymentStatus", "PAID"),
                    calcType = cObj.optString("calcType", if (cObj.optString("paymentStatus", "PAID") == "UNPAID") "CREDIT" else "PERSONNEL"),
                    dueDateEpochMs = if (cObj.has("dueDateEpochMs") && !cObj.isNull("dueDateEpochMs")) cObj.getLong("dueDateEpochMs") else null,
                    reminderEnabled = cObj.optBoolean("reminderEnabled", false),
                    reminderTimeEpochMs = if (cObj.has("reminderTimeEpochMs") && !cObj.isNull("reminderTimeEpochMs")) cObj.getLong("reminderTimeEpochMs") else null
                )

                val itemsList = mutableListOf<CalculationItemEntity>()
                val itemsArr = obj.optJSONArray("items")
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val itObj = itemsArr.getJSONObject(j)
                        itemsList.add(
                            CalculationItemEntity(
                                id = itObj.getString("id"),
                                calculationId = itObj.getString("calculationId"),
                                label = itObj.optString("label", ""),
                                amountCentimes = itObj.getLong("amountCentimes"),
                                rawExpression = if (itObj.has("rawExpression") && !itObj.isNull("rawExpression")) itObj.getString("rawExpression") else null,
                                position = itObj.optInt("position", j),
                                createdAtEpochMs = itObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                                updatedAtEpochMs = itObj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                            )
                        )
                    }
                }
                calcsList.add(CalculationWithItems(calculation = calcEntity, items = itemsList))
            }
        }

        // 3. Checklists
        val checklistsList = mutableListOf<ChecklistWithItems>()
        val checklistsArr = root.optJSONArray("checklists")
        if (checklistsArr != null) {
            for (i in 0 until checklistsArr.length()) {
                val clObj = checklistsArr.getJSONObject(i)
                val cObj = clObj.getJSONObject("checklist")
                val checklistEntity = ChecklistEntity(
                    id = cObj.getString("id"),
                    title = cObj.optString("title", ""),
                    createdAtEpochMs = cObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                    updatedAtEpochMs = cObj.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                    groupId = if (cObj.has("groupId") && !cObj.isNull("groupId")) cObj.getString("groupId") else null
                )

                val itemsList = mutableListOf<ChecklistItemEntity>()
                val itemsArr = clObj.optJSONArray("items")
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val itObj = itemsArr.getJSONObject(j)
                        itemsList.add(
                            ChecklistItemEntity(
                                id = itObj.getString("id"),
                                checklistId = itObj.getString("checklistId"),
                                text = itObj.optString("text", ""),
                                isChecked = itObj.optBoolean("isChecked", false),
                                position = itObj.optInt("position", j),
                                createdAtEpochMs = itObj.optLong("createdAtEpochMs", System.currentTimeMillis())
                            )
                        )
                    }
                }
                checklistsList.add(ChecklistWithItems(checklist = checklistEntity, items = itemsList))
            }
        }

        // 4. Notes
        val notesList = mutableListOf<NoteEntity>()
        val notesArr = root.optJSONArray("notes")
        if (notesArr != null) {
            for (i in 0 until notesArr.length()) {
                val nObj = notesArr.getJSONObject(i)
                notesList.add(
                    NoteEntity(
                        id = nObj.getString("id"),
                        title = nObj.optString("title", ""),
                        content = nObj.optString("content", ""),
                        colorTag = nObj.optString("colorTag", "DEFAULT"),
                        isPinned = nObj.optBoolean("isPinned", false),
                        createdAtEpochMs = nObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        updatedAtEpochMs = nObj.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                        groupId = if (nObj.has("groupId") && !nObj.isNull("groupId")) nObj.getString("groupId") else null
                    )
                )
            }
        }

        // 5. Reminders
        val remindersList = mutableListOf<ReminderEntity>()
        val remindersArr = root.optJSONArray("reminders")
        if (remindersArr != null) {
            for (i in 0 until remindersArr.length()) {
                val rObj = remindersArr.getJSONObject(i)
                remindersList.add(
                    ReminderEntity(
                        id = rObj.getString("id"),
                        title = rObj.optString("title", ""),
                        description = rObj.optString("description", ""),
                        targetEpochMs = rObj.getLong("targetEpochMs"),
                        recurrenceType = rObj.optString("recurrenceType", "ONCE"),
                        repeatDays = rObj.optString("repeatDays", ""),
                        timeHour = rObj.optInt("timeHour", 9),
                        timeMinute = rObj.optInt("timeMinute", 0),
                        isEnabled = rObj.optBoolean("isEnabled", true),
                        isCompleted = rObj.optBoolean("isCompleted", false),
                        colorTag = rObj.optString("colorTag", "BLUE"),
                        calculationId = if (rObj.has("calculationId") && !rObj.isNull("calculationId")) rObj.getString("calculationId") else null,
                        createdAtEpochMs = rObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        updatedAtEpochMs = rObj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }

        // 6. Savings Goals
        val goalsList = mutableListOf<SavingsGoalEntity>()
        val goalsArr = root.optJSONArray("savingsGoals")
        if (goalsArr != null) {
            for (i in 0 until goalsArr.length()) {
                val gObj = goalsArr.getJSONObject(i)
                goalsList.add(
                    SavingsGoalEntity(
                        id = gObj.getString("id"),
                        title = gObj.optString("title", ""),
                        targetAmountCentimes = gObj.getLong("targetAmountCentimes"),
                        currentAmountCentimes = gObj.optLong("currentAmountCentimes", 0L),
                        monthlyContributionCentimes = gObj.optLong("monthlyContributionCentimes", 0L),
                        targetDateEpochMs = if (gObj.has("targetDateEpochMs") && !gObj.isNull("targetDateEpochMs")) gObj.getLong("targetDateEpochMs") else null,
                        currency = gObj.optString("currency", "DIRHAM"),
                        colorTag = gObj.optString("colorTag", "BLUE"),
                        icon = gObj.optString("icon", "STAR"),
                        isCompleted = gObj.optBoolean("isCompleted", false),
                        targetMonths = gObj.optInt("targetMonths", 24),
                        monthlySalaryCentimes = gObj.optLong("monthlySalaryCentimes", 0L),
                        essentialBracket = gObj.optString("essentialBracket", "MEDIUM"),
                        leisureCategory = gObj.optString("leisureCategory", "CAFE"),
                        savingsStyle = gObj.optString("savingsStyle", "BALANCED"),
                        initialAmountCentimes = gObj.optLong("initialAmountCentimes", 0L),
                        leakDailyCostCentimes = gObj.optLong("leakDailyCostCentimes", 2500L),
                        leakDaysPerWeek = gObj.optInt("leakDaysPerWeek", 6),
                        createdAtEpochMs = gObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        updatedAtEpochMs = gObj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }

        // 7. Savings Deposits
        val depositsList = mutableListOf<SavingsDepositEntity>()
        val depositsArr = root.optJSONArray("savingsDeposits")
        if (depositsArr != null) {
            for (i in 0 until depositsArr.length()) {
                val dObj = depositsArr.getJSONObject(i)
                depositsList.add(
                    SavingsDepositEntity(
                        id = dObj.getString("id"),
                        goalId = dObj.getString("goalId"),
                        amountCentimes = dObj.getLong("amountCentimes"),
                        note = dObj.optString("note", ""),
                        dateEpochMs = dObj.getLong("dateEpochMs")
                    )
                )
            }
        }

        // 8. Financial Profiles
        val profilesList = mutableListOf<FinancialProfileEntity>()
        val profilesArr = root.optJSONArray("financialProfiles")
        if (profilesArr != null) {
            for (i in 0 until profilesArr.length()) {
                val pObj = profilesArr.getJSONObject(i)
                profilesList.add(
                    FinancialProfileEntity(
                        id = pObj.getString("id"),
                        goalId = pObj.getString("goalId"),
                        goalOwnership = pObj.optString("goalOwnership", "GOAL_SOLO"),
                        dependentsCount = pObj.optInt("dependentsCount", 0),
                        familyCommitmentCentimes = pObj.optLong("familyCommitmentCentimes", 0L),
                        netMonthlyIncomeCentimes = pObj.optLong("netMonthlyIncomeCentimes", 0L),
                        incomeType = pObj.optString("incomeType", "INCOME_STABLE"),
                        incomeLowestCentimes = pObj.optLong("incomeLowestCentimes", 0L),
                        incomeHighestCentimes = pObj.optLong("incomeHighestCentimes", 0L),
                        additionalIncomeSources = pObj.optString("additionalIncomeSources", "[]"),
                        additionalIncomeRegularity = pObj.optString("additionalIncomeRegularity", ""),
                        housingCentimes = pObj.optLong("housingCentimes", 0L),
                        utilitiesCentimes = pObj.optLong("utilitiesCentimes", 0L),
                        internetPhoneCentimes = pObj.optLong("internetPhoneCentimes", 0L),
                        groceriesCentimes = pObj.optLong("groceriesCentimes", 0L),
                        workTransportCentimes = pObj.optLong("workTransportCentimes", 0L),
                        workTransportType = pObj.optString("workTransportType", "TRANSIT"),
                        healthCentimes = pObj.optLong("healthCentimes", 0L),
                        educationCentimes = pObj.optLong("educationCentimes", 0L),
                        insuranceCentimes = pObj.optLong("insuranceCentimes", 0L),
                        otherEssentialsCentimes = pObj.optLong("otherEssentialsCentimes", 0L),
                        debtType = pObj.optString("debtType", "DEBT_NONE"),
                        debtPaymentsCentimes = pObj.optLong("debtPaymentsCentimes", 0L),
                        paymentDelayFrequency = pObj.optInt("paymentDelayFrequency", 0),
                        endOfMonthBorrowFrequency = pObj.optInt("endOfMonthBorrowFrequency", 0),
                        emergencyFundCentimes = pObj.optLong("emergencyFundCentimes", 0L),
                        emergencyResponse = pObj.optString("emergencyResponse", "UNKNOWN"),
                        emergencyFundLocation = pObj.optString("emergencyFundLocation", "NONE"),
                        monthEndSituation = pObj.optString("monthEndSituation", "END_LITTLE"),
                        spendingAwareness = pObj.optString("spendingAwareness", "APPROXIMATELY"),
                        spendingTrackingHabit = pObj.optString("spendingTrackingHabit", "SOMETIMES"),
                        selectedLeaks = pObj.optString("selectedLeaks", "[]"),
                        leakDetails = pObj.optString("leakDetails", "{}"),
                        seasonalExpenses = pObj.optString("seasonalExpenses", "[]"),
                        savingTiming = pObj.optString("savingTiming", "SAVE_END"),
                        hasStandingTransfer = pObj.optInt("hasStandingTransfer", 0),
                        bonusHandling = pObj.optString("bonusHandling", "DEPENDS"),
                        comfortSavingCentimes = pObj.optLong("comfortSavingCentimes", 0L),
                        minimumSavingCentimes = pObj.optLong("minimumSavingCentimes", 0L),
                        purchaseDecisionStyle = pObj.optString("purchaseDecisionStyle", "WAIT_A_BIT"),
                        discountTriggerBuying = pObj.optInt("discountTriggerBuying", 1),
                        usesShoppingList = pObj.optString("usesShoppingList", "SOMETIMES"),
                        paymentMethodThatMakesSpendMore = pObj.optString("paymentMethodThatMakesSpendMore", "CARD"),
                        userProtectedPreferences = pObj.optString("userProtectedPreferences", "[]"),
                        goalImportance = pObj.optString("goalImportance", "IMPORTANT"),
                        deadlineFlexibility = pObj.optString("deadlineFlexibility", "FLEXIBLE_3M"),
                        willingToIncreaseIncome = pObj.optInt("willingToIncreaseIncome", 0),
                        willingToCutFlexible = pObj.optString("willingToCutFlexible", "A_LITTLE"),
                        computedTags = pObj.optString("computedTags", "[]"),
                        answersVersion = pObj.optInt("answersVersion", 1),
                        createdAtEpochMs = pObj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        updatedAtEpochMs = pObj.optLong("updatedAtEpochMs", System.currentTimeMillis())
                    )
                )
            }
        }

        BackupPayload(
            app = app,
            format = format,
            version = version,
            exportedAtEpochMs = exportedAt,
            groups = groupsList,
            calculations = calcsList,
            checklists = checklistsList,
            notes = notesList,
            reminders = remindersList,
            savingsGoals = goalsList,
            savingsDeposits = depositsList,
            financialProfiles = profilesList
        )
    }

    suspend fun writeBackupToStream(outputStream: OutputStream): Unit = withContext(Dispatchers.IO) {
        val payload = createBackupPayload()
        val json = serializeToJson(payload)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
            writer.flush()
        }
    }

    suspend fun readBackupFromStream(inputStream: InputStream): Result<BackupPayload> = withContext(Dispatchers.IO) {
        runCatching {
            val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseFromJson(json).getOrThrow()
        }
    }

    suspend fun createShareableBackupFile(context: Context): Uri = withContext(Dispatchers.IO) {
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val dateSuffix = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val file = File(backupDir, "sarf_backup_${dateSuffix}.calc")

        val payload = createBackupPayload()
        val json = serializeToJson(payload)
        file.writeText(json, Charsets.UTF_8)

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun restore(payload: BackupPayload, replaceExisting: Boolean): Unit = withContext(Dispatchers.IO) {
        groupDao.restoreGroups(payload.groups, replaceExisting)
        calculationDao.restoreCalculations(payload.calculations, replaceExisting)
        checklistDao?.restoreChecklists(payload.checklists, replaceExisting)
        noteDao?.restoreNotes(payload.notes, replaceExisting)
        reminderDao?.restoreReminders(payload.reminders, replaceExisting)
        savingsDao?.restoreSavings(payload.savingsGoals, payload.savingsDeposits, replaceExisting)
        financialProfileDao?.restoreProfiles(payload.financialProfiles, replaceExisting)
    }
}

