package com.abhiram.innertube.models.response

import com.abhiram.innertube.models.SearchSuggestionsSectionRenderer
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsResponse(
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer,
    )
}
