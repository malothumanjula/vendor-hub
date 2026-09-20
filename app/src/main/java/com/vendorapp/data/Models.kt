package com.vendorapp.data

enum class UserRole { CUSTOMER, VENDOR, DELIVERY_PERSON, DELIVERY_BOY, ADMIN }

data class User(val id: String, val name: String, val phone: String, val role: UserRole)

enum class DeliveryBoyStatus(val label: String) {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    BUSY("BUSY")
}

enum class DeliveryProgressStatus(val label: String) {
    ASSIGNED("Assigned"),
    GOING_TO_VENDOR("Going to vendor"),
    ARRIVED_AT_VENDOR("Arrived at vendor"),
    COLLECTING_ITEM("Collecting item"),
    ITEM_COLLECTED("Item collected"),
    OUT_FOR_DELIVERY("Out for delivery"),
    ARRIVED_AT_CUSTOMER("Arrived at customer"),
    DELIVERED("Delivered"),
    COMPLETED("Completed")
}

data class Category(val id: String, val name: String, val emoji: String)

enum class VendorLocationStatus(val label: String) {
    ACTIVE("Active"),
    MOVING("Moving"),
    RELOCATING("Relocating"),
    INACTIVE("Inactive"),
    SHUT_DOWN("Shut down"),
    ACTIVE_AT_NEW_LOCATION("Active at new location")
}

data class Vendor(
    val id: String,
    val name: String,
    val category: String,
    val location: String,
    val distance: String,
    val rating: Double,
    val status: VendorLocationStatus = VendorLocationStatus.ACTIVE,
    val imageEmoji: String = "📍",
    val isAssistedMarket: Boolean = false,
    val serviceArea: String = "Near market square"
)

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: String,
    val isFromCustomer: Boolean
)

enum class OrderStatus(val label: String) {
    CHAT_STARTED("Chat started"),
    ITEM_DISCUSSION("Item discussion"),
    COST_CONFIRMED("Cost confirmed"),
    DELIVERY_REQUESTED("Delivery requested"),
    PAYMENT_PENDING("Payment pending"),
    PAID("Paid"),
    DELIVERY_ASSIGNED("Delivery assigned"),
    GOING_TO_VENDOR("Going to vendor"),
    ARRIVED_AT_VENDOR("Arrived at vendor"),
    COLLECTING_ITEM("Collecting item"),
    ITEM_COLLECTED("Item collected"),
    OUT_FOR_DELIVERY("Out for delivery"),
    ARRIVED_AT_CUSTOMER("Arrived at customer"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    PAYMENT_FAILED("Payment failed"),
    DELIVERY_FAILED("Delivery failed")
}

data class DeliveryRequest(
    val id: String,
    val title: String,
    val customerName: String,
    val vendorName: String,
    val location: String,
    val status: OrderStatus,
    val amount: Int,
    val deliveryFee: Int,
    val platformFee: Int,
    val isAssistedMarket: Boolean = false,
    val itemSummary: String = "Vegetables and household items",
    val customerLocation: String = "Near Central Park",
    val vendorLocation: String = "Beside Central Park",
    val distance: String = "1.2 km",
    val deliveryEarning: Int = 120
)

data class PaymentRecord(
    val id: String,
    val amount: Int,
    val deliveryFee: Int,
    val platformFee: Int,
    val status: PaymentStatus,
    val provider: String = "Platform wallet"
)

enum class PaymentStatus(val label: String) {
    CREATED("Created"),
    PENDING("Pending"),
    PAID("Paid"),
    FAILED("Failed"),
    REFUNDED("Refunded")
}

data class Wallet(
    val availableBalance: Int = 620,
    val reservedAmount: Int = 180,
    val earnedDeliveryFees: Int = 290,
    val withdrawableBalance: Int = 420,
    val currency: String = "INR"
)

enum class LocationRuleType(val label: String) {
    NORMAL("Normal vendor zone"),
    ASSISTED_MARKET("Assisted market"),
    RESTRICTED_LISTING("Restricted listing"),
    DELIVERY_ENABLED("Delivery enabled")
}

data class LocationRule(
    val id: String,
    val name: String,
    val type: LocationRuleType,
    val description: String,
    val shouldShowAssistedCheckout: Boolean = false
)

data class DeliveryPerson(
    val id: String,
    val name: String,
    val distance: String,
    val rating: Double,
    val available: Boolean,
    val earnings: Int,
    val feeRate: Int
)

data class SafetyViolation(
    val id: String,
    val vendorName: String,
    val reason: String,
    val action: String
)

object MockRepository {
    val categories = listOf(
        Category("tea", "Tea", "☕"),
        Category("vegetables", "Vegetables", "🥬"),
        Category("repair", "Repair", "🧰"),
        Category("snacks", "Snacks", "🥟"),
        Category("flowers", "Flowers", "🌼")
    )

    val vendors = listOf(
        Vendor("v1", "Lakshmi Tea Stall", "Tea & Snacks", "Beside Central Park", "0.8 km", 4.8, VendorLocationStatus.ACTIVE, "☕"),
        Vendor("v2", "Charminar Chaat", "Street food", "Market Street", "1.4 km", 4.6, VendorLocationStatus.ACTIVE, "🥟"),
        Vendor("v3", "Green Basket Market", "Vegetables & groceries", "Vegetable Lane", "0.6 km", 4.7, VendorLocationStatus.RELOCATING, "🥬", true),
        Vendor("v4", "Metro Repair Cart", "Service vendor", "Station Road", "2.1 km", 4.5, VendorLocationStatus.ACTIVE, "🧰")
    )

    val deliveryPersons = listOf(
        DeliveryPerson("d1", "Suresh", "0.9 km", 4.8, true, 1450, 5),
        DeliveryPerson("d2", "Meera", "1.5 km", 4.7, false, 980, 6),
        DeliveryPerson("d3", "Raj", "2.2 km", 4.9, true, 1750, 4)
    )

    val locationRules = listOf(
        LocationRule("lr1", "Downtown vendor zone", LocationRuleType.NORMAL, "Standard street-vendor map discovery and local delivery flow.", false),
        LocationRule("lr2", "Vegetable market lane", LocationRuleType.ASSISTED_MARKET, "Chat-based assisted buying is enabled for market listings.", true),
        LocationRule("lr3", "North restricted road", LocationRuleType.RESTRICTED_LISTING, "Vendor discovery is limited by local permissions.", false)
    )

    val sampleChat = listOf(
        ChatMessage("m1", "Vendor", "Hi Aarav, I can deliver two plates of pani puri and tea.", "09:20 AM", false),
        ChatMessage("m2", "Customer", "I need 2 plates pani puri and 1 tea. Can you arrange delivery?", "09:21 AM", true),
        ChatMessage("m3", "Vendor", "Yes, total is ₹190 including delivery. I can request a pickup once you pay.", "09:22 AM", false)
    )

    fun vendor(id: String): Vendor = vendors.first { it.id == id }
}