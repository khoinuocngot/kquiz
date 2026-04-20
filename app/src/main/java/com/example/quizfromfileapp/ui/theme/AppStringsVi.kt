package com.example.quizfromfileapp.ui.theme

// ═══════════════════════════════════════════════════════════════
// APP STRINGS — TIẾNG VIỆT + GEN Z
// Dùng object này để tất cả text trong app
// ═══════════════════════════════════════════════════════════════
object AppStringsVi {

    // ─── APP & GENERAL ────────────────────────────────────────────
    const val AppName = "Kquiz"
    val AppTagline = "Học gọn. Nhớ nhanh."

    // ─── NAVIGATION ───────────────────────────────────────────────
    val NavHome        = "Trang chủ"
    val NavStudySets   = "Bộ học"
    val NavHistory     = "Lịch sử"
    val NavAbout       = "Giới thiệu"

    // ─── HOME ────────────────────────────────────────────────────
    val HomeGreeting         = "Xin chào nha!"
    val HomeGreetingSub      = "Học hành là số một~"
    val HomeCreateNew        = "Tạo bộ học nè"
    val HomeContinueSection  = "Học tiếp nè"
    val HomeRecentSection    = "Bộ học gần đây"
    val HomeNoRecent         = "Chưa có bộ học nào hết á"
    val HomeEmptyMySets     = "Chưa có bộ học nào hết trơn"
    val HomeEmptyMySetsSub  = "Tạo bộ học đầu tiên nào!"
    val HomeStartLearning    = "Bắt đầu học nào"
    val HomeMyStudySets     = "Bộ học của tớ"
    val HomeTotalCards      = "thẻ"
    val HomeMastered        = "đã nhớ"

    // ─── STUDY SET ─────────────────────────────────────────────────
    val StudySetDetail   = "Chi tiết bộ học"
    val StudySetCards    = "thẻ"
    val StudySetCreated  = "Tạo lúc"
    val StudySetEmpty    = "Bộ học này chưa có thẻ nào"

    // Actions
    val ActionFlashcards = "Thẻ ghi nhớ"
    val ActionLearn     = "Học nè"
    val ActionTest      = "Kiểm tra"
    val ActionStart     = "Bắt đầu thôi"
    val ActionRetry     = "Làm lại phát nữa"
    val ActionReview    = "Ôn lại câu sai"
    val ActionRetake    = "Làm lại"
    val ActionBack      = "Quay về"

    // ─── QUICK IMPORT ─────────────────────────────────────────────
    val QuickImportTitle    = "Nhập nhanh từ văn bản"
    val QuickImportSubtitle = "Dán nội dung vào đây, app lo phần còn lại nha"
    val QuickImportTitleField = "Tên bộ học"
    val QuickImportTitleHint = "VD: Từ vựng TOEIC Unit 1"
    val QuickImportDescField = "Mô tả (không bắt buộc)"
    val QuickImportDescHint = "VD: Từ vựng bài 1-5"
    val QuickImportStep1     = "Chọn loại"
    val QuickImportStep2     = "Chọn định dạng"
    val QuickImportStep3     = "Dán nội dung"
    val QuickImportStep4     = "Xem trước"
    val QuickImportStep5     = "Tạo thôi"
    val QuickImportValidCards = "thẻ hợp lệ"
    val QuickImportInvalidLines = "dòng không parse được"
    val QuickImportCreate     = "Tạo bộ học luôn"
    val QuickImportCancel     = "Huỷ"
    val QuickImportEmptyRaw   = "Chưa dán gì vào hết trơn"

    // ─── IMPORT FILE ───────────────────────────────────────────────
    val ImportFileTitle     = "Nhập từ file"
    val ImportFileHero      = "Quăng file vào đây, app lo phần còn lại"
    val ImportFileSub       = "Hỗ trợ PDF, TXT, ảnh. App sẽ trích nội dung rồi biến thành bộ học cho bạn nè."
    val ImportFileSelect     = "Chọn file nè"
    val ImportFileProcess   = "Xử lý file"
    val ImportFileNext      = "Qua cấu hình quiz"
    val ImportFileCreate    = "Tạo bộ học từ file"
    val ImportFileEmpty     = "Chưa chọn file nào hết trơn"
    val ImportFileTip       = "File càng rõ ràng thì bộ học tạo ra càng ngon đó nha"

    // ─── FLASHCARD SCREEN ─────────────────────────────────────────
    val FlashcardHintFlip     = "Nhấn để lật thẻ"
    val FlashcardHintFlipBack = "Nhấn để lật lại"
    val FlashcardFrontLabel   = "Mặt trước"
    val FlashcardBackLabel    = "Đáp án"
    val FlashcardExplanation  = "Giải thích"
    val FlashcardSourcePage   = "Trang"
    val FlashcardSource       = "Nguồn"
    val FlashcardTapToFlip  = "Nhấn để lật thẻ"
    val FlashcardTapToFlip2 = "Nhấn để lật lại"
    val FlashcardFront      = "Mặt trước"
    val FlashcardBack       = "Đáp án"
    val FlashcardRemembered = "Mình nhớ rồi"
    val FlashcardNeedReview = "Cần ôn lại"
    val FlashcardPrev       = "Trước"
    val FlashcardNext       = "Sau"
    val FlashcardStarred    = "Đã ghim"
    val FlashcardUnstarred  = "Ghim thẻ"
    val FlashcardShuffle    = "Xáo trộn"
    val FlashcardResetOrder = "Khôi phục"
    val FlashcardEndSession = "Kết thúc phiên học!"
    val FlashcardSessionDone = "Bạn đã xem hết bộ thẻ rồi nè!"
    val FlashcardLearnAgain  = "Học lại"
    val FlashcardClose      = "Đóng"
    val FlashcardNoCards    = "Không có thẻ nào"
    val FlashcardNoCardsSub = "Bộ học này chưa có thẻ nào hết trơn"
    val FlashcardFilterAll  = "Tất cả thẻ"
    val FlashcardFilterStar  = "Đã ghim"
    val FlashcardFilterReview = "Cần ôn lại"
    val FlashcardFilterMastered = "Đã thành thạo"
    val FlashcardSortOriginal = "Thứ tự gốc"
    val FlashcardSortRandom  = "Ngẫu nhiên"
    val FlashcardSortReviewFirst = "Cần ôn trước"
    val FlashcardSortStarFirst  = "Đã ghim trước"
    val FlashcardOf         = " / "
    val FlashcardCorrect     = "đúng"
    val FlashcardWrong      = "sai"
    val FlashcardFilter       = "Lọc thẻ"
    val FlashcardSort         = "Sắp xếp"
    val FlashcardEndSessionSub = "Đây là kết quả phiên học của bạn"
    val FlashcardEndlessMode  = "Tiếp tục học"
    val FlashcardWrongReview  = "Ôn câu sai"
    val FlashcardDuration     = "Thời gian"
    val FlashcardOfLabel      = "Thẻ"
    val FlashcardTitleFormat  = "%s (%d thẻ)"

    // ─── LEARN SCREEN ─────────────────────────────────────────────
    val LearnTitle          = "Luyện học"
    val LearnCorrect        = "Chuẩn rồi!"
    val LearnIncorrect     = "Hix, chưa đúng rồi"
    val LearnContinue      = "Qua câu tiếp nè"
    val LearnNextQuestion  = "Câu tiếp theo nào"
    val LearnSessionOver   = "Xong rồi nè!"
    val LearnSessionSub    = "Bạn đã hoàn thành bài luyện học rồi nha"
    val LearnTotalCards    = "Tổng thẻ"
    val LearnCorrectCount  = "Đúng"
    val LearnWrongCount    = "Sai"
    val LearnBackToStudySet = "Quay về bộ học"
    val LearnTryAgain      = "Làm lại"
    val LearnMinCards       = "Cần ít nhất 4 thẻ"
    val LearnMinCardsSub   = "Bộ học này chỉ có"
    val LearnDirectionTermDef = "Thuật ngữ → Định nghĩa"
    val LearnDirectionDefTerm = "Định nghĩa → Thuật ngữ"
    val LearnDirectionQA       = "Câu hỏi → Đáp án"
    val LearnDirectionMCQ      = "Trắc nghiệm"
    val LearnSelectAnswer       = "Chọn đáp án đúng"
    val LearnSelectTerm         = "Chọn thuật ngữ đúng"
    val LearnWrongAnswers       = "câu sai"
    val LearnWrongAnswersReview = "Ôn câu sai"
    val LearnWrongReviewSub     = "Học lại những câu chưa đúng nha"

    // ─── TEST ─────────────────────────────────────────────────────
    val TestTitle       = "Kiểm tra"
    val TestConfig      = "Cấu hình bài kiểm tra"
    val TestStart       = "Bắt đầu thi nào"
    val TestSubmit      = "Nộp bài luôn"
    val TestConfirmSubmit = "Nộp bài luôn chưa?"
    val TestConfirmSub   = "Còn câu chưa trả lời kìa. Nộp luôn nhé?"
    val TestConfirmYes   = "Nộp luôn"
    val TestConfirmNo    = "Chưa, điều chỉnh lại"
    val TestProgress     = "Câu"
    val TestAnswered     = "Đã trả lời"
    val TestFlagged      = "Đánh dấu"
    val TestResult       = "Kết quả bài kiểm tra"
    val TestScore        = "Điểm số"
    val TestCorrect      = "Đúng"
    val TestWrong        = "Sai"
    val TestTotal        = "Tổng"
    val TestExcellent    = "Xuất sắc!"
    val TestGood         = "Tốt lắm rồi!"
    val TestMedium       = "Cố gắng hơn nha"
    val TestLow          = "Hãy ôn lại đi nào"
    val TestReviewWrong  = "Ôn lại câu sai"
    val TestFlagQuestion = "Đánh dấu câu này"
    val TestUnflagQuestion = "Bỏ đánh dấu"
    val TestTimerLabel   = "Thời gian"
    val TestTimerOff     = "Tắt"
    val TestTimerOn      = "Bật"

    // Test config
    val TestConfigAll      = "Tất cả thẻ"
    val TestConfigStarred  = "Chỉ đã ghim"
    val TestConfigReview   = "Cần ôn lại thôi"
    val TestConfigUnmastered = "Chưa thành thạo"
    val TestConfigRandomOn  = "Xáo trộn câu hỏi"
    val TestConfigRandomOff = "Giữ thứ tự"
    val TestConfigQA       = "Hỏi -> Đáp"
    val TestConfigAQ       = "Đáp -> Hỏi"
    val TestConfigMixed    = "Hỗn hợp"
    val TestConfigSource    = "Nguồn câu hỏi"
    val TestConfigQuestionCount = "Số câu hỏi"
    val TestConfigQuestionType  = "Loại câu hỏi"
    val TestConfigOptions   = "Tùy chọn"
    val TestConfigRandomQuestion = "Xáo trộn câu hỏi"
    val TestConfigRandomDesc    = "Sắp xếp câu hỏi ngẫu nhiên"
    val TestConfigTimerDesc     = "Tự động nộp bài khi hết giờ"
    val TestNeedMinCards         = "Cần ít nhất 2 thẻ"
    val TestStartTest            = "Bắt đầu kiểm tra"
    val TestTapToReveal          = "Nhấn để xem đáp án"
    val TestYouSelected          = "Bạn đã chọn: %s"
    val TestYouSkipped           = "Không trả lời"
    val TestDetailTab            = "Chi tiết"
    val TestAnswerTab            = "Đáp án"
    val TestYourAnswer           = "Bạn: %s"
    val TestNoWrongAnswers       = "Bạn đã trả lời đúng tất cả các câu!"
    val TestReviewMode           = "Chế độ ôn tập"
    val TestDirectionTermDef     = "Chọn đáp án đúng"
    val TestDirectionMixed       = "Chọn thuật ngữ đúng"
    val TestScoreMsgExcellent    = "Xuất sắc!"
    val TestScoreMsgGood         = "Tốt lắm!"
    val TestScoreMsgMedium       = "Khá ổn"
    val TestScoreMsgLow          = "Cần cố gắng hơn"
    val TestScoreMsgVeryLow      = "Cần học thêm"

    // ─── HISTORY ──────────────────────────────────────────────────
    val HistoryTitle    = "Lịch sử"
    val HistoryEmpty    = "Chưa có lịch sử học tập nào"
    val HistoryEmptySub = "Các bài kiểm tra sẽ hiện ở đây sau khi bạn học nha"
    val HistoryClearAll = "Xoá hết lịch sử"
    val HistoryClearConfirm = "Xoá hết lịch sử?"
    val HistoryClearSub  = "Tất cả lịch sử học tập sẽ bị xoá. Hành động này không thể hoàn tác đó nha."
    val HistoryClearYes  = "Xoá hết"
    val HistoryClearNo   = "Giữ lại"
    val HistoryScore     = "Điểm"
    val HistoryDate     = "Lúc"

    // ─── ABOUT ─────────────────────────────────────────────────────
    val AboutTitle      = "Giới thiệu"
    val AboutAppName    = "Kquiz"
    val AboutTagline    = "App học bài gọn lẹ, ôn phát nhớ luôn"
    val AboutDeveloper  = "Nhà phát triển"
    val AboutName       = "Ngô Đình Khôi"
    val AboutEmail      = "khoindce200286@gmail.com"
    val AboutEmailLabel = "Email"
    val AboutVersion    = "Phiên bản"
    val AboutTechStack = "Công nghệ"
    val AboutTechItems  = listOf(
        "Android & Kotlin",
        "Jetpack Compose",
        "Room Database",
        "MVVM Architecture"
    )
    val AboutCopyEmail  = "Sao chép email"
    val AboutSendEmail  = "Gửi email"
    val AboutEmailCopied = "Đã sao chép email rồi nè!"

    // ─── EMPTY STATES ──────────────────────────────────────────────
    val EmptyNoStudySets   = "Chưa có bộ học nào hết trơn"
    val EmptyNoStudySetsSub = "Tạo bộ học đầu tiên để bắt đầu học tập nào!"
    val EmptyNoCards        = "Chưa có thẻ nào hết trơn"
    val EmptyNoCardsSub    = "Thêm thẻ vào bộ học này để bắt đầu nha"
    val EmptyNoResults     = "Không tìm thấy gì hết trơn"
    val EmptyNoResultsSub  = "Thử từ khóa khác xem sao nè"
    val EmptyNoHistory     = "Chưa có lịch sử nào"
    val EmptyNoHistorySub  = "Học và kiểm tra để lưu lại tiến độ nha"
    val EmptyNoSearch      = "Không có kết quả nào hết"
    val EmptyNoSearchSub   = "Thử từ khóa khác nha"

    // ─── SNACKBAR / TOAST ──────────────────────────────────────────
    val SnackStudySetCreated = "Tạo bộ học xong rồi nè!"
    val SnackStudySetDeleted = "Đã xoá bộ học rồi nha"
    val SnackCardSaved      = "Đã lưu, yên tâm khỏi quên nè"
    val SnackCopied        = "Đã sao chép rồi nè!"
    val SnackDeleted       = "Đã xoá rồi nha"
    val SnackDuplicated    = "Đã tạo bản sao rồi nè"
    val SnackRenamed      = "Đổi tên xong rồi nè"
    val SnackError        = "Ôi có lỗi rồi, thử lại sau nha"
    val SnackNoCardSelected = "Chưa chọn đáp án nào hết trơn"
    val SnackImported     = "Nhập thành công rồi nè!"

    // ─── DIALOGS ──────────────────────────────────────────────────
    val DialogDeleteStudySet    = "Xoá bộ học này?"
    val DialogDeleteStudySetSub = "Bộ học \"{name}\" sẽ bị xoá vĩnh viễn. Hành động này không thể hoàn tác đó nha."
    val DialogDeleteConfirm     = "Xoá luôn"
    val DialogDeleteCancel      = "Huỷ"
    val DialogDeleteCard        = "Xoá thẻ này?"
    val DialogDeleteCardSub     = "Thẻ \"{term}\" sẽ bị xoá. Hành động này không thể hoàn tác."
    val DialogRenameStudySet    = "Đổi tên bộ học"
    val DialogRenameHint        = "Nhập tên mới cho bộ học"
    val DialogRenameConfirm     = "Lưu lại"
    val DialogDuplicateStudySet = "Tạo bản sao bộ học?"
    val DialogDuplicateSub      = "Một bản sao của \"{name}\" sẽ được tạo ngay thôi nè."
    val DialogDuplicateConfirm  = "Tạo bản sao"

    // ─── STUDY SET TYPE ────────────────────────────────────────────
    val StudySetTypeTermDef = "Thuật ngữ - Định nghĩa"
    val StudySetTypeQA      = "Câu hỏi - Đáp án"
    val StudySetTypeMCQ     = "Trắc nghiệm"

    // ─── CARD EDIT ─────────────────────────────────────────────────
    val CardEditTitle        = "Sửa thẻ"
    val CardEditTerm         = "Thuật ngữ / Câu hỏi"
    val CardEditTermHint     = "Nhập thuật ngữ hoặc câu hỏi"
    val CardEditDefinition   = "Định nghĩa / Đáp án"
    val CardEditDefinitionHint = "Nhập định nghĩa hoặc đáp án"
    val CardEditExplanation  = "Giải thích (tùy chọn)"
    val CardEditExplanationHint = "Nhập giải thích thêm nếu cần"
    val CardEditSave         = "Lưu thay đổi"
    val CardEditSaved        = "Đã lưu thay đổi rồi nè!"
    val CardEditError        = "Không lưu được, thử lại nha"
    val CardEditTitleHint    = "Tiêu đề bộ học"
    val CardEditDescHint     = "Mô tả (tùy chọn)"

    // ─── CARD SEARCH ───────────────────────────────────────────────
    val CardSearchPlaceholder = "Tìm trong bộ học…"
    val CardSearchClear      = "Xóa tìm kiếm"
    val CardSearchResult     = "%d kết quả"
    val CardSearchNoResult   = "Không tìm thấy thẻ nào"
    val CardSearchNoResultSub = "Thử từ khóa khác nha"

    // ─── STUDY SET LIST ────────────────────────────────────────────
    val StudySetListPin   = "Ghim"
    val StudySetListUnpin = "Bỏ ghim"
    val StudySetListFav  = "Yêu thích"
    val StudySetListUnfav = "Bỏ yêu thích"
    val StudySetListProgress = "Tiến độ"

    // ─── DETAIL SCREEN ─────────────────────────────────────────────
    val DetailEditInfo    = "Sửa thông tin"
    val DetailPin         = "Ghim lên đầu"
    val DetailUnpin       = "Bỏ ghim"
    val DetailFavorite    = "Yêu thích"
    val DetailUnfavorite  = "Bỏ yêu thích"
    val DetailDelete      = "Xóa bộ học"
    val DetailNotFound    = "Không tìm thấy bộ học"
    val DetailNotFoundSub = "Bộ học này có thể đã bị xóa"
    val DetailCardPreview = "Xem trước thẻ (%d)"
    val DetailSeeAll      = "Xem tất cả"
    val DetailChooseMode  = "Chọn chế độ học"
    val DetailStudyModes  = "Học bằng"
    val DetailFlashcards  = "Lật thẻ"
    val DetailLearn      = "Luyện hỏi"
    val DetailTest       = "Kiểm tra"
    val DetailProgress   = "Tiến độ thành thạo"
    val DetailMastered   = "thẻ đã thành thạo"
    val DetailNeedsReview = "thẻ cần ôn"
    val DetailTotal      = "Tổng thẻ"
    val DetailStarred    = "Đã ghim"
    val DetailNeedsStudy = "Cần ôn"
    val DetailUpdated    = "Cập nhật %s"
    val DetailPinned     = "Đã ghim"
    val DetailFavorited  = "Yêu thích"
    val DetailLabel       = "Xem trước thẻ"

    // ─── FILTER / SORT ─────────────────────────────────────────────
    val FilterAll       = "Tất cả"
    val FilterStarred   = "Đã ghim"
    val FilterRecent    = "Mới tạo gần đây"
    val FilterStudied   = "Đã học gần đây"
    val SortNewest      = "Mới nhất"
    val SortOldest      = "Cũ nhất"
    val SortAZ         = "A → Z"
    val SortZA         = "Z → A"
    val SortMostCards  = "Nhiều thẻ nhất"
    val SortLeastCards = "Ít thẻ nhất"

    // ─── SEARCH ───────────────────────────────────────────────────
    val SearchPlaceholder = "Tìm bộ học…"
    val SearchClear     = "Xoá tìm kiếm"

    // ─── IMPORT RESULT ─────────────────────────────────────────────
    val ImportResultTitle   = "Kết quả nhập"
    val ImportResultValid   = "thẻ hợp lệ"
    val ImportResultInvalid = "dòng không parse được"
    val ImportResultRaw     = "ký tự thô"
    val ImportResultCleaned = "ký tự sạch"
    val ImportResultPages   = "trang"
    val ImportResultSegs    = "đoạn"

    // ─── SESSION STATS ─────────────────────────────────────────────
    val SessionReviewed   = "Đã xem"
    val SessionRemembered = "Đã nhớ"
    val SessionNeedReview = "Cần ôn lại"
    val SessionStarred    = "Đã ghim"

    // ─── MASTERY ──────────────────────────────────────────────────
    val MasteryNew       = "Mới"
    val MasteryLearning  = "Đang học"
    val MasteryFamiliar  = "Quen rồi"
    val MasteryRemember  = "Nhớ rồi"
    val MasteryProficient = "Thành thạo"
    val MasteryMastered  = "Xuất sắc"
    val MasteryLabel     = "Mức độ"

    // ─── MISC ─────────────────────────────────────────────────────
    val Loading        = "Đang tải…"
    val Parsing       = "Đang phân tích…"
    val Processing    = "Đang xử lý…"
    val Saving        = "Đang lưu…"
    val Cancel        = "Huỷ"
    val Save          = "Lưu lại"
    val Delete        = "Xoá"
    val Edit          = "Sửa"
    val Rename        = "Đổi tên"
    val Duplicate     = "Tạo bản sao"
    val Share         = "Chia sẻ"
    val Copy          = "Sao chép"
    val Done          = "Xong rồi"
    val Confirm       = "Xác nhận"
    val Next          = "Tiếp theo"
    val Prev          = "Trước"
    val Yes           = "Có"
    val No            = "Không"
    val Ok            = "OK"
    val And           = "•"
    val Of            = "/"

    // ─── CHAT GPT PROMPTS ──────────────────────────────────────────
    val PromptTermDefTitle   = "Thuật ngữ - Định nghĩa"
    val PromptQATitle       = "Câu hỏi - Đáp án"
    val PromptMCQTitle      = "Trắc nghiệm"
    val PromptCopied        = "Copy prompt xong rồi nè"
    val PromptCopiedHint    = "Nhớ dán dữ liệu gốc của bạn ngay bên dưới prompt khi gửi ChatGPT nha"

    // Prompt chuẩn: THUẬT NGỮ - ĐỊNH NGHĨA
    val PromptTermDefBody = """Hãy chuyển nội dung tôi gửi thành đúng format để nhập vào app học.

YÊU CẦU BẮT BUỘC:
- Chỉ trả về đúng 1 code block duy nhất.
- Không giải thích gì thêm ngoài code block.
- Mỗi dòng đúng 1 thẻ.
- Vế trái là THUẬT NGỮ.
- Vế phải là ĐỊNH NGHĨA.
- Giữa 2 vế dùng đúng 1 ký tự TAB thật.
- Không thêm dòng trống.
- Không đánh số thứ tự.
- Không dùng bullet.
- Không xuống dòng trong cùng một thẻ.
- Nếu nội dung gốc có xuống dòng, hãy gộp lại thành 1 dòng.
- Không thay TAB bằng dấu khác.

FORMAT:
Thuật ngữ[TAB]Định nghĩa

Chỉ trả kết quả cuối cùng trong 1 code block duy nhất."""

    // Prompt chuẩn: CÂU HỎI - ĐÁP ÁN
    val PromptQABody = """Hãy chuyển nội dung tôi gửi thành đúng format để nhập vào app học.

YÊU CẦU BẮT BUỘC:
- Chỉ trả về đúng 1 code block duy nhất.
- Không giải thích gì thêm ngoài code block.
- Mỗi dòng đúng 1 thẻ.
- Vế trái là CÂU HỎI.
- Vế phải là ĐÁP ÁN ĐÚNG.
- Giữa 2 vế dùng đúng 1 ký tự TAB thật.
- Không thêm dòng trống.
- Không đánh số thứ tự.
- Không dùng bullet.
- Không xuống dòng trong cùng một thẻ.
- Nếu nội dung gốc có xuống dòng, hãy gộp lại thành 1 dòng.
- Không thay TAB bằng dấu khác.

FORMAT:
Câu hỏi[TAB]Đáp án

Chỉ trả kết quả cuối cùng trong 1 code block duy nhất."""

    // Prompt chuẩn: TRẮC NGHIỆM
    val PromptMCQBody = """Hãy chuyển nội dung tôi gửi thành đúng format để nhập vào app học.

YÊU CẦU BẮT BUỘC:
- Chỉ trả về đúng 1 code block duy nhất.
- Không giải thích gì thêm ngoài code block.
- Mỗi dòng đúng 1 câu hỏi.
- Vế trái phải chứa TOÀN BỘ câu hỏi trắc nghiệm gồm nội dung câu hỏi và các lựa chọn A. B. C. D. E. nếu có.
- Vế phải là ĐÁP ÁN ĐÚNG.
- Giữa 2 vế dùng đúng 1 ký tự TAB thật.
- Không thêm dòng trống.
- Không đánh số thứ tự.
- Không dùng bullet.
- Không xuống dòng trong cùng một câu.
- Toàn bộ câu hỏi và lựa chọn phải nằm trên cùng 1 dòng ở vế trái.
- Nếu nội dung gốc có xuống dòng, hãy gộp lại thành 1 dòng.
- Không thêm giải thích.
- Không thay TAB bằng dấu khác.

FORMAT:
Câu hỏi A. lựa chọn A B. lựa chọn B C. lựa chọn C D. lựa chọn D[TAB]Đáp án đúng

Chỉ trả kết quả cuối cùng trong 1 code block duy nhất."""

    // ─── EXAMPLE PLACEHOLDERS ─────────────────────────────────────
    val ExampleTDTerm1   = "CPU"
    val ExampleTDDef1    = "Bộ xử lý trung tâm của máy tính"
    val ExampleTDTerm2   = "RAM"
    val ExampleTDDef2    = "Bộ nhớ tạm thời"
    val ExampleTDTerm3   = "ROM"
    val ExampleTDDef3    = "Bộ nhớ chỉ đọc"
    val ExampleQTerm1    = "CPU là gì?"
    val ExampleQDef1     = "Bộ xử lý trung tâm"
    val ExampleQTerm2    = "RAM dùng để làm gì?"
    val ExampleQDef2     = "Lưu dữ liệu tạm thời khi máy đang chạy"
    val ExampleQTerm3   = "ROM là gì?"
    val ExampleQDef3     = "Bộ nhớ chỉ đọc, lưu trữ firmware"

    // ─── FOLDER ─────────────────────────────────────────────────
    val FolderTitle       = "Thư mục"
    val FolderCreate     = "Tạo thư mục mới"
    val FolderEdit       = "Sửa thư mục"
    val FolderDelete     = "Xoá thư mục"
    val FolderDeleteConfirm = "Xoá thư mục này?"
    val FolderDeleteSub   = "Thư mục \"{name}\" sẽ bị xoá. Các bộ học bên trong sẽ không bị xoá."
    val FolderEmpty      = "Chưa có thư mục nào"
    val FolderEmptySub   = "Tạo thư mục để nhóm bộ học theo môn/chủ đề nha"
    val FolderNameHint    = "Tên thư mục"
    val FolderDescHint   = "Mô tả (không bắt buộc)"
    val FolderNoSets     = "Thư mục trống"
    val FolderNoSetsSub  = "Chưa có bộ học nào trong thư mục này"
    val FolderAll        = "Tất cả bộ học"
    val FolderUncategorized = "Chưa phân loại"
    val FolderSets       = "bộ học"

    // ─── TAG ───────────────────────────────────────────────────
    val TagTitle         = "Nhãn"
    val TagCreate        = "Tạo nhãn mới"
    val TagEdit          = "Sửa nhãn"
    val TagDelete        = "Xoá nhãn"
    val TagDeleteConfirm = "Xoá nhãn này?"
    val TagDeleteSub     = "Nhãn \"{name}\" sẽ bị xoá khỏi tất cả bộ học."
    val TagEmpty         = "Chưa có nhãn nào"
    val TagEmptySub      = "Tạo nhãn để phân loại bộ học nha"
    val TagNameHint      = "Tên nhãn"
    val TagSelect        = "Chọn nhãn"
    val TagApplied       = "Đã gắn nhãn"

    // ─── SMART REVIEW ──────────────────────────────────────────
    val SmartReviewTitle       = "Ôn thông minh"
    val SmartReviewDesc        = "Những thẻ cần ôn nhất sẽ hiện trước nè"
    val SmartReviewEmpty       = "Hết thẻ cần ôn rồi!"
    val SmartReviewEmptySub    = "Tất cả thẻ đều đã được ôn kỹ rồi nha 🎉"
    val SmartReviewEmptySub2   = "Hãy tiếp tục học để thêm thẻ mới nào"
    val SmartReviewNeedCards   = "Cần ít nhất 1 thẻ để ôn"
    val SmartReviewPriority    = "Ưu tiên"
    val SmartReviewNew         = "Thẻ mới"
    val SmartReviewWeak        = "Cần ôn nhiều"
    val SmartReviewDue         = "Đến lúc ôn"

    // ─── PROGRESS / STREAK ─────────────────────────────────────
    val ProgressStreak    = "Chuỗi ngày học"
    val ProgressDay       = "ngày"
    val ProgressDays      = "ngày"
    val ProgressToday     = "Hôm nay"
    val ProgressThisWeek  = "Tuần này"
    val ProgressTotal     = "Tổng cộng"
    val ProgressCardsStudied = "thẻ đã học"
    val ProgressNoStreak  = "Chưa có chuỗi"
    val ProgressStartStreak = "Học ngay để bắt đầu chuỗi!"
    val ProgressBestStreak = "Kỷ lục"
    val ProgressKeepItUp  = "Giữ vững nào!"

    // ─── FILTER ────────────────────────────────────────────────
    val FilterRecentAll      = "Mới nhất"
    val FilterStarredAll     = "Yêu thích"
    val FilterFolder         = "Theo thư mục"
    val FilterTag         = "Theo nhãn"
    val FilterAllSets    = "Tất cả"
    val SortStreak       = "Học nhiều nhất"

    // ─── EXPORT / SHARE ────────────────────────────────────────────
    val ExportShare       = "Xuất & Chia sẻ"
    val ExportTitle       = "Xuất bộ học"
    val ExportStudySet   = "Xuất file bộ học"
    val ExportStudySetDesc = "Chia sẻ sang thiết bị khác"
    val ExportJson       = "Xuất JSON"
    val ExportJsonDesc   = "Backup & khôi phục lại app"
    val ExportCsv        = "Xuất CSV"
    val ExportCsvDesc   = "Mở bằng Excel / Google Sheets"
    val ExportTxt        = "Xuất TXT"
    val ExportTxtDesc   = "Text đơn giản, copy được"
    val ExportSuccess    = "Xuất thành công rồi nè!"
    val ExportFailed     = "Xuất thất bại, thử lại nha"
    val ShareStudySet    = "Chia sẻ bộ học"
    val ShareText        = "Chia sẻ dạng text"
    val ShareTextDesc   = "Copy text bộ học để paste chỗ khác"
    val ExportWrongAnswers = "Xuất câu sai"
    val ExportWrongDesc  = "Tạo bộ học từ câu sai trong bài kiểm tra"
    val ExportedAs       = "Đã xuất: %s"

    // ─── BULK CARD EDIT ─────────────────────────────────────────────
    val BulkEditTitle    = "Sửa nhiều thẻ"
    val BulkSelect       = "Chọn nhiều thẻ"
    val BulkSelectAll    = "Chọn tất cả"
    val BulkDeselectAll  = "Bỏ chọn tất cả"
    val BulkSelected     = "%d thẻ đã chọn"
    val BulkStar         = "Ghim thẻ đã chọn"
    val BulkUnstar       = "Bỏ ghim thẻ đã chọn"
    val BulkDelete       = "Xóa thẻ đã chọn"
    val BulkDeleteConfirm = "Xóa %d thẻ đã chọn?"
    val BulkDeleteSub    = "Hành động này không thể hoàn tác đó nha."
    val BulkDeleteConfirmBtn = "Xóa luôn"
    val BulkNoSelection  = "Chưa chọn thẻ nào hết trơn"
    val BulkActions      = "Hành động"
    val BulkExit         = "Thoát chọn"
    val BulkCardsDeleted = "Đã xoá %d thẻ rồi nè"
    val BulkCardsStarred = "Đã ghim %d thẻ rồi nè"
    val BulkCardsUnstarred = "Đã bỏ ghim %d thẻ rồi nè"
    val BulkCardAdd      = "Thêm thẻ mới"
    val BulkCardAddTitle = "Thêm thẻ"

    // ─── GAMIFICATION ────────────────────────────────────────────────
    val XpLabel          = "XP"
    val LevelLabel       = "Cấp"
    val LevelUp          = "Lên cấp!"
    val LevelUpSub       = "Bạn đã đạt cấp %d rồi nè!"
    val XpGained         = "+%d XP"
    val DailyGoal        = "Mục tiêu hàng ngày"
    val DailyGoalSet     = "Mục tiêu: %d thẻ/ngày"
    val DailyGoalProgress = "%d / %d thẻ"
    val DailyGoalComplete = "Hoàn thành mục tiêu hôm nay!"
    val DailyGoalCelebrate = "Chúc mừng bạn đã hoàn thành mục tiêu ngày hôm nay nè! 🎉"
    val DailyGoalConfig  = "Cài mục tiêu"
    val DailyGoal5       = "5 thẻ — Nhẹ nhàng"
    val DailyGoal10      = "10 thẻ — Bình thường"
    val DailyGoal20      = "20 thẻ — Nỗ lực"
    val DailyGoal30      = "30 thẻ — Khó lắm"
    val TotalXp          = "Tổng XP"
    val TotalLevel       = "Cấp hiện tại"
    val XpPerCard        = "+%d XP/thẻ"
    val XpPerLearn       = "+%d XP/đúng"
    val XpPerTest        = "+%d XP/bài"
    val XpFlashcard      = "Lật thẻ"
    val XpLearn          = "Học đúng"
    val XpTest           = "Làm bài"
    val XpSmartReview    = "Ôn thông minh"

    // ─── POLISH ────────────────────────────────────────────────────
    val FirstTimeHint    = "Nhấn đây để tạo bộ học đầu tiên nè!"
    val PullToRefresh    = "Đang cập nhật…"
    val TapToDismiss     = "Nhấn để đóng"
    val LoadingCards     = "Đang tải thẻ…"
    val NoMoreCards      = "Hết thẻ rồi nè!"
    val FirstCardHint    = "Đây là thẻ đầu tiên của bạn"
    val SettingsTitle    = "Cài đặt"
    val DailyRecapTitle  = "Tổng kết hôm nay"
    val DailyRecapStudied = "Bạn đã học %d thẻ"
    val DailyRecapCorrect = "Đúng %d câu"
    val DailyRecapStreak = "Chuỗi %d ngày liên tiếp"

    // ─── AUDIO ─────────────────────────────────────────────────────────
    val AudioVoiceLabel   = "Giọng đọc"
    val AudioVoiceDesc    = "Bật giọng đọc cho thẻ học"
    val AudioSfxLabel     = "Âm thanh"
    val AudioSfxDesc      = "Âm thanh khi chọn đáp án, lật thẻ"
    val AudioSettings     = "Cài đặt âm thanh"
}
