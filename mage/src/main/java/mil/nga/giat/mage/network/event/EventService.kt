package mil.nga.giat.mage.network.event

import mil.nga.giat.mage.database.model.event.Event
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface EventService {
   @GET("/api/events")
   suspend fun getEvents(): Response<List<Event>>

   @GET("/api/events/{eventId}")
   suspend fun getEvent(@Path("eventId") eventId: String): Response<Event>
}