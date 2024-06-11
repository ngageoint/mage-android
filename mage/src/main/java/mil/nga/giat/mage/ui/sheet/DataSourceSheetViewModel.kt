package mil.nga.giat.mage.ui.sheet

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mil.nga.giat.mage.data.repository.map.BottomSheetRepository
import mil.nga.giat.mage.ui.map.AnnotationProvider
import javax.inject.Inject

@HiltViewModel
class DataSourceSheetViewModel @Inject constructor(
    val annotationProvider: AnnotationProvider,
    bottomSheetRepository: BottomSheetRepository
): ViewModel() {
    val mapAnnotations = bottomSheetRepository.mapAnnotations
}