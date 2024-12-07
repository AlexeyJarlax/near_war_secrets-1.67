package com.pavlov.nearWarSecrets.theme.uiComponents

import android.R.attr.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pavlov.nearWarSecrets.theme.uiComponents.MatrixAnimationSettings.symbols
import kotlinx.coroutines.delay

@Composable
fun CustomCircularProgressIndicator() {
    val symbols = MatrixAnimationSettings.symbols
    val currentSymbol = remember { mutableStateOf(symbols.random()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentSymbol.value = symbols.random()
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(100.dp),
            color = Color(0xFF87E01A)
        )
        Text(
            text = currentSymbol.value.toString(), // Вывод значения символа
            color = Color(0xFF87E01A),
            style = MaterialTheme.typography.h4,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
