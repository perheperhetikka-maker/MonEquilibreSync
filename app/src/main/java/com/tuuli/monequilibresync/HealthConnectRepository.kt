package com.tuuli.monequilibresync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Read-only Health Connect data source for the Android bridge.
 *
 * It reads only the three data types needed for the first production bridge:
 * - daily steps
 * - exercise sessions
 * - distance recorded during each exercise session
 *
 * This repository never writes to Health Connect. Backend synchronization is
 * handled separately by SupabaseRepository so the two responsibilities stay isolated.
 */
class HealthConnectRepository(private val context: Context) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            HealthConnectAvailability.ProviderUpdateRequired
        else -> HealthConnectAvailability.Unavailable
    }

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    suspend fun hasAllPermissions(): Boolean {
        if (availability() != HealthConnectAvailability.Available) return false
        val granted = client().permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun readToday(): TodayHealthSnapshot {
        check(availability() == HealthConnectAvailability.Available) {
            "Health Connect n'est pas disponible sur cet appareil."
        }

        val client = client()
        val granted = client.permissionController.getGrantedPermissions()
        check(granted.containsAll(permissions)) {
            "Les autorisations Health Connect nécessaires ne sont pas accordées."
        }

        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        // Health Connect recommends aggregate() for cumulative data such as steps,
        // because the aggregation layer handles overlapping sources more safely.
        val stepAggregation = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
            )
        )
        val steps = stepAggregation[StepsRecord.COUNT_TOTAL] ?: 0L

        val sessions = client.readRecords(
            ReadRecordsRequest<ExerciseSessionRecord>(
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
            )
        ).records
            .sortedByDescending { it.startTime }
            .map { session ->
                val distanceAggregation = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(
                            session.startTime,
                            session.endTime,
                        ),
                        // Keep the distance tied to the same writer as the session
                        // (for example Samsung Health), which reduces accidental overlap.
                        dataOriginFilter = setOf(session.metadata.dataOrigin),
                    )
                )
                val distanceMeters =
                    distanceAggregation[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0

                ExerciseSummary(
                    recordId = session.metadata.id,
                    sourcePackage = session.metadata.dataOrigin.packageName,
                    exerciseType = session.exerciseType,
                    label = exerciseTypeLabel(session.exerciseType),
                    startTime = session.startTime,
                    endTime = session.endTime,
                    durationMinutes = Duration.between(session.startTime, session.endTime)
                        .toMinutes()
                        .coerceAtLeast(0),
                    distanceMeters = distanceMeters,
                )
            }

        return TodayHealthSnapshot(
            date = LocalDate.now(zone),
            steps = steps,
            exercises = sessions,
            loadedAt = now,
        )
    }

    private fun exerciseTypeLabel(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Marche"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Course"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Randonnée"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Vélo"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Vélo d’intérieur"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Natation"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Natation en eau libre"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Renforcement"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Musculation"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Pilates"
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Danse"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptique"
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> "Escaliers"
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "Autre exercice"
        else -> "Exercice"
    }
}

enum class HealthConnectAvailability {
    Available,
    ProviderUpdateRequired,
    Unavailable,
}

data class TodayHealthSnapshot(
    val date: LocalDate,
    val steps: Long,
    val exercises: List<ExerciseSummary>,
    val loadedAt: Instant,
)

data class ExerciseSummary(
    val recordId: String,
    val sourcePackage: String,
    val exerciseType: Int,
    val label: String,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Long,
    val distanceMeters: Double,
)
