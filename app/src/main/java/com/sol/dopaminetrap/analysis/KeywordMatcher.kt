package com.sol.dopaminetrap.analysis

import com.sol.dopaminetrap.data.ConcernLevel
import com.sol.dopaminetrap.data.ContentCategory

object KeywordMatcher {

    fun analyze(text: String): List<ContentCategory> {
        if (text.isBlank()) return emptyList()
        val lower = text.lowercase()
        return ContentCategory.entries.filter { category ->
            category.keywords.any { keyword -> lower.contains(keyword) }
        }
    }

    fun maxConcernLevel(categories: List<ContentCategory>): ConcernLevel =
        categories.maxByOrNull { it.concernLevel.ordinal }?.concernLevel ?: ConcernLevel.NONE
}
