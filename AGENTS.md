# Repository Guidelines

## Project Structure & Module Organization
- Android app code lives in `app/src/main/java/com/tradesketch/estimator/`.
- Key packages:
  - `ui/screens`, `ui/components`, `ui/theme` for Compose UI.
  - `domain/model`, `domain/usecase`, `domain/calc` for business logic.
  - `data/local`, `data/repository` for persistence and data access.
  - `di` for Hilt modules, `utils` for shared helpers.
- Unit tests are in `app/src/test/java/...`.
- Release and store materials are in `documentation/`, `scripts/`, and `store-assets/`.

## Build, Test, and Development Commands
- `./gradlew.bat assembleDebug` - build debug APK.
- `./gradlew.bat testDebugUnitTest` - run JVM unit tests.
- `./gradlew.bat lint` - run Android lint checks.
- `./gradlew.bat bundleRelease` - build release AAB (`app/build/outputs/bundle/release/app-release.aab`).
- Windows helper flow:
  - `./scripts/01-check-prerequisites.ps1`
  - `./scripts/03-build-release.ps1`

## Coding Style & Naming Conventions
- Language: Kotlin + Jetpack Compose.
- Use 4-space indentation, clear small functions, and immutable data models where practical.
- Naming:
  - `PascalCase` for classes/composables (`ProjectDetailScreen`).
  - `camelCase` for functions/properties (`updatePricingParams`).
  - `UPPER_SNAKE_CASE` for constants.
- Keep domain types precise (`Millimeters`, `Money`) and avoid floating-point money logic.

## Testing Guidelines
- Frameworks: JUnit4 + Kotlin test (`app/build.gradle.kts`).
- Place tests beside domain/util concerns (example: `domain/calc/TakeoffCalculatorTest.kt`).
- Test files should end with `Test.kt`; test names should describe behavior (e.g., `calculateDrywall_withOpenings_appliesWaste`).
- Before PR: run `testDebugUnitTest` and `lint` at minimum.

## Commit & Pull Request Guidelines
- Follow existing history style: imperative, concise subjects (`Add ...`, `Fix ...`, `Prepare ...`).
- Prefer focused commits by area (UI, domain, docs, scripts).
- PRs should include:
  - What changed and why.
  - Test commands run + results.
  - Screenshots/video for UI changes.
  - Updated docs/assets when behavior or release flow changes.

## Security & Configuration Tips
- Never commit keystores, credentials, or private signing values.
- Release signing uses `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Keep `local.properties` machine-local and out of PRs.
