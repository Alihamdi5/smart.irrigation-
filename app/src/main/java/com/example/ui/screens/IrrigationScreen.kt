package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Evaporation
import com.example.data.model.Farm
import com.example.ui.viewmodel.FarmMetrics
import com.example.ui.viewmodel.IrrigationViewModel
import com.example.util.JalaliCalendarHelper
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IrrigationScreen(
    viewModel: IrrigationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val farms by viewModel.farms.collectAsStateWithLifecycle()
    val evaporations by viewModel.evaporations.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Farms, 2: Evaporation

    // Agricultural Organic Dark Green Palette
    val deepOrganicBg = Color(0xFF0C1916) // Earth deep charcoal green
    val surfaceGreenCard = Color(0xFF142622) // Lighter jungle accent
    val primaryGoldWheat = Color(0xFFFFB300) // Wheat energy accent
    val hydrationBlue = Color(0xFF29B6F6) // Fresh clean water blue
    val leafGreen = Color(0xFF66BB6A) // Sugarcane vibrancy green
    val clayRed = Color(0xFFE57373) // Alert dry soil color

    // Reactive calculations derived from farms state
    val farmMetrics = farms.map { it to viewModel.calculateMetrics(it) }
    val totalFarms = farms.size
    val criticalFarms = farmMetrics.count { it.second.status == "بحرانی" }
    val warningFarms = farmMetrics.count { it.second.status == "هشدار" }
    val totalCumulativeEvap = farmMetrics.sumOf { it.second.cumulativeEvap }

    val bannerText = when {
        criticalFarms > 0 -> "هشدار: $criticalFarms مزرعه در وضعیت بحرانی هستند — آبیاری را فوراً آغاز کنید"
        warningFarms > 0 -> "توجه: $warningFarms مزرعه نزدیک به آستانه بحرانی تبخیر آب هستند"
        else -> "وضعیت تمام مزارع مناسب و پایدار است"
    }

    val bannerColor = when {
        criticalFarms > 0 -> clayRed
        warningFarms > 0 -> primaryGoldWheat
        else -> leafGreen
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(deepOrganicBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Main Iranian Toolbar Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceGreenCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Jalali Today Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1B18))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Today Date Icon",
                                    tint = primaryGoldWheat,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = JalaliCalendarHelper.getTodayJalali(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // App Display Logo
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "سامانه هوشمند مدیریت آبیاری نیشکر",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "پایش علمی تبخیر آب مزارع نیشکر",
                                style = MaterialTheme.typography.labelSmall,
                                color = leafGreen,
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Alert Dashboard Announcement Banner (Replaces Python banner_text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bannerColor.copy(alpha = 0.12f))
                            .border(1.dp, bannerColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = when {
                                criticalFarms > 0 -> Icons.Default.Warning
                                warningFarms > 0 -> Icons.Default.Info
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = "Status Banner Icon",
                            tint = bannerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Material 3 Responsive Navigation Pills (Dashboard / Farms / Evaporation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A1412))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "داشبورد پایش", Icons.Default.Dashboard),
                    Triple(1, "مدیریت مزارع", Icons.Default.Agriculture),
                    Triple(2, "ثبت تبخیر آب روزانه", Icons.Default.WaterDrop)
                )

                tabs.forEach { (index, title, icon) ->
                    val isActive = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) leafGreen else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp)
                            .testTag("nav_tab_button_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "$title Tab",
                                tint = if (isActive) Color.Black else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isActive) Color.Black else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Tab Content Frame with Smooth Content Swapping
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "tab_navigation_animated"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> DashboardView(
                            farmsWithMetrics = farmMetrics,
                            totalFarms = totalFarms,
                            criticalCount = criticalFarms,
                            totalCumEvap = totalCumulativeEvap,
                            onGoToFarms = { selectedTab = 1 }
                        )
                        1 -> FarmsManagementView(
                            viewModel = viewModel,
                            farms = farms,
                            onToast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                        2 -> EvaporationManagementView(
                            viewModel = viewModel,
                            evaporations = evaporations,
                            onToast = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }

            // Elegant Bottom Signature Footer for Ali Hamdi (توسعه‌دهنده)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF091412))
                    .border(BorderStroke(1.dp, Color(0xFF142622)))
                    .padding(vertical = 12.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "علی حمدی",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryGoldWheat
                    )
                    Text(
                        text = "طراح و تهیه کننده:",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Developer Ali Hamdi Signature",
                        tint = leafGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==================== VIEW 0: DASHBOARD ====================
@Composable
fun DashboardView(
    farmsWithMetrics: List<Pair<Farm, FarmMetrics>>,
    totalFarms: Int,
    criticalCount: Int,
    totalCumEvap: Double,
    onGoToFarms: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High-Quality Analytics Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Evaporation
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142622)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("کل تبخیر آب تجمعی", fontSize = 11.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f میلی‌متر", totalCumEvap),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF29B6F6),
                            textAlign = TextAlign.Right
                        )
                    }
                }

                // Critical Count
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142622)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("مزارع بحرانی", fontSize = 11.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$criticalCount مزرعه",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (criticalCount > 0) Color(0xFFE57373) else Color(0xFF66BB6A),
                            textAlign = TextAlign.Right
                        )
                    }
                }

                // Total Farms
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF142622)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("کل مزارع ثبت‌شده", fontSize = 11.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalFarms واحد",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }

        // Subheader Title
        item {
            Text(
                text = "وضعیت جاری قطعات کشت نیشکر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Right
            )
        }

        if (farmsWithMetrics.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = "No Farm Placeholder",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "هنوز هیچ مزرعه‌ای ثبت نگردیده است.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = onGoToFarms,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))
                        ) {
                            Text("افزودن اولین مزرعه نیشکر", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(farmsWithMetrics) { (farm, metrics) ->
                FarmOverviewCard(farm = farm, metrics = metrics)
            }
        }
    }
}

@Composable
fun FarmOverviewCard(farm: Farm, metrics: FarmMetrics) {
    val leafGreen = Color(0xFF66BB6A)
    val hydrationBlue = Color(0xFF29B6F6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("farm_overview_card_${farm.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142622)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(metrics.statusColor).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Farm Title and Size Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(metrics.statusColor).copy(alpha = 0.15f))
                        .border(1.dp, Color(metrics.statusColor).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = metrics.status,
                        color = Color(metrics.statusColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Farm Details
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = farm.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "هکتار آبیاری: ${farm.area} هکتار",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Percentage Gauge & Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Gauge
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progressValue = if (farm.target_cpe > 0) {
                        (metrics.cumulativeEvap / farm.target_cpe).toFloat().coerceIn(0f, 1.2f)
                    } else {
                        0f
                    }
                    CircularProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(metrics.statusColor),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFF0F1B18)
                    )
                    Text(
                        text = "${metrics.percentage}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Cumulative stats column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "تبخیر آب تجمعی: ${metrics.cumulativeEvap} میلی‌متر",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "حد مجاز آبیاری (CPE): ${farm.target_cpe} میلی‌متر",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "آخرین آبیاری: ${farm.last_irrigation}",
                        color = primaryGoldStateColor(metrics.status),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = metrics.message,
                        color = Color(metrics.statusColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun primaryGoldStateColor(status: String): Color {
    return when (status) {
        "بحرانی" -> Color(0xFFE57373)
        "هشدار" -> Color(0xFFFFB300)
        else -> Color(0xFF66BB6A)
    }
}

// ==================== VIEW 1: FARMS MANAGEMENT ====================
@Composable
fun FarmsManagementView(
    viewModel: IrrigationViewModel,
    farms: List<Farm>,
    onToast: (String) -> Unit
) {
    val leafGreen = Color(0xFF66BB6A)
    val clayRed = Color(0xFFE57373)
    val sunflowerGold = Color(0xFFFFB300)

    var selectedFarmId by remember { mutableStateOf(0) }
    var inName by remember { mutableStateOf("") }
    var inArea by remember { mutableStateOf("") }
    var inCpe by remember { mutableStateOf("") }
    var inLastIrrigation by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    // Helper to clear form
    fun clearForm() {
        selectedFarmId = 0
        inName = ""
        inArea = ""
        inCpe = ""
        inLastIrrigation = JalaliCalendarHelper.getTodayJalali()
        focusManager.clearFocus()
    }

    // Set initial date on enter
    LaunchedEffect(Unit) {
        if (inLastIrrigation.isEmpty()) {
            inLastIrrigation = JalaliCalendarHelper.getTodayJalali()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Upper Farm Input Card Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("farm_form_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142622)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (selectedFarmId == 0) "ثبت مزرعه نیشکر جدید" else "ویرایش اطلاعات مزرعه نیشکر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Farm Name Input (Persian alignment hint)
                    OutlinedTextField(
                        value = inName,
                        onValueChange = { inName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farm_name_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            textAlign = TextAlign.Right,
                            textDirection = TextDirection.ContentOrRtl
                        ),
                        placeholder = { Text("نام مزرعه یا شماره قطعه کشت", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = leafGreen,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    // Hectares area & CPE limits row inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // CPE Input
                        OutlinedTextField(
                            value = inCpe,
                            onValueChange = { inCpe = it },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("farm_cpe_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                            placeholder = { Text("CPE هدف (mm)", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = leafGreen,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )

                        // Area Input (Hectares)
                        OutlinedTextField(
                            value = inArea,
                            onValueChange = { inArea = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("farm_area_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                            placeholder = { Text("هکتار آبیاری", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = leafGreen,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                    }

                    // Last Irrigation date string
                    OutlinedTextField(
                        value = inLastIrrigation,
                        onValueChange = { inLastIrrigation = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farm_last_irrigation_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                        label = { Text("تاریخ آخرین آبیاری (مثال: YYYY/MM/DD)", color = Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = leafGreen,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    // Control Buttons Block (Save / Restart / Delete / Reset Form)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reset Form Button
                        OutlinedButton(
                            onClick = { clearForm() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("پاک کردن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Save Button (Submit added/edited farm)
                        Button(
                            onClick = {
                                val areaParsed = inArea.toDoubleOrNull() ?: 0.0
                                val cpeParsed = inCpe.toDoubleOrNull() ?: 0.0
                                viewModel.saveFarm(selectedFarmId, inName, areaParsed, cpeParsed, inLastIrrigation) { errMsg ->
                                    onToast(errMsg)
                                    if (errMsg.contains("موفقیت")) {
                                        clearForm()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(2f)
                                .testTag("save_farm_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = leafGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (selectedFarmId == 0) "ذخیره مزرعه" else "اعمال تغییرات",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Special Secondary Buttons: Reset Irrigation cycle and Delete Farm
                    if (selectedFarmId != 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Reset Irrigation
                            Button(
                                onClick = {
                                    viewModel.restartIrrigation(selectedFarmId) { resMsg ->
                                        onToast(resMsg)
                                        clearForm()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("restart_irrigation_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = sunflowerGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Water reset icon", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Text("آبیاری مجدد", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Delete Farm
                            Button(
                                onClick = {
                                    viewModel.deleteFarm(selectedFarmId) { delMsg ->
                                        onToast(delMsg)
                                        clearForm()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("delete_farm_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = clayRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("حذف مزرعه", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Farm Directory Selection Scroll List
        item {
            Text(
                text = "لیست مزارع ثبت شده جهت پیکربندی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        if (farms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = "هیچ مزرعه‌ای یافت نشد.",
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(farms) { farm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedFarmId = farm.id
                            inName = farm.name
                            inArea = farm.area.toString()
                            inCpe = farm.target_cpe.toString()
                            inLastIrrigation = farm.last_irrigation
                        }
                        .testTag("farm_list_item_${farm.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFarmId == farm.id) Color(0xFF1F3530) else Color(0xFF142622)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (selectedFarmId == farm.id) BorderStroke(1.dp, leafGreen) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Direct Select Button indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedFarmId == farm.id) leafGreen else Color(0xFF0F1B18))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedFarmId == farm.id) "انتخاب شده" else "ویرایش",
                                color = if (selectedFarmId == farm.id) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Details Summary
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = farm.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "آبیاری: ${farm.area} هکتار  |  هدف: ${farm.target_cpe} میلی‌متر  |  تاریخ: ${farm.last_irrigation}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== VIEW 2: EVAPORATION REGISTER ====================
@Composable
fun EvaporationManagementView(
    viewModel: IrrigationViewModel,
    evaporations: List<Evaporation>,
    onToast: (String) -> Unit
) {
    val leafGreen = Color(0xFF66BB6A)
    val clayRed = Color(0xFFE57373)

    var editingId by remember { mutableStateOf(0) }
    var inDate by remember { mutableStateOf("") }
    var inEvap by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (inDate.isEmpty()) {
            inDate = JalaliCalendarHelper.getTodayJalali()
        }
    }

    fun clearForm() {
        editingId = 0
        inDate = JalaliCalendarHelper.getTodayJalali()
        inEvap = ""
        focusManager.clearFocus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Evaporation Input panel form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("evap_form_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142622)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingId == 0) "ثبت میزان تبخیر آب روزانه" else "ویرایش مقدار تبخیر آب ثبت‌شده",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Jalali Day Input Date
                    OutlinedTextField(
                        value = inDate,
                        onValueChange = { inDate = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("evap_date_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                        label = { Text("تاریخ ثبت تبخیر آب (مثال: YYYY/MM/DD)", color = Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = leafGreen,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    // Evaporation amount float
                    OutlinedTextField(
                        value = inEvap,
                        onValueChange = { inEvap = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("evap_value_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                        label = { Text("میزان تبخیر آب روزانه (میلی‌متر)", color = Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = leafGreen,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    // Submit controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clear Form Button
                        OutlinedButton(
                            onClick = { clearForm() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("پاک کردن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Submit Save/Update
                        Button(
                            onClick = {
                                val evapDouble = inEvap.toDoubleOrNull()
                                if (evapDouble == null) {
                                    onToast("خطا: مقدار تبخیر آب ارسالی باید عددی معتبر باشد.")
                                } else {
                                    viewModel.saveEvaporation(editingId, inDate, evapDouble) { resultMsg ->
                                        onToast(resultMsg)
                                        if (resultMsg.contains("افزوده") || resultMsg.contains("بروزرسانی") || resultMsg.contains("موفقیت")) {
                                            clearForm()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(2f)
                                .testTag("save_evaporation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = leafGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (editingId == 0) "ثبت و تایید تبخیر آب" else "بروزرسانی رکورد",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Delete item (Shown only during edit mode)
                    if (editingId != 0) {
                        Button(
                            onClick = {
                                viewModel.deleteEvaporation(editingId) { delMsg ->
                                    onToast(delMsg)
                                    clearForm()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_evap_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = clayRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("حذف این رکورد روزانه", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section Title: Saved Readings
        item {
            Text(
                text = "تاریخچه رکوردهای تبخیر آب ثبت‌شده",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }

        if (evaporations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = "هیچ داده‌ای ثبت نگردیده است.",
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(evaporations) { evap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingId = evap.id
                            inDate = evap.date
                            inEvap = evap.evap.toString()
                        }
                        .testTag("evap_list_item_${evap.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (editingId == evap.id) Color(0xFF1F3530) else Color(0xFF142622)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = if (editingId == evap.id) BorderStroke(1.dp, leafGreen) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Direct Edit Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (editingId == evap.id) leafGreen else Color(0xFF0F1B18))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (editingId == evap.id) "در حال ویرایش" else "انتخاب",
                                color = if (editingId == evap.id) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Evaporation stats row text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "${evap.evap} میلی‌متر",
                                color = leafGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = evap.date,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
