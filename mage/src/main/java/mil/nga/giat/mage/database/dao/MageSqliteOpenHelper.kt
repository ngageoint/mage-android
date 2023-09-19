package mil.nga.giat.mage.database.dao

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper
import com.j256.ormlite.field.DataPersisterManager
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.feature.StaticFeature
import mil.nga.giat.mage.database.model.feature.StaticFeatureProperty
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.location.LocationProperty
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
import mil.nga.giat.mage.database.model.observation.ObservationErrorPersister
import mil.nga.giat.mage.database.model.observation.ObservationFavorite
import mil.nga.giat.mage.database.model.observation.ObservationForm
import mil.nga.giat.mage.database.model.observation.ObservationImportant
import mil.nga.giat.mage.database.model.observation.ObservationProperty
import mil.nga.giat.mage.database.model.permission.Role
import mil.nga.giat.mage.database.model.team.Team
import mil.nga.giat.mage.database.model.team.TeamEvent
import mil.nga.giat.mage.database.model.user.User
import mil.nga.giat.mage.database.model.user.UserLocal
import mil.nga.giat.mage.database.model.user.UserTeam
import java.sql.SQLException
import kotlin.Int
import kotlin.Throws

class MageSqliteOpenHelper(
   context: Context
) : OrmLiteSqliteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
   
   @Throws(SQLException::class)
   private fun createTables() {
      TableUtils.createTable(connectionSource, Observation::class.java)
      TableUtils.createTable(connectionSource, ObservationForm::class.java)
      TableUtils.createTable(connectionSource, ObservationProperty::class.java)
      TableUtils.createTable(connectionSource, ObservationImportant::class.java)
      TableUtils.createTable(connectionSource, ObservationFavorite::class.java)
      TableUtils.createTable(connectionSource, Attachment::class.java)
      TableUtils.createTable(connectionSource, User::class.java)
      TableUtils.createTable(connectionSource, UserLocal::class.java)
      TableUtils.createTable(connectionSource, Role::class.java)
      TableUtils.createTable(connectionSource, Event::class.java)
      TableUtils.createTable(connectionSource, Form::class.java)
      TableUtils.createTable(connectionSource, Team::class.java)
      TableUtils.createTable(connectionSource, UserTeam::class.java)
      TableUtils.createTable(connectionSource, TeamEvent::class.java)
      TableUtils.createTable(connectionSource, Location::class.java)
      TableUtils.createTable(connectionSource, LocationProperty::class.java)
      TableUtils.createTable(connectionSource, Layer::class.java)
      TableUtils.createTable(connectionSource, StaticFeature::class.java)
      TableUtils.createTable(connectionSource, StaticFeatureProperty::class.java)
      DataPersisterManager.registerDataPersisters(ObservationErrorPersister.singleton)
   }

   override fun onCreate(sqliteDatabase: SQLiteDatabase, connectionSource: ConnectionSource) {
      try {
         createTables()
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Could not create tables.", e)
      }
   }

   @Throws(SQLException::class)
   private fun dropTables() {
      TableUtils.dropTable<Observation, Long>(connectionSource, Observation::class.java, true)
      TableUtils.dropTable<ObservationForm, Long>(connectionSource, ObservationForm::class.java, true)
      TableUtils.dropTable<ObservationProperty, Long>(connectionSource, ObservationProperty::class.java, true)
      TableUtils.dropTable<ObservationImportant, Long>(connectionSource, ObservationImportant::class.java, true)
      TableUtils.dropTable<ObservationFavorite, Long>(connectionSource, ObservationFavorite::class.java, true)
      TableUtils.dropTable<Attachment, Long>(connectionSource, Attachment::class.java, true)
      TableUtils.dropTable<User, Long>(connectionSource, User::class.java, true)
      TableUtils.dropTable<UserLocal, Long>(connectionSource, UserLocal::class.java, true)
      TableUtils.dropTable<Role, Long>(connectionSource, Role::class.java, true)
      TableUtils.dropTable<Event, Long>(connectionSource, Event::class.java, true)
      TableUtils.dropTable<Form, Long>(connectionSource, Form::class.java, true)
      TableUtils.dropTable<Team, Long>(connectionSource, Team::class.java, true)
      TableUtils.dropTable<UserTeam, Long>(connectionSource, UserTeam::class.java, true)
      TableUtils.dropTable<TeamEvent, Long>(connectionSource, TeamEvent::class.java, true)
      TableUtils.dropTable<Location, Long>(connectionSource, Location::class.java, true)
      TableUtils.dropTable<LocationProperty, Long>(connectionSource, LocationProperty::class.java, true)
      TableUtils.dropTable<Layer, Long>(connectionSource, Layer::class.java, true)
      TableUtils.dropTable<StaticFeature, Long>(connectionSource, StaticFeature::class.java, true)
      TableUtils.dropTable<StaticFeatureProperty, Long>(connectionSource, StaticFeatureProperty::class.java, true)
   }

   override fun onUpgrade(
      database: SQLiteDatabase,
      connectionSource: ConnectionSource,
      oldVersion: Int,
      newVersion: Int
   ) {
      resetDatabase()
   }

   /**
    * Drop and create all tables.
    */
   fun resetDatabase() {
      try {
         Log.d(LOG_NAME, "Reseting Database.")
         dropTables()
         createTables()
         Log.d(LOG_NAME, "Reset Database.")
      } catch (e: SQLException) {
         Log.e(LOG_NAME, "Could not reset Database.", e)
      }
   }

   companion object {
      private const val DATABASE_NAME = "mage.db"
      private val LOG_NAME = MageSqliteOpenHelper::class.java.name

      const val DATABASE_VERSION = 22
   }
}