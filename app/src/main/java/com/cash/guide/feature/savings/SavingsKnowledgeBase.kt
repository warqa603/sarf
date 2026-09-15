package com.cash.guide.feature.savings

import com.cash.guide.domain.JournalLedgerManager

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
}
