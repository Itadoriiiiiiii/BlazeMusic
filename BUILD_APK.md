# Générer l'APK BlazeMusic

## Option 1 — Android Studio

1. Ouvre le dossier `BlazeMusicAndroid` dans Android Studio.
2. Attends la synchronisation Gradle.
3. Menu **Build > Build APK(s)**.
4. L'APK debug sera dans `app/build/outputs/apk/debug/app-debug.apk`.

## Option 2 — GitHub Actions (sans installer Android Studio)

1. Mets ce projet dans un dépôt GitHub.
2. Va dans **Actions**.
3. Lance **Build BlazeMusic APK** avec **Run workflow**.
4. À la fin, télécharge l'artifact `BlazeMusic-debug-apk`.

Le workflow installe Java 17 et Gradle 8.13 automatiquement et compile l'APK.

## Option 3 — Windows

Si Gradle est installé, double-clique sur `build-apk.bat`.

### Note

L'APK debug est destiné aux tests et à l'installation directe. Pour une version release distribuable, il faut signer l'APK avec une clé privée.
