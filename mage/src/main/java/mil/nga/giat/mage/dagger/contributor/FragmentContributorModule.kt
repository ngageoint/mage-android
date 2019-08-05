package mil.nga.giat.mage.dagger.contributor

import dagger.Module
import dagger.android.ContributesAndroidInjector
import mil.nga.giat.mage.login.idp.IdpLoginFragment
import mil.nga.giat.mage.login.ldap.LdapLoginFragment
import mil.nga.giat.mage.login.mage.MageLoginFragment
import mil.nga.giat.mage.map.MapFragment
import mil.nga.giat.mage.map.preference.FeatureOverlayPreferenceActivity
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment
import mil.nga.giat.mage.preferences.ClearDataPreferenceActivity
import mil.nga.giat.mage.preferences.LocationPreferencesActivity

@Module
abstract class FragmentContributorModule {

    @ContributesAndroidInjector
    internal abstract fun contributeMageLoginFragment(): MageLoginFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLdapLoginFragment(): LdapLoginFragment

    @ContributesAndroidInjector
    internal abstract fun contributeIdpLoginFragment(): IdpLoginFragment

    @ContributesAndroidInjector
    internal abstract fun contributeMapFragment(): MapFragment

    @ContributesAndroidInjector
    internal abstract fun contributeObservationFeedFragment(): ObservationFeedFragment

    @ContributesAndroidInjector
    internal abstract fun contributePeopleFeedFragment(): PeopleFeedFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFeatureListFragment(): FeatureOverlayPreferenceActivity.FeatureListFragment

    @ContributesAndroidInjector
    internal abstract fun contributeClearDataFragment(): ClearDataPreferenceActivity.ClearDataFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLocationPrefernceFragment(): LocationPreferencesActivity.LocationPreferenceFragment
}
