package com.burha.fundhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.burha.fundhelper.ui.theme.FundHelperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FundHelperTheme {
                SkeletonLabel()
            }
        }
    }
}

@Composable
private fun SkeletonLabel() {
    Text(text = "fund-helper")
}