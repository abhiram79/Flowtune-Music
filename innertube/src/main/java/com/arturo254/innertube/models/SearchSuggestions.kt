package com.abhiram.innertube.models

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<YTItem>,
)
