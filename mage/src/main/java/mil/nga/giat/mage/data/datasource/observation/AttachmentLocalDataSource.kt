package mil.nga.giat.mage.data.datasource.observation

import android.util.Log
import com.j256.ormlite.dao.Dao
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.sdk.event.IAttachmentEventListener
import mil.nga.giat.mage.sdk.event.IEventDispatcher
import mil.nga.giat.mage.sdk.exceptions.ObservationException
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentLocalDataSource @Inject constructor(
   private val attachmentDao: Dao<Attachment, Long>
): IEventDispatcher<IAttachmentEventListener> {

   private val listeners: MutableCollection<IAttachmentEventListener> = CopyOnWriteArrayList()

   @Throws(Exception::class)
   fun read(id: Long): Attachment {
     return try {
         attachmentDao.queryForId(id)
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to read Attachment: $id", e)
         throw ObservationException("Unable to read Attachment: $id", e)
      }
   }

   @Throws(Exception::class)
   fun read(remoteId: String): Attachment? {
      return try {
         val results = attachmentDao.queryBuilder().where().eq("remote_id", remoteId).query()
         results.firstOrNull()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Unable to read Attachment: $remoteId", e)
         throw ObservationException("Unable to read Attachment: $remoteId", e)
      }
   }

   @Throws(SQLException::class)
   fun create(attachment: Attachment): Attachment {
      try {
         val oldAttachment = read(attachment.id)
         if (attachment.localPath == null) {
            attachment.localPath = oldAttachment.localPath
         }
      } catch (_: Exception) { }
      attachmentDao.createOrUpdate(attachment)
      for (listener in listeners) {
         listener.onAttachmentCreated(attachment)
      }
      return attachment
   }

   /**
    * Persist attachment to database.
    *
    * @param attachment
    * @return the attachment
    * @throws SQLException
    */
   @Throws(SQLException::class)
   fun update(attachment: Attachment): Attachment {
      try {
         val oldAttachment = read(attachment.id)
         if (attachment.localPath == null) {
            attachment.localPath = oldAttachment.localPath
         }
      } catch (_: Exception) { }
      attachmentDao.update(attachment)
      for (listener in listeners) {
         listener.onAttachmentUpdated(attachment)
      }
      return attachment
   }

   /**
    * Deletes an Attachment.
    *
    * @param attachment
    * @throws Exception
    */
   @Throws(SQLException::class)
   fun delete(attachment: Attachment) {
      attachmentDao.deleteById(attachment.id)
      for (listener in listeners) {
         listener.onAttachmentDeleted(attachment)
      }
   }

   /**
    * A List of [Attachment] from the datastore that are dirty (i.e.
    * should be synced with the server).
    *
    * @return
    */
   val dirtyAttachments: List<Attachment>
      get() {
         val qb = attachmentDao.queryBuilder()
         val queryBuilder = attachmentDao.queryBuilder()
         var attachments: List<Attachment> = ArrayList()
         try {
            val count: Long = qb.countOf()
            queryBuilder.where().eq("dirty", true)
            attachments = attachmentDao.query(queryBuilder.prepare())
         } catch (e: SQLException) {
            Log.e(LOG_NAME, "Could not get dirty Attachments.", e)
         }
         return attachments
      }

   override fun addListener(listener: IAttachmentEventListener): Boolean {
      return listeners.add(listener)
   }

   override fun removeListener(listener: IAttachmentEventListener): Boolean {
      return listeners.remove(listener)
   }

   fun uploadableAttachment(attachment: Attachment?) {
      for (listener in listeners) {
         listener.onAttachmentUploadable(attachment)
      }
   }

   companion object {
      private val LOG_NAME = AttachmentLocalDataSource::class.java.name
   }
}