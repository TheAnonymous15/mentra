package com.example.mentra.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mentra.core.common.permissions.PermissionManager
import com.example.mentra.core.common.permissions.PermissionStats
import com.example.mentra.core.common.permissions.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for permission setup screen
 */
@HiltViewModel
class PermissionSetupViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    val locationHelper: LocationPermissionHelper
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<PermissionSetupUiState> = combine(
        permissionManager.permissionState,
        permissionManager.setupComplete,
        _refreshTrigger
    ) { permissionStates, setupComplete, _ ->
        val stats = permissionManager.getPermissionStats()
        PermissionSetupUiState(
            permissionStates = permissionStates,
            stats = stats,
            isSetupComplete = setupComplete,
            canSkip = !stats.isFullySetup && stats.criticalPercentage == 100
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PermissionSetupUiState()
    )

    init {
        updatePermissionStates()
    }

    fun updatePermissionStates() {
        viewModelScope.launch {
            // Small delay to allow system to propagate permission changes
            // Especially important for special permissions granted through Settings
            delay(300)

            permissionManager.updateAllPermissionStates()
            _refreshTrigger.value++
        }
    }

    fun handlePermissionResults(results: Map<String, Boolean>) {
        viewModelScope.launch {
            permissionManager.handlePermissionResults(results)
            _refreshTrigger.value++
        }
    }

    fun getDeniedPermissions(): List<String> {
        val allPermissions = com.example.mentra.core.common.permissions.MentraPermissions.getAllRuntimePermissions()
        return permissionManager.getDeniedPermissions(allPermissions)
    }
}

/**
 * UI state for permission setup screen
 */
data class PermissionSetupUiState(
    val permissionStates: Map<String, PermissionStatus> = emptyMap(),
    val stats: PermissionStats = PermissionStats(
        totalPermissions = 0,
        grantedPermissions = 0,
        deniedPermissions = 0,
        criticalGranted = 0,
        criticalTotal = 0,
        isFullySetup = false
    ),
    val isSetupComplete: Boolean = false,
    val canSkip: Boolean = false
)

