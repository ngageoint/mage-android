package mil.nga.giat.mage.dagger.contributor

import dagger.Module
import dagger.android.ContributesAndroidInjector
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.disclaimer.DisclaimerActivity
import mil.nga.giat.mage.event.EventActivity
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.form.FormDefaultActivity
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.login.oauth.OAuthLoginActivity
import mil.nga.giat.mage.preferences.LocationPreferencesActivity
import mil.nga.giat.mage.profile.ChangePasswordActivity
import mil.nga.giat.mage.profile.ProfileActivity

@Module
abstract class ActivityContributorModule {

    @ContributesAndroidInjector
    internal abstract fun contributeMainActivity(): LandingActivity

    @ContributesAndroidInjector
    internal abstract fun contributeDisclaimerActivity(): DisclaimerActivity

    @ContributesAndroidInjector
    internal abstract fun contributeLoginActivity(): LoginActivity

    @ContributesAndroidInjector
    internal abstract fun contributeOAuthActivity(): OAuthLoginActivity

    @ContributesAndroidInjector
    internal abstract fun contributeEventActivity(): EventActivity

    @ContributesAndroidInjector
    internal abstract fun contributeFormDefaultActivity(): FormDefaultActivity

    @ContributesAndroidInjector
    internal abstract fun contributeEventsActivity(): EventsActivity

    @ContributesAndroidInjector
    internal abstract fun contributeChangePasswordActivity(): ChangePasswordActivity

    @ContributesAndroidInjector
    internal abstract fun contributeLocationPreferencesActivity(): LocationPreferencesActivity

    @ContributesAndroidInjector
    internal abstract fun contributeProfileActivity(): ProfileActivity
}
