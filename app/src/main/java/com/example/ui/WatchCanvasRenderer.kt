package com.example.ui

import android.util.Log
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.data.DialStyle
import com.example.data.WatchItem
import java.util.Calendar
import java.util.Locale
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sqrt

// 3D Immersive Custom Rendering Engine for Vector Smartwatches
@Composable
fun WatchCanvasRenderer(
    watch: WatchItem,
    rotationDegreesX: Float, // Rotational spin (Y-axial visual/shimmer/3D angle tilt)
    rotationDegreesY: Float, // Pitch tilt (-45 to 45 degree angle tilt perspective)
    strapColorOverride: Color?,
    modifier: Modifier = Modifier
) {
    // Continuous time oscillator for mechanical gears/oscillators
    val infiniteTransition = rememberInfiniteTransition(label = "gears_3d")
    val gearAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(15000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "gearRotation3D"
    )

    // Pulsing glow factor for reactors and high-tech displays
    val techPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(2200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "techPulse"
    )

    Canvas(modifier = modifier) {
        try {
            val sizePx = size.minDimension
            val centerPoint = Offset(size.width / 2, size.height / 2)
            val radius = sizePx * 0.36f

            // Convert the interactive 3D swipe offsets into horizontal (tiltX) and vertical (tiltY) angles in dial-space
            // This simulates a full 3D gyroscope tracking watch movement
            val radX = Math.toRadians((rotationDegreesX).toDouble())
            val tiltX = (-sin(radX)).toFloat() * 24f
            val tiltY = rotationDegreesY * 0.55f // Directly mapped from the y-drag

            val normalizedX = ((rotationDegreesX % 360f) + 360f) % 360f
            val strapColor = strapColorOverride ?: watch.primaryColor

            // Utility to project layers at multiple heights Z inside the dimensional casing
            // This creates full stereoscopic parallax!
            fun project3D(z: Float): Offset {
                return Offset(centerPoint.x + tiltX * z, centerPoint.y + tiltY * z)
            }

            when {
                normalizedX in 45f..135f -> {
                    // DRAW RIGHT SIDE PROFILE (Yaw around 90°: crown & physical pusher buttons detail)
                    drawRightSideProfile(centerPoint, radius, strapColor, watch, techPulse, rotationDegreesY)
                }
                normalizedX in 135f..225f -> {
                    // DRAW EXHIBITION CASEBACK (Yaw around 180°: weighted moving rotor & inner clock train)
                    drawImmersiveCaseback(centerPoint, radius, strapColor, watch, techPulse, gearAngle, rotationDegreesX, rotationDegreesY)
                }
                normalizedX in 225f..315f -> {
                    // DRAW LEFT SIDE PROFILE (Yaw around 270°: sleek rounded casing flanking & vents)
                    drawLeftSideProfile(centerPoint, radius, strapColor, watch, techPulse, rotationDegreesY)
                }
                else -> {
                    // DEFAULT FRONT DIAL STUNNING VIEW
                    // ==================== LAYER 1: STRAP BACKGROUND (Z = -0.3f) ====================
                    drawImmersiveStrap(centerPoint, radius, strapColor, tiltX, tiltY, watch)

                    // ==================== LAYER 2: WATCH CASING BASE, CHASSIS & LUGS (Z = -0.1f to 0.0f) ====================
                    drawImmersiveCaseChassis(centerPoint, radius, tiltX, tiltY, watch, techPulse)

                    // ==================== LAYER 3: INNER DIAL FACEPLATE BACKING (Z = 0.05f) ====================
                    drawImmersiveDialBackground(centerPoint, radius, tiltX, tiltY, watch, techPulse, gearAngle)

                    // ==================== LAYER 4: MECHANICAL ENGINE & COG WHEELS (Z = 0.12f) ====================
                    drawImmersiveMechanicals(centerPoint, radius, tiltX, tiltY, watch, gearAngle, techPulse)

                    // ==================== LAYER 5: CARDINAL INDICES & VOLUMETRIC TICK MARKS (Z = 0.20f) ====================
                    drawImmersiveIndexesAndTicks(centerPoint, radius, tiltX, tiltY, watch)

                    // ==================== LAYER 6: SHADOWS OF THE HANDS FOR HEIGHT PERCEPTION (Z = 0.10f) ====================
                    drawImmersiveHandsShadows(centerPoint, radius, tiltX, tiltY, watch, gearAngle)

                    // ==================== LAYER 7: ACTIVE CHRONOMETRIC HANDS (Z = 0.28f) ====================
                    drawImmersiveHands(centerPoint, radius, tiltX, tiltY, watch, gearAngle)

                    // ==================== LAYER 8: REFLECTIVE SAPPHIRE GLASS DOME & SHIMMER (Z = 0.40f) ====================
                    drawImmersiveSapphireReflection(centerPoint, radius, tiltX, tiltY, rotationDegreesX)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("WatchCanvasRenderer", "Crash prevented during watch canvas draw", t)
        }
    }
}

private fun DrawScope.drawImmersiveStrap(
    center: Offset,
    dialRadius: Float,
    primaryColor: Color,
    tiltX: Float,
    tiltY: Float,
    watch: WatchItem
) {
    val pxWidth = dialRadius * 0.88f
    val strapLength = dialRadius * 1.7f
    val backgroundShift = Offset(tiltX * -0.2f, tiltY * -0.2f)
    val shiftedCenter = center + backgroundShift

    // Render depending on strap material type for amazing realism
    when (watch.id) {
        "vector-quantum" -> { // Carbon Fiber Mesh
            drawStrapShadowsAndBacking(shiftedCenter, pxWidth, strapLength, dialRadius)
            
            // Render military carbon fiber pattern with dual structural channels
            val topStrapPath = Path().apply {
                moveTo(shiftedCenter.x - pxWidth / 2, shiftedCenter.y - dialRadius * 0.5f)
                lineTo(shiftedCenter.x - pxWidth * 0.42f, shiftedCenter.y - strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.42f, shiftedCenter.y - strapLength)
                lineTo(shiftedCenter.x + pxWidth / 2, shiftedCenter.y - dialRadius * 0.5f)
                close()
            }
            val bottomStrapPath = Path().apply {
                moveTo(shiftedCenter.x - pxWidth / 2, shiftedCenter.y + dialRadius * 0.5f)
                lineTo(shiftedCenter.x - pxWidth * 0.45f, shiftedCenter.y + strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.45f, shiftedCenter.y + strapLength)
                lineTo(shiftedCenter.x + pxWidth / 2, shiftedCenter.y + dialRadius * 0.5f)
                close()
            }

            val carbonBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF202025), Color(0xFF0F0F12), Color(0xFF16161C))
            )
            drawPath(topStrapPath, brush = carbonBrush)
            drawPath(bottomStrapPath, brush = carbonBrush)

            // Dynamic grid texture to look exactly like layered carbon weave
            val weaveCount = 14
            for (i in 0..weaveCount) {
                val progress = i.toFloat() / weaveCount
                val offsetTopY = shiftedCenter.y - dialRadius * 0.6f - (strapLength - dialRadius * 0.6f) * progress
                val offsetBotY = shiftedCenter.y + dialRadius * 0.6f + (strapLength - dialRadius * 0.6f) * progress
                val currentWidth = pxWidth * (0.5f - 0.08f * progress)

                // Diagonal mesh grid cuts
                drawLine(
                    color = primaryColor.copy(alpha = 0.22f),
                    start = Offset(shiftedCenter.x - currentWidth, offsetTopY),
                    end = Offset(shiftedCenter.x + currentWidth, offsetTopY + 12f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.22f),
                    start = Offset(shiftedCenter.x + currentWidth, offsetTopY),
                    end = Offset(shiftedCenter.x - currentWidth, offsetTopY + 12f),
                    strokeWidth = 3f
                )
                
                drawLine(
                    color = primaryColor.copy(alpha = 0.22f),
                    start = Offset(shiftedCenter.x - currentWidth, offsetBotY),
                    end = Offset(shiftedCenter.x + currentWidth, offsetBotY + 12f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.22f),
                    start = Offset(shiftedCenter.x + currentWidth, offsetBotY),
                    end = Offset(shiftedCenter.x - currentWidth, offsetBotY + 12f),
                    strokeWidth = 3f
                )
            }
        }
        "vector-horizon" -> { // Pure Titanium Link Mesh
            drawStrapShadowsAndBacking(shiftedCenter, pxWidth, strapLength, dialRadius)
            
            val luxuryTitaniumBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF6B6E7B), Color(0xFFB5B8C4), Color(0xFF4A4B54)),
                start = Offset(shiftedCenter.x - pxWidth/2, shiftedCenter.y),
                end = Offset(shiftedCenter.x + pxWidth/2, shiftedCenter.y)
            )
            
            drawRect(
                brush = luxuryTitaniumBrush,
                topLeft = Offset(shiftedCenter.x - pxWidth * 0.45f, shiftedCenter.y - strapLength),
                size = Size(pxWidth * 0.9f, strapLength * 2f)
            )

            // Link segment highlights for immersive 3D texture
            val linksY = 16
            for (i in 1..linksY) {
                val tY = shiftedCenter.y - dialRadius * 0.5f - (strapLength - dialRadius * 0.5f) * (i.toFloat() / linksY)
                val bY = shiftedCenter.y + dialRadius * 0.5f + (strapLength - dialRadius * 0.5f) * (i.toFloat() / linksY)
                
                // Deep link separators
                drawLine(Color(0xFF1E2025), Offset(shiftedCenter.x - pxWidth * 0.45f, tY), Offset(shiftedCenter.x + pxWidth * 0.45f, tY), strokeWidth = 4f)
                drawLine(Color(0xFF1E2025), Offset(shiftedCenter.x - pxWidth * 0.45f, bY), Offset(shiftedCenter.x + pxWidth * 0.45f, bY), strokeWidth = 4f)

                // High polish links glint
                drawLine(Color.White.copy(alpha = 0.25f), Offset(shiftedCenter.x - pxWidth * 0.45f, tY + 4f), Offset(shiftedCenter.x + pxWidth * 0.45f, tY + 4f), strokeWidth = 2f)
                drawLine(Color.White.copy(alpha = 0.25f), Offset(shiftedCenter.x - pxWidth * 0.45f, bY + 4f), Offset(shiftedCenter.x + pxWidth * 0.45f, bY + 4f), strokeWidth = 2f)
            }
        }
        "vector-onyx" -> { // Obsidian Matte Silicon
            drawStrapShadowsAndBacking(shiftedCenter, pxWidth, strapLength, dialRadius)
            
            val topPath = Path().apply {
                moveTo(shiftedCenter.x - pxWidth * 0.47f, shiftedCenter.y - dialRadius * 0.5f)
                lineTo(shiftedCenter.x - pxWidth * 0.43f, shiftedCenter.y - strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.43f, shiftedCenter.y - strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.47f, shiftedCenter.y - dialRadius * 0.5f)
                close()
            }
            val bottomPath = Path().apply {
                moveTo(shiftedCenter.x - pxWidth * 0.47f, shiftedCenter.y + dialRadius * 0.5f)
                lineTo(shiftedCenter.x - pxWidth * 0.43f, shiftedCenter.y + strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.43f, shiftedCenter.y + strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.47f, shiftedCenter.y + dialRadius * 0.5f)
                close()
            }
            
            val siliconBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E1E22), Color(0xFF121215), Color(0xFF28282E)),
                start = Offset(shiftedCenter.x - pxWidth/2, shiftedCenter.y),
                end = Offset(shiftedCenter.x + pxWidth/2, shiftedCenter.y)
            )
            drawPath(topPath, brush = siliconBrush)
            drawPath(bottomPath, brush = siliconBrush)

            // Heavy center aesthetic channel glowing with deep obsidian crimson
            val centerTrackWidth = pxWidth * 0.24f
            drawRect(
                color = Color(0xFF0F0F11),
                topLeft = Offset(shiftedCenter.x - centerTrackWidth / 2, shiftedCenter.y - strapLength),
                size = Size(centerTrackWidth, strapLength * 2f)
            )
            
            // Crimson accent dashes inside the groove
            val dashCount = 8
            for (i in 1..dashCount) {
                val topDashY = shiftedCenter.y - dialRadius * 0.7f - (strapLength - dialRadius * 0.7f) * (i.toFloat() / dashCount)
                val botDashY = shiftedCenter.y + dialRadius * 0.7f + (strapLength - dialRadius * 0.7f) * (i.toFloat() / dashCount)
                
                drawRect(
                    color = primaryColor.copy(alpha = 0.55f),
                    topLeft = Offset(shiftedCenter.x - centerTrackWidth / 2.5f, topDashY),
                    size = Size(centerTrackWidth * 0.8f, 6f)
                )
                drawRect(
                    color = primaryColor.copy(alpha = 0.55f),
                    topLeft = Offset(shiftedCenter.x - centerTrackWidth /.5f, botDashY),
                    size = Size(centerTrackWidth * 0.8f, 6f)
                )
            }
        }
        else -> { // Sandblasted Gold-Titanium links for Chronos or standard analog
            drawStrapShadowsAndBacking(shiftedCenter, pxWidth, strapLength, dialRadius)
            
            val goldBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF7E602A), Color(0xFFE6C374), Color(0xFF4C3814), Color(0xFFC5A358)),
                start = Offset(shiftedCenter.x - pxWidth/2, shiftedCenter.y),
                end = Offset(shiftedCenter.x + pxWidth/2, shiftedCenter.y)
            )
            
            val topPath = Path().apply {
                moveTo(shiftedCenter.x - pxWidth * 0.48f, shiftedCenter.y - dialRadius * 0.5f)
                lineTo(shiftedCenter.x - pxWidth * 0.42f, shiftedCenter.y - strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.42f, shiftedCenter.y - strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.48f, shiftedCenter.y - dialRadius * 0.5f)
                close()
            }
            val bottomPath = Path().apply {
                moveTo(shiftedCenter.x - pxWidth * 0.48f, shiftedCenter.y + dialRadius * 0.5f)
                lineTo(shiftedCenter.x - pxWidth * 0.42f, shiftedCenter.y + strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.42f, shiftedCenter.y + strapLength)
                lineTo(shiftedCenter.x + pxWidth * 0.48f, shiftedCenter.y + dialRadius * 0.5f)
                close()
            }
            drawPath(topPath, brush = goldBrush)
            drawPath(bottomPath, brush = goldBrush)

            // Heavy 3-row blocky link pattern
            val blockCount = 10
            for (i in 0..blockCount) {
                val progress = i.toFloat() / blockCount
                val tY = shiftedCenter.y - dialRadius * 0.6f - (strapLength - dialRadius * 0.6f) * progress
                val bY = shiftedCenter.y + dialRadius * 0.6f + (strapLength - dialRadius * 0.6f) * progress
                val w = pxWidth * (0.48f - 0.06f * progress)

                // draw vertical dividing lines for the three rows of links
                drawLine(Color(0xFF1A1205), Offset(shiftedCenter.x - w * 0.33f, tY), Offset(shiftedCenter.x - w * 0.33f, tY - 14f), strokeWidth = 3f)
                drawLine(Color(0xFF1A1205), Offset(shiftedCenter.x + w * 0.33f, tY), Offset(shiftedCenter.x + w * 0.33f, tY - 14f), strokeWidth = 3f)
                drawLine(Color(0xFF1A1205), Offset(shiftedCenter.x - w * 0.33f, bY), Offset(shiftedCenter.x - w * 0.33f, bY + 14f), strokeWidth = 3f)
                drawLine(Color(0xFF1A1205), Offset(shiftedCenter.x + w * 0.33f, bY), Offset(shiftedCenter.x + w * 0.33f, bY + 14f), strokeWidth = 3f)

                // horizontal blocks
                drawLine(Color(0xFF281C08), Offset(shiftedCenter.x - w, tY), Offset(shiftedCenter.x + w, tY), strokeWidth = 4f)
                drawLine(Color(0xFF281C08), Offset(shiftedCenter.x - w, bY), Offset(shiftedCenter.x + w, bY), strokeWidth = 4f)
            }
        }
    }
}

private fun DrawScope.drawStrapShadowsAndBacking(
    pos: Offset,
    w: Float,
    len: Float,
    r: Float
) {
    // Ambient drop shadow under strap links for dimensional hover
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = Offset(pos.x - w * 0.52f, pos.y - len - 10f),
        size = Size(w * 1.04f, len * 2f + 20f),
        cornerRadius = CornerRadius(16f),
        style = Stroke(width = r * 0.12f)
    )
}

private fun DrawScope.drawImmersiveCaseChassis(
    center: Offset,
    radius: Float,
    tiltX: Float,
    tiltY: Float,
    watch: WatchItem,
    techPulse: Float
) {
    // Drop shadow under the case
    val shadowOffset = Offset(tiltX * -0.2f + 16f, tiltY * -0.2f + 22f)
    drawCircle(
        color = Color.Black.copy(alpha = 0.65f),
        radius = radius * 1.12f,
        center = center + shadowOffset
    )

    // Select dynamic case models!
    when (watch.id) {
        "vector-onyx" -> {
            // Rugged Octagonal Faceted 3D Bezel
            // We calculate 3D vertices of an octagon of radius caseRadius
            val caseRadius = radius * 1.15f
            val verticesOuter = ArrayList<Offset>()
            val verticesInner = ArrayList<Offset>()
            
            // Generate coordinates for outer and inner bezel rings
            for (i in 0 until 8) {
                val angle = i * PI / 4.0 + (PI / 8.0) // 45 degree intervals rotated
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                
                // Project outward incorporating 3D tilt coordinates
                verticesOuter.add(Offset(
                    center.x + (caseRadius * cosA) + (tiltX * -0.05f),
                    center.y + (caseRadius * sinA) + (tiltY * -0.05f)
                ))
                verticesInner.add(Offset(
                    center.x + (radius * 0.95f * cosA) + (tiltX * 0.05f),
                    center.y + (radius * 0.95f * sinA) + (tiltY * 0.05f)
                ))
            }

            // Draw the 8 faceted panels connecting inner to outer vertices
            // Give them dynamic light shading depending on light source position (top-left)
            for (i in 0 until 8) {
                val next = (i + 1) % 8
                val partPath = Path().apply {
                    moveTo(verticesOuter[i].x, verticesOuter[i].y)
                    lineTo(verticesOuter[next].x, verticesOuter[next].y)
                    lineTo(verticesInner[next].x, verticesInner[next].y)
                    lineTo(verticesInner[i].x, verticesInner[i].y)
                    close()
                }

                // Simulate light source from angle ~135 degrees (top-left) by analyzing normal quadrant
                val midpointAngle = (i * 45f + 22.5f) * PI / 180f
                val lightDot = (cos(midpointAngle - PI*0.75) + 1.0) / 2.0 // Shading factor 0 to 1
                
                // Deep obsidian color with red atomic heat sheen inside
                val baseShadeValue = (30 + 55 * lightDot).toInt()
                val crimsonGlow = if (i % 2 == 0) (8 * techPulse).toInt() else 0
                val facetColor = Color(
                    red = (baseShadeValue + crimsonGlow + 25).coerceIn(0, 255),
                    green = (baseShadeValue).coerceIn(0, 255),
                    blue = (baseShadeValue + 8).coerceIn(0, 255)
                )

                drawPath(partPath, color = facetColor)
                // Highlights on the edges
                drawPath(partPath, color = Color.White.copy(alpha = 0.06f * lightDot.toFloat()), style = Stroke(width = 2f))
            }

            // Add chunky armor studs at the cardinal octagon indices
            for (i in 0 until 8 step 2) {
                val pos = verticesOuter[i]
                drawCircle(Color(0xFF1E1E22), radius = 6f, center = pos)
                drawCircle(Color(0xFFFF3366).copy(alpha = 0.4f * techPulse), radius = 4f, center = pos)
            }
        }
        "vector-quantum" -> {
            // Aerospace Carbon Composite with Neon Ring Inserts
            val caseRadius = radius * 1.1f
            // Base carbon circular casing
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E2E36), Color(0xFF121215)),
                    center = center + Offset(tiltX * -0.05f, tiltY * -0.05f),
                    radius = caseRadius
                ),
                radius = caseRadius,
                center = center + Offset(tiltX * -0.05f, tiltY * -0.05f)
            )

            // Tech circular neon alignment groove
            drawCircle(
                color = watch.primaryColor.copy(alpha = 0.8f * techPulse),
                radius = radius * 1.05f,
                center = center + Offset(tiltX * 0.02f, tiltY * 0.02f),
                style = Stroke(width = 3f)
            )

            // Micro fiber alignment plates
            for (i in 0 until 12) {
                val angleDeg = i * 30f
                withTransform({
                    rotate(angleDeg, pivot = center)
                }) {
                    drawRect(
                        color = Color(0xFF0F1012),
                        topLeft = Offset(center.x - 3f, center.y - caseRadius),
                        size = Size(6f, 12f)
                    )
                }
            }
        }
        "vector-horizon" -> {
            // Highly Polished Edge-to-Edge Gold Chassis
            val caseRadius = radius * 1.12f
            
            // Outer golden ring with sweeping gradient highlights for absolute realism
            val goldGlintBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFE5C158), Color(0xFF7C6026), Color(0xFFFFF0A5), 
                    Color(0xFF9E7C33), Color(0xFFFFF7CE), Color(0xFFE5C158)
                ),
                center = center + Offset(tiltX * -0.06f, tiltY * -0.06f)
            )
            
            drawCircle(
                brush = goldGlintBrush,
                radius = caseRadius,
                center = center + Offset(tiltX * -0.06f, tiltY * -0.06f)
            )

            // Inner dark chronometer ring
            drawCircle(
                color = Color(0xFF1A1E24),
                radius = radius * 1.04f,
                center = center + Offset(tiltX * -0.02f, tiltY * -0.02f)
            )

            // Secondary diamond cuts
            for (i in 0 until 12) {
                val angleRad = (i * 30 * PI / 180f).toFloat()
                val pos = Offset(
                    center.x + cos(angleRad) * radius * 1.06f,
                    center.y + sin(angleRad) * radius * 1.06f + tiltY * -0.02f
                )
                drawCircle(Color(0xFFFFF7CE), radius = 2.5f, center = pos)
            }
        }
        else -> { // Grade 5 sandblasted titanium bezel for Chronos
            val caseRadius = radius * 1.1f
            val baseChassisOffset = Offset(tiltX * -0.05f, tiltY * -0.05f)
            
            // Sandblasted titanium radial shimmer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF5A5C66), Color(0xFF28292E)),
                    center = center + baseChassisOffset,
                    radius = caseRadius
                ),
                radius = caseRadius,
                center = center + baseChassisOffset
            )

            // Chronos outer tachymeter scale ticks
            drawCircle(
                color = Color(0xFF3B3D44),
                radius = radius * 1.06f,
                center = center + baseChassisOffset,
                style = Stroke(width = 12f)
            )

            // Tachymetre index dots
            for (i in 0 until 24) {
                val angleDeg = i * 15f
                withTransform({
                    rotate(angleDeg, pivot = center + baseChassisOffset)
                }) {
                    drawLine(
                        color = Color(0xFFFFCC00).copy(alpha = 0.7f),
                        start = Offset(center.x, center.y - radius * 1.08f + baseChassisOffset.y),
                        end = Offset(center.x, center.y - radius * 1.04f + baseChassisOffset.y),
                        strokeWidth = 3f
                    )
                }
            }
        }
    }

    // Interactive mechanical crown dynamic push-button (Z = 0.02f)
    val buttonOffset = Offset(tiltX * 0.05f, tiltY * 0.05f)
    val crownPos = center + Offset(radius * 1.08f, 0f) + buttonOffset
    
    val crownBrush = Brush.verticalGradient(
        colors = listOf(watch.primaryColor.copy(alpha = 0.8f), watch.primaryColor.copy(alpha = 0.3f))
    )
    
    // Extruded crown casing on the right
    drawRoundRect(
        color = Color(0xFF151619),
        topLeft = Offset(crownPos.x - 4f, crownPos.y - radius * 0.18f),
        size = Size(24f, radius * 0.36f),
        cornerRadius = CornerRadius(4f)
    )
    drawRoundRect(
        brush = crownBrush,
        topLeft = Offset(crownPos.x + 8f, crownPos.y - radius * 0.12f),
        size = Size(10f, radius * 0.24f),
        cornerRadius = CornerRadius(2f)
    )
    // Dynamic mini red core cap inside crown
    drawCircle(watch.primaryColor, radius = 2.5f, center = Offset(crownPos.x + 15f, crownPos.y))
}

private fun DrawScope.drawImmersiveDialBackground(
    center: Offset,
    radius: Float,
    tiltX: Float,
    tiltY: Float,
    watch: WatchItem,
    techPulse: Float,
    gearAngle: Float
) {
    val dialOffset = Offset(tiltX * 0.05f, tiltY * 0.05f)
    val dialCenter = center + dialOffset
    val dialRadius = radius * 0.98f

    // Super-deep black void background
    val darkVoidBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF0F1015), Color(0xFF030305)),
        center = dialCenter,
        radius = dialRadius
    )
    drawCircle(brush = darkVoidBrush, radius = dialRadius, center = dialCenter)

    when (watch.id) {
        "vector-quantum" -> { // TECH_MATRIX
            // Cyber Grid concentric patterns with responsive coordinates
            val gridStep = 22f
            val maxBound = dialRadius * 0.92f
            for (x in -8..8) {
                val gridX = dialCenter.x + x * gridStep
                val dx = gridX - dialCenter.x
                for (y in -8..8) {
                    val gridY = dialCenter.y + y * gridStep
                    val dy = gridY - dialCenter.y
                    if (sqrt(dx*dx + dy*dy) < maxBound) {
                        // Drawing miniature green circular grids that glow based on techPulse
                        drawCircle(
                            color = watch.primaryColor.copy(alpha = 0.07f * techPulse),
                            radius = 1.5f,
                            center = Offset(gridX, gridY)
                        )
                    }
                }
            }

            // Radial digital telemetry lines
            drawCircle(
                color = watch.primaryColor.copy(alpha = 0.12f),
                radius = dialRadius * 0.75f,
                center = dialCenter,
                style = Stroke(width = 2f)
            )

            // Matrix Binary waterfall streams (interactive falling data nodes computed dynamically)
            val matrixOffset = (gearAngle * 0.25f) % 40f
            for (col in -3..3) {
                val colX = dialCenter.x + col * (dialRadius * 0.24f)
                val baseStartY = dialCenter.y - dialRadius * 0.7f + matrixOffset
                for (b in 0..4) {
                    val nodeY = baseStartY + b * 22f
                    val dy = nodeY - dialCenter.y
                    val dx = colX - dialCenter.x
                    if (sqrt(dx*dx + dy*dy) < dialRadius * 0.85f) {
                        val digit = if ((b + col) % 2 == 0) "1" else "0"
                        // Glow trailing nodes
                        val nodeAlpha = (0.05f + 0.15f * (b.toFloat() / 5f)) * techPulse
                        drawCircle(
                            color = watch.primaryColor.copy(alpha = nodeAlpha),
                            radius = 4f,
                            center = Offset(colX, nodeY)
                        )
                    }
                }
            }
        }
        "vector-horizon" -> { // TOURBILLON
            // Inner frame exposing mechanical skeleton plates
            drawCircle(
                color = Color(0xFF1E2129),
                radius = dialRadius * 0.88f,
                center = dialCenter,
                style = Stroke(width = radius * 0.08f)
            )

            // Circular brushing geometry for gold clock plates beneath
            val circularPlateGradient = Brush.radialGradient(
                colors = listOf(Color(0xFF1F1C15), Color(0xFF12100C)),
                center = dialCenter,
                radius = dialRadius
            )
            drawCircle(brush = circularPlateGradient, radius = dialRadius * 0.86f, center = dialCenter)

            // Inner brass bridge cut-out pathways
            val cutoutPath = Path().apply {
                addArc(
                    oval = Rect(dialCenter, dialRadius * 0.65f),
                    startAngleDegrees = 45f,
                    sweepAngleDegrees = 270f
                )
            }
            // Emulating metallic framing edges on standard drawing canvas
            drawCircle(
                color = Color(0xFF9E7C33).copy(alpha = 0.3f),
                radius = dialRadius * 0.65f,
                center = dialCenter,
                style = Stroke(width = 3f)
            )
        }
        "vector-onyx" -> { // ANALOG
            // Volumetric Horizontal Carbon Slate Grooves
            val slateStep = 18f
            val clipLimit = dialRadius * 0.9f
            for (i in -10..10) {
                val sY = dialCenter.y + i * slateStep
                val dy = sY - dialCenter.y
                // Solve intersection limit inside circular dial
                val chordHalfLength = sqrt((clipLimit * clipLimit) - (dy * dy).coerceAtLeast(0f))
                if (!chordHalfLength.isNaN() && chordHalfLength > 0f) {
                    // Top light reflection edge
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(dialCenter.x - chordHalfLength, sY - 1.5f),
                        end = Offset(dialCenter.x + chordHalfLength, sY - 1.5f),
                        strokeWidth = 2.5f
                    )
                    // Core dark slate groove groove
                    drawLine(
                        color = Color.Black.copy(alpha = 0.8f),
                        start = Offset(dialCenter.x - chordHalfLength, sY),
                        end = Offset(dialCenter.x + chordHalfLength, sY),
                        strokeWidth = 4f
                    )
                }
            }

            // Crosshair tactical center alignment lines
            drawLine(
                color = watch.primaryColor.copy(alpha = 0.16f),
                start = Offset(dialCenter.x - dialRadius * 0.8f, dialCenter.y),
                end = Offset(dialCenter.x + dialRadius * 0.8f, dialCenter.y),
                strokeWidth = 2f
            )
            drawLine(
                color = watch.primaryColor.copy(alpha = 0.16f),
                start = Offset(dialCenter.x, dialCenter.y - dialRadius * 0.8f),
                end = Offset(dialCenter.x, dialCenter.y + dialRadius * 0.8f),
                strokeWidth = 2f
            )
        }
        else -> { // CHRONOGRAPH (Chronos)
            // Sandblasted carbon racing outer trim and concentric chronograph grids
            drawCircle(
                color = Color(0xFF22242D),
                radius = dialRadius * 0.88f,
                center = dialCenter,
                style = Stroke(width = 3f)
            )

            // Minute scale outer subdivisions
            for (i in 0 until 120) {
                val angleDeg = i * 3f
                val isSecondTick = i % 2 == 0
                val tickLength = if (i % 10 == 0) 14f else if (isSecondTick) 8f else 4f
                val alpha = if (i % 10 == 0) 0.5f else 0.18f
                
                withTransform({
                    rotate(angleDeg, pivot = dialCenter)
                }) {
                    drawLine(
                        color = Color.White.copy(alpha = alpha),
                        start = Offset(dialCenter.x, dialCenter.y - dialRadius * 0.88f),
                        end = Offset(dialCenter.x, dialCenter.y - dialRadius * 0.88f + tickLength),
                        strokeWidth = if (i % 10 == 0) 3f else 1.5f
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawImmersiveMechanicals(
    center: Offset,
    radius: Float,
    tiltX: Float,
    tiltY: Float,
    watch: WatchItem,
    gearAngle: Float,
    techPulse: Float
) {
    // Parallax depth calculation specifically for gears (Z = 0.1f)
    val layerOffset = Offset(tiltX * 0.1f, tiltY * 0.1f)
    val gearCenter = center + layerOffset
    val dialRadius = radius * 0.95f

    when (watch.id) {
        "vector-horizon" -> { // TOURBILLON
            // Complete visible gear train cascading into the main Escapement and balance wheel
            
            // 1. Core escapement background plate
            drawCircle(
                color = Color(0xFF1E1C15),
                radius = dialRadius * 0.44f,
                center = gearCenter + Offset(0f, dialRadius * 0.3f),
                style = Stroke(width = 4f)
            )

            // 2. Continuous rotating fourth wheel and gear pivots (Clockwise)
            rotate(gearAngle, pivot = gearCenter + Offset(-dialRadius * 0.28f, -dialRadius * 0.2f)) {
                drawMechanicalCogwheel3D(
                    pos = gearCenter + Offset(-dialRadius * 0.28f, -dialRadius * 0.2f),
                    innerRadius = dialRadius * 0.22f,
                    teethCount = 18,
                    color = Color(0xFF8C6D2C),
                    spokeCount = 5
                )
            }

            // 3. Interlocking third wheel pinion (Counter-Clockwise speed sweep)
            rotate(-gearAngle * 1.6f, pivot = gearCenter + Offset(dialRadius * 0.15f, -dialRadius * 0.1f)) {
                drawMechanicalCogwheel3D(
                    pos = gearCenter + Offset(dialRadius * 0.15f, -dialRadius * 0.1f),
                    innerRadius = dialRadius * 0.16f,
                    teethCount = 14,
                    color = Color(0xFF70727F),
                    spokeCount = 4
                )
            }

            // 4. HEARTBEAT OF THE WATCH: Dynamic contracting/expanding Tourbillon hairspring
            val hairspringCenter = gearCenter + Offset(0f, dialRadius * 0.35f)
            // Beautiful rotating escapement balance wheel
            rotate(sin(gearAngle * 0.25f) * 140f, pivot = hairspringCenter) {
                // Outer gold balance wheel with dynamic balance-weight screws
                drawCircle(Color(0xFFE5C158), radius = dialRadius * 0.24f, center = hairspringCenter, style = Stroke(width = 5f))
                for (s in 0 until 12) {
                    val screwAngleRad = (s * 30 * PI / 180.0)
                    val sX = hairspringCenter.x + cos(screwAngleRad).toFloat() * dialRadius * 0.24f
                    val sY = hairspringCenter.y + sin(screwAngleRad).toFloat() * dialRadius * 0.24f
                    drawCircle(Color.White, radius = 2.5f, center = Offset(sX, sY))
                }
                
                // Draw 3 spokes of the balance wheel
                for (sp in 0 until 3) {
                    val spAngle = sp * 120f
                    withTransform({
                        rotate(spAngle, pivot = hairspringCenter)
                    }) {
                        drawLine(
                            color = Color(0xFFC4A045),
                            start = hairspringCenter,
                            end = Offset(hairspringCenter.x, hairspringCenter.y - dialRadius * 0.24f),
                            strokeWidth = 4f
                        )
                    }
                }
            }

            // Contracting metal hairspring (3D spirals!)
            val springPulse = 1.0f + sin(gearAngle * 0.25f) * 0.08f
            val hairspringPath = Path()
            val springTurns = 4.5f
            val totalPoints = 120
            for (p in 0..totalPoints) {
                val t = p.toFloat() / totalPoints
                val angleSpring = t * springTurns * 2f * PI
                // Exponential spiral size
                val dR = (dialRadius * 0.03f) + (dialRadius * 0.12f) * t * springPulse
                val springX = hairspringCenter.x + cos(angleSpring).toFloat() * dR
                val springY = hairspringCenter.y + sin(angleSpring).toFloat() * dR
                
                if (p == 0) {
                    hairspringPath.moveTo(springX, springY)
                } else {
                    hairspringPath.lineTo(springX, springY)
                }
            }
            drawPath(
                path = hairspringPath,
                color = Color(0xFF6B7280),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Sparkling transcluent synthetic ruby jewels at the critical pivot center
            drawCircle(Color(0xFFFF1A8C).copy(alpha = 0.5f), radius = 8f, center = hairspringCenter)
            drawCircle(Color(0xFFE60060), radius = 5f, center = hairspringCenter)
            drawCircle(Color.White, radius = 1.5f, center = hairspringCenter + Offset(-1.5f, -1.5f))
        }
        "vector-quantum" -> { // TECH_MATRIX
            // Cybernetic kinetic core (Reactor core)
            val reactorCenter = gearCenter + Offset(0f, dialRadius * 0.28f)
            
            // Background ambient glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(watch.primaryColor.copy(alpha = 0.45f * techPulse), Color.Transparent),
                    center = reactorCenter,
                    radius = dialRadius * 0.45f
                ),
                radius = dialRadius * 0.45f,
                center = reactorCenter
            )

            // Double opposing matrix fins spinning
            rotate(-gearAngle, pivot = reactorCenter) {
                drawCircle(
                    color = watch.primaryColor.copy(alpha = 0.2f),
                    radius = dialRadius * 0.32f,
                    center = reactorCenter,
                    style = Stroke(width = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 25f)))
                )
            }
            rotate(gearAngle * 1.5f, pivot = reactorCenter) {
                drawCircle(
                    color = watch.primaryColor.copy(alpha = 0.4f),
                    radius = dialRadius * 0.24f,
                    center = reactorCenter,
                    style = Stroke(width = 6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(25f, 15f)))
                )
            }

            // Power Reactor Solid Core
            drawCircle(Color(0xFF151821), radius = dialRadius * 0.15f, center = reactorCenter)
            drawCircle(
                color = watch.primaryColor.copy(alpha = 0.85f * techPulse),
                radius = dialRadius * 0.12f,
                center = reactorCenter
            )
            drawCircle(Color.White, radius = dialRadius * 0.05f, center = reactorCenter)
        }
        "vector-chronos" -> { // CHRONOGRAPH (Dual active subdials measuring actual ticking motion!)
            val subdialRadius = dialRadius * 0.23f
            val topSubdialCenter = gearCenter + Offset(0f, -dialRadius * 0.33f)
            val bottomSubdialCenter = gearCenter + Offset(0f, dialRadius * 0.35f)

            // Upper Subdial: Ticking Chrono Milisecond Gauge
            drawCircle(Color(0xFF151619), radius = subdialRadius, center = topSubdialCenter)
            drawCircle(Color.White.copy(alpha = 0.15f), radius = subdialRadius, center = topSubdialCenter, style = Stroke(width = 2f))
            // 4 main quadrants markings
            for (i in 0 until 4) {
                val rA = i * 90f
                withTransform({ rotate(rA, topSubdialCenter) }) {
                    drawLine(Color(0xFFFFB300), topSubdialCenter - Offset(0f, subdialRadius), topSubdialCenter - Offset(0f, subdialRadius * 0.72f), strokeWidth = 3f)
                }
            }
            // Ultra fast sweeping active hand
            rotate(gearAngle * 8f, pivot = topSubdialCenter) {
                drawLine(
                    color = Color(0xFFFFB300),
                    start = topSubdialCenter,
                    end = topSubdialCenter - Offset(0f, subdialRadius * 0.85f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(Color(0xFFFFB300), radius = 3f, center = topSubdialCenter)

            // Lower Subdial: Dual Hours and Minutes gauge in neon green
            drawCircle(Color(0xFF151619), radius = subdialRadius, center = bottomSubdialCenter)
            drawCircle(watch.primaryColor.copy(alpha = 0.3f), radius = subdialRadius, center = bottomSubdialCenter, style = Stroke(width = 3f))
            for (i in 0 until 12) {
                val rA = i * 30f
                withTransform({ rotate(rA, bottomSubdialCenter) }) {
                    drawLine(watch.primaryColor, bottomSubdialCenter - Offset(0f, subdialRadius), bottomSubdialCenter - Offset(0f, subdialRadius * 0.8f), strokeWidth = 2f)
                }
            }
            // Normal sweep hand representation
            rotate(gearAngle * 0.1f, pivot = bottomSubdialCenter) {
                drawLine(
                    color = watch.primaryColor,
                    start = bottomSubdialCenter,
                    end = bottomSubdialCenter - Offset(0f, subdialRadius * 0.8f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(watch.primaryColor, radius = 3.5f, center = bottomSubdialCenter)
        }
        else -> { // Standard mechanical escapement cog inside Vector Onyx analog split preview
            val triangularHolePos = gearCenter + Offset(-dialRadius * 0.35f, 0f)
            // Cutout casing representation
            drawCircle(
                color = Color.Black,
                radius = dialRadius * 0.22f,
                center = triangularHolePos
            )
            drawCircle(
                color = watch.primaryColor.copy(alpha = 0.5f),
                radius = dialRadius * 0.22f,
                center = triangularHolePos,
                style = Stroke(width = 2.5f)
            )

            rotate(-gearAngle, pivot = triangularHolePos) {
                drawMechanicalCogwheel3D(
                    pos = triangularHolePos,
                    innerRadius = dialRadius * 0.18f,
                    teethCount = 10,
                    color = Color(0xFF6B1A24),
                    spokeCount = 3
                )
            }
        }
    }
}

private fun DrawScope.drawImmersiveIndexesAndTicks(
    center: Offset,
    radius: Float,
    tiltX: Float,
    tiltY: Float,
    watch: WatchItem
) {
    // Parallax Height Projection:
    // We render indexes in 3D by drawing a base block at depth Z=0.08f, projecting sides to Z=0.22f, and placing neon caps on top
    val getBaseOffset = Offset(tiltX * 0.05f, tiltY * 0.05f)
    val getTopOffset = Offset(tiltX * 0.22f, tiltY * 0.22f)

    val dialRadius = radius * 0.88f
    val baseCenter = center + getBaseOffset
    val topCenter = center + getTopOffset

    val majorMarkerColor = if (watch.id == "vector-onyx") Color(0xFFFF3366) else watch.primaryColor

    for (hour in 1..12) {
        val angleDeg = hour * 30f
        val angleRad = (angleDeg * PI / 180f).toFloat()
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        // Point calculations for hour indices
        val markerLength = if (hour % 3 == 0) radius * 0.18f else radius * 0.1f
        val markerWidth = if (hour % 3 == 0) 12f else 6f

        val indexPosBase = Offset(
            baseCenter.x + cosA * (dialRadius - markerLength),
            baseCenter.y + sinA * (dialRadius - markerLength)
        )
        val indexPosTop = Offset(
            topCenter.x + cosA * (dialRadius - markerLength),
            topCenter.y + sinA * (dialRadius - markerLength)
        )

        // 1. Draw volumetric 3D extruded dimensional column
        val sidePath1 = Path().apply {
            moveTo(indexPosBase.x - markerWidth / 2, indexPosBase.y)
            lineTo(indexPosTop.x - markerWidth / 2, indexPosTop.y)
            lineTo(indexPosTop.x + markerWidth / 2, indexPosTop.y)
            lineTo(indexPosBase.x + markerWidth / 2, indexPosBase.y)
            close()
        }
        // Darkened block shading for depth
        drawPath(sidePath1, color = Color.Black.copy(alpha = 0.5f))
        drawPath(sidePath1, color = majorMarkerColor.copy(alpha = 0.35f))

        // 2. Neon luminous cap on top
        if (hour % 3 == 0) { // Distinct high-tech cardinal bars
            drawRect3D(
                centerPos = indexPosTop,
                size = Size(markerWidth, markerLength),
                angleRad = angleRad,
                color = if (hour == 12) Color.White else majorMarkerColor
            )
        } else {
            drawCircle(Color.White, radius = 3.5f, center = indexPosTop)
            drawCircle(majorMarkerColor, radius = 2f, center = indexPosTop)
        }
    }
}

private fun DrawScope.drawImmersiveHandsShadows(
    center: Offset,
    radius: Float,
    tiltX: Float,
    tiltY: Float,
    watch: WatchItem,
    gearAngle: Float
) {
    // Distribute shadows with offset in opposite direction of perspective tilt, mimicking a top light source
    val shadowDisplacement = Offset(tiltX * -0.05f + 12f, tiltY * -0.05f + 16f)
    val shadowCenter = center + shadowDisplacement
    val handRadius = radius * 0.85f

    val calendar = Calendar.getInstance()
    val sec = calendar.get(Calendar.SECOND) + (calendar.get(Calendar.MILLISECOND) / 1000f)
    val min = calendar.get(Calendar.MINUTE) + (sec / 60f)
    val hr = (calendar.get(Calendar.HOUR) % 12) + (min / 60f)

    val shadowColor = Color.Black.copy(alpha = 0.55f)

    // Hour Shadow (Sweeping)
    withTransform({
        rotate(hr * 30f, pivot = shadowCenter)
    }) {
        drawLine(
            color = shadowColor,
            start = shadowCenter,
            end = shadowCenter - Offset(0f, handRadius * 0.45f),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
    }

    // Minute Shadow
    withTransform({
        rotate(min * 6f, pivot = shadowCenter)
    }) {
        drawLine(
            color = shadowColor,
            start = shadowCenter,
            end = shadowCenter - Offset(0f, handRadius * 0.72f),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
    }

    // Second Shadow
    withTransform({
        rotate(sec * 6f, pivot = shadowCenter)
    }) {
        drawLine(
            color = shadowColor,
            start = shadowCenter,
            end = shadowCenter - Offset(0f, handRadius * 0.88f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawImmersiveHands(
    center: Offset,
    radius: Float,
    tiltX: Float,
    tiltY: Float,
    watch: WatchItem,
    gearAngle: Float
) {
    // Hands are drawn on the front plane (Z = 0.28f)
    val handsOffset = Offset(tiltX * 0.28f, tiltY * 0.28f)
    val handsCenter = center + handsOffset
    val handRadius = radius * 0.85f

    val calendar = Calendar.getInstance()
    val sec = calendar.get(Calendar.SECOND) + (calendar.get(Calendar.MILLISECOND) / 1000f)
    val min = calendar.get(Calendar.MINUTE) + (sec / 60f)
    val hr = (calendar.get(Calendar.HOUR) % 12) + (min / 60f)

    // Dynamic hand styling based on active watch theme
    when (watch.id) {
        "vector-quantum" -> { // Hyper futuristic skeletonized glowing hands
            // Sweeping Hour hand
            withTransform({ rotate(hr * 30f, pivot = handsCenter) }) {
                drawLine(Color(0xFF1E2129), handsCenter, handsCenter - Offset(0f, handRadius * 0.45f), strokeWidth = 9f, cap = StrokeCap.Round)
                drawLine(watch.primaryColor, handsCenter - Offset(0f, handRadius * 0.1f), handsCenter - Offset(0f, handRadius * 0.43f), strokeWidth = 3f)
            }
            // Sweeping Minute hand
            withTransform({ rotate(min * 6f, pivot = handsCenter) }) {
                drawLine(Color(0xFF1E2129), handsCenter, handsCenter - Offset(0f, handRadius * 0.75f), strokeWidth = 7f, cap = StrokeCap.Round)
                drawLine(watch.primaryColor, handsCenter - Offset(0f, handRadius * 0.2f), handsCenter - Offset(0f, handRadius * 0.73f), strokeWidth = 3.5f)
            }
            // Ultra-fine neon seconds needle
            withTransform({ rotate(sec * 6f, pivot = handsCenter) }) {
                drawLine(watch.primaryColor, handsCenter, handsCenter - Offset(0f, handRadius * 0.88f), strokeWidth = 2.5f)
                drawCircle(watch.primaryColor, radius = 7f, center = handsCenter)
                drawCircle(Color.White, radius = 3f, center = handsCenter)
            }
        }
        "vector-horizon" -> { // Luxury Gold Lancet Tapered style
            withTransform({ rotate(hr * 30f, pivot = handsCenter) }) {
                val pathHour = Path().apply {
                    moveTo(handsCenter.x, handsCenter.y + 12f)
                    lineTo(handsCenter.x - 7f, handsCenter.y)
                    lineTo(handsCenter.x, handsCenter.y - handRadius * 0.46f)
                    lineTo(handsCenter.x + 7f, handsCenter.y)
                    close()
                }
                drawPath(pathHour, color = Color(0xFFC5A358))
                drawPath(pathHour, color = Color.White, style = Stroke(width = 1.5f))
            }
            withTransform({ rotate(min * 6f, pivot = handsCenter) }) {
                val pathMin = Path().apply {
                    moveTo(handsCenter.x, handsCenter.y + 16f)
                    lineTo(handsCenter.x - 5f, handsCenter.y)
                    lineTo(handsCenter.x, handsCenter.y - handRadius * 0.76f)
                    lineTo(handsCenter.x + 5f, handsCenter.y)
                    close()
                }
                drawPath(pathMin, color = Color(0xFFE5C158))
                drawPath(pathMin, color = Color.White, style = Stroke(width = 1f))
            }
            // Extremely delicate gold sweeping wire second
            withTransform({ rotate(sec * 6f, pivot = handsCenter) }) {
                drawLine(Color(0xFFE5C158), handsCenter + Offset(0f, 22f), handsCenter - Offset(0f, handRadius * 0.88f), strokeWidth = 2f)
                drawCircle(Color(0xFFE5C158), radius = 5f, center = handsCenter)
            }
        }
        "vector-onyx" -> { // Armored Solid Red Glow Hands
            withTransform({ rotate(hr * 30f, pivot = handsCenter) }) {
                drawLine(Color(0xFF151619), handsCenter, handsCenter - Offset(0f, handRadius * 0.44f), strokeWidth = 12f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFF3366), handsCenter - Offset(0f, 6f), handsCenter - Offset(0f, handRadius * 0.4f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            withTransform({ rotate(min * 6f, pivot = handsCenter) }) {
                drawLine(Color(0xFF151619), handsCenter, handsCenter - Offset(0f, handRadius * 0.72f), strokeWidth = 9f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFF3366), handsCenter - Offset(0f, 6f), handsCenter - Offset(0f, handRadius * 0.68f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            withTransform({ rotate(sec * 6f, pivot = handsCenter) }) {
                drawLine(Color(0xFFFF3366), handsCenter + Offset(0f, 15f), handsCenter - Offset(0f, handRadius * 0.85f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawCircle(Color(0xFFFF3366), radius = 6f, center = handsCenter)
            }
        }
        else -> { // Precision Cyber Chronograph sport styling for Chronos
            withTransform({ rotate(hr * 30f, pivot = handsCenter) }) {
                drawLine(Color.White, handsCenter, handsCenter - Offset(0f, handRadius * 0.48f), strokeWidth = 8f, cap = StrokeCap.Square)
                drawLine(Color(0xFFFFCC00), handsCenter - Offset(0f, handRadius * 0.35f), handsCenter - Offset(0f, handRadius * 0.45f), strokeWidth = 4f)
            }
            withTransform({ rotate(min * 6f, pivot = handsCenter) }) {
                drawLine(Color.White, handsCenter, handsCenter - Offset(0f, handRadius * 0.75f), strokeWidth = 5f, cap = StrokeCap.Square)
                drawLine(Color(0xFFFFCC00), handsCenter - Offset(0f, handRadius * 0.55f), handsCenter - Offset(0f, handRadius * 0.72f), strokeWidth = 3f)
            }
            withTransform({ rotate(sec * 6f, pivot = handsCenter) }) {
                // Neon Orange split timing second hand
                drawLine(Color(0xFFFF6600), handsCenter, handsCenter - Offset(0f, handRadius * 0.9f), strokeWidth = 2.5f, cap = StrokeCap.Round)
                // Counterweight aeroplane silhouette tip
                drawCircle(Color(0xFFFF6600), radius = 6f, center = handsCenter)
                drawCircle(Color.White, radius = 2f, center = handsCenter)
            }
        }
    }
}

private fun DrawScope.drawImmersiveSapphireReflection(
    center: Offset,
    radius: Float,
    tiltX: Float,
    tiltY: Float,
    rotationX: Float
) {
    // Dome reflection layer on very top flat plane (Z = 0.40f)
    val reflectionOffset = Offset(tiltX * 0.4f, tiltY * 0.4f)
    val domeCenter = center + reflectionOffset
    val domeRadius = radius * 1.02f

    // Studio lighting glaze angle derived from gyroscope X-angle
    val glideAngle = (rotationX * 0.42f) - 40f
    
    // Triple-band light bounce simulation to look identical to real glass curves
    val glossBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent,
            Color.White.copy(alpha = 0.03f),
            Color.White.copy(alpha = 0.15f),
            Color.Transparent,
            Color.White.copy(alpha = 0.08f)
        ),
        start = domeCenter - Offset(domeRadius, domeRadius),
        end = domeCenter + Offset(domeRadius, domeRadius)
    )

    // Inner anti-reflective (AR) sapphire blueish/magenta glare sheen (premium look)
    val arBrush = Brush.radialGradient(
        colors = listOf(
            Color(0x002B0080),
            Color(0x0A2B0080),
            Color(0x22130040),
            Color(0x00000000)
        ),
        center = domeCenter,
        radius = domeRadius * 1.1f
    )
    drawCircle(brush = arBrush, radius = domeRadius, center = domeCenter)

    withTransform({
        rotate(glideAngle, pivot = domeCenter)
    }) {
        drawCircle(brush = glossBrush, radius = domeRadius, center = domeCenter)
    }

    // Outer spherical edge-highlight ring
    drawArc(
        color = Color.White.copy(alpha = 0.14f),
        startAngle = 190f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = domeCenter - Offset(domeRadius * 0.95f, domeRadius * 0.95f),
        size = Size(domeRadius * 1.9f, domeRadius * 1.9f),
        style = Stroke(width = radius * 0.03f)
    )
}

// ==================== GEOMETRIC HELPER EXTRANEOUS METHODS ====================

private fun DrawScope.drawMechanicalCogwheel3D(
    pos: Offset,
    innerRadius: Float,
    teethCount: Int,
    color: Color,
    spokeCount: Int
) {
    // 3D side casting logic (slight extrusion shadow representation of cogwheels)
    drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = innerRadius, center = pos + Offset(4f, 4f))
    drawCircle(color = color, radius = innerRadius, center = pos)
    drawCircle(color = Color(0xFF0F1015), radius = innerRadius * 0.35f, center = pos)

    // Extrude spokes
    for (i in 0 until spokeCount) {
        val angleDeg = i * (360f / spokeCount)
        withTransform({
            rotate(angleDeg, pivot = pos)
        }) {
            drawLine(
                color = color,
                start = pos + Offset(0f, innerRadius * 0.15f),
                end = pos + Offset(0f, innerRadius),
                strokeWidth = innerRadius * 0.2f
            )
        }
    }

    // 3D perimeter teeth blocks
    val toothWidthDegrees = 360f / teethCount
    val teethHeight = innerRadius * 0.16f
    for (i in 0 until teethCount) {
        val angle = i * toothWidthDegrees
        withTransform({
            rotate(angle, pivot = pos)
        }) {
            drawRect(
                color = color,
                topLeft = Offset(pos.x - (innerRadius * 0.1f), pos.y - innerRadius - teethHeight),
                size = Size(innerRadius * 0.2f, teethHeight * 1.4f)
            )
        }
    }
}

private fun DrawScope.drawRect3D(
    centerPos: Offset,
    size: Size,
    angleRad: Float,
    color: Color
) {
    // Draw rotated cardinal indicator bars beautifully
    withTransform({
        rotate(angleRad * 180f / PI.toFloat() + 90f, pivot = centerPos)
    }) {
        drawRoundRect(
            color = color,
            topLeft = centerPos - Offset(size.width / 2, size.height / 2),
            size = size,
            cornerRadius = CornerRadius(2f)
        )
        // High polish center cap
        drawRoundRect(
            color = Color.White,
            topLeft = centerPos - Offset(size.width / 3, size.height / 3),
            size = Size(size.width * 0.6f, size.height * 0.6f),
            cornerRadius = CornerRadius(1f)
        )
    }
}

private fun DrawScope.drawRightSideProfile(
    center: Offset,
    radius: Float,
    strapColor: Color,
    watch: WatchItem,
    techPulse: Float,
    pitchAngle: Float
) {
    val sizePx = radius * 2f
    val caseWidth = sizePx * 1.5f
    val caseHeight = radius * 0.45f
    
    // Draw background drop shadow for depth
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(center.x - caseWidth/2 - 10f, center.y - caseHeight/2 + 10f),
        size = Size(caseWidth + 20f, caseHeight + 15f),
        cornerRadius = CornerRadius(16f)
    )

    // 1. Draw Straps sweeping backwards
    val topStrap = Path().apply {
         moveTo(center.x - caseWidth * 0.4f, center.y - caseHeight * 0.2f)
         lineTo(center.x - caseWidth * 0.38f, center.y - radius * 2.2f)
         lineTo(center.x - caseWidth * 0.2f, center.y - radius * 2.2f)
         lineTo(center.x - caseWidth * 0.18f, center.y - caseHeight * 0.2f)
         close()
    }
    val bottomStrap = Path().apply {
         moveTo(center.x - caseWidth * 0.4f, center.y + caseHeight * 0.2f)
         lineTo(center.x - caseWidth * 0.38f, center.y + radius * 2.2f)
         lineTo(center.x - caseWidth * 0.2f, center.y + radius * 2.2f)
         lineTo(center.x - caseWidth * 0.18f, center.y + caseHeight * 0.2f)
         close()
    }
    
    val strapGradient = Brush.verticalGradient(
        colors = listOf(strapColor.copy(alpha = 0.85f), strapColor.copy(alpha = 0.3f))
    )
    drawPath(topStrap, brush = strapGradient)
    drawPath(bottomStrap, brush = strapGradient)

    // 2. Draw Watch Case Side Chassis Rectangle
    val caseColor = when(watch.id) {
        "vector-onyx" -> Color(0xFF1E1E24)
        "vector-quantum" -> Color(0xFF16161D)
        "vector-horizon" -> Color(0xFFE5C158)
        else -> Color(0xFF4C4D55) // Grade 5 Titanium for Chronos
    }
    
    val dynamicCaseBrush = if (watch.id == "vector-horizon") {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFDF7E), Color(0xFF9E7C33), Color(0xFFFFEFA5))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(caseColor.copy(alpha = 0.9f), caseColor.copy(alpha = 0.95f), Color(0xFF0F0F12))
        )
    }

    drawRoundRect(
        brush = dynamicCaseBrush,
        topLeft = Offset(center.x - caseWidth / 2, center.y - caseHeight / 2),
        size = Size(caseWidth, caseHeight),
        cornerRadius = CornerRadius(12f)
    )

    // 3. Winding Winder Crown (On the right profile)
    val crownWidth = caseWidth * 0.12f
    val crownHeight = caseHeight * 0.72f
    val crownX = center.x + caseWidth / 2 - 4f
    val crownY = center.y - crownHeight / 2

    val crownBrush = if (watch.id == "vector-horizon") {
        Brush.verticalGradient(colors = listOf(Color(0xFFFFF0A5), Color(0xFF9E7C33)))
    } else {
        Brush.verticalGradient(colors = listOf(Color(0xFFCECCD6), Color(0xFF5A5862)))
    }

    drawRoundRect(
        brush = crownBrush,
        topLeft = Offset(crownX, crownY),
        size = Size(crownWidth, crownHeight),
        cornerRadius = CornerRadius(4f)
    )

    // Crown ridges
    val ridgeColor = Color.Black.copy(alpha = 0.6f)
    for (r in 0 until 4) {
        val rY = crownY + (crownHeight / 5f) * (r + 1)
        drawLine(
            color = ridgeColor,
            start = Offset(crownX + 2f, rY),
            end = Offset(crownX + crownWidth - 2f, rY),
            strokeWidth = 3f
        )
    }

    // Crown core tip
    drawRect(
        color = watch.primaryColor,
        topLeft = Offset(crownX + crownWidth - 4f, crownY + crownHeight * 0.3f),
        size = Size(4f, crownHeight * 0.4f)
    )

    // 4. Chronograph Pusher Buttons
    if (watch.id == "vector-chronos" || watch.id == "vector-quantum" || watch.id == "vector-onyx") {
        val pusherWidth = caseWidth * 0.08f
        val pusherHeight = caseHeight * 0.45f
        
        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(crownX - 10f, center.y - caseHeight * 0.45f),
            size = Size(pusherWidth, pusherHeight),
            cornerRadius = CornerRadius(3f)
        )
        drawRect(
            color = watch.primaryColor,
            topLeft = Offset(crownX - 10f + pusherWidth - 4f, center.y - caseHeight * 0.35f),
            size = Size(4f, pusherHeight * 0.4f)
        )

        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(crownX - 10f, center.y + caseHeight * 0.15f),
            size = Size(pusherWidth, pusherHeight),
            cornerRadius = CornerRadius(3f)
        )
        drawRect(
            color = watch.primaryColor,
            topLeft = Offset(crownX - 10f + pusherWidth - 4f, center.y + caseHeight * 0.25f),
            size = Size(4f, pusherHeight * 0.4f)
        )
    }

    // 5. Curved Sapphire Glass Dome
    val glassPath = Path().apply {
        moveTo(center.x - caseWidth * 0.45f, center.y - caseHeight * 0.5f)
        quadraticTo(
            center.x, center.y - caseHeight * 0.85f,
            center.x + caseWidth * 0.45f, center.y - caseHeight * 0.5f
        )
        close()
    }
    
    val glassGlare = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
    )
    drawPath(glassPath, brush = glassGlare)
    drawPath(glassPath, color = Color.White.copy(alpha = 0.15f), style = Stroke(width = 2f))

    drawContextMetricsOverlay(this, center, radius, watch)
}

private fun DrawScope.drawLeftSideProfile(
    center: Offset,
    radius: Float,
    strapColor: Color,
    watch: WatchItem,
    techPulse: Float,
    pitchAngle: Float
) {
    val sizePx = radius * 2f
    val caseWidth = sizePx * 1.5f
    val caseHeight = radius * 0.45f
    
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = Offset(center.x - caseWidth/2 - 10f, center.y - caseHeight/2 + 10f),
        size = Size(caseWidth + 20f, caseHeight + 15f),
        cornerRadius = CornerRadius(16f)
    )

    // 1. Draw Straps sweeping backwards
    val topStrap = Path().apply {
         moveTo(center.x + caseWidth * 0.18f, center.y - caseHeight * 0.2f)
         lineTo(center.x + caseWidth * 0.2f, center.y - radius * 2.2f)
         lineTo(center.x + caseWidth * 0.38f, center.y - radius * 2.2f)
         lineTo(center.x + caseWidth * 0.4f, center.y - caseHeight * 0.2f)
         close()
    }
    val bottomStrap = Path().apply {
         moveTo(center.x + caseWidth * 0.18f, center.y + caseHeight * 0.2f)
         lineTo(center.x + caseWidth * 0.2f, center.y + radius * 2.2f)
         lineTo(center.x + caseWidth * 0.38f, center.y + radius * 2.2f)
         lineTo(center.x + caseWidth * 0.4f, center.y + caseHeight * 0.2f)
         close()
    }
    
    val strapGradient = Brush.verticalGradient(
        colors = listOf(strapColor.copy(alpha = 0.85f), strapColor.copy(alpha = 0.3f))
    )
    drawPath(topStrap, brush = strapGradient)
    drawPath(bottomStrap, brush = strapGradient)

    // 2. Main casing rectangle
    val caseColor = when(watch.id) {
        "vector-onyx" -> Color(0xFF1E1E24)
        "vector-quantum" -> Color(0xFF16161D)
        "vector-horizon" -> Color(0xFFE5C158)
        else -> Color(0xFF4C4D55)
    }
    
    val dynamicCaseBrush = if (watch.id == "vector-horizon") {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF7C6026), Color(0xFFE5C158), Color(0xFF4C3814))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(caseColor.copy(alpha = 0.9f), caseColor.copy(alpha = 0.85f), Color(0xFF0C0C0F))
        )
    }

    drawRoundRect(
        brush = dynamicCaseBrush,
        topLeft = Offset(center.x - caseWidth / 2, center.y - caseHeight / 2),
        size = Size(caseWidth, caseHeight),
        cornerRadius = CornerRadius(12f)
    )

    // 3. Venting & Acoustic speaker grill holes
    val dotColor = Color.Black.copy(alpha = 0.8f)
    val neonDotColor = watch.primaryColor.copy(alpha = 0.7f * techPulse)
    
    for (i in 0 until 3) {
        val dotX = center.x - 30f + (30f * i)
        val dotY = center.y
        drawCircle(color = dotColor, radius = 4f, center = Offset(dotX, dotY))
        drawCircle(color = neonDotColor, radius = 2f, center = Offset(dotX, dotY))
    }

    // 4. Curved Sapphire glass dome
    val glassPath = Path().apply {
        moveTo(center.x - caseWidth * 0.45f, center.y - caseHeight * 0.5f)
        quadraticTo(
            center.x, center.y - caseHeight * 0.85f,
            center.x + caseWidth * 0.45f, center.y - caseHeight * 0.5f
        )
        close()
    }
    val glassGlare = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
    )
    drawPath(glassPath, brush = glassGlare)
    drawPath(glassPath, color = Color.White.copy(alpha = 0.12f), style = Stroke(width = 2f))

    drawContextMetricsOverlay(this, center, radius, watch)
}

private fun DrawScope.drawImmersiveCaseback(
    center: Offset,
    radius: Float,
    strapColor: Color,
    watch: WatchItem,
    techPulse: Float,
    gearAngle: Float,
    rotationX: Float,
    rotationY: Float
) {
    val casebackRadius = radius * 1.1f
    
    drawCircle(
        color = Color.Black.copy(alpha = 0.65f),
        radius = casebackRadius + 12f,
        center = center + Offset(8f, 10f)
    )

    // 1. Titanium/Steel Outer Ring Casing Plate
    val edgeColor = when(watch.id) {
        "vector-onyx" -> Color(0xFF2E2E36)
        "vector-horizon" -> Color(0xFF7E602A)
        else -> Color(0xFF4A4B54)
    }
    
    val ringBrush = Brush.sweepGradient(
        colors = listOf(edgeColor, Color.White.copy(alpha = 0.15f), edgeColor, Color.Black.copy(alpha = 0.7f), edgeColor)
    )
    
    drawCircle(
        color = Color(0xFF1E1F25),
        radius = casebackRadius,
        center = center
    )
    drawCircle(
        brush = ringBrush,
        radius = casebackRadius,
        center = center,
        style = Stroke(width = radius * 0.16f)
    )

    // 2. 6 Holding Screws
    for (s in 0 until 6) {
        val sAngleDeg = s * 60f
        val sAngleRad = Math.toRadians(sAngleDeg.toDouble())
        val scX = center.x + cos(sAngleRad).toFloat() * (casebackRadius * 0.92f)
        val scY = center.y + sin(sAngleRad).toFloat() * (casebackRadius * 0.92f)
        
        drawCircle(color = Color(0xFF222222), radius = 6f, center = Offset(scX, scY))
        drawCircle(color = Color(0xFF999999), radius = 4f, center = Offset(scX, scY))
        withTransform({
            rotate(sAngleDeg + 45f, pivot = Offset(scX, scY))
        }) {
            drawLine(
                color = Color(0xFF222222),
                start = Offset(scX - 3f, scY),
                end = Offset(scX + 3f, scY),
                strokeWidth = 2f
            )
        }
    }

    // 3. Concentration dots representing laser texts
    for (i in 0 until 48) {
        val dotAngle = i * (360f / 48f)
        val dotRad = Math.toRadians(dotAngle.toDouble())
        val dtX = center.x + cos(dotRad).toFloat() * (casebackRadius * 0.85f)
        val dtY = center.y + sin(dotRad).toFloat() * (casebackRadius * 0.85f)
        
        if (i % 6 == 0) {
            drawCircle(Color(0xFFE5C158).copy(alpha = 0.7f), radius = 2.5f, center = Offset(dtX, dtY))
        } else {
            drawCircle(Color.White.copy(alpha = 0.15f), radius = 1.2f, center = Offset(dtX, dtY))
        }
    }

    // 4. Inner Sapphire Window Void
    val windowRadius = radius * 0.75f
    drawCircle(
        color = Color(0xFF0C0D11),
        radius = windowRadius,
        center = center
    )

    // 5. Exhibition plates
    val goldBridge = Path().apply {
        moveTo(center.x - windowRadius * 0.7f, center.y - windowRadius * 0.4f)
        lineTo(center.x + windowRadius * 0.5f, center.y - windowRadius * 0.7f)
        lineTo(center.x + windowRadius * 0.8f, center.y + windowRadius * 0.3f)
        lineTo(center.x - windowRadius * 0.2f, center.y + windowRadius * 0.8f)
        close()
    }
    drawPath(
        path = goldBridge,
        brush = Brush.radialGradient(colors = listOf(Color(0xFF8E6F34), Color(0xFF4C3A14)), center = center, radius = windowRadius)
    )

    val ruby1 = center + Offset(-windowRadius * 0.3f, -windowRadius * 0.2f)
    val ruby2 = center + Offset(windowRadius * 0.4f, windowRadius * 0.1f)
    drawCircle(Color(0xFFFF1A8C).copy(alpha = 0.4f), radius = 6f, center = ruby1)
    drawCircle(Color(0xFFE60060), radius = 4f, center = ruby1)
    drawCircle(Color(0xFFFF1A8C).copy(alpha = 0.4f), radius = 6f, center = ruby2)
    drawCircle(Color(0xFFE60060), radius = 4f, center = ruby2)

    // 6. Dynamic heavy weight oscillating rotor
    val rotorAngle = ((rotationX + rotationY) * 1.5f + (gearAngle * 0.25f)) % 360f
    
    withTransform({
        rotate(rotorAngle, pivot = center)
    }) {
        val rotorPath = Path().apply {
            addArc(
                oval = Rect(center.x - windowRadius, center.y - windowRadius, center.x + windowRadius, center.y + windowRadius),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f
            )
            lineTo(center.x, center.y)
            close()
        }
        
        val rotorBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFC4A045), Color(0xFF9C7D33), Color(0xFF4A3B14))
        )
        val plainRotorBrush = Brush.verticalGradient(
             colors = listOf(Color(0xFF7C828C), Color(0xFF41454D), Color(0xFF22242B))
        )
        
        drawPath(
            path = rotorPath,
            brush = if (watch.id == "vector-horizon") rotorBrush else plainRotorBrush
        )

        val rotorCutout1 = Path().apply {
            addArc(
                oval = Rect(center.x - windowRadius * 0.85f, center.y - windowRadius * 0.85f, center.x + windowRadius * 0.85f, center.y + windowRadius * 0.85f),
                startAngleDegrees = 20f,
                sweepAngleDegrees = 60f
            )
            lineTo(center.x, center.y)
            close()
        }
        drawPath(rotorCutout1, color = Color.Black.copy(alpha = 0.6f), style = Stroke(width = 6f))

        drawCircle(Color(0xFF222222), radius = windowRadius * 0.28f, center = center)
        drawCircle(Color(0xFF9A9CA6), radius = windowRadius * 0.22f, center = center)
        drawCircle(Color.White.copy(alpha = 0.6f), radius = 5f, center = center)
    }

    val glareBrush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent, Color.White.copy(alpha = 0.05f)),
        start = center - Offset(windowRadius, windowRadius),
        end = center + Offset(windowRadius, windowRadius)
    )
    drawCircle(brush = glareBrush, radius = windowRadius, center = center)

    drawContextMetricsOverlay(this, center, radius, watch)
}

private fun drawContextMetricsOverlay(
    scope: DrawScope,
    center: Offset,
    radius: Float,
    watch: WatchItem
) {
    val frameX = center.x - radius * 1.4f
    val frameY = center.y + radius * 0.9f
    scope.drawLine(
        color = watch.primaryColor.copy(alpha = 0.5f),
        start = Offset(frameX, frameY),
        end = Offset(frameX + 30f, frameY),
        strokeWidth = 2f
    )
    scope.drawLine(
        color = watch.primaryColor.copy(alpha = 0.5f),
        start = Offset(frameX, frameY),
        end = Offset(frameX, frameY - 30f),
        strokeWidth = 2f
    )
}
