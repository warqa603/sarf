package com.cash.guide.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cash.guide.data.SavingsRepository
import com.cash.guide.data.db.SavingsDepositEntity
import com.cash.guide.data.db.SavingsGoalEntity
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
    val suggestedMonths: Int = 0
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
    val depositTargetGoal: SavingsGoalEntity? = null
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SavingsUiState> = combine(
        repository.allGoals,
        repository.totalSavedCentimes,
        _selectedTab,
        _isWizardOpen,
        _wizardForm,
        _depositTargetGoal,
        _selectedGoalId,
        activeGoalDepositsFlow
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
            depositTargetGoal = depGoal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsUiState()
    )

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
        _wizardForm.value = _wizardForm.value.copy(leisureCategory = category)
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
                savingsStyle = form.savingsStyle
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
            repository.deleteGoal(goalId)
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

    private fun computeDiagnosis(goal: SavingsGoalEntity): PlanDiagnosis {
        val target = goal.targetAmountCentimes / 100.0
        val current = goal.currentAmountCentimes / 100.0
        val months = goal.targetMonths.coerceAtLeast(1)
        val remaining = (target - current).coerceAtLeast(0.0)
        val monthlyReq = if (months > 0) remaining / months else remaining
        val dailyReq = monthlyReq / 30.0
        val salary = if (goal.monthlySalaryCentimes > 0) goal.monthlySalaryCentimes / 100.0 else 6000.0

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

        val whatToKeep = "الالتزامات الأساسية (الكراء، الفواتير، ومصروف البيت) ضروري تحافظ عليها بلا نقصان باش ما يتأثرش استقرارك."

        val whatToCut = when (goal.leisureCategory) {
            "CAFE" -> "نقص القهاوي والمطاعم للنصف (مثلاً قهوة واحدة فالنهار عوض 2 أو 3 والماكلة من الدار) -> غادي توفر تقريباً بين \u200E+500\u200E و \u200E+800\u200E درهم شهرياً."
            "SHOPPING" -> "طبق 'قاعدة 24 ساعة' قبل شراء أي لبسة أو كماليات، وتفادى الشوبينغ غير المبرمج -> غادي تحمي ما بين \u200E+800\u200E و \u200E+1\u00A0500\u200E درهم شهرياً."
            "OUTINGS" -> "حدد ميزانية كاش مضبوطة للويكاند والخرجات وماتزيدش عليها -> توفير تقريباً ما بين \u200E+600\u200E و \u200E+1\u00A0000\u200E درهم شهرياً."
            else -> "راجع اشتراكات الأنترنت والهاتف والخدمات اللي ما كتستعملهاش بزاف -> توفير ما بين \u200E+200\u200E و \u200E+400\u200E درهم شهرياً."
        }

        val suggestedMonths = if (pressureLevel == "TIGHT") {
            (target / (salary * 0.30).coerceAtLeast(500.0)).toInt().coerceIn(months + 6, 60)
        } else 0

        val alternative = if (suggestedMonths > 0) {
            val newMonthly = target / suggestedMonths
            val currentReqStr = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(monthlyReq.toLong().toString())
            val newMonthlyStr = com.cash.guide.domain.JournalLedgerManager.formatFrenchNumber(newMonthly.toLong().toString())
            "إلى جاك الاقتطاع الشهري (\u200E$currentReqStr\u200E درهم) ضاغط عليك، الاقتراح الذكي: مدد المدة لـ \u200E$suggestedMonths\u200E شهر وغادي يولي القسط فقط \u200E$newMonthlyStr\u200E درهم شهرياً بكل أريحية!"
        } else null

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
            suggestedMonths = suggestedMonths
        )
    }
}
