package fr.benichn.kygg.types

import org.jsoup.nodes.Element

data class Comment(
    val user: User,
    val age: String,
    val content: Element
)