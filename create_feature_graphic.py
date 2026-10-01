import os
import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

ARTIFACT_DIR = r"C:/Users/Joe/.gemini/antigravity/brain/95a362e7-d8a9-47db-bf0a-d05637d009e1"
src_logo = Image.open(os.path.join(ARTIFACT_DIR, "official_logo_source.png")).convert("RGBA")

W, H = 1024, 500
fg = Image.new("RGBA", (W, H), (252, 249, 243, 255))
draw = ImageDraw.Draw(fg)

# Notebook subtle ruled lines
rule_color = (226, 218, 205, 120)
for y in range(40, H, 32):
    draw.line([(30, y), (W - 30, y)], fill=rule_color, width=1)

# Subtle vertical margin line (like a real notebook margin)
draw.line([(415, 20), (415, H - 20)], fill=(235, 160, 160, 90), width=2)

# Place Logo on Left side (inside X=30..400)
alpha = src_logo.split()[-1]
bbox = alpha.getbbox()
tight_logo = src_logo.crop(bbox)
tw, th = tight_logo.size

target_h = 370
target_w = 320
scale = min(target_w / tw, target_h / th)
lw = int(tw * scale)
lh = int(th * scale)
logo_resized = tight_logo.resize((lw, lh), Image.Resampling.LANCZOS)

lx = 45 + (355 - lw) // 2
ly = (H - lh) // 2
fg.paste(logo_resized, (lx, ly), logo_resized)

# Right side: Content & Typography
font_pill = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 13)
font_brand = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 34)
font_ar_brand = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 23)
font_sub = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 14)
font_footer = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 13)

rx = 455

# Badge Pill at top right
badge_text = "LE CARNET INTELLIGENT MAROCAIN"
draw.rounded_rectangle([rx, 46, rx + 285, 78], radius=16, fill=(244, 237, 226, 255), outline=(215, 200, 185, 255), width=1)
draw.text((rx + 18, 52), badge_text, font=font_pill, fill=(180, 83, 60, 255))

# French Title
draw.text((rx, 96), "Gérez vos calculs & dépenses", font=font_brand, fill=(30, 41, 59, 255))
draw.text((rx, 140), "simplement comme sur du papier", font=font_brand, fill=(180, 83, 60, 255))

# Arabic Tagline (properly reshaped and placed)
ar_tag = get_display(arabic_reshaper.reshape("الكناش المغربي الذكي لتدبير المصاريف والحسابات"))
draw.text((rx, 200), ar_tag, font=font_ar_brand, fill=(71, 85, 105, 255))

# 4 Feature Pills
features = [
    ("Calculs ligne par ligne", (220, 38, 38)),
    ("Listes & Partage WhatsApp", (5, 150, 105)),
    ("Tirelire & Objectifs d'épargne", (202, 138, 4)),
    ("100% Hors-ligne & Sécurisé", (37, 99, 235))
]

pill_y = 265
for i, (text, col) in enumerate(features):
    col_idx = i % 2
    row_idx = i // 2
    px = rx + col_idx * 265
    py = pill_y + row_idx * 55
    draw.rounded_rectangle([px, py, px + 252, py + 42], radius=10, fill=(255, 255, 255, 240), outline=(220, 225, 232, 255), width=1)
    # Bullet dot
    draw.ellipse([px + 14, py + 16, px + 24, py + 26], fill=col)
    draw.text((px + 32, py + 11), text, font=font_sub, fill=(30, 41, 59, 255))

# Footer subtle note
draw.text((rx, 395), "Sans connexion requise  •  Sauvegarde locale & PDF  •  Conçu pour le Maroc", font=font_footer, fill=(140, 150, 165, 255))

# Save
out_artifact = os.path.join(ARTIFACT_DIR, "playstore_feature_graphic_1024x500.png")
out_local = r"c:/Users/Joe/Desktop/sarf/playstore_feature_graphic_1024x500.png"
rgb_fg = fg.convert("RGB")
rgb_fg.save(out_artifact, quality=98)
rgb_fg.save(out_local, quality=98)
print("Updated feature graphic successfully")
