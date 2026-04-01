package ai.shieldtv.app.integration.debrid.realdebrid.repository

import org.json.JSONObject

object RealDebridInstantAvailabilityParser {
    fun parseCachedHashes(json: String): Set<String> {
        return runCatching {
            val root = JSONObject(json)
            val result = mutableSetOf<String>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val hash = keys.next()
                val value = root.optJSONObject(hash) ?: continue
                if (value.length() > 0) result += hash
            }
            result
        }.getOrElse { emptySet() }
    }
}
