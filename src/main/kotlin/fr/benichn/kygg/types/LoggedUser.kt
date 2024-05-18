package fr.benichn.kygg.types

data class LoggedUser(
    val upSize: Long,
    val downSize: Long,
    val ratio: Float,
    val isActive: Boolean
)