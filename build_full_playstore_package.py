import os
import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ARTIFACT_DIR = r"C:/Users/Joe/.gemini/antigravity/brain/95a362e7-d8a9-47db-bf0a-d05637d009e1"
LOCAL_DIR = r"C:/Users/Joe/Desktop/sarf"
ASSETS_DIR = os.path.join(LOCAL_DIR, "playstore_assets")

os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(os.path.join(ASSETS_DIR, "french"), exist_ok=True)
os.makedirs(os.path.join(ASSETS_DIR, "arabic"), exist_ok=True)
os.makedirs(os.path.join(ASSETS_DIR, "primary_upload"), exist_ok=True)

W, H = 1080, 2160  # Exactly 1:2 ratio, strictly compliant with Google Play Console (max <= 2x min)

font_badge = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 23)
font_title = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 50)
font_sub = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 27)

def render_slide(slide_info, output_paths):
    bg = Image.new("RGBA", (W, H), (252, 250, 246, 255))
    draw = ImageDraw.Draw(bg)
    
    # 1. Warm Moroccan notebook ruled lines at the top
    rule_color = (226, 218, 205, 115)
    for y in range(40, 320, 36):
        draw.line([(45, y), (W - 45, y)], fill=rule_color, width=1)
        
    is_arabic = slide_info.get("lang") == "ar"
    
    # Badge Pill
    badge_raw = slide_info["badge"]
    badge_text = get_display(arabic_reshaper.reshape(badge_raw)) if is_arabic else badge_raw
    bbox_b = draw.textbbox((0, 0), badge_text, font=font_badge)
    bw = bbox_b[2] - bbox_b[0]
    bh = bbox_b[3] - bbox_b[1]
    
    dot_size = 11
    dot_margin = 12
    pill_w = bw + dot_size + dot_margin + 44
    pill_h = 44
    pill_x = (W - pill_w) // 2
    pill_y = 52
    
    draw.rounded_rectangle(
        [pill_x, pill_y, pill_x + pill_w, pill_y + pill_h],
        radius=22,
        fill=(245, 240, 232, 255),
        outline=(218, 208, 195, 255),
        width=1
    )
    
    bc = slide_info["badge_color"]
    if is_arabic:
        # Dot on the right
        dot_x = pill_x + pill_w - 20 - dot_size
        dot_y = pill_y + (pill_h - dot_size) // 2
        draw.ellipse([dot_x, dot_y, dot_x + dot_size, dot_y + dot_size], fill=bc)
        text_x = pill_x + 20
        text_y = pill_y + (pill_h - bh) // 2 - 2
        draw.text((text_x, text_y), badge_text, font=font_badge, fill=(30, 41, 59, 255))
    else:
        # Dot on the left
        dot_x = pill_x + 20
        dot_y = pill_y + (pill_h - dot_size) // 2
        draw.ellipse([dot_x, dot_y, dot_x + dot_size, dot_y + dot_size], fill=bc)
        text_x = pill_x + 20 + dot_size + dot_margin
        text_y = pill_y + (pill_h - bh) // 2 - 2
        draw.text((text_x, text_y), badge_text, font=font_badge, fill=(30, 41, 59, 255))
        
    # Title
    title_raw = slide_info["title"]
    title_text = get_display(arabic_reshaper.reshape(title_raw)) if is_arabic else title_raw
    bbox_t = draw.textbbox((0, 0), title_text, font=font_title)
    tw = bbox_t[2] - bbox_t[0]
    draw.text(((W - tw) // 2, 122), title_text, font=font_title, fill=(24, 33, 47, 255))
    
    # Subtitle
    sub_raw = slide_info["subtitle"]
    sub_text = get_display(arabic_reshaper.reshape(sub_raw)) if is_arabic else sub_raw
    bbox_s = draw.textbbox((0, 0), sub_text, font=font_sub)
    sw = bbox_s[2] - bbox_s[0]
    draw.text(((W - sw) // 2, 196), sub_text, font=font_sub, fill=(85, 98, 112, 255))
    
    # Mockup Frame
    phone_w = 885
    phone_h = int(phone_w * 2340 / 1080)  # Preserves exact 1080:2340 device aspect ratio (~1917px)
    phone_x = (W - phone_w) // 2
    phone_y = 295
    corner_radius = 50
    bezel = 15
    
    # Soft drop shadow
    shadow_pad = 45
    shadow = Image.new("RGBA", (phone_w + shadow_pad * 2, phone_h + shadow_pad * 2), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle(
        [shadow_pad, shadow_pad + 18, shadow_pad + phone_w, shadow_pad + phone_h + 18],
        radius=corner_radius,
        fill=(0, 0, 0, 85)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    bg.alpha_composite(shadow, (phone_x - shadow_pad, phone_y - shadow_pad))
    
    # Phone Body Frame
    phone_frame = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    pf_draw = ImageDraw.Draw(phone_frame)
    pf_draw.rounded_rectangle(
        [0, 0, phone_w, phone_h],
        radius=corner_radius,
        fill=(28, 33, 40, 255),
        outline=(55, 65, 81, 255),
        width=2
    )
    
    # Screen insertion
    inner_w = phone_w - bezel * 2
    inner_h = phone_h - bezel * 2
    inner_r = corner_radius - 8
    
    src = Image.open(slide_info["src"]).convert("RGBA")
    screen_resized = src.resize((inner_w, inner_h), Image.Resampling.LANCZOS)
    mask = Image.new("L", (inner_w, inner_h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, inner_w, inner_h], radius=inner_r, fill=255)
    phone_frame.paste(screen_resized, (bezel, bezel), mask)
    
    # Punch hole camera
    cam_r = 10
    cam_x = phone_w // 2
    cam_y = bezel + 22
    pf_draw.ellipse([cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r], fill=(12, 14, 18, 255))
    pf_draw.ellipse([cam_x - 3, cam_y - 3, cam_x + 1, cam_y + 1], fill=(40, 50, 65, 180))
    
    bg.alpha_composite(phone_frame, (phone_x, phone_y))
    
    # Save as 24-bit RGB PNG (Strict Play Store requirement: no alpha channel)
    rgb_img = bg.convert("RGB")
    for p in output_paths:
        os.makedirs(os.path.dirname(p), exist_ok=True)
        rgb_img.save(p, quality=96)
        print(f"Saved: {os.path.basename(p)}")

# 1. French Slides Suite
french_slides = [
    {
        "id": "01_accueil.png",
        "lang": "fr",
        "src": os.path.join(ARTIFACT_DIR, "fr_home_clean.png"),
        "badge": "LE CARNET INTELLIGENT MAROCAIN",
        "badge_color": (217, 119, 6),
        "title": "Tout en un seul endroit",
        "subtitle": "Calculs, dépenses, listes de courses et notes organisées"
    },
    {
        "id": "02_calculs.png",
        "lang": "fr",
        "src": os.path.join(ARTIFACT_DIR, "fr_calc_opened.png"),
        "badge": "DÉPENSES & CALCULS LIGNÉS",
        "badge_color": (220, 38, 38),
        "title": "Notez vos dépenses ligne par ligne",
        "subtitle": "Calcul automatique en Dirhams avec gestion du crédit et du reste"
    },
    {
        "id": "03_calculatrice.png",
        "lang": "fr",
        "src": os.path.join(ARTIFACT_DIR, "fr_calc_eval2.png"),
        "badge": "CALCULATRICE INTÉGRÉE",
        "badge_color": (37, 99, 235),
        "title": "Calculez sans quitter votre page",
        "subtitle": "Multiplications, pourcentages et totaux directement sur votre feuille"
    },
    {
        "id": "04_liste_courses.png",
        "lang": "fr",
        "src": os.path.join(ARTIFACT_DIR, "fr_checklist_opened.png"),
        "badge": "LISTE DE COURSES & MARCHÉ",
        "badge_color": (5, 150, 105),
        "title": "Cochez vos courses et partagez",
        "subtitle": "Envoyez la liste complète sur WhatsApp en un clic sans rien oublier"
    },
    {
        "id": "05_epargne_tirelire.png",
        "lang": "fr",
        "src": os.path.join(ARTIFACT_DIR, "fr_savings.png"),
        "badge": "TIRELIRE & OBJECTIFS D'ÉPARGNE",
        "badge_color": (202, 138, 4),
        "title": "Réalisez vos projets pas à pas",
        "subtitle": "Épargnez pour l'Aïd ou la Omra et suivez chaque dirham économisé"
    },
    {
        "id": "06_contacts_artisans.png",
        "lang": "fr",
        "src": os.path.join(ARTIFACT_DIR, "fr_contacts.png"),
        "badge": "RÉPERTOIRE D'ARTISANS & PROCHES",
        "badge_color": (124, 58, 237),
        "title": "Plombier, électricien et commerçants",
        "subtitle": "Appel ou WhatsApp en un tap avec suivi des avances et dettes"
    }
]

# 2. Arabic Slides Suite (WITH USER MANDATED CORRECTIONS)
arabic_slides = [
    {
        "id": "01_kounach_daki.png",
        "lang": "ar",
        "src": os.path.join(ARTIFACT_DIR, "raw_screen_1_home.png"),
        "badge": "الكناش المغربي الذكي",
        "badge_color": (217, 119, 6),
        "title": "كلشي فـ بلاصة وحدة",
        "subtitle": "حسابات، مصاريف، قوائم سخرة، ونوطات منظمة بحال الورقة"
    },
    {
        "id": "02_hssabat_moustara.png",
        "lang": "ar",
        "src": os.path.join(LOCAL_DIR, "calc_opened.png"),
        "badge": "حسابات ومصاريف مسطرة",
        "badge_color": (220, 38, 38),
        "title": "قيد مصاريفك سطر بسطر",
        "subtitle": "حساب تلقائي فوري بالدرهم والريال مع تفاصيل الصرف"
    },
    {
        "id": "03_hassiba_moudmaja.png",
        "lang": "ar",
        "src": os.path.join(LOCAL_DIR, "calc_eval.png"),
        "badge": "حاسبة مدمجة وسط الورقة",
        "badge_color": (37, 99, 235),
        "title": "حسب الكميات والأثمنة فالحين",
        "subtitle": "دير عمليات الضرب والجمع بلا ما تخرج من ورقة الحساب"
    },
    {
        "id": "04_taqdiya_checklist.png",
        "lang": "ar",
        "src": os.path.join(LOCAL_DIR, "checklist_opened.png"),
        "badge": "قائمة التقضية وسخرة الدار",
        "badge_color": (5, 150, 105),
        "title": "كوشي مقاديرك وبارطاجي",  # USER FIX 1: Exact requested spelling
        "subtitle": "صيفط لا ليست لداركم فـ الواتساب بنقرة وحدة بلا نسيان"
    },
    {
        "id": "05_dalil_lme3lmin.png",
        "lang": "ar",
        "src": os.path.join(LOCAL_DIR, "screen_contacts.png"),
        "badge": "دليل المعلمين والحرفيين",
        "badge_color": (124, 58, 237),
        "title": "نوامر البلومبي، التريسيان، ومول الحانوت",  # USER FIX 2: Exact requested wording
        "subtitle": "تواصل فوري بالمكالمة أو الواتساب مع تتبع الكريدي والتسبيقات"
    },
    {
        "id": "06_hassala_tawfir.png",
        "lang": "ar",
        "src": os.path.join(LOCAL_DIR, "screen_savings.png"),
        "badge": "حصالة التوفير والأهداف",
        "badge_color": (202, 138, 4),
        "title": "جمع لعمرة الوالدين أو العيد",
        "subtitle": "حدد أهدافك المالية وتبع شحال وفّرتي وشحال باقي ليك يوم بيوم"
    },
    {
        "id": "07_moussa3id_sawti.png",
        "lang": "ar",
        "src": os.path.join(LOCAL_DIR, "calc_popup.png"),
        "badge": "مساعد صوتي ذكي بالدارجة",
        "badge_color": (225, 29, 72),
        "title": "غير هضر وكولها بالدارجة",
        "subtitle": "سجل سلعتك ومصاريفك بصوتك بلا ما تكتب فـ الكلافي"
    }
]

# 3. Top 8 Primary Upload Sequence (Ideal 8-slide mix ready for immediate submission)
primary_slides = [
    (french_slides[0], "playstore_slide_1_fr_accueil.png"),
    (french_slides[1], "playstore_slide_2_fr_calculs.png"),
    (french_slides[2], "playstore_slide_3_fr_calculatrice.png"),
    (french_slides[3], "playstore_slide_4_fr_courses.png"),
    (french_slides[4], "playstore_slide_5_fr_epargne.png"),
    (arabic_slides[3], "playstore_slide_6_ar_checklist.png"),  # كوشي مقاديرك وبارطاجي
    (arabic_slides[4], "playstore_slide_7_ar_contacts.png"),   # نوامر البلومبي، التريسيان، ومول الحانوت
    (arabic_slides[6], "playstore_slide_8_ar_voice.png"),      # غير هضر وكولها بالدارجة
]

print("=== Generating French Slides ===")
for s in french_slides:
    paths = [
        os.path.join(ASSETS_DIR, "french", s["id"]),
        os.path.join(ARTIFACT_DIR, f"fr_{s['id']}")
    ]
    render_slide(s, paths)

print("\n=== Generating Arabic Slides ===")
for s in arabic_slides:
    paths = [
        os.path.join(ASSETS_DIR, "arabic", s["id"]),
        os.path.join(ARTIFACT_DIR, f"ar_{s['id']}")
    ]
    render_slide(s, paths)

print("\n=== Generating Primary 8 Slides ===")
for slide_info, filename in primary_slides:
    paths = [
        os.path.join(ASSETS_DIR, "primary_upload", filename),
        os.path.join(LOCAL_DIR, filename),
        os.path.join(ARTIFACT_DIR, filename)
    ]
    render_slide(slide_info, paths)

print("\nAll screenshot assets generated successfully!")
