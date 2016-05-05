package mil.nga.giat.mage.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

/**
 * Created by wnewman on 2/23/16.
 */
public class AttachmentVideoDecoder implements ResourceDecoder<Attachment, Bitmap> {
    private static final String LOG_NAME = AttachmentVideoDecoder.class.getName();


    private final Context context;
    private MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    private BitmapPool bitmapPool = new LruBitmapPool(100);

    public AttachmentVideoDecoder(Context context) {
        this.context = context;
    }

    @Override public Resource<Bitmap> decode(Attachment attachment, int width, int height) throws IOException {
        if (attachment.getUrl() != null) {
            return decodeWithUrl(attachment.getUrl());
        } else if (attachment.getLocalPath() != null) {
            return decodeWithFile(attachment.getLocalPath());
        } else {
            throw new IOException("Cannot create thumbnail for video, no URL or local file exists");
        }
    }

    @Override public String getId() {
        return "AttachmentVideoToDrawable";
    }

    private BitmapResource decodeWithUrl(String url) {
        String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(mil.nga.giat.mage.sdk.R.string.tokenKey), null);

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("authorization", "Bearer " + token);
            mediaMetadataRetriever.setDataSource(url, headers);
            Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(100);

            return new BitmapResource(bitmap, bitmapPool);
        } finally {
            if (mediaMetadataRetriever != null) {
                mediaMetadataRetriever.release();
            }
        }
    }

    private BitmapResource decodeWithFile(String path) {
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
        return new BitmapResource(bitmap, bitmapPool);
    }
}
