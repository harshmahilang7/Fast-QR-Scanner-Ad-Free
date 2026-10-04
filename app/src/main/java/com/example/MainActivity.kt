package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.ScanHistoryRepository
import com.example.ui.screens.CreateQrScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StitchLaserCyan
import com.example.ui.viewmodel.CreateQrViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ScannerViewModel

enum class StitchTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    SCAN("Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "tab_scan"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "tab_history"),
    CREATE("Create", Icons.Filled.AddBox, Icons.Outlined.AddBox, "tab_create")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { ScanHistoryRepository(database.scanItemDao()) }

    val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.provideFactory(repository))
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.provideFactory(repository))
    val createQrViewModel: CreateQrViewModel = viewModel(factory = CreateQrViewModel.provideFactory(repository))

    var currentTab by rememberSaveable { mutableStateOf(StitchTab.SCAN) }
    val historyItems by historyViewModel.historyItems.collectAsStateWithLifecycle()

    // Handle back button: if in secondary tabs, return to Scanner tab
    BackHandler(enabled = currentTab != StitchTab.SCAN) {
        currentTab = StitchTab.SCAN
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(32.dp)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.height(64.dp)
                ) {
                    StitchTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            modifier = Modifier.testTag(tab.tag),
                            icon = {
                                if (tab == StitchTab.HISTORY && historyItems.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = GoogleBlue,
                                                contentColor = androidx.compose.ui.graphics.Color.White
                                            ) {
                                                Text(
                                                    text = if (historyItems.size > 99) "99+" else "${historyItems.size}",
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Crossfade(
                targetState = currentTab,
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    StitchTab.SCAN -> {
                        ScannerScreen(
                            viewModel = scannerViewModel,
                            onNavigateToHistory = { currentTab = StitchTab.HISTORY }
                        )
                    }
                    StitchTab.HISTORY -> {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onNavigateToScan = { currentTab = StitchTab.SCAN }
                        )
                    }
                    StitchTab.CREATE -> {
                        CreateQrScreen(
                            viewModel = createQrViewModel
                        )
                    }
                }
            }
        }
    }
}
