package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLocale
import com.example.data.model.SelectedLocationState
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
    val selectedLocation: SelectedLocationState? = null,
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LOCALE = stringPreferencesKey("locale")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val WORKSPACE = stringPreferencesKey("workspace_mode")
        val ACTIVE_PERSON_ID = stringPreferencesKey("active_person_id")
        val HAS_ENTERED = booleanPreferencesKey("has_entered_workspace")
        val SELECTED_LOC_NAME = stringPreferencesKey("selected_loc_name")
        val SELECTED_LOC_DISTRICT = stringPreferencesKey("selected_loc_district")
        val SELECTED_LOC_PROVINCE = stringPreferencesKey("selected_loc_province")
        val SELECTED_LOC_LAT = doublePreferencesKey("selected_loc_lat")
        val SELECTED_LOC_LNG = doublePreferencesKey("selected_loc_lng")
        val SELECTED_LOC_CUSTOM = booleanPreferencesKey("selected_loc_custom")
    }

    val preferencesFlow: Flow<TibbUserPreferences> = context.tibbPrefsStore.data.map { prefs ->
        val locName = prefs[Keys.SELECTED_LOC_NAME]
        val locDistrict = prefs[Keys.SELECTED_LOC_DISTRICT]
        val locLat = prefs[Keys.SELECTED_LOC_LAT]
        val locLng = prefs[Keys.SELECTED_LOC_LNG]
        val selectedLocation = if (!locName.isNullOrBlank() && !locDistrict.isNullOrBlank() && locLat != null && locLng != null) {
            SelectedLocationState(
                name = locName,
                district = locDistrict,
                province = prefs[Keys.SELECTED_LOC_PROVINCE].orEmpty(),
                lat = locLat,
                lng = locLng,
                isCustomPin = prefs[Keys.SELECTED_LOC_CUSTOM] ?: false,
            )
        } else null

        TibbUserPreferences(
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            locale = AppLocale.entries.firstOrNull { it.code == prefs[Keys.LOCALE] } ?: AppLocale.EN,
            unitSystem = if (prefs[Keys.UNIT_SYSTEM] == UnitSystem.IMPERIAL.name) UnitSystem.IMPERIAL else UnitSystem.METRIC,
            workspaceMode = if (prefs[Keys.WORKSPACE] == WorkspaceMode.BHU.name) WorkspaceMode.BHU else WorkspaceMode.GHARANA,
            activePersonId = prefs[Keys.ACTIVE_PERSON_ID],
            hasEnteredWorkspace = prefs[Keys.HAS_ENTERED] ?: false,
            selectedLocation = selectedLocation,
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

    suspend fun setSelectedLocation(location: SelectedLocationState?) {
        context.tibbPrefsStore.edit { prefs ->
            if (location == null) {
                prefs.remove(Keys.SELECTED_LOC_NAME)
                prefs.remove(Keys.SELECTED_LOC_DISTRICT)
                prefs.remove(Keys.SELECTED_LOC_PROVINCE)
                prefs.remove(Keys.SELECTED_LOC_LAT)
                prefs.remove(Keys.SELECTED_LOC_LNG)
                prefs.remove(Keys.SELECTED_LOC_CUSTOM)
            } else {
                prefs[Keys.SELECTED_LOC_NAME] = location.name
                prefs[Keys.SELECTED_LOC_DISTRICT] = location.district
                prefs[Keys.SELECTED_LOC_PROVINCE] = location.province
                prefs[Keys.SELECTED_LOC_LAT] = location.lat
                prefs[Keys.SELECTED_LOC_LNG] = location.lng
                prefs[Keys.SELECTED_LOC_CUSTOM] = location.isCustomPin
            }
        }
    }
}
