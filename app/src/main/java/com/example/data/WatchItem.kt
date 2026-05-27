package com.example.data

import androidx.compose.ui.graphics.Color

enum class DialStyle {
    ANALOG, CHRONOGRAPH, TECH_MATRIX, TOURBILLON
}

data class WatchItem(
    val id: String,
    val name: String,
    val series: String,
    val price: Double,
    val rating: Double,
    val description: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val strapMaterial: String,
    val caseMaterial: String,
    val dialStyle: DialStyle,
    val waterResistance: String = "100m (10 ATM)",
    val diameter: Int = 42,
    val gearCount: Int = 24,
    val weightGrams: Int = 78
) {
    val primaryColor: Color get() = Color(primaryColorHex)
    val accentColor: Color get() = Color(accentColorHex)
}

object WatchCatalog {
    val watches = listOf(
        WatchItem(
            id = "vector-quantum",
            name = "Vector Quantum",
            series = "Quantum Edition",
            price = 4299.00,
            rating = 4.9,
            description = "Elegance meets hyper-performance. Featuring an ultra-black carbon composite case and a real-time reactive solar charged kinetic matrix dial. Designed with stealth-brushed titanium detailing.",
            primaryColorHex = 0xFF00FFB2, // High-glow neon mint
            accentColorHex = 0xFF121217,
            strapMaterial = "Carbon Fiber Mesh Link",
            caseMaterial = "Aerospace Carbon Composite",
            dialStyle = DialStyle.TECH_MATRIX,
            gearCount = 38
        ),
        WatchItem(
            id = "vector-horizon",
            name = "Vector Horizon",
            series = "Horizon Tourbillon",
            price = 6899.00,
            rating = 5.0,
            description = "The ultimate synthesis of classical watchmaking and modern tech aesthetics. Features a fully-exposed rotating Tourbillon engine under a sapphire edge-to-edge dome and steel mesh link.",
            primaryColorHex = 0xFFFFD700, // Rich gold
            accentColorHex = 0xFF00E5FF, // Cyan kinetic parts
            strapMaterial = "Pure Titanium Mesh Link",
            caseMaterial = "Polished Stealth-Alloy",
            dialStyle = DialStyle.TOURBILLON,
            gearCount = 56
        ),
        WatchItem(
            id = "vector-onyx",
            name = "Vector Onyx",
            series = "Stealth Obsidian",
            price = 3799.00,
            rating = 4.8,
            description = "Uncompromisingly dark. Carved from a single block of solid obsidian space alloy, styled with red-glowing sub-elements and high-haptic dials. Built for maximum environmental resistance.",
            primaryColorHex = 0xFFFF3366, // Obsidian crimson glow
            accentColorHex = 0xFF1E1E24,
            strapMaterial = "Obsidian Treated Silicon",
            caseMaterial = "Solid Matte Obsidian Alloy",
            dialStyle = DialStyle.ANALOG,
            gearCount = 20
        ),
        WatchItem(
            id = "vector-chronos",
            name = "Vector Chronos",
            series = "Cyber Chronograph",
            price = 5400.00,
            rating = 4.9,
            description = "A split-second dual motor chronograph featuring active digital sub-dials. Accompanied by solid sandblasted titanium bevels and fully-articulated precision links.",
            primaryColorHex = 0xFF00FF88, // Electric Lime Green
            accentColorHex = 0xFFFFD700, // Gold details
            strapMaterial = "Sandblasted Gold-Titanium Mesh",
            caseMaterial = "Sandblasted Grade 5 Titanium",
            dialStyle = DialStyle.CHRONOGRAPH,
            gearCount = 42
        )
    )
}
