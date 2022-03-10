package mil.nga.giat.mage.newsfeed

import android.app.Application
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.LandingViewModel
import mil.nga.giat.mage.R
import mil.nga.giat.mage.filter.LocationFilterActivity
import mil.nga.giat.mage.map.annotation.MapAnnotation
import mil.nga.giat.mage.profile.ProfileActivity
import mil.nga.giat.mage.sdk.datastore.location.Location
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.utils.googleMapsUri
import javax.inject.Inject

@AndroidEntryPoint
class UserFeedFragment : Fragment() {
   @Inject
   lateinit var application: Application

   private val viewModel: UserFeedViewModel by activityViewModels()
   private val landingViewModel: LandingViewModel by activityViewModels()

   private lateinit var recyclerView: RecyclerView
   private lateinit var swipeContainer: SwipeRefreshLayout

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      viewModel.userFeedState.observe(viewLifecycleOwner, { userFeedState: UserFeedState ->
         recyclerView.adapter = UserListAdapter(
            requireContext(),
            userFeedState,
            userAction = { action ->
               onUserAction(action)
            },
            userClickListener = { user ->
               onUserClick(user)
            }
         )

         (activity as AppCompatActivity?)?.supportActionBar?.subtitle = userFeedState.filterText
      })

      viewModel.refreshState.observe(viewLifecycleOwner, { state: RefreshState ->
         if (state === RefreshState.COMPLETE) {
            swipeContainer.isRefreshing = false
         }
      })
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
      val view = inflater.inflate(R.layout.fragment_feed_people, container, false)
      setHasOptionsMenu(true)

      swipeContainer = view.findViewById(R.id.swipeContainer)
      swipeContainer.setColorSchemeResources(R.color.md_blue_600, R.color.md_orange_A200)
      swipeContainer.setOnRefreshListener { viewModel.refresh() }

      recyclerView = view.findViewById(R.id.recycler_view)
      recyclerView.layoutManager = LinearLayoutManager(activity)
      recyclerView.itemAnimator = DefaultItemAnimator()

      return view
   }

   override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
      inflater.inflate(R.menu.filter, menu)
   }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
      return when (item.itemId) {
         R.id.filter_button -> {
            val intent = Intent(context, LocationFilterActivity::class.java)
            startActivity(intent)
            true
         }
         else -> super.onOptionsItemSelected(item)
      }
   }

   private fun onUserClick(user: User) {
      val profileView = Intent(context, ProfileActivity::class.java)
      profileView.putExtra(ProfileActivity.USER_ID, user.id)
      activity?.startActivity(profileView)
   }

   private fun onUserAction(action: UserAction) {
      when (action) {
         is UserAction.Coordinates -> onUserLocation(action.location)
         is UserAction.Directions -> onUserDirections(action.user, action.location)
         is UserAction.Email -> onUserEmail(action.email)
         is UserAction.Phone -> onUserPhone(action.phone)
      }
   }

   private fun onUserDirections(user: User, location: Location) {
      val icon = MapAnnotation.fromUser(user, location)

      AlertDialog.Builder(requireActivity())
         .setTitle(application.resources.getString(R.string.navigation_choice_title))
         .setItems(R.array.navigationOptions) { _: DialogInterface?, which: Int ->
            when (which) {
               0 -> {
                  val intent = Intent(Intent.ACTION_VIEW, location.geometry.googleMapsUri())
                  startActivity(intent)
               }
               1 -> {
                  landingViewModel.startNavigation(
                     LandingViewModel.Navigable(
                        user.id.toString(),
                        LandingViewModel.NavigableType.OBSERVATION,
                        location.geometry,
                        icon
                     )
                  )
               }
            }
         }
         .setNegativeButton(android.R.string.cancel, null)
         .show()
   }

   private fun onUserPhone(phone: String) {
      val callIntent = Intent(Intent.ACTION_DIAL)
      callIntent.data = Uri.parse("tel:$phone")
      startActivity(callIntent)
   }

   private fun onUserEmail(email: String) {
      val intent = Intent(Intent.ACTION_VIEW)
      intent.data = Uri.parse("mailto:$email")
      startActivity(intent)
   }

   private fun onUserLocation(location: String) {
      val clipboard: ClipboardManager? = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      val clip = ClipData.newPlainText("User Location", location)
      if (clipboard == null || clip == null) return
      clipboard.setPrimaryClip(clip)
      Snackbar.make(requireView(), R.string.location_text_copy_message, Snackbar.LENGTH_SHORT).show()
   }
}