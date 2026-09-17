package com.cash.guide.feature.savings

import androidx.compose.ui.graphics.Color
import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.ui.notebook.HighlighterBlue
import com.cash.guide.ui.notebook.HighlighterGreen
import com.cash.guide.ui.notebook.HighlighterPink
import com.cash.guide.ui.notebook.HighlighterYellow

/**
 * Categories for Moroccan Personal Finance Knowledge Articles
 */
enum class ArticleCategoryGroup(
    val id: String,
    val titleAr: String,
    val titleFr: String,
    val iconEmoji: String
) {
    ALL("all", "الكل", "Tous", "🌟"),
    PSYCHOLOGY("psychology", "سيكولوجيا الشراء", "Psychologie", "🧠"),
    STRATEGY("strategy", "استراتيجيات التوفير", "Stratégies", "🎯"),
    SECURITY("security", "الأمان والديون", "Sécurité & Dettes", "🛡️"),
    HOUSEHOLD("household", "التقضية ومصاريف البيت", "Maison & Quotidien", "🛒"),
    INCOME("income", "الدخل والاستثمار", "Revenus & Croissance", "💼")
}

/**
 * Comprehensive Moroccan Personal Finance Article Model
 */
data class SavingsArticleFull(
    val id: String,
    val categoryGroup: ArticleCategoryGroup,
    val categoryLabelAr: String,
    val categoryLabelFr: String,
    val tagColor: Color,
    val readTimeAr: String,
    val readTimeFr: String,
    val iconEmoji: String,
    val titleAr: String,
    val titleFr: String,
    val summaryAr: String,
    val summaryFr: String,
    val actionPointsAr: List<String>,
    val actionPointsFr: List<String>,
    val caseStudyAr: String?,
    val caseStudyFr: String?,
    val takeawayAr: String,
    val takeawayFr: String
)

/**
 * Daily Moroccan Street-Smart Financial Tip
 */
data class DailyFinancialTip(
    val id: String,
    val iconEmoji: String,
    val titleAr: String,
    val titleFr: String,
    val tipAr: String,
    val tipFr: String,
    val actionAr: String,
    val actionFr: String
)

/**
 * Information model for Spending Leaks (المصطلحات السلبية / أبواب الاستنزاف المالي)
 */
data class SpendingLeakInfo(
    val key: String,
    val titleAr: String,
    val titleFr: String,
    val subtitleAr: String,
    val subtitleFr: String,
    val defaultDailyCostDh: Double,
    val defaultDaysPerWeek: Int,
    val quickCostOptionsDh: List<Double>,
    val quickCostLabelsAr: List<String>,
    val quickCostLabelsFr: List<String>,
    val concreteAdviceAr: String,
    val concreteAdviceFr: String,
    val austerityStepAr: String,
    val austerityStepFr: String
)

/**
 * Information model for Vital Fixed Pillars (المصطلحات الإيجابية / الثوابت التي لا تمس)
 */
data class VitalPillarInfo(
    val key: String,
    val nameAr: String,
    val nameFr: String,
    val rationaleAr: String,
    val rationaleFr: String
)

/**
 * Information model for Goal-specific Hidden Traps & Shock Multipliers (فخاخ الأهداف المالية الخفية)
 */
data class GoalTrapInfo(
    val presetKey: String,
    val titleAr: String,
    val titleFr: String,
    val warningAr: String,
    val warningFr: String,
    val goldenRuleAr: String,
    val goldenRuleFr: String
)

object SavingsKnowledgeBase {

    // --- 1. SPENDING LEAKS (المصطلحات السلبية) ---
    val SPENDING_LEAKS = listOf(
        SpendingLeakInfo(
            key = "CAFE",
            titleAr = "☕ القهاوي، المطاعم، والماكلة من برا",
            titleFr = "☕ Cafés, snacks, restos & repas dehors",
            subtitleAr = "سناك، طاكوس، بيتزا، وقهاوي متكررة بالدرهم الصغير",
            subtitleFr = "Tacos, pizzas, cafés répétitifs et petits extras quotidiens",
            defaultDailyCostDh = 25.0,
            defaultDaysPerWeek = 6,
            quickCostOptionsDh = listOf(15.0, 25.0, 35.0, 50.0),
            quickCostLabelsAr = listOf("15 DH (قهوة/فطور)", "25 DH (قهوة+كوتي)", "35 DH (طاكوس/سناك)", "50 DH (بيتزا/غداء)"),
            quickCostLabelsFr = listOf("15 DH (Café/Matin)", "25 DH (Café+Goûter)", "35 DH (Tacos/Snack)", "50 DH (Pizza/Déjeuner)"),
            concreteAdviceAr = "شرب قهوة وحدة برا فالنهار مع الأصدقاء، والباقي حضرو فالدار أو المكتب، ونقص وجبات السناك برا لمرة فالسيمانة.",
            concreteAdviceFr = "1 seul café extérieur/jour et repas maison au bureau. Gardez les restos/tacos à 1 fois par semaine.",
            austerityStepAr = "🍱 الماكلة من الدار: تحضير وجبة العمل مسبقاً والاكتفاء بقهوة واحدة برا فقط.",
            austerityStepFr = "🍱 Déjeuners maison : Gamelles préparées et limitation stricte à 1 café extérieur."
        ),
        SpendingLeakInfo(
            key = "SHOPPING",
            titleAr = "🛍️ الشوبينغ العشوائي، الملابس، والإلكترونيات",
            titleFr = "🛍️ Shopping impulsif, vêtements & gadgets",
            subtitleAr = "شراء رغبات لحظية وكماليات استهلاكية غير مبرمجة",
            subtitleFr = "Achats coup de cœur et extras non programmés",
            defaultDailyCostDh = 35.0,
            defaultDaysPerWeek = 6,
            quickCostOptionsDh = listOf(20.0, 35.0, 50.0, 80.0),
            quickCostLabelsAr = listOf("20 DH (~600 DH/ش)", "35 DH (~1000 DH/ش)", "50 DH (~1500 DH/ش)", "80 DH (~2400 DH/ش)"),
            quickCostLabelsFr = listOf("20 DH (~600 DH/m)", "35 DH (~1000 DH/m)", "50 DH (~1500 DH/m)", "80 DH (~2400 DH/m)"),
            concreteAdviceAr = "طبق 'قاعدة 72 ساعة' على أي حاجة عجباتك فايتة 150 درهم. قيدها فورقة وتسنى 3 أيام؛ أغلب النزوات كتنسى بوحدها.",
            concreteAdviceFr = "Appliquez la 'règle des 72h' pour tout achat > 150 DH. Notez-le sur carnet : 70% des envies disparaissent seules.",
            austerityStepAr = "🛑 تجميد الكماليات: توقيف تام لشراء الملابس والإلكترونيات والكماليات غير الضرورية.",
            austerityStepFr = "🛑 Gel des plaisirs : Stop absolu sur les vêtements et gadgets non indispensables."
        ),
        SpendingLeakInfo(
            key = "OUTINGS",
            titleAr = "🚗 الخرجات، الكازوال غير المبرمج، وسفريات الويكاند",
            titleFr = "🚗 Sorties imprévues, carburant & week-ends",
            subtitleAr = "مصاريف نهاية الأسبوع، الدوران بالسيارة، والتنقلات العشوائية",
            subtitleFr = "Balades motorisées, carburant superflu et week-ends improvisés",
            defaultDailyCostDh = 30.0,
            defaultDaysPerWeek = 6,
            quickCostOptionsDh = listOf(20.0, 30.0, 50.0, 70.0),
            quickCostLabelsAr = listOf("20 DH (~600 DH/ش)", "30 DH (~900 DH/ش)", "50 DH (~1500 DH/ش)", "70 DH (~2100 DH/ش)"),
            quickCostLabelsFr = listOf("20 DH (~600 DH/m)", "30 DH (~900 DH/m)", "50 DH (~1500 DH/m)", "70 DH (~2100 DH/m)"),
            concreteAdviceAr = "اسحب ميزانية الويكاند كاش فظرف محدد نهار الجمعة، وخلي البطاقة البنكية فالدار باش ما تفوتش السقف.",
            concreteAdviceFr = "Allouez une enveloppe cash fermée le vendredi soir pour le week-end et laissez la carte bancaire à la maison.",
            austerityStepAr = "💵 الأظرفة الكاش: سحب مصروف نهاية الأسبوع نقداً وتفادي الأداء بالكارط بتاتاً.",
            austerityStepFr = "💵 Enveloppe cash : Sorties réglées uniquement en billets sans carte bancaire."
        ),
        SpendingLeakInfo(
            key = "SUBSCRIPTIONS",
            titleAr = "📱 الاشتراكات الرقمية، فورفيات زايدة، ومصاريف متفرقة",
            titleFr = "📱 Abonnements dormants, forfaits & petits extras",
            subtitleAr = "أنترنت فائق بدون حاجة، تطبيقات، واشتراكات مهجورة",
            subtitleFr = "Forfaits surdimensionnés, applis payantes et abonnements inutilisés",
            defaultDailyCostDh = 15.0,
            defaultDaysPerWeek = 6,
            quickCostOptionsDh = listOf(10.0, 15.0, 25.0, 40.0),
            quickCostLabelsAr = listOf("10 DH (~300 DH/ش)", "15 DH (~450 DH/ش)", "25 DH (~750 DH/ش)", "40 DH (~1200 DH/ش)"),
            quickCostLabelsFr = listOf("10 DH (~300 DH/m)", "15 DH (~450 DH/m)", "25 DH (~750 DH/m)", "40 DH (~1200 DH/m)"),
            concreteAdviceAr = "راجع كشف الحساب البنكي ولغي فوراً أي اشتراك (تطبيقات، جيم، منصات مشاهدة) ما استعملتيهش فـ آخر 14 يوم.",
            concreteAdviceFr = "Épluchez vos prélèvements et résiliez sur-le-champ tout abonnement non utilisé ces 14 derniers jours.",
            austerityStepAr = "📱 تطهير الاشتراكات: إلغاء التطبيقات والخدمات غير المستعملة وخفض الباقات.",
            austerityStepFr = "📱 Nettoyage numérique : Résiliation des forfaits superflus et applis dormantes."
        )
    )

    // --- 2. VITAL PILLARS (المصطلحات الإيجابية / الثوابت التي لا تمس) ---
    val VITAL_PILLARS = listOf(
        VitalPillarInfo(
            key = "LOGEMENT",
            nameAr = "الكراء أو طريطمون السكن",
            nameFr = "Loyer ou traite du logement",
            rationaleAr = "خط أحمر أساسي لاستقرارك النفسي والعائلي؛ لا يجب تأخيره بتاتاً.",
            rationaleFr = "Pilier numéro 1 de votre stabilité ; aucune impasse possible."
        ),
        VitalPillarInfo(
            key = "FACTURES",
            nameAr = "فواتير الماء والكهرباء والأنترنت الأساسي",
            nameFr = "Factures eau, électricité & internet de base",
            rationaleAr = "واجبات قارة ترشد بحسن التدبير وليس بالانقطاع.",
            rationaleFr = "Charges incontournables à optimiser avec bon sens."
        ),
        VitalPillarInfo(
            key = "ALIMENTATION_MAISON",
            nameAr = "التقضية المنزلية الصحية للدار",
            nameFr = "Courses alimentaires saines à la maison",
            rationaleAr = "الطبخ المنزلي هو أساس الصحة البدنية ويوفر 60% مقارنة بماكلة الزنقة.",
            rationaleFr = "Cuisiner maison préserve votre santé et coûte 60% moins cher que dehors."
        ),
        VitalPillarInfo(
            key = "SANTE_FAMILLE",
            nameAr = "الصحة، التطبيب، والتأمين",
            nameFr = "Santé, pharmacie et couverture mutuelle",
            rationaleAr = "أولوية قصوى غير قابلة لأي تقشف أو مساومة.",
            rationaleFr = "Priorité absolue ne tolérant aucun compromis d'austérité."
        ),
        VitalPillarInfo(
            key = "EPARGNE_PRIORITAIRE",
            nameAr = "الاقتطاع الفوري (الدفع للنفس أولاً)",
            nameFr = "Épargne prioritaire (Pay yourself first)",
            rationaleAr = "عزل مبلغ التوفير نهار نزول الصالير وقبل صرف أي درهم.",
            rationaleFr = "Isoler sa mensualité le jour même de la paie avant toute autre dépense."
        )
    )

    // --- 3. GOAL-SPECIFIC TRAPS (فخاخ الأهداف المالية الخفية) ---
    val GOAL_TRAPS = listOf(
        GoalTrapInfo(
            presetKey = "CAR",
            titleAr = "صدمة المصاريف الخفية (Frais cachés)",
            titleFr = "Le choc des frais cachés du véhicule",
            warningAr = "شراء الطوموبيل ما كيحبسش فثمن البيع! كاين كارت كريس (Carte grise)، التأمين السنوي، لافينيت (Vignette)، والميكانيك الأولي.",
            warningFr = "Le prix d'achat n'est que la moitié de l'histoire ! Carte grise, assurance annuelle, vignette et révision initiale coûtent cher.",
            goldenRuleAr = "ما تشريش بآخر درهم عندك؛ خلي ديما 15% إلى 20% زايدة على ثمن السيارة لهاد المصاريف.",
            goldenRuleFr = "N'achetez jamais au dernier dirham : prévoyez 15% à 20% en plus pour ces frais incompressibles."
        ),
        GoalTrapInfo(
            presetKey = "HOUSE",
            titleAr = "فخ مصاريف الموثق والتحفيظ (Frais de Notaire)",
            titleFr = "Le piège des frais de notaire & enregistrement",
            warningAr = "رسوم الموثق، التسجيل، والتحفيظ العقاري كتوصل لـ 7% حتى لـ 8% من الثمن الإجمالي للعقار، زيادة على التجهيز والربط.",
            warningFr = "Les frais d'acte, droits d'enregistrement et conservation foncière atteignent 7% à 8% de la valeur totale du bien.",
            goldenRuleAr = "زيد 8% فوراً على مبلغ التسبيق اللي ناوي تجمعه باش ما تفاجأش نهار التوقيع.",
            goldenRuleFr = "Intégrez immédiatement +8% sur votre apport visé pour éviter toute mauvaise surprise chez le notaire."
        ),
        GoalTrapInfo(
            presetKey = "EMERGENCY",
            titleAr = "فخ بطاقة الكيشي (La tentation de la carte)",
            titleFr = "Le piège de la carte bancaire liée",
            warningAr = "إلى خليتي صندوق الطوارئ فـ الحساب الجاري وفـ جيبك كارط كيشي، كيتسلتو الفلوس مع الصرف اليومي بلا ما تشعر!",
            warningFr = "Garder son fonds d'urgence sur un compte à vue avec carte bancaire conduit inévitablement à le dépenser en extras.",
            goldenRuleAr = "افتح حساب توفير بدفتر (Compte sur carnet) بدون بطاقة بنكية باش تصعب عملية السحب وتخليه غير للطوارئ الحقيقية.",
            goldenRuleFr = "Placez l'argent sur un compte sur carnet sans carte pour sanctuariser votre bouclier."
        ),
        GoalTrapInfo(
            presetKey = "PROJECT",
            titleAr = "تجاهل مصاريف التشغيل الأولى (Le BFR)",
            titleFr = "L'oubli du besoin en fonds de roulement (BFR)",
            warningAr = "أغلب المشاريع كتعثر فالأشهر الأولى حيت كيحسبو غير مصاريف الإطلاق، وكينساو 6 شهور ديال التسيير والمصاريف الشخصية.",
            warningFr = "La majorité des projets flanchent par manque de trésorerie pour couvrir les 6 premiers mois avant d'atteindre la rentabilité.",
            goldenRuleAr = "وفر ميزانية إطلاق المشروع + مصاريف كراء ومعيشة لـ 6 أشهر مسبقاً قبل ما تبدا.",
            goldenRuleFr = "Épargnez le coût de lancement + 6 mois de charges fixes et vie courante avant de démissionner."
        ),
        GoalTrapInfo(
            presetKey = "EVENT",
            titleAr = "فخ سلف الاستهلاك للمناسبات",
            titleFr = "Le piège du crédit à la consommation",
            warningAr = "النفخ فمصاريف عرس أو سفر بكريدي استهلاكي كيخليك مديون لأشهر وسنوات من أجل متعة مؤقتة لبضعة أيام.",
            warningFr = "S'endetter pour financer une fête ou un voyage vous lie à des mensualités étouffantes pour un plaisir éphémère.",
            goldenRuleAr = "حدد سقفاً كاش لا يتجاوز مدخراتك الفائضة والتزم بعدم الاقتراض لأي كماليات.",
            goldenRuleFr = "Fixez un budget liquide strict et refusez catégoriquement tout crédit pour une fête."
        )
    )

    fun getLeak(key: String): SpendingLeakInfo {
        return SPENDING_LEAKS.firstOrNull { it.key.equals(key, ignoreCase = true) }
            ?: SPENDING_LEAKS[0]
    }

    fun getGoalTrap(presetKey: String): GoalTrapInfo {
        return GOAL_TRAPS.firstOrNull { it.presetKey.equals(presetKey, ignoreCase = true) }
            ?: GOAL_TRAPS[0]
    }

    /**
     * Computes the shock numbers:
     * - weeklyDrainDh = dailyCostDh * daysPerWeek
     * - monthlyDrainDh = weeklyDrainDh * 52 / 12
     * - yearlyDrainDh = weeklyDrainDh * 52
     * - halfCutYearlyGainDh = yearlyDrainDh * 0.5
     */
    fun computeShockNumbers(dailyCostDh: Double, daysPerWeek: Int): ShockCalculationResult {
        val daily = dailyCostDh.coerceAtLeast(0.0)
        val days = daysPerWeek.coerceIn(1, 7)
        val weeklyDrain = daily * days
        val yearlyDrain = weeklyDrain * 52.0
        val monthlyDrain = yearlyDrain / 12.0
        val halfCutYearlyGain = yearlyDrain * 0.50

        return ShockCalculationResult(
            dailyCostDh = daily,
            daysPerWeek = days,
            weeklyDrainDh = weeklyDrain,
            monthlyDrainDh = monthlyDrain,
            yearlyDrainDh = yearlyDrain,
            halfCutYearlyGainDh = halfCutYearlyGain
        )
    }

    data class ShockCalculationResult(
        val dailyCostDh: Double,
        val daysPerWeek: Int,
        val weeklyDrainDh: Double,
        val monthlyDrainDh: Double,
        val yearlyDrainDh: Double,
        val halfCutYearlyGainDh: Double
    ) {
        fun formatYearlyDrain(): String =
            JournalLedgerManager.formatFrenchNumber(yearlyDrainDh.toLong().toString())

        fun formatMonthlyDrain(): String =
            JournalLedgerManager.formatFrenchNumber(monthlyDrainDh.toLong().toString())

        fun formatHalfCutYearly(): String =
            JournalLedgerManager.formatFrenchNumber(halfCutYearlyGainDh.toLong().toString())
    }

    /**
     * Deterministic Moroccan AI Financial Coach fallback advice when offline or missing key.
     * Generates a sharp, street-smart 3-point coaching verdict.
     */
    fun generateDeterministicCoachVerdict(
        goalTitle: String,
        targetAmountDh: Double,
        targetMonths: Int,
        monthlySalaryDh: Double,
        leakCategory: String,
        dailyCostDh: Double,
        daysPerWeek: Int,
        savingsStyle: String,
        isRtl: Boolean
    ): String {
        val leak = getLeak(leakCategory)
        val shock = computeShockNumbers(dailyCostDh, daysPerWeek)
        val trap = getGoalTrap(
            when {
                goalTitle.contains("سيارة", true) || goalTitle.contains("voiture", true) -> "CAR"
                goalTitle.contains("دار", true) || goalTitle.contains("سكن", true) || goalTitle.contains("maison", true) -> "HOUSE"
                goalTitle.contains("طوارئ", true) || goalTitle.contains("urgence", true) -> "EMERGENCY"
                goalTitle.contains("مشروع", true) || goalTitle.contains("projet", true) -> "PROJECT"
                else -> "EVENT"
            }
        )

        val targetStr = JournalLedgerManager.formatFrenchNumber(targetAmountDh.toLong().toString())
        val yearlyDrainStr = shock.formatYearlyDrain()
        val halfCutStr = shock.formatHalfCutYearly()

        return if (isRtl) {
            """
• 🔍 الصدمة الحقيقية: عادة "${leak.titleAr}" كتكلفك بوحدها $yearlyDrainStr درهم كل عام فمصاريف صغيرة متفرقة!
• ⚡ قرار فوري ذكي: خفض هاد العادة غير للنصف (مرة سناك فالسيمانة وباقي الأيام ماكلة دالدار)، وغادي تربح +$halfCutStr درهم سنوياً تقربك بزاف من هدف "${goalTitle}" ($targetStr درهم).
• 🛡️ رد بالك من الفخ: ${trap.warningAr}
• 💡 وصية الكوتش: ${trap.goldenRuleAr}
            """.trimIndent()
        } else {
            """
• 🔍 Le constat réel : L'habitude "${leak.titleFr}" vous coûte à elle seule $yearlyDrainStr DH/an en petites fuites invisibles !
• ⚡ Action immédiate : Réduisez cette dépense de moitié (repas maison en priorité) pour récupérer +$halfCutStr DH/an pour votre objectif "${goalTitle}" ($targetStr DH).
• 🛡️ Attention au piège : ${trap.warningFr}
• 💡 Règle d'or : ${trap.goldenRuleFr}
            """.trimIndent()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. DAILY GOLDEN FINANCIAL TIPS (نصائح اليوم الذهبية السريعة)
    // ══════════════════════════════════════════════════════════════════════════
    val DAILY_TIPS = listOf(
        DailyFinancialTip(
            id = "tip_24h",
            iconEmoji = "⏳",
            titleAr = "قاعدة الـ 24 ساعة للنزوات",
            titleFr = "Règle des 24h contre les pulsions",
            tipAr = "أي رغبة شراء كمالية فايتة 150 درهم وما كانتش مبرمجة، فرض على راسك مهلة 24 ساعة كاملة قبل ما تخلص. 80% من النزوات كتبرد بوحدها نهار الغد.",
            tipFr = "Pour tout achat plaisir imprévu > 150 DH, imposez-vous un délai de réflexion de 24h. 80% des envies s'évanouissent le lendemain.",
            actionAr = "قيد الحاجة فورقة وسد التطبيق حتى لغدا.",
            actionFr = "Notez l'envie sur un carnet et fermez l'appli."
        ),
        DailyFinancialTip(
            id = "tip_pay_first",
            iconEmoji = "🚀",
            titleAr = "خلص راسك نهار الصالير",
            titleFr = "Payez-vous d'abord le jour de paie",
            tipAr = "ما تسناش نهاية الشهر باش توفر شنو شاط؛ حيت فـ 95% من الحالات ما كيشيط والو. نهار كيدخل الصالير، حول قسط التوفير مباشرة لحساب معزول.",
            tipFr = "N'attendez pas la fin du mois pour épargner les restes. Dès la paie reçue, virez votre épargne sur un compte séparé.",
            actionAr = "دير تحويل أوتوماتيكي نهار نزول الصالير.",
            actionFr = "Activez un ordre de virement permanent."
        ),
        DailyFinancialTip(
            id = "tip_cash_pain",
            iconEmoji = "💵",
            titleAr = "استرجع ألم الكاش الفيزيائي",
            titleFr = "Retrouvez la douleur saine du cash",
            tipAr = "الدفع بالبطاقة والكونتاكتلس كينوم إحساس الدماغ بالفقدان وكيضاعف المصاريف بـ 25%. سحب كاش الأسبوع فظرف مادي وخلي الكارط فالدار.",
            tipFr = "Payer par carte sans contact supprime la sensation de dépense (+25% de fuites). Retirez votre argent de poche en espèces.",
            actionAr = "خرج المصروف اليومي كاش وخلي البطاقة فالدار.",
            actionFr = "Payez vos sorties en billets physiques."
        ),
        DailyFinancialTip(
            id = "tip_grocery_stomach",
            iconEmoji = "🥗",
            titleAr = "لا تتسوق وأنت جائع",
            titleFr = "Ne faites jamais les courses le ventre vide",
            tipAr = "التسوق بمعدة فارغة كيحفز هرمونات الشراء العشوائي وكيخليك تزيد 40% كماليات وسناكات ما مسجلاش فورقة التقضية.",
            tipFr = "Faire ses courses en ayant faim pousse votre cerveau vers les snacks impulsifs (+40% sur le ticket de caisse).",
            actionAr = "شرب قهوة وكول لقمة قبل ما تمشي للسوبرماركت.",
            actionFr = "Prenez une collation avant de partir faire les courses."
        ),
        DailyFinancialTip(
            id = "tip_emergency_first",
            iconEmoji = "🛡️",
            titleAr = "حزام الأمان قبل أي استثمار",
            titleFr = "Le bouclier d'urgence avant tout",
            tipAr = "قبل ما تفكر فـ دار أو طوموبيل، جمع 1 000 إلى 2 000 درهم كصندوق طوارئ أولي يحميك من فخ الاستدانة عند أول مفاجأة صحية أو ميكانيكية.",
            tipFr = "Avant tout grand projet, constituez 1 000 à 2 000 DH de fonds d'urgence pour bloquer le recours au crédit à la première panne.",
            actionAr = "افتح حساب توفير بدفتر بدون بطاقة بنكية.",
            actionFr = "Ouvrez un compte sur carnet sans carte de retrait."
        ),
        DailyFinancialTip(
            id = "tip_latte_shock",
            iconEmoji = "☕",
            titleAr = "فخ 'راها غير 20 درهم'",
            titleFr = "Le piège du 'Ce n'est que 20 DH'",
            tipAr = "المصاريف المجهرية اليومية (قهوة برا، سناك، طاكسي) كتستنزف أكثر من 7 000 درهم فالعام! نقص التردد للنصف كافي يمول أهدافك الكبيرة.",
            tipFr = "Les micro-dépenses invisibles (cafés, snacks) s'élèvent à plus de 7 000 DH/an ! Les diviser par 2 finance vos grands projets.",
            actionAr = "شرب قهوة الصباح فالدار وخصص جلسة القهوة للصحاب.",
            actionFr = "Café maison le matin, sortie café avec les amis."
        )
    )

    // ══════════════════════════════════════════════════════════════════════════
    // 5. COMPREHENSIVE SAVINGS & FINANCIAL KNOWLEDGE ARTICLES (المكتبة الشاملة)
    // ══════════════════════════════════════════════════════════════════════════
    val SAVINGS_ARTICLES = listOf(
        // ──────────────────────────────────────────────────────────────────────
        // Category 1: PSYCHOLOGY (سيكولوجيا الشراء وفخاخ الاستهلاك)
        // ──────────────────────────────────────────────────────────────────────
        SavingsArticleFull(
            id = "art_psych_retail_therapy",
            categoryGroup = ArticleCategoryGroup.PSYCHOLOGY,
            categoryLabelAr = "سيكولوجيا الشراء",
            categoryLabelFr = "Psychologie",
            tagColor = HighlighterPink,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "🧠",
            titleAr = "الشراء العاطفي والتعويضي: علاش كنشريو فاش كنكونو مقلقين؟",
            titleFr = "Achats émotionnels : Pourquoi dépense-t-on sous stress ?",
            summaryAr = "كيفاش الدماغ كيطلب جرعة دوبامين سريعة عبر الشوبينغ أو الماكلة برا كمهدئ للأعصاب.",
            summaryFr = "Comment notre cerveau cherche une dose rapide de dopamine dans les achats pour apaiser l'anxiété.",
            actionPointsAr = listOf(
                "• هرمون الدوبامين: الرغبة فالشراء كتعطي متعة لحظية كتختفي بمجرد ما تخلص وتخرج من المحل.",
                "• الشراء كمسكن للألم: التوتر فالخدمة أو القلق كيدفعنا للتعويض بشراء كماليات غير مبرمجة.",
                "• البدائل المجانية للدوبامين: استبدل الشوبينغ بمشي نص ساعة، اتصال بصديق، أو دوش دافئ.",
                "• اختبار الفراغ النفسي: سول راسك قبل الأداء: واش بصح محتاج هاد السلعة، ولا غير باغي نحس بشي حاجة زوينة؟"
            ),
            actionPointsFr = listOf(
                "• Pic de dopamine : L'excitation d'acheter retombe aussitôt le ticket de caisse payé.",
                "• Consommation pansement : Le stress au travail pousse à compenser par des extras inutiles.",
                "• Dopamine gratuite : Remplacez le shopping par une marche de 30 min, un appel ou du sport.",
                "• Test du vide émotionnel : Demandez-vous si vous achetez un besoin ou un soulagement passager."
            ),
            caseStudyAr = "شراء 3 كماليات أسبوعياً (150 DH كل مرة) لتهدئة الأعصاب = 1 800 DH شهرياً = 21 600 DH فالعام مشات غير فمسكنات لحظية!",
            caseStudyFr = "3 achats de réconfort par semaine (150 DH chacun) = 1 800 DH/mois = 21 600 DH/an envolés en soulagements éphémères !",
            takeawayAr = "💡 الخلاصة: ما تعمرش الفراغ النفسي من جيبك؛ فصل المشاعر عن المحفظة هو سر الأمان المالي.",
            takeawayFr = "💡 En résumé : Ne comblez pas vos émotions avec votre portefeuille ; dissociez stress et argent."
        ),
        SavingsArticleFull(
            id = "art_psych_fomo_social",
            categoryGroup = ArticleCategoryGroup.PSYCHOLOGY,
            categoryLabelAr = "سيكولوجيا الشراء",
            categoryLabelFr = "Psychologie",
            tagColor = HighlighterPink,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "🎭",
            titleAr = "فخ المظاهر و'شنو غيقولو الناس': صرف فلوس ما عندكش لإرضاء ناس ما كيهموكش",
            titleFr = "Le piège du paraître : Dépenser pour impressionner autrui",
            summaryAr = "الضغط الاجتماعي والمقارنة فالسوشيال ميديا كيدفعونا لمستوى عيش كيفوق القدرة ديالنا.",
            summaryFr = "La comparaison sociale et Instagram nous poussent à surconsommer pour préserver une image.",
            actionPointsAr = listOf(
                "• وهم المقارنة: السوشيال ميديا كتعرض أحسن 5% من حياة الناس، ومحاولة تقليدهم كتدمر ميزانيتك.",
                "• الشجاعة فقول 'لا': ما تحشمش تقول 'هاد الخرجة أو السفرية خارج ميزانيتي هاد الشهر'؛ الثقة هي الأساس.",
                "• المظاهر ضد الثروة: الناس اللي لاباس عليهم بصح كيعيشو تحت إمكانياتهم؛ التظاهر بالغنى أسرع طريق للإفلاس.",
                "• الأعراس والمناسبات: متعة ليلة واحدة ما كتستاهلش 3 سنين ديال طريطات السلف الاستهلاكي."
            ),
            actionPointsFr = listOf(
                "• Illusion des réseaux : Instagram ne montre que le meilleur ; copier ce train de vie ruine votre budget.",
                "• Savoir dire non : Osez dire 'Cette sortie dépasse mon budget ce mois-ci' en toute sérénité.",
                "• Richesse discrète : Les personnes financièrement sereines vivent toujours en deçà de leurs moyens.",
                "• Fêtes démesurées : Quelques heures de fête ne valent pas 3 ans de mensualités de crédit."
            ),
            caseStudyAr = "سلف استهلاكي ديال 30 000 DH لعرس أو سفرية بمظاهر زايدة = طريطة 950 DH شهرياً لمدة 3 سنوات (34 200 DH مع الفوائد)!",
            caseStudyFr = "Crédit conso de 30 000 DH pour une fête clinquante = 950 DH/mois pendant 3 ans (34 200 DH remboursés) !",
            takeawayAr = "💡 الخلاصة: كرامتك المالية واستقرارك أهم بمليون مرة من إعجاب عابر للناس.",
            takeawayFr = "💡 En résumé : Votre sérénité financière a infiniment plus de valeur que l'approbation d'autrui."
        ),
        SavingsArticleFull(
            id = "art_psych_pain_of_paying",
            categoryGroup = ArticleCategoryGroup.PSYCHOLOGY,
            categoryLabelAr = "سيكولوجيا الشراء",
            categoryLabelFr = "Psychologie",
            tagColor = HighlighterPink,
            readTimeAr = "⏱️ 2 دقائق",
            readTimeFr = "⏱️ 2 min",
            iconEmoji = "💳",
            titleAr = "تخدير ألم الدفع: علاش البطاقة البنكية والتطبيقات كتخلينا نصرفو بلا عقل؟",
            titleFr = "Anesthésie du paiement : Pourquoi la carte fait trop dépenser",
            summaryAr = "الدراسات العلمية كتاكد أن الدفع بدون تلامس (Sans contact) كيمسح إحساس الفقدان من الدماغ.",
            summaryFr = "Le paiement sans contact et les applis suppriment le signal de douleur de la dépense dans le cerveau.",
            actionPointsAr = listOf(
                "• ألم الكاش الفيزيائي: ملي كتخرج ورقة دـ 200 درهم وتشوفها كتمشي، الدماغ كيحس بفقدان حقيقي وكيفكر مرتين.",
                "• سحر البطاقة والكونتاكتلس: تمرير الكارط كيبان كأنه بلا ثمن، وكيرفع المصاريف بنسبة 25% تلقائياً.",
                "• إغراء تطبيقات التوصيل: سهولة الكليك بضغطة زر كتخليك تكوموندي ماكلة بثمن مضاعف 3 مرات عن ثمنها الحقيقي.",
                "• العلاج الصارم: سحب كاش الأسبوع فظرف مادي وخلي الكارط البنكية فالدار ملي تخرج تدور."
            ),
            actionPointsFr = listOf(
                "• Douleur saine du cash : Donner un billet de 200 DH physique active une alerte immédiate de dépense.",
                "• L'illusion du sans-contact : Biper la carte semble indolore et majore vos dépenses courantes de 25%.",
                "• Piège de la livraison : Commander en 2 clics sur appli fait payer le repas 3 fois son prix réel.",
                "• Solution radicale : Retirez vos menues dépenses en liquide et laissez la carte bancaire à la maison."
            ),
            caseStudyAr = "الاعتماد على الكارط فكل المصاريف كيرفع الاستهلاك بـ 20%: فصالير 6000 DH، هادشي كيعني ضياع 800 DH شهرياً بدون أثر!",
            caseStudyFr = "Payer tout par carte majore les extras de 20% : sur 6 000 DH de salaire, c'est 800 DH envolés chaque mois !",
            takeawayAr = "💡 الخلاصة: رجّع الكاش لمصاريفك اليومية، وغادي تشوف كيفاش المصاريف العشوائية كتفرمل بوحدها.",
            takeawayFr = "💡 En résumé : Réintroduisez les espèces physiques pour freiner immédiatement les fuites d'argent."
        ),
        SavingsArticleFull(
            id = "art_psych_24h_promo",
            categoryGroup = ArticleCategoryGroup.PSYCHOLOGY,
            categoryLabelAr = "سيكولوجيا الشراء",
            categoryLabelFr = "Psychologie",
            tagColor = HighlighterPink,
            readTimeAr = "⏱️ 2 دقائق",
            readTimeFr = "⏱️ 2 min",
            iconEmoji = "🏷️",
            titleAr = "قاعدة الـ 24 ساعة وفخ الصولد الوهمي (The Promo Trap)",
            titleFr = "La règle des 24h et le piège des fausses promotions",
            summaryAr = "علاش كلمة 'تخفيض -50%' كتنوّم التفكير المنطقي، وكيفاش تقتل النزوة فورقة وستيلو.",
            summaryFr = "Comment le mot 'SOLDE -50%' anesthésie la raison et comment désamorcer l'achat coup de tête.",
            actionPointsAr = listOf(
                "• خدعة التوفير الوهمي: إلى شريتي حاجة بـ 250 DH عوض 500 DH وما كنتيش محتاجها، راك ضيعتي 250 DH ماشي وفرتيها!",
                "• قاعدة 24 ساعة: أي شراء لكماليات فايت 150 درهم، فرض على راسك مهلة يوم كامل قبل ما تخلص.",
                "• قائمة الرغبات (Wishlist): قيد الحاجة فمذكرة الهاتف؛ 80% من الرغبات كتموت وتتلاشى بعد 24 إلى 48 ساعة.",
                "• التسوق بورقة محددة: دخل للمحل بحاجيات مكتوبة، وأي حاجة ما مسجلاش فالورقة ممنوع تدخل للبانري."
            ),
            actionPointsFr = listOf(
                "• Fausse économie : Acheter un article à -50% dont vous n'avez pas besoin n'est pas un gain, c'est une perte.",
                "• Délai de 24h : Pour tout achat non indispensable > 150 DH, imposez-vous une journée d'attente.",
                "• Liste d'attente (Wishlist) : Notez l'envie sur smartphone ; 80% des pulsions meurent d'elles-mêmes.",
                "• Liste stricte en magasin : N'achetez strictement que ce qui figure sur votre liste préparée à la maison."
            ),
            caseStudyAr = "تطبيق مهلة 24 ساعة كيحميك من 2 إلى 4 نزوات شراء شهرياً، وتوفير ما بين 400 DH إلى 900 DH كاش كل شهر.",
            caseStudyFr = "Le délai de 24h bloque 2 à 4 achats compulsifs par mois, épargnant de 400 à 900 DH nets chaque mois.",
            takeawayAr = "💡 الخلاصة: الصبر لـ 24 ساعة هو أقوى مضاد حيوي ضد فيروس الشوبينغ والصولد اللحظي.",
            takeawayFr = "💡 En résumé : Patienter 24h est le filtre le plus puissant contre les pièges du marketing."
        ),

        // ──────────────────────────────────────────────────────────────────────
        // Category 2: STRATEGY (استراتيجيات وتقنيات التوفير)
        // ──────────────────────────────────────────────────────────────────────
        SavingsArticleFull(
            id = "art_strat_50_30_20",
            categoryGroup = ArticleCategoryGroup.STRATEGY,
            categoryLabelAr = "استراتيجيات التوفير",
            categoryLabelFr = "Stratégies",
            tagColor = HighlighterGreen,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "🎯",
            titleAr = "قاعدة 50/30/20 بنكهة مغربية واقعية",
            titleFr = "La règle 50/30/20 adaptée à la réalité marocaine",
            summaryAr = "كيفاش توزع صاليرك المغربي بين الضروريات، الكماليات، والتوفير بلا حرمان.",
            summaryFr = "Comment répartir son salaire entre charges fixes, plaisirs et épargne sans frustration.",
            actionPointsAr = listOf(
                "• 50% للثوابت والأساسيات: الكراء، الفواتير، التقضية الصحية، والواجبات المدرسية التي لا تنازل فيها.",
                "• 30% للرغبات ونمط العيش: القهاوي، الخرجات، الكسوة، والترفيه المحسوب بلا تأنيب ضمير.",
                "• 20% للمستقبل والتوفير: اقتطاع فوري لحساب الطوارئ والهدف المالي نهار نزول الصالير.",
                "• التكييف مع الواقع: إذا كان الكراء مرتفع، اعتمد 60% أساسيات / 25% رغبات / 15% توفير كبداية ممتازة."
            ),
            actionPointsFr = listOf(
                "• 50% Charges fixes : Loyer, factures, courses alimentaires saines, scolarité incontournable.",
                "• 30% Loisirs & style de vie : Cafés, sorties du week-end, vêtements et extras sans culpabilité.",
                "• 20% Épargne & Avenir : Virement prioritaire vers le fonds d'urgence et vos objectifs de vie.",
                "• Adaptation locale : Si le loyer pèse lourd, commencez par 60% fixes / 25% plaisirs / 15% épargne."
            ),
            caseStudyAr = "صالير 6 000 DH: 3 000 DH أساسيات + 1 800 DH كماليات ومصاريف مرنة + 1 200 DH توفير صافي كل شهر (14 400 DH فالعام)!",
            caseStudyFr = "Sur 6 000 DH de revenu : 3 000 DH fixes + 1 800 DH plaisirs + 1 200 DH d'épargne mensuelle (14 400 DH/an) !",
            takeawayAr = "💡 الخلاصة: الميزانية ماشي زيرة، بل خريطة واضحة كتعطيك الإذن تصرف بضمير مرتاح.",
            takeawayFr = "💡 En résumé : Le budget n'est pas une prison, c'est l'autorisation de dépenser sans angoisse."
        ),
        SavingsArticleFull(
            id = "art_strat_cash_stuffing",
            categoryGroup = ArticleCategoryGroup.STRATEGY,
            categoryLabelAr = "استراتيجيات التوفير",
            categoryLabelFr = "Stratégies",
            tagColor = HighlighterGreen,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "✉️",
            titleAr = "استراتيجية الأظرفة النقدية (Cash Stuffing) للمبتدئين",
            titleFr = "La méthode des enveloppes pour dompter son cash",
            summaryAr = "طريقة الأظرفة الورقية لتقسيم المصروف الأسبوعي والسيطرة التامة على الكاش.",
            summaryFr = "Attribuer son budget hebdomadaire dans des enveloppes physiques pour bloquer les dérapages.",
            actionPointsAr = listOf(
                "• فكرة الأظرفة: سحب ميزانية المصاريف المرنة بداية كل أسبوع وتوزيعها في أظرفة مسماة ومغلقة.",
                "• أظرفة نموذجية: ظرف السويقة والماكلة، ظرف القهوة والمصروف الشخصي، وظرف الطوارئ الخفيفة.",
                "• قانون النفاد الصارم: إذا سالا ظرف القهوة نهار الخميس، ممنوع تفتح ظرف آخر؛ صبر حتى للإثنين الموالي!",
                "• جائزة نهاية الأسبوع: الصرف اللي شاط فالأظرفة نهار الأحد كيمشي مباشرة لحصالة التوفير الإضافية."
            ),
            actionPointsFr = listOf(
                "• Le principe : Retirer le budget des dépenses variables chaque début de semaine dans des enveloppes dédiées.",
                "• Enveloppes types : Enveloppe Courses/Marché, enveloppe Cafés/Sorties, enveloppe Imprévus quotidiens.",
                "• Règle de l'épuisement : Si l'enveloppe Café est vide le jeudi, on attend lundi sans piocher ailleurs !",
                "• Bonus du dimanche : La monnaie restante dans chaque enveloppe le week-end file droit dans la tirelire."
            ),
            caseStudyAr = "تقسيم 700 DH أسبوعياً فالأظرفة كيمنع السحب العشوائي من الكيشي ويوفر ما لا يقل عن 500 DH شهرياً من التسربات.",
            caseStudyFr = "Allouer 700 DH/semaine en enveloppes évite les retraits sauvages et épargne au moins 500 DH/mois.",
            takeawayAr = "💡 الخلاصة: الأظرفة كتخلي حدود ميزانيتك ملموسة باليد، والفلوس ما كتبقاش تذوب فالهواء.",
            takeawayFr = "💡 En résumé : Les enveloppes matérialisent vos limites et empêchent l'argent de s'évaporer."
        ),
        SavingsArticleFull(
            id = "art_strat_pay_yourself_first",
            categoryGroup = ArticleCategoryGroup.STRATEGY,
            categoryLabelAr = "استراتيجيات التوفير",
            categoryLabelFr = "Stratégies",
            tagColor = HighlighterGreen,
            readTimeAr = "⏱️ 2 دقائق",
            readTimeFr = "⏱️ 2 min",
            iconEmoji = "🚀",
            titleAr = "قاعدة: خلص راسك الأول نهار الصالير (Pay Yourself First)",
            titleFr = "Règle d'or : Payez-vous d'abord le jour de paie",
            summaryAr = "علاش التوفير نهار نزول الصالير ماشي فآخر الشهر هو الفارق بين النجاح والإفلاس.",
            summaryFr = "Pourquoi épargner dès réception du salaire plutôt qu'en fin de mois garantit 100% de succès.",
            actionPointsAr = listOf(
                "• كذبة 'غنخبي اللي شاط': فـ 95% من الحالات ما كيشيط والو فـ 30 فالشهر حيت المصاريف كتمدد لتلتهم كل درهم.",
                "• التوفير كفاتورة إجبارية: حول قسط الهدف لحساب منفصل نهار 1 فالشهر كأنه فاتورة ماء أو كراء لا تنازل فيها.",
                "• التكيف التلقائي: ملي كيبقى ليك فالكونت 80% من الصالير، عقلك كيتبرمج تلقائياً باش يعيش بيه ويكفيه الشهر كامل.",
                "• الأتمتة البنكية: دير أمر تحويل بنكي أوتوماتيكي نهار 2 فالشهر لحساب التوفير وتهنى من التردد."
            ),
            actionPointsFr = listOf(
                "• Le leurre de l'épargne résiduelle : En fin de mois, il ne reste presque jamais rien à épargner.",
                "• Épargner comme une facture : Virez votre objectif le 1er du mois comme une obligation non négociable.",
                "• Adaptation automatique : Quand il reste 80% du salaire sur le compte, le cerveau s'adapte sans souffrir.",
                "• Automatisation bancaire : Mettez en place un virement permanent le 2 de chaque mois."
            ),
            caseStudyAr = "تحويل 800 DH نهار الصالير = توفير 9 600 DH فالعام مضمونة 100%؛ بينما تأجيلها لآخر الشهر كيعطي فـ الغالب 0 DH!",
            caseStudyFr = "Virer 800 DH le jour de paie = 9 600 DH/an d'épargne garantie ; attendre la fin du mois = 0 DH.",
            takeawayAr = "💡 الخلاصة: خلص مستقبلك وأهدافك قبل ما تخلص أي تاجر أو مقهى.",
            takeawayFr = "💡 En résumé : Rémunérez votre avenir avant d'enrichir tous les commerces de la ville."
        ),

        // ──────────────────────────────────────────────────────────────────────
        // Category 3: SECURITY (الأمان المالي ومحاربة الديون)
        // ──────────────────────────────────────────────────────────────────────
        SavingsArticleFull(
            id = "art_sec_emergency_fund",
            categoryGroup = ArticleCategoryGroup.SECURITY,
            categoryLabelAr = "الأمان والديون",
            categoryLabelFr = "Sécurité & Dettes",
            tagColor = HighlighterBlue,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "🛡️",
            titleAr = "صندوق الطوارئ: الدرع الحقيقي قبل أي استثمار أو شراء كبير",
            titleFr = "Le fonds d'urgence : Votre bouclier anti-dettes numéro 1",
            summaryAr = "علاش صندوق الطوارئ هو أول ركيزة للأمان المالي وبشحال خاصك تبدا وفين تحطو.",
            summaryFr = "Pourquoi le fonds de secours est la priorité absolue avant tout grand projet et où le loger.",
            actionPointsAr = listOf(
                "• شنو هو الصندوق: مبلغ مالي مخصص للكوارث الحقيقية فقط (مرض مفاجئ، عطب سيارة، توقف مؤقت للدخل).",
                "• المرحلة 1 (الدرع الأولي): جمع 1 000 إلى 2 000 درهم كصدمة أولية كتحميك من طلب الكريدي عند أول بان.",
                "• المرحلة 2 (الصندوق الكامل): جمع مصاريف شهر إلى 3 أشهر من الأساسيات في حساب آمن ومتاح.",
                "• مكان الصندوق: حساب توفير بدفتر (Compte sur carnet) معزول وبلا بطاقة كيشي باش ما تغريكش السحوبات."
            ),
            actionPointsFr = listOf(
                "• Définition : Des liquidités strictement réservées aux vrais imprévus graves (santé, panne de voiture, perte d'emploi).",
                "• Étape 1 (Mini bouclier) : Rassemblez 1 000 à 2 000 DH pour amortir le premier choc sans vous endetter.",
                "• Étape 2 (Fonds complet) : Sécurisez 1 à 3 mois de charges vitales incompressibles sur un compte épargne.",
                "• Emplacement idéal : Compte sur carnet séparé, sans carte de retrait, pour sanctuariser l'argent."
            ),
            caseStudyAr = "وجود 3 000 DH فصندوق الطوارئ منعك من سلف استهلاكي عند وقوع عطب فالطوموبيل، ووفر عليك فوائد وضغط نفسي رهيب.",
            caseStudyFr = "Avoir 3 000 DH de réserve évite un crédit conso d'urgence à 12% lors d'une panne automobile.",
            takeawayAr = "💡 الخلاصة: صندوق الطوارئ ماشي فلوس راكدة، بل هو بوليصة تأمينك ضد تقلبات الزمان.",
            takeawayFr = "💡 En résumé : Le fonds d'urgence n'est pas de l'argent qui dort, c'est votre tranquillité d'esprit."
        ),
        SavingsArticleFull(
            id = "art_sec_debt_snowball",
            categoryGroup = ArticleCategoryGroup.SECURITY,
            categoryLabelAr = "الأمان والديون",
            categoryLabelFr = "Sécurité & Dettes",
            tagColor = HighlighterBlue,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "❄️",
            titleAr = "خطة التخلص من الديون: استراتيجية كرة الثلج (Debt Snowball)",
            titleFr = "La méthode boule de neige pour liquider ses dettes",
            summaryAr = "كيفاش ترتب ديونك وتخلص منها خطوة بخطوة وتسترجع حريتك المالية.",
            summaryFr = "Classer ses dettes de la plus petite à la plus grande pour créer un élan psychologique irrésistible.",
            actionPointsAr = listOf(
                "• جرد الديون: قيد كاع الكريديات اللي عليك فـ ورقة من الأصغر للأكبر بغض النظر عن نسبة الفائدة.",
                "• الانتصار السريع: سدد الحد الأدنى فكلشي، وركز كل درهم زايد باش تسد أصغر دين الأول.",
                "• شحنة الحماس النفسي: ملي كتسد أول دين كتحس بانتصار كبير، وكتوجه مبلغه للدين اللي موراه ككرة ثلج كبرات.",
                "• وقف النزيف: قطع كاع بطاقات الكريدي الاستهلاكي وممنوع تاخد سلف جديد باش تخلص سلف قديم."
            ),
            actionPointsFr = listOf(
                "• Inventaire total : Listez toutes vos dettes de la plus petite à la plus grande sur papier.",
                "• Victoire rapide : Payez le minimum légal sur tout, et jetez chaque dirham d'extra sur la plus petite dette.",
                "• Élan psychologique : Éliminer la première dette libère une mensualité pour écraser la suivante en boule de neige.",
                "• Stop absolu : Interdiction de souscrire un nouveau crédit pour tenter de rembourser un ancien."
            ),
            caseStudyAr = "سداد دين 1 500 DH لصديق فـ شهرين كيعطيك طاقة وثقة باش تسدد كريدي 8 000 DH فـ 8 أشهر وتتهنى نهائياً!",
            caseStudyFr = "Rembourser une dette de 1 500 DH à un proche en 2 mois donne la force d'éliminer un crédit de 8 000 DH.",
            takeawayAr = "💡 الخلاصة: التخلص من الديون معركة نفسية قبل ما تكون حسابية؛ الانتصارات الصغيرة كتصنع المعجزات.",
            takeawayFr = "💡 En résumé : Le désendettement est une victoire mentale : les petits succès créent les grands exploits."
        ),
        SavingsArticleFull(
            id = "art_sec_sinking_funds",
            categoryGroup = ArticleCategoryGroup.SECURITY,
            categoryLabelAr = "الأمان والديون",
            categoryLabelFr = "Sécurité & Dettes",
            tagColor = HighlighterBlue,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "🌙",
            titleAr = "المصاريف السنوية: العيد الكبير، رمضان، والدخول المدرسي بلا كريدي",
            titleFr = "Sinking Funds : Anticiper l'Aïd, le Ramadan et la rentrée",
            summaryAr = "كيفاش تحول المصاريف السنوية الكبيرة لأقساط شهرية مجهرية كتحميك من مفاجآت المواسم.",
            summaryFr = "Transformer les gros chocs annuels marocains en minuscules provisions mensuelles indolores.",
            actionPointsAr = listOf(
                "• حقيقة المناسبات: العيد والدخول المدرسي والتأمين ماشي مفاجآت؛ راهم كيجيو كل عام فـ نفس التوقيت!",
                "• حساب الحصص الشهرية: جمع تكلفة المناسبات السنوية وقسمها على 12 شهر (Sinking Funds).",
                "• صندوق المناسبات المنفصل: وفر 300 أو 400 درهم شهرياً مخصصة لهاد الباب فظرف أو حساب مستقل.",
                "• نهار المناسبة: كتلقى ثمن الحولي أو الكسوة واجد كاش، وكتدوز العيد فرحان بلا هم الطريطات مورا العيد."
            ),
            actionPointsFr = listOf(
                "• Événements certains : L'Aïd, la rentrée scolaire et les assurances arrivent toujours à date fixe.",
                "• Lissage mensuel : Additionnez ces dépenses annuelles et divisez-les par 12 mois dans une provision dédiée.",
                "• Enveloppe spéciale : Réservez 300 à 400 DH chaque mois sur un compte ou une enveloppe saisons.",
                "• Le jour J : Le budget de la fête est disponible comptant, sans aucun stress de dettes post-événement."
            ),
            caseStudyAr = "350 DH/شهر كتوفر ليك 4 200 DH نهار العيد الكبير؛ كتشري حولي كاش بلا ما تمد يدك للكريدي ولا تنقص من مصاريف دارك.",
            caseStudyFr = "350 DH/mois permettent de réunir 4 200 DH pour l'Aïd : achat comptant sans emprunt ni rogner le budget du foyer.",
            takeawayAr = "💡 الخلاصة: التخطيط المسبق كيحول الجبل المالي لكومة رمل صغيرة كتجاوزها بسهولة.",
            takeawayFr = "💡 En résumé : Provisionner transforme une montagne de dépenses en une colline facilement franchissable."
        ),

        // ──────────────────────────────────────────────────────────────────────
        // Category 4: HOUSEHOLD (التقضية ومصاريف البيت واليومي)
        // ──────────────────────────────────────────────────────────────────────
        SavingsArticleFull(
            id = "art_house_suika_vs_super",
            categoryGroup = ArticleCategoryGroup.HOUSEHOLD,
            categoryLabelAr = "التقضية ومصاريف البيت",
            categoryLabelFr = "Maison & Quotidien",
            tagColor = HighlighterYellow,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "🛒",
            titleAr = "هندسة التقضية الأسبوعية: السويقة والمارشي vs السوبرماركت الكبير",
            titleFr = "La stratégie des courses : Le marché vs les grandes surfaces",
            summaryAr = "أسرار توفير 30% إلى 40% فـ قفة الأسبوع بتوزيع ذكي لأماكن الشراء.",
            summaryFr = "Comment économiser 30 à 40% sur son panier hebdomadaire en répartissant intelligemment ses achats.",
            actionPointsAr = listOf(
                "• قاعدة التخصص: الخضر، الفواكه، البيض، واللحوم من المارشي أو السويقة الأسبوعية (طري وأرخص بـ 40%).",
                "• السوبرماركت للمنظفات فقط: حصر زيارة السوبرماركت فـ المواد المنظفة والأساسية اللي فيها عروض حقيقية.",
                "• التسوق بمعدة ممتلئة: ما تمشيش تقضى وأنت جيعان نهائياً؛ الجوع كيخليك تشري 50% كماليات وسناكات زايدة.",
                "• الوجبات الأسبوعية المسبقة: حدد جدول وجبات الأسبوع قبل ما تخرج تقضى، باش ما تشريش خضرة تخمج فالفران."
            ),
            actionPointsFr = listOf(
                "• Spécialisation des achats : Fruits, légumes, œufs et viandes au marché local (produits frais 40% moins chers).",
                "• Supermarché ciblé : Réservez les grandes surfaces aux produits d'entretien et promos de gros réelles.",
                "• Jamais l'estomac vide : Faire ses courses en ayant faim fait acheter 50% de snacks et douceurs superflues.",
                "• Plan de repas hebdo : Planifiez vos menus avant d'aller faire le marché pour éviter tout gaspillage alimentaire."
            ),
            caseStudyAr = "قفة أسبوعية من السويقة كتقام بـ 250 DH مقابل 420 DH فنفس المكونات من السوبرماركت = توفير 680 DH كل شهر (8 160 DH سنوياً)!",
            caseStudyFr = "Un panier hebdo au marché coûte 250 DH contre 420 DH en hypermarché = 680 DH d'économie/mois (8 160 DH/an) !",
            takeawayAr = "💡 الخلاصة: فرق أماكن الشراء كيوفر ميزانية سفرية كاملة فالعام بدون أي نقص فالجودة.",
            takeawayFr = "💡 En résumé : Répartir ses achats finance les vacances d'été sans réduire la qualité des repas."
        ),
        SavingsArticleFull(
            id = "art_house_cafe_lunch",
            categoryGroup = ArticleCategoryGroup.HOUSEHOLD,
            categoryLabelAr = "التقضية ومصاريف البيت",
            categoryLabelFr = "Maison & Quotidien",
            tagColor = HighlighterYellow,
            readTimeAr = "⏱️ 2 دقائق",
            readTimeFr = "⏱️ 2 min",
            iconEmoji = "☕",
            titleAr = "معادلة القهاوي والغداء برا: استمتع مع صحابك بلا ما تفقر جيبك",
            titleFr = "Cafés et déjeuners dehors : Profiter sans plomber son budget",
            summaryAr = "كيفاش تضبط مصاريف القهوة والزنقة بنظام ذكي يجمع بين المتعة والتوفير.",
            summaryFr = "Concilier moments conviviaux au café et déjeuners au bureau sans ruiner son épargne.",
            actionPointsAr = listOf(
                "• ما تقطعش القهوة: قطع العادات فجأة كيولد انتكاسة؛ الهدف هو ترشيد التردد وليس الحرمان التام.",
                "• قهوة الدار/الخدمة للصباح: شرب قهوة الصباح فالدار أو المكتب، وخصص جلسة المقهى كمتعة مع الأصدقاء.",
                "• تحضير الغداء (Meal Prep): وجد طويجين أو غداك فالدار وخده معاك للخدمة 3 مرات فالسيمانة.",
                "• الأثر التراكمي: تقليص تردد القهوة من 3 مرات لمرة واحدة يومياً كيرجع ليك مئات الدراهم شهرياً."
            ),
            actionPointsFr = listOf(
                "• Ne pas tout couper : Les privations brutales échouent ; l'objectif est la modération sans frustration.",
                "• Café maison le matin : Prenez le café du réveil chez vous ou au bureau, et gardez le bistrot pour les amis.",
                "• Gamelles maison (Meal prep) : Emportez votre déjeuner préparé à la maison 3 fois par semaine au travail.",
                "• Effet cumulé : Passer de 3 cafés extérieurs à 1 seul par jour dégage des centaines de dirhams chaque mois."
            ),
            caseStudyAr = "قهوة الصباح + سناك برا (25 DH يومياً) = 750 DH/شهر. تحضير غداء الدار وقهوة وحدة = توفير 450 DH شهرياً (5 400 DH فالعام)!",
            caseStudyFr = "Café du matin + snack dehors (25 DH/jour) = 750 DH/mois. Gamelle maison + 1 café = 450 DH épargnés/mois (5 400 DH/an) !",
            takeawayAr = "💡 الخلاصة: المتعة ماشي فعدد القهاوي، بل فقيمة الوقت اللي كتدوزو مع الناس اللي كتعز.",
            takeawayFr = "💡 En résumé : Le plaisir réside dans la compagnie des amis, pas dans le nombre de tasses consommées."
        ),
        SavingsArticleFull(
            id = "art_house_bills_cleanup",
            categoryGroup = ArticleCategoryGroup.HOUSEHOLD,
            categoryLabelAr = "التقضية ومصاريف البيت",
            categoryLabelFr = "Maison & Quotidien",
            tagColor = HighlighterYellow,
            readTimeAr = "⏱️ 2 دقائق",
            readTimeFr = "⏱️ 2 min",
            iconEmoji = "💡",
            titleAr = "ترشيد فواتير الماء والضو، وتطهير الاشتراكات الرقمية",
            titleFr = "Optimiser l'électricité, l'eau et assainir ses abonnements",
            summaryAr = "أبواب استنزاف خفية فالدار والأنترنت كتاكل 300 إلى 600 درهم شهرياً بدون فائدة.",
            summaryFr = "Petites fuites invisibles à la maison et forfaits dormants qui coûtent 300 à 600 DH/mois pour rien.",
            actionPointsAr = listOf(
                "• جرد الفورفيات: واش بصح محتاج فورفي 200 DH ولا كافيك فورفي 99 DH مع الويفي فالدار والخدمة؟",
                "• الاشتراكات الميتة: لغي فوراً أي منصة مسلسلات، تطبيق، أو اشتراك جيم ما استعملتيهش فهاد الأسبوعين.",
                "• مراقبة أشطر الماء والكهرباء: رد بالك تفوت الشطر الاقتصادي؛ المصابيح الاقتصادية وإطفاء الأجهزة كيوفر حتى 25%.",
                "• مراجعة عروض الاتصالات سنوياً: شركات الاتصالات كدير عروض جديدة أرخص؛ عيط ليهم وبدل الباقة القديمة."
            ),
            actionPointsFr = listOf(
                "• Audit des forfaits : Avez-vous besoin d'un forfait à 200 DH alors que vous êtes sous Wi-Fi maison/bureau ?",
                "• Résiliation des dormants : Supprimez sur-le-champ les plateformes de streaming ou applis non consultées depuis 15 jours.",
                "• Maîtrise des tranches d'eau/élec : Évitez le passage aux tranches supérieures avec des ampoules LED et bon sens.",
                "• Négociation annuelle : Les opérateurs lancent régulièrement des offres plus avantageuses ; mettez à jour votre forfait."
            ),
            caseStudyAr = "تخفيض فورفي هاتف من 199 DH لـ 99 DH + إلغاء اشتراك بث مهجور 80 DH + ضبط شطر الكهرباء = توفير 280 DH شهرياً (3 360 DH فالعام)!",
            caseStudyFr = "Forfait mobile abaissé de 199 à 99 DH + désabonnement streaming 80 DH + LED = 280 DH/mois épargnés (3 360 DH/an) !",
            takeawayAr = "💡 الخلاصة: الفلوس اللي كتوفرها من هاد الأبواب كتكفيك تمول أهدافك بلا ما تقيس صاليرك.",
            takeawayFr = "💡 En résumé : Ces économies invisibles financent vos projets sans rogner votre confort de vie."
        ),

        // ──────────────────────────────────────────────────────────────────────
        // Category 5: INCOME (الدخل المتغير وبداية الاستثمار)
        // ──────────────────────────────────────────────────────────────────────
        SavingsArticleFull(
            id = "art_inc_variable_freelance",
            categoryGroup = ArticleCategoryGroup.INCOME,
            categoryLabelAr = "الدخل والاستثمار",
            categoryLabelFr = "Revenus & Croissance",
            tagColor = HighlighterYellow,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "💼",
            titleAr = "تدبير الدخل المتغير: خطة الحرفيين، التجار، والفريلانسرز",
            titleFr = "Gérer un revenu variable : Artisans, commerçants & freelances",
            summaryAr = "كيفاش تعيش باستقرار وتوفر بانتظام واخا مدخولك كيتبدل من شهر لشهر.",
            summaryFr = "Stabiliser ses finances et épargner quand le chiffre d'affaires varie d'un mois à l'autre.",
            actionPointsAr = listOf(
                "• وهم الشهر القوي: أكبر غلط كيديروه أصحاب الدخل المتغير هو رفع المصاريف والكماليات ملي كيكون شهر مزيان.",
                "• الصالير الافتراضي الأدنى: حدد لنفسك راتباً شهرياً ثابتاً كيساوي متوسط أضعف 3 أشهر، وعيش بيه فقط.",
                "• حساب العازل (Buffer Account): فـ الشهور القوية، صب كل الفائض فـ حساب العازل باش يغطي الشهور الميتة.",
                "• التوفير كنسبة مئوية: حدد نسبة ثابتة (مثلاً 20%) من أي دخل يدخل كتمشي مباشرة لحساب التوفير."
            ),
            actionPointsFr = listOf(
                "• Le piège du bon mois : L'erreur classique est d'augmenter son train de vie dès qu'un mois rapporte plus.",
                "• Salaire plancher : Versez-vous un salaire mensuel fixe équivalent à la moyenne de vos 3 mois les plus creux.",
                "• Compte tampon (Buffer) : Les mois fastes, stockez la totalité des surplus pour financer les mois calmes.",
                "• Épargne en pourcentage : Isolez 20% de chaque rentrée d'argent directement sur le compte épargne."
            ),
            caseStudyAr = "تاجر كيدخل فشهور الصيف 15 000 DH وفالشتاء 4 000 DH: تحديد صالير 6 000 DH وعزل الفائض كيحميه من ديون الشتاء ويخليه يوفر بانتظام.",
            caseStudyFr = "Commerçant gagnant 15 000 DH en été et 4 000 DH en hiver : un salaire fixe de 6 000 DH lisse l'année sans dette.",
            takeawayAr = "💡 الخلاصة: الانضباط فالشهور القوية هو اللي كيحميك ويعطيك راحة البال فالشهور الضعيفة.",
            takeawayFr = "💡 En résumé : La discipline lors des mois prospères achète votre sérénité lors des périodes calmes."
        ),
        SavingsArticleFull(
            id = "art_inc_micro_invest",
            categoryGroup = ArticleCategoryGroup.INCOME,
            categoryLabelAr = "الدخل والاستثمار",
            categoryLabelFr = "Revenus & Croissance",
            tagColor = HighlighterYellow,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "📈",
            titleAr = "الخطوات الأولى للاستثمار: ما تخليش فلوسك ياكلها التضخم",
            titleFr = "Premiers pas vers l'investissement : Battre l'inflation",
            summaryAr = "شنو دير بالمدخرات من بعد ما تكمل صندوق الطوارئ وهدفك المالي.",
            summaryFr = "Comment faire fructifier son épargne une fois le fonds d'urgence et le premier objectif atteints.",
            actionPointsAr = listOf(
                "• التضخم والسوسة الصامتة: 10 000 DH محطوطة تحت الفراش لمدة 5 سنين كتفقد 20% إلى 30% من قيمتها الشرائية.",
                "• الترتيب الذهبي: 1) تخلص من الديون، 2) عمر صندوق الطوارئ، 3) عاد بدا تفكر فالاستثمار.",
                "• الاستثمار فالذات: أحسن استثمار كيبدا فتعلم مهارة جديدة أو دورة تكوينية كترفع دخلك فالخدمة أو الفريلانس.",
                "• الأصول الملموسة البسيطة: الاستثمار فـ الذهب، حسابات التوفير البنكية، أو المساهمة فـ تجارة مدروسة ومضمونة."
            ),
            actionPointsFr = listOf(
                "• L'érosion de l'inflation : 10 000 DH sous le matelas pendant 5 ans perdent 20 à 30% de pouvoir d'achat.",
                "• Ordre de priorité : 1) Rembourser les dettes, 2) Remplir le fonds de secours, 3) Investir sereinement.",
                "• Investir en soi : La formation professionnelle et de nouvelles compétences offrent le meilleur rendement.",
                "• Actifs tangibles : Épargne rémunérée, or d'investissement ou parts dans une activité commerciale saine."
            ),
            caseStudyAr = "استثمار 500 DH شهرياً فـ تطوير مهارة أو أصل منتج بعد 3 سنوات كتعطي عائد كيفوق 30% مقارنة بإبقائها راكدة فالحساب الجاري.",
            caseStudyFr = "Placer 500 DH/mois dans une compétence ou un actif rentable sur 3 ans dépasse 30% de gain face au compte courant.",
            takeawayAr = "💡 الخلاصة: التوفير كيحميك، ولكن الاستثمار هو اللي كيبني ثروتك واستقلالك المالي.",
            takeawayFr = "💡 En résumé : L'épargne protège votre présent ; l'investissement construit votre indépendance future."
        ),
        SavingsArticleFull(
            id = "art_inc_mindset_wealth",
            categoryGroup = ArticleCategoryGroup.INCOME,
            categoryLabelAr = "الدخل والاستثمار",
            categoryLabelFr = "Revenus & Croissance",
            tagColor = HighlighterYellow,
            readTimeAr = "⏱️ 3 دقائق",
            readTimeFr = "⏱️ 3 min",
            iconEmoji = "💎",
            titleAr = "عقلية الوفرة مقابل عقلية الفقر: كيفاش تفكر بحال الأغنياء؟",
            titleFr = "Mentalité d'abondance vs mentalité de pénurie",
            summaryAr = "الفارق الجوهري بين اللي كيركز على الاستهلاك والمظاهر، واللي كيركز على بناء الأصول.",
            summaryFr = "La différence fondamentale entre ceux qui privilégient le paraître et ceux qui bâtissent des actifs durables.",
            actionPointsAr = listOf(
                "• شراء الأصول ماشي الالتزامات: الفقير كيشري كماليات كتاكل الفلوس، والذكي كيشري أصول كدخل ليه الفلوس.",
                "• الصبر والاستثمار طويل المدى: الأهداف المالية الكبيرة كتحتاج نفس طويل والتزام يومي مستمر.",
                "• الابتعاد عن الربح السريع: أي خطة كتوعدك بالثراء السريع بدون مجهود راها فـ الغالب فخ أو نصب.",
                "• مراجعة الميزانية كعادة مقدسة: 10 دقائق كل يوم أحد لتقييد المصاريف كتخليك سيد مصيرك المالي."
            ),
            actionPointsFr = listOf(
                "• Actifs contre passifs : Achetez ce qui met de l'argent dans votre poche, pas ce qui l'en retire en entretien.",
                "• Patience et long terme : Les grands patrimoines se construisent avec constance et régularité sur des années.",
                "• Fuir les gains miracles : Toute opportunité promettant la richesse sans effort est une arnaque garantie.",
                "• Le bilan dominical : 10 minutes chaque dimanche pour faire le point sur vos dépenses assure votre maîtrise financière."
            ),
            caseStudyAr = "الالتزام بتوفير 500 DH شهرياً واستثمارها لمدة 10 سنوات كيعطيك أزيد من 90 000 DH مع العوائد المركبة، كتبني ليك حرية مالية حقيقية!",
            caseStudyFr = "Épargner 500 DH/mois avec intérêts composés sur 10 ans génère plus de 90 000 DH de patrimoine solide !",
            takeawayAr = "💡 الخلاصة: الثروة ماشي شحال كتدخل، بل شحال كتحتفظ بيه وكيفاش كتخليه يخدم لصالحك.",
            takeawayFr = "💡 En résumé : La vraie richesse ne dépend pas de ce que vous gagnez, mais de ce que vous conservez."
        )
    )

    fun getArticlesByCategory(category: ArticleCategoryGroup): List<SavingsArticleFull> {
        return if (category == ArticleCategoryGroup.ALL) {
            SAVINGS_ARTICLES
        } else {
            SAVINGS_ARTICLES.filter { it.categoryGroup == category }
        }
    }

    fun getRecommendedArticles(recommendedIds: List<String>): List<SavingsArticleFull> {
        if (recommendedIds.isEmpty()) return SAVINGS_ARTICLES
        return SAVINGS_ARTICLES.sortedBy { article ->
            val idx = recommendedIds.indexOf(article.id)
            if (idx >= 0) idx else 999
        }
    }
}
