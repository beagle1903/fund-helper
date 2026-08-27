package com.burha.fundhelper.data

interface FollowBackup {
    suspend fun writeCodes(codes: List<String>)
    suspend fun readCodes(): List<String>
}
