package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.data.model.UserAddress
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

private data class LebanonDistrict(
    val nameEn: String,
    val nameAr: String,
    val lat: Double,
    val lng: Double
)

private val LEBANON_DISTRICTS = listOf(
    LebanonDistrict("Hamra, Beirut", "الحمرا، بيروت", 33.8963, 35.4819),
    LebanonDistrict("Achrafieh", "الأشرفية", 33.8886, 35.5194),
    LebanonDistrict("Verdun", "فردان", 33.8889, 35.4812),
    LebanonDistrict("Jounieh", "جونيه", 33.9808, 35.6178),
    LebanonDistrict("Saida", "صيدا", 33.5571, 35.3729),
    LebanonDistrict("Tripoli", "طرابلس", 34.4367, 35.8497)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveLocationPickerDialog(
    language: AppLanguage,
    currentPinned: UserAddress,
    savedAddresses: List<UserAddress>,
    onSelectLocation: (UserAddress) -> Unit,
    onSaveAddress: (UserAddress) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fusedClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }

    var lat by remember { mutableDoubleStateOf(currentPinned.latitude) }
    var lng by remember { mutableDoubleStateOf(currentPinned.longitude) }
    var label by remember { mutableStateOf(currentPinned.label.ifBlank { "Home" }) }
    var city by remember { mutableStateOf(currentPinned.city.ifBlank { "Beirut" }) }
    var addressLine by remember { mutableStateOf(currentPinned.addressLine) }
    var phone by remember { mutableStateOf(currentPinned.phone) }
    var gpsStatus by remember { mutableStateOf<String?>(null) }

    @SuppressLint("MissingPermission")
    fun fetchGpsLocation() {
        gpsStatus = if (language == AppLanguage.AR) "جاري تحديد الموقع..." else "Acquiring GPS..."
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    lat = loc.latitude
                    lng = loc.longitude
                    gpsStatus = if (language == AppLanguage.AR) "تم تحديد موقعك بدقة" else "GPS location pinned"
                } else {
                    gpsStatus = if (language == AppLanguage.AR) "تعذر تحديد الموقع، اختر من الخريطة" else "GPS unavailable, tap map to pin"
                }
            }
            .addOnFailureListener {
                gpsStatus = if (language == AppLanguage.AR) "تعذر تحديد الموقع، اختر من الخريطة" else "GPS unavailable, tap map to pin"
            }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation()
        } else {
            gpsStatus = if (language == AppLanguage.AR) "يرجى السماح بصلاحية الموقع أو النقر على الخريطة" else "Grant location permission or tap map"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.AR) "تحديد موقع التوصيل" else "Pin Delivery Location",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // Interactive Tap-to-Pin Map Canvas (Lebanon & Beirut Bounding Box)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    val minLat = 33.40
                    val maxLat = 34.55
                    val minLng = 35.30
                    val maxLng = 35.95

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val xRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                    val yRatio = (offset.y / size.height).coerceIn(0f, 1f)
                                    lng = minLng + xRatio * (maxLng - minLng)
                                    lat = maxLat - yRatio * (maxLat - minLat)
                                    gpsStatus = null
                                }
                            }
                    ) {
                        // Sea & Coastal Land aesthetic
                        drawRect(color = Color(0xFFD6EEF8))
                        drawRect(
                            color = Color(0xFFEAF5EE),
                            topLeft = Offset(size.width * 0.22f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.78f, size.height)
                        )
                        // Grid lines
                        val gridColor = Color(0xFFC5DDD0)
                        for (i in 1..4) {
                            val x = size.width * i / 5f
                            val y = size.height * i / 5f
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                        }
                        // District markers
                        LEBANON_DISTRICTS.forEach { d ->
                            val dx = ((d.lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f) * size.width
                            val dy = ((maxLat - d.lat) / (maxLat - minLat)).toFloat().coerceIn(0.05f, 0.95f) * size.height
                            drawCircle(color = Color(0xFF0D7A4B).copy(alpha = 0.35f), radius = 10f, center = Offset(dx, dy))
                        }

                        // Pinned Crosshair & Drop Pin
                        val px = ((lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.05f, 0.95f) * size.width
                        val py = ((maxLat - lat) / (maxLat - minLat)).toFloat().coerceIn(0.05f, 0.95f) * size.height
                        drawLine(
                            color = Color(0xFFF57C00).copy(alpha = 0.5f),
                            start = Offset(px, 0f),
                            end = Offset(px, size.height),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        drawLine(
                            color = Color(0xFFF57C00).copy(alpha = 0.5f),
                            start = Offset(0f, py),
                            end = Offset(size.width, py),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        drawCircle(color = Color.White, radius = 18f, center = Offset(px, py))
                        drawCircle(color = Color(0xFFF57C00), radius = 13f, center = Offset(px, py))
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${"%.4f".format(lat)}, ${"%.4f".format(lng)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                fetchGpsLocation()
                            } else {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .testTag("use_gps_button")
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (language == AppLanguage.AR) "موقعي GPS" else "My GPS")
                    }
                }

                gpsStatus?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LEBANON_DISTRICTS.forEach { d ->
                        AssistChip(
                            onClick = {
                                lat = d.lat
                                lng = d.lng
                                city = if (language == AppLanguage.AR) d.nameAr else d.nameEn
                                if (addressLine.isBlank()) {
                                    addressLine = city
                                }
                            },
                            label = { Text(if (language == AppLanguage.AR) d.nameAr else d.nameEn) }
                        )
                    }
                }

                if (savedAddresses.isNotEmpty()) {
                    Text(
                        text = if (language == AppLanguage.AR) "العناوين المحفوظة" else "Saved Addresses",
                        style = MaterialTheme.typography.titleSmall
                    )
                    savedAddresses.forEach { saved ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectLocation(saved)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(saved.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${saved.addressLine}, ${saved.city}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteAddress(saved.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(if (language == AppLanguage.AR) "التسمية" else "Label") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(if (language == AppLanguage.AR) "المدينة" else "City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = { Text(if (language == AppLanguage.AR) "الشارع والمبنى والطابق" else "Street, Building, Floor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == AppLanguage.AR) "رقم الهاتف" else "Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val addr = UserAddress(
                                label = label.ifBlank { "Home" },
                                addressLine = addressLine.ifBlank { city },
                                city = city.ifBlank { "Beirut" },
                                phone = phone.ifBlank { "+961 70 000 000" },
                                latitude = lat,
                                longitude = lng,
                                defaultAddress = true
                            )
                            onSaveAddress(addr)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.BookmarkAdded, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (language == AppLanguage.AR) "حفظ واستخدام" else "Save & Pin")
                    }

                    Button(
                        onClick = {
                            onSelectLocation(
                                UserAddress(
                                    label = label.ifBlank { "Pinned" },
                                    addressLine = addressLine.ifBlank { city },
                                    city = city.ifBlank { "Beirut" },
                                    phone = phone.ifBlank { currentPinned.phone },
                                    latitude = lat,
                                    longitude = lng
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language == AppLanguage.AR) "تأكيد الموقع" else "Confirm Pin")
                    }
                }
            }
        }
    }
}
