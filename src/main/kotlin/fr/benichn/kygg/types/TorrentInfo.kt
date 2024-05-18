package fr.benichn.kygg.types

import org.jsoup.nodes.Element

data class TorrentInfo(
    val searchResult: SearchResult,
    val hash: String,
    val files: FileTree,
    val nfo: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val description: Element,
    val comments: List<Comment>
)