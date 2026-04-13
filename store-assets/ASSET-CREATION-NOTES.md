# Asset Creation Notes

## Frozen Release Snapshot Standard

Use the newest build for everything.

- Release upload artifact: `app/build/outputs/bundle/release/app-release.aab`
- Media capture artifact: `app/build/outputs/apk/sideload/app-sideload.apk`
- Current release identity: `1.0.22` (`versionCode = 24`)
- Play upload package: `com.tradesketch.estimator`
- Screenshot/video capture package: `com.tradesketch.estimator.local`
- Device baseline for the current media pass: `1440 x 3120`
- Do not mix screenshots or showcase video from older builds.
- When the version changes, refresh all 8 screenshots and the 30-second showcase together.

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
**Count:** Up to 8 supported on Google Play phone listings

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

# Launch the newest signed sideload capture build
adb shell am start -n com.tradesketch.estimator.local/com.tradesketch.estimator.MainActivity

# Navigate to desired screen in app

# Capture
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png 01_welcome.png
```

**Option B: Emulator**
- Use Pixel 5 or similar (1080×2340)
- Extended controls > Screenshot
- Save with descriptive names

**Required Screens:**

1. **01_welcome.png** - Welcome home
   - TradeSketch header and first-run value proposition visible
   - Clean start state for a new user

2. **02_saved_projects.png** - Saved projects on home
   - Saved-project card visible on the home screen
   - Quick-return workflow readable for repeat jobs

3. **03_project_type.png** - Project type choice
   - Blueprint and direct-entry paths visible together
   - Shows both measured takeoff and fast manual entry

4. **04_blueprint_overview.png** - Blueprint overview
   - Active project with blueprint, overlays, and scale context visible
   - Real in-progress workspace, not an empty canvas

5. **05_blueprint_controls.png** - Blueprint editing controls
   - Bottom tool rail and precision editing controls visible
   - Shows active drafting and adjustment flow

6. **06_materials.png** - Materials and pricing
   - Linked quantities, pricing controls, and totals visible together
   - Emphasizes estimate clarity and trust

7. **07_export.png** - Export screen
   - Share/save actions for Estimate PDF, Blueprint PNG/PDF, CSV, and JSON
   - Preview deck and action card visible

8. **08_settings.png** - Settings and help
   - Preferences, reduced motion, and Help & Onboarding visible
   - Replay-tour or support actions visible

**Screenshot Enhancement (Optional):**

Add text overlays using image editor:
- "Quick templates"
- "Precise measurements"
- "Instant calculations"
- "Export anywhere"

Keep overlays minimal and professional.

### 4. Promo Video (Optional but Recommended)

**Duration:** 30 seconds for the Play preview master  
**Play Console Submission:** Upload the final video to YouTube, then add the preview video URL in Play Console  
**Local Working Format:** 1080 × 1920 MP4 is ideal for editing/review  

**Current Showcase Flow:**
1. Open an active project in the newest signed sideload build.
2. Show blueprint editing with measured arcs and sketch curves.
3. Move through trade-specific takeoff results.
4. End on the Export screen with PDF/PNG/CSV/JSON actions.
5. Render with subtitles and narration from `media/play_store_showcase/showcase_voiceover.txt` and `media/play_store_showcase/showcase_captions.srt`.

**Tools:**
- Screen recording: ADB, Android Studio, OBS
- Rendering: `scripts/render_play_store_showcase.ps1`
- Narration: `scripts/generate_play_store_voiceover.ps1`

## Asset Delivery Checklist

Before submission, verify:

- [ ] App icon is 512×512 PNG
- [ ] App icon has transparent background (or appropriate solid)
- [ ] App icon is recognizable at 48×48 (test by shrinking)
- [ ] Feature graphic is 1024×500
- [ ] Feature graphic text is readable at thumbnail size
- [ ] All 8 screenshots are captured from the newest signed sideload build
- [ ] Screenshots show real, working features (not mockups)
- [ ] Screenshots are same aspect ratio
- [ ] Screenshots are high resolution (≥1080px short side)
- [ ] No placeholder text in screenshots
- [ ] Status bar is clean in screenshots (demo mode)
- [ ] Screenshots demonstrate key features from the newest build only
- [ ] Preview video matches the same build as the screenshots
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
