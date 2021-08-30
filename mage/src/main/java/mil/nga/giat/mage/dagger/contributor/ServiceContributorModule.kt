package mil.nga.giat.mage.dagger.contributor

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.location.LocationFetchService
import mil.nga.giat.mage.location.LocationReportingService
import mil.nga.giat.mage.observation.sync.AttachmentPushService
import mil.nga.giat.mage.observation.sync.ObservationFetchService
import mil.nga.giat.mage.observation.sync.ObservationPushService

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceContributorModule {

    @ContributesAndroidInjector
    internal abstract fun contributeLocationReportingService(): LocationReportingService

    @ContributesAndroidInjector
    internal abstract fun contributeLocationFetchService(): LocationFetchService

    @ContributesAndroidInjector
    internal abstract fun contributeObservationFetchService(): ObservationFetchService

    @ContributesAndroidInjector
    internal abstract fun contributeObservationPushService(): ObservationPushService

    @ContributesAndroidInjector
    internal abstract fun contributeAttachmentPushService(): AttachmentPushService

}
