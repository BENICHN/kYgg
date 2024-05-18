package fr.benichn.kygg.types

data class User(
    val url: String,
    val avatarUrl: String,
    val name: String,
    val role: String,
    val upSize: Long,
    val downSize: Long
)