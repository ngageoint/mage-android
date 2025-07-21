package mil.nga.giat.mage.login

import android.util.Log
import okhttp3.internal.toImmutableMap
import org.json.JSONObject
import java.util.TreeMap

enum class ServerAuthTypes {
    LOCAL,
    LDAP,
    OAUTH,
    OPENIDCONNECT,
    SAML
}

object ServerAuthTypesHelper {
    private val LOG_NAME = ServerAuthTypesHelper::class.java.name

    //determine available login options based on JSON from last "api" call
    fun getAvailableServerAuthTypesMap(authStrategiesJSON: JSONObject): Map<ServerAuthTypes, JSONObject> {
        //TreeMap will sort by ServerAuthTypes' natural order (declaration order)
        val recognizedAuthTypesMap: MutableMap<ServerAuthTypes, JSONObject> = TreeMap()

        val strategyKeys = authStrategiesJSON.keys()
        while (strategyKeys.hasNext()) {
            val strategyKey = strategyKeys.next()
            try {
                val strategyJSON = authStrategiesJSON[strategyKey] as JSONObject
                val strategyType = strategyJSON.optString("type")

                //attempt to match the "type" value from the JSON with the values in ServerAuthTypes, case insensitive
                val authType = ServerAuthTypes.entries.find { it.name.equals(strategyType, ignoreCase = true) }

                if (authType != null) {
                    recognizedAuthTypesMap.put(authType, strategyJSON)
                }

            } catch (e: Exception) {
                Log.e(LOG_NAME, "Error parsing authentication strategy", e)
            }
        }

        return recognizedAuthTypesMap.toImmutableMap()
    }
}