package com.jetbrains.kmpapp.data

interface FirebaseService {
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String, displayName: String, username: String): Result<String>
    suspend fun signInAnonymously(): Result<String>
    suspend fun signOut()
    fun isUserSignedIn(): Boolean
    fun getCurrentUserId(): String?
    suspend fun getUserData(userId: String): Result<UserData?>
    suspend fun updateUserProfile(userData: UserData): Result<Unit>
    fun isCurrentUserAnonymous(): Boolean

    suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit>
    suspend fun getCurrentUserRole(): Result<UserRole>
    suspend fun getAllUsers(): Result<List<UserData>>

    
    fun isManufacturer(): Boolean
    fun isAdmin(): Boolean
}