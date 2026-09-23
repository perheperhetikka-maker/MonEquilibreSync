# Mon équilibre Sync — Android Health Connect bridge

Version 0.2.1.

## Scope

The app intentionally handles only:
- daily steps from Health Connect;
- exercise sessions;
- distance associated with each exercise session.

It does **not** currently read calories, heart rate, VO2 max, sleep, nutrition, hydration, body fat, cycle data, or Samsung's proprietary SDK.

## Data path

Samsung Health → Health Connect → Mon équilibre Sync → Supabase.

The app signs in with the same Supabase account used by Mon équilibre. It embeds only the public/publishable key. No service-role or secret key is present in the APK. RLS restricts database rows to the signed-in user.

### Tables

- `health_connect_daily`: one daily Health Connect step total per user/date.
- `health_connect_exercises`: idempotent exercise rows keyed by `(user_id, health_connect_record_id)`.
- `integration_sources`: existing Mon équilibre table, updated to mark `health_connect` connected after a successful sync.

Manual `daily_entries` are deliberately not overwritten.

## Build requirements

- JDK 17
- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- compileSdk 37
- minSdk 28

The project includes `gradlew`, `gradlew.bat`, and `gradle-wrapper.properties`. The wrapper JAR is bootstrapped on first command-line build if absent. Android Studio can also sync the project normally. If it asks to install Android SDK Platform 37 / Build Tools 36.0.0, allow it.

## First phone test

1. Install the debug APK on the Samsung phone.
2. Sign in with the same Mon équilibre email/password.
3. Grant only Health Connect permissions for steps, exercise and distance.
4. Confirm the displayed steps are plausible.
5. Confirm today's exercise sessions and their distance.
6. Tap **Synchroniser maintenant**.
7. Verify rows appear in `health_connect_daily` and `health_connect_exercises` for that user.

No background synchronization is included yet. This is intentional: manual sync is the first reliability checkpoint.
