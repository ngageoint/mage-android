package mil.nga.giat.mage.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import mil.nga.giat.mage.R
import mil.nga.giat.mage.database.model.user.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserFilterMapperTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockSharedPreferences: SharedPreferences

    private lateinit var userFilterMapper: UserFilterMapper
    private val serverUrl = "https://mage.example.com"
    private val serverUrlKey = "mocked_server_url_key"

    @Before
    fun setUp() {
        every { mockContext.getString(R.string.serverURLKey) } returns serverUrlKey
        every { mockSharedPreferences.getString(serverUrlKey, null) } returns serverUrl

        userFilterMapper = UserFilterMapper(mockContext, mockSharedPreferences)
    }

    @Test
    fun `toUserInfoList - correctly maps users`() {
        val users = listOf(user1, user2,userNullAvatar, userBlankDisplayName)
        val userInfoList = userFilterMapper.toUserInfoList(users)

        assertEquals(4, userInfoList.size)

        val user1Info = userInfoList.find { it.id == "1" }
        assertEquals("User 1", user1Info?.displayName)
        assertEquals("user1", user1Info?.userName)
        assertEquals("$serverUrl/avatars/user1.png", user1Info?.avatarIcon)

        val user2Info = userInfoList.find { it.id == "2" }
        assertEquals("User 2", user2Info?.displayName)
        assertEquals("user2", user2Info?.userName)
        assertEquals("https://mage.example.com/avatars/user2.png", user2Info?.avatarIcon)

        val user3Info = userInfoList.find { it.id == "3" }
        assertEquals("User 3", user3Info?.displayName)
        assertEquals("user3", user3Info?.userName)
        assertEquals("", user3Info?.avatarIcon)

        val user4Info = userInfoList.find { it.id == "4" }
        assertEquals("", user4Info?.displayName)
        assertEquals("user4", user4Info?.userName)
        assertEquals("$serverUrl/avatars/user4.png", user4Info?.avatarIcon)
    }

    @Test
    fun `toUserInfoList - sorts users correctly by display name, then username`() {
        val users = listOf(
            user1,
            userBlankDisplayName,
            userNullDisplayName,
            user2,
            userNullAvatar
        )
        val userInfoList = userFilterMapper.toUserInfoList(users)

        assertEquals("User 1", userInfoList[0].displayName)
        assertEquals("User 2", userInfoList[1].displayName)
        assertEquals("User 3", userInfoList[2].displayName)
        assertEquals("user4", userInfoList[3].userName)
        assertEquals("user5", userInfoList[4].userName)
    }

    @Test
    fun `toUserInfoList - empty user list`() {
        val users = emptyList<User>()
        val userInfoList = userFilterMapper.toUserInfoList(users)
        assertTrue(userInfoList.isEmpty())
    }

    @Test
    fun `toUserInfoList - filters out users with null or blank remoteId`() {
        val users = listOf(user1, userNoRemoteId, user2, userBlankRemoteId)
        val userInfoList = userFilterMapper.toUserInfoList(users)

        assertEquals(2, userInfoList.size)
        assertTrue(userInfoList.any { it.id == "1" })
        assertTrue(userInfoList.any { it.id == "2" })
        assertTrue(userInfoList.none { it.displayName == "No Remote ID" })
        assertTrue(userInfoList.none { it.displayName == "Blank Remote ID" })
    }

    @Test
    fun `toUserInfoList - null server URL from SharedPreferences`() {
        every { mockSharedPreferences.getString(serverUrlKey, null) } returns null
        //reinitialize as mockSharedPreferences behavior has changed from setUp
        userFilterMapper = UserFilterMapper(mockContext, mockSharedPreferences)

        val users = listOf(user1)
        val userInfoList = userFilterMapper.toUserInfoList(users)

        assertEquals(1, userInfoList.size)
        assertEquals("1", userInfoList[0].id)
        assertEquals("", userInfoList[0].avatarIcon)
    }

    @Test
    fun `toUserInfoList - blank avatarUrl`() {

        val users = listOf(userBlankAvatar)
        val userInfoList = userFilterMapper.toUserInfoList(users)

        assertEquals(1, userInfoList.size)
        assertEquals("", userInfoList[0].avatarIcon)
    }

    @Test
    fun `toUserInfoList - null userName and displayName`() {
        val users = listOf(userNullDisplayAndUserNames)
        val userInfoList = userFilterMapper.toUserInfoList(users)

        assertEquals(1, userInfoList.size)
        assertEquals("", userInfoList[0].displayName)
        assertEquals("", userInfoList[0].userName)
        assertEquals("$serverUrl/avatars/user7.png", userInfoList[0].avatarIcon)
    }



    private val user1 = User().apply {
        remoteId = "1"
        displayName = "User 1"
        username = "user1"
        avatarUrl = "/avatars/user1.png"
    }

    private val user2 = User().apply {
        remoteId = "2"
        displayName = "User 2"
        username = "user2"
        avatarUrl = "https://mage.example.com/avatars/user2.png"
    }

    private val userNullAvatar = User().apply {
        remoteId = "3"
        displayName = "User 3"
        username = "user3"
        avatarUrl = null
    }

    private val userBlankDisplayName = User().apply {
        remoteId = "4"
        displayName = ""
        username = "user4"
        avatarUrl = "/avatars/user4.png"
    }

    private val userNullDisplayName = User().apply {
        remoteId = "5"
        displayName = null
        username = "user5"
        avatarUrl = "/avatars/user5.png"
    }

    private val userBlankAvatar = User().apply {
        remoteId = " 6"
        displayName = "User 6"
        username = "user6"
        avatarUrl = " "
    }

    private val userNullDisplayAndUserNames = User().apply {
        remoteId = " 7"
        displayName = null
        username = null
        avatarUrl = "/avatars/user7.png"
    }

    private val userNoRemoteId = User().apply {
        remoteId = null
        displayName = "No Remote Id"
        username = "noRemoteId"
    }

    private val userBlankRemoteId = User().apply {
        remoteId = "  "
        displayName = "Blank Remote Id"
        username = "blankRemoteId"
    }
}