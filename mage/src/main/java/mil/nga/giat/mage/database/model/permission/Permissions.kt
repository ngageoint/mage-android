package mil.nga.giat.mage.database.model.permission

import java.io.Serializable

class Permissions : Serializable {
   var permissions = emptyList<Permission>()

   constructor()

   constructor(permissions: List<Permission>) : super() {
      this.permissions = permissions
   }

   companion object {
      private const val serialVersionUID = -1912604919150929355L
   }
}