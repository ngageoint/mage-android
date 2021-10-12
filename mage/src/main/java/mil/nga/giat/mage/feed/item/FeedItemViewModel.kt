package mil.nga.giat.mage.feed.item

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.FeedItemDao
import mil.nga.giat.mage.feed.FeedItemState
import javax.inject.Inject

class SnackbarState(val message: String = "")

@HiltViewModel
class FeedItemViewModel @Inject constructor(
   val application: Application,
   private val feedItemDao: FeedItemDao
): ViewModel() {
   private val _snackbar = MutableStateFlow(SnackbarState())
   val snackbar: StateFlow<SnackbarState>
      get() = _snackbar.asStateFlow()

   fun getFeedItem(feedId: String, feedItemId: String): LiveData<FeedItemState> {
      return Transformations.map(feedItemDao.item(feedId, feedItemId)) {
         FeedItemState.fromItem(it, application)
      }
   }

   fun copyToClipBoard(text: String) {
      val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
      val clip = ClipData.newPlainText("Feed Item Location", text)

      if (clipboard != null && clip != null) {
         clipboard.setPrimaryClip(clip)
         _snackbar.value = SnackbarState(application.getString(R.string.location_text_copy_message))
      }
   }
}