package com.sol.dopaminetrap

import android.content.Context

object OnboardingManager {
    private const val PREFS = "onboarding"
    private const val KEY_MODE = "device_mode"
    private const val KEY_FAMILY_ID = "family_id"
    private const val KEY_CHILD_ID = "child_id"
    private const val KEY_CHILD_NAME = "child_name"
    private const val KEY_DONE = "onboarding_done"
    private const val KEY_CHILD_AGE = "child_age"

    enum class DeviceMode { PARENT, CHILD }

    fun isOnboardingDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DONE, false)

    fun getMode(context: Context): DeviceMode? {
        val mode = prefs(context).getString(KEY_MODE, null) ?: return null
        return runCatching { DeviceMode.valueOf(mode) }.getOrNull()
    }

    fun getFamilyId(context: Context): String? =
        prefs(context).getString(KEY_FAMILY_ID, null)

    fun getChildId(context: Context): String? =
        prefs(context).getString(KEY_CHILD_ID, null)

    fun getChildName(context: Context): String? =
        prefs(context).getString(KEY_CHILD_NAME, null)

    fun getChildAge(context: Context): Int =
        prefs(context).getInt(KEY_CHILD_AGE, 13)

    fun completeOnboarding(
        context: Context,
        mode: DeviceMode,
        familyId: String,
        childId: String,
        childName: String,
        childAge: Int = 13
    ) {
        prefs(context).edit()
            .putBoolean(KEY_DONE, true)
            .putString(KEY_MODE, mode.name)
            .putString(KEY_FAMILY_ID, familyId)
            .putString(KEY_CHILD_ID, childId)
            .putString(KEY_CHILD_NAME, childName)
            .putInt(KEY_CHILD_AGE, childAge)
            .apply()
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
