package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DialStyle
import com.example.data.UserProfileEntity
import com.example.data.WatchItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchCatalogScreen(
    viewModel: WatchViewModel,
    modifier: Modifier = Modifier
) {
    val watches = viewModel.watchCatalog
    val favoriteWatches by viewModel.favoriteWatches.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val categories = listOf("All", "Tech Matrix", "Tourbillon", "Analog", "Chronograph")

    // Filter list based on selections
    val filteredWatches = watches.filter {
        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) || 
                            it.series.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedCategory) {
            "All" -> true
            "Tech Matrix" -> it.dialStyle == DialStyle.TECH_MATRIX
            "Tourbillon" -> it.dialStyle == DialStyle.TOURBILLON
            "Analog" -> it.dialStyle == DialStyle.ANALOG
            "Chronograph" -> it.dialStyle == DialStyle.CHRONOGRAPH
            else -> true
        }
        matchesSearch && matchesCategory
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VECTOR",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = 4.sp,
                            color = Color.White
                        )
                        Text(
                            text = "3D CHRONOS INC",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(DarkSlate, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { viewModel.setScreen("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Terminal",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Modern Stealth Outlined Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_field"),
                    placeholder = { Text("Search 3D watches...", color = SteelGray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = SteelGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        unfocusedContainerColor = DarkSlate,
                        focusedContainerColor = DarkSlate,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Minimalist Pill Selector Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) ElectricCyan else DarkSlate)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("category_pill_$category")
                        ) {
                            Text(
                                text = category.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) ObsidianBlack else Color.White
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (filteredWatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = "Empty",
                        tint = SteelGray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NO WATCHES FOUND MATCHING VECTOR SPECS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(filteredWatches) { watch ->
                    val isFavorite = favoriteWatches.any { it.id == watch.id }
                    WatchCatalogCard(
                        watch = watch,
                        isFavorite = isFavorite,
                        onClickCard = { viewModel.selectWatch(watch.id) },
                        onToggleFavorite = { viewModel.toggleFavoriteInDb(watch.id) },
                        onArClick = {
                            viewModel.selectWatch(watch.id)
                            viewModel.setScreen("try-on")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WatchCatalogCard(
    watch: WatchItem,
    isFavorite: Boolean,
    onClickCard: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSlate),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onClickCard() }
            .testTag("watch_card_${watch.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniature ticking watch renderer - incredibly immersive!
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            ) {
                WatchCanvasRenderer(
                    watch = watch,
                    rotationDegreesX = 0f,
                    rotationDegreesY = 0f,
                    strapColorOverride = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = watch.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = watch.series.uppercase(),
                            color = watch.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Set favorite watch",
                            tint = if (isFavorite) LuxuryGold else SteelGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "$${String.format(Locale.US, "%,.2f", watch.price)}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // AR Try-On Action pill
                    Button(
                        onClick = onArClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "AR Try-On icon",
                            tint = ElectricCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AR TRY-ON",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricCyan
                        )
                    }

                    // 3D Visualizer details
                    Button(
                        onClick = onClickCard,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "3D VIEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ObsidianBlack
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WatchDetailScreen(
    viewModel: WatchViewModel,
    onBackToCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedWatchId by viewModel.selectedWatchId.collectAsState()
    val watches = viewModel.watchCatalog
    val watch = watches.find { it.id == selectedWatchId } ?: watches.first()
    val favoriteWatches by viewModel.favoriteWatches.collectAsState()
    val isFavorite = favoriteWatches.any { it.id == watch.id }

    val rx by viewModel.watchRotationX.collectAsState()
    val ry by viewModel.watchRotationY.collectAsState()
    val strapColorName by viewModel.customStrapColorOption.collectAsState()

    val context = LocalContext.current
    var isCheckingOut by remember { mutableStateOf(false) }

    val strapColors = listOf(
        Pair("Original Matrix", null),
        Pair("Obsidian Treated", ObsidianGlow),
        Pair("Aerospace Titanium", SteelGray),
        Pair("Classic Plating Glow", LuxuryGold)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        // TOP DETAIL PORT NAVIGATION HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToCatalog,
                modifier = Modifier
                    .background(DarkSlate, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to catalog", tint = Color.White)
            }

            Text(
                text = "3D CUSTOMIZER PORTAL",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = ElectricCyan
            )

            IconButton(
                onClick = { viewModel.toggleFavoriteInDb(watch.id) },
                modifier = Modifier
                    .background(DarkSlate, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite in config",
                    tint = if (isFavorite) LuxuryGold else Color.White
                )
            }
        }

        // LARGE INTERACTIVE 3D CANVASES RENDER AREA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.rotateWatch(dragAmount.x, dragAmount.y)
                    }
                }
                .testTag("interactive_3d_render"),
            contentAlignment = Alignment.Center
        ) {
            // Background rotating telemetry lines
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.sweepGradient(
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent, Color.White.copy(alpha = 0.05f))
                        ),
                        shape = CircleShape
                    )
            )

            // Draw primary canvas
            WatchCanvasRenderer(
                watch = watch,
                rotationDegreesX = rx,
                rotationDegreesY = ry,
                strapColorOverride = strapColors.find { it.first == strapColorName }?.second,
                modifier = Modifier.size(240.dp)
            )

            // Dynamic rotation values display
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "drag to rotate",
                        tint = SteelGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SWIPE WATCH TO ROTATE 3D DIAL [X: ${rx.toInt()}° Y: ${ry.toInt()}°]",
                        color = SteelGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // SWAPPABLE STRAPS COMPONENT OPTIONS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "INTERACTIVE MATERIAL ADAPTOR",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                strapColors.forEach { option ->
                    val isSelected = option.first == strapColorName || (strapColorName == null && option.second == null)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) watch.primaryColor else DarkSlate)
                            .clickable { viewModel.setStrapSwap(option.first) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("strap_swap_${option.first}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.first.uppercase(),
                            color = if (isSelected) ObsidianBlack else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CHASSIS SPECIFICATIONS BOTTOM GRID
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                    // Header title & rating
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = watch.name.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, "Rating", tint = LuxuryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = watch.rating.toString(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", watch.price)} USD",
                        color = ElectricCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = watch.description,
                        color = SteelGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 4-Quadrant Specifications
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SpecQuadrantItem(
                            label = "CHASSIS DEPTH",
                            value = "${watch.diameter}MM DIAL",
                            icon = Icons.Default.AspectRatio,
                            modifier = Modifier.weight(1f)
                        )
                        SpecQuadrantItem(
                            label = "METALLIC MESH",
                            value = watch.strapMaterial,
                            icon = Icons.Default.LineStyle,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        SpecQuadrantItem(
                            label = "WATER RESIST",
                            value = watch.waterResistance,
                            icon = Icons.Default.Water,
                            modifier = Modifier.weight(1f)
                        )
                        SpecQuadrantItem(
                            label = "KINETIC COGS",
                            value = "${watch.gearCount} COMP",
                            icon = Icons.Default.Settings,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // PRIMARY ENGAGEMENT BUTTONS: AR try on & Checkout GPay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AR try-on portal trigger
                        Button(
                            onClick = { viewModel.setScreen("try-on") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .border(1.dp, ElectricCyan, RoundedCornerShape(12.dp))
                                .testTag("ar_tryon_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, "AR icon", tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AR WRIST TRY-ON",
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        // Gateway checkout sheet trigger
                        Button(
                            onClick = { isCheckingOut = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("checkout_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, "Checkout", tint = ObsidianBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "SECURE ORDER",
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // CHECKOUT ORDER SHEET OVERLAY (Apple Pay / Google Pay Sandbox)
    if (isCheckingOut) {
        SecurePaymentSheet(
            viewModel = viewModel,
            amount = watch.price,
            watchName = watch.name,
            onDismiss = { isCheckingOut = false }
        )
    }
}

@Composable
fun SpecQuadrantItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = ElectricCyan, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = SteelGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(value.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurePaymentSheet(
    viewModel: WatchViewModel,
    amount: Double,
    watchName: String,
    onDismiss: () -> Unit
) {
    val state by viewModel.paymentState.collectAsState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSlate,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val pState = state) {
                is PaymentState.Idle -> {
                    Text(
                        text = "SECURE PROTOCOL GATEWAY",
                        color = ElectricCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Order Summary: $watchName",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", amount)} USD",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Standard Authorized Apple Pay Button (minimal design dark)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(Color.Black, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.startPaymentFlow(amount, "Apple Pay") }
                            .testTag("apple_pay_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, "Apple Pay biometric verification", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pay with Apple Pay", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Standard Authorized Google Pay Button (minimal design, contrasting accent)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .clickable { viewModel.startPaymentFlow(amount, "Google Pay") }
                            .testTag("google_pay_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalanceWallet, "Wallet Icon", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pay with Google Pay", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is PaymentState.Processing -> {
                    CircularProgressIndicator(color = ElectricCyan)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "ESTABLISHING ENCRYPTED TOKEN TUNNEL...",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                is PaymentState.SecurityConfirming -> {
                    CircularProgressIndicator(color = NeonGreen)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = pState.step.uppercase(),
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                }

                is PaymentState.Completed -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = NeonGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TRANSACTION CONFIRMED",
                        color = NeonGreen,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ID: ${pState.transactionId}",
                        color = SteelGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Secure Sandbox Invoice compiled. Your watch is preparing for shipment.",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.resetPayment()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE RECEIPT", color = Color.White)
                    }
                }

                is PaymentState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = ObsidianGlow,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PAYMENT SYSTEM MALFUNCTION",
                        color = ObsidianGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = pState.message, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.resetPayment() },
                        colors = ButtonDefaults.buttonColors(containerColor = CardGray)
                    ) {
                        Text("RETRY")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileFavoritesScreen(
    viewModel: WatchViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favoriteWatches.collectAsState()
    val logs by viewModel.firebaseSyncLogs.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var editingUsername by remember { mutableStateOf(profile.username) }
    var editingStatus by remember { mutableStateOf(profile.statusText) }
    var isEditing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(DarkSlate, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }

                Text(
                    text = "VECTOR ADMIN CONTROL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = ElectricCyan
                )

                // Trigger quick manual sync with visual feedback
                IconButton(
                    onClick = { viewModel.triggerCloudSyncManual() },
                    modifier = Modifier
                        .background(DarkSlate, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.CloudSync, "Sync Firebase", tint = NeonGreen)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // USER TERMINAL PROFILE CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editingUsername,
                                        onValueChange = { editingUsername = it },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                                        modifier = Modifier.width(200.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = editingStatus,
                                        onValueChange = { editingStatus = it },
                                        singleLine = true,
                                        modifier = Modifier.width(200.dp)
                                    )
                                } else {
                                    Text(
                                        text = profile.username.uppercase(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = profile.statusText,
                                        color = ElectricCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (isEditing) {
                                        viewModel.updateProfileName(editingUsername, editingStatus)
                                    } else {
                                        editingUsername = profile.username
                                        editingStatus = profile.statusText
                                    }
                                    isEditing = !isEditing
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = if (isEditing) "SAVE" else "EDIT",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sync indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("FIREBASE LINK STATUS", color = SteelGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (profile.syncEnabled) NeonGreen else SteelGray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (profile.syncEnabled) "ACTIVE CLOUD-SYNC" else "CLOUD OFF",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("LAST SYNC SEQUENCE", color = SteelGray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date(profile.lastSyncTime)),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // BACKEND SETTINGS PANEL
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FIREBASE SYNC PRESET RULES",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Dynamic Firebase Telemetry Sync",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Allow background watch changes and favorites collection to dump to Firestore instantly.",
                                    color = SteelGray,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = profile.syncEnabled,
                                onCheckedChange = { viewModel.setSyncEnabledState(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = Color.Black)
                            )
                        }
                    }
                }
            }

            // FAVORITES COLLECTION HEADER
            item {
                Text(
                    text = "SAVED WATCHES COLLECTION (${favorites.size})",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // SAVED FAVORITES LIST
            if (favorites.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSlate, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Timeline, "Zero Favorites", tint = SteelGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your vault is empty. Favorite watches on the Catalog or in 3D Mode",
                                color = SteelGray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(favorites) { watch ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSlate, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectWatch(watch.id) }
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color.Black, RoundedCornerShape(8.dp))
                                ) {
                                    WatchCanvasRenderer(
                                        watch = watch,
                                        rotationDegreesX = 0f,
                                        rotationDegreesY = 0f,
                                        strapColorOverride = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(watch.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(watch.series, color = watch.primaryColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$${String.format(Locale.US, "%,.2f", watch.price)}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.toggleFavoriteInDb(watch.id) }) {
                                    Icon(Icons.Default.DeleteOutline, "Remove", tint = ObsidianGlow)
                                }
                            }
                        }
                    }
                }
            }

            // FIREBASE SYNC CYBER CONSOLE LOGS TERMINAL CONTAINER
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FIREBASE TRANSACTION CONSOLE",
                            color = NeonGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF152A1C), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "CONNECTED",
                                color = NeonGreen,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Log terminal console block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF030303), RoundedCornerShape(4.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        logs.forEach { log ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))}] ",
                                    color = SteelGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${log.type}: ",
                                    color = when (log.type) {
                                        "FIREBASE_SUCCESS" -> NeonGreen
                                        "FIREBASE_SYNC" -> ElectricCyan
                                        "INIT" -> SteelGray
                                        else -> LuxuryGold
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = log.detail,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Click the Sync icon in top right to push updates manually.",
                        color = SteelGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
