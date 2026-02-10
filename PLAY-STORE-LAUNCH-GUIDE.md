# TradeSketch Estimator — Complete Play Store Launch Guide

**Your email:** built.to.cell@gmail.com
**App name:** TradeSketch Estimator
**Package:** com.tradesketch.estimator
**Pricing:** Paid (one-time purchase)
**Last updated:** February 10, 2026

This guide walks you through every single step to get TradeSketch Estimator published on the Google Play Store. Nothing is assumed. Every click, every field, every decision is spelled out.

---

## TABLE OF CONTENTS

1. [PHASE 1: Google Play Developer Account](#phase-1-google-play-developer-account)
2. [PHASE 2: Install Required Software](#phase-2-install-required-software)
3. [PHASE 3: Generate Your Signing Keystore](#phase-3-generate-your-signing-keystore)
4. [PHASE 4: Build the Release Bundle](#phase-4-build-the-release-bundle)
5. [PHASE 5: Test the Release Build](#phase-5-test-the-release-build)
6. [PHASE 6: Host Your Privacy Policy](#phase-6-host-your-privacy-policy)
7. [PHASE 7: Create Store Graphics](#phase-7-create-store-graphics)
8. [PHASE 8: Create the App in Play Console](#phase-8-create-the-app-in-play-console)
9. [PHASE 9: Fill Out the Store Listing](#phase-9-fill-out-the-store-listing)
10. [PHASE 10: Content Rating Questionnaire](#phase-10-content-rating-questionnaire)
11. [PHASE 11: Pricing and Distribution](#phase-11-pricing-and-distribution)
12. [PHASE 12: Data Safety Form](#phase-12-data-safety-form)
13. [PHASE 13: Upload the App Bundle](#phase-13-upload-the-app-bundle)
14. [PHASE 14: Submit for Review](#phase-14-submit-for-review)
15. [PHASE 15: After Approval](#phase-15-after-approval)
16. [TROUBLESHOOTING](#troubleshooting)

---

## PHASE 1: Google Play Developer Account

You need a Google Play Developer account. This is a one-time $25 USD registration fee.

### Step 1.1 — Go to the Play Console

1. Open your browser
2. Go to: https://play.google.com/console/signup
3. Sign in with: **built.to.cell@gmail.com**

### Step 1.2 — Pay the Registration Fee

1. You will see a page that says "Create a Google Play developer account"
2. Click **"Get started"**
3. You will be prompted to pay a **one-time $25 USD fee** via Google Pay
4. Enter your payment method (credit card, debit card, or linked bank)
5. Complete the payment
6. Wait for the confirmation email at built.to.cell@gmail.com

### Step 1.3 — Complete Your Developer Profile

After payment, you'll be taken to the developer profile setup. Fill in:

| Field | What to Enter |
|-------|---------------|
| **Developer name** | The name shown publicly on Play Store (e.g., your name, "Built To Cell", or a business name). Choose carefully — changing it later requires a review. |
| **Contact email** | `built.to.cell@gmail.com` |
| **Phone number** | Your personal phone number (not shown publicly, used for verification only) |
| **Website** | Leave blank for now, or enter your website if you have one |

### Step 1.4 — Identity Verification

Google requires identity verification for new developer accounts:

1. You will receive a verification email at built.to.cell@gmail.com
2. Click the verification link in the email
3. You may also be asked to verify your phone number via SMS code
4. If asked for ID verification (varies by region), follow the prompts to upload a government-issued photo ID
5. Verification can take **24-48 hours** — you cannot publish until it's complete

### Step 1.5 — Wait for Approval

- Check built.to.cell@gmail.com for a confirmation email
- Once approved, you can access the Play Console at: https://play.google.com/console/
- **Do not proceed to Phase 8 until your account is verified and active**

---

## PHASE 2: Install Required Software

You need these tools on your computer before you can build.

### Step 2.1 — Install Java Development Kit (JDK 17)

**On macOS:**
```bash
brew install openjdk@17
```
Then add to your shell profile (~/.zshrc or ~/.bashrc):
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```
Reload your terminal:
```bash
source ~/.zshrc
```

**On Windows:**
1. Go to https://adoptium.net/
2. Download "Temurin 17 LTS" for Windows x64
3. Run the installer
4. Check "Set JAVA_HOME variable" during installation
5. Restart your terminal

**On Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

**Verify installation:**
```bash
java -version
```
You should see something like: `openjdk version "17.0.x"`

### Step 2.2 — Install Android Studio

1. Go to https://developer.android.com/studio
2. Download the latest stable version (Ladybug or newer)
3. Run the installer and accept all defaults
4. On first launch, Android Studio will download the Android SDK automatically

**After installation, verify the SDK is set up:**
1. Open Android Studio
2. Go to **Settings** (or **Preferences** on macOS)
3. Navigate to **Languages & Frameworks > Android SDK**
4. Under **SDK Platforms** tab, check that **Android 15 (API 35)** is installed
   - If not, check the box next to it and click **Apply**
5. Under **SDK Tools** tab, ensure these are installed:
   - Android SDK Build-Tools (latest)
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools

### Step 2.3 — Set the ANDROID_HOME Environment Variable

**On macOS/Linux**, add to your shell profile:
```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"    # macOS
# OR
export ANDROID_HOME="$HOME/Android/Sdk"            # Linux

export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

**On Windows**, set via System Properties > Environment Variables:
```
ANDROID_HOME = C:\Users\<YourUsername>\AppData\Local\Android\Sdk
```

**Verify:**
```bash
echo $ANDROID_HOME    # Should print the SDK path
adb version           # Should print "Android Debug Bridge version..."
```

### Step 2.4 — Clone the Project (if you haven't already)

```bash
git clone https://github.com/discover-Austin/tool.git
cd tool
git checkout claude/prepare-play-store-launch-ZTY7K
```

### Step 2.5 — Open the Project in Android Studio

1. Open Android Studio
2. Click **File > Open**
3. Navigate to the `tool` folder you just cloned
4. Click **OK**
5. Wait for Gradle to sync (this may take 2-5 minutes the first time as it downloads all dependencies)
6. If you see a banner asking to update Gradle or AGP, click **Update** to accept

---

## PHASE 3: Generate Your Signing Keystore

Every Android app published to the Play Store must be signed with a cryptographic key. You will generate a keystore file that contains this key. **You must never lose this file or its passwords** — without them, you cannot publish updates to your app.

### Step 3.1 — Open a Terminal

- On macOS/Linux: open Terminal
- On Windows: open Command Prompt or PowerShell

Navigate to the project root:
```bash
cd /path/to/tool
```

### Step 3.2 — Generate the Keystore

Run this exact command, replacing the placeholder values with your real information:

```bash
keytool -genkey -v \
  -keystore tradesketch-release.keystore \
  -alias tradesketch-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

The tool will ask you a series of questions. Here's exactly what to enter:

```
Enter keystore password: [CHOOSE A STRONG PASSWORD — write it down!]
Re-enter new password: [TYPE THE SAME PASSWORD AGAIN]
What is your first and last name? [Your legal name, e.g., "Austin Smith"]
What is the name of your organizational unit? [Press Enter to skip, or enter "Development"]
What is the name of your organization? [Press Enter to skip, or enter your business name]
What is the name of your City or Locality? [Your city, e.g., "Austin"]
What is the name of your State or Province? [Your state, e.g., "Texas"]
What is the two-letter country code for this unit? [US]
Is CN=..., OU=..., O=..., L=..., ST=..., C=... correct? [yes]
```

This creates a file called `tradesketch-release.keystore` in your current directory.

### Step 3.3 — CRITICAL: Back Up the Keystore

**DO THIS RIGHT NOW. Do not skip this.**

1. Copy `tradesketch-release.keystore` to at least TWO safe locations:
   - A USB drive you store somewhere safe
   - A cloud storage folder (Google Drive, Dropbox, etc.)
   - An encrypted backup

2. Write down these four values and store them securely (password manager recommended):
   - **Keystore file location**: the full path to `tradesketch-release.keystore`
   - **Keystore password**: the password you just chose
   - **Key alias**: `tradesketch-release`
   - **Key password**: same as keystore password (or whatever you entered)

**WARNING:** If you lose this keystore, you can NEVER update your app on the Play Store. You would have to publish a completely new app with a new package name. Google cannot recover it for you.

### Step 3.4 — Configure the Build to Use the Keystore

**Option A — Environment variables (recommended for security):**

Add these to your shell profile (~/.zshrc, ~/.bashrc, etc.):
```bash
export KEYSTORE_FILE="/full/path/to/tradesketch-release.keystore"
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="tradesketch-release"
export KEY_PASSWORD="your_key_password"
```

Then reload:
```bash
source ~/.zshrc
```

**Option B — local.properties file (easier but less secure):**

Open (or create) the file `local.properties` in the project root and add:
```properties
KEYSTORE_FILE=/full/path/to/tradesketch-release.keystore
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=tradesketch-release
KEY_PASSWORD=your_key_password
```

**IMPORTANT:** The `local.properties` file is already in `.gitignore`, so it won't be committed to git. Double-check this by running:
```bash
grep "local.properties" .gitignore
```
You should see it listed. If not, add it:
```bash
echo "local.properties" >> .gitignore
```

---

## PHASE 4: Build the Release Bundle

Google Play requires an Android App Bundle (.aab), not an APK.

### Step 4.1 — Build via Command Line

From the project root directory:
```bash
./gradlew clean bundleRelease
```

This will:
1. Clean previous build artifacts
2. Compile all Kotlin code
3. Run R8 code shrinking and obfuscation
4. Sign the bundle with your keystore
5. Output the final .aab file

**Expected output location:**
```
app/build/outputs/bundle/release/app-release.aab
```

### Step 4.2 — Verify the Bundle Was Created

```bash
ls -lh app/build/outputs/bundle/release/app-release.aab
```

You should see a file roughly 3-8 MB in size.

### Step 4.3 — Alternative: Build via Android Studio

1. In Android Studio, go to **Build > Generate Signed Bundle / APK**
2. Select **Android App Bundle**
3. Click **Next**
4. For **Key store path**, click **Choose existing** and select your `tradesketch-release.keystore`
5. Enter your **Key store password**
6. For **Key alias**, select `tradesketch-release`
7. Enter your **Key password**
8. Click **Next**
9. Select **release** as the build variant
10. Click **Finish**
11. Wait for the build to complete — a notification will show the output location

---

## PHASE 5: Test the Release Build

Before uploading to Play Store, test the release build on a real device or emulator.

### Step 5.1 — Build a Universal APK from the Bundle

Install bundletool (one time):
```bash
# macOS
brew install bundletool

# Or download directly:
# https://github.com/google/bundletool/releases
# Place the .jar in your PATH
```

Generate a testable APK:
```bash
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app-release.apks \
  --mode=universal \
  --ks=tradesketch-release.keystore \
  --ks-pass=pass:YOUR_KEYSTORE_PASSWORD \
  --ks-key-alias=tradesketch-release \
  --key-pass=pass:YOUR_KEY_PASSWORD
```

Install on a connected device:
```bash
bundletool install-apks --apks=app-release.apks
```

### Step 5.2 — Run Through This Test Checklist

Open the app and test every single one of these:

- [ ] **App launches** — no crash on startup
- [ ] **Projects screen** — shows "No projects yet" or empty state
- [ ] **Create from template** — tap a template (Bedroom, Garage, etc.), project is created
- [ ] **Open a project** — spaces are listed with correct dimensions
- [ ] **Edit a space** — change dimensions, add an opening (door/window), save
- [ ] **Drywall takeoff** — select Drywall preset, verify sheets/screws/compound are calculated
- [ ] **Concrete takeoff** — select Concrete preset, verify cubic yards are calculated
- [ ] **Paint takeoff** — select Paint preset, verify gallons are calculated
- [ ] **Gravel/Mulch takeoff** — select Gravel preset, verify cubic yards and tons are calculated
- [ ] **Adjust waste %** — change waste percentage, verify totals update
- [ ] **Export as PDF** — tap PDF export, choose save location, verify PDF is created
- [ ] **Export as CSV** — tap CSV export, choose save location, verify CSV is created
- [ ] **Share** — tap Share, verify share sheet appears with estimate text
- [ ] **Settings screen** — accessible and all options work
- [ ] **Offline mode** — turn on airplane mode, verify app works perfectly
- [ ] **Rotate screen** — verify no crashes or data loss on rotation
- [ ] **Back button** — verify navigation works correctly
- [ ] **Delete a project** — verify it's removed from the list

### Step 5.3 — Test on Multiple Devices if Possible

Try to test on:
- A phone running Android 8 or 9 (the minimum supported)
- A phone running Android 14 or 15 (latest)
- A tablet (verify layout doesn't break)

If you only have one device, use Android Studio's emulator to test different configurations.

---

## PHASE 6: Host Your Privacy Policy

Google Play requires a publicly accessible privacy policy URL. Your privacy policy HTML file is already created at `store-assets/legal/privacy-policy.html`.

### Option A — GitHub Pages (Free, Recommended)

This is the easiest free option.

**Step 6.1 — Create a GitHub Pages repository**

1. Go to https://github.com/new
2. Create a new repository named: `tradesketch-privacy`
3. Set it to **Public**
4. Check "Add a README file"
5. Click **Create repository**

**Step 6.2 — Upload the privacy policy**

1. In the new repository, click **Add file > Upload files**
2. Upload the file: `store-assets/legal/privacy-policy.html`
3. Rename it to `index.html` (so it loads as the default page)
4. Click **Commit changes**

**Step 6.3 — Enable GitHub Pages**

1. Go to the repository **Settings**
2. Click **Pages** in the left sidebar
3. Under **Source**, select **Deploy from a branch**
4. Under **Branch**, select **main** and **/ (root)**
5. Click **Save**
6. Wait 1-2 minutes for deployment

**Step 6.4 — Get your privacy policy URL**

Your URL will be:
```
https://<your-github-username>.github.io/tradesketch-privacy/
```

For example: `https://discover-Austin.github.io/tradesketch-privacy/`

**Step 6.5 — Verify the URL works**

1. Open the URL in your browser
2. Confirm the privacy policy displays correctly
3. Check it works on mobile too

**Write down this URL — you'll need it in Phase 9 and Phase 12.**

### Option B — Google Sites (Free alternative)

1. Go to https://sites.google.com/new
2. Sign in with built.to.cell@gmail.com
3. Create a new site called "TradeSketch Privacy Policy"
4. Add a page
5. Paste the content from `privacy-policy.html` (the text, not the HTML code)
6. Publish the site
7. Copy the published URL

### Option C — Firebase Hosting (Free tier)

```bash
npm install -g firebase-tools
firebase login
firebase init hosting
# Select your project or create one
# Set public directory to "store-assets/legal"
# Single page app: No
firebase deploy
```

---

## PHASE 7: Create Store Graphics

You need three types of graphics. Here are the exact specifications and the easiest way to create each one.

### 7.1 — App Icon (512 x 512 px)

This is the icon shown on the Play Store listing page.

**Easiest method — Figma (free):**

1. Go to https://www.figma.com/ and create a free account
2. Create a new design file
3. Press **F** (Frame tool), then in the right panel set width: 512, height: 512
4. Select the frame. In the right panel under **Fill**, click the color and enter: `#0D47A1`
5. Press **R** (Rectangle tool), draw a small rectangle. Change its fill to `#1976D2` with 20% opacity. Duplicate it to create a subtle grid pattern (or skip the grid — a solid blue background is fine)
6. Search Google for "house outline SVG free" or use Material Icons (https://fonts.google.com/icons — search "house")
7. Import or draw a simple house/building outline in white (`#FFFFFF`) centered in the frame
8. Make sure there's at least 50px padding on all sides (the icon should not touch the edges)
9. Select the frame and go to **File > Export**, choose **PNG**, scale **1x**, click **Export**
10. Save as `ic_launcher_512.png`

**Even easier — use Android Studio's built-in icon generator (after you have a basic design):**
1. In Android Studio, right-click the `res` folder
2. Select **New > Image Asset**
3. Choose **Launcher Icons (Adaptive and Legacy)**
4. Select your 512x512 PNG as the foreground
5. Adjust background color to `#0D47A1`
6. Click **Next > Finish** — this generates ALL required icon sizes for the app

### 7.2 — Feature Graphic (1024 x 500 px)

This is the banner shown at the top of your Play Store listing.

**Using Figma:**

1. Create a new frame: 1024 x 500
2. Fill background with a gradient from `#0D47A1` to `#1976D2`
3. Add text:
   - "TradeSketch Estimator" — Roboto Bold, 64px, white
   - "Fast, accurate material takeoffs — offline." — Roboto Regular, 28px, `#B3E5FC`
4. Place your app icon (smaller, ~150x150) on the left side
5. Optionally add subtle construction-themed graphics (ruler lines, blueprint grid)
6. Export as PNG: `feature_graphic_1024x500.png`

**Using Canva (even easier):**

1. Go to https://www.canva.com/ (free account)
2. Click **Create a design** > **Custom size** > 1024 x 500 px
3. Choose a blue/professional template or start blank
4. Add your app name and tagline
5. Download as PNG

### 7.3 — Screenshots (1080 x 2400 px, minimum 4, recommended 6)

These show off your app in the Play Store listing.

**Step 7.3.1 — Set up a clean phone/emulator**

If using a real phone:
```bash
# Connect phone via USB, enable USB debugging in Developer Options
adb devices    # Verify it shows your device

# Enter demo mode (shows clean status bar — full battery, Wi-Fi, clean time)
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
```

If using Android Studio emulator:
1. Open **Device Manager** in Android Studio
2. Create a virtual device: **Pixel 7** (or similar)
3. Select system image: **API 35**
4. Launch the emulator
5. Install your release APK on it

**Step 7.3.2 — Capture the 6 required screenshots**

Open the app and navigate to each screen. For each screenshot:
- On real device: `adb shell screencap -p /sdcard/screenshot.png && adb pull /sdcard/screenshot.png <filename>.png`
- On emulator: Click the camera icon in the emulator toolbar, or press Ctrl+S (Cmd+S on macOS)
- On phone: Press Power + Volume Down simultaneously

Capture these 6 screens:

| # | Screen to Show | What Should Be Visible | Filename |
|---|---------------|----------------------|----------|
| 1 | **Projects list** | Template cards (Bedroom, Garage, Driveway, Yard Bed), clean empty state | `01_projects.png` |
| 2 | **Project detail — Spaces** | A project with 2-3 spaces listed showing dimensions | `02_spaces.png` |
| 3 | **Space editor** | Dimension input fields, openings section, live area preview | `03_editor.png` |
| 4 | **Drywall takeoff** | Drywall preset selected, results showing sheets + screws + compound | `04_drywall.png` |
| 5 | **Concrete takeoff** | Concrete preset selected, cubic yards result | `05_concrete.png` |
| 6 | **Export screen** | PDF/CSV/Share buttons, preview of estimate summary | `06_export.png` |

**Step 7.3.3 — Exit demo mode when done**

```bash
adb shell am broadcast -a com.android.systemui.demo -e command exit
```

**Step 7.3.4 — Optional: Add text overlays to screenshots**

Use Figma, Canva, or any image editor to add a short headline above or below each screenshot:

| Screenshot | Overlay Text |
|-----------|-------------|
| 01 | "Start from templates or scratch" |
| 02 | "Model any space with precision" |
| 03 | "Doors & windows auto-calculated" |
| 04 | "Instant drywall takeoffs" |
| 05 | "Concrete? Covered." |
| 06 | "Export as PDF, CSV, or share" |

---

## PHASE 8: Create the App in Play Console

Now you're ready to set up the app in the Google Play Console.

### Step 8.1 — Open Play Console

1. Go to https://play.google.com/console/
2. Sign in with **built.to.cell@gmail.com**

### Step 8.2 — Create a New App

1. Click the blue **"Create app"** button (top right)
2. Fill in the form:

| Field | Value |
|-------|-------|
| **App name** | `TradeSketch Estimator` |
| **Default language** | English (United States) – en-US |
| **App or Game** | Select **App** |
| **Free or Paid** | Select **Paid** |

3. Check all the declaration checkboxes at the bottom:
   - "I acknowledge that the app..." ✅
   - "I acknowledge that my app..." ✅
4. Click **"Create app"**

**IMPORTANT NOTE ABOUT PAID APPS:** To sell paid apps, you must also set up a Google Payments merchant account. The Play Console will prompt you to do this. Follow the steps — you'll need:
- Your legal name
- Your address
- Tax information (Social Security Number or EIN if US-based)
- A bank account for receiving payments

---

## PHASE 9: Fill Out the Store Listing

After creating the app, you'll be in the app's dashboard. There's a checklist on the left side. Let's go through each section.

### Step 9.1 — Main Store Listing

1. In the left menu, click **Grow > Store presence > Main store listing**
2. Fill in every field:

**App name:**
```
TradeSketch Estimator
```

**Short description** (max 80 characters):
```
Model spaces and get drywall & concrete takeoffs—offline.
```

**Full description** (copy this exactly):
```
TradeSketch Estimator: Fast, Accurate Material Takeoffs for Skilled Trades

Make professional material estimates for drywall, concrete, paint, and gravel/mulch projects—completely offline, with no account required.

FEATURES:
• Model spaces with simple measurements (rooms, walls, slabs, yard beds)
• Account for doors and windows automatically
• Choose from preset templates or start from scratch
• Instant calculations for drywall sheets, concrete yards, paint gallons, and more
• Customize waste percentages, sheet sizes, and material parameters
• Export estimates as PDF or CSV
• Share estimates via text or email
• No internet required—works anywhere

BUILT FOR:
• Contractors and estimators
• Skilled trades (drywall, concrete, painting)
• Remodelers and builders
• Serious DIY enthusiasts
• Anyone who needs fast, reliable quantity takeoffs

TEMPLATES INCLUDED:
• Bedroom (4 walls + ceiling with openings)
• Garage (concrete slab)
• Driveway (concrete slab)
• Yard bed (gravel/mulch)

ESTIMATES PROVIDED:
• Drywall: sheets, screws, joint compound
• Concrete: cubic yards (with optional bag equivalent)
• Gravel/Mulch: cubic yards and tons
• Paint: gallons based on coverage and coats

PRIVACY & SECURITY:
• 100% offline—no data collection
• No account or sign-in required
• No ads or tracking
• All data stays on your device
• Minimal permissions (export files only when you choose)

ACCURACY & DISCLAIMERS:
All estimates include industry-standard waste factors and are clearly labeled as "Estimate Only." Always verify quantities with actual site conditions, local building codes, and material suppliers before purchasing or starting work.

PERFECT FOR QUICK ESTIMATES:
Get a ballpark quantity in under 60 seconds using templates, or model complex spaces with precision. Whether you're bidding a job, planning a DIY project, or comparing quotes, TradeSketch Estimator gives you confidence in your numbers.

NO HIDDEN COSTS:
One-time purchase. No subscriptions. No in-app purchases. No surprises.

START ESTIMATING TODAY:
Download TradeSketch Estimator and take the guesswork out of material takeoffs.
```

### Step 9.2 — Upload Graphics

Scroll down to the graphics section:

1. **App icon**: Upload `ic_launcher_512.png` (512 x 512)
2. **Feature graphic**: Upload `feature_graphic_1024x500.png` (1024 x 500)
3. **Phone screenshots**: Upload all 6 screenshots in order (01 through 06)
   - Drag them in the correct order — the first screenshot is the most important
4. **Tablet screenshots**: Optional but recommended. If you don't have a tablet, skip this.

### Step 9.3 — Contact Details

Still on the store listing page, scroll to "Contact details":

| Field | Value |
|-------|-------|
| **Email** | `built.to.cell@gmail.com` |
| **Phone** | Optional — leave blank if you prefer |
| **Website** | Optional — leave blank or enter your site |

### Step 9.4 — Save

Click **"Save"** at the bottom right.

---

## PHASE 10: Content Rating Questionnaire

### Step 10.1 — Navigate to Content Rating

1. In the left menu, click **Policy > App content > Content rating**
2. Click **"Start questionnaire"**

### Step 10.2 — Select the Category

1. Email address: `built.to.cell@gmail.com`
2. Confirm email: `built.to.cell@gmail.com`
3. App category: Select **"Utility, Productivity, Communication, or Other"**
4. Click **Next**

### Step 10.3 — Answer Every Question "No"

You will see a series of yes/no questions. Answer every single one **"No"**:

| Question | Answer |
|----------|--------|
| Does your app contain violence? | **No** |
| Does your app contain sexual content? | **No** |
| Does your app contain profanity or crude humor? | **No** |
| Does your app reference illegal drugs, alcohol, or tobacco? | **No** |
| Does your app simulate gambling? | **No** |
| Does your app allow users to gamble with real money? | **No** |
| Can users communicate with each other? | **No** |
| Does your app share user location? | **No** |
| Can users share personal information? | **No** |
| Does your app share user info with third parties? | **No** |
| Does your app contain discriminatory content? | **No** |
| Does your app contain social issues, news, or politics? | **No** |
| Does your app allow unrestricted web access? | **No** |

### Step 10.4 — Submit and Confirm

1. Click **"Save"**, then **"Next"**
2. You'll see the rating results:
   - **ESRB**: Everyone (E)
   - **PEGI**: 3
   - **USK**: 0
3. Click **"Submit"**

---

## PHASE 11: Pricing and Distribution

### Step 11.1 — Set Up Pricing

1. In the left menu, click **Monetize > Products > App pricing**
   (Or: **Policy > App content > Pricing**)
2. Your app should already be set to **Paid** (from when you created it)
3. Click **"Set price"**
4. Enter your price. Recommendations for a professional utility app:
   - **$4.99 USD** — competitive sweet spot for productivity tools
   - **$2.99 USD** — lower barrier, good for initial traction
   - **$6.99 USD** — if you want to position as premium

5. Click **"Apply to all countries"** to auto-convert to local currencies
6. Review the converted prices for major markets (UK, EU, Canada, Australia)
7. Click **"Save"**

### Step 11.2 — Select Countries

1. In the left menu, click **Release > Production > Countries / regions**
2. Click **"Add countries / regions"**
3. Click **"Select all"** to distribute worldwide (recommended)
   - Or individually select countries if you prefer
4. Click **"Add countries / regions"**
5. Confirm by clicking **"Add"**

---

## PHASE 12: Data Safety Form

Google requires you to declare what data your app collects.

### Step 12.1 — Navigate to Data Safety

1. In the left menu, click **Policy > App content > Data safety**
2. Click **"Start"**

### Step 12.2 — Answer the Overview Questions

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **No** |

Since you answered "No", the form becomes very short.

### Step 12.3 — Privacy Policy URL

You will be asked for a privacy policy URL. Enter the URL from Phase 6:
```
https://<your-github-username>.github.io/tradesketch-privacy/
```

### Step 12.4 — Submit

1. Review your answers
2. Click **"Save"**
3. Click **"Submit"**

---

## PHASE 13: Upload the App Bundle

### Step 13.1 — Create a Production Release

1. In the left menu, click **Release > Production**
2. Click **"Create new release"**

### Step 13.2 — App Signing by Google Play

On first release, Google will ask you to opt into **Play App Signing**.

1. Click **"Continue"** to accept Play App Signing (this is required for new apps)
2. This means Google manages the actual signing key used for distribution, while your upload key (the keystore you created) is used to verify your identity

### Step 13.3 — Upload the Bundle

1. Click **"Upload"** (or drag and drop)
2. Select the file: `app/build/outputs/bundle/release/app-release.aab`
3. Wait for the upload to complete (may take 30-60 seconds)
4. You should see the bundle listed with:
   - Version code: 1
   - Version name: 1.0.0

### Step 13.4 — Release Name and Notes

| Field | Value |
|-------|-------|
| **Release name** | `1.0.0` |
| **Release notes** | Copy this: |

```
Initial release of TradeSketch Estimator

Features:
• Model rooms, walls, slabs, and yard beds
• Calculate drywall, concrete, paint, and gravel/mulch quantities
• Use pre-built templates for common projects
• Export estimates as PDF or CSV
• 100% offline with no data collection
• No account required

This is the first public release. We welcome your feedback!
```

### Step 13.5 — Review

1. Click **"Review release"**
2. Google will run automated checks on your bundle
3. You may see warnings (not errors) — these are usually informational
4. If there are errors, they will tell you exactly what to fix

---

## PHASE 14: Submit for Review

### Step 14.1 — Final Dashboard Check

Before submitting, verify all sections show a green checkmark (✅) in the left sidebar:

- [ ] ✅ Store listing (app name, descriptions, graphics, contact)
- [ ] ✅ Content rating (questionnaire submitted)
- [ ] ✅ Pricing (price set)
- [ ] ✅ Data safety (form submitted)
- [ ] ✅ Target audience and content (if prompted — select 18+ for simplicity, or "Not designed for children")
- [ ] ✅ News app (if prompted — select "No, my app is not a news app")
- [ ] ✅ COVID-19 contact tracing (if prompted — select "My app is not a COVID-19 contact tracing or status app")
- [ ] ✅ Government apps (if prompted — select "No")
- [ ] ✅ Financial features (if prompted — select "My app does not provide financial features")

### Step 14.2 — Submit

1. Click **"Start rollout to Production"**
2. Confirm by clicking **"Rollout"**

### Step 14.3 — Wait for Review

- First-time app reviews typically take **3 to 7 days**
- In some cases, it can be faster (24-48 hours) or slower (up to 14 days)
- You will receive an email at **built.to.cell@gmail.com** when:
  - Your app is **approved** (it goes live on Play Store)
  - Your app is **rejected** (with reasons and how to fix)

### Step 14.4 — If Your App Is Rejected

Common reasons for rejection and how to fix them:

| Reason | Fix |
|--------|-----|
| "Privacy policy does not match app behavior" | Verify your privacy policy URL loads correctly |
| "Screenshots show unavailable features" | Retake screenshots showing real, working features |
| "App crashes on launch" | Test your release build thoroughly (Phase 5) |
| "Metadata contains misleading claims" | Review your description for accuracy |
| "App does not provide sufficient value" | Ensure all features work, add more detail to description |

After fixing, upload a new bundle and resubmit.

---

## PHASE 15: After Approval

### Step 15.1 — Verify Your Listing

1. Search "TradeSketch Estimator" on Google Play (may take a few hours to index)
2. Or go directly to: `https://play.google.com/store/apps/details?id=com.tradesketch.estimator`
3. Verify everything looks correct

### Step 15.2 — Monitor Initial Feedback

1. In Play Console, check **Quality > Android vitals** for crash reports
2. Check **Quality > Reviews** for user feedback
3. Respond to reviews — especially negative ones — professionally and promptly

### Step 15.3 — First Update Checklist (for the future)

When you're ready to publish an update:

1. Make your code changes
2. In `app/build.gradle.kts`, increment:
   - `versionCode` by 1 (e.g., 1 → 2)
   - `versionName` as appropriate (e.g., "1.0.0" → "1.1.0")
3. Build: `./gradlew clean bundleRelease`
4. In Play Console: **Release > Production > Create new release**
5. Upload the new .aab
6. Add "What's new" notes
7. Submit

---

## TROUBLESHOOTING

### "Gradle sync failed" or "Could not resolve dependencies"

**Cause:** Network issue or missing SDK component.
**Fix:**
1. Check internet connection
2. In Android Studio: **File > Sync Project with Gradle Files**
3. If that fails: `./gradlew clean --refresh-dependencies`
4. Verify Android SDK 35 is installed (Android Studio > Settings > SDK Manager)

### "JAVA_HOME is not set" or "Unsupported class file major version"

**Cause:** Wrong Java version.
**Fix:**
```bash
java -version   # Must show 17 or higher
echo $JAVA_HOME  # Must point to JDK 17+
```

### "Keystore was tampered with, or password was incorrect"

**Cause:** Wrong password or corrupted keystore.
**Fix:**
1. Verify you're using the correct password (check your password manager)
2. Verify the keystore file path is correct
3. If truly lost, you must generate a new keystore (but this means you can't update an already-published app)

### "bundleRelease FAILED: No configured signing config"

**Cause:** The signing config wasn't found.
**Fix:**
1. Verify environment variables are set: `echo $KEYSTORE_FILE`
2. Or verify `local.properties` has the correct paths
3. Make sure the keystore file actually exists at the path specified

### "R8: Missing class" warnings during release build

**Cause:** Code shrinking removed something needed at runtime.
**Fix:** Add a keep rule in `app/proguard-rules.pro`:
```
-keep class the.missing.Class { *; }
```

### "App rejected: Privacy policy URL does not load"

**Cause:** The privacy policy page is not accessible.
**Fix:**
1. Open the URL in a browser and verify it loads
2. Make sure the GitHub Pages repository is public
3. Wait 5 minutes if you just deployed — GitHub Pages can be slow

### "Play Console shows 0 supported devices"

**Cause:** Usually a manifest issue.
**Fix:** This should not happen with this app since we have no hardware requirements. If it does, check `AndroidManifest.xml` for any accidental `<uses-feature>` tags.

---

## QUICK REFERENCE CARD

| Item | Value |
|------|-------|
| **App name** | TradeSketch Estimator |
| **Package name** | com.tradesketch.estimator |
| **Version** | 1.0.0 (versionCode 1) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 (Android 15) |
| **Contact email** | built.to.cell@gmail.com |
| **Pricing** | Paid (one-time purchase) |
| **Category** | Productivity |
| **Content rating** | Everyone (E) |
| **Key alias** | tradesketch-release |
| **Bundle output** | app/build/outputs/bundle/release/app-release.aab |
| **Privacy policy** | store-assets/legal/privacy-policy.html (host this) |

---

**You've got this. Follow each phase in order, check off each step, and your app will be live on the Play Store.**
