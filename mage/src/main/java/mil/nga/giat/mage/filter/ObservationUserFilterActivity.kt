package mil.nga.giat.mage.filter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mil.nga.giat.mage.ui.filter.UserFilterScreen

@AndroidEntryPoint
class ObservationUserFilterActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UserFilterScreen(onNavigateUp = { finish() })
        }
    }
}