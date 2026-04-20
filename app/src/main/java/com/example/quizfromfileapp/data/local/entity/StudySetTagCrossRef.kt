package com.example.quizfromfileapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table cho quan hệ N-N giữa StudySet và Tag.
 *
 * @param studySetId ID của StudySet
 * @param tagId      ID của Tag
 */
@Entity(
    tableName = "study_set_tags",
    primaryKeys = ["studySetId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = StudySetEntityRoom::class,
            parentColumns = ["id"],
            childColumns = ["studySetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studySetId"), Index("tagId")]
)
data class StudySetTagCrossRef(
    val studySetId: Long,
    val tagId: Long
)
