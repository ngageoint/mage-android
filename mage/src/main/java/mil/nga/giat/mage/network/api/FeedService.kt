package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedContent
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FeedService {

    @GET("/api/events/{eventId}/feeds")
    suspend fun getFeeds(@Path("eventId") eventId: String): Response<Array<Feed>>

    @POST("/api/events/{eventId}/feeds/{feedId}/content")
    suspend fun getFeedItems(@Path("eventId") eventId: String, @Path("feedId") feedId: String): Response<FeedContent>
}