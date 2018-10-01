package mil.nga.giat.mage.dagger.contributor

import dagger.Module
import dagger.android.ContributesAndroidInjector
import mil.nga.giat.mage.location.LocationReportingService

@Module
abstract class ServiceContributorModule {

    @ContributesAndroidInjector
    internal abstract fun contributeLocationReportingService(): LocationReportingService

}
