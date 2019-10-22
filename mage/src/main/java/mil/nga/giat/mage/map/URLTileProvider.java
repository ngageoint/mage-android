package mil.nga.giat.mage.map;

import android.util.Log;

import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

public class URLTileProvider extends UrlTileProvider {

    private static final String LOG_NAME = URLTileProvider.class.getName();

    private final URL myBaseURL;

    public URLTileProvider(int width, int height, URL baseUrl) {
        super(width, height);

        myBaseURL = baseUrl;
    }

    @Override
    public URL getTileUrl(int x, int y, int z) {
        String path = myBaseURL.toString();
        path = path.replaceAll("\\{s\\}\\.", "");
        path = path.replaceAll("\\{x\\}", Integer.toString(x));
        path = path.replaceAll("\\{y\\}", Integer.toString(y));
        path = path.replaceAll("\\{z\\}", Integer.toString(z));

        URL newPath = null;

        try{
            newPath = new URL(path);
        }catch(MalformedURLException e){
            Log.w(LOG_NAME, "Problem with URL " + path, e);
        }

        return newPath;
    }

}
