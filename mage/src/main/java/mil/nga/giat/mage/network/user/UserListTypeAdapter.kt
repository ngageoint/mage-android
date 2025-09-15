package mil.nga.giat.mage.network.user

import android.text.TextUtils
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import mil.nga.giat.mage.utils.UserInfo
import java.io.IOException

class UserListTypeAdapter() : TypeAdapter<UserInfo>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: UserInfo?) {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): UserInfo? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        reader.beginObject()

        var id = ""
        var username = ""
        var displayName = ""
        var avatarUrl = ""

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "username" -> username = reader.nextString()
                "displayName" -> displayName = reader.nextString()
                "avatarUrl" -> if (reader.peek() == JsonToken.NULL) reader.nextNull() else avatarUrl = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (TextUtils.isEmpty(id)) {
            return null;
        }

        return UserInfo(
            id = id,
            userName = username,
            displayName = displayName,
            avatarIcon = avatarUrl
        )
    }
}