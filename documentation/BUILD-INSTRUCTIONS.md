# Build Instructions

Updated: March 8, 2026

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
```

## Scripted Release Path

```powershell
./scripts/01-check-prerequisites.ps1
./scripts/02-generate-keystore.ps1
./scripts/03-build-release.ps1
```

Expected artifact:

- `app/build/outputs/bundle/release/app-release.aab`

## Signing Keys

Required values:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

