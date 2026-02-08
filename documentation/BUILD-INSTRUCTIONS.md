# Build Instructions for TradeSketch Estimator

This document provides step-by-step instructions for building the TradeSketch Estimator Android app, including both debug and release builds.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [Building Debug Version](#building-debug-version)
4. [Building Release Version](#building-release-version)
5. [Generating Keystore](#generating-keystore)
6. [Signing Configuration](#signing-configuration)
7. [Testing the Build](#testing-the-build)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software

- **JDK 17 or later** - [Download from Adoptium](https://adoptium.net/)
- **Android Studio Hedgehog (2023.1.1) or later** - [Download](https://developer.android.com/studio)
- **Android SDK** with:
  - Android 14 (API 34) - for compilation
  - Android 8.0 (API 26) - minimum supported version
  - Build Tools 34.0.0 or later

### Verify Installation

```bash
# Check Java version
java -version  # Should show 17 or higher

# Check Android SDK
echo $ANDROID_HOME  # Should point to SDK directory

# Check Gradle wrapper
./gradlew --version
```

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/discover-Austin/tool.git
cd tool
```

### 2. Sync Gradle

```bash
./gradlew clean
```

This will download all dependencies. If you encounter network issues accessing
Google Maven repositories, ensure you have internet access and that no firewall
is blocking https://dl.google.com.

### 3. Open in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned repository
4. Wait for Gradle sync to complete

## Building Debug Version

Debug builds are unsigned and suitable for development and testing.

### Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Output location
ls app/build/outputs/apk/debug/app-debug.apk
```

### Android Studio

1. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. Wait for build to complete
3. Click "locate" in the notification to find the APK

### Install Debug APK

```bash
# Connect device or start emulator
adb devices

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Building Release Version

Release builds require signing with a keystore and are optimized with R8.

### Important Notes

- Never commit keystores to version control
- Keep keystore password secure
- Back up your keystore - if lost, you cannot update your app

## Generating Keystore

### First Time Setup

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD
```

Interactive prompts:
- First and last name: [Your name or company]
- Organizational unit: [Your team/department]
- Organization: [Your company]
- City/Locality: [Your city]
- State/Province: [Your state]
- Country code: [US, etc.]

The keystore file `release.keystore` will be created in your current directory.

**IMPORTANT:** Back this up securely! You'll need it for all future updates.

### Store Keystore Securely

```bash
# DO NOT commit to git
echo "*.keystore" >> .gitignore

# Move to secure location (optional)
mv release.keystore ~/.android/keystores/tradesketch-release.keystore
```

## Signing Configuration

### Option 1: Environment Variables (Recommended)

Set these before building:

```bash
export KEYSTORE_FILE="/path/to/release.keystore"
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="release-key"
export KEY_PASSWORD="your_key_password"
```

### Option 2: Local Properties File

Create `local.properties` in project root:

```properties
# Never commit this file!
keystore.file=/path/to/release.keystore
keystore.password=your_keystore_password
key.alias=release-key
key.password=your_key_password
```

Update `app/build.gradle.kts`:

```kotlin
android {
    // ... other config ...
    
    signingConfigs {
        create("release") {
            val keystoreFile = project.findProperty("keystore.file") as String?
            val keystorePassword = project.findProperty("keystore.password") as String?
            val keyAlias = project.findProperty("key.alias") as String?
            val keyPassword = project.findProperty("key.password") as String?
            
            if (keystoreFile != null && File(keystoreFile).exists()) {
                storeFile = File(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                throw GradleException("Keystore not configured. See BUILD-INSTRUCTIONS.md")
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## Building Release AAB

### Command Line

```bash
# Ensure signing is configured
./gradlew bundleRelease

# Output location
ls app/build/outputs/bundle/release/app-release.aab
```

### Android Studio

1. Select **Build > Generate Signed Bundle / APK**
2. Choose **Android App Bundle**
3. Select or create keystore
4. Enter passwords
5. Select **release** build variant
6. Click **Finish**

### Verify the Bundle

```bash
# Install bundletool
# Download from https://github.com/google/bundletool/releases

# Build APKs from bundle
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app-release.apks \
  --mode=universal

# Extract universal APK
unzip app-release.apks -d extracted/

# Install on connected device
bundletool install-apks --apks=app-release.apks
```

## Testing the Build

### Automated Tests

```bash
# Run all unit tests
./gradlew test

# View results
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Manual Testing Checklist

After installing a build:

- [ ] App launches without crashes
- [ ] Templates create projects correctly
- [ ] Space editor accepts valid inputs
- [ ] Takeoff calculations produce expected results
- [ ] PDF export works (grants SAF permission)
- [ ] CSV export works
- [ ] Share functionality works
- [ ] Settings screen accessible
- [ ] Privacy policy displays correctly
- [ ] App works in offline mode (airplane mode)
- [ ] No permission dialogs for sensitive data
- [ ] TalkBack navigation works

### Lint Checks

```bash
# Run lint
./gradlew lint

# View report
open app/build/reports/lint-results-debug.html
```

Target: **0 errors**. Warnings should be reviewed and resolved if relevant.

## Troubleshooting

### Build Fails: "Plugin not found"

**Symptom:** Gradle cannot resolve Android Gradle Plugin

**Solution:**
- Ensure you have internet access
- Check that Google Maven repository is accessible
- Try: `./gradlew clean --refresh-dependencies`
- Verify `settings.gradle.kts` has `google()` repository

### Build Fails: "ANDROID_HOME not set"

**Solution:**
```bash
export ANDROID_HOME=/path/to/android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
```

Add to `~/.bashrc` or `~/.zshrc` for persistence.

### Build Fails: "Keystore not found"

**Solution:**
- Verify keystore path in local.properties or environment variables
- Ensure keystore file exists and is readable
- Check passwords are correct

### R8 Errors in Release Build

**Solution:**
- Review ProGuard rules in `app/proguard-rules.pro`
- Add keep rules for classes that R8 incorrectly removes
- Test with `--stacktrace` flag: `./gradlew bundleRelease --stacktrace`

### Out of Memory During Build

**Solution:**
```bash
# Edit gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### Dependency Resolution Timeout

**Solution:**
```bash
# Increase timeout in gradle.properties
systemProp.org.gradle.internal.http.connectionTimeout=120000
systemProp.org.gradle.internal.http.socketTimeout=120000
```

## Build Variants

The project supports multiple build variants:

| Variant | Minified | Debuggable | Signing |
|---------|----------|------------|---------|
| debug   | No       | Yes        | Debug   |
| release | Yes      | No         | Release |

## Gradle Tasks Reference

```bash
# Clean build artifacts
./gradlew clean

# Compile app
./gradlew compileDebugKotlin
./gradlew compileReleaseKotlin

# Run tests
./gradlew test
./gradlew testDebugUnitTest

# Run lint
./gradlew lint

# Assemble APK
./gradlew assembleDebug
./gradlew assembleRelease

# Build App Bundle
./gradlew bundleDebug
./gradlew bundleRelease

# Install on device
./gradlew installDebug
./gradlew installRelease

# Check dependencies
./gradlew dependencies
```

## CI/CD Integration

For automated builds in CI:

```yaml
# Example GitHub Actions snippet
- name: Build Release AAB
  env:
    KEYSTORE_FILE: ${{ secrets.KEYSTORE_FILE }}
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: ./gradlew bundleRelease
```

Store keystore and passwords as secrets in your CI system.

## Additional Resources

- [Android Developer Guide](https://developer.android.com/studio/build)
- [App Bundle Format](https://developer.android.com/guide/app-bundle)
- [Signing Your App](https://developer.android.com/studio/publish/app-signing)
- [R8 Code Shrinking](https://developer.android.com/studio/build/shrink-code)

## Support

For build issues:
1. Check this document first
2. Review Gradle output carefully
3. Search [Stack Overflow](https://stackoverflow.com/questions/tagged/android-gradle)
4. Open an issue on GitHub with full error log

---

**Last Updated:** February 8, 2026
