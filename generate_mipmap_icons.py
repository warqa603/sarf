from PIL import Image
import os

ARTIFACT_DIR = r"C:/Users/Joe/.gemini/antigravity/brain/95a362e7-d8a9-47db-bf0a-d05637d009e1"
RES_DIR = r"c:/Users/Joe/Desktop/sarf/app/src/main/res"

src_logo = Image.open(os.path.join(ARTIFACT_DIR, "official_logo_source.png")).convert("RGBA")
alpha = src_logo.split()[-1]
bbox = alpha.getbbox()
tight_logo = src_logo.crop(bbox)
tw, th = tight_logo.size

densities = {
    "mipmap-mdpi": (48, 108),
    "mipmap-hdpi": (72, 162),
    "mipmap-xhdpi": (96, 216),
    "mipmap-xxhdpi": (144, 324),
    "mipmap-xxxhdpi": (192, 432),
}

for folder, (icon_sz, fg_sz) in densities.items():
    folder_path = os.path.join(RES_DIR, folder)
    if not os.path.exists(folder_path):
        os.makedirs(folder_path, exist_ok=True)
        
    # 1. Standard full icon (icon_sz x icon_sz) with cream background
    bg_icon = Image.new("RGBA", (icon_sz, icon_sz), (251, 248, 243, 255))
    target_inner = int(icon_sz * 0.82)
    sc = min(target_inner / tw, target_inner / th)
    nw, nh = int(tw * sc), int(th * sc)
    rsz = tight_logo.resize((nw, nh), Image.Resampling.LANCZOS)
    bg_icon.paste(rsz, ((icon_sz - nw) // 2, (icon_sz - nh) // 2), rsz)
    
    # Save as webp & png
    bg_icon.save(os.path.join(folder_path, "ic_launcher.webp"))
    bg_icon.save(os.path.join(folder_path, "ic_launcher_round.webp"))
    
    # 2. Adaptive Foreground (fg_sz x fg_sz) with central safe area (66dp of 108dp = ~61%)
    fg_canvas = Image.new("RGBA", (fg_sz, fg_sz), (0, 0, 0, 0))
    fg_target = int(fg_sz * 0.62)
    sc_fg = min(fg_target / tw, fg_target / th)
    nw_fg, nh_fg = int(tw * sc_fg), int(th * sc_fg)
    rsz_fg = tight_logo.resize((nw_fg, nh_fg), Image.Resampling.LANCZOS)
    fg_canvas.paste(rsz_fg, ((fg_sz - nw_fg) // 2, (fg_sz - nh_fg) // 2), rsz_fg)
    fg_canvas.save(os.path.join(folder_path, "ic_launcher_foreground.webp"))

print("All Android launcher icons generated successfully!")
