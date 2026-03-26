# Build Instructions

Updated: March 25, 2026

## Baseline

- `compileSdk = 35`
- `targetSdk = 35`
- `minSdk = 26`
- Release shrinking is enabled (`isMinifyEnabled = true`, `isShrinkResources = true`)

## PowerShell Commands

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
./gradlew.bat :app:assembleSideload
```

## Scripted Release Path

```powershell
./scripts/01-check-prerequisites.ps1
./scripts/02-generate-keystore.ps1
./scripts/03-build-release.ps1
./scripts/04-capture-screenshots.ps1
./scripts/generate_play_store_voiceover.ps1
./scripts/record_play_store_showcase.ps1
./scripts/render_play_store_showcase.ps1
```

Expected artifact:

- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/sideload/app-sideload.apk`

## Frozen Release Rule

- Build the upload bundle and capture build from the same commit.
- Capture screenshots and preview video from the newest signed sideload build only.
- When the version changes, replace all 8 screenshots and the 30-second showcase together.

## Signing Keys

Required values:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`


