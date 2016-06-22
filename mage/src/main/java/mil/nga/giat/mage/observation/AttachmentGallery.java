package mil.nga.giat.mage.observation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.transcode.BitmapToGlideDrawableTranscoder;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.AttachmentVideoDecoder;
import mil.nga.giat.mage.glide.AttachmentVideoModelLoader;
import mil.nga.giat.mage.glide.BitmapFileDecoder;
import mil.nga.giat.mage.glide.PlayBitmapOverlayTransformation;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

/**
 * Created by wnewman on 5/11/15.
 */
public class AttachmentGallery {
    public interface OnAttachmentClickListener {
        public void onAttachmentClick(Attachment attachment);
    }

    private Context context;
    private int width;
    private int height;
    private OnAttachmentClickListener attachmentClickListener;

    private AttachmentGallery() {

    }

    public AttachmentGallery(Context context, int width, int height) {
        this.context = context;
        this.width = width;
        this.height = height;
    }

    public void addOnAttachmentClickListener(OnAttachmentClickListener attachmentClickListener) {
        this.attachmentClickListener = attachmentClickListener;
    }

    public void addAttachments(ViewGroup gallery, Collection<Attachment> attachments) {
        for (final Attachment a : attachments) {
            addAttachment(gallery, a);
        }
    }

    public void addAttachment(ViewGroup gallery, final Attachment a) {
        final String absPath = a.getLocalPath();
        final String remoteId = a.getRemoteId();
        final ImageView iv = new ImageView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        iv.setLayoutParams(lp);
        lp.setMargins(0, 0, 25, 0);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (attachmentClickListener != null) {
                    attachmentClickListener.onAttachmentClick(a);
                }
            }
        });
        gallery.addView(iv);

        // get content type from everywhere I can think of
        String contentType = a.getContentType();
        if (contentType == null || "".equalsIgnoreCase(contentType) || "application/octet-stream".equalsIgnoreCase(contentType)) {
            String name = a.getName();
            if (name == null) {
                name = a.getLocalPath();
                if (name == null) {
                    name = a.getRemotePath();
                }
            }
            contentType = MediaUtility.getMimeType(name);
        }

        if (absPath != null) {
            if (contentType == null) {
                Glide.with(context).load("").placeholder(R.drawable.ic_attachment_gray_48dp).into(iv);
            } else if (contentType.startsWith("image")) {
                Glide.with(context).load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
            } else if (contentType.startsWith("video")) {
                Glide.with(context)
                        .using(new AttachmentVideoModelLoader(), Attachment.class)
                        .from(Attachment.class)
                        .as(Bitmap.class)
                        .transcode(new BitmapToGlideDrawableTranscoder(context), GlideDrawable.class)
                        .decoder(new AttachmentVideoDecoder(context))
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .encoder(new BitmapEncoder())
                        .cacheDecoder(new BitmapFileDecoder())
                        .transform(new CenterCrop(context), new PlayBitmapOverlayTransformation(context))
                        .load(a)
                        .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                        .error(R.drawable.ic_videocam_gray_48dp)
                        .into(iv);
            } else if (contentType.startsWith("audio")) {
                Glide.with(context).load("").placeholder(R.drawable.ic_mic_gray_48dp).into(iv);
            }
        } else if (remoteId != null) {
            if (contentType == null) {
                Glide.with(context).load(R.drawable.ic_attachment_gray_48dp).into(iv);
            } else if (contentType.startsWith("image")) {
                Glide.with(context).load(a).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
            } else if (contentType.startsWith("video")) {
                iv.setBackgroundResource(android.R.drawable.progress_indeterminate_horizontal);
                final AnimationDrawable progress = (AnimationDrawable) iv.getBackground();
                progress.start();

                Glide.with(context)
                        .using(new AttachmentVideoModelLoader(), Attachment.class)
                        .from(Attachment.class)
                        .as(Bitmap.class)
                        .transcode(new BitmapToGlideDrawableTranscoder(context), GlideDrawable.class)
                        .decoder(new AttachmentVideoDecoder(context))
                        .diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .encoder(new BitmapEncoder())
                        .cacheDecoder(new BitmapFileDecoder())
                        .transform(new CenterCrop(context), new PlayBitmapOverlayTransformation(context))
                        .load(a)
                        .error(R.drawable.ic_videocam_gray_48dp)
                        .listener(new RequestListener<Attachment, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, Attachment model, Target<GlideDrawable> target, boolean isFirstResource) {
                                progress.stop();
                                iv.setBackground(null);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, Attachment model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                progress.stop();
                                iv.setBackground(null);
                                return false;
                            }
                        })
                        .into(iv);
            } else if (contentType.startsWith("audio")) {
                Glide.with(context).load("").placeholder(R.drawable.ic_mic_gray_48dp).into(iv);
            }
        }
    }
}
