package mil.nga.giat.mage.sdk

class Compatibility {

   data class Server(val major: Int, val minor: Int)

   companion object {
      private val servers = listOf(
         Server(6, 0)
      )

      fun isCompatibleWith(major: Int, minor: Int): Boolean {
         for (server: Server in servers) {
            if (server.major == major && server.minor <= minor) {
               return true
            }
         }

         return false
      }
   }
}
