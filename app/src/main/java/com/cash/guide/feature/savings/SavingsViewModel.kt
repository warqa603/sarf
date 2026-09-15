package com.cash.guide.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.SavingsRepository
import com.cash.guide.data.db.SavingsDepositEntity
import com.cash.guide.data.db.SavingsGoalEntity
import com.cash.guide.domain.ai.GeminiDarijaService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SavingsTab {
    PLAN,
    DIAGNOSTIC,
    TIPS
}

data class WizardFormState(
    val step: Int = 1,
    val goalPreset: String = "CAR",
    val customTitle: String = "",
    val targetAmount: Double = 80000.0,
    val durationMonths: Int = 24,
    val salaryBracket: String = "MED",
    val salaryAmount: Double = 6000.0,
    val essentialBracket: String = "MEDIUM",
    val leisureCategory: String = "CAFE",
    val leakDailyCost: Double = 25.0,
    val leakDaysPerWeek: Int = 6,
    val savingsStyle: String = "BALANCED",
    val initialAmount: Double = 0.0
) {
    val displayTitle: String
        get() = when (goalPreset) {
            "CAR" -> "شراء سيارة"
            "HOUSE" -> "تسبيق سكن / دار"
            "EMERGENCY" -> "صندوق الطوارئ"
            "PROJECT" -> "مشروع تجاري"
            "EVENT" -> "مناسبة / سفر"
            else -> if (customTitle.isNotBlank()) customTitle else "هدف مالي"
        }
}

data class PlanDiagnosis(
    val monthlyRequired: Double,
    val dailyRequired: Double,
    val salary: Double,
    val essentialsEstimated: Double,
    val leisureEstimated: Double,
    val monthlySavingsPotential: Double,
    val pressureLevel: String,
    val whatToKeep: String,
    val whatToCut: String,
    val alternativeSuggestion: String?,
    val suggestedMonths: Int = 0,
    val leakInfo: SpendingLeakInfo,
    val shockNumbers: SavingsKnowledgeBase.ShockCalculationResult,
    val goalTrap: GoalTrapInfo,
    val vitalPillars: List<VitalPillarInfo>,
    val austerityStepsAr: List<String>,
    val austerityStepsFr: List<String>,
    val austeritySteps: List<String> = austerityStepsAr
)

data class SavingsUiState(
    val goals: List<SavingsGoalEntity> = emptyList(),
    val activeGoal: SavingsGoalEntity? = null,
    val activeGoalDeposits: List<SavingsDepositEntity> = emptyList(),
    val totalSavedCentimes: Long = 0L,
    val selectedTab: SavingsTab = SavingsTab.PLAN,
    val isWizardOpen: Boolean = false,
    val wizardForm: WizardFormState = WizardFormState(),
    val diagnosis: PlanDiagnosis? = null,
    val depositTargetGoal: SavingsGoalEntity? = null,
    val aiCoachAdvice: String? = null,
    val isAiCoachLoading: Boolean = false,
    // New: full diagnostic result from questionnaire engine
    val fullDiagnosticResult: FullDiagnosticResult? = null,
    val questionnaireAnswers: QuestionnaireAnswers? = null,
    // Questionnaire flow
    val isQuestionnaireOpen: Boolean = false,
    val questionnaireState: QuestionnaireAnswers = QuestionnaireAnswers(),
    // Simulator
    val isSimulatorOpen: Boolean = false,
    val simulatorState: SimulatorState = SimulatorState(),
    // Monthly check-in
    val isCheckInOpen: Boolean = false
)

class SavingsViewModel(
    private val repository: SavingsRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(SavingsTab.PLAN)
    val selectedTab: StateFlow<SavingsTab> = _selectedTab.asStateFlow()

    private val _isWizardOpen = MutableStateFlow(false)
    val isWizardOpen: StateFlow<Boolean> = _isWizardOpen.asStateFlow()

    private val _wizardForm = MutableStateFlow(WizardFormState())
    val wizardForm: StateFlow<WizardFormState> = _wizardForm.asStateFlow()

    private val _depositTargetGoal = MutableStateFlow<SavingsGoalEntity?>(null)
    val depositTargetGoal: StateFlow<SavingsGoalEntity?> = _depositTargetGoal.asStateFlow()

    private val _selectedGoalId = MutableStateFlow<String?>(null)
    val selectedGoalId: StateFlow<String?> = _selectedGoalId.asStateFlow()

    private val _aiCoachAdvice = MutableStateFlow<String?>(null)
    val aiCoachAdvice: StateFlow<String?> = _aiCoachAdvice.asStateFlow()

    private val _isAiCoachLoading = MutableStateFlow(false)
    val isAiCoachLoading: StateFlow<Boolean> = _isAiCoachLoading.asStateFlow()

    // Questionnaire state
    private val _isQuestionnaireOpen = MutableStateFlow(false)
    private val _questionnaireState = MutableStateFlow(QuestionnaireAnswers())
    private val _fullDiagnosticResult = MutableStateFlow<FullDiagnosticResult?>(null)
    private val _questionnaireAnswers = MutableStateFlow<QuestionnaireAnswers?>(null)

    // Simulator state
    private val _isSimulatorOpen = MutableStateFlow(false)
    private val _simulatorState = MutableStateFlow(SimulatorState())

    // Check-in state
    private val _isCheckInOpen = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeGoalDepositsFlow: Flow<List<SavingsDepositEntity>> = combine(
        repository.allGoals,
        _selectedGoalId
    ) { goals, selId ->
        val active = goals.firstOrNull { it.id == selId }
            ?: goals.firstOrNull { !it.isCompleted }
            ?: goals.firstOrNull()
        active
    }.flatMapLatest { goal ->
        if (goal != null) repository.getDepositsForGoal(goal.id)
        else flowOf(emptyList())
    }

    /** Observe active goal's profile reactively */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeProfileFlow = combine(
        repository.allGoals,
        _selectedGoalId
    ) { goals, selId ->
        val active = goals.firstOrNull { it.id == selId }
            ?: goals.firstOrNull { !it.isCompleted }
            ?: goals.firstOrNull()
        active?.id
    }.flatMapLatest { goalId ->
        if (goalId != null) repository.getProfileForGoalFlow(goalId)
        else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SavingsUiState> = combine(
        listOf(
            repository.allGoals,
            repository.totalSavedCentimes,
            _selectedTab,
            _isWizardOpen,
            _wizardForm,
            _depositTargetGoal,
            _selectedGoalId,
            activeGoalDepositsFlow,
            _aiCoachAdvice,
            _isAiCoachLoading,
            _fullDiagnosticResult,
            _questionnaireAnswers,
            _isQuestionnaireOpen,
            _questionnaireState,
            _isSimulatorOpen,
            _simulatorState,
            _isCheckInOpen
        )
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val goals = args[0] as List<SavingsGoalEntity>
        val totalSaved = args[1] as Long
        val tab = args[2] as SavingsTab
        val isWizard = args[3] as Boolean
        val wizard = args[4] as WizardFormState
        val depGoal = args[5] as SavingsGoalEntity?
        val selId = args[6] as String?
        @Suppress("UNCHECKED_CAST")
        val deposits = args[7] as List<SavingsDepositEntity>
        val coachAdvice = args[8] as String?
        val isCoachLoading = args[9] as Boolean
        val fullDiag = args[10] as FullDiagnosticResult?
        val qAnswers = args[11] as QuestionnaireAnswers?
        val isQOpen = args[12] as Boolean
        val qState = args[13] as QuestionnaireAnswers
        val isSimOpen = args[14] as Boolean
        val simState = args[15] as SimulatorState
        val isChkOpen = args[16] as Boolean

        val activeGoal = goals.firstOrNull { it.id == selId }
            ?: goals.firstOrNull { !it.isCompleted }
            ?: goals.firstOrNull()
        val diagnosis = activeGoal?.let { computeDiagnosis(it) }

        SavingsUiState(
            goals = goals,
            activeGoal = activeGoal,
            activeGoalDeposits = deposits,
            totalSavedCentimes = totalSaved,
            selectedTab = tab,
            isWizardOpen = isWizard,
            wizardForm = wizard,
            diagnosis = diagnosis,
            depositTargetGoal = depGoal,
            aiCoachAdvice = coachAdvice,
            isAiCoachLoading = isCoachLoading,
            fullDiagnosticResult = fullDiag,
            questionnaireAnswers = qAnswers,
            isQuestionnaireOpen = isQOpen,
            questionnaireState = qState,
            isSimulatorOpen = isSimOpen,
            simulatorState = simState,
            isCheckInOpen = isChkOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsUiState()
    )

    init {
        // Whenever active goal changes, load its profile and recompute full diagnostic
        viewModelScope.launch {
            combine(
                activeProfileFlow,
                repository.allGoals,
                _selectedGoalId
            ) { profileEntity, goals, selId ->
                val activeGoal = goals.firstOrNull { it.id == selId }
                    ?: goals.firstOrNull { !it.isCompleted }
                    ?: goals.firstOrNull()
                Pair(profileEntity, activeGoal)
            }.collect { (profileEntity, activeGoal) ->
                if (profileEntity != null && activeGoal != null) {
                    val answers = FinancialDiagnosticEngine.entityToAnswers(profileEntity)
                    _questionnaireAnswers.value = answers
                    _fullDiagnosticResult.value = FinancialDiagnosticEngine.buildResult(answers, activeGoal)
                } else {
                    _questionnaireAnswers.value = null
                    _fullDiagnosticResult.value = null
                }
            }
        }
    }

    fun selectGoal(goalId: String) {
        _selectedGoalId.value = goalId
    }

    fun selectTab(tab: SavingsTab) {
        _selectedTab.value = tab
    }

    fun startWizard() {
        _wizardForm.value = WizardFormState()
        _isWizardOpen.value = true
    }

    fun closeWizard() {
        _isWizardOpen.value = false
    }

    fun nextWizardStep() {
        val current = _wizardForm.value.step
        if (current < 7) {
            _wizardForm.value = _wizardForm.value.copy(step = current + 1)
        } else {
            finishWizard()
        }
    }

    fun prevWizardStep() {
        val current = _wizardForm.value.step
        if (current > 1) {
            _wizardForm.value = _wizardForm.value.copy(step = current - 1)
        } else {
            closeWizard()
        }
    }

    fun setWizardGoalPreset(preset: String, defaultAmount: Double) {
        val current = _wizardForm.value
        _wizardForm.value = current.copy(
            goalPreset = preset,
            targetAmount = if (preset != "CUSTOM") defaultAmount else current.targetAmount
        )
    }

    fun setWizardCustomTitle(title: String) {
        _wizardForm.value = _wizardForm.value.copy(customTitle = title)
    }

    fun setWizardTargetAmount(amount: Double) {
        _wizardForm.value = _wizardForm.value.copy(targetAmount = amount.coerceAtLeast(100.0))
    }

    fun setWizardDuration(months: Int) {
        _wizardForm.value = _wizardForm.value.copy(durationMonths = months.coerceIn(1, 120))
    }

    fun setWizardSalary(bracket: String, defaultAmount: Double) {
        _wizardForm.value = _wizardForm.value.copy(
            salaryBracket = bracket,
            salaryAmount = defaultAmount
        )
    }

    fun setWizardCustomSalary(amount: Double) {
        _wizardForm.value = _wizardForm.value.copy(salaryAmount = amount.coerceAtLeast(0.0))
    }

    fun setWizardEssentials(bracket: String) {
        _wizardForm.value = _wizardForm.value.copy(essentialBracket = bracket)
    }

    fun setWizardLeisure(category: String) {
        val defaultCost = SavingsKnowledgeBase.getLeak(category).defaultDailyCostDh
        val defaultDays = SavingsKnowledgeBase.getLeak(category).defaultDaysPerWeek
        _wizardForm.value = _wizardForm.value.copy(
            leisureCategory = category,
            leakDailyCost = defaultCost,
            leakDaysPerWeek = defaultDays
        )
    }

    fun setWizardLeakDailyCost(amount: Double) {
        _wizardForm.value = _wizardForm.value.copy(leakDailyCost = amount.coerceAtLeast(1.0))
    }

    fun setWizardLeakDaysPerWeek(days: Int) {
        _wizardForm.value = _wizardForm.value.copy(leakDaysPerWeek = days.coerceIn(1, 7))
    }

    fun setWizardSavingsStyle(style: String) {
        _wizardForm.value = _wizardForm.value.copy(savingsStyle = style)
    }

    fun setWizardInitialAmount(amount: Double) {
        _wizardForm.value = _wizardForm.value.copy(initialAmount = amount.coerceAtLeast(0.0))
    }

    fun finishWizard() {
        val form = _wizardForm.value
        val title = form.displayTitle
        val targetCentimes = (form.targetAmount * 100).toLong()
        val initialCentimes = (form.initialAmount * 100).toLong()
        val salaryCentimes = (form.salaryAmount * 100).toLong()
        val leakDailyCentimes = (form.leakDailyCost * 100).toLong()
        val targetEpochMs = Calendar.getInstance().apply {
            add(Calendar.MONTH, form.durationMonths)
        }.timeInMillis

        viewModelScope.launch {
            val newId = repository.createGoal(
                title = title,
                targetAmountCentimes = targetCentimes,
                initialAmountCentimes = initialCentimes,
                monthlyContributionCentimes = if (form.durationMonths > 0) targetCentimes / form.durationMonths else 0L,
                targetDateEpochMs = targetEpochMs,
                colorTag = when (form.goalPreset) {
                    "CAR" -> "BLUE"
                    "HOUSE" -> "GREEN"
                    "EMERGENCY" -> "AMBER"
                    "PROJECT" -> "PURPLE"
                    else -> "PINK"
                },
                targetMonths = form.durationMonths,
                monthlySalaryCentimes = salaryCentimes,
                essentialBracket = form.essentialBracket,
                leisureCategory = form.leisureCategory,
                savingsStyle = form.savingsStyle,
                leakDailyCostCentimes = leakDailyCentimes,
                leakDaysPerWeek = form.leakDaysPerWeek
            )
            _selectedGoalId.value = newId
            _isWizardOpen.value = false
            _selectedTab.value = SavingsTab.PLAN
        }
    }

    fun openDepositSheet(goal: SavingsGoalEntity) {
        _depositTargetGoal.value = goal
    }

    fun closeDepositSheet() {
        _depositTargetGoal.value = null
    }

    fun addDeposit(goalId: String, amountCentimes: Long, note: String = "") {
        viewModelScope.launch {
            repository.addDeposit(goalId, amountCentimes, note)
            closeDepositSheet()
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            if (_selectedGoalId.value == goalId) {
                _selectedGoalId.value = null
            }
            repository.deleteGoalWithProfile(goalId)
        }
    }

    fun deleteDeposit(depositId: String) {
        viewModelScope.launch {
            repository.deleteDeposit(depositId)
        }
    }

    fun applySuggestedDuration(goalId: String, newMonths: Int) {
        viewModelScope.launch {
            repository.updateGoalDuration(goalId, newMonths)
        }
    }

    // ── Questionnaire ─────────────────────────────────────────────────────────

    fun openQuestionnaire() {
        val existingAnswers = _questionnaireAnswers.value
        _questionnaireState.value = existingAnswers ?: QuestionnaireAnswers()
        _isQuestionnaireOpen.value = true
    }

    fun closeQuestionnaire() {
        _isQuestionnaireOpen.value = false
    }

    fun updateQuestionnaireAnswers(answers: QuestionnaireAnswers) {
        _questionnaireState.value = answers
    }

    fun submitQuestionnaire(goal: SavingsGoalEntity) {
        val answers = _questionnaireState.value
        viewModelScope.launch {
            val tags = FinancialDiagnosticEngine.buildTags(answers, goal)
            val existing = repository.getProfileForGoal(goal.id)
            if (existing != null) {
                val updated = FinancialDiagnosticEngine.answersToEntity(answers, goal.id, tags)
                    .copy(id = existing.id, createdAtEpochMs = existing.createdAtEpochMs)
                repository.updateFinancialProfile(updated)
            } else {
                val entity = FinancialDiagnosticEngine.answersToEntity(answers, goal.id, tags)
                repository.saveFinancialProfile(entity)
            }
            // Compute and cache result
            val result = FinancialDiagnosticEngine.buildResult(answers, goal)
            _fullDiagnosticResult.value = result
            _questionnaireAnswers.value = answers
            _isQuestionnaireOpen.value = false
        }
    }

    // ── Simulator ─────────────────────────────────────────────────────────────

    fun openSimulator() {
        _isSimulatorOpen.value = true
    }

    fun closeSimulator() {
        _isSimulatorOpen.value = false
    }

    fun updateSimulatorState(state: SimulatorState) {
        _simulatorState.value = state
    }

    // ── Monthly Check-In ──────────────────────────────────────────────────────

    fun openMonthlyCheckIn() {
        _isCheckInOpen.value = true
    }

    fun closeCheckIn() {
        _isCheckInOpen.value = false
    }

    fun submitCheckIn(goal: SavingsGoalEntity, checkIn: MonthlyCheckInAnswers) {
        viewModelScope.launch {
            // If income/obligations changed, flag the questionnaire as stale
            if (checkIn.incomeOrObligationsChanged) {
                // Open full questionnaire next time
                _questionnaireState.value = _questionnaireAnswers.value ?: QuestionnaireAnswers()
            }
            // If user saved something this month, log it as a deposit
            if (checkIn.actualSavedCentimes > 0) {
                repository.addDeposit(goal.id, checkIn.actualSavedCentimes, "فحص الشهر")
            }
            _isCheckInOpen.value = false
        }
    }

    // ── AI Coach ─────────────────────────────────────────────────────────────

    fun requestAiCoachAdvice(goal: SavingsGoalEntity, isRtl: Boolean) {
        if (_isAiCoachLoading.value) return
        _isAiCoachLoading.value = true
        viewModelScope.launch {
            val targetDh = goal.targetAmountCentimes / 100.0
            val salaryDh = if (goal.monthlySalaryCentimes > 0) goal.monthlySalaryCentimes / 100.0 else 6000.0
            val leakDailyDh = if (goal.leakDailyCostCentimes > 0) goal.leakDailyCostCentimes / 100.0 else 25.0
            val leakDays = if (goal.leakDaysPerWeek > 0) goal.leakDaysPerWeek else 6

            val aiResult = try {
                GeminiDarijaService.generateSavingsCoachAdvice(
                    goalTitle = goal.title,
                    targetAmountDh = targetDh,
                    targetMonths = goal.targetMonths,
                    monthlySalaryDh = salaryDh,
                    leakCategory = goal.leisureCategory,
                    leakDailyCostDh = leakDailyDh,
                    leakDaysPerWeek = leakDays,
                    savingsStyle = goal.savingsStyle,
                    isRtl = isRtl
                )
            } catch (e: Exception) {
                null
            }

            _aiCoachAdvice.value = aiResult ?: SavingsKnowledgeBase.generateDeterministicCoachVerdict(
                goalTitle = goal.title,
                targetAmountDh = targetDh,
                targetMonths = goal.targetMonths,
                monthlySalaryDh = salaryDh,
                leakCategory = goal.leisureCategory,
                dailyCostDh = leakDailyDh,
                daysPerWeek = leakDays,
                savingsStyle = goal.savingsStyle,
                isRtl = isRtl
            )
            _isAiCoachLoading.value = false
        }
    }

    fun clearAiCoachAdvice() {
        _aiCoachAdvice.value = null
    }

    // ── Legacy computeDiagnosis (kept for Plan tab backward compat) ───────────

    private fun computeDiagnosis(goal: SavingsGoalEntity): PlanDiagnosis {
        val target = goal.targetAmountCentimes / 100.0
        val current = goal.currentAmountCentimes / 100.0
        val months = goal.targetMonths.coerceAtLeast(1)
        val remaining = (target - current).coerceAtLeast(0.0)
        val monthlyReq = if (months > 0) remaining / months else remaining
        val dailyReq = monthlyReq / 30.0
        val salary = if (goal.monthlySalaryCentimes > 0) goal.monthlySalaryCentimes / 100.0 else 6000.0

        val leakDailyDh = if (goal.leakDailyCostCentimes > 0) goal.leakDailyCostCentimes / 100.0 else 25.0
        val leakDays = if (goal.leakDaysPerWeek > 0) goal.leakDaysPerWeek else 6
        val leakInfo = SavingsKnowledgeBase.getLeak(goal.leisureCategory)
        val shockNumbers = SavingsKnowledgeBase.computeShockNumbers(leakDailyDh, leakDays)
        val goalTrap = SavingsKnowledgeBase.getGoalTrap(
            when {
                goal.title.contains("سيارة", true) || goal.title.contains("voiture", true) -> "CAR"
                goal.title.contains("دار", true) || goal.title.contains("سكن", true) || goal.title.contains("maison", true) -> "HOUSE"
                goal.title.contains("طوارئ", true) || goal.title.contains("urgence", true) -> "EMERGENCY"
                goal.title.contains("مشروع", true) || goal.title.contains("projet", true) -> "PROJECT"
                else -> "EVENT"
            }
        )

        val essentialsRatio = when (goal.essentialBracket) {
            "LOW" -> 0.35
            "HIGH" -> 0.70
            else -> 0.50
        }
        val essentialsEstimated = salary * essentialsRatio

        val leisureRatio = when (goal.savingsStyle) {
            "TURBO" -> 0.15
            else -> 0.30
        }
        val leisureEstimated = salary * leisureRatio

        val pressureLevel = when {
            salary > 0 && (monthlyReq / salary) <= 0.25 -> "EASY"
            salary > 0 && (monthlyReq / salary) <= 0.45 -> "MODERATE"
            else -> "TIGHT"
        }

        val whatToKeep = "الالتزامات الأساسية (الكراء، الفواتير، التقضية الصحية، والصحة) خط أحمر لا يجب المساس به لضمان استقرارك."

        val whatToCut = "عادة \"${leakInfo.titleAr}\" كتكلفك بوحدها ${shockNumbers.formatMonthlyDrain()} DH شهرياً (${shockNumbers.formatYearlyDrain()} DH/عام). نقصها للنصف كيوفر ليك +${shockNumbers.formatHalfCutYearly()} DH سنوياً!"

        val suggestedMonths = if (pressureLevel == "TIGHT") {
            (target / (salary * 0.30).coerceAtLeast(500.0)).toInt().coerceIn(months + 6, 60)
        } else 0

        val alternative = if (suggestedMonths > 0) {
            val newMonthly = target / suggestedMonths
            val currentReqStr = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(monthlyReq.toLong().toString())
            val newMonthlyStr = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(newMonthly.toLong().toString())
            "إلى جاك الاقتطاع الشهري (\u200E$currentReqStr\u200E درهم) ضاغط عليك، الاقتراح الذكي: مدد المدة لـ \u200E$suggestedMonths\u200E شهر وغادي يولي القسط فقط \u200E$newMonthlyStr\u200E درهم شهرياً بكل أريحية!"
        } else null

        val austerityStepsAr = listOf(
            if (goal.savingsStyle == "TURBO") {
                "🛑 تجميد الكماليات: توقيف مؤقت لشراء الملابس والإلكترونيات الإضافية"
            } else {
                "⏳ قاعدة 24 ساعة: التمهل يوماً كاملاً قبل أي شراء غير مبرمج فايت 150 DH"
            },
            leakInfo.austerityStepAr,
            "💵 الأظرفة الكاش: سحب ميزانية الأسبوع نقداً وتفادي الكارط للكماليات",
            "🚀 الاقتطاع الفوري: عزل مبلغ التوفير (${com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(monthlyReq.toLong().toString())} DH) أول ما يدخل الصالير",
            "💰 النتيجة المباشرة: توفير إضافي بـ +${shockNumbers.formatHalfCutYearly()} DH سنوياً وتسريع الهدف!"
        )

        val austerityStepsFr = listOf(
            if (goal.savingsStyle == "TURBO") {
                "🛑 Gel des extras : Pause sur vêtements et gadgets superflus"
            } else {
                "⏳ Règle des 24h : Attendre un jour complet avant tout achat > 150 DH"
            },
            leakInfo.austerityStepFr,
            "💵 Enveloppe cash : Retrait de poche en liquide pour bloquer la carte",
            "🚀 Payez-vous d'abord : Isoler l'épargne (${com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(monthlyReq.toLong().toString())} DH) dès la paie",
            "💰 Gain direct : Récupérer +${shockNumbers.formatHalfCutYearly()} DH/an pour booster votre objectif !"
        )

        return PlanDiagnosis(
            monthlyRequired = monthlyReq,
            dailyRequired = dailyReq,
            salary = salary,
            essentialsEstimated = essentialsEstimated,
            leisureEstimated = leisureEstimated,
            monthlySavingsPotential = (salary - essentialsEstimated - (if (goal.savingsStyle == "TURBO") leisureEstimated * 0.5 else leisureEstimated)).coerceAtLeast(0.0),
            pressureLevel = pressureLevel,
            whatToKeep = whatToKeep,
            whatToCut = whatToCut,
            alternativeSuggestion = alternative,
            suggestedMonths = suggestedMonths,
            leakInfo = leakInfo,
            shockNumbers = shockNumbers,
            goalTrap = goalTrap,
            vitalPillars = SavingsKnowledgeBase.VITAL_PILLARS,
            austerityStepsAr = austerityStepsAr,
            austerityStepsFr = austerityStepsFr
        )
    }
}
