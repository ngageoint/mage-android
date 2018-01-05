package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;

import com.squareup.okhttp.ResponseBody;

import mil.nga.giat.mage.sdk.http.HttpClientManager;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Retrofit;
import retrofit.http.GET;

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
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(HttpClientManager.getInstance(context).httpClient())
                .build();

        ApiService service = retrofit.create(ApiService.class);
        service.getApi().enqueue(callback);
    }
}