package mil.nga.giat.mage.network.event

import mil.nga.giat.mage.database.model.event.Event
import retrofit2.Response
import retrofit2.http.GET

interface EventService {
   @GET("/api/events")
   suspend fun getEvents(): Response<List<Event>>
}