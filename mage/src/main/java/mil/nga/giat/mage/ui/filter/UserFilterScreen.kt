package mil.nga.giat.mage.ui.filter

import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import mil.nga.giat.mage.R
import mil.nga.giat.mage.ui.theme.MageTheme
import mil.nga.giat.mage.ui.theme.linkColor
import mil.nga.giat.mage.ui.theme.topAppBarBackground
import mil.nga.giat.mage.utils.UserInfo
import kotlin.text.isNotBlank


@Composable
fun UserFilterScreen(
    onNavigateUp: () -> Unit,
    viewModel: UserFilterViewModel = hiltViewModel()
) {

    val usersForEvent by viewModel.usersForEvent.collectAsState()
    val usersMatchingSearch by viewModel.usersMatchingSearch.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedUsersIdsForFilter by viewModel.selectedUserIdsForFilter.collectAsState()

    val onToggleUserSelection: (UserInfo) -> Unit = { userInfo ->
        viewModel.toggleUserIdSelection(userInfo)
    }
    val onSearchQueryChanged: (String) -> Unit = { query ->
        viewModel.onSearchQueryChanged(query)
    }
    val onClearSelectedUsers = { viewModel.clearSelectedUserFilters() }

    UserFilterScreenContent(isLoading, usersForEvent, usersMatchingSearch, searchQuery, selectedUsersIdsForFilter, onNavigateUp, onToggleUserSelection, onSearchQueryChanged, onClearSelectedUsers)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFilterScreenContent(
    isLoading: Boolean,
    usersForEvent: List<UserInfo>,
    usersMatchingSearch: List<UserInfo>,
    searchQuery: String,
    selectedUserIdsForFilter: Set<String>,
    onNavigateUp: () -> Unit,
    onToggleUserSelection: (UserInfo) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onClearSelectedUserFilters: () -> Unit
) {
    val selectedUsernamesForFilterText = remember(selectedUserIdsForFilter, usersForEvent) {
        if (selectedUserIdsForFilter.isEmpty()) {
            ""
        } else {
            usersForEvent.asSequence().filter { userInfo ->
                (selectedUserIdsForFilter.contains(userInfo.id))
            }
                .map { if (!TextUtils.isEmpty(it.displayName)) {it.displayName} else { it.userName } }
                .filter { it.isNotBlank() }
                .sortedBy { it.lowercase() }
                .joinToString(", ")
        }
    }

    MageTheme {
        Scaffold(
            containerColor = colorResource(R.color.background),
            topBar = {
                TopAppBar(
                    modifier = Modifier
                        .background(color = androidx.compose.material.MaterialTheme.colors.topAppBarBackground)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
                    colors = topAppBarColors(
                        containerColor = androidx.compose.material.MaterialTheme.colors.topAppBarBackground,
                        titleContentColor = Color.White ),
                    title = { Text(stringResource(R.string.observation_users_filter)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))) {

                if (selectedUsernamesForFilterText.isNotEmpty()) {
                    UserFilterSelectedUsersSection(selectedUsernamesForFilterText, onClearSelectedUserFilters)
                }

                SearchBar(
                    query = searchQuery,
                    onQueryChange = { onSearchQueryChanged(it) },
                    onSearch = {},
                    placeholder = { Text("Search Users by Name", color = colorResource(R.color.text_variant))},
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    onActiveChange = { },
                    shadowElevation = 2.dp,
                    colors = SearchBarDefaults.colors(containerColor = colorResource(R.color.background_card), inputFieldColors = SearchBarDefaults.inputFieldColors(focusedTextColor = colorResource(R.color.text_variant), unfocusedTextColor = colorResource(R.color.text_variant))),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        .padding(top = 6.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                    active = false, //docked search bar
                    content = {} //not using an expanded view for docked search bar
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (usersForEvent.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 20.dp), contentAlignment = Alignment.TopCenter) {
                        Text("No users found for this event.", color = colorResource(R.color.text_primary), fontSize = 16.sp)
                    }
                }
                else if (usersMatchingSearch.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 20.dp), contentAlignment = Alignment.TopCenter) {
                        Text("No users found matching search", color = colorResource(R.color.text_primary), fontSize = 16.sp)
                    }
                }  else {
                    //list of users
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(count = usersMatchingSearch.size, key = { index -> usersMatchingSearch[index].id }, itemContent =  { user ->
                            val userInfo = usersMatchingSearch[user]
                            val isSelected = selectedUserIdsForFilter.contains(userInfo.id)
                            UserListItem(user = userInfo, isSelected, onToggleUserSelection = { onToggleUserSelection(userInfo) })
                        })
                    }
                }
            }
        }
    }
}


@Composable
fun UserFilterSelectedUsersSection(selectedUsernamesForFilterText: String, onClearSelectedUserFilters: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp, start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = "Selected Users:",
                fontSize = 14.sp,
                color = colorResource(R.color.text_variant)
            )
            Text(
                text = selectedUsernamesForFilterText,
                fontSize = 12.sp,
                color = colorResource(R.color.text_variant)
            )
        }

        IconButton(
            onClick = onClearSelectedUserFilters,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UserListItem(user: UserInfo, isUserSelected: Boolean, onToggleUserSelection: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.background_card))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = user.avatarIcon,
                contentDescription = "",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = placeholder(R.drawable.default_avatar),
                failure = placeholder(R.drawable.default_avatar)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (!TextUtils.isEmpty(user.displayName)) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorResource(R.color.text_primary)
                    )
                }

                if (!TextUtils.isEmpty(user.userName)) {
                    Text(
                        text = user.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.text_variant)
                    )
                }
            }

            IconButton(onClick = { onToggleUserSelection() }) {
                Icon(
                    imageVector = if (isUserSelected) Icons.Filled.CheckCircle else Icons.Filled.AddCircle,
                    contentDescription = "",
                    tint = if (isUserSelected) androidx.compose.material.MaterialTheme.colors.linkColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Preview
@Composable
fun UserFilterScreenWithSelectedUserPreview() {
    val sampleUsers = listOf(
        UserInfo("1", "John Doe", "jdoe", ""),
        UserInfo("2", "Jane Smith", "jsmith", ""),
        UserInfo("3", "Robert Johnson", "rjohnson", "")
    )
    UserFilterScreenContent(
        isLoading = false,
        usersForEvent = sampleUsers,
        usersMatchingSearch = sampleUsers,
        searchQuery = "",
        selectedUserIdsForFilter = setOf("2"),
        onNavigateUp = {},
        onToggleUserSelection = {},
        onSearchQueryChanged = {},
        onClearSelectedUserFilters = {}
    )
}

@Preview
@Composable
fun UserFilterScreenWithNoSelectedUserPreview() {
    val sampleUsers = listOf(
        UserInfo("1", "John Doe", "jdoe", ""),
        UserInfo("2", "Jane Smith", "jsmith", ""),
        UserInfo("3", "Robert Johnson", "rjohnson", "")
    )
    UserFilterScreenContent(
        isLoading = false,
        usersForEvent = sampleUsers,
        usersMatchingSearch = sampleUsers,
        searchQuery = "",
        selectedUserIdsForFilter = emptySet(),
        onNavigateUp = {},
        onToggleUserSelection = {},
        onSearchQueryChanged = {},
        onClearSelectedUserFilters = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewUserListItem() {
    MaterialTheme {
        UserListItem(user = UserInfo("1", "John Doe", "jDoe", ""), false, {})
    }
}