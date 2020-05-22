package mil.nga.giat.mage.dagger.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import mil.nga.giat.mage.dagger.factory.ViewModelFactory
import mil.nga.giat.mage.dagger.factory.ViewModelKey
import mil.nga.giat.mage.login.LoginViewModel

@Module
internal abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    protected abstract fun loginViewModel(viewModel: LoginViewModel): ViewModel
}

