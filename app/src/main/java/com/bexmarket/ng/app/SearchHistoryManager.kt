package com.bexmarket.ng.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val HISTORY_KEY = "recent_searches"

    fun getHistory(): List<String> {
        val json = sharedPreferences.getString(HISTORY_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveSearch(query: String) {
        if (query.isBlank()) return
        
        val currentHistory = getHistory().toMutableList()
        
        // Remove if exists to move to top
        currentHistory.remove(query)
        
        // Add to top
        currentHistory.add(0, query)
        
        // Limit to 10 unique terms
        val limitedHistory = currentHistory.take(10)
        
        val json = gson.toJson(limitedHistory)
        sharedPreferences.edit().putString(HISTORY_KEY, json).apply()
    }

    fun clearHistory() {
        sharedPreferences.edit().remove(HISTORY_KEY).apply()
    }
}
