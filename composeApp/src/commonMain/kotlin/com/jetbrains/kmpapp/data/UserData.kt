package com.jetbrains.kmpapp.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val role: UserRole = UserRole.USER
)

@Serializable
enum class UserRole {
    USER,
    MANUFACTURER,
    ADMIN
}