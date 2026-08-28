package com.burha.fundhelper.data.local

import com.burha.fundhelper.domain.FeeLine
import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot

object SnapshotMapper {
    fun toEntity(snapshot: FundSnapshot): SnapshotEntity = SnapshotEntity(
        code = snapshot.code,
        name = snapshot.name,
        kind = snapshot.kind.name,
        price = snapshot.price,
        priceDate = snapshot.priceDate,
        returnsJson = SnapshotJson.returnsToJson(snapshot.returns),
        fundType = snapshot.fundType,
        risk = snapshot.risk,
        feesJson = SnapshotJson.feesToJson(snapshot.fees.map { it.label to it.value }),
        fetchedAt = snapshot.fetchedAt,
        payCount = snapshot.payCount,
        prevPayCount = snapshot.prevPayCount,
        investorCount = snapshot.investorCount,
        prevInvestorCount = snapshot.prevInvestorCount,
    )

    fun toDomain(entity: SnapshotEntity): FundSnapshot = FundSnapshot(
        code = entity.code,
        name = entity.name,
        kind = FundKind.valueOf(entity.kind),
        price = entity.price,
        priceDate = entity.priceDate,
        returns = SnapshotJson.returnsFromJson(entity.returnsJson),
        fundType = entity.fundType,
        risk = entity.risk,
        fees = SnapshotJson.feesFromJson(entity.feesJson).map { FeeLine(it.first, it.second) },
        fetchedAt = entity.fetchedAt,
        payCount = entity.payCount,
        prevPayCount = entity.prevPayCount,
        investorCount = entity.investorCount,
        prevInvestorCount = entity.prevInvestorCount,
    )
}
