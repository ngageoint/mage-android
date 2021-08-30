package mil.nga.giat.mage.dagger.contributor

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.login.idp.IdpLoginFragment
import mil.nga.giat.mage.login.ldap.LdapLoginFragment
import mil.nga.giat.mage.login.mage.MageLoginFragment
import mil.nga.giat.mage.map.MapFragment
import mil.nga.giat.mage.map.preference.OnlineLayersPreferenceActivity
import mil.nga.giat.mage.map.preference.TileOverlayPreferenceActivity
import mil.nga.giat.mage.newsfeed.ObservationFeedFragment
import mil.nga.giat.mage.newsfeed.PeopleFeedFragment
import mil.nga.giat.mage.observation.edit.FormPickerBottomSheetFragment
import mil.nga.giat.mage.preferences.ClearDataPreferenceActivity
import mil.nga.giat.mage.preferences.LocationPreferencesActivity

@Module
@InstallIn(SingletonComponent::class)
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
    internal abstract fun contributeOverlayListFragment(): TileOverlayPreferenceActivity.OverlayListFragment

    @ContributesAndroidInjector
    internal abstract fun contributeClearDataFragment(): ClearDataPreferenceActivity.ClearDataFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLocationPreferenceFragment(): LocationPreferencesActivity.LocationPreferenceFragment

    @ContributesAndroidInjector
    internal abstract fun contributeOnlineLayersListFragment(): OnlineLayersPreferenceActivity.OnlineLayersListFragment
}
