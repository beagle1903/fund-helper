package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.FollowBackup

class FakeFollowBackup : FollowBackup {
    var codes: List<String> = emptyList()
    var writeCount: Int = 0
    var writeError: Boolean = false
    var readError: Boolean = false

    override suspend fun writeCodes(codes: List<String>) {
        writeCount += 1
        if (writeError) error("backup write failed")
        this.codes = codes
    }

    override suspend fun readCodes(): List<String> {
        if (readError) error("backup read failed")
        return codes
    }
}
