package com.tuuli.monequilibresync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

data class AppUiState(
    val initializing: Boolean = true,
    val signedIn: Boolean = false,
    val userEmail: String? = null,
    val healthAvailability: HealthConnectAvailability? = null,
    val permissionsGranted: Boolean = false,
    val snapshot: TodayHealthSnapshot? = null,
    val busy: Boolean = false,
    val lastSyncAt: Instant? = null,
    val message: String? = null,
    val error: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val healthRepository = HealthConnectRepository(application)
    private val supabaseRepository = SupabaseRepository()

    val permissions: Set<String> = healthRepository.permissions

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val signedIn = supabaseRepository.isSignedIn()
            if (!signedIn) {
                _state.value = AppUiState(initializing = false, signedIn = false)
                return@launch
            }

            val availability = healthRepository.availability()
            if (availability != HealthConnectAvailability.Available) {
                _state.value = AppUiState(
                    initializing = false,
                    signedIn = true,
                    userEmail = supabaseRepository.currentUserEmail(),
                    healthAvailability = availability,
                )
                return@launch
            }

            try {
                val granted = healthRepository.hasAllPermissions()
                if (!granted) {
                    _state.value = AppUiState(
                        initializing = false,
                        signedIn = true,
                        userEmail = supabaseRepository.currentUserEmail(),
                        healthAvailability = availability,
                        permissionsGranted = false,
                    )
                    return@launch
                }

                val snapshot = healthRepository.readToday()
                _state.value = AppUiState(
                    initializing = false,
                    signedIn = true,
                    userEmail = supabaseRepository.currentUserEmail(),
                    healthAvailability = availability,
                    permissionsGranted = true,
                    snapshot = snapshot,
                )
            } catch (security: SecurityException) {
                _state.value = AppUiState(
                    initializing = false,
                    signedIn = true,
                    userEmail = supabaseRepository.currentUserEmail(),
                    healthAvailability = availability,
                    permissionsGranted = false,
                )
            } catch (t: Throwable) {
                _state.value = AppUiState(
                    initializing = false,
                    signedIn = true,
                    userEmail = supabaseRepository.currentUserEmail(),
                    healthAvailability = availability,
                    error = t.message ?: "Impossible de lire Health Connect.",
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, message = null)
            try {
                supabaseRepository.signIn(email, password)
                refresh()
            } catch (t: Throwable) {
                _state.value = AppUiState(
                    initializing = false,
                    signedIn = false,
                    busy = false,
                    error = t.message ?: "Connexion impossible.",
                )
            }
        }
    }

    fun signOut() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, message = null)
            try {
                supabaseRepository.signOut()
            } finally {
                _state.value = AppUiState(initializing = false, signedIn = false)
            }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        if (granted.containsAll(permissions)) refresh()
        else _state.value = _state.value.copy(
            permissionsGranted = false,
            error = "Les trois autorisations sont nécessaires pour cette version du connecteur.",
        )
    }

    fun syncNow() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, error = null, message = null)
            try {
                val snapshot = healthRepository.readToday()
                val result = supabaseRepository.sync(snapshot)
                _state.value = _state.value.copy(
                    snapshot = snapshot,
                    busy = false,
                    lastSyncAt = result.syncedAt,
                    message = "Synchronisé : ${result.steps} pas et ${result.exerciseCount} séance(s).",
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    busy = false,
                    error = t.message ?: "Synchronisation impossible.",
                )
            }
        }
    }
}
