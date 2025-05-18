package com.jetbrains.kmpapp.data

class FirebaseServiceStub : FirebaseService {
    override suspend fun signIn(email: String, password: String): Result<String> =
        Result.failure(UnsupportedOperationException("Firebase not implemented on iOS yet"))

    override suspend fun signUp(email: String, password: String, displayName: String, username: String): Result<String> =
        Result.failure(UnsupportedOperationException("Firebase not implemented on iOS yet"))

    override suspend fun signInAnonymously(): Result<String> =
        Result.failure(UnsupportedOperationException("Firebase not implemented on iOS yet"))

    override suspend fun signOut() {}

    override fun isUserSignedIn(): Boolean = false

    override fun getCurrentUserId(): String? = null

    override suspend fun getUserData(userId: String): Result<UserData?> =
        Result.success(null)

    override suspend fun updateUserProfile(userData: UserData): Result<Unit> =
        Result.failure(UnsupportedOperationException("Firebase not implemented on iOS yet"))

    override fun isCurrentUserAnonymous(): Boolean = true

    override suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit> =
        Result.failure(UnsupportedOperationException("Firebase not implemented on iOS yet"))

    override suspend fun getCurrentUserRole(): Result<UserRole> =
        Result.success(UserRole.USER)

    override suspend fun getAllUsers(): Result<List<UserData>> =
        Result.success(emptyList())

    override fun isManufacturer(): Boolean = false

    override fun isAdmin(): Boolean = false
}

actual fun firebaseService(): FirebaseService = FirebaseServiceStub()