package com.tuuli.monequilibresync

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Minimal backend bridge.
 *
 * Security model:
 * - only the public/publishable Supabase key is embedded in the APK;
 * - the user signs in with the same account as Mon équilibre;
 * - the user's JWT is attached by the Auth client;
 * - Postgres RLS restricts rows to auth.uid() = user_id;
 * - no service-role key is ever present on-device.
 */
class SupabaseRepository {

    private val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    ) {
        install(Auth) {
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
    }

    fun currentUserEmail(): String? = client.auth.currentSessionOrNull()?.user?.email

    /**
     * Auth storage loading is asynchronous on Android. On a cold start the in-memory
     * session can therefore be null for a short moment even when a persisted login
     * exists. Explicitly ask Auth to load storage before deciding the user is logged out.
     */
    suspend fun isSignedIn(): Boolean {
        if (client.auth.currentSessionOrNull() != null) return true
        client.auth.loadFromStorage()
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun signIn(email: String, password: String) {
        require(email.isNotBlank()) { "Adresse e-mail requise." }
        require(password.isNotBlank()) { "Mot de passe requis." }
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun sync(snapshot: TodayHealthSnapshot): SyncResult {
        val session = client.auth.currentSessionOrNull()
            ?: error("Vous devez être connectée à Mon équilibre.")
        val userId = session.user?.id
            ?: error("Session Supabase invalide : utilisateur absent.")
        val syncedAt = Instant.now().toString()

        val daily = HealthConnectDailyUpsert(
            userId = userId,
            entryDate = snapshot.date.toString(),
            steps = snapshot.steps.coerceIn(0, 200_000).toInt(),
            source = "health_connect",
            syncedAt = syncedAt,
        )
        client.from("health_connect_daily").upsert(daily) {
            onConflict = "user_id,entry_date"
        }

        val exercises = snapshot.exercises.map { exercise ->
            HealthConnectExerciseUpsert(
                userId = userId,
                healthConnectRecordId = exercise.recordId,
                sourcePackage = exercise.sourcePackage,
                exerciseType = exercise.exerciseType,
                exerciseLabel = exercise.label,
                startTime = exercise.startTime.toString(),
                endTime = exercise.endTime.toString(),
                durationMinutes = exercise.durationMinutes.coerceIn(0, 10_080).toInt(),
                distanceMeters = exercise.distanceMeters.coerceAtLeast(0.0),
                syncedAt = syncedAt,
            )
        }
        if (exercises.isNotEmpty()) {
            client.from("health_connect_exercises").upsert(exercises) {
                onConflict = "user_id,health_connect_record_id"
            }
        }

        client.from("integration_sources").upsert(
            IntegrationSourceUpsert(
                userId = userId,
                provider = "health_connect",
                status = "connected",
                lastSyncAt = syncedAt,
            )
        ) {
            onConflict = "user_id,provider"
        }

        return SyncResult(
            steps = daily.steps,
            exerciseCount = exercises.size,
            syncedAt = Instant.parse(syncedAt),
        )
    }
}

@Serializable
private data class HealthConnectDailyUpsert(
    @SerialName("user_id") val userId: String,
    @SerialName("entry_date") val entryDate: String,
    val steps: Int,
    val source: String,
    @SerialName("synced_at") val syncedAt: String,
)

@Serializable
private data class HealthConnectExerciseUpsert(
    @SerialName("user_id") val userId: String,
    @SerialName("health_connect_record_id") val healthConnectRecordId: String,
    @SerialName("source_package") val sourcePackage: String,
    @SerialName("exercise_type") val exerciseType: Int,
    @SerialName("exercise_label") val exerciseLabel: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("distance_meters") val distanceMeters: Double,
    @SerialName("synced_at") val syncedAt: String,
)

@Serializable
private data class IntegrationSourceUpsert(
    @SerialName("user_id") val userId: String,
    val provider: String,
    val status: String,
    @SerialName("last_sync_at") val lastSyncAt: String,
)

data class SyncResult(
    val steps: Int,
    val exerciseCount: Int,
    val syncedAt: Instant,
)
