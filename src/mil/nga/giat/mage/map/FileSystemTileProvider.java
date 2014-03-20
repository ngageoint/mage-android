package mil.nga.giat.mage.map;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.android.gms.maps.model.UrlTileProvider;

public class FileSystemTileProvider extends UrlTileProvider {

	private static final String fileUrlFormat = "file://%s/%d/%d/%d.png";
	
	private String baseDirectory;
	
	public FileSystemTileProvider(int width, int height, String baseDirectory) {
		super(width, height);
		
		this.baseDirectory = baseDirectory;
	}

	@Override
	public URL getTileUrl(int x, int y, int z) {
        String tile = String.format(fileUrlFormat, baseDirectory, z, x, y);
        URL fileURL = null;
        try {
            fileURL = new URL(tile);
        } catch (MalformedURLException e) {
        	
        }
        
        return fileURL;
	}
}
