package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;

import mil.nga.giat.mage.sdk.http.HttpClientManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.http.GET;

/***
 * RESTful communication for the API
 *
 * @author newmanw
 */

public class ApiResource {

    public interface ApiService {

        @GET("/api")
        Call<ResponseBody> getApi();
    }

    private static final String LOG_NAME = ApiResource.class.getName();

    private Context context;

    public ApiResource(Context context) {
        this.context = context;
    }

    public void getApi(String url, Callback<ResponseBody> callback) {
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(HttpClientManager.getInstance().httpClient())
                    .build();

            ApiService service = retrofit.create(ApiService.class);
            service.getApi().enqueue(callback);
        } catch (IllegalArgumentException e) {
            callback.onFailure(null, e);
        }
    }
}