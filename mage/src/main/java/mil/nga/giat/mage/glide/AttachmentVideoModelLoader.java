package mil.nga.giat.mage.glide;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

/**
 * Created by wnewman on 2/23/16.
 */
public class AttachmentVideoModelLoader implements ModelLoader<Attachment, Attachment> {
    @Override public DataFetcher<Attachment> getResourceFetcher(final Attachment model, int width, int height) {
        return new DataFetcher<Attachment>() {
            @Override public Attachment loadData(Priority priority) throws Exception {
                return model;
            }

            @Override public String getId() {
                return model.getUrl() != null ? model.getUrl() : model.getLocalPath();
            }

            @Override public void cleanup() { }

            @Override public void cancel() { }
        };
    }
}
