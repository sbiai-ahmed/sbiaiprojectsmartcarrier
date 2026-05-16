package com.example.myapplication.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppScrollbar(
    state: LazyListState,
    modifier: Modifier
) {
    // في أندرويد، التمرير الافتراضي يكفي عادةً
}

@Composable
actual fun AppScrollbar(
    state: androidx.compose.foundation.ScrollState,
    modifier: Modifier
) {
    // في أندرويد، التمرير الافتراضي يكفي عادةً
}
