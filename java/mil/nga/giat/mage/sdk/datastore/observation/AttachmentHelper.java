package mil.nga.giat.mage.sdk.datastore.observation;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.Files;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.DaoHelper;
import mil.nga.giat.mage.sdk.event.IAttachmentEventListener;
import mil.nga.giat.mage.sdk.event.IEventDispatcher;
import mil.nga.giat.mage.sdk.exceptions.ObservationException;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

public class AttachmentHelper extends DaoHelper<Attachment> implements IEventDispatcher<IAttachmentEventListener> {

	private static final String LOG_NAME = AttachmentHelper.class.getName();

	private static final SecureRandom random = new SecureRandom();

	private final Dao<Attachment, Long> attachmentDao;

	private Collection<IAttachmentEventListener> listeners = new CopyOnWriteArrayList<IAttachmentEventListener>();

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
	public Attachment create(Attachment attachment) throws Exception {
		attachmentDao.createOrUpdate(attachment);

		for (IAttachmentEventListener listener : listeners) {
			listener.onAttachmentCreated(attachment);
		}

		return attachment;
	}

	@Override
	public Attachment update(Attachment attachment) throws SQLException {
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
		QueryBuilder<Attachment, Long> queryBuilder = attachmentDao.queryBuilder();
		List<Attachment> attachments = new ArrayList<Attachment>();

		try {
			queryBuilder.where().eq("dirty", true);
			attachments = attachmentDao.query(queryBuilder.prepare());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_NAME, "Could not get dirty Attachments.");
		}
		return attachments;
	}

	/**
	 * This method will attempt to correctly rotate and format an image in preparation for display or uploading.
	 *
	 * @param attachment
	 */
	public void stageForUpload(Attachment attachment) throws Exception {
		File stageDir = MediaUtility.getMediaStageDirectory();
		File inFile = new File(attachment.getLocalPath());
		// add random string to the front of the filename to avoid conflicts
		File stagedFile = new File(stageDir, new BigInteger(30, random).toString(32) + new File(attachment.getLocalPath()).getName());

		Log.d(LOG_NAME, "Staging file: " + stagedFile.getAbsolutePath());
		Log.d(LOG_NAME, "Local path is: " + attachment.getLocalPath());
		if (stagedFile.getAbsolutePath().equalsIgnoreCase(attachment.getLocalPath())) {
			Log.d(LOG_NAME, "Attachment is already staged.  Nothing to do.");
			return;
		}
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
		Integer outImageSize = sharedPreferences.getInt(mApplicationContext.getString(R.string.imageUploadSizeKey), mApplicationContext.getResources().getInteger(R.integer.imageUploadSizeDefaultValue));

		if (MediaUtility.isImage(stagedFile.getAbsolutePath())) {

			// if not original image size
			if (outImageSize > 0) {

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				options.inSampleSize = 2;
				Bitmap bitmap = BitmapFactory.decodeFile(inFile.getAbsolutePath(), options);

				// Scale file
				Integer inWidth = bitmap.getWidth();
				Integer inHeight = bitmap.getHeight();

				Integer outWidth = outImageSize;
				Integer outHeight = outImageSize;

				if (inWidth > inHeight) {
					outWidth = outImageSize;
					outHeight = ((Double) ((inHeight.doubleValue() / inWidth.doubleValue()) * outImageSize.doubleValue())).intValue();
				} else if (inWidth < inHeight) {
					outWidth = ((Double) ((inWidth.doubleValue() / inHeight.doubleValue()) * outImageSize.doubleValue())).intValue();
					outHeight = outImageSize;
				}
				bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

				// TODO: TESTING, might still run out of memory...
				if(outImageSize <= 1024) {
					bitmap = MediaUtility.orientBitmap(bitmap, inFile.getAbsolutePath(), true);
				}

				OutputStream out = new FileOutputStream(stagedFile);
				bitmap.compress(CompressFormat.JPEG, 100, out);

				out.flush();
				out.close();
				bitmap.recycle();
				MediaUtility.copyExifData(inFile, stagedFile);
			} else {
				Files.copy(inFile, stagedFile);
			}
		} else {
			Files.copy(inFile, stagedFile);
		}
		attachment.setLocalPath(stagedFile.getAbsolutePath());
		AttachmentHelper.getInstance(mApplicationContext).uploadableAttachment(attachment);
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
