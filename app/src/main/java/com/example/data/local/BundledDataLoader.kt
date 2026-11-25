package com.example.data.local

import android.content.Context
import com.example.data.model.DiseaseReferenceCard
import com.example.data.model.DrapBlocklist
import com.example.data.model.EmergencyFirstAidProtocol
import com.example.data.model.EmergencyRule
import com.example.data.model.FacilityRecord
import com.example.data.model.HomeCareProtocol
import com.example.data.model.KnowledgeMetadata
import com.example.data.model.MedicalConditionEntry
import com.example.data.model.SymptomTaxonomy
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class BundledDataLoader(
    private val context: Context? = null,
    private val assetOpener: ((String) -> String)? = null,
) {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun readAsset(assetPath: String): String {
        assetOpener?.let { return it(assetPath) }
        return requireNotNull(context) { "Context or assetOpener required" }
            .assets.open(assetPath).bufferedReader().use { it.readText() }
    }

    val facilities: List<FacilityRecord> by lazy {
        loadList("data/facilities.pk.json")
    }

    val whoHomeCare: List<HomeCareProtocol> by lazy {
        loadList("data/who_home_care.json")
    }

    val emergencyFirstAid: List<EmergencyFirstAidProtocol> by lazy {
        loadList("data/emergency_first_aid.json")
    }

    val diseaseReference: List<DiseaseReferenceCard> by lazy {
        loadList("data/disease_reference.json")
    }

    val drapBlocklist: DrapBlocklist by lazy {
        val json = readAsset("data/drap_blocklist.json")
        moshi.adapter(DrapBlocklist::class.java).fromJson(json)
            ?: DrapBlocklist(emptyList(), emptyList(), emptyList(), emptyList())
    }

    val knowledgeMetadata: KnowledgeMetadata by lazy {
        val json = readAsset("data/knowledge_metadata.json")
        requireNotNull(moshi.adapter(KnowledgeMetadata::class.java).fromJson(json)) {
            "Failed to parse data/knowledge_metadata.json"
        }
    }

    val symptomTaxonomy: SymptomTaxonomy by lazy {
        val json = readAsset("data/symptom_taxonomy.json")
        requireNotNull(moshi.adapter(SymptomTaxonomy::class.java).fromJson(json)) {
            "Failed to parse data/symptom_taxonomy.json"
        }
    }

    val emergencyRules: List<EmergencyRule> by lazy {
        loadList<EmergencyRule>("data/emergency_rules.json").sortedBy { it.priority }
    }

    val medicalConditions: List<MedicalConditionEntry> by lazy {
        loadList("data/medical_conditions.json")
    }

    private inline fun <reified T> loadList(assetPath: String): List<T> {
        val json = readAsset(assetPath)
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        return moshi.adapter<List<T>>(type).fromJson(json) ?: emptyList()
    }
}
