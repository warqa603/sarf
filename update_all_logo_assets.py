import os
import shutil
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import arabic_reshaper
from bidi.algorithm import get_display

ARTIFACT_DIR = r"C:/Users/Joe/.gemini/antigravity/brain/95a362e7-d8a9-47db-bf0a-d05637d009e1"
LOCAL_DIR = r"c:/Users/Joe/Desktop/sarf"
ASSETS_DIR = os.path.join(LOCAL_DIR, "playstore_assets")
RES_DIR = os.path.join(LOCAL_DIR, "app", "src", "main", "res")

NEW_LOGO_SRC = os.path.join(ARTIFACT_DIR, ".user_uploaded", "media_1789951863479.png")

# 1. Copy source to artifact & local dir as official_logo_source.png
shutil.copy2(NEW_LOGO_SRC, os.path.join(ARTIFACT_DIR, "official_logo_source.png"))
shutil.copy2(NEW_LOGO_SRC, os.path.join(LOCAL_DIR, "official_logo_source.png"))
shutil.copy2(NEW_LOGO_SRC, os.path.join(ASSETS_DIR, "official_logo_source.png"))
print("Copied official_logo_source.png")

# Load and crop tight content
src_img = Image.open(NEW_LOGO_SRC).convert("RGBA")
bbox = src_img.getbbox()
content = src_img.crop(bbox)
cw, ch = content.size

# --- 2. Generate Play Store Icon (512x512, 24-bit RGB) ---
bg_cream = (251, 248, 243)
icon_512 = Image.new("RGBA", (512, 512), bg_cream + (255,))

# Safe zone: height ~410px out of 512px leaves ~50px safe margins for Google Play squircle
scale_512 = 410.0 / ch
tw_512, th_512 = int(cw * scale_512), int(ch * scale_512)
scaled_512 = content.resize((tw_512, th_512), Image.Resampling.LANCZOS)
px_512 = (512 - tw_512) // 2
py_512 = (512 - th_512) // 2
icon_512.alpha_composite(scaled_512, (px_512, py_512))

rgb_icon_512 = icon_512.convert("RGB")
rgb_icon_512.save(os.path.join(ARTIFACT_DIR, "playstore_icon_512.png"), quality=98)
rgb_icon_512.save(os.path.join(LOCAL_DIR, "playstore_icon_512.png"), quality=98)
rgb_icon_512.save(os.path.join(ASSETS_DIR, "playstore_icon_512.png"), quality=98)
print("Generated playstore_icon_512.png (512x512)")

# --- 3. Generate Android Launcher Mipmap Icons ---
# Densities: (name, fg_size, legacy_size)
densities = [
    ("mipmap-mdpi", 108, 48),
    ("mipmap-hdpi", 162, 72),
    ("mipmap-xhdpi", 216, 96),
    ("mipmap-xxhdpi", 324, 144),
    ("mipmap-xxxhdpi", 432, 192),
]

for folder_name, fg_size, leg_size in densities:
    target_folder = os.path.join(RES_DIR, folder_name)
    os.makedirs(target_folder, exist_ok=True)
    
    # A) Foreground layer (adaptive: 108dp canvas, ~66% safe zone diameter)
    fg_canvas = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    fg_target_h = int(fg_size * 0.65)
    fg_scale = fg_target_h / float(ch)
    fg_tw = int(cw * fg_scale)
    fg_th = int(ch * fg_scale)
    fg_scaled = content.resize((fg_tw, fg_th), Image.Resampling.LANCZOS)
    fg_x = (fg_size - fg_tw) // 2
    fg_y = (fg_size - fg_th) // 2
    fg_canvas.paste(fg_scaled, (fg_x, fg_y), fg_scaled)
    
    fg_path = os.path.join(target_folder, "ic_launcher_foreground.webp")
    fg_canvas.save(fg_path, "WEBP", quality=98)
    
    # B) Legacy square/squircle icon (ic_launcher.webp)
    leg_canvas = Image.new("RGBA", (leg_size, leg_size), bg_cream + (255,))
    leg_target_h = int(leg_size * 0.80)
    leg_scale = leg_target_h / float(ch)
    leg_tw = int(cw * leg_scale)
    leg_th = int(ch * leg_scale)
    leg_scaled = content.resize((leg_tw, leg_th), Image.Resampling.LANCZOS)
    leg_x = (leg_size - leg_tw) // 2
    leg_y = (leg_size - leg_th) // 2
    leg_canvas.paste(leg_scaled, (leg_x, leg_y), leg_scaled)
    
    # Apply rounded squircle mask for legacy launcher
    mask_leg = Image.new("L", (leg_size, leg_size), 0)
    r_leg = max(4, int(leg_size * 0.20))
    ImageDraw.Draw(mask_leg).rounded_rectangle([0, 0, leg_size, leg_size], radius=r_leg, fill=255)
    leg_final = Image.new("RGBA", (leg_size, leg_size), (0, 0, 0, 0))
    leg_final.paste(leg_canvas, (0, 0), mask_leg)
    
    leg_path = os.path.join(target_folder, "ic_launcher.webp")
    leg_final.save(leg_path, "WEBP", quality=98)
    
    # C) Legacy round icon (ic_launcher_round.webp)
    round_canvas = Image.new("RGBA", (leg_size, leg_size), bg_cream + (255,))
    round_target_h = int(leg_size * 0.72)
    round_scale = round_target_h / float(ch)
    round_tw = int(cw * round_scale)
    round_th = int(ch * round_scale)
    round_scaled = content.resize((round_tw, round_th), Image.Resampling.LANCZOS)
    round_x = (leg_size - round_tw) // 2
    round_y = (leg_size - round_th) // 2
    round_canvas.paste(round_scaled, (round_x, round_y), round_scaled)
    
    mask_round = Image.new("L", (leg_size, leg_size), 0)
    ImageDraw.Draw(mask_round).ellipse([0, 0, leg_size, leg_size], fill=255)
    round_final = Image.new("RGBA", (leg_size, leg_size), (0, 0, 0, 0))
    round_final.paste(round_canvas, (0, 0), mask_round)
    
    round_path = os.path.join(target_folder, "ic_launcher_round.webp")
    round_final.save(round_path, "WEBP", quality=98)

print("Updated all Android launcher icons (mdpi to xxxhdpi)")

# --- 4. Update Play Store Feature Graphic (1024x500) ---
W, H = 1024, 500
fg = Image.new("RGBA", (W, H), (252, 249, 243, 255))
draw = ImageDraw.Draw(fg)

# Notebook subtle ruled lines
rule_color = (226, 218, 205, 120)
for y in range(40, H, 32):
    draw.line([(30, y), (W - 30, y)], fill=rule_color, width=1)

# Subtle vertical margin line (like a real notebook margin)
draw.line([(415, 20), (415, H - 20)], fill=(235, 160, 160, 90), width=2)

# Place new logo on Left side (inside X=30..400)
target_h = 370
target_w = 340
scale_fg = min(target_w / float(cw), target_h / float(ch))
lw = int(cw * scale_fg)
lh = int(ch * scale_fg)
logo_resized = content.resize((lw, lh), Image.Resampling.LANCZOS)

lx = 35 + (375 - lw) // 2
ly = (H - lh) // 2
fg.paste(logo_resized, (lx, ly), logo_resized)

# Right side typography
font_pill = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 13)
font_brand = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 34)
font_ar_brand = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 23)
font_sub = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 14)
font_footer = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 13)

rx = 455

# Badge Pill
badge_text = "LE CARNET INTELLIGENT MAROCAIN"
draw.rounded_rectangle([rx, 46, rx + 285, 78], radius=16, fill=(244, 237, 226, 255), outline=(215, 200, 185, 255), width=1)
draw.text((rx + 18, 52), badge_text, font=font_pill, fill=(180, 83, 60, 255))

# French Title
draw.text((rx, 96), "Gérez vos calculs & dépenses", font=font_brand, fill=(30, 41, 59, 255))
draw.text((rx, 140), "simplement comme sur du papier", font=font_brand, fill=(180, 83, 60, 255))

# Arabic Tagline
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
    draw.ellipse([px + 14, py + 16, px + 24, py + 26], fill=col)
    draw.text((px + 32, py + 11), text, font=font_sub, fill=(30, 41, 59, 255))

draw.text((rx, 395), "Sans connexion requise  •  Sauvegarde locale & PDF  •  Conçu pour le Maroc", font=font_footer, fill=(140, 150, 165, 255))

rgb_fg = fg.convert("RGB")
rgb_fg.save(os.path.join(ARTIFACT_DIR, "playstore_feature_graphic_1024x500.png"), quality=98)
rgb_fg.save(os.path.join(LOCAL_DIR, "playstore_feature_graphic_1024x500.png"), quality=98)
rgb_fg.save(os.path.join(ASSETS_DIR, "playstore_feature_graphic_1024x500.png"), quality=98)
print("Updated playstore_feature_graphic_1024x500.png")

print("\nAll logo assets updated successfully!")
