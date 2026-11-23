# Building a signed release

The app builds as a debug APK with no setup (`gradlew assembleDebug`). A release build that you can
install on other phones or upload to Google Play must be signed with a key that only you hold.

## 1. Before publishing on Google Play

* Change `applicationId` in `app/build.gradle.kts` (currently `com.example.usconstitution`).
  Google Play rejects `com.example` ids. Pick a reverse-domain id you control, for example
  `com.yourname.usconstitution`, and keep it forever: it cannot be changed after publishing.
* Raise `versionCode` by 1 for every upload, and update `versionName`.

## 2. Create the signing key (once)

Run this yourself. `keytool` asks you for the passwords, so nobody else ever sees them. Use the JDK that
comes with Android Studio:

    & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
        -keystore "$env:USERPROFILE\keys\us-constitution-release.jks" `
        -alias release -keyalg RSA -keysize 2048 -validity 10000

(Create the `keys` folder first, and keep it OUTSIDE this project and outside OneDrive-synced
folders if you can.)

**Back up the `.jks` file and both passwords somewhere safe.** If you lose the key you cannot update
the app under the same listing. Google Play App Signing (enabled by default for new apps) lets Google
hold the final signing key and gives you an upload key you can reset, which is the safer setup.

## 3. Tell the build about the key

Copy `keystore.properties.example` to `keystore.properties` in the project root and fill in the path,
alias and passwords. The file is listed in `.gitignore`.

## 4. Build

    .\gradlew.bat assembleRelease     # app/build/outputs/apk/release/app-release.apk  (install on phones)
    .\gradlew.bat bundleRelease       # app/build/outputs/bundle/release/app-release.aab  (upload to Play)

Without `keystore.properties` these still build, but the release APK is unsigned and will not install.

## 5. Check the signature

    & "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.1.0\apksigner.bat" verify --verbose app\build\outputs\apk\release\app-release.apk
