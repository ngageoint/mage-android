package mil.nga.giat.mage.sdk.preferences

import android.content.SharedPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

fun SharedPreferences.getFloatFlowForKey(keyForFloat: String) = callbackFlow<Float?> {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (keyForFloat == key) {
            trySend(getFloat(key, 0f))
        }
    }
    registerOnSharedPreferenceChangeListener(listener)
    if (contains(keyForFloat)) {
        send(getFloat(keyForFloat, 0f))
    } else {
        send(null)
    }
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.buffer(Channel.UNLIMITED)

fun SharedPreferences.getIntegerFlowForKey(keyForInt: String) = callbackFlow<Int?> {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (keyForInt == key) {
            trySend(getInt(key, 0))
        }
    }
    registerOnSharedPreferenceChangeListener(listener)
    if (contains(keyForInt)) {
        send(getInt(keyForInt, 0))
    } else {
        send(null)
    }
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.buffer(Channel.UNLIMITED)

fun SharedPreferences.getBooleanFlowForKey(keyForBoolean: String) = callbackFlow<Boolean?> {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (keyForBoolean == key) {
            trySend(getBoolean(key, false))
        }
    }
    registerOnSharedPreferenceChangeListener(listener)
    if (contains(keyForBoolean)) {
        send(getBoolean(keyForBoolean, false))
    } else {
        send(null)
    }
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.buffer(Channel.UNLIMITED)