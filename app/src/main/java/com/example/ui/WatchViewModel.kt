package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UserProfileEntity
import com.example.data.WatchItem
import com.example.data.WatchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PaymentState {
    object Idle : PaymentState
    object Processing : PaymentState
    data class SecurityConfirming(val step: String) : PaymentState
    data class Completed(val transactionId: String, val amount: Double, val date: Long) : PaymentState
    data class Error(val message: String) : PaymentState
}

sealed interface ArState {
    object Idle : ArState
    object PermissionRequesting : ArState
    class Active(val watchItem: WatchItem, val wristGuideScale: Float = 1.0f, val rotationDegrees: Float = 0.0f) : ArState
}

data class SyncLog(val timestamp: Long, val type: String, val detail: String)

class WatchViewModel(private val repository: WatchRepository) : ViewModel() {

    // Selected screen destination (simple minimal navigation state)
    // "login", "hero", "catalog", "details", "try-on", "favorites", "profile"
    private val _currentScreen = MutableStateFlow("login")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Currently selected watch in detail view
    private val _selectedWatchId = MutableStateFlow("vector-quantum")
    val selectedWatchId: StateFlow<String> = _selectedWatchId.asStateFlow()

    // Observe Favorites Room Flow
    val favoriteWatches: StateFlow<List<WatchItem>> = repository.favoriteWatchesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val watchCatalog: List<WatchItem> = repository.watchCatalog

    // Observe Profile Room Flow
    val userProfile: StateFlow<UserProfileEntity> = repository.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfileEntity()
        )

    // Interactive 3D renderer state
    private val _watchRotationX = MutableStateFlow(0f)
    val watchRotationX: StateFlow<Float> = _watchRotationX.asStateFlow()

    private val _watchRotationY = MutableStateFlow(0f)
    val watchRotationY: StateFlow<Float> = _watchRotationY.asStateFlow()

    private val _customStrapColorOption = MutableStateFlow<String?>(null)
    val customStrapColorOption: StateFlow<String?> = _customStrapColorOption.asStateFlow()

    // AR Customization State
    private val _arWristScale = MutableStateFlow(1.0f)
    val arWristScale: StateFlow<Float> = _arWristScale.asStateFlow()

    private val _arRotation = MutableStateFlow(0f)
    val arRotation: StateFlow<Float> = _arRotation.asStateFlow()

    // Payment states
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    // Live terminal log simulation mimicking real-time active Firebase cloud Sync
    private val _firebaseSyncLogs = MutableStateFlow<List<SyncLog>>(
        listOf(
            SyncLog(System.currentTimeMillis() - 25000, "INIT", "Firebase sync initialized connection..."),
            SyncLog(System.currentTimeMillis() - 20000, "AUTH", "Synchronized OAuth with Vector cloud securely."),
            SyncLog(System.currentTimeMillis() - 15000, "READ", "Fetched 4 synced favorite schemas from firebase.")
        )
    )
    val firebaseSyncLogs: StateFlow<List<SyncLog>> = _firebaseSyncLogs.asStateFlow()

    // Filter selection category (All, Quantum, Tourbillon, Analogs, Chronos)
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Search queries
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun selectWatch(watchId: String) {
        _selectedWatchId.value = watchId
        _currentScreen.value = "details"
        // reset custom colors/rotation
        _watchRotationX.value = 0f
        _watchRotationY.value = 0f
        _customStrapColorOption.value = null
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun rotateWatch(dx: Float, dy: Float) {
        _watchRotationX.value = (_watchRotationX.value + dx) % 360f
        _watchRotationY.value = (_watchRotationY.value + dy).coerceIn(-45f, 45f)
    }

    fun setStrapSwap(colorHexName: String?) {
        _customStrapColorOption.value = colorHexName
    }

    fun adjustArScale(scaleChange: Float) {
        _arWristScale.value = (_arWristScale.value + scaleChange).coerceIn(0.5f, 2.0f)
    }

    fun rotateArModel(degrees: Float) {
        _arRotation.value = (_arRotation.value + degrees) % 360f
    }

    fun toggleFavoriteInDb(watchId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(watchId)
            // Add custom log entry to terminalsync
            addSyncLog("PERSIST", "Room DB local favorite toggled: $watchId")
            
            // Simulating real-time auto-sync to Firebase matching user intent
            if (userProfile.value.syncEnabled) {
                delay(600)
                addSyncLog("FIREBASE_SYNC", "Pushing updated database state to Firebase Firestore collections...")
                delay(800)
                repository.triggerCloudBackup()
                addSyncLog("FIREBASE_SUCCESS", "Successfully synced nodes, transaction sequence complete.")
            }
        }
    }

    fun addSyncLog(type: String, detail: String) {
        val updatedList = _firebaseSyncLogs.value.toMutableList()
        updatedList.add(0, SyncLog(System.currentTimeMillis(), type, detail))
        if(updatedList.size > 20) {
            updatedList.removeAt(updatedList.lastIndex)
        }
        _firebaseSyncLogs.value = updatedList
    }

    fun triggerCloudSyncManual() {
        viewModelScope.launch {
            addSyncLog("SYNC_MANUAL", "Connecting to Firebase server cluster. Authenticating JWT token...")
            delay(1000)
            repository.triggerCloudBackup()
            addSyncLog("FIREBASE_SYNC", "Realtime channel established. Uploading database checkpoints...")
            delay(1200)
            addSyncLog("FIREBASE_SUCCESS", "Synchronized in 172ms across 2 client terminals.")
        }
    }

    fun updateProfileName(newName: String, status: String) {
        viewModelScope.launch {
            val updatedProfile = userProfile.value.copy(username = newName, statusText = status)
            repository.updateProfile(updatedProfile)
            addSyncLog("PROFILE_UDP", "Saved local configuration changes.")
            if (userProfile.value.syncEnabled) {
                delay(1000)
                triggerCloudSyncManual()
            }
        }
    }

    fun setSyncEnabledState(isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(syncEnabled = isEnabled)
            repository.updateProfile(updated)
            addSyncLog("CONFIG_SET", "Realtime sync preset changed to: $isEnabled")
        }
    }

    // Payment Processing Logic
    fun startPaymentFlow(amount: Double, gateway: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            addSyncLog("PAY_GATEWAY", "Injected $gateway sandbox context securely.")
            
            delay(1200)
            _paymentState.value = PaymentState.SecurityConfirming("Interfacing biometric verification (Face ID / Fingerprint / Passcode)")
            
            delay(1500)
            _paymentState.value = PaymentState.SecurityConfirming("Establishing encrypted secure enclave handshake with Apple Pay / Android Pay tokens...")
            
            delay(2000)
            val successId = "TXN-${System.currentTimeMillis() % 10000000}-${(100..999).random()}"
            _paymentState.value = PaymentState.Completed(
                transactionId = successId,
                amount = amount,
                date = System.currentTimeMillis()
            )
            addSyncLog("PAY_SUCCESS", "Processed payment invoice $successId successfully inside secure sandbox environment.")
        }
    }

    fun resetPayment() {
        _paymentState.value = PaymentState.Idle
    }
}

class WatchViewModelFactory(private val repository: WatchRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
