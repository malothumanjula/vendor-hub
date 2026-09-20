package com.vendorapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vendorapp.data.AppLanguage
import com.vendorapp.data.BackendRepository
import com.vendorapp.data.ChatMessage
import com.vendorapp.data.DeliveryRequest
import com.vendorapp.data.LocationRule
import com.vendorapp.data.MockRepository
import com.vendorapp.data.OrderStatus
import com.vendorapp.data.PaymentRecord
import com.vendorapp.data.PaymentStatus
import com.vendorapp.data.PreferencesRepository
import com.vendorapp.data.UserRole
import com.vendorapp.data.Vendor
import com.vendorapp.data.Wallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val isLoaded: Boolean = false,
    val selectedLanguage: AppLanguage? = null,
    val role: UserRole? = null,
    val userName: String = "Aarav",
    val vendors: List<Vendor> = MockRepository.vendors,
    val activeVendor: Vendor? = MockRepository.vendors.firstOrNull(),
    val isSignedIn: Boolean = false,
    val activeChat: List<ChatMessage> = MockRepository.sampleChat,
    val deliveryRequest: DeliveryRequest = DeliveryRequest(
        "DR-1001",
        "Tea and snack delivery request",
        "Aarav",
        "Lakshmi Tea Stall",
        "Beside Central Park",
        OrderStatus.PAYMENT_PENDING,
        180,
        30,
        9,
        false,
        "2 plates pani puri + 1 tea",
        "Near Central Park",
        "Beside Central Park",
        "1.2 km",
        120
    ),
    val payment: PaymentRecord = PaymentRecord("PAY-1001", 180, 30, 9, PaymentStatus.PAID),
    val wallet: Wallet = Wallet(),
    val locationRule: LocationRule = MockRepository.locationRules.first(),
    val deliveryAvailable: Boolean = true,
    val deliveryBoyOnline: Boolean = true,
    val deliveryRequests: List<DeliveryRequest> = listOf(
        DeliveryRequest(
            "DR-2001",
            "Vegetable delivery",
            "Priya",
            "Green Basket Market",
            "Vegetable Lane",
            OrderStatus.DELIVERY_REQUESTED,
            240,
            35,
            10,
            true,
            "Fresh vegetables and rice",
            "Near City Market",
            "Vegetable Lane",
            "1.6 km",
            150
        ),
        DeliveryRequest(
            "DR-2002",
            "Chaat order",
            "Naveen",
            "Charminar Chaat",
            "Market Street",
            OrderStatus.PAID,
            180,
            25,
        8,
            false,
            "2 plates pani puri and masala tea",
            "Near Station Road",
            "Market Street",
            "2.3 km",
            110
        )
    ),
    val myDeliveries: List<DeliveryRequest> = emptyList(),
    val activeDelivery: DeliveryRequest? = null,
    val safetyWarning: String? = null,
    val isLoading: Boolean = false,
    val apiError: String? = null,
    val activeChatId: String? = null,
    val searchQuery: String = "",
    val currentRoute: String = "map"
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = PreferencesRepository(application)
    private val backend = BackendRepository()
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.selectedLanguage.collect { language ->
                _uiState.update { it.copy(isLoaded = true, selectedLanguage = language) }
            }
        }
    }

    fun selectLanguage(language: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun saveSelectedLanguage() {
        _uiState.value.selectedLanguage?.let { language ->
            viewModelScope.launch { preferences.saveLanguage(language) }
        }
    }

    fun signIn(role: UserRole, name: String) {
        _uiState.update {
            it.copy(
                role = role,
                userName = name.ifBlank { if (role == UserRole.VENDOR) "Ravi Kumar" else "Aarav" },
                isSignedIn = true,
                activeVendor = it.activeVendor ?: MockRepository.vendors.firstOrNull()
            )
        }
        loadVendors()
    }

    fun loadVendors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, apiError = null) }
            val result = backend.vendors()
            _uiState.update { it.copy(isLoading = false, vendors = result.value ?: MockRepository.vendors, apiError = result.error) }
        }
    }

    fun searchVendors(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(vendors = MockRepository.vendors) }
            return
        }
        val filtered = MockRepository.vendors.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
        }
        _uiState.update { it.copy(vendors = filtered.ifEmpty { MockRepository.vendors }) }
    }

    fun selectVendor(vendor: Vendor) {
        _uiState.update { it.copy(activeVendor = vendor) }
    }

    fun openVendorChat(vendorId: String) {
        val vendor = MockRepository.vendors.firstOrNull { it.id == vendorId }
        _uiState.update { it.copy(activeVendor = vendor, activeChatId = vendorId, safetyWarning = null) }
        viewModelScope.launch {
            val result = backend.createChat(vendorId)
            if (result.value != null) {
                _uiState.update { state -> state.copy(activeChatId = result.value, apiError = null) }
            } else {
                _uiState.update { it.copy(apiError = result.error) }
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val normalized = text.trim()
        val suspicious = normalized.contains(Regex("(?i)(upi|@\\w+|paytm|phonepe|gpay|qr code|pay outside|bank acc|upi id|[0-9]{10,})"))
        if (suspicious) {
            _uiState.update {
                it.copy(
                    safetyWarning = "Payment outside the platform is not allowed. Please keep payments within the app."
                )
            }
            return
        }

        val newMessage = ChatMessage("m${_uiState.value.activeChat.size + 1}", "Customer", normalized, "now", true)
        _uiState.update { state -> state.copy(activeChat = state.activeChat + newMessage, safetyWarning = null) }
    }

    fun createDeliveryRequest(vendorId: String) {
        val vendor = MockRepository.vendors.firstOrNull { it.id == vendorId } ?: return
        val summary = _uiState.value.deliveryRequest.itemSummary
        val request = _uiState.value.deliveryRequest.copy(
            vendorName = vendor.name,
            location = vendor.location,
            title = "${vendor.name} delivery request",
            status = OrderStatus.DELIVERY_REQUESTED,
            amount = 180,
            deliveryFee = 30,
            platformFee = 9,
            isAssistedMarket = vendor.isAssistedMarket,
            itemSummary = summary
        )
        _uiState.update { it.copy(deliveryRequest = request, payment = PaymentRecord("PAY-1002", 180, 30, 9, PaymentStatus.CREATED)) }
    }

    fun confirmPayment() {
        val payment = _uiState.value.payment.copy(status = PaymentStatus.PAID)
        val request = _uiState.value.deliveryRequest.copy(status = OrderStatus.PAID)
        _uiState.update { it.copy(payment = payment, deliveryRequest = request, wallet = it.wallet.copy(availableBalance = it.wallet.availableBalance + 180)) }
    }

    fun updateDeliveryStatus(status: OrderStatus) {
        _uiState.update { it.copy(deliveryRequest = it.deliveryRequest.copy(status = status)) }
    }

    fun setLocationRule(rule: LocationRule) {
        _uiState.update { it.copy(locationRule = rule) }
    }

    fun toggleAvailability() {
        _uiState.update { it.copy(deliveryAvailable = !it.deliveryAvailable) }
    }

    fun toggleDeliveryBoyOnline() {
        _uiState.update { it.copy(deliveryBoyOnline = !it.deliveryBoyOnline) }
    }

    fun acceptDeliveryRequest(requestId: String) {
        _uiState.update { state ->
            val updatedList = state.deliveryRequests.map { request ->
                if (request.id == requestId) request.copy(status = OrderStatus.DELIVERY_ASSIGNED) else request
            }
            val accepted = updatedList.firstOrNull { it.id == requestId }
            state.copy(
                deliveryRequests = updatedList.filterNot { it.id == requestId },
                myDeliveries = (state.myDeliveries + listOfNotNull(accepted)).distinctBy { it.id },
                activeDelivery = accepted ?: state.activeDelivery,
                deliveryBoyOnline = true
            )
        }
    }

    fun updateActiveDelivery(status: OrderStatus) {
        _uiState.update { state ->
            val active = state.activeDelivery ?: state.deliveryRequest
            val updatedActive = active.copy(status = status)
            state.copy(
                activeDelivery = updatedActive,
                deliveryRequest = if (state.activeDelivery == null) state.deliveryRequest.copy(status = status) else state.deliveryRequest,
                myDeliveries = state.myDeliveries.map { if (it.id == active.id) updatedActive else it }
            )
        }
    }

    fun setCurrentRoute(route: String) {
        _uiState.update { it.copy(currentRoute = route) }
    }
}
