package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Auth"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun isFirebaseReady(context: Context): Boolean {
    val appCtx = context.applicationContext
    val initialized = try {
        FirebaseApp.getApps(appCtx).isNotEmpty() ||
            FirebaseApp.initializeApp(appCtx) != null
    } catch (t: Throwable) {
        Log.w(TAG, "Firebase initialization unavailable: ${t.message}")
        false
    }
    return initialized && resolveWebClientId(appCtx) != null
}

fun safeCurrentFirebaseUser(context: Context): FirebaseUser? {
    if (!isFirebaseReady(context)) return null
    return runCatching { Firebase.auth.currentUser }.getOrNull()
}

private fun resolveWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId == 0) return null
    return runCatching { context.getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (!isFirebaseReady(context)) {
        onUnauthenticated()
        return
    }
    if (safeCurrentFirebaseUser(context) != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveWebClientId(context) ?: run {
        onUnauthenticated()
        return
    }
    val activity = context.findActivity() ?: context
    scope.launch {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                withTimeoutOrNull(3000L) {
                    runCatching { Firebase.auth.currentUser?.reload()?.await() }
                }
                withContext(Dispatchers.Main) { onAuthSuccess() }
            } else {
                withContext(Dispatchers.Main) { onUnauthenticated() }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            withContext(Dispatchers.Main) { onUnauthenticated() }
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    loginHint: String? = null,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String?) -> Unit = {}
) {
    if (!isFirebaseReady(context)) {
        onAuthError("Firebase is not configured in this build (missing google-services.json).")
        return
    }
    val clientId = resolveWebClientId(context) ?: run {
        onAuthError("Google Sign-In configuration missing: add google-services.json with default_web_client_id")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to launch Google Sign-In without an Activity context")
        return
    }
    scope.launch {
        try {
            val result = try {
                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
                credentialManager.getCredential(activity, request)
            } catch (e: GetCredentialCancellationException) {
                throw e
            } catch (primaryErr: GetCredentialException) {
                Log.w(TAG, "Primary SignInWithGoogle failed (${primaryErr.type}), trying GoogleId fallback", primaryErr)
                val fallbackOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption)
                    .build()
                credentialManager.getCredential(activity, fallbackRequest)
            }
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                withTimeoutOrNull(3000L) {
                    runCatching { Firebase.auth.currentUser?.reload()?.await() }
                }
                withContext(Dispatchers.Main) { onAuthSuccess() }
            } else {
                withContext(Dispatchers.Main) { onAuthError("Unexpected credential type") }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            withContext(Dispatchers.Main) { onAuthCancelled(e.message) }
        } catch (t: Throwable) {
            Log.w(TAG, "Credential Manager Google Sign-In failed (${t.message}), launching browser OAuth fallback", t)
            launchBrowserGoogleOAuth(
                activity = activity,
                loginHint = loginHint,
                onAuthSuccess = onAuthSuccess,
                onAuthError = onAuthError,
                onAuthCancelled = onAuthCancelled
            )
        }
    }
}

fun signInWithGoogleBrowser(
    context: Context,
    loginHint: String? = null,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String?) -> Unit = {}
) {
    if (!isFirebaseReady(context)) {
        onAuthError("Firebase is not configured in this build (missing google-services.json).")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to launch browser Google Sign-In without an Activity context")
        return
    }
    scope.launch {
        launchBrowserGoogleOAuth(
            activity = activity,
            loginHint = loginHint,
            onAuthSuccess = onAuthSuccess,
            onAuthError = onAuthError,
            onAuthCancelled = onAuthCancelled
        )
    }
}

suspend fun checkPendingBrowserSignIn(
    context: Context,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    if (!isFirebaseReady(context)) return
    try {
        val pending = Firebase.auth.pendingAuthResult ?: return
        pending.await()
        withTimeoutOrNull(3000L) {
            runCatching { Firebase.auth.currentUser?.reload()?.await() }
        }
        withContext(Dispatchers.Main) { onAuthSuccess() }
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Log.w(TAG, "Pending browser sign-in failed: ${t.message}", t)
        withContext(Dispatchers.Main) { onAuthError(t.localizedMessage ?: "Sign in failed") }
    }
}

private suspend fun launchBrowserGoogleOAuth(
    activity: Activity,
    loginHint: String?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: (String?) -> Unit
) {
    try {
        val pending = Firebase.auth.pendingAuthResult
        if (pending != null) {
            pending.await()
        } else {
            val builder = OAuthProvider.newBuilder("google.com")
            builder.addCustomParameter("prompt", "select_account")
            if (!loginHint.isNullOrBlank()) {
                builder.addCustomParameter("login_hint", loginHint.trim())
            }
            Firebase.auth.startActivityForSignInWithProvider(activity, builder.build()).await()
        }
        withTimeoutOrNull(3000L) {
            runCatching { Firebase.auth.currentUser?.reload()?.await() }
        }
        withContext(Dispatchers.Main) { onAuthSuccess() }
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        val msg = t.localizedMessage ?: "Sign in failed"
        if (msg.contains("cancel", ignoreCase = true)) {
            withContext(Dispatchers.Main) { onAuthCancelled(msg) }
        } else {
            Log.e(TAG, "Browser Google OAuth failed", t)
            withContext(Dispatchers.Main) { onAuthError(msg) }
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to clear credential state: ${t.message}")
            } finally {
                if (isFirebaseReady(context)) {
                    runCatching { Firebase.auth.signOut() }
                }
                onSignOutComplete()
            }
        }
    }
}
