# mn git apk compiler

A Kotlin + Jetpack Compose Android app for importing GitHub repositories, plus a GitHub Actions workflow that can build a selected Android Gradle project and publish its APK as a downloadable workflow artifact.

## Current capabilities

- Dark premium Compose UI.
- Accepts `https://github.com/owner/repository` URLs.
- Optional GitHub Personal Access Token for private repositories.
- Downloads the repository ZIP from the GitHub API and extracts it into the app's private files directory.
- Rejects archive paths that attempt to escape the extraction directory.
- Includes a manual GitHub Actions workflow for building a target Android project.

## Important architecture note

The Android app currently downloads and extracts source code locally. It does **not** execute a complete Android SDK/Gradle build on the phone. A full Android build environment is not normally available inside an ordinary Android app sandbox. The included GitHub Actions workflow performs compilation on a GitHub-hosted runner instead.

The workflow is manually started from the **Actions** tab of the repository containing this project. It checks out the selected target repository, runs its Gradle wrapper task, and uploads the resulting APK as an artifact. It is not yet triggered directly by the Android app.

## Build the Android app

1. Open this folder in Android Studio.
2. Use JDK 17.
3. Allow Gradle sync to finish.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, compile SDK 35 and minimum SDK 26. Android Studio may prompt you to install the Android 35 SDK.

## Run the GitHub Actions APK workflow

1. Push this repository to GitHub.
2. Open **Settings > Secrets and variables > Actions**.
3. For private target repositories, add a repository secret named `GH_PAT`. Use a fine-grained token with read-only **Contents** access to the target repository. For public target repositories, the default workflow token is usually sufficient.
4. Open **Actions > Build APK from GitHub repository > Run workflow**.
5. Enter the target repository as `owner/name`, optionally specify a branch/tag/commit, and leave the task as `assembleDebug` unless the target project needs another task.
6. When the workflow finishes, download the APK from the run's **Artifacts** section.

### Target repository requirements

- It must be an Android Gradle project.
- It should include a Gradle wrapper (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`, and wrapper properties).
- The selected Gradle task must produce APK files under `build/outputs/apk/`.

### Security warning

Building a repository executes its Gradle build scripts and potentially other project code. Only build repositories you trust. Do not use a personal token with broad account permissions. Never print tokens in logs or commit them to source control.

## Local repository download behavior

Downloaded files are stored in the app's private internal storage under `files/repositories/`. They are not automatically visible in the public Downloads folder. The token is held in UI memory only and is not deliberately persisted by this app. Android may retain process memory temporarily; clear the field after use if sharing the device.
