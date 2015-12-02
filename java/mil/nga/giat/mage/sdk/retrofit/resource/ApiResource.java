package mil.nga.giat.mage.sdk.retrofit.resource;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

import mil.nga.giat.mage.sdk.retrofit.HttpClient;
import retrofit.Call;
import retrofit.Response;
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

    public String getApi(String url) throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(HttpClient.httpClient(context))
                .build();

        ApiService service = retrofit.create(ApiService.class);
        Response<ResponseBody> response = service.getApi().execute();

        if (response.isSuccess()) {
            return response.body().string();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return null;
    }
}