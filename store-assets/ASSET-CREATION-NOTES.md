# Asset Creation Notes

## Graphics Assets Required

This file documents the graphics assets needed for Play Store submission and provides specifications for creating them.

### 1. App Icon (512×512)

**File:** `ic_launcher_512.png`  
**Dimensions:** 512 × 512 pixels  
**Format:** PNG with alpha channel (32-bit)  
**Color Space:** sRGB

**Design Concept:**
- Blueprint grid background (light blue lines on darker blue)
- Simple house/building outline in white/light color
- Geometric, flat design
- Recognizable at small sizes

**Color Palette:**
- Primary: #1976D2 (Material Blue 700)
- Accent: #00BCD4 (Material Cyan)
- Background: #0D47A1 (Material Blue 900)
- Foreground: #FFFFFF (White)

**Design Tools:**
- Figma (recommended, free for individuals)
- Adobe Illustrator
- Inkscape (free, open source)
- Android Asset Studio (online, free)

**Steps:**
1. Create 512×512 canvas
2. Fill background with #0D47A1
3. Add blueprint grid pattern (subtle, 20px spacing)
4. Center simple house icon (outline only)
5. Ensure 10% safe area around edges
6. Export as PNG-32

**Android Launcher Icons:**
Once you have the 512×512 master, generate adaptive icons:
- Use Android Studio: Right-click res > New > Image Asset
- Select "Launcher Icons (Adaptive and Legacy)"
- Choose your 512×512 PNG
- Generate all densities automatically

### 2. Feature Graphic (1024×500)

**File:** `feature_graphic_1024x500.png`  
**Dimensions:** 1024 × 500 pixels  
**Format:** PNG or JPEG  

**Design Layout:**
```
+------------------------------------------------------------+
|  [App Icon]  TradeSketch Estimator                        |
|              Material takeoffs, offline.                   |
|                                                            |
|  [Blueprint Background with Ruler/Measuring Elements]     |
+------------------------------------------------------------+
```

**Content:**
- App icon (left side, 200×200 px)
- App name in large, bold text
- Tagline below name
- Blueprint/construction themed background
- Simple, clean design

**Typography:**
- App name: 72pt, Bold, Roboto or similar sans-serif
- Tagline: 36pt, Regular, Roboto

**Background Ideas:**
- Faded blueprint lines
- Measuring tape graphic
- Geometric shapes representing walls/spaces
- Tools silhouettes (subtle, not busy)

### 3. Screenshots (Phone)

**Dimensions:** 1080 × 2400 pixels (or device native)  
**Format:** PNG (recommended) or JPEG  
**Count:** 6 required

**Current Repo Path:**

- `store-assets/screenshots/`

**Capture Method:**

**Option A: Real Device**
```bash
# Enable demo mode
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

# Navigate to desired screen in app

# Capture
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png 01_projects.png
```

**Option B: Emulator**
- Use Pixel 5 or similar (1080×2340)
- Extended controls > Screenshot
- Save with descriptive names

**Required Screens:**

1. **01_projects.png** - Projects list
   - Show template cards (Bedroom, Garage, Driveway, Yard Bed)
   - Empty state or 1-2 recent projects
   - Clean, uncluttered

2. **02_spaces.png** - Model/workspace tab
   - List of spaces with dimensions
   - "Add Space" button visible
   - Shows space types (Wall, Slab, etc.)

3. **03_editor.png** - Editing a space
   - Dimension input fields
   - Openings section (doors/windows)
   - Live area preview
   - Save/Cancel buttons

4. **04_drywall.png** - Drywall results
   - Preset selector showing "Drywall" selected
   - Parameter cards (waste%, sheet size)
   - Results card with totals
   - Item list (sheets, screws, compound)
   - Clear quantities and units

5. **05_concrete.png** - Concrete results
   - Preset selector showing "Concrete" selected
   - Parameters (thickness, waste%)
   - Cubic yards calculation
   - Optional bag equivalent

6. **06_export.png** - Export screen
   - Copy, Share, CSV, PDF buttons
   - Preview of summary text
   - Disclaimer visible

**Screenshot Enhancement (Optional):**

Add text overlays using image editor:
- "Quick templates"
- "Precise measurements"
- "Instant calculations"
- "Export anywhere"

Keep overlays minimal and professional.

### 4. Promo Video (Optional but Recommended)

**Duration:** 30-120 seconds  
**Format:** MP4 or WebM  
**Max Size:** 100 MB  

**Script:**
1. (0-5s) "Estimating materials for your next project?"
2. (5-15s) Show creating project from template
3. (15-25s) Show takeoff calculation
4. (25-30s) Show export to PDF
5. (30s) "TradeSketch Estimator - Available now"

**Tools:**
- Screen recording: ADB, Android Studio, OBS
- Editing: DaVinci Resolve (free), iMovie, Kdenlive
- Narration: Optional voiceover or text overlays

## Asset Delivery Checklist

Before submission, verify:

- [ ] App icon is 512×512 PNG
- [ ] App icon has transparent background (or appropriate solid)
- [ ] App icon is recognizable at 48×48 (test by shrinking)
- [ ] Feature graphic is 1024×500
- [ ] Feature graphic text is readable at thumbnail size
- [ ] All 6 screenshots are captured
- [ ] Screenshots show real, working features (not mockups)
- [ ] Screenshots are same aspect ratio
- [ ] Screenshots are high resolution (≥1080px short side)
- [ ] No placeholder text in screenshots
- [ ] Status bar is clean in screenshots (demo mode)
- [ ] Screenshots demonstrate key features
- [ ] All assets use consistent color scheme

## Brand Guidelines

**Colors:**
- Primary: Blue (#1976D2)
- Secondary: Teal (#00BCD4)
- Surface: Near-white (#FAFAFA)
- On-surface: Dark gray (#212121)

**Typography:**
- Primary: Roboto (Material Design standard)
- Alternative: Open Sans, Inter

**Imagery Style:**
- Flat design preferred
- Minimal gradients
- Professional, not playful
- Construction/blueprint theme
- Clean, uncluttered

**Tone:**
- Professional
- Confident
- Helpful
- Straightforward
- No jargon

## Quick Reference Tools

**Free Design Tools:**
- [Figma](https://figma.com) - Vector design
- [Photopea](https://www.photopea.com/) - Online Photoshop alternative
- [GIMP](https://www.gimp.org/) - Desktop image editor
- [Inkscape](https://inkscape.org/) - Vector graphics editor

**Icon Resources:**
- [Material Icons](https://fonts.google.com/icons) - Free icons
- [Flaticon](https://www.flaticon.com/) - Free with attribution
- [Material Design](https://material.io/design) - Guidelines

**Color Tools:**
- [Material Color Tool](https://material.io/resources/color/)
- [Coolors](https://coolors.co/) - Palette generator
- [Adobe Color](https://color.adobe.com/)

**Mockup Tools:**
- [Shots](https://shots.so/) - Device mockups
- [MockuPhone](https://mockuphone.com/) - Free device frames

## Validation

Test icons at multiple sizes:
```bash
# Generate all densities for testing
mkdir -p test-icons/{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}

# Use ImageMagick to resize
convert ic_launcher_512.png -resize 48x48 test-icons/mdpi/ic_launcher.png
convert ic_launcher_512.png -resize 72x72 test-icons/hdpi/ic_launcher.png
convert ic_launcher_512.png -resize 96x96 test-icons/xhdpi/ic_launcher.png
convert ic_launcher_512.png -resize 144x144 test-icons/xxhdpi/ic_launcher.png
convert ic_launcher_512.png -resize 192x192 test-icons/xxxhdpi/ic_launcher.png
```

View at actual size to ensure recognizability.

## Asset Approval Process

1. Create drafts
2. Review for consistency
3. Test at actual sizes
4. Get feedback from target users
5. Iterate if needed
6. Finalize and export

## Next Steps

Once assets are created:
1. Place in `store-assets/graphics/` directory
2. Place screenshots in `store-assets/screenshots/`
3. Verify dimensions with Play Console requirements
4. Upload to Play Console during submission

---

**Note:** Asset creation is the final step before submission. Take time to create professional, polished assets that represent your app well.
