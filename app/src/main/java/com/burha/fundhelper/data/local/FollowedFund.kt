package com.burha.fundhelper.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class FollowedFund(
    @Embedded val follow: FollowEntity,
    @Relation(parentColumn = "code", entityColumn = "code")
    val snapshot: SnapshotEntity?,
)
