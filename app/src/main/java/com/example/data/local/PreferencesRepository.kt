package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLocale
import com.example.data.model.UnitSystem
import com.example.data.model.WorkspaceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tibbPrefsStore by preferencesDataStore(name = "tibb_preferences")

data class TibbUserPreferences(
    val darkMode: Boolean = true,
    val locale: AppLocale = AppLocale.EN,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val workspaceMode: WorkspaceMode = WorkspaceMode.GHARANA,
    val activePersonId: String? = null,
    val hasEnteredWorkspace: Boolean = false,
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LOCALE = stringPreferencesKey("locale")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val WORKSPACE = stringPreferencesKey("workspace_mode")
        val ACTIVE_PERSON_ID = stringPreferencesKey("active_person_id")
        val HAS_ENTERED = booleanPreferencesKey("has_entered_workspace")
    }

    val preferencesFlow: Flow<TibbUserPreferences> = context.tibbPrefsStore.data.map { prefs ->
        TibbUserPreferences(
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            locale = AppLocale.entries.firstOrNull { it.code == prefs[Keys.LOCALE] } ?: AppLocale.EN,
            unitSystem = if (prefs[Keys.UNIT_SYSTEM] == UnitSystem.IMPERIAL.name) UnitSystem.IMPERIAL else UnitSystem.METRIC,
            workspaceMode = if (prefs[Keys.WORKSPACE] == WorkspaceMode.BHU.name) WorkspaceMode.BHU else WorkspaceMode.GHARANA,
            activePersonId = prefs[Keys.ACTIVE_PERSON_ID],
            hasEnteredWorkspace = prefs[Keys.HAS_ENTERED] ?: false,
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.tibbPrefsStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLocale(locale: AppLocale) {
        context.tibbPrefsStore.edit { it[Keys.LOCALE] = locale.code }
    }

    suspend fun setUnitSystem(unit: UnitSystem) {
        context.tibbPrefsStore.edit { it[Keys.UNIT_SYSTEM] = unit.name }
    }

    suspend fun setWorkspaceMode(mode: WorkspaceMode) {
        context.tibbPrefsStore.edit { it[Keys.WORKSPACE] = mode.name }
    }

    suspend fun setActivePersonId(personId: String?) {
        context.tibbPrefsStore.edit {
            if (personId == null) it.remove(Keys.ACTIVE_PERSON_ID)
            else it[Keys.ACTIVE_PERSON_ID] = personId
        }
    }

    suspend fun setHasEnteredWorkspace(entered: Boolean) {
        context.tibbPrefsStore.edit { it[Keys.HAS_ENTERED] = entered }
    }
}
