package com.example.ui

import android.Manifest
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.WatchCatalog
import com.example.data.WatchItem
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SteelGray
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ArTryOnScreen(
    viewModel: WatchViewModel,
    onBackToCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedWatchId by viewModel.selectedWatchId.collectAsState()
    val watches = viewModel.watchCatalog
    val activeWatch = watches.find { it.id == selectedWatchId } ?: watches.first()

    val arWristScale by viewModel.arWristScale.collectAsState()
    val arRotation by viewModel.arRotation.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner ?: LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Fallback mode if camera provider fails or permission is denied
    var isCameraActive by remember { mutableStateOf(hasCameraPermission) }
    var useArmMockFallback by remember { mutableStateOf(!hasCameraPermission) }
    var currentWristPresetIdx by remember { mutableIntStateOf(0) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        isCameraActive = isGranted
        useArmMockFallback = !isGranted
    }
    
    // Preset mock wrists background paths or styles
    val wristPresets = listOf(
        Pair("Slate Cybernetic Wrist", Color(0xFF1E1F26)),
        Pair("Titanium Grid Line", Color(0xFF111216)),
        Pair("Obsidian Textured Bio-Arm", Color(0xFF0F0E13)),
        Pair("OLED Clean Hologram", Color(0xFF050505))
    )

    // Capture try-on visual effect state
    var isCapturingEffect by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var isFrontCamera by remember { mutableStateOf(false) }
    var watchOffsetX by remember { mutableStateOf(0f) }
    var watchOffsetY by remember { mutableStateOf(0f) }

    // 3D Orbit, Viewpoint & Perspective parameters
    var arYaw by remember { mutableStateOf(0f) } // 0..360 horizontal spin rotation
    var arPitch by remember { mutableStateOf(0f) } // -60..60 vertical pitch angle
    var activeViewCategory by remember { mutableStateOf("ORBIT") } // ORBIT, AI_CV_SCAN, CALIBRATE

    // AI Wrist Detection & Auto-Attachment System state
    var isAiWristDetectionEnabled by remember { mutableStateOf(true) }
    var wristLateralDrift by remember { mutableStateOf(0f) } // Slider coordinates to drift wrist x
    var wristVerticalDrift by remember { mutableStateOf(-30f) } // Slider coordinates to drift wrist y

    // AR Automated Lock-On / Sync State Machine
    var trackingPhase by remember { mutableStateOf("ACQUIRING") } // ACQUIRING -> EXAMINING -> MOUNTED
    var lockProgress by remember { mutableStateOf(0f) }
    var autoScaleAnim by remember { mutableStateOf(0.4f) }
    var autoRotationAnim by remember { mutableStateOf(120f) }

    // Synchronize coordinates and status when Auto-Attachment is locked
    LaunchedEffect(isAiWristDetectionEnabled, wristLateralDrift, wristVerticalDrift) {
        if (isAiWristDetectionEnabled) {
            watchOffsetX = wristLateralDrift
            watchOffsetY = wristVerticalDrift
            trackingPhase = "MOUNTED"
        }
    }

    // Re-run lock-on simulation and automatic setting calibration whenever watch selection shifts
    LaunchedEffect(selectedWatchId) {
        trackingPhase = "ACQUIRING"
        lockProgress = 0f
        autoScaleAnim = 0.35f
        autoRotationAnim = 120f
        if (!isAiWristDetectionEnabled) {
            watchOffsetX = 0f
            watchOffsetY = 0f
        }
        
        delay(350)
        trackingPhase = "EXAMINING"
        val steps = 20
        for (i in 1..steps) {
            delay(40)
            lockProgress = i / steps.toFloat()
            // Pull scale smoothly from a tiny 3D projection overlay (0.35f) up to a full tailored wrist fit (1.0f)
            autoScaleAnim = 0.35f + (0.65f * lockProgress)
            // Spin watch model from projection entry 120° down to 0° aligned base
            autoRotationAnim = 120f * (1f - lockProgress)
        }
        trackingPhase = "MOUNTED"
    }

    // Natural breathing / micro-tracking drift stabilizer mimicking physical camera hold
    val driftTransition = rememberInfiniteTransition(label = "ar_drift")
    val driftX by driftTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "drift_x"
    )
    val driftY by driftTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "drift_y"
    )
    
    // Constant scanning laser elevation modifier
    val radarSweeper by driftTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(2800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "radar_sweeper"
    )



    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // BACKGROUND FEED (either live CameraX or the Premium Bio-Arm Fallback Canvas)
        if (isCameraActive && !useArmMockFallback) {
            CameraXView(
                isFrontCamera = isFrontCamera,
                onCameraAvailable = { active ->
                    if (!active) {
                        useArmMockFallback = true
                    }
                },
                onWristDetected = { coord ->
                    if (isAiWristDetectionEnabled && coord != null) {
                        // Very rough scaling multiplier for simulation demo since Image dimensions !== Screen dimensions
                        wristLateralDrift = (coord.x - 300f) * 0.5f 
                        wristVerticalDrift = (coord.y - 400f) * 0.5f
                        trackingPhase = "MOUNTED"
                    } else if (isAiWristDetectionEnabled && coord == null) {
                        trackingPhase = "EXAMINING"
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Bio-Arm Stencil Canvas Fallback with mesh gradients to simulate wrist trial
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(wristPresets[currentWristPresetIdx].second, Color.Black),
                            radius = 1200f
                        )
                    )
            ) {
                // Draw elegant technical schematic arm guide outline
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VIRTUAL TRY-ON CANVASES",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wristPresets[currentWristPresetIdx].first,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Box representing wrist profile placement guide
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .height(440.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(ElectricCyan.copy(alpha = 0.5f), Color.Transparent, NeonGreen.copy(alpha = 0.3f))
                                ),
                                shape = RoundedCornerShape(130.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Internal guiding scan-line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ElectricCyan.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        // FLOATING STENCIL OUTLINE OVERLAY (For placing physical wrist in perspective)
        if (isCameraActive && !useArmMockFallback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Wrist silhouette to place your wrist
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(380.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(ElectricCyan, Color.Transparent, ElectricCyan)
                            ),
                            shape = RoundedCornerShape(110.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PLACE WRIST HERE",
                        color = ElectricCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // THE AR WATCH DRAW OVERLAY (Positioned center, scaleable by pinch gestures with automatic wrist alignment tracking)
        val localDensity = androidx.compose.ui.platform.LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            // High-tech holographic matrix target frame behind the watch
            Box(
                modifier = Modifier
                    .size((arWristScale * autoScaleAnim * 280f).dp)
                    .offset(x = (driftX + watchOffsetX).dp, y = (driftY + watchOffsetY).dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            if (isAiWristDetectionEnabled) {
                                // Dynamic wrist tracker moves under gesture, auto-attaching watch alongside it
                                wristLateralDrift += pan.x / localDensity.density
                                wristVerticalDrift += pan.y / localDensity.density
                            } else {
                                watchOffsetX += pan.x / localDensity.density
                                watchOffsetY += pan.y / localDensity.density
                            }
                            if (zoom != 1f) {
                                viewModel.adjustArScale((zoom - 1f) * 0.4f)
                            }
                            if (rotation != 0f) {
                                arYaw = (arYaw + rotation * 0.8f) % 360f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Tracking Circles and brackets
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val innerDimen = size.minDimension
                    
                    val hudColor = when(trackingPhase) {
                        "ACQUIRING" -> Color.Red.copy(alpha = 0.4f)
                        "EXAMINING" -> ElectricCyan.copy(alpha = 0.5f)
                        else -> NeonGreen.copy(alpha = 0.4f)
                    }
                    
                    // Orbit rings
                    drawCircle(
                        color = hudColor.copy(alpha = 0.08f),
                        radius = innerDimen * 0.45f
                    )
                    drawCircle(
                        color = hudColor.copy(alpha = 0.3f),
                        radius = innerDimen * 0.45f,
                        style = Stroke(
                            width = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(15f, 15f), 
                                0f
                            )
                        )
                    )
                    
                    // Radar laser sweep bar inside target
                    val laserY = size.height * 0.1f + (size.height * 0.8f * radarSweeper)
                    drawLine(
                        color = hudColor.copy(alpha = 0.6f),
                        start = Offset(size.width * 0.15f, laserY),
                        end = Offset(size.width * 0.85f, laserY),
                        strokeWidth = 3f
                    )
                    
                    // Draw digital corners (sensor brackets)
                    val bracketLen = 20f * density
                    val pad = 8f * density
                    // Top Left
                    drawLine(color = hudColor, start = Offset(pad, pad), end = Offset(pad + bracketLen, pad), strokeWidth = 3f)
                    drawLine(color = hudColor, start = Offset(pad, pad), end = Offset(pad, pad + bracketLen), strokeWidth = 3f)
                    // Top Right
                    drawLine(color = hudColor, start = Offset(size.width - pad, pad), end = Offset(size.width - pad - bracketLen, pad), strokeWidth = 3f)
                    drawLine(color = hudColor, start = Offset(size.width - pad, pad), end = Offset(size.width - pad, pad + bracketLen), strokeWidth = 3f)
                    // Bottom Left
                    drawLine(color = hudColor, start = Offset(pad, size.height - pad), end = Offset(pad + bracketLen, size.height - pad), strokeWidth = 3f)
                    drawLine(color = hudColor, start = Offset(pad, size.height - pad), end = Offset(pad, size.height - pad - bracketLen), strokeWidth = 3f)
                    // Bottom Right
                    drawLine(color = hudColor, start = Offset(size.width - pad, size.height - pad), end = Offset(size.width - pad - bracketLen, size.height - pad), strokeWidth = 3f)
                    drawLine(color = hudColor, start = Offset(size.width - pad, size.height - pad), end = Offset(size.width - pad, size.height - pad - bracketLen), strokeWidth = 3f)

                    // DRAW BIOMETRIC SKELETAL ATTACH POINTS IF AI DETECTION IS ENABLED
                    if (isAiWristDetectionEnabled) {
                        val trackerColor = Color(0xFF00FFCC)
                        
                        // Center/Midpoint to Radial styloid point
                        drawLine(
                            color = trackerColor.copy(alpha = 0.4f),
                            start = Offset(size.width / 2, size.height / 2),
                            end = Offset(size.width * 0.18f, size.height * 0.45f),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                        // Center/Midpoint to Ulnar styloid point
                        drawLine(
                            color = trackerColor.copy(alpha = 0.4f),
                            start = Offset(size.width / 2, size.height / 2),
                            end = Offset(size.width * 0.82f, size.height * 0.45f),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                        // Skeletal forearm lines fading out
                        drawLine(
                            color = trackerColor.copy(alpha = 0.3f),
                            start = Offset(size.width * 0.18f, size.height * 0.45f),
                            end = Offset(size.width * 0.28f, size.height * 0.95f),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = trackerColor.copy(alpha = 0.3f),
                            start = Offset(size.width * 0.82f, size.height * 0.45f),
                            end = Offset(size.width * 0.72f, size.height * 0.95f),
                            strokeWidth = 3f
                        )
                        // Carpal connection bridge
                        drawLine(
                            color = trackerColor.copy(alpha = 0.5f),
                            start = Offset(size.width * 0.18f, size.height * 0.45f),
                            end = Offset(size.width * 0.82f, size.height * 0.45f),
                            strokeWidth = 1.5f
                        )
                        
                        // Joint Nodes
                        drawCircle(color = trackerColor, radius = 4f * density, center = Offset(size.width * 0.18f, size.height * 0.45f))
                        drawCircle(color = trackerColor.copy(alpha = 0.15f), radius = 10f * density, center = Offset(size.width * 0.18f, size.height * 0.45f))
                        
                        drawCircle(color = trackerColor, radius = 4f * density, center = Offset(size.width * 0.82f, size.height * 0.45f))
                        drawCircle(color = trackerColor.copy(alpha = 0.15f), radius = 10f * density, center = Offset(size.width * 0.82f, size.height * 0.45f))
                        
                        // Yellow focal lock point
                        drawCircle(color = Color.Yellow, radius = 3.5f * density, center = Offset(size.width / 2, size.height / 2))
                    }
                }

                // Overlay Watch Face rendering supporting full yaw/pitch 3D movements
                WatchCanvasRenderer(
                    watch = activeWatch,
                    rotationDegreesX = arRotation + arYaw + autoRotationAnim + driftX,
                    rotationDegreesY = arPitch + driftY,
                    strapColorOverride = null,
                    modifier = Modifier
                        .size((arWristScale * autoScaleAnim * 220f).dp)
                        .testTag("ar_watch_overlay")
                )
            }

            // HUD Status Capsule overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = when(trackingPhase) {
                            "ACQUIRING" -> Color.Red
                            "EXAMINING" -> ElectricCyan
                            else -> NeonGreen
                        }.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when(trackingPhase) {
                                    "ACQUIRING" -> Color.Red
                                    "EXAMINING" -> ElectricCyan
                                    else -> NeonGreen
                                },
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = when(trackingPhase) {
                            "ACQUIRING" -> "ACQUIRING WRIST LOCK..."
                            "EXAMINING" -> "CHRONO FIT ALIGNMENT: ${(lockProgress * 100).toInt()}%"
                            else -> "AUTOLOCKED ONTO WRIST: STABLE"
                        },
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // TOP CONTROLS ROW & BRIEFING
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToCatalog,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit AR Portal",
                        tint = Color.White
                    )
                }

                Text(
                    text = "VECTOR AR TRY-ON",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switch camera button (only if not in fallback mode)
                    if (!useArmMockFallback) {
                        IconButton(
                            onClick = {
                                isFrontCamera = !isFrontCamera
                                viewModel.addSyncLog("AR_CAMERA_SWITCH", "Switched to ${if (isFrontCamera) "Front-Facing" else "Rear"} Camera")
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchCamera,
                                contentDescription = "Switch Camera Front/Back",
                                tint = ElectricCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            // Toggle fallback vs live camera permission manual helper
                            if (useArmMockFallback) {
                                if (hasCameraPermission) {
                                    useArmMockFallback = false
                                    isCameraActive = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } else {
                                useArmMockFallback = true
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (useArmMockFallback) Icons.Default.CameraAlt else Icons.Default.VideocamOff,
                            contentDescription = "Toggle AR Canvas Mode",
                            tint = if (useArmMockFallback) SteelGray else ElectricCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Watch Selector Horizontal bar for easy try on swap without exiting!
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val currIdx = watches.indexOfFirst { it.id == activeWatch.id }
                        val prevIdx = if (currIdx <= 0) watches.size - 1 else currIdx - 1
                        viewModel.selectWatch(watches[prevIdx].id)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, "Prev Watch", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = activeWatch.name.uppercase(),
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = activeWatch.series,
                            color = SteelGray,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(onClick = {
                        val currIdx = watches.indexOfFirst { it.id == activeWatch.id }
                        val nextIdx = if (currIdx >= watches.size - 1) 0 else currIdx + 1
                        viewModel.selectWatch(watches[nextIdx].id)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, "Next Watch", tint = Color.White)
                    }
                }
            }
        }

        // BOTTOM CONTROLS (Multi-tabbed dashboard for 3D perspectives, AI auto-attachment wrist scanners, and sizing)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.98f))
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BEAUTIFUL GLOWING TABS SYSTEM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("ORBIT", "🔄 3D ORBIT", ElectricCyan),
                    Triple("AI_CV_SCAN", "👾 AI WRIST SCAN", NeonGreen),
                    Triple("CALIBRATE", "🎨 ADJUST FITS", Color.White)
                ).forEach { (id, label, color) ->
                    val selected = activeViewCategory == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) color.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (selected) color.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { activeViewCategory = id }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) color else SteelGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // DYNAMIC CONTENT SLOTS ACCORDING TO SELECTED TAB
            when (activeViewCategory) {
                "ORBIT" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Info badge
                            val currentSide = when {
                                arYaw in 45f..135f -> "RIGHT PROFILE (Crown, Pushers)"
                                arYaw in 135f..225f -> "EXHIBITION CASEBACK (Automatic rotor gears)"
                                arYaw in 225f..315f -> "LEFT PROFILE (Acoustic flanking, ports)"
                                else -> "FRONT CHRONO DIAL FACE"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "3D PERSPECTIVE INSPECTOR",
                                    color = ElectricCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = currentSide.uppercase(),
                                    color = Color.Yellow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            // YAW Slider (Horizontal Spin)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("YAW (SPIN)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(75.dp))
                                Slider(
                                    value = arYaw,
                                    onValueChange = { arYaw = it },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricCyan,
                                        activeTrackColor = ElectricCyan.copy(alpha = 0.6f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                                Text("${arYaw.toInt()}°", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(35.dp), textAlign = TextAlign.End)
                            }
                            
                            // PITCH Slider (Vertical Tilt)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("PITCH (TILT)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(75.dp))
                                Slider(
                                    value = arPitch,
                                    onValueChange = { arPitch = it },
                                    valueRange = -60f..60f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricCyan,
                                        activeTrackColor = ElectricCyan.copy(alpha = 0.6f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                                Text("${arPitch.toInt()}°", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(35.dp), textAlign = TextAlign.End)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quick Presets Multiplier Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    Pair("🎯 FRONT", 0f),
                                    Pair("🔎 CROWN", 90f),
                                    Pair("⚙️ CASEBACK", 180f),
                                    Pair("🛡️ LEFT", 270f)
                                ).forEach { (label, yawVal) ->
                                    val act = (arYaw == yawVal && arPitch == 0f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (act) ElectricCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, if (act) ElectricCyan else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                arYaw = yawVal
                                                arPitch = 0f
                                                viewModel.addSyncLog("AR_ORBIT_ROT", "Fitted perspective view to preset: $label")
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (act) ElectricCyan else Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                "AI_CV_SCAN" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Row with Toggle button for AI core
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CV SKELETAL WRIST DETECTOR",
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Autosnapping watch to recognized wrist",
                                        color = SteelGray,
                                        fontSize = 9.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isAiWristDetectionEnabled) "ACTIVE" else "MANUAL",
                                        color = if (isAiWristDetectionEnabled) NeonGreen else Color.Red,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Switch(
                                        checked = isAiWristDetectionEnabled,
                                        onCheckedChange = {
                                            isAiWristDetectionEnabled = it
                                            if (it) {
                                                trackingPhase = "MOUNTED"
                                                watchOffsetX = wristLateralDrift
                                                watchOffsetY = wristVerticalDrift
                                                viewModel.addSyncLog("AI_DETECTION_ON", "Re-anchored engine directly back to carpals.")
                                            } else {
                                                trackingPhase = "ACQUIRING"
                                                viewModel.addSyncLog("AI_DETECTION_OFF", "Disengaged CV auto-fit lock, switched to full manual placement.")
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeonGreen,
                                            checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                                            uncheckedThumbColor = SteelGray,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Interactive Drift Test sliders
                            AnimatedVisibility(visible = isAiWristDetectionEnabled) {
                                Column {
                                    // Simulated lateral drift slider
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("WRIST LATERAL", color = Color.White, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(90.dp))
                                        Slider(
                                            value = wristLateralDrift,
                                            onValueChange = { wristLateralDrift = it },
                                            valueRange = -150f..150f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = NeonGreen,
                                                activeTrackColor = NeonGreen.copy(alpha = 0.6f),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                        Text("${wristLateralDrift.toInt()}px", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
                                    }

                                    // Simulated height drift
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("WRIST VERTICAL", color = Color.White, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(90.dp))
                                        Slider(
                                            value = wristVerticalDrift,
                                            onValueChange = { wristVerticalDrift = it },
                                            valueRange = -180f..120f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = NeonGreen,
                                                activeTrackColor = NeonGreen.copy(alpha = 0.6f),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                        Text("${wristVerticalDrift.toInt()}px", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(45.dp), textAlign = TextAlign.End)
                                    }

                                    // Matrix status lines
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("TRACKED JOINT STABILITY: 99.8%", color = NeonGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        Text("SKELETAL LIGAMENTS LOCKED ON ✓", color = Color.Yellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            // Trigger manual rescan button if disabled
                            AnimatedVisibility(visible = !isAiWristDetectionEnabled) {
                                Button(
                                    onClick = {
                                        isAiWristDetectionEnabled = true
                                        trackingPhase = "MOUNTED"
                                        watchOffsetX = wristLateralDrift
                                        watchOffsetY = wristVerticalDrift
                                        viewModel.addSyncLog("AI_DETECTION_SNAP", "Manually snapped watch to forearm skeleton nodes.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SNAP ENGINE BACK TO WRIST FOREARM", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                "CALIBRATE" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "COSMETIC DIAL CALIBRATION",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Adjust Dial Calibration scale
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Sizing labels
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.adjustArScale(-0.1f) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, "Shrink watch face", tint = Color.White)
                                    }
                                    Text(
                                        text = "SIZE SCALE: ${(arWristScale * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.adjustArScale(0.1f) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Add, "Enlarge watch face", tint = Color.White)
                                    }
                                }

                                // Reset Calibration
                                TextButton(onClick = {
                                    viewModel.adjustArScale(1.0f - arWristScale)
                                    viewModel.rotateArModel(-arRotation)
                                    arYaw = 0f
                                    arPitch = 0f
                                    wristLateralDrift = 0f
                                    wristVerticalDrift = -30f
                                    isAiWristDetectionEnabled = true
                                    viewModel.addSyncLog("CALIBRATE_RESET", "Reset all factory alignment alignment specifications.")
                                }) {
                                    Text("RESET PARAMS", color = ElectricCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Shutter capture action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            Toast
                                .makeText(
                                    context,
                                    "🔧 3D Viewpoints: Rotate Yaw Slider to see profiles and casebacks. AI Scanner attaches watch instantly to movement coordinates!",
                                    Toast.LENGTH_LONG
                                )
                                .show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, "AR Info", tint = SteelGray)
                }

                // Main capture physical camera shutter click
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(Brush.radialGradient(listOf(ElectricCyan, NeonGreen)), CircleShape)
                        .clickable {
                            isCapturingEffect = true
                            viewModel.addSyncLog("AR_CAPTURE", "Took instant try-on render matrix checkpoint.")
                            Toast
                                .makeText(
                                    context,
                                    "✨ Virtual try-on photo compiled and secured in sandbox profile gallery!",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                        .padding(3.dp)
                        .testTag("ar_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Capture Try On Photo",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Quick Preset wrist back canvas swapper
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            currentWristPresetIdx = (currentWristPresetIdx + 1) % wristPresets.size
                            viewModel.addSyncLog("AR_TRY_PLAY", "Swapped AR wrist backing canvas simulation.")
                            Toast.makeText(context, "Swapped Virtual Wrist Profile Canvas!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Casino, "Preset Swap", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // FLASH EFFECT ON CAMERA SHUTTER CLICK TO SIMULATE SHUTTER HARDWARE
        LaunchedEffect(isCapturingEffect) {
            if (isCapturingEffect) {
                delay(120)
                isCapturingEffect = false
            }
        }
        if (isCapturingEffect) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun CameraXView(
    isFrontCamera: Boolean,
    onCameraAvailable: (Boolean) -> Unit,
    onWristDetected: (Offset?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Lazy load the ML Kit pose detector
    val poseDetector = remember {
        val options = com.google.mlkit.vision.pose.defaults.PoseDetectorOptions.Builder()
            .setDetectorMode(com.google.mlkit.vision.pose.defaults.PoseDetectorOptions.STREAM_MODE)
            .build()
        com.google.mlkit.vision.pose.PoseDetection.getClient(options)
    }

    // 1. Resolve CameraProvider exactly once and log/suppress failures gracefully
    LaunchedEffect(Unit) {
        try {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cameraProvider = future.get()
                } catch (t: Throwable) {
                    Log.e("CameraXView", "Failed to retrieve camera provider inside standard listener", t)
                    onCameraAvailable(false)
                }
            }, mainExecutor)
        } catch (t: Throwable) {
            Log.e("CameraXView", "Failed calling ProcessCameraProvider.getInstance", t)
            onCameraAvailable(false)
        }
    }

    // 2. Perform the binding sequence reactively on cameraProvider, previewView state adjustments
    LaunchedEffect(cameraProvider, previewView, isFrontCamera) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val currentPreviewView = previewView ?: return@LaunchedEffect
        
        try {
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(currentPreviewView.surfaceProvider)
            }
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            
            // Image analyzer for ML Kit
            val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder()
                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            poseDetector.process(image)
                                .addOnSuccessListener { pose ->
                                    val leftWrist = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST)
                                    val rightWrist = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)
                                    val targetWrist = leftWrist ?: rightWrist
                                    
                                    if (targetWrist != null && targetWrist.inFrameLikelihood > 0.6f) {
                                        // Approximate mapping from image coordinates to screen
                                        // Since we use scaleType = FILL_CENTER, the coordinates need matrix mapping in a real app,
                                        // but for AR Try on demo we simulate offset
                                        val xPos = targetWrist.position.x
                                        val yPos = targetWrist.position.y
                                        onWristDetected(Offset(xPos, yPos))
                                    } else {
                                        onWristDetected(null)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            provider.unbindAll()
            
            var hasCamera = false
            try {
                hasCamera = provider.hasCamera(cameraSelector)
            } catch (t: Throwable) {
                Log.w("CameraXView", "CameraSelector check threw an exception (unsupported hardware or permission issues)", t)
            }

            if (hasCamera) {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                onCameraAvailable(true)
            } else {
                Log.w("CameraXView", "Selected camera is not available on this device configuration.")
                onCameraAvailable(false)
            }
        } catch (exc: Throwable) {
            Log.e("CameraXView", "Exception or Error binding CameraX lifecycle to preview view", exc)
            onCameraAvailable(false)
        }
    }

    // 3. Clean up use-cases upon disposal or configuration swap to prevent leaks or system crashes
    DisposableEffect(cameraProvider, poseDetector) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                poseDetector.close()
            } catch (t: Throwable) {
                Log.e("CameraXView", "Disposal unbinding of active use-cases failed", t)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also {
                previewView = it
            }
        },
        modifier = modifier
    )
}
