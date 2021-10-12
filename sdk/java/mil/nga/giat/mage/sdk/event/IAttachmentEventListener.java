package mil.nga.giat.mage.sdk.event;

import mil.nga.giat.mage.sdk.datastore.observation.Attachment;

public interface IAttachmentEventListener extends IEventListener {

	public void onAttachmentCreated(final Attachment attachment);

	public void onAttachmentUpdated(final Attachment attachment);

	public void onAttachmentDeleted(final Attachment attachment);

	public void onAttachmentUploadable(final Attachment attachment);
}
