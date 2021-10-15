package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.sdk.datastore.user.Team
import mil.nga.giat.mage.sdk.datastore.user.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface TeamService {

    @GET("/api/events/{eventId}/teams?populate=users")
    suspend fun getTeams(@Path("eventId") eventId: String?): Response<Map<Team, Collection<User>>>

}