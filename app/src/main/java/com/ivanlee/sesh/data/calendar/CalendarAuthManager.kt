package com.ivanlee.sesh.data.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ivanlee.sesh.R
import com.ivanlee.sesh.data.preferences.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class CalendarAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val authService = AuthorizationService(context)

    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token")
    )

    private val redirectUri = Uri.parse("com.ivanlee.sesh:/oauth2callback")

    private var authState: AuthState = AuthState()

    suspend fun initialize() {
        val json = preferencesRepository.googleAuthState.firstOrNull()
        if (json != null) {
            try {
                authState = AuthState.jsonDeserialize(json)
            } catch (_: Exception) {
                authState = AuthState()
            }
        }
    }

    fun getAuthorizationIntent(): Intent {
        val clientId = context.getString(R.string.google_client_id)
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
            .setScopes("openid", "https://www.googleapis.com/auth/calendar.events")
            .build()

        return authService.getAuthorizationRequestIntent(request)
    }

    suspend fun handleAuthorizationResponse(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        authState.update(response, exception)

        if (response != null) {
            // Exchange authorization code for tokens
            suspendCoroutine { continuation ->
                authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                    authState.update(tokenResponse, tokenException)
                    if (tokenException != null) {
                        continuation.resumeWithException(tokenException)
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }
        } else if (exception != null) {
            throw exception
        }

        // Persist auth state
        preferencesRepository.saveGoogleAuthState(authState.jsonSerializeString())
    }

    suspend fun getValidAccessToken(): String? {
        initialize()
        if (!authState.isAuthorized) return null

        return suspendCoroutine { continuation ->
            authState.performActionWithFreshTokens(authService) { accessToken, _, exception ->
                if (exception != null) {
                    continuation.resume(null)
                } else {
                    continuation.resume(accessToken)
                }
            }
        }
    }

    suspend fun isAuthorized(): Boolean {
        initialize()
        return authState.isAuthorized
    }

    suspend fun clearAuth() {
        authState = AuthState()
        preferencesRepository.clearGoogleAuth()
    }
}
