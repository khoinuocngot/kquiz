package com.example.quizfromfileapp.data.quickimport

import com.example.quizfromfileapp.data.local.entity.FlashcardEntityRoom

// Loại bộ học
enum class StudySetType(
    val displayName: String,
    val labelFront: String,
    val labelBack: String,
    val selectorLabel: String,
    val itemType: String
) {
    TERM_DEFINITION("Thuật ngữ - Định nghĩa", "THUẬT NGỮ", "ĐỊNH NGHĨA", "Thuật ngữ - Định nghĩa", FlashcardEntityRoom.ITEM_TYPE_TERM_DEFINITION),
    QUESTION_ANSWER("Câu hỏi - Đáp án", "CÂU HỎI", "ĐÁP ÁN", "Câu hỏi - Đáp án", FlashcardEntityRoom.ITEM_TYPE_QUESTION_ANSWER),
    MULTIPLE_CHOICE_BANK("Trắc nghiệm", "CÂU HỎI", "ĐÁP ÁN ĐÚNG", "Trắc nghiệm", FlashcardEntityRoom.ITEM_TYPE_MULTIPLE_CHOICE);

    fun toPromptTemplateType(): PromptTemplateType = when (this) {
        TERM_DEFINITION -> PromptTemplateType.TERM_DEFINITION
        QUESTION_ANSWER -> PromptTemplateType.QUESTION_ANSWER
        MULTIPLE_CHOICE_BANK -> PromptTemplateType.MULTIPLE_CHOICE
    }
}

data class QuickImportConfig(
    val title: String = "",
    val description: String = "",
    val rawText: String = "",
    val termDefDelimiter: TermDefDelimiter = TermDefDelimiter.TAB,
    val cardDelimiterMode: CardDelimiterMode = CardDelimiterMode.ONE_PER_LINE,
    val cardDelimiterCustom: String = "",
    val studySetType: StudySetType = StudySetType.MULTIPLE_CHOICE_BANK
) {
    enum class TermDefDelimiter(val displayName: String, val value: String) {
        TAB("Tab", "\t"),
        COMMA("Dấu phẩy (,)", ","),
        COLON("Dấu hai chấm (:)", ":"),
        SEMICOLON("Dấu chấm phẩy (;)", ";"),
        PIPE("Dấu gạch đứng (|)", "|"),
        ARROW("Mũi tên (->)", "->"),
        EQUALS("Dấu bằng (=)", "="),
        CUSTOM("Tùy chỉnh", "")
    }

    enum class CardDelimiterMode(val displayName: String) {
        ONE_PER_LINE("Mỗi dòng là 1 thẻ"),
        CUSTOM("Tùy chỉnh")
    }
}

data class ParsedCard(
    val term: String,
    val definition: String,
    val isValid: Boolean,
    val rawLine: String,
    val errorMessage: String? = null,
    // MCQ fields
    val choices: List<String> = emptyList(),
    val correctChoiceIndex: Int = -1,
    val itemType: String = FlashcardEntityRoom.ITEM_TYPE_TERM_DEFINITION
)

data class ParseResult(
    val validCards: List<ParsedCard>,
    val invalidLines: List<ParsedCard>,
    val totalLines: Int,
    val rawLineCount: Int = 0,
    val rawCharCount: Int = 0
) {
    val validCount: Int get() = validCards.size
    val invalidCount: Int get() = invalidLines.size
    val isEmpty: Boolean get() = validCards.isEmpty()
}

enum class PromptTemplateType(val tabLabel: String) {
    TERM_DEFINITION("Thuật ngữ - Định nghĩa"),
    QUESTION_ANSWER("Câu hỏi - Đáp án"),
    MULTIPLE_CHOICE("Trắc nghiệm");

    fun toStudySetType(): StudySetType = when (this) {
        TERM_DEFINITION -> StudySetType.TERM_DEFINITION
        QUESTION_ANSWER -> StudySetType.QUESTION_ANSWER
        MULTIPLE_CHOICE -> StudySetType.MULTIPLE_CHOICE_BANK
    }
}

object PromptTemplateProvider {

    fun getQuickImportPrompt(type: PromptTemplateType): String = when (type) {
        PromptTemplateType.TERM_DEFINITION -> PromptTermDefContent
        PromptTemplateType.QUESTION_ANSWER -> PromptQAContent
        PromptTemplateType.MULTIPLE_CHOICE -> PromptMCQContent
    }

    const val PromptTermDefContent = """Hãy chuyển nội dung tôi gửi thành đúng format để nhập vào app học.

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

    const val PromptQAContent = """Hãy chuyển nội dung tôi gửi thành đúng format để nhập vào app học.

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

    const val PromptMCQContent = """Hãy chuyển nội dung tôi gửi thành đúng format để nhập vào app học.

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
}
