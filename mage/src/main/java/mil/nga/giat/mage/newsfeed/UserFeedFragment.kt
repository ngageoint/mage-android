package mil.nga.giat.mage.newsfeed

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.R
import mil.nga.giat.mage.filter.LocationFilterActivity
import mil.nga.giat.mage.profile.ProfileActivity
import mil.nga.giat.mage.sdk.datastore.user.User

@AndroidEntryPoint
class UserFeedFragment : Fragment() {

   private lateinit var viewModel: UserFeedViewModel
   private lateinit var recyclerView: RecyclerView
   private lateinit var swipeContainer: SwipeRefreshLayout

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      viewModel = ViewModelProvider(this).get(UserFeedViewModel::class.java)
   }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
      viewModel.userFeedState.observe(viewLifecycleOwner, { userFeedState: UserFeedState ->
         recyclerView.adapter = UserListAdapter(requireContext(), userFeedState) { user: User -> userClick(user) }

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

   private fun userClick(user: User) {
      val profileView = Intent(context, ProfileActivity::class.java)
      profileView.putExtra(ProfileActivity.USER_ID, user.remoteId)
      activity?.startActivity(profileView)
   }
}