package mil.nga.giat.mage.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;

import java.io.InputStream;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

/**
 * Created by wnewman on 12/3/15.
 */
public class MageGlideModule implements com.bumptech.glide.module.GlideModule {
    private static final int DEFAULT_DISK_CACHE_SIZE = 250 * 1024 * 1024;

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, DEFAULT_DISK_CACHE_SIZE));
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(Attachment.class, InputStream.class, new AttachmentLoader.Factory());
    }
}
