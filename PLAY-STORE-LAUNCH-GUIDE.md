# TradeSketch Estimator — Play Store Launch Guide (Windows)

**Your email:** built.to.cell@gmail.com
**App name:** TradeSketch Estimator
**Package:** com.tradesketch.estimator
**Pricing:** Paid (one-time purchase)
**Last updated:** February 10, 2026

---

## HOW TO USE THIS GUIDE

Most of the technical work is automated by PowerShell scripts in the `scripts\` folder.
Open PowerShell, `cd` into the project folder, and run:

```powershell
.\scripts\LAUNCH.ps1
```

That gives you a menu to run each step. This document explains the **why** and the
**manual Play Console steps** that can't be scripted.

---

## TABLE OF CONTENTS

| Phase | What | Automated? |
|-------|------|-----------|
| 1 | [Google Play Developer Account](#phase-1--google-play-developer-account) | No (browser) |
| 2 | [Install Prerequisites](#phase-2--install-prerequisites) | **Verified by script** |
| 3 | [Generate Signing Keystore](#phase-3--generate-signing-keystore) | **Yes** |
| 4 | [Build the Release Bundle](#phase-4--build-the-release-bundle) | **Yes** |
| 5 | [Test the Release Build](#phase-5--test-the-release-build) | Partially |
| 6 | [Host Privacy Policy](#phase-6--host-privacy-policy) | **Yes** |
| 7 | [Create Store Graphics](#phase-7--create-store-graphics) | No (design work) |
| 8 | [Create App in Play Console](#phase-8--create-app-in-play-console) | No (browser) |
| 9 | [Fill Out Store Listing](#phase-9--fill-out-store-listing) | **Clipboard helper** |
| 10 | [Content Rating](#phase-10--content-rating-questionnaire) | No (browser) |
| 11 | [Pricing & Distribution](#phase-11--pricing-and-distribution) | No (browser) |
| 12 | [Data Safety Form](#phase-12--data-safety-form) | No (browser) |
| 13 | [Upload the Bundle](#phase-13--upload-the-bundle) | No (browser) |
| 14 | [Submit for Review](#phase-14--submit-for-review) | No (browser) |
| 15 | [After Approval](#phase-15--after-approval) | **Version bump script** |

---

## PHASE 1 — Google Play Developer Account

This is a one-time setup. You pay $25 and Google verifies your identity.

### Step 1.1 — Register

1. Open your browser and go to: **https://play.google.com/console/signup**
2. Sign in with: **built.to.cell@gmail.com**
3. Click **"Get started"**
4. Pay the **$25 USD** one-time registration fee with a credit/debit card

### Step 1.2 — Fill Out Your Developer Profile

| Field | What to Enter |
|-------|---------------|
| **Developer name** | The name buyers see on the Play Store. Pick carefully. Examples: your real name, "Built To Cell", or a business name. |
| **Contact email** | `built.to.cell@gmail.com` |
| **Phone number** | Your phone (not shown publicly — verification only) |
| **Website** | Leave blank or enter your site |

### Step 1.3 — Identity Verification

Google will email you at **built.to.cell@gmail.com**:

1. Click the verification link in the email
2. You may need to verify your phone via SMS
3. You may need to upload a photo ID (driver's license or passport)
4. Wait **24-48 hours** for Google to approve

### Step 1.4 — Merchant Account (Required for Paid Apps)

Since TradeSketch Estimator is a **paid app**, Google will prompt you to set up a
**Google Payments merchant account** so they can pay you. You'll need:

- Your legal name
- Your address
- Tax information (SSN or EIN if US-based)
- A bank account for receiving payments

Follow the prompts inside the Play Console. This must be done before you can publish
a paid app.

**Do not continue to Phase 8 until your account is fully verified.**

---

## PHASE 2 — Install Prerequisites

### The Script Way (recommended)

Open PowerShell and run:

```powershell
cd C:\path\to\tool
.\scripts\01-check-prerequisites.ps1
```

This checks everything automatically and tells you exactly what's missing. If
something fails, follow the instructions below.

### Manual Installation (if the script says something is missing)

#### Java 17

1. Go to **https://adoptium.net/**
2. Click **"Latest LTS Release"** — download **Temurin 17** for **Windows x64** (.msi installer)
3. Run the installer
4. **IMPORTANT:** On the "Custom Setup" screen, make sure **"Set JAVA_HOME variable"** is checked. If you miss this, you'll have to set it manually later.
5. Click through to finish

**Verify it worked** — open a NEW PowerShell window and type:
```powershell
java -version
```
You should see `openjdk version "17.0.something"`.

If you see an older version or an error, the PATH wasn't set. Fix it:
```powershell
# Find where Java was installed (usually here):
dir "C:\Program Files\Eclipse Adoptium\"

# Then set it permanently:
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot", "User")
```
Restart PowerShell after setting the variable.

#### Android Studio & SDK

1. Go to **https://developer.android.com/studio**
2. Click **"Download Android Studio"**
3. Run the `.exe` installer — accept all defaults
4. On first launch, Android Studio downloads the Android SDK automatically (this takes 5-10 minutes)
5. Once it finishes, go to **File > Settings > Languages & Frameworks > Android SDK**
6. **SDK Platforms tab:** Make sure **Android 15 (API 35)** is checked. If not, check it and click **Apply**.
7. **SDK Tools tab:** Make sure these are checked:
   - Android SDK Build-Tools (latest version)
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools (latest version)
8. Click **Apply** and wait for downloads

**Set ANDROID_HOME** (if the check script said it's missing):

```powershell
# The default SDK location on Windows:
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
```
Restart PowerShell.

#### Git

1. Go to **https://git-scm.com/download/win**
2. Download and run the installer
3. Accept all defaults (the installer has many screens — just keep clicking Next)
4. Verify: `git --version`

#### Clone the Project

```powershell
cd C:\Users\YourName\Documents
git clone https://github.com/discover-Austin/tool.git
cd tool
git checkout claude/prepare-play-store-launch-ZTY7K
```

#### Open in Android Studio

1. Open Android Studio
2. **File > Open**
3. Navigate to the `tool` folder
4. Click **OK**
5. Wait for Gradle to sync (2-5 minutes first time — it downloads all dependencies)
6. If prompted to update Gradle or AGP, click **Update**

---

## PHASE 3 — Generate Signing Keystore

### The Script Way

```powershell
.\scripts\02-generate-keystore.ps1
```

This script:
- Asks for a password (hidden input)
- Asks for your name, city, state, country
- Generates the keystore file
- Creates a backup on your Desktop with a credentials reminder file
- Writes the signing config into `local.properties` (gitignored)
- Adds safety entries to `.gitignore`

**That's it. You're done with signing setup.**

### What Just Happened (so you understand)

Every Android app needs a cryptographic key to prove you are you. The keystore file
(`tradesketch-release.keystore`) contains this key. When you upload your app to
Google Play, Google checks that future updates are signed by the same key.

**If you lose this keystore, you can never update your app.** You'd have to
publish a brand new app with a new package name. That's why the script backs it
up to your Desktop. After you've confirmed the backup, move it to:
- A USB drive in a safe place
- Your Google Drive or Dropbox
- A password manager (like 1Password or Bitwarden)

---

## PHASE 4 — Build the Release Bundle

### The Script Way

```powershell
.\scripts\03-build-release.ps1
```

This script:
1. Verifies your signing config exists
2. Runs all 16 unit tests
3. Runs Android lint checks
4. Builds the release App Bundle (.aab) with R8 code shrinking
5. Shows you the file size and path
6. Optionally builds a debug APK and installs it on your phone

The output file is at:
```
app\build\outputs\bundle\release\app-release.aab
```

This is the file you upload to Google Play Console in Phase 13.

### If the Build Fails

| Error | Fix |
|-------|-----|
| `JAVA_HOME is not set` | Reinstall JDK 17 with "Set JAVA_HOME" checked, or set manually (see Phase 2) |
| `SDK location not found` | Set ANDROID_HOME (see Phase 2) |
| `Keystore was tampered with` | Wrong password. Check `local.properties` or your Desktop backup. |
| `Could not resolve dependencies` | Check internet. Run `.\gradlew.bat clean --refresh-dependencies` |
| Tests fail | Fix the test, then rebuild. Test report: `app\build\reports\tests\testDebugUnitTest\index.html` |

---

## PHASE 5 — Test the Release Build

The build script offers to install on your phone. If you skipped that, you can
install manually:

```powershell
# Connect your phone via USB (USB Debugging must be enabled)
# Then:
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Test Checklist — Go Through Every Item

Open the app and check:

- [ ] App launches without crashing
- [ ] Projects screen shows templates (Bedroom, Garage, Driveway, Yard Bed)
- [ ] Tapping a template creates a project
- [ ] Project detail shows spaces with correct dimensions
- [ ] Editing a space: can change dimensions, add doors/windows, save
- [ ] Drywall takeoff: shows sheets, screws, joint compound
- [ ] Concrete takeoff: shows cubic yards
- [ ] Paint takeoff: shows gallons
- [ ] Gravel/Mulch takeoff: shows cubic yards and tons
- [ ] Changing waste % updates the totals
- [ ] PDF export: tap export, choose location, PDF file is created
- [ ] CSV export: same as above but CSV
- [ ] Share button: shows Android share sheet
- [ ] Settings screen works
- [ ] App works in airplane mode (100% offline)
- [ ] Screen rotation doesn't crash or lose data
- [ ] Back button navigates correctly
- [ ] Deleting a project removes it from the list

---

## PHASE 6 — Host Privacy Policy

Google Play requires a live, publicly accessible URL for your privacy policy.

### The Script Way

```powershell
.\scripts\05-deploy-privacy-policy.ps1
```

This script:
1. Installs the GitHub CLI if missing (via `winget`)
2. Logs you into GitHub (opens a browser)
3. Creates a public repo called `tradesketch-privacy`
4. Pushes `privacy-policy.html` as `index.html`
5. Enables GitHub Pages
6. Waits for the page to go live
7. Saves the URL to `store-assets\PRIVACY_POLICY_URL.txt`

Your privacy policy will be at:
```
https://YOUR-GITHUB-USERNAME.github.io/tradesketch-privacy/
```

**Save this URL.** You'll paste it into Play Console in Phases 9 and 12.

---

## PHASE 7 — Create Store Graphics

This is the one part you must do by hand. You need three things:

### 7.1 — App Icon (512 x 512 pixels)

**Easiest free method — Figma:**

1. Go to **https://figma.com/** — create a free account
2. Create a new design file
3. Press **F** (Frame tool), set width: 512, height: 512
4. Select the frame. Under **Fill**, set color: `#0D47A1` (dark blue)
5. Find a free house/building outline icon:
   - https://fonts.google.com/icons — search "house" or "construction"
   - Download the SVG, drag it into Figma
6. Make the icon white (`#FFFFFF`), center it, leave 50px padding on all sides
7. **File > Export** — choose PNG, 1x scale
8. Save as `ic_launcher_512.png`

**Then generate all Android icon sizes:**
1. In Android Studio, right-click the `res` folder
2. **New > Image Asset**
3. Choose **Launcher Icons (Adaptive and Legacy)**
4. Select your 512x512 PNG
5. Set background color to `#0D47A1`
6. Click **Next > Finish**

### 7.2 — Feature Graphic (1024 x 500 pixels)

**Easiest — Canva:**

1. Go to **https://canva.com/** — free account
2. **Create a design** > **Custom size** > 1024 x 500
3. Set background: blue gradient (`#0D47A1` to `#1976D2`)
4. Add text: "TradeSketch Estimator" (large, bold, white)
5. Below it: "Fast, accurate material takeoffs — offline." (smaller, light blue)
6. Optionally add your icon on the left side
7. **Download** as PNG
8. Save as `feature_graphic_1024x500.png`

### 7.3 — Screenshots (6 needed, 1080 x 2400 pixels)

**The Script Way:**

```powershell
.\scripts\04-capture-screenshots.ps1
```

This script:
1. Connects to your phone/emulator via ADB
2. Puts the status bar into "demo mode" (clean clock at 9:41, full battery, no notifications)
3. Walks you through each of the 6 screens
4. You navigate on your phone, press Enter, and it captures
5. Pulls all screenshots to `store-assets\screenshots\`
6. Exits demo mode

**The 6 screens to capture:**

| # | Screen | What to Show |
|---|--------|-------------|
| 1 | Projects list | Template cards visible |
| 2 | Project detail | Spaces listed with dimensions |
| 3 | Space editor | Dimension inputs, openings, area preview |
| 4 | Drywall takeoff | Sheets, screws, compound results |
| 5 | Concrete takeoff | Cubic yards result |
| 6 | Export screen | PDF/CSV/Share buttons, estimate preview |

**Optional: Add text overlays** using Canva or Figma:
- Screenshot 1: "Start from templates or scratch"
- Screenshot 2: "Model any space with precision"
- Screenshot 3: "Doors & windows auto-calculated"
- Screenshot 4: "Instant drywall takeoffs"
- Screenshot 5: "Concrete? Covered."
- Screenshot 6: "Export as PDF, CSV, or share"

---

## PHASE 8 — Create App in Play Console

1. Go to **https://play.google.com/console/**
2. Sign in with **built.to.cell@gmail.com**
3. Click the blue **"Create app"** button (top right)
4. Fill in:

| Field | Value |
|-------|-------|
| **App name** | `TradeSketch Estimator` |
| **Default language** | English (United States) – en-US |
| **App or Game** | App |
| **Free or Paid** | **Paid** |

5. Check both declaration boxes at the bottom
6. Click **"Create app"**

---

## PHASE 9 — Fill Out Store Listing

### The Script Way (copies each field to your clipboard)

```powershell
.\scripts\06-copy-store-listing.ps1
```

This script reads each piece of text from the `store-assets\listing\` files and
copies them to your clipboard one at a time. You just paste into Play Console.

### Manual Reference (what goes where)

In Play Console: **Grow > Store presence > Main store listing**

**App name:**
```
TradeSketch Estimator
```

**Short description** (80 chars max):
```
Model spaces and get drywall & concrete takeoffs—offline.
```

**Full description** — copy from `store-assets\listing\full-description.txt`

**Graphics:**
- App icon: upload `ic_launcher_512.png`
- Feature graphic: upload `feature_graphic_1024x500.png`
- Phone screenshots: upload all 6 from `store-assets\screenshots\` in order

**Contact details:**

| Field | Value |
|-------|-------|
| **Email** | `built.to.cell@gmail.com` |
| **Phone** | Optional |
| **Website** | Optional |

Click **"Save"**.

---

## PHASE 10 — Content Rating Questionnaire

1. **Policy > App content > Content rating**
2. Click **"Start questionnaire"**
3. Email: `built.to.cell@gmail.com` (enter twice to confirm)
4. Category: **"Utility, Productivity, Communication, or Other"**
5. Click **Next**
6. Answer **every single question "No"**:

| Question | Answer |
|----------|--------|
| Violence? | **No** |
| Sexual content? | **No** |
| Profanity or crude humor? | **No** |
| Drugs, alcohol, or tobacco? | **No** |
| Simulated gambling? | **No** |
| Real-money gambling? | **No** |
| Users communicate with each other? | **No** |
| Shares user location? | **No** |
| Users share personal info? | **No** |
| Shares info with third parties? | **No** |
| Discriminatory content? | **No** |
| Social issues, news, or politics? | **No** |
| Unrestricted web access? | **No** |

7. Click **Save**, then **Next**
8. You'll see: ESRB Everyone (E), PEGI 3, USK 0
9. Click **"Submit"**

---

## PHASE 11 — Pricing and Distribution

### Set Your Price

1. **Monetize > Products > App pricing**
2. App should already be **Paid** (from Phase 8)
3. Click **"Set price"**
4. Enter your price (in USD):

| Price | Strategy |
|-------|----------|
| **$2.99** | Lower barrier, good for first traction |
| **$4.99** | Sweet spot for professional utility tools |
| **$6.99** | Premium positioning |

5. Click **"Apply to all countries"** — this auto-converts to local currencies
6. Review the prices for UK, EU, Canada, Australia
7. Click **"Save"**

### Select Countries

1. **Release > Production > Countries / regions**
2. Click **"Add countries / regions"**
3. Click **"Select all"** to distribute worldwide
4. Click **"Add"**, then confirm

---

## PHASE 12 — Data Safety Form

1. **Policy > App content > Data safety**
2. Click **"Start"**
3. **"Does your app collect or share any of the required user data types?"** — select **No**
4. Enter your privacy policy URL (the one from Phase 6):
   ```
   https://YOUR-GITHUB-USERNAME.github.io/tradesketch-privacy/
   ```
5. Click **"Save"**, then **"Submit"**

---

## PHASE 13 — Upload the Bundle

1. **Release > Production**
2. Click **"Create new release"**
3. Accept **Play App Signing** (required for new apps — click **Continue**)
4. Click **"Upload"** and select:
   ```
   app\build\outputs\bundle\release\app-release.aab
   ```
5. Wait for upload (30-60 seconds)
6. Verify it shows: Version code 1, Version name 1.0.0
7. **Release name:** `1.0.0`
8. **Release notes** (paste this):

```
Initial release of TradeSketch Estimator

Features:
- Model rooms, walls, slabs, and yard beds
- Calculate drywall, concrete, paint, and gravel/mulch quantities
- Use pre-built templates for common projects
- Export estimates as PDF or CSV
- 100% offline with no data collection
- No account required

This is the first public release. We welcome your feedback!
```

9. Click **"Review release"**
10. Check for errors (warnings are usually OK)

---

## PHASE 14 — Submit for Review

### Final Checklist

Verify all sections show green checkmarks in the left sidebar:

- [ ] Store listing (name, descriptions, graphics, contact)
- [ ] Content rating (questionnaire submitted)
- [ ] Pricing (price set)
- [ ] Data safety (form submitted)
- [ ] Target audience: select **"Not designed for children"** or **18+**
- [ ] News app: **"No"**
- [ ] Financial features: **"No"**

### Submit

1. Click **"Start rollout to Production"**
2. Click **"Rollout"** to confirm

### What Happens Next

- Review takes **3-7 days** (sometimes faster, sometimes up to 14 days)
- You'll get an email at **built.to.cell@gmail.com** when approved or rejected
- If rejected, the email explains why. Common fixes:

| Rejection Reason | Fix |
|-----------------|-----|
| Privacy policy URL doesn't load | Check your GitHub Pages is live |
| Screenshots show broken features | Retake with working app |
| App crashes on launch | Test release build more carefully |
| Misleading metadata | Review description for accuracy |

---

## PHASE 15 — After Approval

### Verify Your Listing

Your app will be at:
```
https://play.google.com/store/apps/details?id=com.tradesketch.estimator
```

(May take a few hours to appear in search results.)

### Monitor

In Play Console:
- **Quality > Android vitals** — crash reports
- **Quality > Reviews** — user feedback (respond promptly!)
- **Financial > Financial overview** — sales data

### Publishing Updates

When you want to release a new version:

```powershell
.\scripts\07-bump-version.ps1
```

This script:
1. Shows current version
2. Increments versionCode automatically
3. Lets you pick new versionName (patch, minor, or custom)
4. Updates `build.gradle.kts`
5. Rebuilds the release bundle
6. Opens the What's New file for you to edit

Then upload the new `.aab` in Play Console > Release > Production > Create new release.

---

## TROUBLESHOOTING

### PowerShell says "scripts cannot be run on this system"

This is the execution policy. Fix it:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```
Then try again.

### "java is not recognized as an internal or external command"

Java is not in your PATH.
1. Find where Java was installed: `dir "C:\Program Files\Eclipse Adoptium\"`
2. Set JAVA_HOME:
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot", "User")
```
3. Add to PATH:
```powershell
$currentPath = [System.Environment]::GetEnvironmentVariable("PATH", "User")
[System.Environment]::SetEnvironmentVariable("PATH", "$currentPath;%JAVA_HOME%\bin", "User")
```
4. **Restart PowerShell.**

### "keytool is not recognized"

keytool comes with Java. If Java works but keytool doesn't:
```powershell
# Find it:
dir "C:\Program Files\Eclipse Adoptium\" -Recurse -Filter "keytool.exe"
# It's usually at: C:\Program Files\Eclipse Adoptium\jdk-17...\bin\keytool.exe
# Make sure that bin folder is in your PATH
```

### "ANDROID_HOME is not set"

```powershell
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
```
Restart PowerShell.

### Build takes forever or freezes

Increase Gradle memory. Edit `gradle.properties` in the project root:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
```

### ADB says "no devices/emulators found"

For a real phone:
1. **Settings > About Phone** — tap "Build Number" 7 times to unlock Developer Options
2. **Settings > Developer Options** — turn on **USB Debugging**
3. Plug phone into PC with a USB cable
4. Tap **Allow** on the USB debugging prompt on your phone
5. Run: `& "$env:ANDROID_HOME\platform-tools\adb.exe" devices`

For an emulator:
1. Open Android Studio > **Device Manager** (phone icon in toolbar)
2. Click **Create Virtual Device** > **Pixel 7** > **API 35** > **Finish**
3. Click the play button to start it

### "Play Console shows 0 supported devices"

This shouldn't happen — the app has no hardware requirements. Check
`app\src\main\AndroidManifest.xml` for accidental `<uses-feature>` tags.

---

## QUICK REFERENCE

| Item | Value |
|------|-------|
| **App name** | TradeSketch Estimator |
| **Package** | com.tradesketch.estimator |
| **Version** | 1.0.0 (code 1) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 (Android 15) |
| **Email** | built.to.cell@gmail.com |
| **Pricing** | Paid (one-time purchase) |
| **Category** | Productivity |
| **Content rating** | Everyone (E) |
| **Key alias** | tradesketch-release |
| **Bundle** | `app\build\outputs\bundle\release\app-release.aab` |
| **Privacy policy** | `store-assets\legal\privacy-policy.html` |

## SCRIPTS QUICK REFERENCE

Run from PowerShell in the project root:

| Script | What It Does |
|--------|-------------|
| `.\scripts\LAUNCH.ps1` | Master menu — shows status, launches any script |
| `.\scripts\01-check-prerequisites.ps1` | Verifies Java, SDK, Git, Gradle |
| `.\scripts\02-generate-keystore.ps1` | Creates keystore, backs up, writes config |
| `.\scripts\03-build-release.ps1` | Runs tests, lint, builds signed .aab |
| `.\scripts\04-capture-screenshots.ps1` | Walks you through 6 screenshots via ADB |
| `.\scripts\05-deploy-privacy-policy.ps1` | Deploys privacy policy to GitHub Pages |
| `.\scripts\06-copy-store-listing.ps1` | Copies each listing field to clipboard |
| `.\scripts\07-bump-version.ps1` | Increments version for app updates |
