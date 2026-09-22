package com.bexmarket.ng.app

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.appwrite.ID
import io.appwrite.enums.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val businessName: String = "",
    val username: String = "",
    val locationState: String = "Lagos",
    val locationAxis: String = "Mainland",
    val profilePic: String? = null,
    val isVerified: Boolean = false,
    val subscriptionExpiresAt: String? = null
)

sealed class AuthStatus {
    object Idle : AuthStatus()
    object Loading : AuthStatus()
    object Authenticated : AuthStatus()
    object Unauthenticated : AuthStatus()
    data class Error(val message: String) : AuthStatus()
}

class AuthViewModel : ViewModel() {
    // Use getters to avoid UninitializedPropertyAccessException if accessed during init
    private val account get() = AppwriteProvider.account
    private val databases get() = AppwriteProvider.databases
    private val storage get() = AppwriteProvider.storage

    private val DATABASE_ID = "6a4d8f75003a9cd90f61"
    private val USERS_COLLECTION_ID = "users"
    private val BUCKET_ID = "6a4dce2200029a992eb4" // Using the same bucket as products for now

    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Idle)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private var userIdForPhone: String = ""

    init {
        checkSession()
    }

    /**
     * Checks if a user has an active session. 
     * Call this on app startup to keep the user logged in.
     */
    fun checkSession() {
        viewModelScope.launch {
            if (_authStatus.value == AuthStatus.Authenticated) {
                Log.d("AuthViewModel", "Already authenticated, skipping check")
                return@launch
            }
            
            Log.d("AuthViewModel", "Checking session... Current Status: ${_authStatus.value}")
            _authStatus.value = AuthStatus.Loading
            try {
                // Perform network call on IO dispatcher
                val session = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(5000) {
                        account.getSession("current")
                    }
                }
                
                if (session != null) {
                    Log.d("AuthViewModel", "Session found: ${session.id}")
                    val user = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        account.get()
                    }
                    Log.d("AuthViewModel", "User data retrieved: ${user.id} (${user.email})")
                    fetchUserProfile(user.id, user.email, user.name)
                    _authStatus.value = AuthStatus.Authenticated
                } else {
                    Log.d("AuthViewModel", "No active session found (timeout or null)")
                    _authStatus.value = AuthStatus.Unauthenticated
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking session: ${e.message}", e)
                _authStatus.value = AuthStatus.Unauthenticated
                _currentUser.value = null
            }
        }
    }

    private suspend fun fetchUserProfile(userId: String, email: String, name: String) {
        try {
            Log.d("AppwriteDebug", "Fetching profile for userId: $userId")
            val user = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                account.get()
            }
            val prefsData = user.prefs.data
            val profilePicUrl = prefsData["profilePicUrl"] as? String ?: ""
            val businessName = prefsData["businessName"] as? String ?: ""
            val username = prefsData["username"] as? String ?: ""
            val phone = prefsData["phone"] as? String ?: user.phone
            val whatsapp = prefsData["whatsapp"] as? String ?: ""
            val locationState = prefsData["locationState"] as? String ?: "Lagos"
            val locationAxis = prefsData["locationAxis"] as? String ?: "Mainland"
            val isVerified = prefsData["isVerified"] as? Boolean ?: false
            val subscriptionExpiresAt = prefsData["subscriptionExpiresAt"] as? String
            
            _currentUser.value = UserProfile(
                uid = userId,
                name = name,
                email = email,
                phone = phone,
                whatsapp = whatsapp,
                businessName = businessName,
                username = username,
                locationState = locationState,
                locationAxis = locationAxis,
                profilePic = profilePicUrl.ifEmpty { null },
                isVerified = isVerified,
                subscriptionExpiresAt = subscriptionExpiresAt
            )
            Log.d("AppwriteDebug", "Profile loaded from prefs. Phone: $phone, WhatsApp: $whatsapp")
        } catch (e: Exception) {
            Log.e("AppwriteDebug", "Error fetching profile: ${e.message}", e)
            _currentUser.value = UserProfile(uid = userId, name = name, email = email)
        }
    }

    fun updateProfilePicture(context: android.content.Context, uri: android.net.Uri?) {
        val userProfile = _currentUser.value ?: return
        viewModelScope.launch {
            _authStatus.value = AuthStatus.Loading
            try {
                val fullUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (uri == null) {
                        account.updatePrefs(prefs = mapOf("profilePicUrl" to ""))
                        ""
                    } else {
                        val file = java.io.File(context.cacheDir, "profile_${userProfile.uid}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(file).use { output -> input.copyTo(output) }
                        }

                        val appwriteFile = storage.createFile(
                            bucketId = BUCKET_ID,
                            fileId = io.appwrite.ID.unique(),
                            file = io.appwrite.models.InputFile.fromPath(file.absolutePath)
                        )
                        
                        val url = AppwriteProvider.repository.getFullImageUrl(appwriteFile.id)
                        account.updatePrefs(prefs = mapOf("profilePicUrl" to url))
                        
                        if (file.exists()) file.delete()
                        url
                    }
                }
                
                // Sync with Users collection
                syncUserToCollection(userProfile.uid, mapOf("profilePicUrl" to fullUrl))
                
                checkSession() // Refresh user profile
            } catch (e: Exception) {
                Log.e("AppwriteDebug", "Profile pic update failed: ${e.message}", e)
                _authStatus.value = AuthStatus.Error("Failed to update profile picture")
            }
        }
    }

    fun addSubscriptionDays(days: Int, onResult: (Boolean, String?) -> Unit) {
        val userProfile = _currentUser.value
        if (userProfile == null) {
            onResult(false, "User profile not loaded")
            return
        }
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US)
                    val currentExpiryStr = userProfile.subscriptionExpiresAt
                    val currentExpiry = if (!currentExpiryStr.isNullOrEmpty()) {
                        try { sdf.parse(currentExpiryStr) } catch (e: Exception) { java.util.Date() }
                    } else {
                        java.util.Date()
                    }
                    
                    val baseDate = if (currentExpiry.after(java.util.Date())) currentExpiry else java.util.Date()
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = baseDate
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, days)
                    val newExpiryStr = sdf.format(calendar.time)

                    // Get current user preferences to preserve them
                    val user = account.get()
                    val currentPrefs = user.prefs.data.toMutableMap()
                    currentPrefs["subscriptionExpiresAt"] = newExpiryStr

                    // Update prefs and sync to collection
                    account.updatePrefs(prefs = currentPrefs)
                    syncUserToCollection(userProfile.uid, mapOf("subscriptionExpiresAt" to newExpiryStr))
                }
                checkSession()
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update subscription", e)
                onResult(false, e.localizedMessage ?: "Failed to update subscription")
            }
        }
    }

    private suspend fun syncUserToCollection(userId: String, data: Map<String, Any>) {
        try {
            // Try to update existing doc
            databases.updateDocument(
                databaseId = DATABASE_ID,
                collectionId = USERS_COLLECTION_ID,
                documentId = userId,
                data = data
            )
        } catch (e: Exception) {
            // If not found, create it
            try {
                databases.createDocument(
                    databaseId = DATABASE_ID,
                    collectionId = USERS_COLLECTION_ID,
                    documentId = userId,
                    data = data
                )
            } catch (innerE: Exception) {
                Log.e("AuthViewModel", "Sync to collection failed: ${innerE.message}")
            }
        }
    }

    fun signUp(email: String, password: String, name: String, businessName: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting signUp for: $email")
            _authStatus.value = AuthStatus.Loading
            try {
                // Use IO dispatcher for all network calls
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // 1. Create Account
                    val user = account.create(
                        userId = ID.unique(),
                        email = email,
                        password = password,
                        name = name
                    )
                    Log.d("AuthViewModel", "Account created: ${user.id}")
                    
                    // 2. Log in to get a session (needed to update prefs)
                    account.createEmailPasswordSession(email, password)
                    
                    // 3. Store Username/Business Name in User Prefs
                    account.updatePrefs(
                        prefs = mapOf(
                            "username" to name,
                            "businessName" to businessName
                        )
                    )
                    Log.d("AuthViewModel", "User prefs updated")

                    // 4. Sync with Users collection
                    syncUserToCollection(
                        userId = user.id,
                        data = mapOf(
                            "name" to name,
                            "email" to email,
                            "businessName" to businessName,
                            "username" to name
                        )
                    )
                }

                checkSession() // Refresh state
            } catch (e: Exception) {
                Log.e("AuthViewModel", "SignUp failed", e)
                _authStatus.value = AuthStatus.Error(e.message ?: "Sign up failed. Please check your details.")
            }
        }
    }

    fun signUpWithUsername(username: String, password: String, businessName: String) {
        val tempEmail = "${username.trim().lowercase()}@bexmarket.ng"
        signUp(tempEmail, password, username, businessName)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting login for: $email")
            _authStatus.value = AuthStatus.Loading
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    account.createEmailPasswordSession(email, password)
                }
                Log.d("AuthViewModel", "Session created successfully")
                checkSession() // Refresh state and user info
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed: ${e.message}", e)
                _authStatus.value = AuthStatus.Error(e.message ?: "Invalid email or password")
            }
        }
    }

    fun loginWithUsername(username: String, password: String) {
        val tempEmail = "${username.trim().lowercase()}@bexmarket.ng"
        login(tempEmail, password)
    }

    fun loginWithGoogle(activity: ComponentActivity) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting Google OAuth2 session")
            _authStatus.value = AuthStatus.Loading
            try {
                account.createOAuth2Session(activity, OAuthProvider.GOOGLE)
                Log.d("AuthViewModel", "Google session callback initiated")
                // checkSession() will be called when user returns to app
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google login failed: ${e.message}")
                _authStatus.value = AuthStatus.Error(e.message ?: "Google Sign-in failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting logout")
            try {
                account.deleteSession("current")
                Log.d("AuthViewModel", "Session deleted from Appwrite server")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Note: Remote session deletion failed or was already gone: ${e.message}")
            } finally {
                // ALWAYS clear local state to avoid "auto-login" loop
                _authStatus.value = AuthStatus.Unauthenticated
                _currentUser.value = null
                Log.d("AuthViewModel", "Local session state cleared")
            }
        }
    }
}
