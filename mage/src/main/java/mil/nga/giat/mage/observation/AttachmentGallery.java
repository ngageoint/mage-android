package mil.nga.giat.mage.observation;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Collection;

import mil.nga.giat.mage.R;
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
        ImageView iv = new ImageView(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(width, height);
        iv.setLayoutParams(lp);
        iv.setPadding(0, 0, 10, 0);
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
                Glide.with(context).load(R.drawable.ic_attachment_gray_48dp).into(iv);
            } else if (contentType.startsWith("image")) {
                Glide.with(context).load(new File(absPath)).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
            } else if (contentType.startsWith("video")) {
                Glide.with(context).load("").placeholder(R.drawable.ic_videocam_gray_48dp).into(iv);
            } else if (contentType.startsWith("audio")) {
                Glide.with(context).load(R.drawable.ic_mic_gray_48dp).into(iv);
            }
        } else if (remoteId != null) {
            if (contentType == null) {
                Glide.with(context).load(R.drawable.ic_attachment_gray_48dp).into(iv);
            } else if (contentType.startsWith("image")) {
                Glide.with(context).load(a).placeholder(android.R.drawable.progress_indeterminate_horizontal).centerCrop().into(iv);
            } else if (contentType.startsWith("video")) {
                Glide.with(context).load("").placeholder(R.drawable.ic_videocam_gray_48dp).into(iv);
            } else if (contentType.startsWith("audio")) {
                Glide.with(context).load(R.drawable.ic_mic_gray_48dp).into(iv);
            }
        }
    }
}
