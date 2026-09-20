package com.vendorapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vendorapp.data.AppLanguage
import com.vendorapp.data.ChatMessage
import com.vendorapp.data.DeliveryRequest
import com.vendorapp.data.MockRepository
import com.vendorapp.data.OrderStatus
import com.vendorapp.data.UserRole
import com.vendorapp.data.Vendor
import com.vendorapp.ui.components.AppButton
import com.vendorapp.ui.components.AppTextField
import com.vendorapp.ui.components.BottomNavigation
import com.vendorapp.ui.components.EmptyState
import com.vendorapp.ui.components.LanguageCard
import com.vendorapp.ui.components.LoadingState
import com.vendorapp.ui.components.SectionHeader
import kotlinx.coroutines.delay

private object Routes {
    const val LANGUAGE = "language"
    const val ROLE = "role-selection"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot-password"
    const val MAP = "map"
    const val CHATS = "chats"
    const val ACTIVITY = "activity"
    const val PROFILE = "profile"
    const val VENDOR_DETAILS = "vendor-details"
    const val PAYMENT = "payment"
    const val TRACKING = "tracking"
    const val DASHBOARD = "vendor-dashboard"
    const val DELIVERY_BOY_DASHBOARD = "delivery-boy-dashboard"
    const val DELIVERY_REQUESTS = "delivery-requests"
    const val DELIVERY_REQUEST_DETAILS = "delivery-request-details"
    const val DELIVERY_PICKUP = "delivery-pickup"
    const val DELIVERY_WALLET = "delivery-wallet"
}

@Composable
fun VendorApp(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    if (!state.isLoaded) {
        LoadingState(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing))
        return
    }

    val start = if (state.selectedLanguage == null) Routes.LANGUAGE else Routes.ROLE

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LANGUAGE) {
            LanguageScreen(state.selectedLanguage, viewModel::selectLanguage) {
                viewModel.saveSelectedLanguage()
                nav.navigate(Routes.ROLE) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
            }
        }
        composable(Routes.ROLE) {
            RoleScreen { role -> nav.navigate("${Routes.LOGIN}/${role.name}") }
        }
        composable("${Routes.LOGIN}/{role}") { entry ->
            val roleParam = entry.arguments?.getString("role")
            val role = when (roleParam) {
                UserRole.VENDOR.name -> UserRole.VENDOR
                UserRole.DELIVERY_BOY.name, UserRole.DELIVERY_PERSON.name -> UserRole.DELIVERY_BOY
                else -> UserRole.CUSTOMER
            }
            LoginScreen(role == UserRole.VENDOR, { nav.navigate(Routes.REGISTER) }, { nav.navigate(Routes.FORGOT) }) {
                viewModel.signIn(role, it)
                val destination = when (role) {
                    UserRole.VENDOR -> Routes.DASHBOARD
                    UserRole.DELIVERY_BOY -> Routes.DELIVERY_BOY_DASHBOARD
                    else -> Routes.MAP
                }
                nav.navigate(destination) { popUpTo(Routes.ROLE) { inclusive = true } }
            }
        }
        composable(Routes.REGISTER) {
            RegisterScreen { name ->
                viewModel.signIn(UserRole.CUSTOMER, name)
                nav.navigate(Routes.MAP) { popUpTo(Routes.ROLE) { inclusive = true } }
            }
        }
        composable(Routes.FORGOT) { ForgotPasswordScreen { nav.popBackStack() } }
        composable(Routes.MAP) { CustomerHome(state, viewModel, nav) }
        composable(Routes.CHATS) { ChatScreen(state, viewModel, nav) }
        composable(Routes.ACTIVITY) { ActivityScreen(state, nav) }
        composable(Routes.PROFILE) { ProfileScreen(state.userName, state.role ?: UserRole.CUSTOMER, state.selectedLanguage, nav) }
        composable(Routes.VENDOR_DETAILS) { VendorDetailsScreen(state, viewModel, nav) }
        composable(Routes.PAYMENT) { PaymentScreen(state, viewModel, nav) }
        composable(Routes.TRACKING) { TrackingScreen(state.deliveryRequest, nav) }
        composable(Routes.DASHBOARD) { VendorDashboard(state, viewModel, nav) }
        composable(Routes.DELIVERY_BOY_DASHBOARD) { DeliveryBoyDashboardScreen(state, viewModel, nav) }
        composable(Routes.DELIVERY_REQUESTS) { DeliveryRequestsScreen(state, viewModel, nav) }
        composable(Routes.DELIVERY_REQUEST_DETAILS) { DeliveryRequestDetailsScreen(state, viewModel, nav) }
        composable(Routes.DELIVERY_PICKUP) { DeliveryPickupScreen(state, viewModel, nav) }
        composable(Routes.DELIVERY_WALLET) { DeliveryWalletScreen(state, nav) }
    }
}

@Composable
private fun LanguageScreen(selected: AppLanguage?, select: (AppLanguage) -> Unit, continueAction: () -> Unit) {
    var visible by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { AppLanguage.entries.indices.forEach { delay(90); visible = it + 1 } }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF5E4), Color(0xFFFFFBF7)))).windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(Modifier.size(76.dp), CircleShape, MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📍", style = MaterialTheme.typography.headlineMedium) }
        }
        Spacer(Modifier.size(14.dp))
        Text("VendorMap", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text("Find a nearby street vendor, chat, pay, and get it delivered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(34.dp))
        Text("Choose Your Language", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Select a language to get started", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(24.dp))
        AppLanguage.entries.forEachIndexed { index, language ->
            AnimatedVisibility(visible = visible > index, enter = fadeIn() + slideInVertically { it / 2 }, modifier = Modifier.padding(bottom = 12.dp)) {
                LanguageCard(language, selected == language, { select(language) })
            }
        }
        AppButton("Continue", continueAction, Modifier.fillMaxWidth(), selected != null)
        Spacer(Modifier.size(12.dp))
    }
}

@Composable
private fun RoleScreen(onRole: (UserRole) -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(22.dp)) {
        Spacer(Modifier.height(35.dp))
        Text("Welcome to VendorMap", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("How would you like to use the app?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(30.dp))
        RoleCard("Customer", "Find nearby vendors, chat, and arrange delivery from the map.", "🧭") { onRole(UserRole.CUSTOMER) }
        Spacer(Modifier.height(16.dp))
        RoleCard("Vendor", "Go live, chat with customers, and trigger delivery requests.", "🏪") { onRole(UserRole.VENDOR) }
        Spacer(Modifier.height(16.dp))
        RoleCard("Delivery Boy", "Accept delivery jobs, navigate to vendors and customers, and track earnings.", "🚴") { onRole(UserRole.DELIVERY_BOY) }
    }
}

@Composable
private fun RoleCard(title: String, body: String, emoji: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LoginScreen(isVendor: Boolean, register: () -> Unit, forgot: () -> Unit, login: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AuthScaffold(if (isVendor) "Vendor sign in" else "Welcome back", "Your neighborhood network is ready.") {
        AppTextField(name, { name = it }, "Full name", leadingIcon = Icons.Default.Person)
        Spacer(Modifier.height(12.dp))
        AppTextField(phone, { phone = it }, "Phone number", leadingIcon = Icons.Default.Phone)
        Spacer(Modifier.height(12.dp))
        AppTextField(password, { password = it }, "Password", leadingIcon = Icons.Default.Lock, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(10.dp))
        AppButton("Sign in", { login(name) }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("New here? ")
            Text("Create account", Modifier.clickable(onClick = register), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegisterScreen(done: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AuthScaffold("Create your account", "A few details and you are ready to begin.") {
        AppTextField(name, { name = it }, "Full name", leadingIcon = Icons.Default.Person)
        Spacer(Modifier.height(20.dp))
        AppButton("Create account", { done(name) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ForgotPasswordScreen(back: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    AuthScaffold("Reset password", "We will send a reset link to your phone.") {
        AppTextField(phone, { phone = it }, "Phone number", leadingIcon = Icons.Default.Phone)
        Spacer(Modifier.height(20.dp))
        AppButton("Send reset link", back, Modifier.fillMaxWidth())
    }
}

@Composable
private fun AuthScaffold(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(22.dp)) {
        Spacer(Modifier.height(42.dp))
        Surface(Modifier.size(62.dp), CircleShape, MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(30.dp))
        content()
    }
}

@Composable
private fun CustomerHome(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    Scaffold(bottomBar = { BottomNavigation("map") { route -> vm.setCurrentRoute(route); nav.navigate(route) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Namaste, ${state.userName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text("Nearby vendors", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            SearchBox(state.searchQuery) { vm.searchVendors(it) }
            Spacer(Modifier.height(12.dp))
            MapCard(state.vendors, state.activeVendor) { vendor ->
                vm.selectVendor(vendor)
                nav.navigate(Routes.VENDOR_DETAILS)
            }
            Spacer(Modifier.height(16.dp))
            SectionHeader("Nearby vendors")
            Spacer(Modifier.height(12.dp))
            state.vendors.take(4).forEach { vendor ->
                VendorTile(vendor) {
                    vm.selectVendor(vendor)
                    nav.navigate(Routes.VENDOR_DETAILS)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ChatScreen(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    var draft by remember { mutableStateOf("") }
    Scaffold(bottomBar = { BottomNavigation("chats") { route -> vm.setCurrentRoute(route); nav.navigate(route) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Chats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (state.safetyWarning != null) Text("Safety alert", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            if (state.safetyWarning != null) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(state.safetyWarning, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(Modifier.height(12.dp))
            }
            state.activeChat.forEach { message ->
                ChatBubble(message)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(draft, { draft = it }, "Message vendor", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                AppButton("Send", { vm.sendChatMessage(draft); draft = "" }, enabled = draft.isNotBlank())
            }
            Spacer(Modifier.height(10.dp))
            AppButton("Request delivery", { nav.navigate(Routes.PAYMENT) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ActivityScreen(state: AppUiState, nav: NavHostController) {
    Scaffold(bottomBar = { BottomNavigation("activity") { nav.navigate(it) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            ActivityCard("Delivery request", state.deliveryRequest.status.label, "From ${state.deliveryRequest.vendorName}")
            ActivityCard("Payment", state.payment.status.label, "₹${state.payment.amount} paid through platform")
            ActivityCard("Wallet", "Available", "₹${state.wallet.availableBalance} available balance")
            Spacer(Modifier.height(10.dp))
            AppButton("Track order", { nav.navigate(Routes.TRACKING) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProfileScreen(name: String, role: UserRole, language: AppLanguage?, nav: NavHostController) {
    Scaffold(bottomBar = { BottomNavigation("profile") { nav.navigate(it) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(56.dp), CircleShape, MaterialTheme.colorScheme.primary) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (role == UserRole.VENDOR) "Vendor account" else "Customer account")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            ProfileRow(Icons.Default.Phone, "Phone", "+91 98765 43210")
            ProfileRow(Icons.Default.Language, "Language", language?.englishName ?: "English")
            ProfileRow(Icons.Default.ReceiptLong, "Orders", "Delivery history")
            ProfileRow(Icons.Default.Settings, "Settings", "Notifications and preferences")
        }
    }
}

@Composable
private fun VendorDetailsScreen(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    val vendor = state.activeVendor ?: MockRepository.vendors.first()
    Scaffold(topBar = { BackHeader(vendor.name, nav) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            VendorHeader(vendor)
            Spacer(Modifier.height(16.dp))
            Text("Current status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(vendor.status.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Location: ${vendor.location}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Distance: ${vendor.distance}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            AppButton("Chat with vendor", { vm.openVendorChat(vendor.id); nav.navigate(Routes.CHATS) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            AppButton("Request delivery", { vm.createDeliveryRequest(vendor.id); nav.navigate(Routes.PAYMENT) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PaymentScreen(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    Scaffold(topBar = { BackHeader("Payment summary", nav) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Vendor/item cost: ₹${state.payment.amount}", style = MaterialTheme.typography.bodyLarge)
            Text("Delivery cost: ₹${state.deliveryRequest.deliveryFee}", style = MaterialTheme.typography.bodyLarge)
            Text("Platform fee: ₹${state.deliveryRequest.platformFee}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("₹${state.payment.amount + state.deliveryRequest.deliveryFee + state.deliveryRequest.platformFee}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
            AppButton("Pay securely", { vm.confirmPayment(); nav.navigate(Routes.TRACKING) }, Modifier.fillMaxWidth(), icon = Icons.Default.Payment)
            Spacer(Modifier.height(10.dp))
            AppButton("Test payment flow", { vm.confirmPayment(); nav.navigate(Routes.TRACKING) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TrackingScreen(deliveryRequest: com.vendorapp.data.DeliveryRequest, nav: NavHostController) {
    Scaffold(topBar = { BackHeader("Delivery tracking", nav) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(deliveryRequest.vendorName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(deliveryRequest.itemSummary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            val steps = OrderStatus.entries.filter { it != OrderStatus.CANCELLED && it != OrderStatus.PAYMENT_FAILED && it != OrderStatus.DELIVERY_FAILED }
            val currentIndex = steps.indexOf(deliveryRequest.status).coerceAtLeast(0)
            LinearProgressIndicator(progress = { (currentIndex + 1).toFloat() / steps.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            steps.forEachIndexed { index, status ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(28.dp), CircleShape, if (index <= currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (index <= currentIndex) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(status.label, fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun VendorDashboard(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    Scaffold(bottomBar = { NavigationBar { listOf(Triple(Routes.DASHBOARD, "Dashboard", Icons.Default.Storefront), Triple(Routes.DELIVERY_WALLET, "Wallet", Icons.Default.TrendingUp), Triple(Routes.PROFILE, "Profile", Icons.Default.Person)).forEach { (route, label, icon) -> NavigationBarItem(selected = route == Routes.DASHBOARD, onClick = { if (route == Routes.PROFILE) nav.navigate(route) else if (route == Routes.DELIVERY_WALLET) nav.navigate(route) }, icon = { Icon(icon, label) }, label = { Text(label) }) } } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Good morning, ${state.userName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Live vendor dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Online", if (state.deliveryAvailable) "Live" else "Away")
                StatCard("Chats", "3")
                StatCard("Wallet", "₹${state.wallet.availableBalance}")
            }
            Spacer(Modifier.height(18.dp))
            SectionHeader("Delivery status")
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current location status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Your location is being monitored. A 50m movement rule is active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    AppButton(if (state.deliveryAvailable) "Go offline" else "Go online", { vm.toggleAvailability() }, Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionHeader("Incoming delivery requests")
            Spacer(Modifier.height(8.dp))
            ActivityCard("Pani puri order", state.deliveryRequest.status.label, "Customer: ${state.deliveryRequest.customerName}")
        }
    }
}

@Composable
private fun DeliveryBoyDashboardScreen(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    Scaffold(bottomBar = { NavigationBar {
        listOf(
            Triple(Routes.DELIVERY_BOY_DASHBOARD, "Dashboard", Icons.Default.Storefront),
            Triple(Routes.DELIVERY_REQUESTS, "Available", Icons.Default.LocationOn),
            Triple(Routes.DELIVERY_WALLET, "Wallet", Icons.Default.TrendingUp),
            Triple(Routes.PROFILE, "Profile", Icons.Default.Person)
        ).forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = route == Routes.DELIVERY_BOY_DASHBOARD || (route == Routes.PROFILE && state.currentRoute == Routes.PROFILE),
                onClick = {
                    when (route) {
                        Routes.DELIVERY_BOY_DASHBOARD -> nav.navigate(Routes.DELIVERY_BOY_DASHBOARD) { launchSingleTop = true }
                        Routes.DELIVERY_REQUESTS -> nav.navigate(Routes.DELIVERY_REQUESTS) { launchSingleTop = true }
                        Routes.DELIVERY_WALLET -> nav.navigate(Routes.DELIVERY_WALLET) { launchSingleTop = true }
                        Routes.PROFILE -> nav.navigate(Routes.PROFILE) { launchSingleTop = true }
                    }
                },
                icon = { Icon(icon, label) },
                label = { Text(label) }
            )
        }
    } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Good morning, ${state.userName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Delivery boy dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(16.dp), color = if (state.deliveryBoyOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                    Text(if (state.deliveryBoyOnline) "● ONLINE" else "○ OFFLINE", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Today's earnings", "₹${state.wallet.earnedDeliveryFees}")
                StatCard("Completed", "${state.myDeliveries.count { it.status == OrderStatus.COMPLETED }}")
                StatCard("Pending", "${state.deliveryRequests.size}")
            }
            Spacer(Modifier.height(18.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(16.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (state.deliveryBoyOnline) "Available for deliveries" else "Not accepting deliveries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    AppButton(if (state.deliveryBoyOnline) "Go offline" else "Go online", { vm.toggleDeliveryBoyOnline() }, Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionHeader("Available requests")
            Spacer(Modifier.height(8.dp))
            state.deliveryRequests.take(2).forEach { request ->
                DeliveryRequestCompactCard(request, onClick = { nav.navigate(Routes.DELIVERY_REQUEST_DETAILS) }, onAccept = { vm.acceptDeliveryRequest(request.id) })
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(18.dp))
            SectionHeader("My deliveries")
            Spacer(Modifier.height(8.dp))
            if (state.activeDelivery != null) {
                ActivityCard("Active delivery", state.activeDelivery.status.label, "${state.activeDelivery.vendorName} → ${state.activeDelivery.customerName}")
            } else {
                EmptyState("No active delivery", "Accept a request to begin a delivery run.")
            }
        }
    }
}

@Composable
private fun DeliveryRequestsScreen(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    Scaffold(topBar = { BackHeader("Available requests", nav) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (state.deliveryRequests.isEmpty()) {
                EmptyState("No requests", "There are no active delivery requests right now.")
            } else {
                state.deliveryRequests.forEach { request ->
                    DeliveryRequestCompactCard(request, onClick = { nav.navigate(Routes.DELIVERY_REQUEST_DETAILS) }, onAccept = { vm.acceptDeliveryRequest(request.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DeliveryRequestCompactCard(request: DeliveryRequest, onClick: () -> Unit, onAccept: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(request.id, fontWeight = FontWeight.Bold)
                Text(request.status.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(request.vendorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Vendor: ${request.vendorLocation} • Customer: ${request.customerLocation}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Distance: ${request.distance} • Delivery fee: ₹${request.deliveryFee}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("View details", onClick, Modifier.weight(1f))
                AppButton("Accept delivery", onAccept, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeliveryRequestDetailsScreen(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    val request = state.activeDelivery ?: state.deliveryRequest
    Scaffold(topBar = { BackHeader("Delivery details", nav) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Delivery #${request.id}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(16.dp)) {
                    Text("Vendor: ${request.vendorName}", fontWeight = FontWeight.Bold)
                    Text("Location: ${request.vendorLocation} • ${request.distance}")
                    Spacer(Modifier.height(8.dp))
                    Text("Customer: ${request.customerName}", fontWeight = FontWeight.Bold)
                    Text("Delivery location: ${request.customerLocation}")
                    Spacer(Modifier.height(8.dp))
                    Text("Request: ${request.itemSummary}")
                    Text("Vendor confirmed cost: ₹${request.amount}")
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(16.dp)) {
                    Text("Payment summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Vendor/item cost: ₹${request.amount}")
                    Text("Delivery fee: ₹${request.deliveryFee}")
                    Text("Platform fee: ₹${request.platformFee}")
                    Text("Total: ₹${request.amount + request.deliveryFee + request.platformFee}")
                    Text("Delivery earning: ₹${request.deliveryEarning}")
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("Accept delivery", { vm.acceptDeliveryRequest(request.id); nav.navigate(Routes.DELIVERY_PICKUP) }, Modifier.weight(1f))
                AppButton("Reject", { nav.popBackStack() }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeliveryPickupScreen(state: AppUiState, vm: AppViewModel, nav: NavHostController) {
    val delivery = state.activeDelivery ?: state.deliveryRequest
    val currentStatus = delivery.status
    val currentStep = OrderStatus.entries.indexOf(currentStatus).coerceAtLeast(0)
    Scaffold(topBar = { BackHeader("Pickup flow", nav) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(delivery.vendorName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(delivery.vendorLocation, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(currentStatus.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { (currentStep + 1).toFloat() / OrderStatus.entries.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
            val nextAction = when (currentStatus) {
                OrderStatus.DELIVERY_ASSIGNED -> "Start pickup"
                OrderStatus.GOING_TO_VENDOR -> "I've arrived"
                OrderStatus.ARRIVED_AT_VENDOR -> "Start collection"
                OrderStatus.COLLECTING_ITEM -> "Item collected"
                OrderStatus.ITEM_COLLECTED -> "Start delivery"
                OrderStatus.OUT_FOR_DELIVERY -> "I've arrived"
                OrderStatus.ARRIVED_AT_CUSTOMER -> "Mark delivered"
                else -> "Update status"
            }
            AppButton(nextAction, {
                val nextStatus = when (currentStatus) {
                    OrderStatus.DELIVERY_ASSIGNED -> OrderStatus.GOING_TO_VENDOR
                    OrderStatus.GOING_TO_VENDOR -> OrderStatus.ARRIVED_AT_VENDOR
                    OrderStatus.ARRIVED_AT_VENDOR -> OrderStatus.COLLECTING_ITEM
                    OrderStatus.COLLECTING_ITEM -> OrderStatus.ITEM_COLLECTED
                    OrderStatus.ITEM_COLLECTED -> OrderStatus.OUT_FOR_DELIVERY
                    OrderStatus.OUT_FOR_DELIVERY -> OrderStatus.ARRIVED_AT_CUSTOMER
                    OrderStatus.ARRIVED_AT_CUSTOMER -> OrderStatus.DELIVERED
                    else -> OrderStatus.COMPLETED
                }
                vm.updateActiveDelivery(nextStatus)
                if (nextStatus == OrderStatus.DELIVERED || nextStatus == OrderStatus.COMPLETED) {
                    nav.navigate(Routes.DELIVERY_WALLET) { launchSingleTop = true }
                }
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            AppButton("Navigate to vendor", { /* external map navigation placeholder */ }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DeliveryWalletScreen(state: AppUiState, nav: NavHostController) {
    Scaffold(topBar = { BackHeader("Delivery wallet", nav) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(20.dp)) {
                    Text("Current wallet balance", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("₹${state.wallet.availableBalance}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Today's earnings: ₹${state.wallet.earnedDeliveryFees}")
            Text("Total earnings: ₹${state.wallet.earnedDeliveryFees}")
            Text("Pending earnings: ₹${state.wallet.reservedAmount}")
            Text("Completed earnings: ₹${state.wallet.availableBalance}")
            Spacer(Modifier.height(16.dp))
            ActivityCard("Delivery #12345", "Delivery earnings", "+₹${state.wallet.earnedDeliveryFees}")
        }
    }
}

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(10.dp))
            androidx.compose.material3.TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search vendors or locations") },
                colors = androidx.compose.material3.TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
            )
        }
    }
}

@Composable
private fun MapCard(vendors: List<Vendor>, activeVendor: Vendor?, onSelect: (Vendor) -> Unit) {
    Surface(Modifier.fillMaxWidth().height(270.dp), RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF9AD0C2), Color(0xFFDFF7F5), Color(0xFFE9F6FF)))) ) {
            Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Text("Map view", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Current location • 2.1 km radius", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.align(Alignment.Center).size(220.dp), contentAlignment = Alignment.Center) {
                repeat(4) { index ->
                    Surface(Modifier.align(if (index % 2 == 0) Alignment.TopCenter else Alignment.BottomCenter).size(32.dp).clickable { activeVendor?.let(onSelect) }, shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {}
                }
            }
            vendors.take(3).forEachIndexed { index, vendor ->
                val x = 18 + index * 28
                val y = 44 + index * 20
                Box(Modifier.align(Alignment.TopStart).padding(start = x.dp, top = y.dp)) {
                    Surface(Modifier.size(38.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(vendor.imageEmoji, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
            activeVendor?.let {
                Surface(Modifier.align(Alignment.BottomStart).padding(16.dp).clickable { onSelect(it) }, RoundedCornerShape(18.dp), MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(it.name, fontWeight = FontWeight.Bold)
                        Text("${it.status.label} • ${it.distance}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorTile(vendor: Vendor, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = click), RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(52.dp), RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(vendor.imageEmoji, style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(vendor.name, fontWeight = FontWeight.Bold)
                Text(vendor.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(" ${vendor.location} • ${vendor.distance}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(vendor.status.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VendorHeader(vendor: Vendor) {
    Surface(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
        Row(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant))).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(vendor.imageEmoji, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.size(18.dp))
            Column {
                Text(vendor.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(vendor.category)
                Text("${vendor.status.label} • ${vendor.distance}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val background = if (message.isFromCustomer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isFromCustomer) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromCustomer) Arrangement.End else Arrangement.Start
    ) {
        Surface(Modifier.fillMaxWidth(0.78f), RoundedCornerShape(18.dp), color = background) {
            Column(Modifier.padding(12.dp)) {
                Text(message.sender, style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.8f))
                Text(message.text, color = textColor)
                Text(message.timestamp, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ActivityCard(title: String, status: String, detail: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BackHeader(title: String, nav: NavHostController) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(16.dp))
        Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(Modifier.width(108.dp), RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

