package pe.edu.unjbg.studylock.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pe.edu.unjbg.studylock.core.designsystem.SlBottomBar
import pe.edu.unjbg.studylock.core.designsystem.SlColors
import pe.edu.unjbg.studylock.di.AppGraph
import pe.edu.unjbg.studylock.feature.access.AccessActiveScreen
import pe.edu.unjbg.studylock.feature.courses.CourseDetailScreen
import pe.edu.unjbg.studylock.feature.courses.CourseDetailViewModel
import pe.edu.unjbg.studylock.feature.courses.CoursesScreen
import pe.edu.unjbg.studylock.feature.courses.CoursesViewModel
import pe.edu.unjbg.studylock.feature.home.HomeScreen
import pe.edu.unjbg.studylock.feature.home.HomeViewModel
import pe.edu.unjbg.studylock.feature.import.ImportScreen
import pe.edu.unjbg.studylock.feature.import.ImportViewModel
import pe.edu.unjbg.studylock.feature.onboarding.AppSelectionScreen
import pe.edu.unjbg.studylock.feature.onboarding.ConnectAccountScreen
import pe.edu.unjbg.studylock.feature.onboarding.OnboardingViewModel
import pe.edu.unjbg.studylock.feature.onboarding.PermissionsScreen
import pe.edu.unjbg.studylock.feature.onboarding.WelcomeScreen
import pe.edu.unjbg.studylock.feature.progress.ProgressScreen
import pe.edu.unjbg.studylock.feature.progress.ProgressViewModel
import pe.edu.unjbg.studylock.feature.schedule.ScheduleScreen
import pe.edu.unjbg.studylock.feature.schedule.ScheduleViewModel
import pe.edu.unjbg.studylock.feature.session.SessionScreen
import pe.edu.unjbg.studylock.feature.session.StudyViewModel
import pe.edu.unjbg.studylock.feature.settings.SettingsScreen
import pe.edu.unjbg.studylock.feature.settings.SettingsViewModel
import pe.edu.unjbg.studylock.ui.viewModelFactory

@Composable
fun StudyLockRoot(graph: AppGraph, startOnboarding: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in Routes.tabRoutes

    fun navigateTab(target: String) {
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun goHome() {
        nav.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = SlColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                SlBottomBar(
                    currentRoute = currentRoute,
                    onSelect = ::navigateTab,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (startOnboarding) Routes.WELCOME else Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onStart = { nav.navigate(Routes.ACCOUNT) },
                    onConnect = { nav.navigate(Routes.ACCOUNT) },
                )
            }

            composable(Routes.ACCOUNT) {
                val vm: OnboardingViewModel = viewModel(
                    factory = viewModelFactory { OnboardingViewModel(graph.prefs, graph.blockedApps) },
                )
                ConnectAccountScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.PERMISSIONS) },
                )
            }

            composable(Routes.PERMISSIONS) {
                PermissionsScreen(
                    onBack = { nav.popBackStack() },
                    onContinue = { nav.navigate(Routes.APPS) },
                )
            }

            composable(Routes.APPS) {
                val vm: OnboardingViewModel = viewModel(
                    factory = viewModelFactory { OnboardingViewModel(graph.prefs, graph.blockedApps) },
                )
                AppSelectionScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() },
                    onDone = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(
                    factory = viewModelFactory { HomeViewModel(graph.stats, graph.prefs) },
                )
                val ui by vm.ui.collectAsStateWithLifecycle()
                HomeScreen(
                    ui = ui,
                    onStartReview = { courseId -> nav.navigate(Routes.session(courseId)) },
                    onOpenAccess = { nav.navigate(Routes.ACCESS_ACTIVE) },
                    onBrowseCourses = { navigateTab(Routes.COURSES) },
                )
            }

            composable(Routes.COURSES) {
                val vm: CoursesViewModel = viewModel(
                    factory = viewModelFactory { CoursesViewModel(graph.courses) },
                )
                val ui by vm.ui.collectAsStateWithLifecycle()
                CoursesScreen(
                    ui = ui,
                    onOpenCourse = { courseId -> nav.navigate(Routes.courseDetail(courseId)) },
                    onQuickReview = { nav.navigate(Routes.session(Routes.WEAK)) },
                    onImport = { nav.navigate(Routes.IMPORT) },
                )
            }

            composable(Routes.IMPORT) {
                val context = LocalContext.current.applicationContext
                val vm: ImportViewModel = viewModel(
                    factory = viewModelFactory { ImportViewModel(context, graph.content) },
                )
                ImportScreen(viewModel = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.SCHEDULE) {
                val vm: ScheduleViewModel = viewModel(
                    factory = viewModelFactory { ScheduleViewModel(graph.schedule, graph.courses) },
                )
                ScheduleScreen(viewModel = vm, onBack = { nav.popBackStack() })
            }

            composable(Routes.COURSE_DETAIL) {
                val courseId = it.arguments?.getString("courseId").orEmpty()
                val vm: CourseDetailViewModel = viewModel(
                    factory = viewModelFactory { CourseDetailViewModel(graph.courses, courseId) },
                )
                val course by vm.ui.collectAsStateWithLifecycle()
                CourseDetailScreen(
                    course = course,
                    onBack = { nav.popBackStack() },
                    onStartReview = { id -> nav.navigate(Routes.session(id)) },
                )
            }

            composable(Routes.SESSION) {
                val courseId = it.arguments?.getString("courseId").orEmpty()
                val vm: StudyViewModel = viewModel(
                    factory = viewModelFactory { StudyViewModel(graph, courseId) },
                )
                SessionScreen(viewModel = vm, onDone = { goHome() })
            }

            composable(Routes.PROGRESS) {
                val vm: ProgressViewModel = viewModel(
                    factory = viewModelFactory { ProgressViewModel(graph.stats, graph.courses) },
                )
                val ui by vm.ui.collectAsStateWithLifecycle()
                ProgressScreen(ui = ui)
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory { SettingsViewModel(graph.prefs) },
                )
                SettingsScreen(
                    viewModel = vm,
                    onImport = { nav.navigate(Routes.IMPORT) },
                    onSchedule = { nav.navigate(Routes.SCHEDULE) },
                    onResetOnboarding = {
                        nav.navigate(Routes.WELCOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.ACCESS_ACTIVE) {
                AccessActiveScreen(onClose = { nav.popBackStack() })
            }
        }
    }
}

@Composable
fun navigateToTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
