package mil.nga.giat.mage.network.api

import mil.nga.giat.mage.sdk.datastore.user.Role
import retrofit2.Response
import retrofit2.http.GET

interface RoleService {
   @GET("/api/roles")
   suspend fun getRoles(): Response<Collection<Role>>
}