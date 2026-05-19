# Build APK

## Automatisch (GitHub Actions)

Für automatische APK-Builds bei jedem Release:

1. Erstelle `.github/workflows/build-release.yml` mit diesem Inhalt:

```yaml
name: Build Release APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
      
    - name: Cache Gradle
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build Debug APK
      run: ./gradlew :androidApp:assembleDebug --no-daemon
      
    - name: Build Release APK
      run: ./gradlew :androidApp:assembleRelease --no-daemon
      
    - name: Rename APKs
      run: |
        mkdir -p release-apks
        cp androidApp/build/outputs/apk/debug/androidApp-debug.apk release-apks/notes-app-debug-${{ github.ref_name }}.apk
        cp androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk release-apks/notes-app-release-${{ github.ref_name }}-unsigned.apk
        ls -la release-apks/
    
    - name: Upload APKs to Release
      uses: softprops/action-gh-release@v1
      with:
        files: release-apks/*.apk
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

2. Pushe zu GitHub - bei jedem Tag `v*` wird automatisch die APK gebaut und zum Release hinzugefügt

## Manuell bauen

### Voraussetzungen

- Java JDK 17
- Android SDK (API 34)
- Android Studio oder Gradle

### Schritte

```bash
# Repo klonen
git clone https://github.com/Free4Agent/notes-app.git
cd notes-app

# Debug APK bauen
./gradlew :androidApp:assembleDebug

# Release APK bauen (unsigned)
./gradlew :androidApp:assembleRelease

# APKs finden unter:
# androidApp/build/outputs/apk/debug/androidApp-debug.apk
# androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk
```

### Signieren (für Release)

```bash
# Keystore erstellen
keytool -genkey -v -keystore notes.keystore -alias notes -keyalg RSA -keysize 2048 -validity 10000

# APK signieren
apksigner sign --ks notes.keystore --out notes-app-signed.apk androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk
```

## Installation

```bash
# Auf Android-Gerät installieren
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Oder APK aufs Gerät kopieren und per Datei-Manager installieren ("Unbekannte Quellen" erlauben).
