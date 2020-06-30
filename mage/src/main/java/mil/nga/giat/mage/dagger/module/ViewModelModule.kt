package mil.nga.giat.mage.dagger.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import mil.nga.giat.mage.LandingActivity
import mil.nga.giat.mage.LandingViewModel
import mil.nga.giat.mage.dagger.factory.ViewModelFactory
import mil.nga.giat.mage.dagger.factory.ViewModelKey
import mil.nga.giat.mage.event.EventViewModel
import mil.nga.giat.mage.feed.FeedViewModel
import mil.nga.giat.mage.feed.item.FeedItemViewModel
import mil.nga.giat.mage.login.LoginViewModel
import mil.nga.giat.mage.login.ServerUrlViewModel
import mil.nga.giat.mage.map.MapViewModel
import mil.nga.giat.mage.map.preference.MapPreferencesViewModel

@Module
internal abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(ServerUrlViewModel::class)
    protected abstract fun serverUrlViewModel(viewModel: ServerUrlViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    protected abstract fun loginViewModel(viewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LandingViewModel::class)
    protected abstract fun landingViewModel(viewModel: LandingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EventViewModel::class)
    protected abstract fun eventViewModel(viewModel: EventViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MapViewModel::class)
    protected abstract fun mapViewModel(viewModel: MapViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MapPreferencesViewModel::class)
    protected abstract fun mapPreferencesViewModel(viewModel: MapPreferencesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FeedViewModel::class)
    protected abstract fun feedViewModel(viewModel: FeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FeedItemViewModel::class)
    protected abstract fun feedItemViewModel(viewModel: FeedItemViewModel): ViewModel
}

