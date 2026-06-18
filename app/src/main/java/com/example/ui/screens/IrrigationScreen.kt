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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

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
        .sortedByDescending { pair ->
            when (pair.second.status) {
                "بحرانی" -> 4
                "هشدار" -> 3
                "ایمن" -> 2
                "بدون هدف" -> 1
                else -> 0
            }
        }
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

                // Farm Details (Removed irrigation hectares display as requested)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = farm.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "شناسه مزرعه: #${farm.id}",
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

    var searchQuery by remember { mutableStateOf("") }
    var showLastIrrigationDatePicker by remember { mutableStateOf(false) }

    if (showLastIrrigationDatePicker) {
        JalaliWheelDatePickerDialog(
            initialDate = if (inLastIrrigation.isNotEmpty()) inLastIrrigation else JalaliCalendarHelper.getTodayJalali(),
            onDismissRequest = { showLastIrrigationDatePicker = false },
            onDateConfirmed = { date ->
                inLastIrrigation = date
                showLastIrrigationDatePicker = false
            }
        )
    }

    val focusManager = LocalFocusManager.current

    val filteredAndSortedFarms = remember(farms, searchQuery) {
        farms.filter { farm ->
            searchQuery.isEmpty() ||
                farm.name.contains(searchQuery, ignoreCase = true) ||
                farm.id.toString() == searchQuery.trim() ||
                "#${farm.id}" == searchQuery.trim()
        }.sortedByDescending { farm ->
            val metrics = viewModel.calculateMetrics(farm)
            when (metrics.status) {
                "بحرانی" -> 4
                "هشدار" -> 3
                "ایمن" -> 2
                "بدون هدف" -> 1
                else -> 0
            }
        }
    }

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
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF23443B))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small state modifier badge in Persian
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedFarmId == 0) leafGreen.copy(alpha = 0.15f) else sunflowerGold.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (selectedFarmId == 0) "مزرعه جدید" else "در حال ویرایش",
                                color = if (selectedFarmId == 0) leafGreen else sunflowerGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Title with icon
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedFarmId == 0) "تعریف مزرعه نیشکر" else "ویرایش مـزرعـه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = if (selectedFarmId == 0) Icons.Default.Agriculture else Icons.Default.Edit,
                                contentDescription = "Farm Header Icon",
                                tint = leafGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

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
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Name icon",
                                tint = leafGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    // CPE limits & Date Row (Area limit input deleted as requested!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // CPE Input (Target limit)
                        OutlinedTextField(
                            value = inCpe,
                            onValueChange = { inCpe = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("farm_cpe_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                            placeholder = { Text("CPE هدف (mm)", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = leafGreen,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Adjust,
                                    contentDescription = "Target CPE limit icon",
                                    tint = leafGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // Last Irrigation date string (Read-only + Touch triggering rotational wheel picker)
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clickable { showLastIrrigationDatePicker = true }
                        ) {
                            OutlinedTextField(
                                value = inLastIrrigation,
                                onValueChange = { },
                                readOnly = true,
                                enabled = false, // Prevents keyboard focus
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("farm_last_irrigation_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                                placeholder = { Text("تاریخ آخرین آبیاری", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    focusedBorderColor = leafGreen,
                                    unfocusedBorderColor = Color.DarkGray,
                                    disabledTextColor = Color.White,
                                    disabledBorderColor = Color.DarkGray,
                                    disabledPlaceholderColor = Color.Gray
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Date icon",
                                        tint = leafGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Control Buttons Block (Save / Restart / Delete / Reset Form)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reset Form Button
                        OutlinedButton(
                            onClick = { clearForm() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Form icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("انصراف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Save Button (Submit added/edited farm)
                        Button(
                            onClick = {
                                // Defaulting the internal area to 12.0 hectares if empty/new
                                val areaParsed = inArea.toDoubleOrNull() ?: 12.0
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedFarmId == 0) Icons.Default.AddCircleOutline else Icons.Default.Check,
                                    contentDescription = "Save Action icon",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (selectedFarmId == 0) "ذخیره مـزرعـه" else "ثبت تغییرات مـزرعـه",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
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
                                shape = RoundedCornerShape(10.dp)
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
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete icon", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text("حذف مزرعه", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("farm_search_input"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.ContentOrRtl
                ),
                placeholder = {
                    Text(
                        text = "جستجوی مزرعه (نام یا کد شناسه مزرعه)",
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = leafGreen,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    focusedBorderColor = leafGreen,
                    unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            )
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

        if (filteredAndSortedFarms.isEmpty()) {
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
            items(filteredAndSortedFarms) { farm ->
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
                                text = "کد مزرعه: #${farm.id}  |  هدف: ${farm.target_cpe} میلی‌متر  |  تاریخ: ${farm.last_irrigation}",
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
    var showEvapDatePicker by remember { mutableStateOf(false) }

    if (showEvapDatePicker) {
        JalaliWheelDatePickerDialog(
            initialDate = if (inDate.isNotEmpty()) inDate else JalaliCalendarHelper.getTodayJalali(),
            onDismissRequest = { showEvapDatePicker = false },
            onDateConfirmed = { date ->
                inDate = date
                showEvapDatePicker = false
            }
        )
    }

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

                    // Jalali Day Input Date (Read-only + Touch triggering rotational wheel picker)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEvapDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = inDate,
                            onValueChange = { },
                            readOnly = true,
                            enabled = false, // Prevents keyboard focus
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("evap_date_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                            label = { Text("تاریخ ثبت تبخیر آب", color = Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = leafGreen,
                                unfocusedBorderColor = Color.DarkGray,
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.DarkGray,
                                disabledLabelColor = Color.LightGray
                            )
                        )
                    }

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

// ==================== WHEEL DATE PICKER COMPONENTS ====================
@Composable
fun JalaliWheelDatePickerDialog(
    initialDate: String,
    onDismissRequest: () -> Unit,
    onDateConfirmed: (String) -> Unit
) {
    // Parse initial date (e.g., 1403/03/24)
    val parts = initialDate.split("/")
    val initialYear = parts.getOrNull(0)?.toIntOrNull() ?: 1403
    val initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: 1
    val initialDay = parts.getOrNull(2)?.toIntOrNull() ?: 1

    val years = (1400..1410).map { it.toString() }
    val months = (1..12).map { String.format(Locale.US, "%02d", it) }
    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    val days = (1..31).map { String.format(Locale.US, "%02d", it) }

    var selectedYear by remember { mutableStateOf(initialYear.toString()) }
    var selectedMonthIndex by remember { mutableStateOf((initialMonth - 1).coerceIn(0, 11)) }
    var selectedDay by remember { mutableStateOf(String.format(Locale.US, "%02d", initialDay)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val formattedMonth = String.format(Locale.US, "%02d", selectedMonthIndex + 1)
                    onDateConfirmed("$selectedYear/$formattedMonth/$selectedDay")
                }
            ) {
                Text("تایید", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("انصراف", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF142622),
        title = {
            Text(
                text = "انتخاب تاریخ آبیاری",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF0C1916), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Day Wheel Picker
                    WheelColumnPicker(
                        label = "روز",
                        items = days,
                        selectedItem = selectedDay,
                        onItemSelected = { selectedDay = it },
                        modifier = Modifier.weight(1f)
                    )

                    // Month Wheel Picker
                    WheelColumnPicker(
                        label = "ماه",
                        items = monthNames,
                        selectedItem = monthNames[selectedMonthIndex],
                        onItemSelected = { selectedMonthIndex = monthNames.indexOf(it).coerceIn(0, 11) },
                        modifier = Modifier.weight(1.3f)
                    )

                    // Year Wheel Picker
                    WheelColumnPicker(
                        label = "سال",
                        items = years,
                        selectedItem = selectedYear,
                        onItemSelected = { selectedYear = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@Composable
fun WheelColumnPicker(
    label: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val leafGreen = Color(0xFF66BB6A)
    val itemHeight = 40.dp

    val selectedIndex = items.indexOf(selectedItem).coerceIn(0, items.size - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val centerIndex = listState.firstVisibleItemIndex
        if (centerIndex in items.indices) {
            onItemSelected(items[centerIndex])
        }
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = leafGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F231F)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(leafGreen.copy(alpha = 0.15f))
                    .border(1.dp, leafGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )

            val coroutineScope = rememberCoroutineScope()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxHeight(),
                contentPadding = PaddingValues(vertical = itemHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items.size) { index ->
                    val isSelected = items[index] == selectedItem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight - 8.dp)
                            .clickable {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                                onItemSelected(items[index])
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = items[index],
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = if (isSelected) 15.sp else 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
