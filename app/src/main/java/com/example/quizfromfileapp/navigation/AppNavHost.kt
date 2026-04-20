package com.example.quizfromfileapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.quizfromfileapp.ui.screens.AppSharedViewModel
import com.example.quizfromfileapp.ui.screens.about.AboutScreen
import com.example.quizfromfileapp.ui.screens.history.HistoryScreen
import com.example.quizfromfileapp.ui.screens.home.HomeScreen
import com.example.quizfromfileapp.ui.screens.importfile.ImportFileScreen
import com.example.quizfromfileapp.ui.screens.organization.OrganizationScreen
import com.example.quizfromfileapp.ui.screens.organization.SmartReviewScreen
import com.example.quizfromfileapp.ui.screens.processing.ProcessingScreen
import com.example.quizfromfileapp.ui.screens.quiz.QuizScreen
import com.example.quizfromfileapp.ui.screens.quizconfig.QuizConfigScreen
import com.example.quizfromfileapp.ui.screens.quickimport.QuickImportDialog
import com.example.quizfromfileapp.ui.screens.result.ResultScreen
import com.example.quizfromfileapp.ui.screens.studyset.StudySetListScreen
import com.example.quizfromfileapp.ui.screens.studyset.detail.StudySetDetailScreen
import com.example.quizfromfileapp.ui.screens.studyset.detail.CardEditScreen
import com.example.quizfromfileapp.ui.screens.studyset.flashcard.FlashcardScreen
import com.example.quizfromfileapp.ui.screens.studyset.importfile.ImportStudySetPreviewScreen
import com.example.quizfromfileapp.ui.screens.studyset.learn.LearnScreen
import com.example.quizfromfileapp.ui.screens.studyset.test.ReviewWrongAnswersScreen
import com.example.quizfromfileapp.ui.screens.studyset.test.TestConfigScreen
import com.example.quizfromfileapp.ui.screens.studyset.test.TestResultScreen
import com.example.quizfromfileapp.ui.screens.studyset.test.TestScreen
import com.example.quizfromfileapp.ui.screens.studyset.test.TestViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(navController: NavHostController) {
    val sharedViewModel = remember { AppSharedViewModel() }

    // Shared TestViewModel - tạo 1 lần, dùng chung cho TestConfig → Test → TestResult
    var currentTestStudySetId by remember { mutableStateOf(0L) }
    val testViewModel: TestViewModel = viewModel(
        key = "test_view_model_$currentTestStudySetId"
    )

    // Quick import dialog state
    var showQuickImport by remember { mutableStateOf(false) }
    var pendingStudySetId by remember { mutableStateOf<Long?>(null) }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                onNavigateToImportFile = {
                    sharedViewModel.clearAll()
                    navController.navigate(AppRoute.ImportFile.route)
                },
                onNavigateToHistory = {
                    navController.navigate(AppRoute.History.route)
                },
                onNavigateToStudySetList = {
                    navController.navigate(AppRoute.StudySetList.route)
                },
                onNavigateToAbout = {
                    navController.navigate(AppRoute.About.route)
                },
                onNavigateToOrganization = {
                    navController.navigate(AppRoute.Folders.route)
                },
                onNavigateToSmartReview = {
                    navController.navigate(AppRoute.SmartReview.route)
                },
                onNavigateToImportStudySet = {
                    navController.navigate(AppRoute.ImportStudySet.route)
                },
                onShowQuickImport = { showQuickImport = true },
                onNavigateToStudySet = { studySetId ->
                    navController.navigate(AppRoute.StudySetDetail.createRoute(studySetId))
                }
            )
        }

        composable(AppRoute.ImportFile.route) {
            ImportFileScreen(
                sharedViewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProcessing = {
                    navController.navigate(AppRoute.Processing.route)
                }
            )
        }

        composable(AppRoute.Processing.route) {
            ProcessingScreen(
                sharedViewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack(AppRoute.Home.route, inclusive = false)
                },
                onNavigateToQuizConfig = {
                    navController.navigate(AppRoute.QuizConfig.route)
                }
            )
        }

        composable(AppRoute.QuizConfig.route) {
            QuizConfigScreen(
                sharedViewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToQuiz = {
                    navController.navigate(AppRoute.Quiz.route)
                }
            )
        }

        composable(AppRoute.Quiz.route) {
            QuizScreen(
                sharedViewModel = sharedViewModel,
                onNavigateBack = {
                    sharedViewModel.clearUserAnswers()
                    sharedViewModel.setCurrentQuestionIndex(0)
                    navController.popBackStack(AppRoute.Home.route, inclusive = false)
                },
                onNavigateToResult = {
                    navController.navigate(AppRoute.Result.route) {
                        popUpTo(AppRoute.Home.route)
                    }
                }
            )
        }

        composable(AppRoute.Result.route) {
            ResultScreen(
                sharedViewModel = sharedViewModel,
                onNavigateBack = {
                    sharedViewModel.clearUserAnswers()
                    sharedViewModel.setCurrentQuestionIndex(0)
                    navController.popBackStack(AppRoute.Quiz.route, inclusive = false)
                },
                onNavigateToHome = {
                    sharedViewModel.clearAll()
                    navController.navigate(AppRoute.Home.route) {
                        popUpTo(AppRoute.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.History.route) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoute.ImportStudySet.route) {
            ImportStudySetPreviewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onImportSuccess = { studySetId ->
                    navController.popBackStack()
                    navController.navigate(AppRoute.StudySetDetail.createRoute(studySetId))
                }
            )
        }

        composable(AppRoute.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoute.Folders.route) {
            OrganizationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoute.SmartReview.route) {
            SmartReviewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ═══════════════════════════════════════════════════════
        // STUDY SET ROUTES
        // ═══════════════════════════════════════════════════════

        composable(AppRoute.StudySetList.route) {
            StudySetListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { studySetId ->
                    navController.navigate(AppRoute.StudySetDetail.createRoute(studySetId))
                },
                onShowQuickImport = { showQuickImport = true }
            )
        }

        composable(
            route = AppRoute.StudySetDetail.route,
            arguments = listOf(navArgument("studySetId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable
            StudySetDetailScreen(
                studySetId = studySetId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToFlashcards = { id ->
                    navController.navigate(AppRoute.Flashcard.createRoute(id))
                },
                onNavigateToLearn = { id ->
                    navController.navigate(AppRoute.Learn.createRoute(id))
                },
                onNavigateToTestConfig = { id ->
                    currentTestStudySetId = id
                    navController.navigate(AppRoute.TestConfig.createRoute(id))
                },
                onEditCard = { studySetId, cardId ->
                    navController.navigate(AppRoute.CardEdit.createRoute(studySetId, cardId))
                }
            )
        }

        composable(
            route = AppRoute.Flashcard.route,
            arguments = listOf(navArgument("studySetId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350)) },
            popEnterTransition = { slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)) },
            popExitTransition = { slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)) }
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable
            FlashcardScreen(
                studySetId = studySetId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AppRoute.Learn.route,
            arguments = listOf(navArgument("studySetId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350)) },
            popEnterTransition = { slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)) },
            popExitTransition = { slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)) }
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable
            LearnScreen(
                studySetId = studySetId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ─── TEST FLOW: cùng shared TestViewModel ───

        composable(
            route = AppRoute.TestConfig.route,
            arguments = listOf(navArgument("studySetId") { type = NavType.LongType }),
            enterTransition = { fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(250)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(250)) }
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable
            currentTestStudySetId = studySetId

            TestConfigScreen(
                studySetId = studySetId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartTest = {
                    // startTest() đã được gọi trong TestConfigScreen, ở đây chỉ navigate
                    navController.navigate(AppRoute.Test.createRoute(studySetId))
                },
                viewModel = testViewModel
            )
        }

        composable(
            route = AppRoute.Test.route,
            arguments = listOf(navArgument("studySetId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350)) },
            popEnterTransition = { slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)) },
            popExitTransition = { slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)) }
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable
            currentTestStudySetId = studySetId

            TestScreen(
                studySetId = studySetId,
                onNavigateBack = {
                    navController.popBackStack(AppRoute.StudySetDetail.route, inclusive = false)
                },
                onSubmit = {
                    navController.navigate(AppRoute.TestResult.createRoute(studySetId))
                },
                viewModel = testViewModel
            )
        }

        composable(
            route = AppRoute.TestResult.route,
            arguments = listOf(navArgument("studySetId") { type = NavType.LongType }),
            enterTransition = { fadeIn(tween(350)) + slideInHorizontally(tween(350)) { it / 4 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutHorizontally(tween(350)) { it / 4 } + fadeOut(tween(350)) }
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable
            currentTestStudySetId = studySetId

            TestResultScreen(
                studySetId = studySetId,
                viewModel = testViewModel,
                onRetake = {
                    // retakeTest() đã được gọi trong TestResultScreen
                    navController.navigate(AppRoute.Test.createRoute(studySetId)) {
                        popUpTo(AppRoute.TestConfig.route) { inclusive = false }
                    }
                },
                onReviewWrongAnswers = {
                    navController.navigate(AppRoute.ReviewWrongAnswers.createRoute(studySetId))
                },
                onBackToStudySet = {
                    navController.popBackStack(AppRoute.StudySetDetail.route, inclusive = false)
                }
            )
        }

        composable(
            route = AppRoute.ReviewWrongAnswers.route,
            arguments = listOf(navArgument("studySetId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable

            ReviewWrongAnswersScreen(
                studySetId = studySetId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = testViewModel
            )
        }

        composable(
            route = AppRoute.CardEdit.route,
            arguments = listOf(
                navArgument("studySetId") { type = NavType.LongType },
                navArgument("cardId") { type = NavType.LongType }
            ),
            enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
            exitTransition = { slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350)) },
            popEnterTransition = { slideInHorizontally(tween(350)) { -it / 3 } + fadeIn(tween(350)) },
            popExitTransition = { slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)) }
        ) { backStackEntry ->
            val studySetId = backStackEntry.arguments?.getLong("studySetId") ?: return@composable
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable

            CardEditScreen(
                studySetId = studySetId,
                cardId = cardId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    // Quick Import Bottom Sheet
    if (showQuickImport) {
        QuickImportDialog(
            onDismiss = { showQuickImport = false },
            onSuccess = { studySetId ->
                showQuickImport = false
                pendingStudySetId = studySetId
                navController.navigate(AppRoute.StudySetDetail.createRoute(studySetId))
            }
        )
    }
}
