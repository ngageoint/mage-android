package mil.nga.giat.mage.observation.sync

import retrofit2.Response
import java.net.HttpURLConnection

sealed class ResponseFlag {
    data object SuccessFlag : ResponseFlag()
    data object FailureFlag : ResponseFlag()
    data object RetryFlag : ResponseFlag()

    companion object {
        //Combine multiple ResponseFlags into a single result. If any flag is marked as retry, the result will be retry
        //If any flag is marked as failure and no flags are marked as retry, the result will be failure
        fun combineResponseFlags(vararg flags: ResponseFlag): ResponseFlag {
            return when {
                flags.any { it is RetryFlag } -> RetryFlag
                flags.any { it is FailureFlag } -> FailureFlag
                else -> SuccessFlag
            }
        }

        //map retrofit API response to ResultFlag
        fun <T> processResponse(response: Response<T>): ResponseFlag {
            return when {
                response.isSuccessful -> SuccessFlag
                response.code() == HttpURLConnection.HTTP_UNAUTHORIZED -> FailureFlag
                else -> RetryFlag
            }
        }

        //map retrofit API response to ResultFlag for archive API
        fun <T> processArchiveResponse(response: Response<T>): ResponseFlag {
            return when {
                response.isSuccessful -> SuccessFlag
                response.code() == HttpURLConnection.HTTP_NOT_FOUND -> SuccessFlag //observation no longer exists on the server
                response.code() == HttpURLConnection.HTTP_UNAUTHORIZED -> FailureFlag
                else -> RetryFlag
            }
        }
    }
}