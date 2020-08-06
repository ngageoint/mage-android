package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.data.feed.Feed
import mil.nga.giat.mage.data.feed.FeedContent
import mil.nga.giat.mage.data.feed.FeedItem
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FeedService {

    @GET("/api/events/{eventId}/feeds")
    fun getFeeds(@Path("eventId") eventId: String): Call<Array<Feed>>

    @POST("/api/events/{eventId}/feeds/{feedId}/content")
    fun getFeedItems(@Path("eventId") eventId: String, @Path("feedId") feedId: String): Call<FeedContent>
}