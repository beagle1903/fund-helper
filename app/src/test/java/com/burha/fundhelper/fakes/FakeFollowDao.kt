package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.local.FollowDao
import com.burha.fundhelper.data.local.FollowEntity
import com.burha.fundhelper.data.local.FollowedFund
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class FakeFollowDao(
    private val snapshots: FakeSnapshotDao,
) : FollowDao {
    private val codes = MutableStateFlow<List<String>>(emptyList())

    override suspend fun insert(follow: FollowEntity) {
        codes.value = (codes.value + follow.code).distinct().sorted()
    }

    override suspend fun delete(code: String) {
        codes.value = codes.value.filterNot { it == code }
    }

    override suspend fun getCodes(): List<String> = codes.value

    override fun observeFollowed(): Flow<List<FollowedFund>> =
        combine(codes, snapshots.observeAll()) { followCodes, snapMap ->
            followCodes.map { code ->
                FollowedFund(
                    follow = FollowEntity(code),
                    snapshot = snapMap[code],
                )
            }
        }
}
