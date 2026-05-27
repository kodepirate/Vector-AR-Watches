package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfileEntity
import com.example.data.WatchItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Classic Luxury Elite Colors
private val EliteGold = Color(0xFFD4AF37)        // Satin Swiss gold finish
private val WarmMutedBeige = Color(0xFFC5A880)   // Champagne tones
private val AntiqueSilver = Color(0xFFE2E2E2)     // Brushed steel / Platinum accents
private val BreguetBlue = Color(0xFF1E3D59)       // Deep blued steel hands for luxury indicators
private val ClassicSlate = Color(0xFF1A1A1D)      // Fine leather / Velvet drawer contrast background
private val OverlapCyan = Color(0xFF90F1EF)       // Elegant thin laser holo projection tint

/**
 * Minimalist, classic Swiss-inspired luxury login experience.
 * Tailored for a premium haute horlogerie virtual salon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: WatchViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var operatorName by remember { mutableStateOf("") }
    var securityPin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Biometric/Calibration activation state
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("SECURE PORTAL STANDBY") }

    // Fine linear accent line animation (representing optical calibration scanning)
    val infiniteTransition = rememberInfiniteTransition(label = "luxury_grid")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_shift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .drawBehind {
                // Draw fine elegant layout margins (classic watchmaking schematic paper)
                val marginColor = Color.White.copy(alpha = 0.02f)
                val thinBorderBrush = Brush.verticalGradient(
                    listOf(EliteGold.copy(alpha = 0.08f), Color.Transparent, EliteGold.copy(alpha = 0.03f))
                )
                
                // Draw luxury hairline side guides
                drawLine(
                    brush = thinBorderBrush,
                    start = Offset(40.dp.toPx(), 0f),
                    end = Offset(40.dp.toPx(), size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    brush = thinBorderBrush,
                    start = Offset(size.width - 40.dp.toPx(), 0f),
                    end = Offset(size.width - 40.dp.toPx(), size.height),
                    strokeWidth = 1f
                )

                // Elegant ambient radial golden glow inside the center
                drawCircle(
                    color = EliteGold.copy(alpha = 0.012f),
                    radius = size.width * 0.5f,
                    center = center
                )
            }
            .padding(24.dp)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // --- LUXURY BRAND HEADER ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Text(
                    text = "V E C T O R",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                    fontSize = 32.sp,
                    letterSpacing = 8.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ATELIER D'HAUTE HORLOGERIE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmMutedBeige
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Minimalist separator line
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(EliteGold.copy(alpha = 0.4f))
                )
            }

            // --- CLASSIC BOUTIQUE CREATIVE PANEL ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(EliteGold.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MEMBERSHIP VAULT",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp,
                    color = EliteGold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enter credentials to unlock the augmented collection.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = SteelGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Custom Sleek Minimalist Outlined User Input
                OutlinedTextField(
                    value = operatorName,
                    onValueChange = {
                        operatorName = it
                        errorMessage = null
                    },
                    label = { 
                        Text(
                            "CLIENT IDENTITY", 
                            fontFamily = FontFamily.Monospace, 
                            fontSize = 10.sp, 
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    placeholder = { Text("e.g. Jean-Luc Constant", color = SteelGray.copy(alpha = 0.6f), fontSize = 12.sp) },
                    isError = errorMessage != null,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Serif),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EliteGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        cursorColor = EliteGold,
                        focusedLabelColor = EliteGold,
                        unfocusedLabelColor = WarmMutedBeige,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = ClassicSlate.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("operator_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Passcode Input
                OutlinedTextField(
                    value = securityPin,
                    onValueChange = {
                        if (it.length <= 8) {
                            securityPin = it
                            errorMessage = null
                        }
                    },
                    label = { 
                        Text(
                            "ATELIER KEYCODE PIN", 
                            fontFamily = FontFamily.Monospace, 
                            fontSize = 10.sp, 
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    placeholder = { Text("Enter elite access PIN", color = SteelGray.copy(alpha = 0.6f), fontSize = 12.sp) },
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPinVisible) "Hide PIN" else "Show PIN",
                                tint = WarmMutedBeige
                            )
                        }
                    },
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = errorMessage != null,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EliteGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        cursorColor = EliteGold,
                        focusedLabelColor = EliteGold,
                        unfocusedLabelColor = WarmMutedBeige,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = ClassicSlate.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input"),
                    singleLine = true
                )

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Gorgeous Classic Call-To-Action Button (Luxury Gold & Minimal)
                Button(
                    onClick = {
                        if (operatorName.trim().isEmpty()) {
                            errorMessage = "CLIENT IDENTITY REQUIRED"
                            return@Button
                        }
                        if (securityPin.trim().isEmpty()) {
                            errorMessage = "ACCESS PASSCODE REQUIRED"
                            return@Button
                        }
                        statusText = "CALIBRATING ESCAPEMENT CHRONOLOGY..."
                        coroutineScope.launch {
                            delay(600)
                            viewModel.updateProfileName(operatorName.trim(), "Atelier Connoisseur")
                            onLoginSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EliteGold),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("login_submit_btn")
                ) {
                    Text(
                        text = "ENTER VIRTUAL SALON",
                        color = ObsidianBlack,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // --- LUXURY CHRONOMETER VERIFICATION PORTAL (Biometrics) ---
                Text(
                    text = "OR SCAN PHYSICAL ESCAPEMENT SYNC",
                    color = SteelGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tap classic pocket-watch dial interface with Roman Numeral indicators!
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(ClassicSlate)
                        .border(
                            width = 1.5.dp,
                            color = if (isCalibrating) EliteGold else WarmMutedBeige.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable(enabled = !isCalibrating) {
                            coroutineScope.launch {
                                isCalibrating = true
                                errorMessage = null
                                statusText = "ENGAGING CHRONOGRAPH ESCAPEMENT..."
                                delay(400)
                                
                                val steps = 12
                                for (i in 1..steps) {
                                    delay(120)
                                    calibrationProgress = i / steps.toFloat()
                                    statusText = "ALIGNING COSC ATOMIC LENS: ${(calibrationProgress * 100).toInt()}%"
                                }
                                
                                statusText = "GENEVA CALIBRATION FULLY ALIGNED"
                                delay(400)
                                val finalName = operatorName.ifEmpty { "Gilded Patron" }
                                viewModel.updateProfileName(finalName, "Atelier Connoisseur")
                                onLoginSuccess()
                            }
                        }
                        .testTag("biometric_pad"),
                    contentAlignment = Alignment.Center
                ) {
                    val angleOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(12000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "watch_hand_angle"
                    )

                    // Render classic vintage physical pocketwatch dial vectors
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerOffset = center
                        val radius = size.minDimension / 2.0f
                        
                        // Outer vintage gold ring
                        drawCircle(
                            color = EliteGold.copy(alpha = 0.15f),
                            radius = radius * 0.9f,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        
                        // Roman numerals or indices hashes at 12, 3, 6, 9
                        val tickLengths = 6.dp.toPx()
                        val tickColor = EliteGold.copy(alpha = 0.6f)
                        
                        listOf(0f, 90f, 180f, 270f).forEach { angleDegrees ->
                            val r = angleDegrees * (PI / 180.0)
                            val startX = centerOffset.x + (radius * 0.78f * sin(r)).toFloat()
                            val startY = centerOffset.y - (radius * 0.78f * cos(r)).toFloat()
                            val endX = centerOffset.x + ((radius * 0.78f + tickLengths) * sin(r)).toFloat()
                            val endY = centerOffset.y - ((radius * 0.78f + tickLengths) * cos(r)).toFloat()
                            
                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // Ticking hands. Spinning is passive, but goes rapid during calibration scan
                        val finalAngle = if (isCalibrating) {
                            angleOffset * 4.5f
                        } else {
                            angleOffset
                        }

                        // Draw classic watch hand (Minute Hand)
                        withTransform({
                            rotate(degrees = finalAngle, pivot = centerOffset)
                        }) {
                            drawLine(
                                color = BreguetBlue,
                                start = centerOffset,
                                end = Offset(centerOffset.x, centerOffset.y - (radius * 0.68f)),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Draw hour hour hand (slower)
                        withTransform({
                            rotate(degrees = finalAngle / 12f, pivot = centerOffset)
                        }) {
                            drawLine(
                                color = BreguetBlue,
                                start = centerOffset,
                                end = Offset(centerOffset.x, centerOffset.y - (radius * 0.45f)),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Gold central cap pin
                        drawCircle(
                            color = EliteGold,
                            radius = 4.dp.toPx()
                        )
                    }

                    // Discrete transparent finger scanning glass overlay when interactive
                    if (isCalibrating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(OverlapCyan.copy(alpha = 0.05f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = statusText.uppercase(),
                    color = if (isCalibrating) OverlapCyan else WarmMutedBeige,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- BOTTOM SWISS ASSURANCE SIGNATURES ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp, top = 24.dp)
            ) {
                Text(
                    text = "CHRONOGRAPHE AUGMENTÉ • CHRONOS COUPOLE",
                    color = SteelGray.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "REINFORCED LABS SWITZERLAND • COSC COMPLIANT",
                    color = EliteGold.copy(alpha = 0.35f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


/**
 * Elite Atelier Salon Hero Screen.
 * Minimalist luxury presentation featuring an elegant orbital preview of our flagship masterpiece.
 * Allows toggling "HOLOGRAPHIC OVERLAY" (representing premium augmented watches) and calibration sync.
 */
@Composable
fun HeroScreen(
    viewModel: WatchViewModel,
    onExploreCatalog: () -> Unit,
    onStartAr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val syncLogs by viewModel.firebaseSyncLogs.collectAsState()
    
    // Choose the flagship piece from Catalog (First watch is Tourbillon or Premium Matrix)
    val flagshipWatch: WatchItem = viewModel.watchCatalog.firstOrNull() 
        ?: com.example.data.WatchCatalog.watches.first()

    // Flag for augmented holographic overlay diagnostics (the "augmented" look!)
    var showHoloOverlay by remember { mutableStateOf(true) }

    // Interactive Drag rotation states for flagship engine
    var dragRotationX by remember { mutableStateOf(10f) }
    var dragRotationY by remember { mutableStateOf(5f) }
    var isInteracting by remember { mutableStateOf(false) }

    // Smooth continuous rotation when untouched
    val infiniteTransition = rememberInfiniteTransition(label = "luxury_idle_orbit")
    val passiveSpinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Compute appropriate angle representation
    val activeRotationX = if (isInteracting) dragRotationX else dragRotationX + passiveSpinDegrees

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .safeDrawingPadding()
    ) {
        // Minimalist fine background outline margins
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = EliteGold.copy(alpha = 0.05f)
            val cornerSize = 16.dp.toPx()
            val indent = 16.dp.toPx()
            
            // Draw luxury fine perimeter guide lines
            drawLine(strokeColor, Offset(indent, indent), Offset(size.width - indent, indent), 1f)
            drawLine(strokeColor, Offset(indent, size.height - indent), Offset(size.width - indent, size.height - indent), 1f)
            drawLine(strokeColor, Offset(indent, indent), Offset(indent, size.height - indent), 1f)
            drawLine(strokeColor, Offset(size.width - indent, indent), Offset(size.width - indent, size.height - indent), 1f)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // --- ATELIER CONNOISSEUR STATUS HEADER ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OFFICIAL PARTNER SALON",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 2.sp,
                            color = EliteGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userProfile.username.uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Luxury certification badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, EliteGold.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "GOLD MEMBER",
                            color = EliteGold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // --- THE IMMERSIVE CHRONOMETER ATELIER SHOWCASE ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClassicSlate.copy(alpha = 0.6f))
                        .border(
                            width = 1.dp,
                            color = EliteGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Item Header (Classic style)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MASTERPIECE FLAGSHIP PREVIEW",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = WarmMutedBeige,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = flagshipWatch.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color.White
                            )
                        }

                        // Holo Mode toggle button (augmented feature showcase)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (showHoloOverlay) OverlapCyan.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                .border(
                                    width = 1.dp,
                                    color = if (showHoloOverlay) OverlapCyan else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { showHoloOverlay = !showHoloOverlay }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (showHoloOverlay) OverlapCyan else SteelGray, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AUGMENTED LENS",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showHoloOverlay) Color.White else SteelGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Embedded Watch Renderer - Luxury Atelier showcase viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        if (showHoloOverlay) OverlapCyan.copy(alpha = 0.05f) else EliteGold.copy(alpha = 0.03f), 
                                        Color.Transparent
                                    ),
                                    radius = 450f
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isInteracting = true },
                                    onDragEnd = { /* stays active at manual drag stance */ },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragRotationX = (dragRotationX + dragAmount.x * 0.65f) % 360f
                                        dragRotationY = (dragRotationY + dragAmount.y * 0.5f).coerceIn(-45f, 45f)
                                    }
                                )
                            }
                            .testTag("interactive_hero_orbit"),
                        contentAlignment = Alignment.Center
                    ) {
                        
                        // Traditional watchmaking guilloche / dial background guide rings on canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val innerDimen = size.minDimension
                            
                            // Golden concentric orbital axes guidelines
                            drawCircle(
                                color = EliteGold.copy(alpha = 0.05f),
                                radius = innerDimen * 0.44f,
                                style = Stroke(width = 1f)
                            )
                            
                            drawCircle(
                                color = EliteGold.copy(alpha = 0.02f),
                                radius = innerDimen * 0.34f,
                                style = Stroke(width = 1f)
                            )

                            // Clean luxury outer crosshairs in gold
                            drawLine(
                                color = EliteGold.copy(alpha = 0.06f),
                                start = Offset(size.width * 0.15f, size.height / 2),
                                end = Offset(size.width * 0.85f, size.height / 2),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = EliteGold.copy(alpha = 0.06f),
                                start = Offset(size.width / 2, size.height * 0.15f),
                                end = Offset(size.width / 2, size.height * 0.85f),
                                strokeWidth = 1f
                            )

                            // Subtle blued steel overlay ticks if holographic lens is active
                            if (showHoloOverlay) {
                                drawCircle(
                                    color = OverlapCyan.copy(alpha = 0.18f),
                                    radius = innerDimen * 0.24f,
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f)
                                    )
                                )
                                // Compass bounding boxes
                                drawRect(
                                    color = OverlapCyan.copy(alpha = 0.08f),
                                    topLeft = Offset(size.width / 2 - innerDimen * 0.35f, size.height / 2 - innerDimen * 0.35f),
                                    size = Size(innerDimen * 0.7f, innerDimen * 0.7f),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }

                        // Premium mechanical core rendering
                        WatchCanvasRenderer(
                            watch = flagshipWatch,
                            rotationDegreesX = activeRotationX,
                            rotationDegreesY = dragRotationY,
                            strapColorOverride = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        )

                        // Clean overlay text indicators
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .border(1.dp, EliteGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isInteracting) {
                                    "ORBIT COMPASS: ${activeRotationX.toInt()}° LAT / ${dragRotationY.toInt()}° LON"
                                } else {
                                    "SWIPE HORIZONTALLY TO ROTATE PIECE"
                                },
                                color = EliteGold,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Traditional swiss horology calibration info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryCard(
                            title = "SWISS CALIBER",
                            value = "VAL-832 AUTO",
                            color = EliteGold,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            title = "COSC CERTIFIED",
                            value = "+0.2s/DAY",
                            color = AntiqueSilver,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            title = "HOLO LENS BEAM",
                            value = if (showHoloOverlay) "450nm ACTIVE" else "OFF-GRID COLD",
                            color = if (showHoloOverlay) OverlapCyan else SteelGray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- CLASSIC LUXURY PORTAL BUTTONS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Elegant polished gold text button
                    Button(
                        onClick = onExploreCatalog,
                        colors = ButtonDefaults.buttonColors(containerColor = EliteGold),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("hero_explore_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "EXPLORE COLLECTION",
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = null,
                                tint = ObsidianBlack,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Semi-transparent platinum button for AR Try-on portal
                    Button(
                        onClick = onStartAr,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, EliteGold.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("hero_tryon_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = EliteGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LAUNCH AR TRY-ON",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // --- SWISS COSC TEST REPORT / CLOUD REGISTRY FEEDS ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ClassicSlate.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EliteGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ATELIER COSC CALIBRATION FEED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Chronograph sync button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.triggerCloudSyncManual() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CALIBRATE NOW",
                                color = EliteGold,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // List of gorgeous status feed logs
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (syncLogs.isEmpty()) {
                            Text(
                                text = "> CONNECTED TO ATELIER SERVER... LISTENER COLD-READY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = SteelGray
                            )
                        } else {
                            syncLogs.forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "[SYSTEM OK]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.type.contains("SUCCESS")) EliteGold else OverlapCyan,
                                        modifier = Modifier.width(85.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.detail.replace("Cloud", "Atelier").replace("Firebase", "COSC Core"),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = AntiqueSilver,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TelemetryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.015f))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = SteelGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
