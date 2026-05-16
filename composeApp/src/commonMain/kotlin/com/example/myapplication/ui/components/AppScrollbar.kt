package com.example.myapplication.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
)

@Composable
expect fun AppScrollbar(
    state: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
)
