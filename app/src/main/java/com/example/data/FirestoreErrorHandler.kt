package com.example.data

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    GET,
    LIST,
    WRITE,
}

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?,
): Nothing {
    if (error is CancellationException) throw error
    val currentUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
    val payload = JSONObject().apply {
        put("error", error.message ?: error::class.java.simpleName)
        put("operationType", operationType.name.lowercase())
        put("path", path ?: JSONObject.NULL)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", currentUser?.uid ?: JSONObject.NULL)
                put("email", currentUser?.email ?: JSONObject.NULL)
                put("emailVerified", currentUser?.isEmailVerified ?: false)
                put("isAnonymous", currentUser?.isAnonymous ?: false)
                val providers = JSONArray()
                currentUser?.providerData?.forEach { info ->
                    providers.put(
                        JSONObject().apply {
                            put("providerId", info.providerId)
                            put("displayName", info.displayName ?: JSONObject.NULL)
                            put("email", info.email ?: JSONObject.NULL)
                        },
                    )
                }
                put("providerInfo", providers)
            },
        )
    }
    Log.e("FirestoreError", "Firestore Error: $payload", error)
    throw error
}

fun userFriendlyFirebaseMessage(error: Throwable): String {
    return when {
        error is FirebaseNetworkException ->
            "Network unavailable. Changes could not reach the server right now."
        error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Action blocked by security rules. Please verify your inputs or sign in again."
        error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Service temporarily unreachable. Check your internet connection and retry."
        else -> error.message ?: "Unable to complete request. Please try again."
    }
}
