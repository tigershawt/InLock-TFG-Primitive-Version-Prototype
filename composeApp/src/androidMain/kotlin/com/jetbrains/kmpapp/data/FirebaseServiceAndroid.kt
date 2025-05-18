package com.jetbrains.kmpapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseServiceAndroid : FirebaseService {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val usersCollection by lazy { firestore.collection("users") }

    private var cachedUserData: UserData? = null
    private var cachedUserRole: UserRole? = null

    override suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val userId = suspendCoroutine { continuation ->
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: ""
                        continuation.resume(uid)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            try {
                val userData = getUserData(userId).getOrNull()
                if (userData != null) {
                    cachedUserData = userData
                    cachedUserRole = userData.role
                }
            } catch (_: Exception) { }

            return@withContext userId
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        username: String
    ): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val userId = suspendCoroutine<String> { continuation ->
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: ""
                        continuation.resume(uid)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            suspendCoroutine<Unit> { continuation ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                auth.currentUser?.updateProfile(profileUpdates)
                    ?.addOnSuccessListener {
                        continuation.resume(Unit)
                    }
                    ?.addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    } ?: continuation.resumeWithException(Exception("Current user is null"))
            }

            val userData = UserData(
                id = userId,
                email = email,
                displayName = displayName,
                username = username,
                role = UserRole.USER
            )

            suspendCoroutine<Unit> { continuation ->
                usersCollection.document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        cachedUserData = userData
                        cachedUserRole = userData.role
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            return@withContext userId
        }
    }

    override suspend fun signInAnonymously(): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                auth.signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        val userId = authResult.user?.uid ?: ""
                        continuation.resume(userId)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            cachedUserData = null
            cachedUserRole = null
            auth.signOut()
        }
    }

    override fun isUserSignedIn(): Boolean {
        return try {
            auth.currentUser != null
        } catch (_: Exception) {
            false
        }
    }

    override fun getCurrentUserId(): String? {
        return try {
            auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    override fun isCurrentUserAnonymous(): Boolean {
        return try {
            auth.currentUser?.isAnonymous ?: false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getUserData(userId: String): Result<UserData?> = runCatching {
        withContext(Dispatchers.IO) {
            if (cachedUserData != null && cachedUserData?.id == userId) {
                return@withContext cachedUserData
            }

            suspendCoroutine { continuation ->
                usersCollection.document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            try {
                                val userData = document.toObject(UserData::class.java)
                                if (userData != null) {
                                    cachedUserData = userData
                                    cachedUserRole = userData.role
                                }
                                continuation.resume(userData)
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            }
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        }
    }

    override suspend fun updateUserProfile(userData: UserData): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                usersCollection.document(userData.id)
                    .set(userData)
                    .addOnSuccessListener {
                        cachedUserData = userData
                        cachedUserRole = userData.role
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }
        }
    }

    override suspend fun updateUserRole(userId: String, role: UserRole): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserRole = getCurrentUserRole().getOrNull()
            if (currentUserRole != UserRole.ADMIN) {
                throw SecurityException("Only admins can update user roles")
            }

            val userSnapshot = usersCollection.document(userId).get().await()
            val userData = userSnapshot.toObject(UserData::class.java) ?:
            throw Exception("User not found")

            val updatedUserData = userData.copy(role = role)

            usersCollection.document(userId).set(updatedUserData).await()

            if (userData.id == getCurrentUserId()) {
                cachedUserData = updatedUserData
                cachedUserRole = role
            }
        }
    }

    override suspend fun getCurrentUserRole(): Result<UserRole> = runCatching {
        withContext(Dispatchers.IO) {
            if (cachedUserRole != null) {
                return@withContext cachedUserRole!!
            }

            val userId = getCurrentUserId() ?: throw Exception("No user is signed in")
            val userData = getUserData(userId).getOrNull() ?: throw Exception("User data not found")

            cachedUserRole = userData.role

            return@withContext userData.role
        }
    }

    override suspend fun getAllUsers(): Result<List<UserData>> = runCatching {
        withContext(Dispatchers.IO) {
            val currentUserRole = getCurrentUserRole().getOrNull()
            if (currentUserRole != UserRole.ADMIN) {
                throw SecurityException("Only admins can view all users")
            }

            val snapshot = usersCollection.get().await()
            return@withContext snapshot.documents.mapNotNull {
                it.toObject(UserData::class.java)
            }
        }
    }

    override fun isManufacturer(): Boolean {
        val userId = getCurrentUserId() ?: return false

        cachedUserRole?.let {
            return it == UserRole.MANUFACTURER || it == UserRole.ADMIN
        }

        if (cachedUserData != null && cachedUserData?.id == userId) {
            val role = cachedUserData?.role
            return role == UserRole.MANUFACTURER || role == UserRole.ADMIN
        }

        return try {
            val documentSnapshot = firestore
                .collection("users")
                .document(userId)
                .get(com.google.firebase.firestore.Source.CACHE)
                .isSuccessful

            if (documentSnapshot) {
                val cachedData = firestore
                    .collection("users")
                    .document(userId)
                    .get(com.google.firebase.firestore.Source.CACHE)
                    .result

                if (cachedData != null && cachedData.exists()) {
                    val userData = cachedData.toObject(UserData::class.java)
                    val role = userData?.role
                    cachedUserRole = role
                    cachedUserData = userData
                    role == UserRole.MANUFACTURER || role == UserRole.ADMIN
                } else {
                    false
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun isAdmin(): Boolean {
        val userId = getCurrentUserId() ?: return false

        cachedUserRole?.let {
            return it == UserRole.ADMIN
        }

        if (cachedUserData != null && cachedUserData?.id == userId) {
            return cachedUserData?.role == UserRole.ADMIN
        }

        return try {
            val documentSnapshot = firestore
                .collection("users")
                .document(userId)
                .get(com.google.firebase.firestore.Source.CACHE)
                .isSuccessful

            if (documentSnapshot) {
                val cachedData = firestore
                    .collection("users")
                    .document(userId)
                    .get(com.google.firebase.firestore.Source.CACHE)
                    .result

                if (cachedData != null && cachedData.exists()) {
                    val userData = cachedData.toObject(UserData::class.java)
                    val role = userData?.role
                    cachedUserRole = role
                    cachedUserData = userData
                    role == UserRole.ADMIN
                } else {
                    false
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}