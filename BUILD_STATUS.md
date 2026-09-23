# Build status — Mon équilibre Sync 0.2.1

## Verified in this package

- Android project structure is complete.
- AndroidManifest.xml parses successfully.
- Scope remains limited to READ_STEPS, READ_EXERCISE and READ_DISTANCE.
- Supabase uses the public publishable key only; no service-role or secret key is present.
- Supabase upserts use the current request DSL (`onConflict = ...`).
- Cold-start session restoration explicitly calls `auth.loadFromStorage()` when needed.
- Backend tables and user-scoped RLS policies have already been created in the Mon équilibre Supabase project.
- AGP 9.4.0 / Gradle 9.6.0 / Java 17 configuration is aligned with Android's published compatibility matrix.

## Not yet proven in this container

A real `assembleDebug` has not been completed here because this execution environment has no Android SDK and its shell has no external DNS access for Maven/Gradle dependency resolution. This is an environment limitation, not a successful-build claim.

## First real build command

On a machine with Android Studio / SDK installed and normal internet access:

```bash
./gradlew :app:assembleDebug --stacktrace
```

Expected APK path after a successful build:

`app/build/outputs/apk/debug/app-debug.apk`

## v0.2.2
- Ajout d'un workflow GitHub Actions reproductible pour compiler automatiquement l'APK debug.
- Le workflow installe Java 17 + Android API 37, exécute `assembleDebug`, puis publie l'APK comme artefact.
