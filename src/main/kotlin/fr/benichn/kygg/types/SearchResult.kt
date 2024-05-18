package fr.benichn.kygg.types

import java.time.LocalDateTime

data class SearchResult(
    val category: Int,
    val relativeUrl: String,
    val id: Int,
    val title: String,
    val nComments: Int,
    val date: LocalDateTime,
    val size: Long,
    val nCompleted: Int,
    val nSeeders: Int,
    val nLeechers: Int
)