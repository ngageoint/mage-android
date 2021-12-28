package mil.nga.giat.mage.map.feature

import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline

interface Mappable<T> {
   val feature: T
   var tag: Any?
   var visible: Boolean
   fun remove()
}

fun Marker.toMappable() = object : Mappable<Marker> {
   override val feature = this@toMappable
   override var tag = this@toMappable.tag
   override var visible = this@toMappable.isVisible
   override fun remove() = feature.remove()
}

fun Polyline.toMappable() = object : Mappable<Polyline> {
   override val feature = this@toMappable
   override var tag = this@toMappable.tag
   override var visible = this@toMappable.isVisible
   override fun remove() = feature.remove()

}

fun Polygon.toMappable() = object : Mappable<Polygon> {
   override val feature = this@toMappable
   override var tag = this@toMappable.tag
   override var visible = this@toMappable.isVisible
   override fun remove() = feature.remove()
}