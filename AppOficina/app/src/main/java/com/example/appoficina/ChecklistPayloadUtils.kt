package com.example.appoficina

import org.json.JSONObject

object ChecklistPayloadUtils {
    fun resolveChecklist(sectionKey: String?, payload: JSONObject): JSONObject {
        val isPosto02 = sectionKey?.equals("posto02", ignoreCase = true) == true
        val embedded = if (isPosto02) {
            payload.optJSONObject("checklist") ?: payload
        } else {
            payload.optJSONObject("checklist")
        }

        return when {
            embedded == null -> payload
            embedded === payload -> payload
            else -> mergeChecklistPayload(embedded, payload, sectionKey)
        }
    }

    fun mergeChecklistPayload(
        primary: JSONObject,
        original: JSONObject,
        sectionKey: String?,
    ): JSONObject {
        val merged = JSONObject(primary.toString())

        fun copyPrimitive(key: String) {
            if (!merged.has(key)) {
                val value = original.opt(key)
                if (value != null && value != JSONObject.NULL) {
                    merged.put(key, value)
                }
            }
        }

        copyPrimitive("obra")
        copyPrimitive("ano")

        if (!merged.has("respondentes")) {
            original.optJSONObject("respondentes")?.let { merged.put("respondentes", it) }
        }

        if (!merged.has("itens")) {
            original.optJSONArray("itens")?.let { merged.put("itens", it) }
        }

        sectionKey?.let { key ->
            if (!merged.has(key)) {
                val directMatch = original.optJSONObject(key)
                if (directMatch != null) {
                    merged.put(key, directMatch)
                } else {
                    val iterator = original.keys()
                    while (iterator.hasNext()) {
                        val candidate = iterator.next()
                        if (candidate.equals(key, ignoreCase = true)) {
                            val section = original.optJSONObject(candidate)
                            if (section != null) {
                                merged.put(key, section)
                            }
                            break
                        }
                    }
                }
            }
        } ?: run {
            val iterator = original.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                if (merged.has(key)) {
                    continue
                }
                val section = original.optJSONObject(key) ?: continue
                if (section.optJSONArray("itens") != null) {
                    merged.put(key, section)
                }
            }
        }

        return merged
    }
}
