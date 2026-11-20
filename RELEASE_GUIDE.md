# CONNECT Android — Local Testing & Release Build Guide

## 1. Project Identity & Firebase Project
- **App Name**: `CONNECT`
- **Application ID**: `com.connect.social`
- **Namespace**: `com.example`
- **Existing Firebase Project**: `gen-lang-client-0397961657` (`app/google-services.json`)
- **Named Firestore Database ID**: `ai-studio-android-connect-323c8a6f-24e8-43bf-97f9-eea3c5c9b54f` (`app/src/main/res/values/firebase_applet_config.xml`)
- **Required Manifest Permissions**: `INTERNET`, `ACCESS_NETWORK_STATE` (photo selection uses zero-permission `PickVisualMedia`)

## 2. Export & Open Locally
1. Export the complete source project as a ZIP via **Settings -> Export ZIP** in AI Studio (or push to GitHub).
2. Open the extracted folder in **Android Studio** (JDK 11+, Android SDK 36).
3. Keep `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` intact.

## 3. Local Debug Testing (Debug APK)
- Build the debug APK (`debugConfig` signing):
  ```bash
  gradle :app:assembleDebug
  ```
- Output path: `app/build/outputs/apk/debug/app-debug.apk`
- Install on a connected device or local Android Studio emulator:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- Run JVM unit + Robolectric Compose UI tests:
  ```bash
  gradle :app:testDebugUnitTest
  ```

## 4. Signed Android App Bundle (AAB) Release Workflow
`app/build.gradle.kts` already defines `signingConfigs.release` reading environment variables:
- `KEYSTORE_PATH` (defaults to `${rootDir}/my-upload-key.jks`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- `keyAlias = "upload"`

### Steps to Generate a Real Signed Release AAB
1. **Generate a real upload keystore locally** (never commit `.jks` files; `*.jks` is git-ignored):
   ```bash
   keytool -genkeypair -v \
     -keystore my-upload-key.jks \
     -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Register your release / Play App Signing SHA-1 & SHA-256** in **Firebase Console > Project Settings > Android app (`com.connect.social`)**, then download the refreshed `google-services.json` if needed so Google Sign-In works in release builds.
3. **Export signing environment variables and build the AAB**:
   ```bash
   export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
   export STORE_PASSWORD="<your-keystore-password>"
   export KEY_PASSWORD="<your-key-password>"
   gradle :app:bundleRelease
   ```
4. **Release AAB output path**:
   `app/build/outputs/bundle/release/app-release.aab`
