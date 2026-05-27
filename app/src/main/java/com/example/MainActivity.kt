package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.WatchDatabase
import com.example.data.WatchRepository
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    // Lazy initialization of Room database and Repository
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            WatchDatabase::class.java,
            "vector_watches_vault.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val repository by lazy {
        WatchRepository(database.watchDao())
    }

    private val viewModel by lazy {
        ViewModelProvider(this, WatchViewModelFactory(repository))[WatchViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic global uncaught exception handler to prevent silent thread crashes and output details clearly
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MAIN_CRASH", "CRITICAL: Uncaught exception on thread - " + thread.name, throwable)
            throwable.printStackTrace()
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ObsidianBlack,
                    bottomBar = {
                        // Dynamic floating nav capsule: hides in active AR try-on or login check for maximum full-bleed viewing
                        if (currentScreen != "try-on" && currentScreen != "login") {
                            FloatingNavigationCapsule(
                                currentScreen = currentScreen,
                                onNavigate = { viewModel.setScreen(it) }
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets.navigationBars
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (currentScreen != "try-on" && currentScreen != "login") 80.dp else 0.dp) // Cushion space for floating bottom bar
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_trans"
                        ) { screen ->
                            when (screen) {
                                "login" -> {
                                    LoginScreen(
                                        viewModel = viewModel,
                                        onLoginSuccess = { viewModel.setScreen("hero") },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "hero" -> {
                                    HeroScreen(
                                        viewModel = viewModel,
                                        onExploreCatalog = { viewModel.setScreen("catalog") },
                                        onStartAr = { viewModel.setScreen("try-on") },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "catalog" -> {
                                    WatchCatalogScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "details" -> {
                                    WatchDetailScreen(
                                        viewModel = viewModel,
                                        onBackToCatalog = { viewModel.setScreen("catalog") },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "try-on" -> {
                                    ArTryOnScreen(
                                        viewModel = viewModel,
                                        onBackToCatalog = { viewModel.setScreen("details") },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "profile" -> {
                                    ProfileFavoritesScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.setScreen("hero") },
                                        modifier = Modifier.fillMaxSize()
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
fun FloatingNavigationCapsule(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic glassmorphism background capsule border
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black.copy(alpha = 0.82f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("floating_nav_bar"),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Screen 1: Home/Hero portal
            val isHeroActive = currentScreen == "hero"
            NavigationCapsuleItem(
                label = "HOME",
                icon = Icons.Default.Home,
                isActive = isHeroActive,
                onClick = { onNavigate("hero") },
                modifier = Modifier.testTag("nav_hero_btn")
            )

            // Screen 2: Store/Catalog
            val isCatalogActive = currentScreen == "catalog" || currentScreen == "details"
            NavigationCapsuleItem(
                label = "STORE",
                icon = Icons.Default.Storefront,
                isActive = isCatalogActive,
                onClick = { onNavigate("catalog") },
                modifier = Modifier.testTag("nav_store_btn")
            )

            // Screen 3: AR try-on portal
            val isTryOnActive = currentScreen == "try-on"
            NavigationCapsuleItem(
                label = "AR TRY",
                icon = Icons.Default.CameraAlt,
                isActive = isTryOnActive,
                onClick = { onNavigate("try-on") },
                modifier = Modifier.testTag("nav_artry_btn")
            )

            // Screen 4: Settings profile
            val isProfileActive = currentScreen == "profile"
            NavigationCapsuleItem(
                label = "VAULT",
                icon = Icons.Default.Person,
                isActive = isProfileActive,
                onClick = { onNavigate("profile") },
                modifier = Modifier.testTag("nav_profile_btn")
            )
        }
    }
}

@Composable
fun NavigationCapsuleItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) ElectricCyan else SteelGray,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            color = if (isActive) ElectricCyan else SteelGray
        )
    }
}
