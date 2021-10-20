package mil.nga.giat.mage.network

import androidx.annotation.NonNull
import androidx.annotation.Nullable

// A generic class that contains data and userStatus about loading  data.
class Resource<T> private constructor(
        @param:NonNull @field:NonNull val status: Status,
        @param:Nullable @field:Nullable val data: T?,
        @param:Nullable @field:Nullable val message: String?
) {

    enum class Status {
        SUCCESS, ERROR, LOADING
    }

    companion object {

        fun <T> success(data: T): Resource<T> {
            return Resource(Status.SUCCESS, data, null)
        }

        fun <T> error(message: String, data: T?): Resource<T> {
            return Resource(Status.ERROR, data, message)
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(Status.LOADING, data, null)
        }
    }
}