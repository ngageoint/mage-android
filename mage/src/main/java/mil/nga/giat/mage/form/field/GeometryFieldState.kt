package mil.nga.giat.mage.form.field

import com.google.android.gms.maps.model.LatLng
import mil.nga.giat.mage.form.FormField
import mil.nga.giat.mage.observation.ObservationLocation

class GeometryFieldState(
  definition: FormField<ObservationLocation>,
  val defaultMapZoom: Float? = null,
  val defaultMapCenter: LatLng? = null
) : FieldState<ObservationLocation, FieldValue.Location>(
    definition,
    validator = ::isValid,
    errorFor = ::errorMessage,
    hasValue = ::hasValue
  )

private fun errorMessage(definition: FormField<ObservationLocation>, value: FieldValue.Location?): String {
  return "Please enter a value"
}

private fun isValid(definition: FormField<ObservationLocation>, value: FieldValue.Location?): Boolean {
  return !definition.required || value != null
}

private fun hasValue(value: FieldValue.Location?): Boolean {
  return value?.location != null
}