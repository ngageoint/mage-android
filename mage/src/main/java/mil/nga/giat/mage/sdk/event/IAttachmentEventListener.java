package mil.nga.giat.mage.sdk.event;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

public interface IAttachmentEventListener extends IEventListener {

	void onAttachmentCreated(final Attachment attachment);

	void onAttachmentUpdated(final Attachment attachment);

	void onAttachmentDeleted(final Attachment attachment);

	void onAttachmentUploadable(final Attachment attachment);
}
