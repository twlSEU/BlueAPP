package com.example.blue.core.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.blue.core.AppContainer
import com.example.blue.core.util.SleepDateRules
import com.example.blue.feature.accounting.AccountEditorScreen
import com.example.blue.feature.accounting.AccountingBrowseScreen
import com.example.blue.feature.accounting.AccountingDayScreen
import com.example.blue.feature.accounting.AccountingMonthScreen
import com.example.blue.feature.accounting.AccountingYearScreen
import com.example.blue.feature.accounting.AccountingYearSummaryScreen
import com.example.blue.feature.accounting.CategoryScreen
import com.example.blue.feature.backup.BackupAction
import com.example.blue.feature.backup.BackupActionHost
import com.example.blue.feature.common.FeatureHubScreen
import com.example.blue.feature.common.FeatureHubTab
import com.example.blue.feature.diary.DiaryBrowseScreen
import com.example.blue.feature.diary.DiaryEditorScreen
import com.example.blue.feature.diary.DiaryMonthScreen
import com.example.blue.feature.diary.DiaryYearScreen
import com.example.blue.feature.diary.DiaryYearSummaryScreen
import com.example.blue.feature.home.HomeScreen
import com.example.blue.feature.home.HomeActionDestination
import com.example.blue.feature.home.HomeMetrics
import com.example.blue.data.repository.HomeMetricsSnapshot
import com.example.blue.feature.sleep.SleepArchiveMonthScreen
import com.example.blue.feature.sleep.SleepArchiveYearScreen
import com.example.blue.feature.sleep.SleepEditorScreen
import com.example.blue.feature.sleep.SleepSummaryScreen
import com.example.blue.feature.sleep.SleepTimeEstimator
import com.example.blue.feature.time.LifeTraceScreen
import com.example.blue.feature.time.TimeEventDetailScreen
import com.example.blue.feature.time.TimeEventEditorScreen
import com.example.blue.feature.time.TimeEventListScreen
import com.example.blue.model.AccountType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.launch

private object Routes {
    const val DiaryQuick = "diary/quick"
    const val DiaryBrowse = "diary/browse"
    const val DiarySummary = "diary/summary"
    const val DiaryArchive = "diary/archive"
    const val AccountingBrowse = "accounting/browse"
    const val AccountingSummary = "accounting/summary"
    const val AccountingArchive = "accounting/archive"
    const val SleepSummary = "sleep/summary"
    const val SleepArchive = "sleep/archive"
    const val LifeTrace = "time/life-trace"
    const val TimeEvents = "time/events"
    const val TimeEventNew = "time/events/new"
}

@Composable
fun LifeRecordNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sleepEstimator = remember(context.applicationContext) {
        SleepTimeEstimator(context.applicationContext)
    }
    val messageHostState = remember { SnackbarHostState() }
    var messageIsError by remember { mutableStateOf(false) }
    val messageScope = rememberCoroutineScope()
    val showGlobalMessage: (String, Boolean) -> Unit = { message, isError ->
        messageIsError = isError
        messageScope.launch {
            messageHostState.currentSnackbarData?.dismiss()
            messageHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.fillMaxSize(),
            // Compose animation clocks honor the system animator duration scale, including 0x.
            enterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(220)) { width -> width / 12 }
            },
            exitTransition = {
                fadeOut(tween(180)) + slideOutHorizontally(tween(200)) { width -> -width / 16 }
            },
            popEnterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(220)) { width -> -width / 12 }
            },
            popExitTransition = {
                fadeOut(tween(180)) + slideOutHorizontally(tween(200)) { width -> width / 16 }
            },
        ) {
            composable(AppDestination.Home.route) {
                val homeMonth = remember { YearMonth.now() }
                var requestedBackupAction by remember { mutableStateOf<BackupAction?>(null) }
                val backupManagerProvider = remember(container) { { container.backupManager } }
                val metricsSnapshot by remember(container.homeRepository, homeMonth) {
                    container.homeRepository.observeMetrics(homeMonth)
                }.collectAsStateWithLifecycle(initialValue = HomeMetricsSnapshot())
                val metrics = remember(metricsSnapshot) {
                    HomeMetrics(
                        diaryMonthCount = metricsSnapshot.diaryMonthCount,
                        accountingMonthCount = metricsSnapshot.accountingMonthCount,
                        sleepMonthCount = metricsSnapshot.sleepMonthCount,
                        timeEventCount = metricsSnapshot.timeEventCount,
                    )
                }
                BackupActionHost(
                    managerProvider = backupManagerProvider,
                    requestedAction = requestedBackupAction,
                    onActionConsumed = { requestedBackupAction = null },
                    onMessage = showGlobalMessage,
                )
                HomeScreen(
                    metrics = metrics,
                    onActionClick = { destination ->
                        when (destination) {
                            HomeActionDestination.DIARY_QUICK_ADD ->
                                navController.navigateFromClick(Routes.DiaryQuick)
                            HomeActionDestination.ACCOUNTING_QUICK_ENTRY -> {
                                val today = LocalDate.now()
                                navController.navigateFromClick(
                                    "accounting/editor/new/${today.year}/${today.monthValue}/${today.dayOfMonth}",
                                )
                            }
                            HomeActionDestination.SLEEP_QUICK_RECORD ->
                                navController.navigateFromClick(
                                    SleepDateRules.defaultQuickRecordDate(LocalDateTime.now()).sleepEditorRoute(),
                                )
                            HomeActionDestination.TIME_LIFE_TRACE ->
                                navController.navigateFromClick(Routes.LifeTrace)
                            HomeActionDestination.TIME_EVENTS ->
                                navController.navigateFromClick(Routes.TimeEvents)
                            HomeActionDestination.BACKUP_EXPORT ->
                                requestedBackupAction = BackupAction.EXPORT
                            HomeActionDestination.BACKUP_RESTORE ->
                                requestedBackupAction = BackupAction.RESTORE
                        }
                    },
                    onFeatureClick = { destination ->
                        navController.navigateFromClick(destination.route)
                    },
                )
            }

            composable(AppDestination.Diary.route) {
                DiaryHubScreen(container = container, navController = navController)
            }
            composable(AppDestination.Accounting.route) {
                AccountingHubScreen(container = container, navController = navController)
            }
            composable(AppDestination.Sleep.route) {
                SleepHubScreen(container = container, navController = navController)
            }
            composable(AppDestination.Time.route) {
                TimeHubScreen(container = container, navController = navController)
            }

            composable(Routes.DiaryQuick) {
                QuickDiaryResolver(
                    container = container,
                    navController = navController,
                    onError = { showGlobalMessage(it, true) },
                )
            }
            composable(Routes.DiaryBrowse) {
                DiaryBrowseScreen(
                    repository = container.diaryRepository,
                    imageStorage = container.diaryImageStorage,
                    onOpenDiary = { id -> navController.navigateFromClick("diary/editor/$id") },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.DiarySummary) {
                DiaryYearSummaryScreen(
                    repository = container.diaryRepository,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.DiaryArchive) {
                DiaryYearScreen(
                    repository = container.diaryRepository,
                    imageStorage = container.diaryImageStorage,
                    onOpenMonth = { year, month -> navController.navigateFromClick("diary/month/$year/$month") },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("diary/month/{year}/{month}") { entry ->
                val year = entry.arguments?.getString("year")?.toIntOrNull() ?: return@composable
                val month = entry.arguments?.getString("month")?.toIntOrNull() ?: return@composable
                val yearMonth = runCatching { YearMonth.of(year, month) }.getOrNull() ?: return@composable
                val defaultDate = when {
                    yearMonth == YearMonth.now() -> LocalDate.now()
                    yearMonth < YearMonth.now() -> yearMonth.atEndOfMonth()
                    else -> yearMonth.atDay(1)
                }
                DiaryMonthScreen(
                    repository = container.diaryRepository,
                    year = year,
                    month = month,
                    onOpenDiary = { id -> navController.navigateFromClick("diary/editor/$id") },
                    onCreateDiary = {
                        navController.navigateFromClick(
                            "diary/editor/new/${defaultDate.year}/${defaultDate.monthValue}/${defaultDate.dayOfMonth}",
                        )
                    },
                    onCreateDiaryForDate = { date ->
                        navController.navigateFromClick(
                            "diary/editor/new/${date.year}/${date.monthValue}/${date.dayOfMonth}",
                        )
                    },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("diary/editor/new/{year}/{month}") { entry ->
                val year = entry.arguments?.getString("year")?.toIntOrNull() ?: return@composable
                val month = entry.arguments?.getString("month")?.toIntOrNull() ?: return@composable
                DiaryEditorScreen(
                    repository = container.diaryRepository,
                    imageStorage = container.diaryImageStorage,
                    diaryId = null,
                    initialYearMonth = runCatching { YearMonth.of(year, month) }.getOrNull(),
                    onShowMessage = showGlobalMessage,
                    onSaved = navController::navigateUpFromClick,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("diary/editor/new/{year}/{month}/{day}") { entry ->
                val date = entry.localDateArguments() ?: return@composable
                DiaryEditorScreen(
                    repository = container.diaryRepository,
                    imageStorage = container.diaryImageStorage,
                    diaryId = null,
                    initialDate = date,
                    initialYearMonth = YearMonth.from(date),
                    onShowMessage = showGlobalMessage,
                    onSaved = navController::navigateUpFromClick,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("diary/editor/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.takeUnless { it == "new" }
                DiaryEditorScreen(
                    repository = container.diaryRepository,
                    imageStorage = container.diaryImageStorage,
                    diaryId = id,
                    onShowMessage = showGlobalMessage,
                    onSaved = navController::navigateUpFromClick,
                    onBack = navController::navigateUpFromClick,
                )
            }

            composable(Routes.AccountingBrowse) {
                AccountingBrowseScreen(
                    repository = container.accountRepository,
                    onOpenEntry = { id -> navController.navigateFromClick("accounting/editor/$id") },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.AccountingSummary) {
                AccountingYearSummaryScreen(
                    repository = container.accountRepository,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.AccountingArchive) {
                AccountingYearScreen(
                    repository = container.accountRepository,
                    onOpenMonth = { year, month ->
                        navController.navigateFromClick("accounting/month/$year/$month")
                    },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("accounting/month/{year}/{month}") { entry ->
                val year = entry.arguments?.getString("year")?.toIntOrNull() ?: return@composable
                val month = entry.arguments?.getString("month")?.toIntOrNull() ?: return@composable
                AccountingMonthScreen(
                    repository = container.accountRepository,
                    year = year,
                    month = month,
                    onOpenDay = { day -> navController.navigateFromClick("accounting/day/$year/$month/$day") },
                    onCreateEntry = {
                        navController.navigateFromClick("accounting/editor/new/$year/$month/1")
                    },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("accounting/day/{year}/{month}/{day}") { entry ->
                val date = entry.localDateArguments() ?: return@composable
                AccountingDayScreen(
                    repository = container.accountRepository,
                    year = date.year,
                    month = date.monthValue,
                    day = date.dayOfMonth,
                    onEdit = { id ->
                        navController.navigateFromClick(
                            id?.let { "accounting/editor/$it" }
                                ?: "accounting/editor/new/${date.year}/${date.monthValue}/${date.dayOfMonth}",
                        )
                    },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("accounting/editor/new/{year}/{month}/{day}") { entry ->
                val initialDate = entry.localDateArguments() ?: return@composable
                AccountEditorScreen(
                    repository = container.accountRepository,
                    entryId = null,
                    initialDate = initialDate,
                    onManageCategories = { type ->
                        navController.navigateFromClick("accounting/categories/${type.name}")
                    },
                    onShowMessage = showGlobalMessage,
                    onSaved = navController::navigateUpFromClick,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("accounting/editor/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.takeUnless { it == "new" }
                AccountEditorScreen(
                    repository = container.accountRepository,
                    entryId = id,
                    onManageCategories = { type ->
                        navController.navigateFromClick("accounting/categories/${type.name}")
                    },
                    onShowMessage = showGlobalMessage,
                    onSaved = navController::navigateUpFromClick,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("accounting/categories/{type}") { entry ->
                val type = entry.arguments?.getString("type")
                    ?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }
                    ?: return@composable
                CategoryScreen(container.accountRepository, type, navController::navigateUpFromClick)
            }

            composable("sleep/editor/{year}/{month}/{day}") { entry ->
                val recordDate = entry.localDateArguments() ?: return@composable
                SleepEditorScreen(
                    repository = container.sleepRepository,
                    estimator = sleepEstimator,
                    initialDate = recordDate,
                    onOpenUsageAccessSettings = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    },
                    onShowMessage = showGlobalMessage,
                    onSaved = navController::navigateUpFromClick,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.SleepSummary) {
                SleepSummaryScreen(
                    repository = container.sleepRepository,
                    onOpenDay = { date -> navController.navigateFromClick(date.sleepEditorRoute()) },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.SleepArchive) {
                SleepArchiveYearScreen(
                    repository = container.sleepRepository,
                    onOpenMonth = { year, month ->
                        navController.navigateFromClick("sleep/archive/month/$year/$month")
                    },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("sleep/archive/month/{year}/{month}") { entry ->
                val year = entry.arguments?.getString("year")?.toIntOrNull() ?: return@composable
                val month = entry.arguments?.getString("month")?.toIntOrNull() ?: return@composable
                SleepArchiveMonthScreen(
                    repository = container.sleepRepository,
                    year = year,
                    month = month,
                    onOpenDay = { date -> navController.navigateFromClick(date.sleepEditorRoute()) },
                    onBack = navController::navigateUpFromClick,
                )
            }

            composable(Routes.LifeTrace) {
                LifeTraceScreen(
                    repository = container.timeRepository,
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.TimeEvents) {
                TimeEventListScreen(
                    repository = container.timeRepository,
                    imageStorage = container.timeImageStorage,
                    onOpenEvent = { id -> navController.navigateFromClick("time/events/detail/$id") },
                    onCreateEvent = { navController.navigateFromClick(Routes.TimeEventNew) },
                    onEditEvent = { id -> navController.navigateFromClick("time/events/edit/$id") },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable(Routes.TimeEventNew) {
                TimeEventEditorScreen(
                    repository = container.timeRepository,
                    imageStorage = container.timeImageStorage,
                    eventId = null,
                    onSaved = { id ->
                        navController.navigateFromClick("time/events/detail/$id") {
                            popUpTo(Routes.TimeEventNew) { inclusive = true }
                        }
                    },
                    onBack = navController::navigateUpFromClick,
                    onShowMessage = showGlobalMessage,
                )
            }
            composable("time/events/detail/{id}") { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                TimeEventDetailScreen(
                    repository = container.timeRepository,
                    imageStorage = container.timeImageStorage,
                    eventId = id,
                    onEdit = { navController.navigateFromClick("time/events/edit/$id") },
                    onBack = navController::navigateUpFromClick,
                )
            }
            composable("time/events/edit/{id}") { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                TimeEventEditorScreen(
                    repository = container.timeRepository,
                    imageStorage = container.timeImageStorage,
                    eventId = id,
                    onSaved = { navController.navigateUpFromClick() },
                    onBack = navController::navigateUpFromClick,
                    onShowMessage = showGlobalMessage,
                )
            }

        }

        SnackbarHost(
            hostState = messageHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 10.dp, end = 20.dp),
            snackbar = { data ->
                GlobalMessageCard(message = data.visuals.message, isError = messageIsError)
            },
        )
    }
}

private val diaryHubTabs = listOf(
    FeatureHubTab(key = "archive", label = "年月"),
    FeatureHubTab(key = "browse", label = "浏览"),
    FeatureHubTab(key = "summary", label = "总结"),
)

private val accountingHubTabs = listOf(
    FeatureHubTab(key = "archive", label = "年月"),
    FeatureHubTab(key = "browse", label = "浏览"),
    FeatureHubTab(key = "summary", label = "总结"),
)

private val sleepHubTabs = listOf(
    FeatureHubTab(key = "archive", label = "年月"),
    FeatureHubTab(key = "summary", label = "总结"),
)

private val timeHubTabs = listOf(
    FeatureHubTab(key = "life-trace", label = "岁痕"),
    FeatureHubTab(key = "events", label = "去来"),
)
@Composable
private fun DiaryHubScreen(container: AppContainer, navController: NavHostController) {
    FeatureHubScreen(
        tabs = diaryHubTabs,
        accentColor = Color(0xFF4F83A9),
    ) { page ->
        when (page) {
            0 -> DiaryYearScreen(
                repository = container.diaryRepository,
                imageStorage = container.diaryImageStorage,
                onOpenMonth = { year, month -> navController.navigateFromClick("diary/month/$year/$month") },
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
            1 -> DiaryBrowseScreen(
                repository = container.diaryRepository,
                imageStorage = container.diaryImageStorage,
                onOpenDiary = { id -> navController.navigateFromClick("diary/editor/$id") },
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
            else -> DiaryYearSummaryScreen(
                repository = container.diaryRepository,
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
        }
    }
}

@Composable
private fun AccountingHubScreen(container: AppContainer, navController: NavHostController) {
    FeatureHubScreen(
        tabs = accountingHubTabs,
        accentColor = Color(0xFFA86F40),
    ) { page ->
        when (page) {
            0 -> AccountingYearScreen(
                repository = container.accountRepository,
                onOpenMonth = { year, month ->
                    navController.navigateFromClick("accounting/month/$year/$month")
                },
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
            1 -> AccountingBrowseScreen(
                repository = container.accountRepository,
                onOpenEntry = { id -> navController.navigateFromClick("accounting/editor/$id") },
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
            else -> AccountingYearSummaryScreen(
                repository = container.accountRepository,
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
        }
    }
}

@Composable
private fun SleepHubScreen(container: AppContainer, navController: NavHostController) {
    FeatureHubScreen(
        tabs = sleepHubTabs,
        accentColor = Color(0xFF706AAA),
    ) { page ->
        when (page) {
            0 -> SleepArchiveYearScreen(
                repository = container.sleepRepository,
                onOpenMonth = { year, month ->
                    navController.navigateFromClick("sleep/archive/month/$year/$month")
                },
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
            else -> SleepSummaryScreen(
                repository = container.sleepRepository,
                onOpenDay = { date -> navController.navigateFromClick(date.sleepEditorRoute()) },
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
        }
    }
}

@Composable
private fun TimeHubScreen(container: AppContainer, navController: NavHostController) {
    FeatureHubScreen(
        tabs = timeHubTabs,
        accentColor = Color(0xFF587A82),
    ) { page ->
        when (page) {
            0 -> LifeTraceScreen(
                repository = container.timeRepository,
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
            else -> TimeEventListScreen(
                repository = container.timeRepository,
                imageStorage = container.timeImageStorage,
                onOpenEvent = { id -> navController.navigateFromClick("time/events/detail/$id") },
                onCreateEvent = { navController.navigateFromClick(Routes.TimeEventNew) },
                onEditEvent = { id -> navController.navigateFromClick("time/events/edit/$id") },
                onBack = navController::navigateUpFromClick,
                showTopBar = false,
            )
        }
    }
}

@Composable
private fun QuickDiaryResolver(
    container: AppContainer,
    navController: NavHostController,
    onError: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        runCatching { container.diaryRepository.findLatestDiaryOnDate(today) }
            .onSuccess { existing ->
                val target = existing?.diary?.id?.let { "diary/editor/$it" }
                    ?: "diary/editor/new/${today.year}/${today.monthValue}/${today.dayOfMonth}"
                navController.navigate(target) {
                    popUpTo(Routes.DiaryQuick) { inclusive = true }
                    launchSingleTop = true
                }
            }
            .onFailure { error ->
                onError(error.message ?: "无法打开今天的日记")
                navController.navigateUp()
            }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun androidx.navigation.NavBackStackEntry.localDateArguments(): LocalDate? {
    val year = arguments?.getString("year")?.toIntOrNull() ?: return null
    val month = arguments?.getString("month")?.toIntOrNull() ?: return null
    val day = arguments?.getString("day")?.toIntOrNull() ?: return null
    return runCatching { LocalDate.of(year, month, day) }.getOrNull()
}

/**
 * Accepts navigation only while the visible destination is fully resumed. Navigation
 * moves it out of RESUMED synchronously, so a second tap during the transition is ignored.
 */
private fun NavHostController.navigateFromClick(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) return
    navigate(route) {
        launchSingleTop = true
        builder()
    }
}

private fun NavHostController.navigateUpFromClick(): Boolean {
    if (currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) return false
    return navigateUp()
}

private fun LocalDate.sleepEditorRoute(): String =
    "sleep/editor/$year/$monthValue/$dayOfMonth"

@Composable
private fun GlobalMessageCard(message: String, isError: Boolean) {
    val accent = if (isError) Color(0xFFD95C5C) else Color(0xFF3F8D78)
    val background = if (isError) Color(0xFFFFF3F2) else Color(0xFFF0F8F5)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = background,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isError) "!" else "✓",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (isError) "操作未完成" else "保存成功",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D4555),
                )
                Text(message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF647A88))
            }
        }
    }
}
