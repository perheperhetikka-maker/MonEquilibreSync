# Compilation automatique de l'APK avec GitHub Actions

Ce projet contient `.github/workflows/build-android-apk.yml`.

Dès que le projet est placé dans un dépôt GitHub sur la branche `main`, GitHub Actions lance automatiquement une compilation debug.

Le workflow :
1. configure Java 17 ;
2. configure le SDK Android ;
3. installe Android API 37 ;
4. lance `./gradlew --no-daemon --stacktrace assembleDebug` ;
5. publie `app-debug.apk` comme artefact `MonEquilibreSync-debug` si la compilation réussit.

Le workflow peut aussi être relancé manuellement via l'onglet **Actions** > **Build Android APK** > **Run workflow**.

Aucun secret GitHub n'est requis pour la version actuelle : la configuration utilise uniquement la clé Supabase publishable, qui est déjà destinée au client. La sécurité des données dépend des politiques RLS Supabase.
