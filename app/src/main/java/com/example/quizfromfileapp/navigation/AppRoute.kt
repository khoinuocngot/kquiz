package com.example.quizfromfileapp.navigation

sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object ImportFile : AppRoute("import_file")
    data object Processing : AppRoute("processing")
    data object QuizConfig : AppRoute("quiz_config")
    data object Quiz : AppRoute("quiz")
    data object Result : AppRoute("result")
    data object History : AppRoute("history")

    // Study Set routes
    data object StudySetList : AppRoute("study_set_list")
    data object StudySetDetail : AppRoute("study_set_detail/{studySetId}") {
        fun createRoute(studySetId: Long) = "study_set_detail/$studySetId"
    }
    data object Flashcard : AppRoute("flashcard/{studySetId}") {
        fun createRoute(studySetId: Long) = "flashcard/$studySetId"
    }
    data object Learn : AppRoute("learn/{studySetId}") {
        fun createRoute(studySetId: Long) = "learn/$studySetId"
    }
    data object TestConfig : AppRoute("test_config/{studySetId}") {
        fun createRoute(studySetId: Long) = "test_config/$studySetId"
    }
    data object Test : AppRoute("test/{studySetId}") {
        fun createRoute(studySetId: Long) = "test/$studySetId"
    }
    data object TestResult : AppRoute("test_result/{studySetId}") {
        fun createRoute(studySetId: Long) = "test_result/$studySetId"
    }
    data object ReviewWrongAnswers : AppRoute("review_wrong/{studySetId}") {
        fun createRoute(studySetId: Long) = "review_wrong/$studySetId"
    }
    data object CardEdit : AppRoute("card_edit/{studySetId}/{cardId}") {
        fun createRoute(studySetId: Long, cardId: Long) = "card_edit/$studySetId/$cardId"
    }

    // Import/Export
    data object ImportStudySet : AppRoute("import_study_set")

    // Other
    data object About : AppRoute("about")
    data object Folders : AppRoute("folders")
    data object SmartReview : AppRoute("smart_review")
}
