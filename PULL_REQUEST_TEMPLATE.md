## Bump compile/target SDK to 36 and version to 1.0.23

This PR updates the Android module to target the newer Android API level and increments the application version.

Changes:
- compileSdk: 35 → 36
- targetSdk: 35 → 36
- versionCode: 24 → 25
- versionName: "1.0.22" → "1.0.23"

Why:
- Keeps the app up-to-date with the latest Android SDK and prepares the code for upcoming OS behavior changes.

Verification steps:
- CI should run the Gradle build and unit tests for the PR.
- Locally, validate with:
  - ./gradlew clean assembleDebug
  - ./gradlew assembleRelease (requires local signing config)
  - ./gradlew test

If you want a different PR title, target branch, or more context in the body, tell me and I can update before I open the PR.