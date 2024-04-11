package mil.nga.giat.mage.ui.setup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountStateViewModel @Inject constructor(
   val contact: AdminContact
): ViewModel()