package mil.nga.giat.mage.network.feed

import mil.nga.giat.mage.database.model.feed.Feed
import mil.nga.giat.mage.database.model.feed.FeedContent
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FeedService {

    @GET("/api/events/{eventId}/feeds")
    suspend fun getFeeds(@Path("eventId") eventId: String): Response<List<Feed>>

    @POST("/api/events/{eventId}/feeds/{feedId}/content")
    suspend fun getFeedItems(@Path("eventId") eventId: String, @Path("feedId") feedId: String): Response<FeedContent>
}