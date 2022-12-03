package top.limbang.entity

import kotlinx.serialization.Serializable

@Serializable
data class UserMessage(
    val name: String,
    val message: String
)