package com.bexmarket.ng.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import io.appwrite.services.Messaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)
    private val account get() = AppwriteProvider.account
    private val databases get() = AppwriteProvider.databases

    val isDarkMode = themePreferences.isDarkMode
    val isNotificationsEnabled = themePreferences.notificationsEnabled

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    fun toggleTheme(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setNotificationsEnabled(enabled)
            try {
                if (enabled) {
                    val token = FirebaseMessaging.getInstance().token.await()
                    Log.d("SettingsViewModel", "FCM Token: $token")
                    
                    // Subscribe to 'new_products' topic
                    FirebaseMessaging.getInstance().subscribeToTopic("new_products").await()
                    Log.d("SettingsViewModel", "Subscribed to new_products topic")
                } else {
                    // Unsubscribe from 'new_products' topic
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("new_products").await()
                    Log.d("SettingsViewModel", "Unsubscribed from new_products topic")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Notification toggle error: ${e.message}")
            }
        }
    }

    fun saveProfileChanges(name: String, email: String, phone: String, whatsapp: String, locationState: String, locationAxis: String) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Loading
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // 1. Update Name in Appwrite Account
                    account.updateName(name)
                    
                    // 2. Update Metadata in User Preferences
                    account.updatePrefs(
                        prefs = mapOf(
                            "phone" to phone,
                            "whatsapp" to whatsapp,
                            "locationState" to locationState,
                            "locationAxis" to locationAxis,
                            "businessName" to name // Syncing name to businessName for consistency if needed
                        )
                    )

                    // 3. Update in Users Collection
                    val user = account.get()
                    databases.updateDocument(
                        databaseId = "6a4d8f75003a9cd90f61",
                        collectionId = "users",
                        documentId = user.id,
                        data = mapOf(
                            "name" to name,
                            "phone" to phone,
                            "whatsapp" to whatsapp,
                            "locationState" to locationState,
                            "locationAxis" to locationAxis,
                            "businessName" to name,
                            "email" to email
                        )
                    )
                }
                
                _saveStatus.value = SaveStatus.Success
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Save profile failed: ${e.message}", e)
                _saveStatus.value = SaveStatus.Error(e.message ?: "Failed to save changes")
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    sealed class SaveStatus {
        object Idle : SaveStatus()
        object Loading : SaveStatus()
        object Success : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }
}
