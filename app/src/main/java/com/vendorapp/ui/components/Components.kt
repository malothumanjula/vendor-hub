package com.vendorapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vendorapp.data.AppLanguage

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        visualTransformation = visualTransformation,
        singleLine = true
    )
}

@Composable
fun LanguageCard(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (selected) 1.02f else 1f, label = "language card scale")
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
        ),
        elevation = CardDefaults.cardElevation(if (selected) 7.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            ) {
                BoxCenter {
                    Text(
                        language.symbol,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(language.nativeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(language.englishName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = selected, enter = fadeIn() + scaleIn()) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.padding(7.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun VendorCard(name: String, neighborhood: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(52.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)) {
                BoxCenter { Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(neighborhood, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun FoodItemCard(name: String, price: String, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(54.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                BoxCenter { Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(price, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = "Add $name") }
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    BoxCenter(modifier.fillMaxWidth()) { CircularProgressIndicator() }
}

@Composable
fun BottomNavigation(selectedRoute: String, onRouteSelected: (String) -> Unit) {
    val items = listOf(
        Triple("map", "Map", Icons.Default.LocationOn),
        Triple("chats", "Chats", Icons.Default.ReceiptLong),
        Triple("activity", "Activity", Icons.Default.ShoppingBag),
        Triple("profile", "Profile", Icons.Default.Storefront)
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
        items.forEach { (route, label, icon) ->
            val selected = route == selectedRoute
            Column(
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onRouteSelected(route) }.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BoxCenter(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier, contentAlignment = Alignment.Center) { content() }
}
