package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.LocalUserDataRepository
import com.example.data.local.MuslimBanglaDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.OfflineContentRepository
import com.example.ui.screens.DuaDhikrScreen
import com.example.ui.screens.HadithBookListScreen
import com.example.ui.screens.HadithDetailScreen
import com.example.ui.screens.HadithHubScreen
import com.example.ui.screens.HijriCalendarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IslamicQuizScreen
import com.example.ui.screens.MatrimonialChatGateScreen
import com.example.ui.screens.MatrimonialGateScreen
import com.example.ui.screens.PrayerQiblaScreen
import com.example.ui.screens.QuranHubScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasbihScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.screens.ZakatCalculatorScreen
import com.example.ui.viewmodels.HadithViewModel
import com.example.ui.viewmodels.HomeSettingsViewModel
import com.example.ui.viewmodels.QuranViewModel
import com.example.ui.viewmodels.ToolsViewModel

private data class TopNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private object AppRoutes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val HADITH = "hadith"
    const val TOOLS = "tools"
    const val MATRIMONIAL = "matrimonial"
    const val QURAN_READER = "quran_reader/{surahId}"
    const val HADITH_BOOK = "hadith_book/{bookId}"
    const val HADITH_DETAIL = "hadith_detail/{hadithId}"
    const val TOOL_DETAIL = "tool/{toolRoute}"
    const val MATRIMONIAL_CHAT = "matrimonial_chat/{requestId}"
    const val SETTINGS = "settings"
}

@Composable
fun MuslimBanglaApp() {
    val appContext = LocalContext.current.applicationContext as Application
    val roomDb = MuslimBanglaDatabase.getInstance(appContext)
    val dao = roomDb.dao()
    val prefsRepo = UserPreferencesRepository(appContext)
    val offlineRepo = OfflineContentRepository(appContext, dao)
    val localUserRepo = LocalUserDataRepository(dao, prefsRepo)

    val homeSettingsVm: HomeSettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeSettingsViewModel(
                    prefsRepo = prefsRepo,
                    localUserRepo = localUserRepo,
                    dao = dao,
                    offlineRepo = offlineRepo
                )
            }
        }
    )
    val quranVm: QuranViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                QuranViewModel(
                    dao = dao,
                    offlineRepo = offlineRepo,
                    localUserRepo = localUserRepo,
                    prefsRepo = prefsRepo
                )
            }
        }
    )
    val hadithVm: HadithViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HadithViewModel(
                    dao = dao,
                    offlineRepo = offlineRepo,
                    localUserRepo = localUserRepo
                )
            }
        }
    )
    val toolsVm: ToolsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ToolsViewModel(
                    dao = dao,
                    prefsRepo = prefsRepo,
                    localUserRepo = localUserRepo
                )
            }
        }
    )
    MainAppScaffold(
        prefsRepo = prefsRepo,
        homeSettingsVm = homeSettingsVm,
        quranVm = quranVm,
        hadithVm = hadithVm,
        toolsVm = toolsVm
    )
}

@Composable
private fun MainAppScaffold(
    prefsRepo: UserPreferencesRepository,
    homeSettingsVm: HomeSettingsViewModel,
    quranVm: QuranViewModel,
    hadithVm: HadithViewModel,
    toolsVm: ToolsViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topTabs = listOf(
        TopNavItem(AppRoutes.HOME, "হোম", Icons.Rounded.Home),
        TopNavItem(AppRoutes.QURAN, "কুরআন", Icons.AutoMirrored.Rounded.MenuBook),
        TopNavItem(AppRoutes.HADITH, "হাদিস", Icons.Rounded.LibraryBooks),
        TopNavItem(AppRoutes.TOOLS, "আমল", Icons.Rounded.AutoAwesomeMosaic),
        TopNavItem(AppRoutes.MATRIMONIAL, "বিবাহ", Icons.Rounded.Favorite)
    )
    val showTopLevelChrome = currentRoute in topTabs.map { it.route }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelChrome && !useNavRail) {
                    NavigationBar {
                        topTabs.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = { navigateTopTab(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelChrome && useNavRail) {
                    NavigationRail {
                        topTabs.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.route,
                                onClick = { navigateTopTab(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("rail_${item.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.HOME
                    ) {
                        composable(AppRoutes.HOME) {
                            HomeScreen(
                                viewModel = homeSettingsVm,
                                onOpenQuranSurah = { surahId ->
                                    navController.navigate("quran_reader/$surahId")
                                },
                                onOpenHadithDetail = { hadithId ->
                                    navController.navigate("hadith_detail/$hadithId")
                                },
                                onOpenToolRoute = { toolRoute ->
                                    navController.navigate("tool/$toolRoute")
                                },
                                onOpenMatrimonial = {
                                    navigateTopTab(AppRoutes.MATRIMONIAL)
                                },
                                onOpenSettings = {
                                    navController.navigate(AppRoutes.SETTINGS)
                                }
                            )
                        }

                        composable(AppRoutes.QURAN) {
                            QuranHubScreen(
                                viewModel = quranVm,
                                onOpenSurah = { surahId ->
                                    navController.navigate("quran_reader/$surahId")
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.QURAN_READER,
                            arguments = listOf(navArgument("surahId") { type = NavType.IntType })
                        ) { entry ->
                            val surahId = entry.arguments?.getInt("surahId") ?: 1
                            QuranReaderScreen(
                                surahId = surahId,
                                viewModel = quranVm,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppRoutes.HADITH) {
                            HadithHubScreen(
                                viewModel = hadithVm,
                                onOpenBook = { bookId ->
                                    navController.navigate("hadith_book/$bookId")
                                },
                                onOpenHadith = { hadithId ->
                                    navController.navigate("hadith_detail/$hadithId")
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.HADITH_BOOK,
                            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
                        ) { entry ->
                            val bookId = entry.arguments?.getString("bookId") ?: "bukhari"
                            HadithBookListScreen(
                                bookId = bookId,
                                viewModel = hadithVm,
                                onBack = { navController.popBackStack() },
                                onOpenHadith = { hadithId ->
                                    navController.navigate("hadith_detail/$hadithId")
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.HADITH_DETAIL,
                            arguments = listOf(navArgument("hadithId") { type = NavType.StringType })
                        ) { entry ->
                            val hadithId = entry.arguments?.getString("hadithId") ?: "bukhari_1"
                            HadithDetailScreen(
                                hadithId = hadithId,
                                viewModel = hadithVm,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppRoutes.TOOLS) {
                            ToolsHubScreen(
                                onNavigateTool = { toolRoute ->
                                    navController.navigate("tool/$toolRoute")
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.TOOL_DETAIL,
                            arguments = listOf(navArgument("toolRoute") { type = NavType.StringType })
                        ) { entry ->
                            when (entry.arguments?.getString("toolRoute")) {
                                "prayer_qibla" -> PrayerQiblaScreen(toolsVm) { navController.popBackStack() }
                                "hijri" -> HijriCalendarScreen { navController.popBackStack() }
                                "dua" -> DuaDhikrScreen(toolsVm) { navController.popBackStack() }
                                "tasbih" -> TasbihScreen(toolsVm) { navController.popBackStack() }
                                "zakat" -> ZakatCalculatorScreen { navController.popBackStack() }
                                "quiz" -> IslamicQuizScreen(toolsVm) { navController.popBackStack() }
                                else -> PrayerQiblaScreen(toolsVm) { navController.popBackStack() }
                            }
                        }

                        composable(AppRoutes.MATRIMONIAL) {
                            MatrimonialGateScreen(
                                prefsRepo = prefsRepo,
                                onOpenChat = { requestId ->
                                    navController.navigate("matrimonial_chat/$requestId")
                                }
                            )
                        }

                        composable(
                            route = AppRoutes.MATRIMONIAL_CHAT,
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { entry ->
                            val requestId = entry.arguments?.getString("requestId") ?: ""
                            MatrimonialChatGateScreen(
                                requestId = requestId,
                                prefsRepo = prefsRepo,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppRoutes.SETTINGS) {
                            SettingsScreen(
                                viewModel = homeSettingsVm,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
