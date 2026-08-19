package mil.nga.giat.mage.observation.attachment;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.glide.GlideApp;
import mil.nga.giat.mage.glide.transform.VideoOverlayTransformation;
import mil.nga.giat.mage.database.model.observation.Attachment;

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

    public void addAttachment(ViewGroup gallery, final Attachment a) {
        boolean isUploading = a.isDirty() && a.getProcessingStatus() == null;
        boolean isPending = "pending".equals(a.getProcessingStatus());
        boolean isFailed = "rejected".equals(a.getProcessingStatus()) || "error".equals(a.getProcessingStatus());

        final AppCompatImageView iv = new AppCompatImageView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        iv.setLayoutParams(lp);
        iv.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.background_attachment, context.getTheme()));
        lp.setMargins(0, 16, 25, 16);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (attachmentClickListener != null) {
                    attachmentClickListener.onAttachmentClick(a);
                }
            }
        };
        iv.setOnClickListener(clickListener);

        if (isUploading || isPending || isFailed) {
            iv.setScaleType(ImageView.ScaleType.CENTER);

            View.OnClickListener overlayClickListener = clickListener;
            if (isFailed) {
                iv.setImageResource(R.drawable.ic_error_outline_white_24dp);
                final String message = a.getProcessingMessage() != null ? a.getProcessingMessage() : "Upload failed";
                overlayClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    }
                };
            } else {
                CircularProgressDrawable placeholderProgress = new CircularProgressDrawable(context);
                placeholderProgress.setStrokeWidth(6f);
                placeholderProgress.setCenterRadius(width / 6);
                placeholderProgress.setColorSchemeColors(context.getResources().getColor(R.color.md_blue_600), context.getResources().getColor(R.color.md_orange_A200));
                placeholderProgress.start();
                iv.setImageDrawable(placeholderProgress);
            }

            TextView label = new TextView(context);
            label.setText(isFailed ? "Upload Failed" : isPending ? "Upload pending..." : "Uploading...");
            label.setTextColor(ResourcesCompat.getColor(context.getResources(), android.R.color.white, context.getTheme()));
            label.setGravity(Gravity.CENTER);
            label.setTextSize(10f);
            label.setMaxLines(2);
            label.setPadding(4, 4, 4, 4);

            FrameLayout overlay = new FrameLayout(context);
            overlay.setLayoutParams(lp);
            overlay.setOnClickListener(overlayClickListener);
            overlay.addView(iv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            overlay.addView(label, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

            gallery.addView(overlay);
            return;
        }

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
            if (mimeType != null) {
                isVideo = mimeType.startsWith("video/");
            }
        } else if (a.getContentType() != null) {
            isVideo = a.getContentType().startsWith("video/");
        }

        Collection<BitmapTransformation> transformations = new ArrayList<>();
        transformations.add(new CenterCrop());
        if (isVideo) {
            transformations.add(new VideoOverlayTransformation(context));
        }

        BitmapTransformation[] foo = transformations.toArray(new BitmapTransformation[]{});

        GlideApp.with(context)
                .asBitmap()
                .load(a)
                .placeholder(progress)
                .fallback(R.drawable.ic_attachment_200dp)
                .error(R.drawable.ic_attachment_200dp)
                .transforms(foo)
                .into(iv);
    }
}
