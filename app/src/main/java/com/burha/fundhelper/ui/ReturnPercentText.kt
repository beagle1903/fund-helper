package com.burha.fundhelper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import com.burha.fundhelper.R
import com.burha.fundhelper.ui.theme.ReturnNegativeDark
import com.burha.fundhelper.ui.theme.ReturnNegativeLight
import com.burha.fundhelper.ui.theme.ReturnPositiveDark
import com.burha.fundhelper.ui.theme.ReturnPositiveLight

@Composable
fun ReturnPercentText(
    value: Double?,
    modifier: Modifier = Modifier,
    leading: String = "",
) {
    val dash = stringResource(R.string.price_missing)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val color = when (returnSign(value)) {
        ReturnSign.Positive -> if (dark) ReturnPositiveDark else ReturnPositiveLight
        ReturnSign.Negative -> if (dark) ReturnNegativeDark else ReturnNegativeLight
        ReturnSign.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val body = if (value == null) dash else "${formatNumber(value)}%"
    val text = if (value == null || leading.isEmpty()) body else "$leading $body"
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}
