package com.burha.fundhelper

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/** Swaps FundHelperApp for Hilt's test application so @HiltAndroidTest can replace modules. */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, appName: String, context: Context): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
