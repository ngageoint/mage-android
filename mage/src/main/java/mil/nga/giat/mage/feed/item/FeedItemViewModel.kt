package mil.nga.giat.mage.feed.item

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.dao.feed.FeedItemDao
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
      return feedItemDao.item(feedId, feedItemId).map {
         FeedItemState.fromItem(it, application)
      }.asLiveData()
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