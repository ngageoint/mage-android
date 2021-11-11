package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.sdk.datastore.user.Event
import mil.nga.giat.mage.sdk.datastore.user.Team
import retrofit2.Response
import retrofit2.http.GET

interface EventService {
   @GET("/api/events")
   suspend fun getEvents(): Response<Map<Event, Collection<Team>>>
}