package com.ivanlee.sesh.data.calendar

import com.ivanlee.sesh.data.db.entity.SessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarRepository @Inject constructor(
    private val authManager: CalendarAuthManager
) {
    companion object {
        private const val CALENDAR_API_URL =
            "https://www.googleapis.com/calendar/v3/calendars/primary/events"
    }

    private fun buildEventJson(session: SessionEntity, categoryName: String?): JSONObject {
        val summary = if (session.title.isNotEmpty()) {
            "Focus: ${session.title}"
        } else {
            "Focus Session"
        }

        val descriptionParts = mutableListOf<String>()
        if (categoryName != null) {
            descriptionParts.add("Category: $categoryName")
        }
        descriptionParts.add("Duration: ${session.actualSeconds / 60}m")
        descriptionParts.add("Type: ${session.sessionType}")
        if (session.overflowSeconds > 0) {
            descriptionParts.add("Overflow: ${session.overflowSeconds / 60}m")
        }
        if (session.pauseSeconds > 0) {
            descriptionParts.add("Paused: ${session.pauseSeconds / 60}m")
        }
        if (!session.notes.isNullOrBlank()) {
            descriptionParts.add("")
            descriptionParts.add("Notes: ${session.notes}")
        }

        return JSONObject().apply {
            put("summary", summary)
            put("description", descriptionParts.joinToString("\n"))
            put("start", JSONObject().apply {
                put("dateTime", session.startedAt)
                put("timeZone", java.util.TimeZone.getDefault().id)
            })
            put("end", JSONObject().apply {
                put("dateTime", session.endedAt)
                put("timeZone", java.util.TimeZone.getDefault().id)
            })
        }
    }

    /**
     * Creates a Google Calendar event for a completed session.
     * Returns the event ID if created successfully, null otherwise.
     */
    suspend fun createEvent(session: SessionEntity, categoryName: String?): String? {
        val accessToken = authManager.getValidAccessToken() ?: return null
        val event = buildEventJson(session, categoryName)

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(CALENDAR_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(event.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    JSONObject(responseBody).optString("id").takeIf { it.isNotBlank() }
                } else {
                    connection.disconnect()
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Updates an existing Google Calendar event.
     * Returns true if updated successfully.
     */
    suspend fun updateEvent(eventId: String, session: SessionEntity, categoryName: String?): Boolean {
        val accessToken = authManager.getValidAccessToken() ?: return false
        val event = buildEventJson(session, categoryName)

        return withContext(Dispatchers.IO) {
            try {
                val encodedEventId = URLEncoder.encode(eventId, Charsets.UTF_8.name())
                val url = URL("$CALENDAR_API_URL/$encodedEventId")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "PUT"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 15_000
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(event.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                connection.disconnect()
                responseCode in 200..299
            } catch (_: Exception) {
                false
            }
        }
    }
}
