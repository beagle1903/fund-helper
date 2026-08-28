package com.burha.fundhelper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val code: String,
    val name: String,
    val kind: String,
    val price: Double?,
    val priceDate: String?,
    val returnsJson: String,
    val fundType: String?,
    val risk: String?,
    val feesJson: String,
    val fetchedAt: Long,
    val payCount: Double? = null,
    val prevPayCount: Double? = null,
    val investorCount: Double? = null,
    val prevInvestorCount: Double? = null,
)
