package mil.nga.giat.mage.sdk.datastore.observation;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.event.IAttachmentEventListener;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;

public class AttachmentHelper extends DaoHelper<Attachment> implements IEventDispatcher<IAttachmentEventListener> {

	private static final String LOG_NAME = AttachmentHelper.class.getName();

	private final Dao<Attachment, Long> attachmentDao;

	private final Collection<IAttachmentEventListener> listeners = new CopyOnWriteArrayList();

	/**
	 * Singleton.
	 */
	private static AttachmentHelper attachmentHelper;

	/**
	 * Use of a Singleton here ensures that an excessive amount of DAOs are not
	 * created.
	 *
	 * @param context
	 *            Application Context
	 * @return A fully constructed and operational ObservationHelper.
	 */
	public static AttachmentHelper getInstance(Context context) {
		if (attachmentHelper == null) {
			attachmentHelper = new AttachmentHelper(context);
		}
		return attachmentHelper;
	}

	/**
	 * Only one-per JVM. Singleton.
	 *
	 * @param context
	 */
	private AttachmentHelper(Context context) {
		super(context);

		try {
			// Set up DAOs
			attachmentDao = daoStore.getAttachmentDao();
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to communicate with Attachment database.", sqle);

			throw new IllegalStateException("Unable to communicate with Attachment database.", sqle);
		}
	}

	@Override
	public Attachment read(Long id) throws Exception {
		Attachment attachment;
		try {
			attachment = attachmentDao.queryForId(id);
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to read Attachment: " + id, sqle);
			throw new ObservationException("Unable to read Attachment: " + id, sqle);
		}

		return attachment;
	}

	@Override
	public Attachment read(String remoteId) throws Exception {
		Attachment attachment = null;
		try {
			List<Attachment> results = attachmentDao.queryBuilder().where().eq("remote_id", remoteId).query();
			if (results != null && results.size() > 0) {
				attachment = results.get(0);
			}
		} catch (SQLException sqle) {
			Log.e(LOG_NAME, "Unable to read Attachment: " + remoteId, sqle);
			throw new ObservationException("Unable to read Attachment: " + remoteId, sqle);
		}

		return attachment;
	}

	@Override
	public Attachment create(Attachment attachment) throws SQLException {
		try {
			Attachment oldAttachment = read(attachment.getId());
			if (oldAttachment != null && attachment.getLocalPath() == null) {
				attachment.setLocalPath(oldAttachment.getLocalPath());
			}
		} catch (Exception e) {
		}

		attachmentDao.createOrUpdate(attachment);

		for (IAttachmentEventListener listener : listeners) {
			listener.onAttachmentCreated(attachment);
		}

		return attachment;
	}

	/**
	 *  Persist attachment to database.
	 *
	 * The localPath member will not be set to null (removed).  Please use
	 * the  {@link AttachmentHelper#removeLocalPath(Attachment)} method to
	 * set the localPath to null.
	 *
	 * @param attachment
	 * @return the attachment
	 * @throws SQLException
	 */
	@Override
	public Attachment update(Attachment attachment) throws SQLException {
		try {
			Attachment oldAttachment = read(attachment.getId());
			if (oldAttachment != null && attachment.getLocalPath() == null) {
				attachment.setLocalPath(oldAttachment.getLocalPath());
			}
		} catch (Exception e) {
		}

		attachmentDao.update(attachment);

		for (IAttachmentEventListener listener : listeners) {
			listener.onAttachmentUpdated(attachment);
		}

		return attachment;
	}

	/**
	 * Deletes an Attachment.
	 *
	 * @param attachment
	 * @throws Exception
	 */
	public void delete(Attachment attachment) throws SQLException {
		attachmentDao.deleteById(attachment.getId());

		for (IAttachmentEventListener listener : listeners) {
			listener.onAttachmentDeleted(attachment);
		}
	}

	/**
	 * A List of {@link Attachment} from the datastore that are dirty (i.e.
	 * should be synced with the server).
	 *
	 * @return
	 */
	public List<Attachment> getDirtyAttachments() {
		QueryBuilder<Attachment, Long> qb = attachmentDao.queryBuilder();


		QueryBuilder<Attachment, Long> queryBuilder = attachmentDao.queryBuilder();
		List<Attachment> attachments = new ArrayList<Attachment>();

		try {
			long count = qb.countOf();

			queryBuilder.where().eq("dirty", true);
			attachments = attachmentDao.query(queryBuilder.prepare());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_NAME, "Could not get dirty Attachments.");
		}
		return attachments;
	}

	@Override
	public boolean addListener(IAttachmentEventListener listener) {
		return listeners.add(listener);
	}

	@Override
	public boolean removeListener(IAttachmentEventListener listener) {
		return listeners.remove(listener);
	}

	public void uploadableAttachment(Attachment attachment) {
		for (IAttachmentEventListener listener : listeners) {
			listener.onAttachmentUploadable(attachment);
		}
	}
}
