package fr.benichn.kygg.types

data class SearchResults(
    val results: List<SearchResult>,
    val isEndOfSearch: Boolean
)