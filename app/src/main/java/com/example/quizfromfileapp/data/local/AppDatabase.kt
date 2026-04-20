package com.example.quizfromfileapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.quizfromfileapp.data.local.dao.FlashcardDao
import com.example.quizfromfileapp.data.local.dao.FolderDao
import com.example.quizfromfileapp.data.local.dao.StudySetDao
import com.example.quizfromfileapp.data.local.dao.StudyStatsDao
import com.example.quizfromfileapp.data.local.dao.TagDao
import com.example.quizfromfileapp.data.local.entity.DailyStudyStatsEntity
import com.example.quizfromfileapp.data.local.entity.FlashcardEntityRoom
import com.example.quizfromfileapp.data.local.entity.FolderEntity
import com.example.quizfromfileapp.data.local.entity.StudySetEntityRoom
import com.example.quizfromfileapp.data.local.entity.StudySetTagCrossRef
import com.example.quizfromfileapp.data.local.entity.TagEntity

@Database(
    entities = [
        StudySetEntityRoom::class,
        FlashcardEntityRoom::class,
        FolderEntity::class,
        TagEntity::class,
        StudySetTagCrossRef::class,
        DailyStudyStatsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studySetDao(): StudySetDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun studyStatsDao(): StudyStatsDao

    companion object {
        private const val DATABASE_NAME = "quiz_app_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Destroy the singleton instance.
         * Use only in tests or when you need to reset completely.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
