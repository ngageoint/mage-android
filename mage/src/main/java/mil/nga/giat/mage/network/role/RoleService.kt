package mil.nga.giat.mage.network.role

import mil.nga.giat.mage.database.model.permission.Role
import retrofit2.Response
import retrofit2.http.GET

interface RoleService {
   @GET("/api/roles")
   suspend fun getRoles(): Response<List<Role>>
}