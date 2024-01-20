package mil.nga.giat.mage.di

import android.app.Application
import com.j256.ormlite.android.apptools.OpenHelperManager
import com.j256.ormlite.dao.Dao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.database.dao.MageSqliteOpenHelper
import mil.nga.giat.mage.database.model.event.Event
import mil.nga.giat.mage.database.model.event.Form
import mil.nga.giat.mage.database.model.geojson.StaticFeature
import mil.nga.giat.mage.database.model.geojson.StaticFeatureProperty
import mil.nga.giat.mage.database.model.layer.Layer
import mil.nga.giat.mage.database.model.location.Location
import mil.nga.giat.mage.database.model.location.LocationProperty
import mil.nga.giat.mage.database.model.observation.Attachment
import mil.nga.giat.mage.database.model.observation.Observation
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
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class OrmLiteModule {

    @Provides
    @Singleton
    fun provideSQLiteOpenHelper(application: Application): MageSqliteOpenHelper {
        OpenHelperManager.setOpenHelperClass(MageSqliteOpenHelper::class.java)
        return OpenHelperManager.getHelper(application, MageSqliteOpenHelper::class.java)
    }

    @Provides
    @Singleton
    fun provideUserDao(daoStore: MageSqliteOpenHelper): Dao<User, Long> {
        return daoStore.getDao(User::class.java)
    }

    @Provides
    @Singleton
    fun provideUserLocalDao(daoStore: MageSqliteOpenHelper): Dao<UserLocal, Long> {
        return daoStore.getDao(UserLocal::class.java)
    }

    @Provides
    @Singleton
    fun provideRoleDao(daoStore: MageSqliteOpenHelper): Dao<Role, Long> {
        return daoStore.getDao(Role::class.java)
    }

    @Provides
    @Singleton
    fun provideTeamDao(daoStore: MageSqliteOpenHelper): Dao<Team, Long> {
        return daoStore.getDao(Team::class.java)
    }

    @Provides
    @Singleton
    fun provideUserTeamDao(daoStore: MageSqliteOpenHelper): Dao<UserTeam, Long> {
        return daoStore.getDao(UserTeam::class.java)
    }

    @Provides
    @Singleton
    fun provideEventDao(daoStore: MageSqliteOpenHelper): Dao<Event, Long> {
        return daoStore.getDao(Event::class.java)
    }

    @Provides
    @Singleton
    fun provideTeamEventDao(daoStore: MageSqliteOpenHelper): Dao<TeamEvent, Long> {
        return daoStore.getDao(TeamEvent::class.java)
    }

    @Provides
    @Singleton
    fun provideFormDao(daoStore: MageSqliteOpenHelper): Dao<Form, Long> {
        return daoStore.getDao(Form::class.java)
    }

    @Provides
    @Singleton
    fun provideLocationDao(daoStore: MageSqliteOpenHelper): Dao<Location, Long> {
        return daoStore.getDao(Location::class.java)
    }

    @Provides
    @Singleton
    fun provideLocationPropertyDao(daoStore: MageSqliteOpenHelper): Dao<LocationProperty, Long> {
        return daoStore.getDao(LocationProperty::class.java)
    }

    @Provides
    @Singleton
    fun provideObservationDao(daoStore: MageSqliteOpenHelper): Dao<Observation, Long> {
        return daoStore.getDao(Observation::class.java)
    }

    @Provides
    @Singleton
    fun provideObservationFormDao(daoStore: MageSqliteOpenHelper): Dao<ObservationForm, Long> {
        return daoStore.getDao(ObservationForm::class.java)
    }

    @Provides
    @Singleton
    fun provideObservationPropertyDao(daoStore: MageSqliteOpenHelper): Dao<ObservationProperty, Long> {
        return daoStore.getDao(ObservationProperty::class.java)
    }

    @Provides
    @Singleton
    fun provideObservationImportantDao(daoStore: MageSqliteOpenHelper): Dao<ObservationImportant, Long> {
        return daoStore.getDao(ObservationImportant::class.java)
    }

    @Provides
    @Singleton
    fun provideObservationFavoriteDao(daoStore: MageSqliteOpenHelper): Dao<ObservationFavorite, Long> {
        return daoStore.getDao(ObservationFavorite::class.java)
    }

    @Provides
    @Singleton
    fun provideAttachmentDao(daoStore: MageSqliteOpenHelper): Dao<Attachment, Long> {
        return daoStore.getDao(Attachment::class.java)
    }

    @Provides
    @Singleton
    fun provideLayerDao(daoStore: MageSqliteOpenHelper): Dao<Layer, Long> {
        return daoStore.getDao(Layer::class.java)
    }

    @Provides
    @Singleton
    fun provideFeatureDaoDao(daoStore: MageSqliteOpenHelper): Dao<StaticFeature, Long> {
        return daoStore.getDao(StaticFeature::class.java)
    }

    @Provides
    @Singleton
    fun provideFeaturePropertyDaoDao(daoStore: MageSqliteOpenHelper): Dao<StaticFeatureProperty, Long> {
        return daoStore.getDao(StaticFeatureProperty::class.java)
    }
}
