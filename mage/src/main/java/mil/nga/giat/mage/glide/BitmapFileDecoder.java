package mil.nga.giat.mage.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.io.File;

/**
 * Created by wnewman on 2/23/16.
 */
public class BitmapFileDecoder implements ResourceDecoder<File, Bitmap> {

    BitmapPool bitmapPool = new LruBitmapPool(10);

    @Override
    public Resource<Bitmap> decode(File source, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
        return new BitmapResource(bitmap, bitmapPool);
    }

    @Override
    public String getId() {
        return "";
    }

}
