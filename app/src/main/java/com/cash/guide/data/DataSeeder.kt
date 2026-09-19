package com.cash.guide.data

import android.content.Context
import com.cash.guide.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

object DataSeeder {

    suspend fun seedCleanData(context: Context) = withContext(Dispatchers.IO) {
        val db = HssabiDatabase.getInstance(context)
        val calcDao = db.calculationDao()
        val groupDao = db.calculationGroupDao()
        val checklistDao = db.checklistDao()
        val noteDao = db.noteDao()
        val reminderDao = db.reminderDao()
        val contactDao = db.contactDao()

        // 1. Clear existing sample data (Savings/Épargne is deliberately preserved intact)
        calcDao.deleteAllCalculations()
        groupDao.deleteAllGroups()
        checklistDao.deleteAllChecklists()
        noteDao.deleteAllNotes()
        reminderDao.deleteAllReminders()
        contactDao.deleteAllContacts()

        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 3600_000L
        val day = 86400_000L

        // Detect language: Arabic / Darija vs French / Default
        val settingsRepo = SettingsRepository(context)
        val currentLang = runCatching { settingsRepo.appLanguage.first() }.getOrDefault("fr")
        val isArabic = currentLang == "ar" || currentLang == "dar" || Locale.getDefault().language == "ar"

        // 2. Insert Unified Groups (Calculations, Notes, Checklists, Contacts)
        val groups = if (isArabic) {
            listOf(
                // Calculation Groups
                CalculationGroupEntity("group_marche", "تقدية والمارشي 🛒", "#F4D66D", now - 15 * day, now - 15 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_dar", "مصاريف وفواتير الدار 🏠", "#9BD7D5", now - 15 * day, now - 15 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_voiture", "صيانة ومازوط السيارة 🚗", "#89B5D8", now - 14 * day, now - 14 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_chantier", "إصلاحات وصباغة الشقة 🔨", "#F7BDAB", now - 14 * day, now - 14 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_khdama", "رواتب وأجور الخدامة 👥", "#C9DDA0", now - 12 * day, now - 12 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_commerce", "سلعة وتجارة المحل 📦", "#B3C5E7", now - 12 * day, now - 12 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_loisirs", "خرجات واستراحة العائلة ☕", "#D3C5E5", now - 10 * day, now - 10 * day, "CALCULATIONS"),
                // Notes Groups
                CalculationGroupEntity("group_notes_projets", "أفكار ومشاريع 💡", "#F5B093", now - 15 * day, now - 15 * day, "NOTES"),
                CalculationGroupEntity("group_notes_contacts", "أرقام ومعارف موثوقة 📞", "#A8CFE3", now - 15 * day, now - 15 * day, "NOTES"),
                CalculationGroupEntity("group_notes_budget", "نصائح وتدبير الميزانية 💰", "#C9DDA0", now - 14 * day, now - 14 * day, "NOTES"),
                // Checklists Groups
                CalculationGroupEntity("group_check_courses", "تقدية ومشتريات 🛒", "#F4D66D", now - 15 * day, now - 15 * day, "CHECKLISTS"),
                CalculationGroupEntity("group_check_voyage", "سفر وعطل 🚗", "#89B5D8", now - 15 * day, now - 15 * day, "CHECKLISTS"),
                CalculationGroupEntity("group_check_maison", "مدرسة وترتيبات 📚", "#F7BDAB", now - 14 * day, now - 14 * day, "CHECKLISTS"),
                // Contacts Groups
                CalculationGroupEntity("group_contact_artisans", "حرفيين ومعلمين 🔨", "#89B5D8", now - 15 * day, now - 15 * day, "CONTACTS"),
                CalculationGroupEntity("group_contact_famille", "العائلة والمقربين 👨‍👩‍👧", "#F7BDAB", now - 15 * day, now - 15 * day, "CONTACTS"),
                CalculationGroupEntity("group_contact_sante", "صحة وأطباء 🩺", "#9BD7D5", now - 14 * day, now - 14 * day, "CONTACTS"),
                CalculationGroupEntity("group_contact_commerce", "موردين وسلعة 📦", "#F4D66D", now - 12 * day, now - 12 * day, "CONTACTS")
            )
        } else {
            listOf(
                // Calculation Groups
                CalculationGroupEntity("group_marche", "Taqdiya & Marché 🛒", "#F4D66D", now - 15 * day, now - 15 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_dar", "Mssarif d Dar 🏠", "#9BD7D5", now - 15 * day, now - 15 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_voiture", "Voiture & Mazout 🚗", "#89B5D8", now - 14 * day, now - 14 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_chantier", "Chantier & Travaux 🔨", "#F7BDAB", now - 14 * day, now - 14 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_khdama", "Salaires Khdama 👥", "#C9DDA0", now - 12 * day, now - 12 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_commerce", "Fournisseurs & Sel3a 📦", "#B3C5E7", now - 12 * day, now - 12 * day, "CALCULATIONS"),
                CalculationGroupEntity("group_loisirs", "Café & Famille ☕", "#D3C5E5", now - 10 * day, now - 10 * day, "CALCULATIONS"),
                // Notes Groups
                CalculationGroupEntity("group_notes_projets", "Idées & Projets 💡", "#F5B093", now - 15 * day, now - 15 * day, "NOTES"),
                CalculationGroupEntity("group_notes_contacts", "Contacts & Artisans 📞", "#A8CFE3", now - 15 * day, now - 15 * day, "NOTES"),
                CalculationGroupEntity("group_notes_budget", "Conseils & Budget 💰", "#C9DDA0", now - 14 * day, now - 14 * day, "NOTES"),
                // Checklists Groups
                CalculationGroupEntity("group_check_courses", "Courses & Taqdiya 🛒", "#F4D66D", now - 15 * day, now - 15 * day, "CHECKLISTS"),
                CalculationGroupEntity("group_check_voyage", "Voyages & Sorties 🚗", "#89B5D8", now - 15 * day, now - 15 * day, "CHECKLISTS"),
                CalculationGroupEntity("group_check_maison", "Maison & École 📚", "#F7BDAB", now - 14 * day, now - 14 * day, "CHECKLISTS"),
                // Contacts Groups
                CalculationGroupEntity("group_contact_artisans", "Artisans & M3elmin 🔨", "#89B5D8", now - 15 * day, now - 15 * day, "CONTACTS"),
                CalculationGroupEntity("group_contact_famille", "Famille & Proches 👨‍👩‍👧", "#F7BDAB", now - 15 * day, now - 15 * day, "CONTACTS"),
                CalculationGroupEntity("group_contact_sante", "Santé & Médecins 🩺", "#9BD7D5", now - 14 * day, now - 14 * day, "CONTACTS"),
                CalculationGroupEntity("group_contact_commerce", "Fournisseurs & Sel3a 📦", "#F4D66D", now - 12 * day, now - 12 * day, "CONTACTS")
            )
        }

        for (g in groups) {
            groupDao.insertGroup(g)
        }

        // 3. Insert Checklists & Checklist Items
        data class SeedChecklistItem(val text: String, val isChecked: Boolean)
        data class SeedChecklist(val title: String, val groupId: String?, val offsetMs: Long, val items: List<SeedChecklistItem>)

        val seedChecklists = if (isArabic) {
            listOf(
                SeedChecklist(
                    title = "تقدية الشهر من مرجان",
                    groupId = "group_check_courses",
                    offsetMs = 30 * minute,
                    items = listOf(
                        SeedChecklistItem("زيت المائدة لوسيور 5 لتر", true),
                        SeedChecklistItem("سكر قالب وأتاي سلطان شعرة", true),
                        SeedChecklistItem("فرماج أحمر 250 غرام + كيري للأولاد", true),
                        SeedChecklistItem("باك حليب سنطرال يو إتش تي ويوغورت", true),
                        SeedChecklistItem("مسحوق تصبين أريال 5 كلغ للمكينة", false),
                        SeedChecklistItem("سائل غسيل الأواني أوني بالحامض", false),
                        SeedChecklistItem("حفاظات ومناديل مبللة بامبرز", false),
                        SeedChecklistItem("قهوة سريعة الذوبان وشكلاط غبرة", false)
                    )
                ),
                SeedChecklist(
                    title = "أشغال ومهام هاد السيمانة",
                    groupId = "group_check_maison",
                    offsetMs = 5 * hour,
                    items = listOf(
                        SeedChecklistItem("خلاص واجب السنديك الشهري عند السي مصطفى", true),
                        SeedChecklistItem("موعد في البنك لتحويل مستحقات المورد", true),
                        SeedChecklistItem("أخذ فاكتور السلعة من عند مول الكارطون", false),
                        SeedChecklistItem("مراقبة فينيسيون الصباغة مع المعلم رشيد", false),
                        SeedChecklistItem("مازوط كامل للطوموبيل وتفقد النيفو د الزيت", false)
                    )
                ),
                SeedChecklist(
                    title = "لوازم السفر ويكاند لمراكش",
                    groupId = "group_check_voyage",
                    offsetMs = 35 * hour,
                    items = listOf(
                        SeedChecklistItem("وراق الطوموبيل (لافيزيت، لاسورونس، لافينيت)", true),
                        SeedChecklistItem("شارجورات التيليفونات وبنك الطاقة (Powerbank)", true),
                        SeedChecklistItem("صيدلية الإسعافات الأولية (دوليبران، واقي الشمس)", true),
                        SeedChecklistItem("فاليزات حوايج الصيف ونظارات شمسية", true),
                        SeedChecklistItem("مراقبة ضغط العجلات الأربع وعجلة السكور", false),
                        SeedChecklistItem("صرف نقدي (كاش) لبيراج طريق السيار", false),
                        SeedChecklistItem("سوارت الشقة وتأكيد حجز الرياض", false)
                    )
                ),
                SeedChecklist(
                    title = "أدوات ومستلزمات الدخول المدرسي",
                    groupId = "group_check_maison",
                    offsetMs = 80 * hour,
                    items = listOf(
                        SeedChecklistItem("كتب ومقررات الابتدائي والإعدادي", true),
                        SeedChecklistItem("باك دفاتر 100 و200 صفحة غلاف كبيير", true),
                        SeedChecklistItem("جوج محافظ صحية للظهر (عالية الجودة)", true),
                        SeedChecklistItem("مقلمات كاملة (ستيلويات، قلم رصاص، ممحاة)", false),
                        SeedChecklistItem("علب الأقلام الملونة والصباغة المائية", false),
                        SeedChecklistItem("وزرات بيضاء للمدرسة وألبسة الرياضة", false)
                    )
                )
            )
        } else {
            listOf(
                SeedChecklist(
                    title = "Taqdiya d Chhar (Marjane)",
                    groupId = "group_check_courses",
                    offsetMs = 30 * minute,
                    items = listOf(
                        SeedChecklistItem("Huile de table 5L Lesieur", true),
                        SeedChecklistItem("Sucre en morceaux & Thé Sultan Chaïb", true),
                        SeedChecklistItem("Fromage rouge 250g + Kiri pour enfants", true),
                        SeedChecklistItem("Pack Lait Centrale UHT & Yaourts", true),
                        SeedChecklistItem("Lessive machine Ariel 5kg", false),
                        SeedChecklistItem("Liquide vaisselle Oni Citron", false),
                        SeedChecklistItem("Couches bébé & lingettes douces", false),
                        SeedChecklistItem("Café soluble & Chocolat en poudre", false)
                    )
                ),
                SeedChecklist(
                    title = "Tâches & Travaux de la Semaine",
                    groupId = "group_check_maison",
                    offsetMs = 5 * hour,
                    items = listOf(
                        SeedChecklistItem("Payer la cotisation Sandik chez Si Mohamed", true),
                        SeedChecklistItem("Rendez-vous banque pour virement fournisseur", true),
                        SeedChecklistItem("Récupérer facture fournisseur tissus au garage", false),
                        SeedChecklistItem("Valider fin de pose peinture avec M3ellem Rachid", false),
                        SeedChecklistItem("Faire le plein de gasoil pour la voiture", false)
                    )
                ),
                SeedChecklist(
                    title = "Voyage Famille Weekend Marrakech",
                    groupId = "group_check_voyage",
                    offsetMs = 35 * hour,
                    items = listOf(
                        SeedChecklistItem("Papiers de la voiture (Assurance, visite, vignette)", true),
                        SeedChecklistItem("Chargeurs téléphones & Powerbank", true),
                        SeedChecklistItem("Trousse pharmacie premiers soins (Doliprane, écran total)", true),
                        SeedChecklistItem("Valises vêtements d'été & lunettes", true),
                        SeedChecklistItem("Vérifier pression des 4 pneus + roue de secours", false),
                        SeedChecklistItem("Espèces pour le péage autoroute Casa-Marrakech", false),
                        SeedChecklistItem("Clés appartement / Réservation Riad", false)
                    )
                ),
                SeedChecklist(
                    title = "Fournitures Rentrée Scolaire",
                    groupId = "group_check_maison",
                    offsetMs = 80 * hour,
                    items = listOf(
                        SeedChecklistItem("Manuels scolaires Primaire & Collège", true),
                        SeedChecklistItem("Lot de 12 cahiers 100p & 200p grands carreaux", true),
                        SeedChecklistItem("2 Cartables ergonomiques haute qualité", true),
                        SeedChecklistItem("Trousses complètes (stylos à bille, crayons, gomme)", false),
                        SeedChecklistItem("Boîtes de feutres de couleur & règle", false),
                        SeedChecklistItem("Tabliers blancs et tenues d'EPS", false)
                    )
                )
            )
        }

        for (sc in seedChecklists) {
            val checklistId = UUID.randomUUID().toString()
            val time = now - sc.offsetMs
            val checklistEntity = ChecklistEntity(
                id = checklistId,
                title = sc.title,
                createdAtEpochMs = time,
                updatedAtEpochMs = time,
                groupId = sc.groupId
            )
            val itemEntities = sc.items.mapIndexed { idx, item ->
                ChecklistItemEntity(
                    id = UUID.randomUUID().toString(),
                    checklistId = checklistId,
                    text = item.text,
                    isChecked = item.isChecked,
                    position = idx,
                    createdAtEpochMs = time + idx * 1000L
                )
            }
            checklistDao.insertChecklistWithItems(checklistEntity, itemEntities)
        }

        // 4. Insert Detailed Notes
        data class SeedNote(
            val title: String,
            val content: String,
            val colorTag: String,
            val isPinned: Boolean,
            val groupId: String?,
            val offsetMs: Long
        )

        val seedNotes = if (isArabic) {
            listOf(
                SeedNote(
                    title = "فكرة مشروع: بيع منتجات الصناعة التقليدية والزيوت أونلاين",
                    content = "💡 دراسة أولية لإطلاق متجر إلكتروني للمنتجات المغربية الأصيلة:\n\n" +
                            "1. المنتجات المعتمدة:\n" +
                            "• زيت الأركان التجميلي والغذائي (تعاونيات تارودانت الحاصلة على شواهد الجودة).\n" +
                            "• ماء الورد المقطر الأصلي من قلعة مكونة.\n" +
                            "• فخار وصناعة جلدية عصرية ومودرن (فاس ومراكش).\n\n" +
                            "2. التغليف والتقديم (Packaging):\n" +
                            "• علب كرتونية فاخرة مذهبة بشعار مغربي راقٍ.\n" +
                            "• بطاقة شكر بالخط المغربي ولمسة تقليدية.\n\n" +
                            "3. التوزيع واللوجستيك:\n" +
                            "• عقد شحن وتوصيل سريع مع أمانة إكسبريس (Amana) و Cathedis مع خدمة الدفع عند الاستلام (COD).\n" +
                            "• مدة التوصيل المستهدفة: 24 ساعة في كازا والرباط، 48 ساعة لباقي المدن.\n\n" +
                            "4. الأهداف المالية:\n" +
                            "• هامش ربح صافي لا يقل عن 35%.\n" +
                            "• تحقيق معدل 15 طلباً يومياً في الأشهر الثلاثة الأولى.",
                    colorTag = "YELLOW",
                    isPinned = true,
                    groupId = "group_notes_projets",
                    offsetMs = 2 * hour
                ),
                SeedNote(
                    title = "أرقام وعناوين الحرفيين الموثوقين (Casablanca & Rabat)",
                    content = "📞 دليل الحرفيين المعتمدين للدار والأشغال العاجلة:\n\n" +
                            "• السي محمد (بلومبي وشوفوا - المعاريف):\n" +
                            "  06 61 24 88 XX - خدمة نقية، متوفر للطوارئ.\n\n" +
                            "• السي رشيد (كهربائي معتمد وتوزيع اللوحات - درب غلف):\n" +
                            "  06 63 41 99 XX - متخصص ليد، بريزات، وطابلوات ديجونكتور.\n\n" +
                            "• المعلم حسن (صباغ وفينيسيون إندوي - بوركون):\n" +
                            "  06 70 15 33 XX - دقيق في المواعيد، نقاء تام في الخدمة.\n\n" +
                            "• السي العربي (ميكانيك ودييزل - روش نوار):\n" +
                            "  06 62 77 44 XX - كراج معتمد للفيدونج والفانات والفران.\n\n" +
                            "• ديباناج وقطر السيارات 24/24 (أوتوروت والمدينة):\n" +
                            "  05 22 99 11 XX.",
                    colorTag = "BLUE",
                    isPinned = true,
                    groupId = "group_notes_contacts",
                    offsetMs = 30 * hour
                ),
                SeedNote(
                    title = "قواعد ذهبية لتدبير ميزانية الأسرة (قاعدة 50/30/20)",
                    content = "💰 خطة توزيع الصرف الشهري للحفاظ على التوازن المالي:\n\n" +
                            "1. 50% للمصاريف الثابتة والضرورية:\n" +
                            "• الكراء / قسط السكن.\n" +
                            "• فواتير الماء والكهرباء والويفي والسنديك.\n" +
                            "• تقدية الشهر التموينية وسوق الخضار الأسبوعي.\n" +
                            "• مصاريف دراسة وتمدرس الأولاد.\n\n" +
                            "2. 30% للمصاريف المتغيرة والترفيه:\n" +
                            "• الخرجات العائلية والمطاعم والويكاند.\n" +
                            "• شراء الملابس ومصاريف شخصية.\n\n" +
                            "3. 20% للادخار والاستثمار التلقائي:\n" +
                            "• صندوق طوارئ يغطي 3 أشهر على الأقل.\n" +
                            "• شراء قطع ذهب أو ادخار تدريجي في مشاريع مدرة للدخل.",
                    colorTag = "GREEN",
                    isPinned = false,
                    groupId = "group_notes_budget",
                    offsetMs = 55 * hour
                ),
                SeedNote(
                    title = "ترتيبات ومصاريف عيد الأضحى المبارك",
                    content = "🐑 لائحة الاحتياجات السنوية للعيد الكبير:\n\n" +
                            "• حولي العيد: التوجه إلى ضيعة بنسليمان أو البروج (سلالة الصردي أو البركي المليح).\n" +
                            "• لوازم العيد: فاخر زوين نوعية ممتازة، شوايات إينوكس، قطبان جديدة، مجمر فخار، مبرد وماكينة شحذ السكاكين.\n" +
                            "• التوابل والبهارات: قزبر حبوب مطحون، راس الحانوت، سكينجبير بلدي، كامون بلدي جديد، برقوق ولوز وزيت العود.\n" +
                            "• بركة الوالدين والأحباب: هدايا وصلة الرحم.",
                    colorTag = "PINK",
                    isPinned = false,
                    groupId = "group_notes_budget",
                    offsetMs = 160 * hour
                )
            )
        } else {
            listOf(
                SeedNote(
                    title = "Idée Projet: Vente Artisanat & Huiles Bio en Ligne",
                    content = "💡 Étude préliminaire de lancement boutique e-commerce produits marocains:\n\n" +
                            "1. Produits phares:\n" +
                            "• Huile d'Argan cosmétique & alimentaire certifiée (Coopératives de Taroudant).\n" +
                            "• Eau de rose distillée pure (Kelaat M'Gouna).\n" +
                            "• Céramique et maroquinerie moderne épurée (Fès & Marrakech).\n\n" +
                            "2. Packaging & Présentation:\n" +
                            "• Coffrets cartonnés haut de gamme avec dorure sobre et ruban artisanal.\n" +
                            "• Carte de remerciement personnalisée avec calligraphie marocaine.\n\n" +
                            "3. Logistique & Distribution:\n" +
                            "• Convention de livraison rapide avec Amana Express ou Cathedis avec paiement à la livraison (Cash on Delivery).\n" +
                            "• Délais cibles: 24h Casablanca / Rabat, 48h reste du Royaume.\n\n" +
                            "4. Objectifs Financiers:\n" +
                            "• Marge brute ciblée: 35% minimum.\n" +
                            "• Objectif: 15 commandes par jour dès le premier trimestre.",
                    colorTag = "YELLOW",
                    isPinned = true,
                    groupId = "group_notes_projets",
                    offsetMs = 2 * hour
                ),
                SeedNote(
                    title = "Contacts Artisans & M3elmin de Confiance (Casa / Rabat)",
                    content = "📞 Carnet d'adresses artisans testés et fiables:\n\n" +
                            "• Si Mohamed (Plombier & Chauffe-eau, Maarif):\n" +
                            "  06 61 24 88 XX - Réactif, travail impeccable pour urgences.\n\n" +
                            "• Si Rachid (Électricien qualifié & Tableaux, Derb Ghallef):\n" +
                            "  06 63 41 99 XX - Spécialiste LED, prises et disjoncteurs.\n\n" +
                            "• المعلم حسن (M3ellem Peintre & Finitions, Bourgogne):\n" +
                            "  06 70 15 33 XX - Ponctuel, finitions propres et soignées.\n\n" +
                            "• Si Larbi (Mécanicien Dieseliste, Roches Noires):\n" +
                            "  06 62 77 44 XX - Garage de confiance pour vidange et freins.\n\n" +
                            "• Dépannage & Remorquage 24/24 (Autoroute / Ville):\n" +
                            "  05 22 99 11 XX.",
                    colorTag = "BLUE",
                    isPinned = true,
                    groupId = "group_notes_contacts",
                    offsetMs = 30 * hour
                ),
                SeedNote(
                    title = "Règles d'or: Gestion du Budget Familial (50/30/20)",
                    content = "💰 Stratégie d'équilibre financier et de gestion des dépenses:\n\n" +
                            "1. 50% Charges fixes & Nécessités:\n" +
                            "• Loyer / Traite du logement.\n" +
                            "• Factures d'eau, électricité, fibre internet et syndic.\n" +
                            "• Taqdiya mensuelle du supermarché et marché de légumes.\n" +
                            "• Scolarité et fournitures des enfants.\n\n" +
                            "2. 30% Dépenses flexibles & Plaisirs:\n" +
                            "• Sorties en famille, restaurants et sorties weekend.\n" +
                            "• Shopping vêtements et imprévus quotidiens.\n\n" +
                            "3. 20% Épargne automatique & Investissement:\n" +
                            "• Fonds d'urgence disponible couvrant au moins 3 mois de charges.\n" +
                            "• Achats réguliers de pièces d'or ou épargne rémunérée.",
                    colorTag = "GREEN",
                    isPinned = false,
                    groupId = "group_notes_budget",
                    offsetMs = 55 * hour
                ),
                SeedNote(
                    title = "Préparatifs & Organisation Aïd Al Adha",
                    content = "🐑 Liste des préparatifs pour l'Aïd Al Adha:\n\n" +
                            "• Mouton: Visite à la ferme de Benslimane ou El Brouj (Race Sardi ou Berki de qualité).\n" +
                            "• Accessoires: Charbon de bois de qualité (Fham zwin), grilles inox, brochettes neuves, brasero traditionnel (Mejmer), aiguisage des couteaux.\n" +
                            "• Épices & Ingrédients: Mrouzia fraîche, coriandre moulue, cumin beldi, amandes, pruneaux et huile d'olive vierge.\n" +
                            "• Solidarité & Famille: Enveloppes cadeaux et visites familiales.",
                    colorTag = "PINK",
                    isPinned = false,
                    groupId = "group_notes_budget",
                    offsetMs = 160 * hour
                )
            )
        }

        for (sn in seedNotes) {
            val time = now - sn.offsetMs
            val noteEntity = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = sn.title,
                content = sn.content,
                colorTag = sn.colorTag,
                isPinned = sn.isPinned,
                createdAtEpochMs = time,
                updatedAtEpochMs = time,
                groupId = sn.groupId
            )
            noteDao.insertNote(noteEntity)
        }

        // 5. Insert Active Reminders (Carousel & Deadlines)
        val seedReminders = if (isArabic) {
            listOf(
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "فاتورة الماء والكهرباء (ليديك / ريضال)",
                    description = "485 درهم - الأداء عبر التطبيق البنكي قبل يوم 28",
                    targetEpochMs = now + 2 * day,
                    recurrenceType = ReminderRecurrence.MONTHLY.name,
                    timeHour = 10,
                    timeMinute = 0,
                    colorTag = "BLUE",
                    createdAtEpochMs = now - 2 * day,
                    updatedAtEpochMs = now - 2 * day
                ),
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "موعد طبيب الأسنان للأولاد",
                    description = "عيادة الدكتور بنسودة - الفحص الدوري والتنظيف",
                    targetEpochMs = now + 3 * day,
                    recurrenceType = ReminderRecurrence.ONCE.name,
                    timeHour = 11,
                    timeMinute = 0,
                    colorTag = "GREEN",
                    createdAtEpochMs = now - 1 * day,
                    updatedAtEpochMs = now - 1 * day
                ),
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "واجب السنديك الشهري",
                    description = "150 درهم - التسليم يداً بيد للسي مصطفى (سنديك العمارة)",
                    targetEpochMs = now + 4 * day,
                    recurrenceType = ReminderRecurrence.MONTHLY.name,
                    timeHour = 18,
                    timeMinute = 30,
                    colorTag = "YELLOW",
                    createdAtEpochMs = now - 3 * day,
                    updatedAtEpochMs = now - 3 * day
                ),
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "موعد فيدونج وصيانة السيارة",
                    description = "تغيير زيت توتال 5W30 + فلتر الزيت والهواء عند السي العربي",
                    targetEpochMs = now + 8 * day,
                    recurrenceType = ReminderRecurrence.EVERY_6_MONTHS.name,
                    timeHour = 9,
                    timeMinute = 30,
                    colorTag = "RED",
                    createdAtEpochMs = now - 5 * day,
                    updatedAtEpochMs = now - 5 * day
                )
            )
        } else {
            listOf(
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Facture Lydec (Eau & Électricité)",
                    description = "485 DH - Règlement via application bancaire avant le 28",
                    targetEpochMs = now + 2 * day,
                    recurrenceType = ReminderRecurrence.MONTHLY.name,
                    timeHour = 10,
                    timeMinute = 0,
                    colorTag = "BLUE",
                    createdAtEpochMs = now - 2 * day,
                    updatedAtEpochMs = now - 2 * day
                ),
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Rendez-vous Dentiste Enfants",
                    description = "Cabinet Dr. Bensouda - Contrôle semestriel et détartrage",
                    targetEpochMs = now + 3 * day,
                    recurrenceType = ReminderRecurrence.ONCE.name,
                    timeHour = 11,
                    timeMinute = 0,
                    colorTag = "GREEN",
                    createdAtEpochMs = now - 1 * day,
                    updatedAtEpochMs = now - 1 * day
                ),
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Cotisation Mensuelle Sandik",
                    description = "150 DH - Remise en mains propres chez Si Mustapha (Syndic)",
                    targetEpochMs = now + 4 * day,
                    recurrenceType = ReminderRecurrence.MONTHLY.name,
                    timeHour = 18,
                    timeMinute = 30,
                    colorTag = "YELLOW",
                    createdAtEpochMs = now - 3 * day,
                    updatedAtEpochMs = now - 3 * day
                ),
                ReminderEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Vidange Voiture (10 000 km)",
                    description = "Changement huile Total 5W30 + filtres chez Si Larbi",
                    targetEpochMs = now + 8 * day,
                    recurrenceType = ReminderRecurrence.EVERY_6_MONTHS.name,
                    timeHour = 9,
                    timeMinute = 30,
                    colorTag = "RED",
                    createdAtEpochMs = now - 5 * day,
                    updatedAtEpochMs = now - 5 * day
                )
            )
        }

        for (rem in seedReminders) {
            reminderDao.insert(rem)
        }

        // 5. Insert Moroccan Contacts (Artisans, Famille, Santé, Fournisseurs)
        val seedContacts = if (isArabic) {
            listOf(
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "سي حسن بلومبي",
                    phoneNumber = "0661234567",
                    secondaryPhone = "0522123456",
                    note = "معلم طيارة في الفويت ورشاشات الدوش وسخان الماء",
                    groupId = "group_contact_artisans",
                    colorTag = "BLUE",
                    isPinned = true,
                    createdAtEpochMs = now - 10 * day,
                    updatedAtEpochMs = now - 2 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "خالتي فاطمة الزهراء 🌸",
                    phoneNumber = "0672445566",
                    secondaryPhone = null,
                    note = "عائلة - الدار البيضاء حي الولفة",
                    groupId = "group_contact_famille",
                    colorTag = "PINK",
                    isPinned = true,
                    createdAtEpochMs = now - 12 * day,
                    updatedAtEpochMs = now - 3 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "معلم رشيد إلكتريسيان",
                    phoneNumber = "0670987654",
                    secondaryPhone = null,
                    note = "إصلاح ديجونكتور وتريسيستي الدار والسبوتات",
                    groupId = "group_contact_artisans",
                    colorTag = "YELLOW",
                    isPinned = false,
                    createdAtEpochMs = now - 9 * day,
                    updatedAtEpochMs = now - 4 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "د. أمينة الفاسي",
                    phoneNumber = "0522334455",
                    secondaryPhone = "0660112233",
                    note = "عيادة الأسنان - المواعيد بالواتساب",
                    groupId = "group_contact_sante",
                    colorTag = "GREEN",
                    isPinned = false,
                    createdAtEpochMs = now - 14 * day,
                    updatedAtEpochMs = now - 5 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "كاراج المعلم باسو",
                    phoneNumber = "0663112233",
                    secondaryPhone = null,
                    note = "فيدونج وسكانير وفرانات لجميع أنواع السيارات",
                    groupId = "group_contact_artisans",
                    colorTag = "BLUE",
                    isPinned = false,
                    createdAtEpochMs = now - 8 * day,
                    updatedAtEpochMs = now - 1 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "المعلم عزيز الصباغ",
                    phoneNumber = "0654889900",
                    secondaryPhone = null,
                    note = "صباغة متقونة ونقية (خيال، صابلي وسبيطولار)",
                    groupId = "group_contact_artisans",
                    colorTag = "PURPLE",
                    isPinned = false,
                    createdAtEpochMs = now - 7 * day,
                    updatedAtEpochMs = now - 2 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "عمر مول الهري",
                    phoneNumber = "0661998877",
                    secondaryPhone = null,
                    note = "سلعة الجملة ونصف الجملة، التوصيل للمحل",
                    groupId = "group_contact_commerce",
                    colorTag = "YELLOW",
                    isPinned = false,
                    createdAtEpochMs = now - 11 * day,
                    updatedAtEpochMs = now - 3 * day
                )
            )
        } else {
            listOf(
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Si Hassan Plombier",
                    phoneNumber = "0661234567",
                    secondaryPhone = "0522123456",
                    note = "Dépannage fuites d'eau & chauffe-eau, rapide",
                    groupId = "group_contact_artisans",
                    colorTag = "BLUE",
                    isPinned = true,
                    createdAtEpochMs = now - 10 * day,
                    updatedAtEpochMs = now - 2 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Khalti Fatima-Zahra 🌸",
                    phoneNumber = "0672445566",
                    secondaryPhone = null,
                    note = "Famille - Casablanca Oulfa",
                    groupId = "group_contact_famille",
                    colorTag = "PINK",
                    isPinned = true,
                    createdAtEpochMs = now - 12 * day,
                    updatedAtEpochMs = now - 3 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Rachid Électricien",
                    phoneNumber = "0670987654",
                    secondaryPhone = null,
                    note = "Installation disjoncteur, prises & spots LED",
                    groupId = "group_contact_artisans",
                    colorTag = "YELLOW",
                    isPinned = false,
                    createdAtEpochMs = now - 9 * day,
                    updatedAtEpochMs = now - 4 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Dr. Amina El Fassi",
                    phoneNumber = "0522334455",
                    secondaryPhone = "0660112233",
                    note = "Cabinet dentaire - Rdv par WhatsApp",
                    groupId = "group_contact_sante",
                    colorTag = "GREEN",
                    isPinned = false,
                    createdAtEpochMs = now - 14 * day,
                    updatedAtEpochMs = now - 5 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Garage Si Bassou",
                    phoneNumber = "0663112233",
                    secondaryPhone = null,
                    note = "Vidange, plaquettes de frein & diagnostic valise",
                    groupId = "group_contact_artisans",
                    colorTag = "BLUE",
                    isPinned = false,
                    createdAtEpochMs = now - 8 * day,
                    updatedAtEpochMs = now - 1 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Aziz Peintre",
                    phoneNumber = "0654889900",
                    secondaryPhone = null,
                    note = "Peinture d'intérieur propre, salon marocain",
                    groupId = "group_contact_artisans",
                    colorTag = "PURPLE",
                    isPinned = false,
                    createdAtEpochMs = now - 7 * day,
                    updatedAtEpochMs = now - 2 * day
                ),
                ContactEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Omar Moul Lheri",
                    phoneNumber = "0661998877",
                    secondaryPhone = null,
                    note = "Denrées de base en gros, livraison disponible",
                    groupId = "group_contact_commerce",
                    colorTag = "YELLOW",
                    isPinned = false,
                    createdAtEpochMs = now - 11 * day,
                    updatedAtEpochMs = now - 3 * day
                )
            )
        }

        for (c in seedContacts) {
            contactDao.insertContact(c)
        }

        // Helper data class for building seeds
        data class SeedItem(val label: String, val amountCentimes: Long, val expression: String? = null)
        data class SeedCalc(
            val title: String,
            val groupId: String?,
            val currency: String,
            val offsetMs: Long,
            val note: String? = null,
            val paymentStatus: String = "PAID",
            val calcType: String = if (paymentStatus == "UNPAID") "CREDIT" else "PERSONNEL",
            val dueDateOffsetMs: Long? = null,
            val items: List<SeedItem>
        )

        val seedCalculations = if (isArabic) {
            listOf(
                // --- TODAY ---
                SeedCalc(
                    title = "تقدية سوق الأربعاء",
                    groupId = "group_marche",
                    currency = "DIRHAM",
                    offsetMs = 1 * hour,
                    note = "تقدية ديال السيمانة خضرة وفواكه طرية",
                    items = listOf(
                        SeedItem("لحم البقري هبرة (2 كلغ)", 19000L, "190"),
                        SeedItem("دجاج رومي مغسول ومنقي (3 كلغ)", 6500L, "65"),
                        SeedItem("بطاطا ومطيشة وبصلة", 4500L, "45"),
                        SeedItem("ديسير (بنان وتفاح)", 3500L, "35"),
                        SeedItem("زيتون مشرمل وحامض مصير", 1500L, "15"),
                        SeedItem("قزبر ومعدنوس ونعناع وشيبة", 800L, "8")
                    )
                ),
                SeedCalc(
                    title = "فطور وقهوة مع الكليان",
                    groupId = "group_loisirs",
                    currency = "DIRHAM",
                    offsetMs = 3 * hour,
                    note = "لقاء عمل لمناقشة مشروع المحل مع السي محمد",
                    items = listOf(
                        SeedItem("2 قهوة كريم", 3600L, "36"),
                        SeedItem("2 عصير برتقال معصور", 4000L, "40"),
                        SeedItem("أومليط بالخليع الفاسي", 4500L, "45"),
                        SeedItem("قرعة ماء سيدي علي كبرى", 1200L, "12")
                    )
                ),
                SeedCalc(
                    title = "مصاريف الحانوت د الحومة",
                    groupId = "group_dar",
                    currency = "DIRHAM",
                    offsetMs = 6 * hour,
                    items = listOf(
                        SeedItem("2 لتر حليب سنطرال معقم", 1600L, "16"),
                        SeedItem("بيدوزيت لوسيور 5 لتر", 8800L, "88"),
                        SeedItem("فرماج أحمر ممتاز 250 غرام", 3200L, "32"),
                        SeedItem("خبز الدار (4 خبزات)", 800L, "8"),
                        SeedItem("باك دانون فاني", 1400L, "14")
                    )
                ),

                // --- YESTERDAY ---
                SeedCalc(
                    title = "مازوط وغسيل الطوموبيل",
                    groupId = "group_voiture",
                    currency = "DIRHAM",
                    offsetMs = 25 * hour,
                    items = listOf(
                        SeedItem("مازوط كامل (شيل في-باور)", 52000L, "520"),
                        SeedItem("غسيل شامل داخلي وخارجي", 5000L, "50"),
                        SeedItem("سائل تنظيف الزجاج", 2500L, "25"),
                        SeedItem("قهوة إكسبريس بالمحطة", 1500L, "15")
                    )
                ),
                SeedCalc(
                    title = "فواتير الشهر (الماء، الضو، الويفي)",
                    groupId = "group_dar",
                    currency = "DIRHAM",
                    offsetMs = 28 * hour,
                    note = "تم الأداء عبر التطبيق البنكي",
                    items = listOf(
                        SeedItem("فاتورة ليديك (الماء والضو)", 48500L, "485"),
                        SeedItem("اشتراك اتصالات المغرب فايبر 100 ميغا", 24900L, "249"),
                        SeedItem("واجب السنديك الشهري", 15000L, "150"),
                        SeedItem("تعبئة إنوي 4G", 5000L, "50")
                    )
                ),
                SeedCalc(
                    title = "صيدلية الحراسة",
                    groupId = "group_dar",
                    currency = "DIRHAM",
                    offsetMs = 33 * hour,
                    items = listOf(
                        SeedItem("دوليبران 1000 ملغ (2 علب)", 3400L, "34"),
                        SeedItem("سيرو السعال هوميكس", 4800L, "48"),
                        SeedItem("فيتامين C فوار", 5500L, "55"),
                        SeedItem("سيروم فيزيولوجي وقطن طبي", 2200L, "22")
                    )
                ),

                // --- 2 DAYS AGO ---
                SeedCalc(
                    title = "سلعة الصباغة - ورشة الإصلاحات",
                    groupId = "group_chantier",
                    currency = "DIRHAM",
                    offsetMs = 48 * hour,
                    paymentStatus = "UNPAID",
                    dueDateOffsetMs = 5 * day,
                    items = listOf(
                        SeedItem("2 صرادل صباغة فينيل أسترال أبيض", 76000L, "760"),
                        SeedItem("سطل كولا سيكا للاسمنت", 18000L, "180"),
                        SeedItem("إندوي ليسينغ (2 أكياس)", 14000L, "140"),
                        SeedItem("3 رولويات صباغة ضد التنقيط", 9500L, "95"),
                        SeedItem("كاغيط الحرش وسكوتش المسكاج", 6500L, "65")
                    )
                ),
                SeedCalc(
                    title = "غداء فريق العمال بالورشة",
                    groupId = "group_loisirs",
                    currency = "DIRHAM",
                    offsetMs = 53 * hour,
                    items = listOf(
                        SeedItem("شواء كفتة وقطبان (1 كلغ)", 18000L, "180"),
                        SeedItem("سلاطات مغربية وتكتوكة", 3500L, "35"),
                        SeedItem("مشروبات أولماس وكوكا", 3000L, "30"),
                        SeedItem("دلاح وبطيخ طري", 2500L, "25")
                    )
                ),

                // --- 3 DAYS AGO ---
                SeedCalc(
                    title = "رواتب أسبوع الخدامة",
                    groupId = "group_khdama",
                    currency = "DIRHAM",
                    offsetMs = 74 * hour,
                    note = "أجور عمال الصباغة والجبص",
                    paymentStatus = "UNPAID",
                    dueDateOffsetMs = 6 * day,
                    items = listOf(
                        SeedItem("رشيد (معلم جباص وصباغ)", 180000L, "1800"),
                        SeedItem("حسن (عامل مؤهل)", 130000L, "1300"),
                        SeedItem("مصطفى (مساعد وعامل يومي)", 100000L, "1000"),
                        SeedItem("نقل وتوصيل الفريق", 20000L, "200")
                    )
                ),
                SeedCalc(
                    title = "مورد الكرتون والتغليف للمحل",
                    groupId = "group_commerce",
                    currency = "RIAL",
                    offsetMs = 78 * hour,
                    note = "فاتورة التغليف رقم 842",
                    items = listOf(
                        SeedItem("500 كرتونة حجم متوسط", 125000L, "25000"),
                        SeedItem("20 رولو لصاق بني عريض", 45000L, "9000"),
                        SeedItem("بابيي بول 100 متر للتغليف", 60000L, "12000"),
                        SeedItem("مصاريف شحن وتوصيل الشاحنة", 30000L, "6000")
                    )
                ),

                // --- LAST WEEK ---
                SeedCalc(
                    title = "فيدونج وبلاكيط فران السيارة",
                    groupId = "group_voiture",
                    currency = "DIRHAM",
                    offsetMs = 120 * hour,
                    items = listOf(
                        SeedItem("زيت محرك توتال 5W30 (5 لتر)", 46000L, "460"),
                        SeedItem("فلتر الزيت وفلتر الهواء", 16000L, "160"),
                        SeedItem("فلتر المازوط الأصلي", 14000L, "140"),
                        SeedItem("مجموعة بلاكيط فران بوش", 34000L, "340"),
                        SeedItem("يد عاملة كراج السي العربي", 15000L, "150")
                    )
                ),
                SeedCalc(
                    title = "أدوات ومقررات الدخول المدرسي",
                    groupId = "group_dar",
                    currency = "DIRHAM",
                    offsetMs = 145 * hour,
                    note = "مكتبة الوحدة - التحضير للموسم الدراسي",
                    items = listOf(
                        SeedItem("مقررات وكتب الابتدائي والإعدادي", 68000L, "680"),
                        SeedItem("دفاتر 200 و100 صفحة (مجموعة 12)", 12000L, "120"),
                        SeedItem("2 محافظ ظهر مدرسية مريحة", 35000L, "350"),
                        SeedItem("مقلمات وأقلام حبر ومساطر", 11000L, "110")
                    )
                ),
                SeedCalc(
                    title = "كهرباء وإضاءة المحل التجاري",
                    groupId = "group_chantier",
                    currency = "DIRHAM",
                    offsetMs = 170 * hour,
                    items = listOf(
                        SeedItem("3 رولويات خيط كهرباء 2.5 مم", 45000L, "450"),
                        SeedItem("سبوطات ليد إنكاستري (باك د 8)", 32000L, "320"),
                        SeedItem("ديجونكتور ديفيرونسييل لوغراند", 18000L, "180"),
                        SeedItem("سوارات وبريزات سيمون", 14000L, "140"),
                        SeedItem("قنوات بلاستيكية ومسامير التثبيت", 7500L, "75")
                    )
                ),
                SeedCalc(
                    title = "خرجة عائلية ويكاند لشاطئ بوزنيقة",
                    groupId = "group_loisirs",
                    currency = "DIRHAM",
                    offsetMs = 195 * hour,
                    items = listOf(
                        SeedItem("مظلة و4 كراسي استراحة للبحر", 8000L, "80"),
                        SeedItem("غداء أسماك طرية وفواكه البحر", 38000L, "380"),
                        SeedItem("مثلجات وعصائر للأطفال", 7500L, "75"),
                        SeedItem("طريق السيار وموقف السيارات المحروس", 4500L, "45")
                    )
                ),
                SeedCalc(
                    title = "جزارة - لحم هبرة وكفتة",
                    groupId = "group_marche",
                    currency = "DIRHAM",
                    offsetMs = 220 * hour,
                    items = listOf(
                        SeedItem("3 كلغ لحم عجل هبرة ممتاز", 33000L, "330"),
                        SeedItem("1.5 كلغ كفتة طازجة متبلة", 16500L, "165"),
                        SeedItem("سجق بقري طازج", 8500L, "85")
                    )
                ),
                SeedCalc(
                    title = "سلعة مورد أقمشة الخياطة",
                    groupId = "group_commerce",
                    currency = "RIAL",
                    offsetMs = 300 * hour,
                    note = "سلعة الخياطة التقليدية والعصرية",
                    paymentStatus = "UNPAID",
                    dueDateOffsetMs = 7 * day,
                    items = listOf(
                        SeedItem("ثوب مليفة ممتازة نمرة 1 (15 متر)", 210000L, "42000"),
                        SeedItem("ثوب حرير أصلي للتبطين (20 متر)", 100000L, "20000"),
                        SeedItem("سفيفة وعقاد بلدية حرّة", 70000L, "14000")
                    )
                )
            )
        } else {
            listOf(
                // --- TODAY ---
                SeedCalc(
                    title = "Taqdiya d Souq Larba3",
                groupId = "group_marche",
                currency = "DIRHAM",
                offsetMs = 1 * hour,
                note = "Taqdiya dial simana m3a l-khodra triya",
                items = listOf(
                    SeedItem("Lhem lbaqri (2kg)", 19000L, "190"),
                    SeedItem("Djej romi mghssoul (3kg)", 6500L, "65"),
                    SeedItem("Batata w Maticha w Bssla", 4500L, "45"),
                    SeedItem("Disser (Banan w Tffa7)", 3500L, "35"),
                    SeedItem("Zitoun mchermel w 7amed", 1500L, "15"),
                    SeedItem("Rbi3 w na3na3 w chiba", 800L, "8")
                )
            ),
            SeedCalc(
                title = "Café & Ftour Client",
                groupId = "group_loisirs",
                currency = "DIRHAM",
                offsetMs = 3 * hour,
                note = "Rendez-vous projet avec Si Mohamed",
                items = listOf(
                    SeedItem("2 Café Crème", 3600L, "36"),
                    SeedItem("2 Jus d'Orange Pressé", 4000L, "40"),
                    SeedItem("Omelette Khlii3", 4500L, "45"),
                    SeedItem("Bouteille d'eau Sidi Ali", 1200L, "12")
                )
            ),
            SeedCalc(
                title = "Hanout d L7ouma",
                groupId = "group_dar",
                currency = "DIRHAM",
                offsetMs = 6 * hour,
                items = listOf(
                    SeedItem("2 Lait Centrale UHT", 1600L, "16"),
                    SeedItem("Bidou d Zit 5L Lesieur", 8800L, "88"),
                    SeedItem("Fromage rouge 250g", 3200L, "32"),
                    SeedItem("Khobz dar (4 khobzat)", 800L, "8"),
                    SeedItem("Pack Danone vanille", 1400L, "14")
                )
            ),

            // --- YESTERDAY ---
            SeedCalc(
                title = "Plein Mazout & Lavage",
                groupId = "group_voiture",
                currency = "DIRHAM",
                offsetMs = 25 * hour,
                items = listOf(
                    SeedItem("Plein Diesel Shell V-Power", 52000L, "520"),
                    SeedItem("Lavage complet intérieur/extérieur", 5000L, "50"),
                    SeedItem("Liquide lave-glace", 2500L, "25"),
                    SeedItem("Café express station", 1500L, "15")
                )
            ),
            SeedCalc(
                title = "Factures d Chhar",
                groupId = "group_dar",
                currency = "DIRHAM",
                offsetMs = 28 * hour,
                note = "Factures réglées via l'application bancaire",
                items = listOf(
                    SeedItem("Facture Lydec (Lma w Ddo)", 48500L, "485"),
                    SeedItem("Abonnement Maroc Telecom Fibre", 24900L, "249"),
                    SeedItem("Cotisation Sandik Immeuble", 15000L, "150"),
                    SeedItem("Recharge Inwi 4G", 5000L, "50")
                )
            ),
            SeedCalc(
                title = "Pharmacie d Garde",
                groupId = "group_dar",
                currency = "DIRHAM",
                offsetMs = 33 * hour,
                items = listOf(
                    SeedItem("Doliprane 1000mg (2 boîtes)", 3400L, "34"),
                    SeedItem("Sirop Toux Humex", 4800L, "48"),
                    SeedItem("Vitamine C Effervescente", 5500L, "55"),
                    SeedItem("Sérum physiologique & Coton", 2200L, "22")
                )
            ),

            // --- 2 DAYS AGO ---
            SeedCalc(
                title = "Sel3a d Sbagha - Chantier",
                groupId = "group_chantier",
                currency = "DIRHAM",
                offsetMs = 48 * hour,
                paymentStatus = "UNPAID",
                dueDateOffsetMs = 5 * day,
                items = listOf(
                    SeedItem("2 Sradel Vinyl Mat Blanc Astral", 76000L, "760"),
                    SeedItem("Sattel Kolat Ciment Sika", 18000L, "180"),
                    SeedItem("Enduit de lissage (2 sacs)", 14000L, "140"),
                    SeedItem("3 Rouleaux antigoutte", 9500L, "95"),
                    SeedItem("Papier verre w Scotch masquage", 6500L, "65")
                )
            ),
            SeedCalc(
                title = "Déjeuner Équipe Chantier",
                groupId = "group_loisirs",
                currency = "DIRHAM",
                offsetMs = 53 * hour,
                items = listOf(
                    SeedItem("Grillades mixtes (1kg kefta & kotlet)", 18000L, "180"),
                    SeedItem("Salades marocaines & Tektouka", 3500L, "35"),
                    SeedItem("Bouteille Oulmès & Coca", 3000L, "30"),
                    SeedItem("Pastèque & Melon", 2500L, "25")
                )
            ),

            // --- 3 DAYS AGO ---
            SeedCalc(
                title = "Salaires Semaine Khdama",
                groupId = "group_khdama",
                currency = "DIRHAM",
                offsetMs = 74 * hour,
                note = "Règlement semaine du 31 Août au 05 Septembre",
                paymentStatus = "UNPAID",
                dueDateOffsetMs = 6 * day,
                items = listOf(
                    SeedItem("Rachid (M3ellem plâtrier)", 180000L, "1800"),
                    SeedItem("Hassan (Ouvrier qualifié)", 130000L, "1300"),
                    SeedItem("Mustapha (Aide & Manœuvre)", 100000L, "1000"),
                    SeedItem("Transport aller-retour équipe", 20000L, "200")
                )
            ),
            SeedCalc(
                title = "Fournisseur Carton & Emballage",
                groupId = "group_commerce",
                currency = "RIAL",
                offsetMs = 78 * hour,
                note = "Facture N° 842 payée en espèces",
                items = listOf(
                    SeedItem("500 Cartons format moyen", 125000L, "25000"),
                    SeedItem("20 Rouleaux Adhésif Marron", 45000L, "9000"),
                    SeedItem("Papier bulle 100 mètres", 60000L, "12000"),
                    SeedItem("Frais de livraison camion", 30000L, "6000")
                )
            ),

            // --- LAST WEEK ---
            SeedCalc(
                title = "Vidange & Plaquettes Voiture",
                groupId = "group_voiture",
                currency = "DIRHAM",
                offsetMs = 120 * hour,
                items = listOf(
                    SeedItem("Huile Moteur Total 5W30 (5L)", 46000L, "460"),
                    SeedItem("Filtre à Huile & Filtre à Air", 16000L, "160"),
                    SeedItem("Filtre à Gasoil", 14000L, "140"),
                    SeedItem("Jeu de Plaquettes de frein Bosch", 34000L, "340"),
                    SeedItem("Main d'œuvre garage Si Larbi", 15000L, "150")
                )
            ),
            SeedCalc(
                title = "Fournitures Scolaires Rentrée",
                groupId = "group_dar",
                currency = "DIRHAM",
                offsetMs = 145 * hour,
                note = "Librairie Al Wahda - Préparation rentrée",
                items = listOf(
                    SeedItem("Livres scolaires Primaire & Collège", 68000L, "680"),
                    SeedItem("Cahiers 200p & 100p (lot de 12)", 12000L, "120"),
                    SeedItem("2 Cartables ergonomiques", 35000L, "350"),
                    SeedItem("Trousses, stylos, feutres & règles", 11000L, "110")
                )
            ),
            SeedCalc(
                title = "Électricité & Câblage Magasin",
                groupId = "group_chantier",
                currency = "DIRHAM",
                offsetMs = 170 * hour,
                items = listOf(
                    SeedItem("3 Rouleaux Câble 2.5mm", 45000L, "450"),
                    SeedItem("Spots LED encastrables (lot de 8)", 32000L, "320"),
                    SeedItem("Disjoncteur différentiel Legrand", 18000L, "180"),
                    SeedItem("Prises et interrupteurs Simon", 14000L, "140"),
                    SeedItem("Goulottes & accessoires fixation", 7500L, "75")
                )
            ),
            SeedCalc(
                title = "Sortie Familiale Weekend Plage",
                groupId = "group_loisirs",
                currency = "DIRHAM",
                offsetMs = 195 * hour,
                items = listOf(
                    SeedItem("Parasol & 4 chaises pliantes location", 8000L, "80"),
                    SeedItem("Déjeuner Poissons frais Mohammedia", 38000L, "380"),
                    SeedItem("Glaces & Goûter pour les enfants", 7500L, "75"),
                    SeedItem("Péage et parking gardé", 4500L, "45")
                )
            ),
            SeedCalc(
                title = "Boucherie - Lhem & Kfta",
                groupId = "group_marche",
                currency = "DIRHAM",
                offsetMs = 220 * hour,
                items = listOf(
                    SeedItem("3kg Viande de veau filet", 33000L, "330"),
                    SeedItem("1.5kg Kefta fraîche assaisonnée", 16500L, "165"),
                    SeedItem("Saucisses de bœuf fraîches", 8500L, "85")
                )
            ),
            SeedCalc(
                title = "Voyage Professionnel Tanger",
                groupId = "group_commerce",
                currency = "DIRHAM",
                offsetMs = 245 * hour,
                items = listOf(
                    SeedItem("Billet Al Boraq Aller-Retour Casa-Tanger", 32000L, "320"),
                    SeedItem("Nuitée Hôtel ibis", 48000L, "480"),
                    SeedItem("Repas & Dîner d'affaires", 25000L, "250"),
                    SeedItem("Taxi & déplacements sur place", 9000L, "90")
                )
            ),
            SeedCalc(
                title = "Réparation Climatiseur & Gaz",
                groupId = "group_dar",
                currency = "DIRHAM",
                offsetMs = 270 * hour,
                items = listOf(
                    SeedItem("Recharge Gaz R410A", 35000L, "350"),
                    SeedItem("Nettoyage des filtres & désinfection", 12000L, "120"),
                    SeedItem("Main d'œuvre frigoriste", 15000L, "150")
                )
            ),
            SeedCalc(
                title = "Sel3a Fournisseur Tissus",
                groupId = "group_commerce",
                currency = "RIAL",
                offsetMs = 300 * hour,
                note = "Sel3a d l-khiyata dial l-3id",
                paymentStatus = "UNPAID",
                dueDateOffsetMs = 7 * day,
                items = listOf(
                    SeedItem("Toub Mlifa première qualité (15m)", 210000L, "42000"),
                    SeedItem("Toub Soie pour doublure (20m)", 100000L, "20000"),
                    SeedItem("Sfifa w 3qad sfifa beldia", 70000L, "14000")
                )
            )
        )
    }

        for (seed in seedCalculations) {
            val calcId = UUID.randomUUID().toString()
            val time = now - seed.offsetMs
            val dueDate = seed.dueDateOffsetMs?.let { now + it }
            val calcEntity = CalculationEntity(
                id = calcId,
                title = seed.title,
                currency = seed.currency,
                createdAtEpochMs = time,
                updatedAtEpochMs = time,
                status = "SAVED",
                note = seed.note,
                groupId = seed.groupId,
                paymentStatus = seed.paymentStatus,
                calcType = seed.calcType,
                dueDateEpochMs = dueDate,
                reminderEnabled = dueDate != null,
                reminderTimeEpochMs = dueDate
            )

            val itemEntities = seed.items.mapIndexed { idx, item ->
                CalculationItemEntity(
                    id = UUID.randomUUID().toString(),
                    calculationId = calcId,
                    label = item.label,
                    amountCentimes = item.amountCentimes,
                    rawExpression = item.expression ?: item.label,
                    position = idx,
                    createdAtEpochMs = time + idx * 1000L,
                    updatedAtEpochMs = time + idx * 1000L
                )
            }

            calcDao.upsertCalculationWithItems(calcEntity, itemEntities)
        }
    }
}