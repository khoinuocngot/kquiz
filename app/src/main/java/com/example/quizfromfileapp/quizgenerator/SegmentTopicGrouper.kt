package com.example.quizfromfileapp.quizgenerator

import android.util.Log
import com.example.quizfromfileapp.domain.model.ContentSegment

/**
 * Gom nhóm segments theo topic/page để distractor chỉ lấy từ cùng nhóm.
 *
 * Chiến lược gom nhóm:
 * 1. Page-based grouping: segments cùng page/page range → cùng group
 * 2. Semantic clustering: nếu có heading nearby → cùng heading group
 * 3. Fallback: toàn bộ segments → 1 group lớn
 *
 * Mục đích: Khi sinh câu hỏi từ 1 segment, distractor chỉ lấy từ
 * cùng topic/page gần, tránh lấy bừa từ toàn bộ tài liệu gây
 * distractor quá dễ hoặc sai ngữ cảnh.
 */
object SegmentTopicGrouper {

    private const val TAG = "SegmentTopicGrouper"

    /**
     * Nhóm segments theo topic/page.
     *
     * @param segments Danh sách segments đã filter
     * @return Danh sách nhóm, mỗi nhóm có segments cùng page hoặc cùng semantic topic
     */
    fun groupByTopic(segments: List<ContentSegment>): List<SegmentTopicGroup> {
        if (segments.isEmpty()) return emptyList()

        val sorted = segments.sortedBy { it.sourcePageStart ?: 0 }

        // Gom theo page range
        val pageGroups = sorted.groupBy { seg ->
            seg.sourcePageStart ?: 0
        }

        val groups = mutableListOf<SegmentTopicGroup>()

        for ((pageKey, pageSegments) in pageGroups) {
            if (pageSegments.size >= 2) {
                val firstSeg = pageSegments.first()
                val pageStart = firstSeg.sourcePageStart ?: 0
                val pageEnd = firstSeg.sourcePageEnd ?: pageStart
                groups.add(
                    SegmentTopicGroup(
                        groupId = "page_$pageKey",
                        pageRange = pageStart to pageEnd,
                        segments = pageSegments,
                        topicHint = extractTopicHint(pageSegments)
                    )
                )
            } else {
                val firstSeg = pageSegments.first()
                val pageStart = firstSeg.sourcePageStart ?: 0
                val pageEnd = firstSeg.sourcePageEnd ?: pageStart
                if (groups.isNotEmpty()) {
                    val lastGroup = groups.last()
                    groups[groups.size - 1] = lastGroup.copy(
                        segments = lastGroup.segments + pageSegments
                    )
                } else {
                    groups.add(
                        SegmentTopicGroup(
                            groupId = "page_$pageKey",
                            pageRange = pageStart to pageEnd,
                            segments = pageSegments,
                            topicHint = extractTopicHint(pageSegments)
                        )
                    )
                }
            }
        }

        logd("groupByTopic: ${segments.size} segments → ${groups.size} groups")
        for (g in groups) {
            logd("  Group ${g.groupId}: ${g.segments.size} segments, pages ${g.pageRange}, hint='${g.topicHint.take(30)}'")
        }

        return groups
    }

    /**
     * Tìm nhóm chứa segment cụ thể.
     */
    fun findGroupOf(segment: ContentSegment, groups: List<SegmentTopicGroup>): SegmentTopicGroup? {
        return groups.find { seg -> seg.segments.any { it.id == segment.id } }
    }

    /**
     * Lấy segments cùng nhóm (trừ segment đang xét).
     * Dùng làm nguồn distractors.
     */
    fun getSameGroupSegments(
        segment: ContentSegment,
        groups: List<SegmentTopicGroup>
    ): List<ContentSegment> {
        val group = findGroupOf(segment, groups) ?: return emptyList()
        return group.segments.filter { it.id != segment.id }
    }

    /**
     * Lấy segment cùng page gần nhất (dùng làm nguồn distractor).
     */
    fun getNearbySegments(
        segment: ContentSegment,
        allSegments: List<ContentSegment>,
        maxCount: Int = 5
    ): List<ContentSegment> {
        val targetPage = segment.sourcePageStart ?: return emptyList()

        return allSegments
            .filter { it.id != segment.id }
            .map { seg ->
                val pageDist = kotlin.math.abs((seg.sourcePageStart ?: 0) - targetPage)
                pageDist to seg
            }
            .sortedBy { it.first }
            .take(maxCount)
            .map { it.second }
    }

    /**
     * Trích topic hint từ group segments (lấy từ đầu của segment đầu tiên).
     */
    private fun extractTopicHint(segments: List<ContentSegment>): String {
        if (segments.isEmpty()) return ""

        // Lấy 3 từ đầu của segment dài nhất trong group
        val longestSegment = segments.maxByOrNull { it.text.length } ?: segments.first()
        return longestSegment.text
            .split(Regex("""[\s]+"""))
            .take(4)
            .joinToString(" ")
            .take(50)
    }

    /**
     * Kiểm tra 2 segments có cùng topic không.
     */
    fun areSameTopic(seg1: ContentSegment, seg2: ContentSegment): Boolean {
        // Cùng page → cùng topic
        if (seg1.sourcePageStart != null && seg2.sourcePageStart != null) {
            if (seg1.sourcePageStart == seg2.sourcePageStart) return true
        }

        // So sánh token overlap
        val sim = SemanticSimilarityHelper.tokenBasedSimilarity(seg1.text, seg2.text)
        return sim > 0.40
    }

    private fun logd(message: String) {
        if (LlmGenerationConfig.DEBUG_LOGGING) {
            Log.d(TAG, message)
        }
    }
}

/**
 * Một nhóm segment cùng topic/page.
 *
 * @param groupId         ID nhóm (VD: "page_3")
 * @param pageRange       Khoảng trang (start, end)
 * @param segments        Danh sách segments trong nhóm
 * @param topicHint       Gợi ý topic (3-4 từ đầu)
 */
data class SegmentTopicGroup(
    val groupId: String,
    val pageRange: Pair<Int, Int>,
    val segments: List<ContentSegment>,
    val topicHint: String
)
