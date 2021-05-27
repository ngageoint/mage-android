package mil.nga.giat.mage.dagger.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import mil.nga.giat.mage._server5.login.SignupViewModel_server5
import mil.nga.giat.mage.dagger.factory.ViewModelFactory
import mil.nga.giat.mage.dagger.factory.ViewModelKey
import mil.nga.giat.mage.login.LoginViewModel
import mil.nga.giat.mage.login.SignupViewModel
import mil.nga.giat.mage.observation.form.FormViewModel

@Module
internal abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    protected abstract fun loginViewModel(viewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SignupViewModel::class)
    protected abstract fun signupViewModel(viewModel: SignupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SignupViewModel_server5::class)
    protected abstract fun signupViewModel_server5(viewModel: SignupViewModel_server5): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FormViewModel::class)
    protected abstract fun formViewModel(viewModel: FormViewModel): ViewModel
}

