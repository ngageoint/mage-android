package mil.nga.giat.mage.network.team

import mil.nga.giat.mage.database.model.team.Team
import mil.nga.giat.mage.network.user.UserWithRoleId
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface TeamService {

    @GET("/api/events/{eventId}/teams?populate=users")
    suspend fun getTeams(@Path("eventId") eventId: String?): Response<Map<Team, List<UserWithRoleId>>>

}