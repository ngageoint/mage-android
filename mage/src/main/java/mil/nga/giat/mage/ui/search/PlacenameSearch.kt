package mil.nga.giat.mage.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mil.nga.giat.mage.search.GeocoderResult
import mil.nga.giat.mage.search.SearchResponse
import mil.nga.giat.mage.search.SearchResponseType
import mil.nga.giat.mage.ui.coordinate.CoordinateTextButton
import mil.nga.giat.mage.ui.theme.onSurfaceDisabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacenameSearch(
   onSearchResultTap: (SearchResponseType, GeocoderResult) -> Unit,
   viewModel: PlacenameSearchViewModel = hiltViewModel()
) {
   val screenHeight = LocalConfiguration.current.screenHeightDp
   val focusManager = LocalFocusManager.current

   var query by rememberSaveable { mutableStateOf("") }
   val searchState by viewModel.searchState.observeAsState()

   Column(Modifier.height((screenHeight / 2).dp)) {
      SearchBar(
         placeholder = { Text(text = "Search") },
         leadingIcon = { Icon(Icons.Default.Search, "search") },
         query = query,
         onQueryChange = { query = it },
         onSearch = {
            focusManager.clearFocus();
            viewModel.search(query)
         },
         active = false,
         onActiveChange = { },
         modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
      ){}

      searchState?.let {
         SearchContent(searchState = it) { type, result ->
            onSearchResultTap(type, result)
         }
      }
   }
}

@Composable
private fun SearchContent(
   searchState: SearchState,
   onSearchResultTap: (SearchResponseType, GeocoderResult) -> Unit
) {
   when (searchState) {
      is SearchState.Searching -> SearchProgress()
      is SearchState.Complete -> {
         SearchResponse(searchState.response) { type, result ->
            onSearchResultTap(type, result)
         }
      }
   }
}

@Composable
private fun SearchResponse(
   searchResponse: SearchResponse,
   onSearchResultTap: (SearchResponseType, GeocoderResult) -> Unit
) {
   when (searchResponse) {
      is SearchResponse.Error -> {}
      is SearchResponse.Success -> {
         SearchResults(searchResponse.results) { result ->
            onSearchResultTap(searchResponse.type, result)
         }
      }
   }
}

@Composable
private fun SearchResults(
   results: List<GeocoderResult>,
   onSearchResultTap: (GeocoderResult) -> Unit
) {
   Surface(
      Modifier.nestedScroll(rememberNestedScrollInteropConnection())
   ) {
      LazyColumn(Modifier.fillMaxSize()) {
         items(count = results.size) { index ->
            val result = results[index]
            SearchResult(result) {
               onSearchResultTap(result)
            }
            Divider(Modifier.padding(start = 16.dp))
         }
      }
   }
}

@Composable
private fun SearchResult(
   result: GeocoderResult,
   onTap: () -> Unit,
) {
   Column(
      Modifier
         .clickable { onTap() }
         .fillMaxWidth()
         .padding(vertical = 4.dp)
   ) {
      Text(
         text = result.name,
         style = MaterialTheme.typography.bodyLarge,
         modifier = Modifier.padding(start = 16.dp)
      )

      CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
         result.address?.let {
            Text(
               text = it,
               style = MaterialTheme.typography.bodyMedium,
               modifier = Modifier.padding(start = 16.dp)
            )
         }
      }

      Box {
         CoordinateTextButton(
            latLng = result.location,
            icon = {
               Icon(
                  imageVector = Icons.Default.MyLocation,
                  tint = MaterialTheme.colorScheme.tertiary,
                  contentDescription = "search",
                  modifier = Modifier.size(16.dp)
               )
            },
            onCopiedToClipboard = {}
         )
      }
   }
}

@Composable
private fun SearchProgress() {
   Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.fillMaxSize()
   ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
         CircularProgressIndicator(Modifier.padding(bottom = 16.dp))

         CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceDisabled) {
            Text(
               text = "Searching...",
               style = MaterialTheme.typography.bodyLarge
            )
         }
      }
   }
}