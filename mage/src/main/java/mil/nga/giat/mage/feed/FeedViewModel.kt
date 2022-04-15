package mil.nga.giat.mage.feed

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mil.nga.giat.mage.R
import mil.nga.giat.mage.data.feed.*
import mil.nga.giat.mage.feed.item.SnackbarState
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val application: Application,
    private val feedDao: FeedDao,
    private val feedItemDao: FeedItemDao,
    private val feedRepository: FeedRepository
): ViewModel() {
    private val _snackbar = MutableStateFlow(SnackbarState())
    val snackbar: StateFlow<SnackbarState>
        get() = _snackbar.asStateFlow()

    private val _feedId = MutableLiveData<String>()
    val feed: LiveData<Feed> = Transformations.switchMap(_feedId) { feedId ->
        feedDao.feed(feedId)
    }

    val feedItems: LiveData<Flow<PagingData<FeedItemState>>> = Transformations.map(feed) { feed ->
        Pager(PagingConfig(pageSize = 20)) {
            feedItemDao.pagingSource(feed.id)
        }.flow.cachedIn(viewModelScope).map {
            it.map { feedItem ->
                FeedItemState.fromItem(ItemWithFeed(feed, feedItem), application)
            }
        }
    }

    fun setFeedId(feedId: String) {
        _feedId.postValue(feedId)
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing.asStateFlow()
    fun refresh() {
        val feed = this.feed.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.emit(true)
            feedRepository.syncFeed(feed)
            _isRefreshing.emit(false)
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