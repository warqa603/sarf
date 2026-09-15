# Hssabi — Savings Questionnaire & Offline Diagnostic Engine
## الهيكل النهائي للقيسيونير + الدياگنوستيك الذكي بدون إنترنت

> **الهدف:** بناء نظام تشخيص مالي محلي (Offline) يعطي للمستخدم المغربي نتيجة تبدو "مفصلة عليه" وليس مجرد نصائح عامة.  
> **المبدأ:** كل جواب يخلق `Fact/Tag`، وكل Fact يدخل في الحسابات والقواعد، ثم يخرج منه Diagnostic واضح + خطة + Simulateurs + Conseils مرتبطة مباشرة بوضعية المستخدم.

---

# 0) الفلسفة العامة

القيسيونير لا يجب أن يكون 40 سؤالاً يظهرون للجميع.

الأفضل هو:
- **12–15 سؤال Core** يجاوب عليهم الجميع.
- **أسئلة Branching** تظهر فقط عند الحاجة.
- أغلب المستخدمين: 15–20 جواب.
- الحالات المعقدة: حتى 25–30 جواب.
- مدة مستهدفة: 3 إلى 6 دقائق.
- كل سؤال يجب أن يغيّر شيئاً في التشخيص أو التوصية. إذا لم يغيّر شيئاً، لا نطرحه.

النظام النهائي:

`Questionnaire → Facts → Metrics → Flags → Profile → Recommendations → Simulators → Content Feed`

---

# 1) البيانات الموجودة أصلاً من إنشاء الهدف

قبل Diagnostic، الـ Wizard ديال الهدف يعطي:

| Field | مثال |
|---|---|
| Goal Type | سيارة |
| Goal Title | سيارة جديدة |
| Target Amount | 100 000 DH |
| Current Saved | 18 000 DH |
| Deadline | 24 شهر |
| Priority | عالية |
| Goal Flexibility | المدة قابلة للتغيير / غير قابلة |
| Why it matters | تنقل للعمل |

من هاد المعلومات نحسب مباشرة:

```text
remaining = targetAmount - currentSaved
requiredMonthly = remaining / monthsRemaining
requiredWeekly = requiredMonthly * 12 / 52
requiredDaily = requiredMonthly * 12 / 365
requiredSavingRate = requiredMonthly / netMonthlyIncome
```

لكن ما نعطيوش حكم قبل ما نعرفو واقع المستخدم.

---

# 2) المرحلة A — الوضع الشخصي والأسري

## Q-A1 — كتدبر هاد الهدف بوحدك ولا مع شخص آخر؟
- بوحدي
- مع الزوج/الزوجة
- مع العائلة
- مساهمة مشتركة أخرى

**Tags**
- `GOAL_SOLO`
- `GOAL_SHARED`

**لماذا؟**
إذا الهدف مشترك، التشخيص يقدر يسول على المساهمة المشتركة بدل ما يحمل كامل الهدف على دخل شخص واحد.

---

## Q-A2 — شحال من شخص كتساهم فمصاريفهم بشكل منتظم؟
- غير راسي
- شخص واحد
- 2
- 3 أو أكثر

**Tags**
- `DEPENDENTS_NONE`
- `DEPENDENTS_LOW`
- `DEPENDENTS_HIGH`

**Use**
يزيد وزن "المرونة" ويمنع توصيات قاسية.

---

## Q-A3 — واش عندك التزامات عائلية ثابتة كل شهر؟
- لا
- نعم، مبلغ شبه ثابت
- نعم، كيختلف

إذا نعم:
### Q-A3b — تقريباً شحال فالشهر؟
Numeric / Range.

**Category:** Essential/Committed, لا يصنف Leak.

---

# 3) المرحلة B — الدخل

## Q-B1 — شحال متوسط الدخل الصافي الشهري؟
Numeric.

إذا المستخدم ما بغاش يعطي رقم:
- أقل من 3 000
- 3 000–5 000
- 5 000–8 000
- 8 000–12 000
- 12 000–20 000
- أكثر
- متغير بزاف

**مهم:** الحساب الدقيق يفضل Numeric، والRanges fallback فقط.

---

## Q-B2 — الدخل ديالك:
- قار تقريباً كل شهر
- كيتبدل شوية
- متغير بزاف
- موسمي

**Tags**
- `INCOME_STABLE`
- `INCOME_VARIABLE`
- `INCOME_HIGH_VARIANCE`
- `INCOME_SEASONAL`

---

## Q-B3 — إذا متغير: شحال تقريباً أقل شهر وآحسن شهر؟
- Lowest monthly income
- Highest monthly income

ومن الأفضل كذلك:
- Average last 3 months
- Average last 6 months (اختياري)

**Derived**
```text
incomeVariance = high - low
conservativeIncome = min(average, low + safetyMargin)
```

الـ Diagnostic ديال الدخل المتغير يبنى على conservative income، ماشي على أحسن شهر.

---

## Q-B4 — واش عندك دخل إضافي؟
Multi-select:
- لا
- عمل جانبي
- freelance
- كراء
- عمولات/bonus
- تجارة صغيرة
- دعم أسري
- آخر

إذا نعم:
### Q-B4b — واش هاد الدخل:
- منتظم
- مرات مرات
- موسمي

---

# 4) المرحلة C — المصاريف الأساسية المحمية

هنا الهدف ليس "قصّر هاد المصاريف"، بل نحسب الحد الحقيقي اللي ما خاصناش نمسوه.

نسول المبلغ الشهري التقريبي لكل بند، مع زر:
`ما عارفش → تقدير`

## Q-C1 — السكن
- كراء
- قرض سكن
- ساكن مع العائلة / بدون كراء مباشر
- آخر

Amount monthly.

## Q-C2 — الماء + الكهرباء
Amount monthly average.

## Q-C3 — الإنترنت + الهاتف الضروري
Amount monthly.

## Q-C4 — تقضية الدار / الماكلة الأساسية
Amount monthly.

## Q-C5 — التنقل الضروري للعمل/الدراسة
- نقل عمومي
- بنزين
- طاكسي
- مختلط
Amount monthly.

## Q-C6 — الصحة والأدوية
Amount monthly average.

## Q-C7 — الدراسة / الأطفال / الحضانة
Amount monthly.

## Q-C8 — تأمينات ضرورية
Monthly equivalent.

## Q-C9 — مصاريف أساسية أخرى
Custom label + amount.

**All get tag**
`PROTECTED_ESSENTIAL`

**Rule**
Never output:
> "نقص الدوا" / "نقص الكراء" / "قطع الإنترنت"  
بدل ذلك إذا العبء مرتفع:
> "المصاريف الأساسية عندك مرتفعة، لذلك الخطة خاصها تكون أهدأ أو المدة أطول."

---

# 5) المرحلة D — الديون والالتزامات المالية

## Q-D1 — واش عندك ديون أو أقساط؟
- لا
- قرض سكن
- قرض سيارة
- قرض استهلاك
- بطاقة/تسهيلات
- دين للأشخاص
- أكثر من واحد

## Q-D2 — شحال مجموع mensualités فالشهر؟
Numeric.

## Q-D3 — واش كتأخر فشي أداء أحياناً؟
- لا
- نادراً
- مرات
- غالباً

## Q-D4 — واش كتسلف باش تكمل الشهر؟
- أبداً
- نادراً
- بعض الشهور
- غالباً

**Flags**
- `DEBT_NONE`
- `DEBT_PRESENT`
- `DEBT_STRESS`
- `MONTH_END_BORROWING`
- `PAYMENT_DELAY`

**Metric**
```text
debtServiceRate = monthlyDebtPayments / netIncome
```

**Warning**
إذا rate مرتفع جداً أو المستخدم كيأخر الأداء:
الأولوية Diagnostic = تخفيف الضغط + mini emergency buffer، ماشي رفع التوفير بقوة.

> ملاحظة مرجعية: FMEF تستعمل 50% كعتبة تحذير من عبء مديونية مرتفع في محاكيها؛ داخل Hssabi تعامل معها كـ warning تعليمي، ماشي كقرار بنكي أو قاعدة مطلقة.

---

# 6) المرحلة E — صندوق الطوارئ والحماية

## Q-E1 — واش عندك فلوس للطوارئ منفصلين على الهدف؟
- لا
- أقل من 1 000 DH
- 1 000–3 000 DH
- تقريباً شهر ديال المصاريف
- 2–3 أشهر
- أكثر

إذا Numeric متاح أفضل.

## Q-E2 — إلا وقع مصروف ضروري مفاجئ بـ 3 000 DH، غالباً غادي:
- نخلصو من فلوس جاهزة
- ناخد من هدف التوفير
- نستعمل الكريدي/دين
- نسلف من شخص
- ما عارفش

## Q-E3 — فلوس الطوارئ فين كتكون؟
- نفس compte اليومي
- compte منفصل
- cash
- ما عنديش

**Metrics**
```text
essentialMonthly = sum(protectedEssentials)
emergencyMonths = emergencyFund / essentialMonthly
```

**Flags**
- `NO_EMERGENCY_FUND`
- `LOW_EMERGENCY_BUFFER`
- `EMERGENCY_READY`
- `GOAL_AT_RISK_FROM_EMERGENCY`

---

# 7) المرحلة F — واش الشهر كيسالي مرتاح؟

## Q-F1 — فآخر الشهر غالباً:
- كيبقى مبلغ مزيان
- كيبقى شوية
- كنكون قريب للصفر
- كيسالي المال قبل الشهر
- كنحتاج تسلاف/كريدي

## Q-F2 — واش عارف تقريباً فين كيمشي الصرف؟
- نعم مزيان
- تقريباً
- لا

## Q-F3 — واش كتكتب/كتتبع المصاريف؟
- تقريباً كلشي
- غير الكبار
- مرات
- لا

**Flags**
- `CASHFLOW_HEALTHY`
- `CASHFLOW_TIGHT`
- `CASHFLOW_NEGATIVE`
- `SPENDING_VISIBILITY_LOW`
- `TRACKING_GOOD`

---

# 8) المرحلة G — المصاريف المرنة / Leaks

هنا يكون السؤال Multi-select:

> "شنو الحوايج اللي كتحس أنها كتجر منك الفلوس أكثر من اللازم؟"

Options:
- القهوة / الفطور برا
- الماكلة برا / delivery
- shopping / الملابس / الكماليات
- sorties / restaurants
- طاكسي غير ضروري
- اشتراكات رقمية
- ألعاب / in-app purchases
- paris / jeux d'argent
- cigarettes / habit أخرى (optional neutral category)
- هدايا ومناسبات بلا budget
- مشتريات online
- مصاريف صغيرة ما كنعرفش فين كتمشي
- ما عنديش مشكل واضح
- آخر

لكل category مختارة، نسول Dynamic Branch:

## مثال Coffee branch
### Q-G-COFFEE-1
شحال تقريباً كل مرة؟
- 10
- 15
- 20
- 25
- 30+
- custom

### Q-G-COFFEE-2
شحال من مرة فالأسبوع؟
1–7.

### Q-G-COFFEE-3
شنو مستعد تبدل بلا ما تحس بالحرمان؟
- نقص مرة وحدة فالأسبوع
- نقص للنصف
- ندير بديل من الدار
- ما بغيتش نمس هاد المصروف

**Derived**
```text
monthlyCost = costPerUse * weeklyFrequency * 52 / 12
annualCost = costPerUse * weeklyFrequency * 52
savingAt25 = annualCost * 0.25
savingAt50 = annualCost * 0.50
```

نفس Branch pattern ل:
- eating out
- shopping
- outings
- taxis
- subscriptions

---

# 9) Branch خاص بالاشتراكات

## Q-G-SUB-1 — شنو subscriptions اللي عندك؟
- streaming
- cloud
- apps
- gym
- gaming
- other

## Q-G-SUB-2 — شحال المجموع الشهري؟

## Q-G-SUB-3 — واش كتستعملهم كاملين؟
- نعم
- كاين واحد/جوج قليل
- بزاف ما كنستعملهمش

**Recommendation**
مش "حبسهم كاملين"، بل:
> "راجع غير الاشتراكات اللي ما كتستعملهاش."

---

# 10) Branch خاص بالشوبينغ

## Q-G-SHOP-1
واش كتخطط قبل الشراء؟
- غالباً
- مرات
- قليل

## Q-G-SHOP-2
شنو كيحفز الشراء غالباً؟
- الحاجة فعلاً
- التخفيض
- الملل / المزاج
- social media
- مناسبة
- ما عارفش

## Q-G-SHOP-3
متوسط shopping المرن فالشهر؟

**Tags**
- `IMPULSE_BUYING`
- `DISCOUNT_TRIGGER`
- `EMOTIONAL_SPENDING`

---

# 11) Branch خاص بالماكلة برا

## Q-G-FOOD-1
شحال من مرة فالأسبوع؟

## Q-G-FOOD-2
متوسط كل مرة؟

## Q-G-FOOD-3
علاش غالباً؟
- الخدمة بعيدة / ضرورة
- ما كاينش وقت
- convenience
- خروج/متعة

**Critical context rule**
إذا السبب ضرورة عمل:
لا نقول "حبس الماكلة برا".
نقترح:
- يوم أو جوج meal prep
- سقف أسبوعي
- بديل أقل تكلفة

---

# 12) Branch خاص بالـ Paris / Jeux d'argent

إذا اختار المستخدم:
- نسول شحال تقريباً فالأسبوع/الشهر.
- واش كيرجع يزيد اللعب من بعد الخسارة؟
  - لا
  - مرات
  - غالباً

**Flag**
`LOSS_CHASING_RISK`

**التوصية**
- ما نحاولوش "نحسن استراتيجية اللعب".
- ما نقدموش calculator ديال الربح المتوقع.
- نحسب فقط التكلفة الحقيقية:
  - أسبوع
  - شهر
  - سنة
- رسالة واضحة:
> "الخسارة ما خاصهاش تولي سبب باش تزيد مبلغ آخر باش ترجعها. الأفضل تحط سقف صفري أو توقف مدة وتجرب تحول نفس المبلغ لهدفك."

إذا المستخدم كيبان تحت ضغط:
> "إذا حسيت بأن التحكم فهاد المصروف صعيب، طلب الدعم من شخص موثوق أو مختص أحسن من محاولة تعويض الخسارة."

---

# 13) المرحلة H — المصاريف الموسمية والسنوية

> "شنو المصاريف اللي كتجي مرة أو مرات قليلة فالعام وكتفاجئك؟"

Multi-select:
- التأمين
- vignette / سيارة
- صيانة السيارة
- الدخول المدرسي
- رمضان
- عيد الأضحى
- عيد الفطر
- سفر الصيف
- مناسبات/أعراس
- علاج/أسنان
- صيانة المنزل
- ضرائب/رسوم
- هدايا
- أخرى

لكل عنصر:
- المبلغ السنوي التقريبي
- الشهر المتوقع

**Derived**
```text
monthlySinkingFund = annualExpense / monthsUntilDue
```

**Flag**
`IRREGULAR_EXPENSE_UNFUNDED`

---

# 14) المرحلة I — سلوك التوفير

## Q-I1 — دابا كتدخر:
- أول الشهر
- وسط الشهر
- آخر الشهر إذا بقى
- بشكل غير منتظم
- ما كندخرش

## Q-I2 — واش كتدير تحويل ثابت؟
- نعم
- لا

## Q-I3 — إلا جاك دخل إضافي/bonus، غالباً:
- نوفر جزء
- نصرف أغلبو
- نسد التزامات
- حسب الحالة

## Q-I4 — شنو مبلغ التوفير اللي تقدر تدير مرتاح بلا ضغط؟
Numeric.

## Q-I5 — شنو أقل مبلغ تقدر تحافظ عليه حتى فشهر صعيب؟
Numeric.

**مهم**
هادي كتسمح نبنيو:
- `ComfortSaving`
- `MinimumSaving`

بدل رقم واحد مثالي.

---

# 15) المرحلة J — السلوك الشرائي

## Q-J1 — قبل شراء حاجة ماشي ضرورية:
- كنقرر بسرعة
- كنستنى شوية
- كنقارن ونفكر

## Q-J2 — واش التخفيض كيدفعك تشري حاجة ما كنتيش ناوي عليها؟
- بزاف
- مرات
- نادراً

## Q-J3 — واش كتدخل للسوق/المتجر بلائحة؟
- غالباً
- مرات
- لا

## Q-J4 — وسيلة الأداء اللي كتخليك تصرف أكثر فإحساسك؟
- cash
- card
- wallet/app
- ما كاينش فرق
- ما عارفش

هذه الأسئلة ماشي للحكم، بل لاختيار Advice:
- 24h rule
- shopping list
- cash cap
- wishlist

---

# 16) المرحلة K — حدود المستخدم: "شنو ما بغيتش نمسوه؟"

سؤال مهم جداً باش النصائح تكون إنسانية:

> "شنو الحوايج اللي ما بغيتيش الخطة تطلب منك تنقصها؟"

Multi-select:
- القهوة
- الخروج
- sport
- مساعدة العائلة
- سفر
- shopping
- ماكلة برا
- ما عنديش مشكل
- custom

**Tag**
`USER_PROTECTED_PREFERENCE`

التوصيات ما تقترحش تقليص مباشر لشي حاجة المستخدم أعلن أنها مهمة عنده؛ تبحث عن بدائل أخرى.

---

# 17) المرحلة L — المرونة في الهدف

## Q-L1 — الهدف بالنسبة ليك:
- ضروري جداً
- مهم
- nice-to-have

## Q-L2 — التاريخ:
- ثابت
- ممكن نزيد 3 أشهر
- ممكن نزيد 6 أشهر
- مرن

## Q-L3 — واش مستعد تزيد الدخل باش توصل؟
- لا
- ممكن
- نعم

## Q-L4 — واش مستعد تنقص شوية من المصاريف المرنة؟
- لا
- شوية
- متوسط
- بزاف لفترة قصيرة

---

# 18) المقاييس اللي يتحسبو محلياً

## 18.1 Cash Flow
```text
monthlyIncome
- protectedEssentials
- debtPayments
- familyCommitments
- averageFlexibleSpending
- monthlyEquivalentIrregularExpenses
= freeCashFlow
```

## 18.2 Current Saving Capacity
```text
realisticCapacity =
min(
  userComfortSaving,
  freeCashFlowAdjusted
)
```

## 18.3 Goal Gap
```text
goalGap = target - currentSaved
```

## 18.4 Goal Required Monthly
```text
requiredMonthly = goalGap / monthsRemaining
```

## 18.5 Feasibility Gap
```text
feasibilityGap = realisticCapacity - requiredMonthly
```

## 18.6 Emergency Coverage
```text
emergencyMonths = emergencyFund / essentialMonthly
```

## 18.7 Leak Potential
لكل leak:
```text
monthlyLeak
reasonableReduction
monthlyRecoverable
annualRecoverable
```

## 18.8 Irregular Expense Reserve
```text
monthlySinkingNeeds = sum(all annual/seasonal sinking funds)
```

---

# 19) تصنيف قابلية الهدف

ما نستعملوش "ناجح/فاشل".

## `COMFORTABLE`
realisticCapacity >= requiredMonthly * 1.15

Message:
> "الهدف داخل فالقدرة ديالك بهامش مريح."

## `FEASIBLE`
realisticCapacity قريب من requiredMonthly.

Message:
> "الهدف واقعي، غير خاص الاستمرار والتنظيم."

## `TIGHT`
requiredMonthly أكبر شوية من القدرة الحالية.

Message:
> "الهدف ممكن، ولكن الخطة الحالية غادي تضغط عليك شوية."

Then show:
- reduce 1–2 flexible leaks
- OR extend deadline

## `AGGRESSIVE`
الفارق كبير.

Message:
> "المبلغ والمدة الحاليين قاصحين على الميزانية. الأفضل نبدلو واحد منهم بدل ما نضغطو على المصاريف الأساسية."

## `UNSAFE_NOW`
- negative cashflow
- serious debt stress
- frequent borrowing

Message:
> "قبل ما نسرعو فهاد الهدف، خاصنا نرجعو مساحة آمنة فالشهر."

No shame.

---

# 20) Profile Engine — ماشي Persona واحدة

الأفضل Diagnostic يعطي:

### Primary Profile
مثلاً:
`دخل قار + هامش ضيق`

### Secondary Flags
- `بلا صندوق طوارئ`
- `تسرب مرتفع فالماكلة برا`
- `مصاريف سنوية ما موجدش ليها`
- `الهدف مستعجل`

مثال النتيجة:
> **البروفايل ديالك:** الدخل مستقر، ولكن الهامش الشهري ضيق بسبب التزامات ثابتة ومصاريف أكل برا متكررة. الهدف ممكن، ولكن باش توصل بلا ضغط خاصنا نربحو تقريباً 450 DH فالشهر أو نزيدو المدة 4 شهور.

هادشي أذكى من:
> "أنت مسرف."

---

# 21) ترتيب الأولويات في Diagnostic

Recommendation engine يمشي بهذا الترتيب:

### Priority 1 — Safety
- دخل سلبي
- تأخر أقساط
- تسلاف آخر الشهر
- no emergency + fragile

### Priority 2 — Essential stability
- protected essentials
- family obligations
- annual unavoidable expenses

### Priority 3 — Goal feasibility
- required vs realistic saving

### Priority 4 — Controllable leaks
- coffee
- food out
- shopping
- subscriptions
- etc.

### Priority 5 — Habits
- pay yourself first
- 24h rule
- tracking
- weekly review

### Priority 6 — Optimization
- faster deadline
- higher savings
- extra income

---

# 22) شكل صفحة Diagnostic النهائي

## Section 1 — "الخلاصة ديالك"
3–4 أسطر فقط:
- income type
- financial margin
- top problem
- goal feasibility

مثال:
> "الهدف ديالك ممكن، ولكن دابا كيحتاج 2 300 DH فالشهر بينما الهامش المريح عندك تقريباً 1 750 DH. أكبر فرصتين عندك هما الماكلة برا والاشتراكات غير المستعملة."

---

## Section 2 — الأرقام المهمة
- الدخل
- الأساسيات
- الالتزامات/الديون
- المصاريف المرنة
- الهامش الحقيقي
- المطلوب للهدف

---

## Section 3 — "شنو ما خاصناش نمسو"
Protected expenses:
- السكن
- الصحة
- الأساسيات
- الدراسة
- commitments

---

## Section 4 — "فين كاينة الفرصة"
Top 1–3 leaks فقط.

مثال:
```text
ماكلة برا: 920 DH / شهر
إذا نقصتي 2 مرات فالأسبوع:
≈ +320 DH / شهر
≈ +3 840 DH / عام
```

---

## Section 5 — "الخطة اللي نقترحو"
### Plan A — مريح
1 700 DH/month → 28 months

### Plan B — متوازن
2 050 DH/month → 24 months
requires +350 DH recovery

### Plan C — سريع
2 400 DH/month → 21 months
only if user chose aggressive mode

---

## Section 6 — "دير هاد 3 حوايج هاد الأسبوع"
Only 3 personalized actions.

مثال:
1. حدد budget 450 DH للأكل برا.
2. وقف subscription واحد ما كتستعملوش.
3. حوّل 400 DH للهدف نهار الصالير.

---

## Section 7 — "جرب السيناريو"
Button:
`حاسبة التوفير`

---

# 23) Interactive Diagnostic Calculator

هاد الـ calculator يقرأ answers ديال questionnaire مسبقاً ويعمر inputs تلقائياً.

## Mode 1 — "فين كيمشي الصرف؟"
كل leak مع:
- cost/use
- frequency
- monthly
- yearly

## Mode 2 — "إلا نقصت شوية؟"
Slider:
- 0%
- 25%
- 50%
- 75%

Result:
```text
كتربح شهرياً: +X DH
كتربح سنوياً: +Y DH
الهدف يقدر يقرب بـ Z أشهر
```

## Mode 3 — "إلا زدت المدة؟"
Slider deadline +1 to +12 months.

Shows new required monthly.

## Mode 4 — "إلا زدت التوفير؟"
+100 / +200 / +500 custom.

Shows new target date.

## Mode 5 — "شهر صعيب"
Simulate income -10% / -20%.
Shows whether plan survives.

## Mode 6 — "المصاريف السنوية"
Add annual expense:
amount + due date.
Shows monthly reserve.

## Mode 7 — "صندوق الطوارئ"
How many months coverage currently and target 1/3/6 months.

---

# 24) Recommendation Object

كل recommendation فالداتا عندها:

```kotlin
data class SavingsRecommendation(
    val id: String,
    val title: String,
    val body: String,
    val triggerTags: Set<String>,
    val excludeTags: Set<String>,
    val priority: Int,
    val category: RecommendationCategory,
    val estimatedMonthlyImpactRule: ImpactRule?,
    val articleIds: List<String>,
    val calculatorAction: CalculatorAction?
)
```

مثال:

```text
ID: FOOD_OUT_HALF
Trigger:
- FOOD_OUT_HIGH
Exclude:
- FOOD_OUT_REQUIRED_FOR_WORK
Body:
"جرب تنقص غير جوج مرات فالأسبوع بدل ما تحبس الماكلة برا كاملة."
Calculator:
cost × 2 saved occasions/week
```

---

# 25) Facts / Tags المقترحة

```text
INCOME_STABLE
INCOME_VARIABLE
INCOME_SEASONAL
CASHFLOW_HEALTHY
CASHFLOW_TIGHT
CASHFLOW_NEGATIVE
DEBT_PRESENT
DEBT_STRESS
PAYMENT_DELAY
MONTH_END_BORROWING
NO_EMERGENCY_FUND
LOW_EMERGENCY_BUFFER
EMERGENCY_READY
SPENDING_VISIBILITY_LOW
TRACKING_GOOD
COFFEE_HIGH
FOOD_OUT_HIGH
SHOPPING_HIGH
IMPULSE_BUYING
SUBSCRIPTIONS_UNUSED
TAXI_FLEX_HIGH
GAMBLING_SPEND
LOSS_CHASING_RISK
IRREGULAR_EXPENSE_UNFUNDED
SAVE_FIRST
SAVE_WHATS_LEFT
GOAL_COMFORTABLE
GOAL_TIGHT
GOAL_AGGRESSIVE
GOAL_FIXED_DEADLINE
GOAL_FLEXIBLE_DEADLINE
DEPENDENTS_HIGH
USER_PROTECTED_PREFERENCE
```

---

# 26) Rule Scoring

كل Advice كتجمع Score:

```text
+100 safety critical
+60 exact answer match
+40 goal relevance
+30 high financial impact
+20 seasonal relevance
+15 behavior match
-100 excluded/protected preference
-50 already completed
-30 recently shown
```

نعرض:
- 1 Critical max
- 2 High-value recommendations
- 3 Practical tips
- articles المرتبطة من بعد

---

# 27) ربط Diagnostic بالـ Conseils

مثال:

User:
- FOOD_OUT_HIGH
- NO_EMERGENCY_FUND
- INCOME_STABLE
- GOAL_CAR

Conseils feed starts with:
1. كيفاش تنقص الماكلة برا بلا حرمان
2. صندوق الطوارئ قبل المصاريف المفاجئة
3. الثمن الحقيقي للسيارة: التأمين والصيانة
4. Pay yourself first
5. budget أسبوعي

ماشي:
- مقال generic على الاستثمار.

---

# 28) إعادة التشخيص

Diagnostic ما يكونش مرة وحدة للأبد.

## Quick monthly check-in — 4 أسئلة
1. شحال قدرت توفر؟
2. شنو أكبر حاجة عطلتك؟
3. واش تبدل الدخل/الالتزامات؟
4. واش الهدف/التاريخ باقي نفسه؟

Then recompute.

Full questionnaire:
- عند إنشاء هدف جديد
- أو user chooses "إعادة التشخيص"

---

# 29) Data Model اقتراحي

```kotlin
data class FinancialProfileSnapshot(
    val goalId: String,
    val monthlyIncomeCentimes: Long?,
    val incomeType: IncomeType,
    val protectedExpensesCentimes: Long,
    val debtPaymentsCentimes: Long,
    val familyCommitmentsCentimes: Long,
    val flexibleExpensesCentimes: Long,
    val irregularMonthlyReserveCentimes: Long,
    val emergencyFundCentimes: Long,
    val comfortSavingCentimes: Long?,
    val minimumSavingCentimes: Long?,
    val tags: Set<String>,
    val answersVersion: Int,
    val createdAt: Long
)

data class SpendingLeak(
    val type: LeakType,
    val costPerUseCentimes: Long,
    val frequencyPerWeek: Double,
    val reason: LeakReason?,
    val userWillingness: ReductionWillingness
)

data class DiagnosticMetrics(
    val freeCashFlowCentimes: Long,
    val requiredMonthlyCentimes: Long,
    val feasibilityGapCentimes: Long,
    val emergencyMonths: Double?,
    val debtServiceRate: Double?,
    val recoverableMonthlyCentimes: Long,
    val status: GoalFeasibility
)
```

---

# 30) Diagnostic Result Model

```kotlin
data class DiagnosticResult(
    val summary: String,
    val profileLabel: String,
    val metrics: DiagnosticMetrics,
    val protectedAreas: List<String>,
    val topLeaks: List<LeakInsight>,
    val warnings: List<DiagnosticWarning>,
    val planOptions: List<SavingsPlanOption>,
    val topActions: List<ActionStep>,
    val recommendedContentIds: List<String>,
    val recommendedCalculatorModes: List<CalculatorMode>
)
```

---

# 31) Offline Intelligence Principle

التطبيق ما يحتاجش AI باش يكون ذكي.

الـ intelligence الأساسي يكون:

```text
Answers
→ normalized facts
→ exact calculations
→ tags
→ rule scoring
→ recommendations
→ content selection
```

AI يكون Extra فقط:
- user presses "حلل ليا أكثر"
- الـ AI ياخذ Profile Snapshot + DiagnosticResult
- يعيد الشرح بطريقة conversational
- ما يغيرش الأرقام المحسوبة محلياً
- ما يخترعش بيانات

هذا مهم:
**Local engine = source of truth**
**AI = explainer / coach**

---

# 32) أمثلة End-to-End

## Example A
Income: 9 000 DH  
Essentials: 4 300  
Debt: 900  
Flexible: 1 600  
Goal needs: 2 100/month  
Coffee: 25 × 6/week  
No emergency fund.

Output:
- Current free margin ≈ 2 200 before irregular reserve.
- Goal technically feasible but fragile.
- Coffee ≈ 650/month.
- Reducing 50% ≈ +325/month.
- First emergency milestone recommended alongside goal.
- Plan A: 1 700 goal + 300 emergency.
- Plan B after emergency: 2 000+ goal.
- Articles: emergency fund, coffee leak, pay yourself first.

---

## Example B
Variable income 5 000–12 000  
Average 8 000  
Fixed essentials 4 000  
No debt  
Goal 40 000 in 12 months.

Output:
- Do not budget from 8 000 blindly.
- Conservative base near low-income month.
- Required target 3 333/month is too aggressive relative to weak month.
- Recommend percentage contribution:
  - minimum base each month
  - % of income above baseline
- Build one-month buffer first/parallel.

---

## Example C
Income 7 000  
Debt payments 3 700  
Borrowing end of month  
Goal: travel 15 000.

Output:
- Diagnostic status: UNSAFE_NOW.
- No aggressive savings target.
- Mini emergency buffer.
- debt/budget pressure content.
- travel goal remains saved but plan paused/extended.
- no shame wording.

---

# 33) Sources / Editorial Basis

الهيكل المالي يعتمد على مبادئ منشورة من:
- **Fondation Marocaine pour l’Education Financière (FMEF):** إدارة الميزانية، تصنيف المصاريف، تحديد الأهداف، الادخار، القروض، محاكيات الميزانية والادخار.
- **AMMC:** الاحتفاظ بالسيولة للطوارئ قبل الاستثمار، تحديد الهدف والأفق، فهم المخاطر، وعدم الاعتماد على وعود الربح السريع.
- **ACAPS:** الحماية، التأمين، الادخار طويل المدى والتقاعد.

هذه المرجعيات تدعم فكرة أن التشخيص يبدأ بالموارد والمصاريف والأهداف، يفرق بين الحاجات والرغبات، ويراعي الالتزامات والسيولة والحماية قبل أي توصيات أكثر تقدماً.

---

# 34) القرار النهائي المقترح

القيسيونير ما يكونش "Form".
يكون **Financial Interview** ذكي.

والـ Diagnostic ما يكونش "نتيجة Score".
يكون:

1. **فهمنا وضعيتك**
2. **حسبنا القدرة الحقيقية**
3. **حمينا الضروريات**
4. **لقينا أكبر فرص التوفير**
5. **عطيناك 2–3 سيناريوهات**
6. **قلنا لك 3 حوايج تدير دابا**
7. **فتحنا لك Simulator باش تجرب بيدك**
8. **ربطنا لك المقالات المناسبة**

الهدف النهائي:
> المستخدم يحس أن Hssabi ما عطاهش "نصيحة عامة"، بل دار ليه تشخيص على المقاس باستعمال الأرقام والأجوبة ديالو، وكلشي يقدر يخدم Offline.
