package mil.nga.giat.mage.map

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

class MapAndViewProvider(
   private val mapFragment: SupportMapFragment
) : OnGlobalLayoutListener, OnMapReadyCallback {

   private lateinit var listener: OnMapAndViewReadyListener
   private val mapView: View? = mapFragment.view

   private var isViewReady = false
   private var isMapReady = false
   private var map: GoogleMap? = null

   interface OnMapAndViewReadyListener {
      fun onMapAndViewReady(googleMap: GoogleMap?)
   }

   fun getMapAndViewAsync(listener: OnMapAndViewReadyListener) {
      this.listener = listener

      if (mapView?.width != 0 && mapView?.height != 0) {
         isViewReady = true
      } else {
         mapView.viewTreeObserver.addOnGlobalLayoutListener(this)
      }

      mapFragment.getMapAsync(this)
   }

   override fun onMapReady(googleMap: GoogleMap?) {
      map = googleMap ?: return
      isMapReady = true
      checkReady()
   }

   override fun onGlobalLayout() {
      mapView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
      isViewReady = true
      checkReady()
   }

   private fun checkReady() {
      if (isViewReady && isMapReady) {
         listener.onMapAndViewReady(map)
      }
   }
}