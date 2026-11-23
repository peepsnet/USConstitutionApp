# Building a signed release

The app builds as a debug APK with no setup (`gradlew assembleDebug`). A release build that you can
install on other phones, attach to a GitHub Release, or upload to Google Play must be signed with a
key that only you hold.

## The easy way: `build-release.ps1`

Run it in a PowerShell window, from the project folder:

    powershell -ExecutionPolicy Bypass -File .\build-release.ps1

* **First run:** it creates your signing key at `%USERPROFILE%\keys\us-constitution-release.jks`
  (keytool asks for a password and some details; the details are stored in the certificate and are
  visible to anyone who inspects the APK).
* **Every run:** it asks for the keystore password, passes it to Gradle through the environment for that
  one run (nothing is written to a file), builds the minified release, checks the signature and copies
  the APK to `US-Constitution-3.0.0.apk` in the folder above the project.

**Back up the `.jks` file and its password.** If you lose either you cannot publish updates to the
app: Android only installs an update that is signed with the same key as the installed version.

## Other ways to give Gradle the key

* `keystore.properties` in the project root (copy `keystore.properties.example`; it is git-ignored), or
* the environment variables `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Then `gradlew assembleRelease` signs automatically. Without signing details it produces an unsigned
APK that will not install. `gradlew bundleRelease` builds the `.aab` bundle that Google Play expects.

## Before each new version

* Raise `versionCode` by 1 in `app/build.gradle.kts` for every release, and update `versionName`.
* Tag the commit (for example `v3.0.1`) and attach the APK to a GitHub Release.

## Before publishing on Google Play

* Change `applicationId` in `app/build.gradle.kts` (currently `com.example.usconstitution`). Google Play
  rejects `com.example` ids. Pick a reverse-domain id you control and keep it forever.
* Use Play App Signing (the default for new apps): Google holds the final signing key and your key
  becomes an upload key that can be reset.

## Check a signature

    & "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.1.0\apksigner.bat" verify --verbose --print-certs US-Constitution-3.0.0.apk
