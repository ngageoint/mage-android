package mil.nga.giat.mage.ui.filter

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mil.nga.giat.mage.data.datasource.event.EventLocalDataSource
import mil.nga.giat.mage.data.datasource.user.UserLocalDataSource
import mil.nga.giat.mage.data.repository.user.UserRepository
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.utils.UserFilterMapper
import mil.nga.giat.mage.utils.UserFilterPrefsManager
import mil.nga.giat.mage.utils.UserInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import mil.nga.giat.mage.data.datasource.permission.RoleLocalDataSource
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.di.TokenProvider
import mil.nga.giat.mage.network.device.DeviceService
import mil.nga.giat.mage.network.user.UserService
import retrofit2.Response

@ExperimentalCoroutinesApi
class UserFilterViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val testScope = TestScope(testDispatcher + Job())

    @MockK private lateinit var mockUserService: UserService
    @MockK private lateinit var mockUserFilterMapper: UserFilterMapper
    @MockK private lateinit var mockUserLocalDataSource: UserLocalDataSource
    @MockK private lateinit var mockEventLocalDataSource: EventLocalDataSource
    @MockK private lateinit var mockUserFilterPrefsManager: UserFilterPrefsManager

    //dependencies needed to instantiate UserRepository
    @MockK private lateinit var mockApplication: Application
    @MockK private lateinit var mockPreferences: SharedPreferences
    @MockK private lateinit var mockDaoStore: MageSqliteOpenHelper
    @MockK private lateinit var mockDeviceService: DeviceService
    @MockK private lateinit var mockTokenProvider: TokenProvider
    @MockK private lateinit var mockRoleLocalDataSource: RoleLocalDataSource

    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: UserFilterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        userRepository = UserRepository(
            application = mockApplication,
            preferences = mockPreferences,
            daoStore = mockDaoStore,
            userService = mockUserService,
            deviceService = mockDeviceService,
            tokenProvider = mockTokenProvider,
            roleLocalDataSource = mockRoleLocalDataSource,
            userLocalDataSource = mockUserLocalDataSource
        )

        every { mockUserLocalDataSource.readCurrentUser() } returns currentUser
        every { mockEventLocalDataSource.currentEvent } returns currentEvent

        coEvery {
            mockUserFilterMapper.toUserInfoList(
                allRepoUsers.toList().sortedBy { it.displayName ?: it.username })
        } returns allMappedUserInfoFromRepo // Ensure consistent sorting for mock

        coEvery {
            mockUserFilterMapper.toUserInfoList(allLocalDsUsers.sortedBy {
                it.displayName ?: it.username
            })
        } returns allMappedUserInfoFromLocalDs

        every { mockUserFilterPrefsManager.getUserFilterList() } returns emptyList()
        every { mockUserFilterPrefsManager.updateUserFilterSharedPrefs(any()) } returns Unit
        every { mockUserFilterPrefsManager.clearCurrentFilter() } returns Unit

        //mock for fallback scenario
        coEvery { mockEventLocalDataSource.read(currentEvent.id) } returns currentEvent
        coEvery { mockUserLocalDataSource.getUsersInEvent(currentEvent) } returns allLocalDsUsers.toSet()

        //default to successful retrieval from repo
        val mockServiceResponse: Response<List<UserInfo>> = Response.success(allMappedUserInfoFromRepo)
        coEvery { mockUserService.getUsersForEvent(currentEvent.remoteId) } returns mockServiceResponse

    }

    private fun initializeViewModel() {
        viewModel = UserFilterViewModel(
            userRepository = userRepository,
            userFilterMapper = mockUserFilterMapper,
            userLocalDataSource = mockUserLocalDataSource,
            eventLocalDataSource = mockEventLocalDataSource,
            userFilterPrefsManager = mockUserFilterPrefsManager
        )
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.coroutineContext[Job]?.cancel() //cancel the scope's job to clean up coroutines
    }

    @Test
    fun `init - load users from API in UserService`() =
        testScope.runTest {

            initializeViewModel()

            advanceUntilIdle() //ensures coroutines launched with testDispatcher are executed until completion or suspension

            //viewModel.usersMatchingSearch initial value is empty list, so wait for the first non empty list to be populated
            val actualUsersMatchingSearch = viewModel.usersMatchingSearch.filter { it.isNotEmpty() }.first()


            assertFalse(viewModel.isLoading.value)
            assertEquals(allMappedUserInfoFromRepo, viewModel.usersForEvent.value)

            assertEquals("", viewModel.searchQuery.value)
            assertTrue(viewModel.selectedUsersForFilter.value.isEmpty())
            assertEquals(allMappedUserInfoFromRepo, actualUsersMatchingSearch)

            coVerify(exactly = 1) { mockUserService.getUsersForEvent(currentEvent.remoteId) }

            verify { mockUserFilterPrefsManager.getUserFilterList() }
        }

    @Test
    fun `init - fallback to local data source when API call returns empty list`() =
        testScope.runTest {

            //override successful UserService response in setUp() to return empty list instead
            val mockServiceEmptyResponse: Response<List<UserInfo>> = Response.success(emptyList())
            coEvery { mockUserService.getUsersForEvent(currentEvent.remoteId) } returns mockServiceEmptyResponse

            initializeViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.isLoading.value)
            assertEquals(allMappedUserInfoFromLocalDs, viewModel.usersForEvent.value)
            coVerify { mockUserService.getUsersForEvent(currentEvent.remoteId) }
            coVerify { mockUserLocalDataSource.getUsersInEvent(currentEvent) }
            coVerify {
                mockUserFilterMapper.toUserInfoList(allLocalDsUsers)
            }
        }

    @Test
    fun `init - load previously stored user ID filter`() =
        testScope.runTest {
            val previouslySelectedIds = listOf("id1", "id3")
            every { mockUserFilterPrefsManager.getUserFilterList() } returns previouslySelectedIds
            initializeViewModel()
            advanceUntilIdle()

            assertEquals(setOf(userInfo1, userInfo3), viewModel.selectedUsersForFilter.value)
        }

    @Test
    fun `init - handle no current event`() =
        testScope.runTest {
            every { mockEventLocalDataSource.currentEvent } returns null

            initializeViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.isLoading.value)
            assertTrue(viewModel.usersForEvent.value.isEmpty())
            assertTrue(viewModel.selectedUsersForFilter.value.isEmpty())

            coVerify(exactly = 0) { userRepository.getUserSetForEvent(any()) } //should not attempt to fetch users
        }


    @Test
    fun `onSearchQueryChanged - update searchQuery and filter users by display name`() =
        testScope.runTest {

            initializeViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("Ali")

            //viewModel.usersMatchingSearch initial value is empty list, so wait for the first non empty list to be populated
            val actualUsersMatchingSearch = viewModel.usersMatchingSearch.filter { it.isNotEmpty() }.first()

            assertEquals("Ali", viewModel.searchQuery.value)
            assertEquals(1, actualUsersMatchingSearch.size)
            actualUsersMatchingSearch.any { it.id == "id1" }.let { assertTrue(it) } //Alice

            //now set to empty
            viewModel.onSearchQueryChanged("")
            advanceUntilIdle() //allow Flow processing

            assertEquals("", viewModel.searchQuery.value)
            assertEquals(allMappedUserInfoFromRepo.size, viewModel.usersMatchingSearch.value.size)
        }

    @Test
    fun `onSearchQueryChanged - filters users by username if no display name match`() =
        testScope.runTest {

            initializeViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("c3")

            //viewModel.usersMatchingSearch initial value is empty list, so wait for the first non empty list to be populated
            val actualUsersMatchingSearch = viewModel.usersMatchingSearch.filter { it.isNotEmpty() }.first()

            assertEquals(1, actualUsersMatchingSearch.size)
            actualUsersMatchingSearch.any { it.id == "id3" }.let { assertTrue(it) } //Charlie

            //now set to empty
            viewModel.onSearchQueryChanged("")
            advanceUntilIdle() //allow Flow processing

            assertEquals("", viewModel.searchQuery.value)
            assertEquals(allMappedUserInfoFromRepo.size, viewModel.usersMatchingSearch.value.size)
    }


    private val currentUser = User().apply { remoteId = "currentUserRemoteId" }
    private val currentEvent = Event("123", "Test Event", "", "").apply { id = 1 }

    private val user1 = User().apply { remoteId = "id1"; displayName = "Alice"; username = "a1" }
    private val user2 = User().apply { remoteId = "id2"; displayName = "Bob"; username = "b2" }
    private val user3 = User().apply { remoteId = "id3"; displayName = "Charlie"; username = "c3" }
    private val user4 = User().apply { remoteId = "id4"; displayName = "David"; username = "d4" }


    private val userInfo1 = UserInfo("id1", "Alice", "a1", "avatar1")
    private val userInfo2 = UserInfo("id2", "Bob", "b2", "avatar2")
    private val userInfo3 = UserInfo("id3", "Charlie", "c3", "avatar3")
    private val userInfo4 = UserInfo("id4", "David", "d4", "avatar4")


    private val allRepoUsers = setOf(user1, user2, user3, user4)
    private val allLocalDsUsers = listOf(user1, user2)
    private val allMappedUserInfoFromRepo = listOf(userInfo1, userInfo2, userInfo3, userInfo4) // Assuming mapper sorts
    private val allMappedUserInfoFromLocalDs = listOf(userInfo1, userInfo2)



}