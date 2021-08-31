package mil.nga.giat.mage.dagger.contributor

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage._server5.login.SignupActivity_server5
import mil.nga.giat.mage.disclaimer.DisclaimerActivity
import mil.nga.giat.mage.event.EventActivity
import mil.nga.giat.mage.event.EventsActivity
import mil.nga.giat.mage.form.defaults.FormDefaultActivity
import mil.nga.giat.mage.login.LoginActivity
import mil.nga.giat.mage.login.SignupActivity
import mil.nga.giat.mage.login.idp.IdpLoginActivity
import mil.nga.giat.mage.observation.edit.ObservationEditActivity
import mil.nga.giat.mage.observation.view.ObservationViewActivity
import mil.nga.giat.mage.preferences.LocationPreferencesActivity
import mil.nga.giat.mage.profile.ChangePasswordActivity
import mil.nga.giat.mage.profile.ProfileActivity

@Module
@InstallIn(SingletonComponent::class)
abstract class ActivityContributorModule {

    @ContributesAndroidInjector
    internal abstract fun contributeMainActivity(): LandingActivity

    @ContributesAndroidInjector
    internal abstract fun contributeDisclaimerActivity(): DisclaimerActivity

    @ContributesAndroidInjector
    internal abstract fun contributeLoginActivity(): LoginActivity

    @ContributesAndroidInjector
    internal abstract fun contributeSignupActivity(): SignupActivity

    @ContributesAndroidInjector
    internal abstract fun contributeSignupActivity_server5(): SignupActivity_server5

    @ContributesAndroidInjector
    internal abstract fun contributeIdpActivity(): IdpLoginActivity

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
