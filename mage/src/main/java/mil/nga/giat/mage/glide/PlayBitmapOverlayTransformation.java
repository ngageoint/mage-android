package mil.nga.giat.mage.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 2/23/16.
 */
public class PlayBitmapOverlayTransformation extends BitmapTransformation {

    Context context;

    public PlayBitmapOverlayTransformation(Context context) {
        super(context);

        this.context = context;
    }

//    public PlayOverlay(BitmapPool bitmapPool) {
//        super(bitmapPool);
//    }

    // Bitmap doesn't implement equals, so == and .equals are equivalent here.
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        final Bitmap toReuse = pool.get(outWidth, outHeight, toTransform.getConfig() != null
                ? toTransform.getConfig() : Bitmap.Config.ARGB_8888);

        Bitmap transformed = overlay(toReuse, toTransform, outWidth, outHeight);
        if (toReuse != null && toReuse != transformed && !pool.put(toReuse)) {
            toReuse.recycle();
        }

        return transformed;
    }

    @Override
    public String getId() {
        return "PlayOverlay";
    }

    private Bitmap overlay(Bitmap recycled, Bitmap toOverlay, int width, int height) {
        if (toOverlay == null) {
            return null;
        }

        final Bitmap result;
        if (recycled != null) {
            result = recycled;
        } else {
            result = Bitmap.createBitmap(width, height, toOverlay.getConfig());
        }

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(toOverlay, new Matrix(), null);

        int playHeight = height / 4;
        int playWidth = width / 4;
        int x = (height / 2) - playHeight;
        int y = (width / 2) - playWidth;

        Bitmap play = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_play_circle_outline_white_48dp);
        Bitmap playScaled = Bitmap.createScaledBitmap(play, width / 2, height / 2, false);

        canvas.drawBitmap(playScaled, x, y, null);
        return result;
    }
}
