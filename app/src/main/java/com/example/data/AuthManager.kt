package com.example.data

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthManager {

    private const val TAG = "PaiseJarAuth"

    suspend fun attemptAutoSignIn(context: Context): FirebaseUser? {
        val current = Firebase.auth.currentUser
        if (current != null) return current

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = CredentialManager.create(context).getCredential(
                context = context,
                request = request,
            )
            exchangeGoogleCredentialForFirebaseUser(result)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            Log.d(TAG, "No authorized credential cached for silent sign-in.")
            null
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent sign-in cancelled by user.", e)
            null
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Silent sign-in skipped: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Silent sign-in unavailable: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogleInteractive(context: Context): Result<FirebaseUser> {
        return try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = context.getString(R.string.default_web_client_id),
            ).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = CredentialManager.create(context).getCredential(
                context = context,
                request = request,
            )
            val user = exchangeGoogleCredentialForFirebaseUser(result)
                ?: return Result.failure(IllegalStateException("Google Sign-In did not return an account."))
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "User cancelled Google Sign-In.", e)
            Result.failure(IllegalStateException("Sign-in cancelled. Tap below when you're ready to continue."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed.", e)
            Result.failure(IllegalStateException(e.message ?: "Google Sign-In is unavailable right now."))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sign-in error.", e)
            Result.failure(e)
        }
    }

    private suspend fun exchangeGoogleCredentialForFirebaseUser(
        result: GetCredentialResponse,
    ): FirebaseUser? {
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            return authResult.user
        }
        return null
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            Firebase.auth.signOut()
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: ClearCredentialException) {
                Log.w(TAG, "Failed to clear Credential Manager state on sign-out.", e)
            }
        }
    }

    suspend fun deleteFirebaseAuthAccount(context: Context): Result<Unit> {
        return withContext(NonCancellable) {
            try {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    user.delete().await()
                }
                Firebase.auth.signOut()
                runCatching {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Account deletion in Auth requires recent login or network.", e)
                Firebase.auth.signOut()
                runCatching {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                }
                Result.failure(e)
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    buttonText: String = "Continue with Google",
    onSignInSuccess: (FirebaseUser) -> Unit,
    onSignInError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (isLoading) return@Button
            isLoading = true
            scope.launch {
                val res = AuthManager.signInWithGoogleInteractive(context)
                isLoading = false
                res.fold(
                    onSuccess = { user -> onSignInSuccess(user) },
                    onFailure = { err -> onSignInError(err.message ?: "Google Sign-In failed.") },
                )
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("google_sign_in_button"),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(10.dp))
            Text("Signing in…")
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Login,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
