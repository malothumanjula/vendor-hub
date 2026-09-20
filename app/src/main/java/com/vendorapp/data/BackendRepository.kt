package com.vendorapp.data

class BackendRepository(private val api: BackendApi = BackendApi()) {
    suspend fun login(email: String, password: String) = api.login(email, password)
    suspend fun register(email: String, password: String, name: String, role: UserRole) = api.register(email, password, name, role)
    suspend fun currentUser() = api.currentUser()
    suspend fun vendors() = api.vendors()
    suspend fun searchVendors(query: String) = api.searchVendors(query)
    suspend fun vendor(id: String) = api.vendor(id)
    suspend fun createChat(vendorId: String) = api.createChat(vendorId)
    suspend fun messages(chatId: String) = api.messages(chatId)
    suspend fun sendMessage(chatId: String, message: String) = api.sendMessage(chatId, message)
    suspend fun createRequest(vendorId: String, itemSummary: String, chatId: String?) = api.createRequest(vendorId, itemSummary, chatId)
    suspend fun createPayment(requestId: String) = api.createPayment(requestId)
    suspend fun confirmPayment(paymentId: String) = api.confirmPayment(paymentId)
}