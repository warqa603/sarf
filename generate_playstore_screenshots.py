import os
import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ARTIFACT_DIR = r"C:/Users/Joe/.gemini/antigravity/brain/95a362e7-d8a9-47db-bf0a-d05637d009e1"
LOCAL_DIR = r"C:/Users/Joe/Desktop/sarf"

slides = [
    {
        "id": "playstore_slide_1_home.png",
        "src": os.path.join(ARTIFACT_DIR, "raw_screen_1_home.png"),
        "badge": "الكناش المغربي الذكي",
        "badge_color": (217, 119, 6),
        "title": "كلشي فـ بلاصة وحدة",
        "subtitle": "حسابات، مصاريف، قوائم سخرة، ونوطات منظمة بحال الورقة"
    },
    {
        "id": "playstore_slide_2_calc.png",
        "src": os.path.join(LOCAL_DIR, "calc_opened.png"),
        "badge": "حسابات ومصاريف مسطرة",
        "badge_color": (220, 38, 38),
        "title": "قيد مصاريفك سطر بسطر",
        "subtitle": "حساب تلقائي فوري بالدرهم والريال مع تفاصيل الصرف"
    },
    {
        "id": "playstore_slide_3_calculator.png",
        "src": os.path.join(LOCAL_DIR, "calc_eval.png"),
        "badge": "حاسبة مدمجة وسط الورقة",
        "badge_color": (37, 99, 235),
        "title": "حسب الكميات والأثمنة فالحين",
        "subtitle": "دير عمليات الضرب والجمع بلا ما تخرج من ورقة الحساب"
    },
    {
        "id": "playstore_slide_4_checklist.png",
        "src": os.path.join(LOCAL_DIR, "checklist_opened.png"),
        "badge": "قائمة التقضية وسخرة الدار",
        "badge_color": (5, 150, 105),
        "title": "كوشي مقاضيك وبارطاجي",
        "subtitle": "صيفط لا ليست لداركم فـ الواتساب بنقرة وحدة بلا نسيان"
    },
    {
        "id": "playstore_slide_5_contacts.png",
        "src": os.path.join(LOCAL_DIR, "screen_contacts.png"),
        "badge": "دليل المعلمين والحرفيين",
        "badge_color": (124, 58, 237),
        "title": "نمر البلومبي، التريسيان، ومول الحانوت",
        "subtitle": "تواصل فوري بالمكالمة أو الواتساب مع تتبع الكريدي والتسبيقات"
    },
    {
        "id": "playstore_slide_6_savings.png",
        "src": os.path.join(LOCAL_DIR, "screen_savings.png"),
        "badge": "حصالة التوفير والأهداف",
        "badge_color": (202, 138, 4),
        "title": "جمع لعمرة الوالدين أو العيد",
        "subtitle": "حدد أهدافك المالية وتبع شحال وفّرتي وشحال باقي ليك يوم بيوم"
    },
    {
        "id": "playstore_slide_7_notes.png",
        "src": os.path.join(LOCAL_DIR, "note_opened.png"),
        "badge": "كناش الملاحظات والأفكار",
        "badge_color": (13, 148, 136),
        "title": "قواعد ذهبية لتدبير المصروف",
        "subtitle": "أوراق ملونة، ماركورات، وخطط مالية ذكية لأسرتك"
    },
    {
        "id": "playstore_slide_8_voice.png",
        "src": os.path.join(LOCAL_DIR, "calc_popup.png"),
        "badge": "مساعد صوتي ذكي بالدارجة",
        "badge_color": (225, 29, 72),
        "title": "غير هضر وكولها بالدارجة",
        "subtitle": "سجل سلعتك ومصاريفك بصوتك بلا ما تكتب فـ الكلافي"
    }
]

W, H = 1080, 2400
font_badge = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 23)
font_title = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 52)
font_sub = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 28)

for idx, slide in enumerate(slides, 1):
    bg = Image.new("RGBA", (W, H), (252, 250, 246, 255))
    draw = ImageDraw.Draw(bg)
    
    # Warm ruled paper lines across header
    rule_color = (226, 218, 205, 110)
    for y in range(40, 360, 36):
        draw.line([(50, y), (W - 50, y)], fill=rule_color, width=1)
        
    # 1. Badge Pill
    badge_text = get_display(arabic_reshaper.reshape(slide["badge"]))
    bbox_b = draw.textbbox((0, 0), badge_text, font=font_badge)
    bw = bbox_b[2] - bbox_b[0]
    bh = bbox_b[3] - bbox_b[1]
    
    dot_size = 10
    dot_margin = 12
    pill_w = bw + dot_size + dot_margin + 44
    pill_h = 44
    pill_x = (W - pill_w) // 2
    pill_y = 60
    
    draw.rounded_rectangle(
        [pill_x, pill_y, pill_x + pill_w, pill_y + pill_h],
        radius=22,
        fill=(245, 240, 232, 255),
        outline=(218, 208, 195, 255),
        width=1
    )
    
    # Dot & text in pill
    dot_x = pill_x + pill_w - 22 - dot_size
    dot_y = pill_y + (pill_h - dot_size) // 2
    bc = slide["badge_color"]
    draw.ellipse([dot_x, dot_y, dot_x + dot_size, dot_y + dot_size], fill=bc)
    
    text_x = pill_x + 22
    text_y = pill_y + (pill_h - bh) // 2 - 2
    draw.text((text_x, text_y), badge_text, font=font_badge, fill=(30, 41, 59, 255))
    
    # 2. Main Title
    title_text = get_display(arabic_reshaper.reshape(slide["title"]))
    bbox_t = draw.textbbox((0, 0), title_text, font=font_title)
    tw = bbox_t[2] - bbox_t[0]
    draw.text(((W - tw) // 2, 134), title_text, font=font_title, fill=(24, 33, 47, 255))
    
    # 3. Subtitle
    sub_text = get_display(arabic_reshaper.reshape(slide["subtitle"]))
    bbox_s = draw.textbbox((0, 0), sub_text, font=font_sub)
    sw = bbox_s[2] - bbox_s[0]
    draw.text(((W - sw) // 2, 214), sub_text, font=font_sub, fill=(85, 98, 112, 255))
    
    # 4. Device Mockup Frame
    screen = Image.open(slide["src"]).convert("RGBA")
    phone_w = 880
    phone_h = 2020
    phone_x = (W - phone_w) // 2
    phone_y = 355
    corner_radius = 50
    bezel = 16
    
    # Shadow
    shadow_pad = 50
    shadow = Image.new("RGBA", (phone_w + shadow_pad * 2, phone_h + shadow_pad * 2), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle(
        [shadow_pad, shadow_pad + 18, shadow_pad + phone_w, shadow_pad + phone_h + 18],
        radius=corner_radius,
        fill=(0, 0, 0, 85)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    bg.alpha_composite(shadow, (phone_x - shadow_pad, phone_y - shadow_pad))
    
    # Phone Body
    phone_frame = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    pf_draw = ImageDraw.Draw(phone_frame)
    pf_draw.rounded_rectangle(
        [0, 0, phone_w, phone_h],
        radius=corner_radius,
        fill=(28, 33, 40, 255),
        outline=(55, 65, 81, 255),
        width=2
    )
    
    # Inner screen
    inner_w = phone_w - bezel * 2
    inner_h = phone_h - bezel * 2
    inner_r = corner_radius - 8
    
    screen_resized = screen.resize((inner_w, inner_h), Image.Resampling.LANCZOS)
    mask = Image.new("L", (inner_w, inner_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, inner_w, inner_h], radius=inner_r, fill=255)
    
    phone_frame.paste(screen_resized, (bezel, bezel), mask)
    
    # Camera punch hole
    cam_r = 10
    cam_x = phone_w // 2
    cam_y = bezel + 22
    pf_draw.ellipse([cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r], fill=(12, 14, 18, 255))
    pf_draw.ellipse([cam_x - 3, cam_y - 3, cam_x + 1, cam_y + 1], fill=(40, 50, 65, 180))
    
    bg.alpha_composite(phone_frame, (phone_x, phone_y))
    
    # Save both locally and in artifact directory
    out_artifact = os.path.join(ARTIFACT_DIR, slide["id"])
    out_local = os.path.join(LOCAL_DIR, slide["id"])
    rgb_img = bg.convert("RGB")
    rgb_img.save(out_artifact, quality=96)
    rgb_img.save(out_local, quality=96)
    print(f"[{idx}/8] Created {slide['id']}")

print("All 8 Play Store screenshots created successfully!")
