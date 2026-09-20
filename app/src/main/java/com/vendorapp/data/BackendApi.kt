package com.vendorapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ApiResult<T>(val value: T? = null, val error: String? = null)

class BackendApi(private val baseUrl: String = "http://10.0.2.2:8080") {
    private var token: String? = null

    suspend fun login(email: String, password: String): ApiResult<User> = request("/auth/login", "POST", JSONObject().put("email", email).put("password", password)) { body ->
        token = body.optString("token")
        userFrom(body.getJSONObject("user"))
    }

    suspend fun register(email: String, password: String, name: String, role: UserRole): ApiResult<User> = request("/auth/register", "POST", JSONObject().put("email", email).put("password", password).put("name", name).put("role", role.name)) { body ->
        token = body.optString("token")
        userFrom(body.getJSONObject("user"))
    }

    suspend fun vendors(): ApiResult<List<Vendor>> = request("/vendors", "GET") { body ->
        val array = body.optJSONArray("vendors") ?: org.json.JSONArray()
        List(array.length()) { index -> vendorFrom(array.getJSONObject(index)) }
    }

    suspend fun currentUser(): ApiResult<User> = request("/users/me", "GET") { body -> userFrom(body) }

    suspend fun searchVendors(query: String): ApiResult<List<Vendor>> = request("/vendors/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}", "GET") { body ->
        val array = body.optJSONArray("results") ?: org.json.JSONArray()
        List(array.length()) { index -> vendorFrom(array.getJSONObject(index)) }
    }

    suspend fun vendor(id: String): ApiResult<Vendor> = request("/vendors/$id", "GET") { body -> vendorFrom(body) }

    suspend fun createChat(vendorId: String): ApiResult<String> = request("/chats", "POST", JSONObject().put("vendor_id", vendorId)) { it.getString("id") }

    suspend fun sendMessage(chatId: String, message: String): ApiResult<ChatMessage> = request("/chats/$chatId/messages", "POST", JSONObject().put("message", message)) { body ->
        ChatMessage(body.getString("id"), body.optString("sender_role", "CUSTOMER"), body.getString("message"), "now", true)
    }

    suspend fun messages(chatId: String): ApiResult<List<ChatMessage>> = request("/chats/$chatId/messages", "GET") { body ->
        val array = body.optJSONArray("messages") ?: org.json.JSONArray()
        List(array.length()) { index ->
            val message = array.getJSONObject(index)
            ChatMessage(message.getString("id"), message.optString("sender_role", "CUSTOMER"), message.getString("message"), message.optString("created_at", "now"), message.optString("sender_role") == "CUSTOMER")
        }
    }

    suspend fun createRequest(vendorId: String, itemSummary: String, chatId: String?): ApiResult<String> = request("/delivery-requests", "POST", JSONObject().put("vendor_id", vendorId).put("item_summary", itemSummary).apply { chatId?.let { put("chat_id", it) } }) { it.getString("id") }

    suspend fun createPayment(requestId: String): ApiResult<PaymentRecord> = request("/payments/create", "POST", JSONObject().put("delivery_request_id", requestId)) { body -> paymentFrom(body) }

    suspend fun confirmPayment(paymentId: String): ApiResult<PaymentRecord> = request("/payments/$paymentId/confirm", "POST") { body -> paymentFrom(body) }

    private suspend inline fun <T> request(path: String, method: String, payload: JSONObject? = null, crossinline parse: (JSONObject) -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
                token?.let { setRequestProperty("Authorization", "Bearer $it") }
                if (payload != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    outputStream.use { it.write(payload.toString().toByteArray()) }
                }
            }
            val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) return@withContext ApiResult(error = text.ifBlank { "Request failed (${connection.responseCode})" })
            ApiResult(value = parse(JSONObject(text)))
        } catch (error: Exception) {
            ApiResult(error = error.message ?: "Network request failed")
        }
    }

    private fun userFrom(body: JSONObject) = User(body.getString("id"), body.optString("name"), body.optString("email"), UserRole.valueOf(body.optString("role", "CUSTOMER")))
    private fun vendorFrom(body: JSONObject) = Vendor(
        body.getString("id"),
        body.getString("name"),
        body.optString("category"),
        body.optString("location"),
        body.optDouble("distance_km", 0.0).let { "%.1f km".format(it) },
        0.0,
        when (body.optString("status", "ACTIVE").uppercase()) {
            "ACTIVE" -> VendorLocationStatus.ACTIVE
            "MOVING" -> VendorLocationStatus.MOVING
            "RELOCATING" -> VendorLocationStatus.RELOCATING
            "INACTIVE" -> VendorLocationStatus.INACTIVE
            "SHUT_DOWN" -> VendorLocationStatus.SHUT_DOWN
            "ACTIVE_AT_NEW_LOCATION" -> VendorLocationStatus.ACTIVE_AT_NEW_LOCATION
            else -> VendorLocationStatus.ACTIVE
        },
        "🍲",
        body.optString("location_type") == "ASSISTED_MARKET",
        body.optString("description")
    )
    private fun paymentFrom(body: JSONObject) = PaymentRecord(body.getString("id"), body.optInt("item_amount", body.optInt("amount")), body.optInt("delivery_fee"), body.optInt("platform_fee"), PaymentStatus.valueOf(body.optString("status", "PENDING")))
}