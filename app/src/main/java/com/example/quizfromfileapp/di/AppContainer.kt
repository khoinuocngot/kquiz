package com.example.quizfromfileapp.di

import android.content.Context
import com.example.quizfromfileapp.data.local.AppDatabase
import com.example.quizfromfileapp.data.local.QuizHistoryStorage
import com.example.quizfromfileapp.data.local.StudySetStorage
import com.example.quizfromfileapp.data.repository.ExportRepository
import com.example.quizfromfileapp.data.repository.FlashcardExportInfo
import com.example.quizfromfileapp.data.repository.StudySetExportData
import com.example.quizfromfileapp.domain.usecase.ImportStudySetUseCase
import com.example.quizfromfileapp.data.repository.GamificationRepository
import com.example.quizfromfileapp.data.repository.OrganizationRepository
import com.example.quizfromfileapp.data.repository.QuizHistoryRepository
import com.example.quizfromfileapp.data.repository.StudyAudioManager
import com.example.quizfromfileapp.data.repository.StudySetRepository
import com.example.quizfromfileapp.data.repository.StudySetRepositoryRoom
import com.example.quizfromfileapp.domain.usecase.ClearQuizHistoryUseCase
import com.example.quizfromfileapp.domain.usecase.DeleteQuizHistoryUseCase
import com.example.quizfromfileapp.domain.usecase.GetQuizHistoryUseCase
import com.example.quizfromfileapp.domain.usecase.SaveQuizHistoryUseCase
import kotlinx.coroutines.runBlocking

object AppContainer {
    private lateinit var quizHistoryStorage: QuizHistoryStorage
    private lateinit var quizHistoryRepository: QuizHistoryRepository

    private lateinit var appDatabase: AppDatabase
    lateinit var studySetRepository: StudySetRepository

    // Room-based repository (new)
    lateinit var studySetRepositoryRoom: StudySetRepositoryRoom

    // Organization repository (folders, tags, stats)
    lateinit var organizationRepository: OrganizationRepository

    // Export repository (export/shares)
    lateinit var exportRepository: ExportRepository

    // Gamification repository (XP, Level, Daily Goal)
    lateinit var gamificationRepository: GamificationRepository

    // Audio manager (TTS + Sound effects)
    lateinit var audioManager: StudyAudioManager

    // Quiz History UseCases
    val getQuizHistoryUseCase: GetQuizHistoryUseCase by lazy { GetQuizHistoryUseCase(quizHistoryRepository) }
    val saveQuizHistoryUseCase: SaveQuizHistoryUseCase by lazy { SaveQuizHistoryUseCase(quizHistoryRepository) }
    val deleteQuizHistoryUseCase: DeleteQuizHistoryUseCase by lazy { DeleteQuizHistoryUseCase(quizHistoryRepository) }
    val clearQuizHistoryUseCase: ClearQuizHistoryUseCase by lazy { ClearQuizHistoryUseCase(quizHistoryRepository) }

    // Import Study Set UseCase
    val importStudySetUseCase: ImportStudySetUseCase by lazy {
        ImportStudySetUseCase(exportRepository, studySetRepositoryRoom)
    }

    /**
     * Initialize the app container.
     * Sets up Room database, repositories, and runs JSON migration if needed.
     */
    fun initialize(context: Context) {
        quizHistoryStorage = QuizHistoryStorage(context)
        quizHistoryRepository = QuizHistoryRepository(quizHistoryStorage)

        // Initialize Room database
        appDatabase = AppDatabase.getInstance(context)
        studySetRepositoryRoom = StudySetRepositoryRoom(appDatabase)

        // Initialize legacy repository (backward compat) using Room
        studySetRepository = StudySetRepository(studySetRepositoryRoom)

        // Initialize organization repository
        organizationRepository = OrganizationRepository(appDatabase)

        // Initialize export repository
        exportRepository = ExportRepository(context)

        // Initialize gamification repository
        gamificationRepository = GamificationRepository(context)

        // Initialize audio manager
        audioManager = StudyAudioManager(context)

        // Run JSON → Room migration in background
        runBlocking {
            studySetRepositoryRoom.migrateFromJson(context)
        }
    }

    /**
     * Direct access to Room DAOs (for advanced use cases).
     */
    fun getAppDatabase(): AppDatabase = appDatabase
}
