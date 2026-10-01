import os
import shutil

DESKTOP_DIR = r"C:/Users/Joe/Desktop/WARQA_PLAYSTORE_READY"
LOCAL_DIR = r"c:/Users/Joe/Desktop/sarf"
SOURCE_ASSETS = os.path.join(LOCAL_DIR, "playstore_assets")

os.makedirs(DESKTOP_DIR, exist_ok=True)
os.makedirs(os.path.join(DESKTOP_DIR, "AUTRES_LANGUES", "TOUT_EN_FRANCAIS"), exist_ok=True)
os.makedirs(os.path.join(DESKTOP_DIR, "AUTRES_LANGUES", "TOUT_EN_ARABE"), exist_ok=True)

# 1. Copy App Icon
shutil.copy2(
    os.path.join(SOURCE_ASSETS, "playstore_icon_512.png"),
    os.path.join(DESKTOP_DIR, "00_ICON_OFFICIELLE_512x512.png")
)

# 2. Copy Covers (1024x500)
shutil.copy2(
    os.path.join(SOURCE_ASSETS, "playstore_feature_graphic_1024x500.png"),
    os.path.join(DESKTOP_DIR, "01_COVER_PRINCIPAL_1024x500.png")
)
shutil.copy2(
    os.path.join(SOURCE_ASSETS, "playstore_feature_graphic_ar_1024x500.png"),
    os.path.join(DESKTOP_DIR, "01_COVER_ARABE_1024x500.png")
)

# 3. Copy the 8 Primary Screens (Sequentially numbered 1 to 8 for easy multi-select upload)
primary_mapping = [
    ("playstore_slide_1_fr_accueil.png", "SCREEN_1_Accueil_FR.png"),
    ("playstore_slide_2_fr_calculs.png", "SCREEN_2_Calculs_Lignes_FR.png"),
    ("playstore_slide_3_fr_calculatrice.png", "SCREEN_3_Calculatrice_FR.png"),
    ("playstore_slide_4_fr_courses.png", "SCREEN_4_Courses_WhatsApp_FR.png"),
    ("playstore_slide_5_fr_epargne.png", "SCREEN_5_Epargne_Tirelire_FR.png"),
    ("playstore_slide_6_ar_checklist.png", "SCREEN_6_Checklist_Kouchi_Maqadirek_AR.png"),
    ("playstore_slide_7_ar_contacts.png", "SCREEN_7_Repertoire_Nwamer_Lme3lmin_AR.png"),
    ("playstore_slide_8_ar_voice.png", "SCREEN_8_Moussa3id_Sawti_Darija_AR.png"),
]

for src_name, dst_name in primary_mapping:
    src_file = os.path.join(SOURCE_ASSETS, "primary_upload", src_name)
    shutil.copy2(src_file, os.path.join(DESKTOP_DIR, dst_name))

# 4. Copy French & Arabic suites
for f in os.listdir(os.path.join(SOURCE_ASSETS, "french")):
    if f.endswith(".png"):
        shutil.copy2(os.path.join(SOURCE_ASSETS, "french", f), os.path.join(DESKTOP_DIR, "AUTRES_LANGUES", "TOUT_EN_FRANCAIS", f))

for f in os.listdir(os.path.join(SOURCE_ASSETS, "arabic")):
    if f.endswith(".png"):
        shutil.copy2(os.path.join(SOURCE_ASSETS, "arabic", f), os.path.join(DESKTOP_DIR, "AUTRES_LANGUES", "TOUT_EN_ARABE", f))

# 5. Create DESCRIPTIONS_ET_TITRES.txt with clear copy-paste blocks
text_content = """================================================================================
                    WARQA • ورقة - PACK PLAY STORE
================================================================================
Ce dossier contient tous les éléments nécessaires pour publier l'application
Warqa sur la Google Play Console.
================================================================================


--------------------------------------------------------------------------------
1. TEXTES EN FRANÇAIS (FICHE PRINCIPALE)
--------------------------------------------------------------------------------

[TITRE DE L'APPLICATION] (Max 30 caractères)
Copier ci-dessous :
Warqa • Carnet & Dépenses

(Longueur : 25 caractères - Conforme Google Play)


[DESCRIPTION COURTE] (Max 80 caractères)
Copier ci-dessous :
Le carnet marocain intelligent : calculs, dépenses, listes de courses & notes.

(Longueur : 78 caractères - Conforme Google Play)


[DESCRIPTION COMPLÈTE]
Copier ci-dessous :
Warqa (ورقة) est le carnet de notes et de comptes pensé spécialement pour le quotidien des familles et des commerçants au Maroc. 

Fini les bouts de papier égarés et les calculs approximatifs ! Avec son design chaleureux inspiré des vrais cahiers d'écolier et sa marge rouge emblématique, Warqa allie la simplicité du papier à la puissance d'un assistant intelligent 100% marocain.

📝 TOUTES VOS PAGES EN UN SEUL ENDROIT :
• Carnet de calculs & dépenses : Notez vos achats ligne par ligne (viande, légumes, factures, épicerie...). L'application calcule automatiquement les totaux, le reste et la monnaie en Dirhams (DH) avec conversion en Rial.
• Calculatrice intégrée : Plus besoin d'ouvrir une calculette externe ! Tapez directement vos opérations de multiplications, pourcentages et additions au cœur même de vos feuilles de calcul.
• Listes de courses & سخرة : Préparez votre liste de marché (Taqdiya), cochez les articles achetés au fur et à mesure et partagez la liste d'un simple clic sur WhatsApp avec votre famille.
• Répertoire des Artisans & المعلمين : Gardez les numéros de votre plombier, électricien, menuisier ou de l'épicier du quartier. Suivez en toute clarté les avances versées, les acomptes et le crédit (كريدي).
• Tirelire & Objectifs d'épargne : Donnez vie à vos projets familiaux (Mouton de l'Aïd, voyage, vacances ou Omra pour les parents). Fixez un montant et suivez vos économies jour après jour.

🎙️ DICTÉE VOCALE INTELLIGENTE EN DARIJA :
Pas le temps de taper ? Parlez simplement à votre téléphone en Darija marocaine ou en français ! Warqa comprend le langage parlé et retranscrit instantanément vos articles et vos montants sur votre feuille.

🔒 100% HORS-LIGNE & VIE PRIVÉE PROTÉGÉE :
• Fonctionne partout, même au fond du souk ou sans connexion Internet.
• Vos données et vos comptes restent strictement stockés sur votre téléphone. Aucune inscription obligatoire, aucun espionnage, zéro publicité intrusive.

Téléchargez Warqa (ورقة) dès aujourd'hui et retrouvez le plaisir de tenir vos comptes en toute sérénité !



--------------------------------------------------------------------------------
2. TEXTES EN ARABE (FICHE ARABE / بالدارجة المغربية)
--------------------------------------------------------------------------------

[اسم التطبيق] (أقصى حد 30 حرف)
انسخ ما يلي :
ورقة • كناش الحسابات والمصاريف

(الطول : 30 حرف - مطابق لمعايير قوقل بلاي)


[الوصف القصير] (أقصى حد 80 حرف)
انسخ ما يلي :
الكناش المغربي الذكي: قيد حساباتك، سخرتك، مصاريفك ونوامر معلمين الدار.

(الطول : 73 حرف - مطابق لمعايير قوقل بلاي)


[الوصف الكامل]
انسخ ما يلي :
تطبيق "ورقة • Warqa" هو الكناش المغربي الذكي لي كيجمع ليك كاع حساباتك، سخرتك، مصاريف دارك، ونوامر الحرفيين فـ بلاصة وحدة، بتصميم دافي مستوحى من الدفتر المغربي الأصيل بالسطورة ومارج الحمر.

بلا ما تبقى تقلب على طراف د الكاغط ولا تدوخ فـ الحسابات! ورقة كيعطيك السلاسة ديال الكناش التقليدي مع ذكاء وسرعة الهاتف.

📝 مميزات تطبيق ورقة :
• أوراق الحسابات والمصاريف : قيد تقضية السيمانة، مصاريف الدار، أو حساب الحانوت سطر بسطر. التطبيق كيحسب ليك المجموع تلقائياً بالدرهم مع تحويل الصرف للريال فالحين بلا ما تدوخ.
• حاسبة مدمجة وسط الورقة : دير عمليات الضرب، الجمع، والنسب المئوية مباشرة وسط الورقة بلا ما تخرج من التطبيق.
• قوائم السخرة والتقضية (Checklist) : قاد لا ليست د السخرة، كوشي السلعة لي شريتي فـ المارشي، وصيفط القائمة كاملة لداركم فـ الواتساب بنقرة وحدة بلا نسيان.
• دليل المعلمين والحرفيين : احتفظ بنوامر البلومبي، التريسيان، الصباغ، مول الحانوت، والكساب. تبع معاهم التسبيقات، شحال عطيتي وشحال باقي فـ الكريدي بكل وضوح.
• حصالة التوفير والأهداف : جمع لعمرة الوالدين، خروف العيد، ولا عطلة الصيف. حدد الهدف وتبع شحال وفرتي وشحال باقي ليك نهار بنهار.

🎙️ المساعد الصوتي بالدارجة المغربية :
إلى كنتي زربان وما فيك ما يكتب فـ الكلافي، غير هضر بالدارجة والتطبيق كيفهم كلامك وكيقيد السلعة والثمن فـ الورقة فالحين وبلا تعقيد.

🔒 خصوصية تامة وخدام 100% بلا إنترنت (Offline) :
• خدام ديما وخا تكون فـ قاع السيمانة ولا فـ بلاصة ما فيهاش الريزو.
• بياناتك وحساباتك كتبقى مسجلة غير فـ تيلفونك، بلا تسجيل حساب، بلا ما يخرج حتى رقم من جهازك.

تيليشارجي تطبيق "ورقة" دابا، وجمع شتات حساباتك ومصاريفك بكل راحة بال!



--------------------------------------------------------------------------------
3. LISTE DES FICHIERS IMAGES DU DOSSIER
--------------------------------------------------------------------------------
- 00_ICON_OFFICIELLE_512x512.png       : À glisser dans "Icône de l'application" (512x512)
- 01_COVER_PRINCIPAL_1024x500.png      : À glisser dans "Graphique vedette / Feature graphic" (1024x500)
- SCREEN_1_Accueil_FR.png              : Capture 1 (Téléphone)
- SCREEN_2_Calculs_Lignes_FR.png       : Capture 2 (Téléphone)
- SCREEN_3_Calculatrice_FR.png         : Capture 3 (Téléphone)
- SCREEN_4_Courses_WhatsApp_FR.png     : Capture 4 (Téléphone)
- SCREEN_5_Epargne_Tirelire_FR.png     : Capture 5 (Téléphone)
- SCREEN_6_Checklist_Kouchi_Maqadirek_AR.png : Capture 6 (Téléphone - كوشي مقاديرك)
- SCREEN_7_Repertoire_Nwamer_Lme3lmin_AR.png : Capture 7 (Téléphone - نوامر البلومبي والتريسيان)
- SCREEN_8_Moussa3id_Sawti_Darija_AR.png     : Capture 8 (Téléphone - غير هضر وكولها بالدارجة)

Dossier "AUTRES_LANGUES/" :
- TOUT_EN_FRANCAIS/ : 6 captures si vous souhaitez une fiche 100% française
- TOUT_EN_ARABE/    : 7 captures si vous souhaitez une fiche 100% arabe
================================================================================
"""

with open(os.path.join(DESKTOP_DIR, "DESCRIPTIONS_ET_TITRES.txt"), "w", encoding="utf-8") as f:
    f.write(text_content)

# Also keep a mirror in the project directory
shutil.copytree(DESKTOP_DIR, os.path.join(LOCAL_DIR, "PLAYSTORE_PACKAGE"), dirs_exist_ok=True)

print("Clean package created successfully on Desktop:")
print(DESKTOP_DIR)
