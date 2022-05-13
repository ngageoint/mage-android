package mil.nga.giat.mage.newsfeed

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.LandingViewModel
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateFormatter
import mil.nga.giat.mage.filter.ObservationFilterActivity
import mil.nga.giat.mage.location.LocationPolicy
import mil.nga.giat.mage.newsfeed.ObservationFeedViewModel.RefreshState
import mil.nga.giat.mage.newsfeed.ObservationListAdapter.ObservationActionListener
import mil.nga.giat.mage.observation.attachment.AttachmentGallery
import mil.nga.giat.mage.observation.attachment.AttachmentViewerActivity
import mil.nga.giat.mage.observation.ObservationLocation
import mil.nga.giat.mage.observation.attachment.AttachmentViewActivity
import mil.nga.giat.mage.observation.edit.ObservationEditActivity
import mil.nga.giat.mage.observation.view.ObservationViewActivity
import mil.nga.giat.mage.sdk.datastore.location.LocationHelper
import mil.nga.giat.mage.sdk.datastore.observation.Observation
import mil.nga.giat.mage.sdk.datastore.user.UserHelper
import mil.nga.giat.mage.utils.googleMapsUri
import javax.inject.Inject

@AndroidEntryPoint
class ObservationFeedFragment : Fragment() {
   @Inject
   lateinit var application: Application

   private val viewModel: ObservationFeedViewModel by activityViewModels()
   private val landingViewModel: LandingViewModel by activityViewModels()

   private lateinit var recyclerView: RecyclerView
   private lateinit var swipeContainer: SwipeRefreshLayout
   private lateinit var attachmentGallery: AttachmentGallery
   private var listState: Parcelable? = null

   @Inject
   lateinit var locationPolicy: LocationPolicy
   private lateinit var locationProvider: LiveData<Location?>

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      locationProvider = locationPolicy.bestLocationProvider
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      val view = inflater.inflate(R.layout.fragment_news_feed, container, false)

      setHasOptionsMenu(true)
      swipeContainer = view.findViewById(R.id.swipeContainer)
      swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200)
      swipeContainer.setOnRefreshListener { viewModel.refresh() }

      view.findViewById<View>(R.id.new_observation_button).setOnClickListener { onNewObservation() }

      attachmentGallery = AttachmentGallery(context, 200, 200)
      attachmentGallery.addOnAttachmentClickListener { attachment ->
         val intent = Intent(context, AttachmentViewActivity::class.java)
         intent.putExtra(AttachmentViewActivity.ATTACHMENT_ID_EXTRA, attachment.id)
         startActivity(intent)
      }

      recyclerView = view.findViewById(R.id.recycler_view)
      recyclerView.layoutManager = LinearLayoutManager(activity)
      recyclerView.itemAnimator = DefaultItemAnimator()

      view.findViewById<Button>(R.id.filter).setOnClickListener { filter() }

      return view
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      viewModel.observationFeedState.observe(viewLifecycleOwner) { feedState ->
         recyclerView.adapter = ObservationListAdapter(
            requireContext(),
            feedState,
            attachmentGallery,
            object : ObservationActionListener {
               override fun onObservationClick(observation: Observation) {
                  observationClick(observation)
               }

               override fun onObservationDirections(observation: Observation) {
                  observationDirections(observation)
               }

               override fun onObservationLocation(observation: Observation) {
                  observationLocation(observation)
               }
            })

         landingViewModel.setFilterText(feedState.filterText)

         val numberOfItems = recyclerView.adapter?.itemCount ?: 0
         view.findViewById<View>(R.id.recycler_view).visibility = if (numberOfItems > 1) View.VISIBLE else View.INVISIBLE
         view.findViewById<View>(R.id.no_content).visibility = if (numberOfItems > 1) View.GONE else View.VISIBLE
      }

      viewModel.refreshState.observe(viewLifecycleOwner) { state: RefreshState ->
         if (state === RefreshState.COMPLETE) {
            swipeContainer.isRefreshing = false
         }
      }
   }

   override fun onResume() {
      super.onResume()

      if (listState != null) {
         recyclerView.layoutManager?.onRestoreInstanceState(listState)
      }
   }

   override fun onPause() {
      super.onPause()

      listState = recyclerView.layoutManager?.onSaveInstanceState()
   }

   override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
      inflater.inflate(R.menu.filter, menu)
   }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
      return when (item.itemId) {
         R.id.filter_button -> {
            filter()
            true
         }
         else -> super.onOptionsItemSelected(item)
      }
   }

   override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
      when (requestCode) {
         PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               onNewObservation()
            }
         }
      }
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)

      if (requestCode == OBSERVATION_VIEW_REQUEST_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
               val resultType = data.getSerializableExtra(ObservationViewActivity.OBSERVATION_RESULT_TYPE) as ObservationViewActivity.ResultType
               onObservationResult(resultType, data)
            }
         }
      }
   }

   private fun filter() {
      val intent = Intent(activity, ObservationFilterActivity::class.java)
      startActivity(intent)
   }

   private fun onNewObservation() {
      val location = getLocation()
      if (!UserHelper.getInstance(context).isCurrentUserPartOfCurrentEvent) {
         AlertDialog.Builder(requireActivity(), R.style.AppCompatAlertDialogStyle)
            .setTitle(requireActivity().resources.getString(R.string.no_event_title))
            .setMessage(requireActivity().resources.getString(R.string.observation_no_event_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
      } else if (location != null) {
         val intent = Intent(activity, ObservationEditActivity::class.java)
         intent.putExtra(ObservationEditActivity.LOCATION, location)
         startActivity(intent)
      } else {
         if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder(requireActivity(), R.style.AppCompatAlertDialogStyle)
               .setTitle(requireActivity().resources.getString(R.string.location_missing_title))
               .setMessage(requireActivity().resources.getString(R.string.location_missing_message))
               .setPositiveButton(android.R.string.ok, null)
               .show()
         } else {
            AlertDialog.Builder(requireActivity(), R.style.AppCompatAlertDialogStyle)
               .setTitle(requireActivity().resources.getString(R.string.location_access_observation_title))
               .setMessage(requireActivity().resources.getString(R.string.location_access_observation_message))
               .setPositiveButton(android.R.string.ok) { _, _ ->
                  requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
               }
               .show()
         }
      }
   }

   private fun getLocation(): ObservationLocation? {
      var observationLocation: ObservationLocation? = null

      // if there is not a location from the location service, then try to pull one from the database.
      if (locationProvider.value == null) {
         val locations = LocationHelper.getInstance(context).getCurrentUserLocations(1, true)
         locations.firstOrNull()?.let { location ->
            val provider = location.propertiesMap["provider"]?.value?.toString() ?: ObservationLocation.MANUAL_PROVIDER

            observationLocation = ObservationLocation(provider, location.geometry)
            observationLocation?.time = location.timestamp.time
            location.propertiesMap["accuracy"]?.value?.toString()?.let {
               observationLocation?.accuracy = it.toFloat()
            }
         }
      } else {
         observationLocation = ObservationLocation(locationProvider.value)
      }
      return observationLocation
   }

   private fun observationDirections(observation: Observation) {
      AlertDialog.Builder(requireActivity())
         .setTitle(application.resources.getString(R.string.navigation_choice_title))
         .setItems(R.array.navigationOptions) { _: DialogInterface?, which: Int ->
            when (which) {
               0 -> {
                  val intent = Intent(Intent.ACTION_VIEW, observation.geometry.googleMapsUri())
                  startActivity(intent)
               }
               1 -> {
                  landingViewModel.startObservationNavigation(observation.id)
               }
            }
         }
         .setNegativeButton(android.R.string.cancel, null)
         .show()
   }

   private fun observationClick(observation: Observation) {
      val intent = Intent(context, ObservationViewActivity::class.java)
      intent.putExtra(ObservationViewActivity.OBSERVATION_ID_EXTRA, observation.id)
      startActivityForResult(intent, OBSERVATION_VIEW_REQUEST_CODE)
   }

   private fun observationLocation(observation: Observation) {
      val centroid = observation.geometry.centroid
      val coordinates = CoordinateFormatter(requireContext()).format(LatLng(centroid.y, centroid.x))
      val clipboard: ClipboardManager? = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      val clip = ClipData.newPlainText("Observation Location", coordinates)
      if (clipboard == null || clip == null) return
      clipboard.setPrimaryClip(clip)
      Snackbar.make(requireView(), R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
   }

   private fun onObservationResult(resultType: ObservationViewActivity.ResultType, intent: Intent) {
      if (resultType == ObservationViewActivity.ResultType.NAVIGATE) {
         val observationId = intent.getLongExtra(ObservationViewActivity.OBSERVATION_ID_EXTRA, -1)
         landingViewModel.startObservationNavigation(observationId)
      }
   }

   companion object {
      private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100
      private const val OBSERVATION_VIEW_REQUEST_CODE = 200
   }
}