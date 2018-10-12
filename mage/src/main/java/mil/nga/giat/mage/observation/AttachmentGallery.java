package mil.nga.giat.mage.observation;

import android.content.Context;
import android.support.v4.widget.CircularProgressDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

/**
 * Created by wnewman on 5/11/15.
 */
public class AttachmentGallery {
    public interface OnAttachmentClickListener {
        void onAttachmentClick(Attachment attachment);
    }

    private Context context;
    private int width;
    private int height;
    private OnAttachmentClickListener attachmentClickListener;

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

    void addAttachment(ViewGroup gallery, final Attachment a) {
        final ImageView iv = new ImageView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        iv.setLayoutParams(lp);
        iv.setBackgroundColor(context.getResources().getColor(R.color.md_grey_100));
        lp.setMargins(0, 16, 25, 16);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (attachmentClickListener != null) {
                    attachmentClickListener.onAttachmentClick(a);
                }
            }
        });
        gallery.addView(iv);

        CircularProgressDrawable progress = new CircularProgressDrawable(context);
        progress.setStrokeWidth(10f);
        progress.setCenterRadius(width / 4);
        progress.setColorSchemeColors(context.getResources().getColor(R.color.md_blue_600), context.getResources().getColor(R.color.md_orange_A200));
        progress.start();

        boolean isVideo = false;
        if (a.getLocalPath() != null) {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(a.getLocalPath());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
            isVideo = mimeType.contains("video/mp4");
        } else if (a.getContentType() != null) {
            isVideo = a.getContentType().contains("video/mp4");
        }

        Collection<BitmapTransformation> transformations = new ArrayList<>();
        transformations.add(new CenterCrop());
        if (isVideo) {
            transformations.add(new VideoOverlayTransformation(context));
        }

        GlideApp.with(context)
                .asBitmap()
                .load(a)
                .placeholder(progress)
                .fallback(R.drawable.ic_attachment_200dp)
                .error(R.drawable.ic_attachment_200dp)
                .transforms(transformations.toArray(new BitmapTransformation[]{}))
                .into(iv);
    }
}
