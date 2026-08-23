package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.local.SnapshotDao
import com.burha.fundhelper.data.local.SnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSnapshotDao : SnapshotDao {
    private val rows = MutableStateFlow<Map<String, SnapshotEntity>>(emptyMap())

    fun observeAll(): Flow<Map<String, SnapshotEntity>> = rows

    override suspend fun upsert(entity: SnapshotEntity) {
        rows.value = rows.value + (entity.code to entity)
    }

    override suspend fun upsertAll(entities: List<SnapshotEntity>) {
        rows.value = rows.value + entities.associateBy { it.code }
    }

    override fun observe(code: String): Flow<SnapshotEntity?> =
        rows.map { it[code] }

    override suspend fun get(code: String): SnapshotEntity? = rows.value[code]
}
