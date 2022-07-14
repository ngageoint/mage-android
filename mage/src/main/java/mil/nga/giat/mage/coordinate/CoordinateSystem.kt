package mil.nga.giat.mage.coordinate

enum class CoordinateSystem(val preferenceValue: Int) {
   WGS84(0), MGRS(1), DMS(2), GARS(3);

   companion object {
      fun fromPreference(preferenceValue: Int): CoordinateSystem {
         for (coordinateSystem in values()) {
            if (coordinateSystem.preferenceValue == preferenceValue) {
               return coordinateSystem
            }
         }

         return WGS84
      }
   }
}