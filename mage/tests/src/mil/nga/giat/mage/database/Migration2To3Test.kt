package mil.nga.giat.mage.database

import android.content.res.AssetFileDescriptor
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import mil.nga.giat.mage.data.datasource.observation.ObservationLocationLocalDataSource
import mil.nga.giat.mage.data.repository.map.ObservationMapImage
import mil.nga.giat.mage.data.repository.map.ObservationsTileRepository
import mil.nga.giat.mage.data.repository.observation.icon.ObservationIconRepository
import mil.nga.giat.mage.database.model.observation.ObservationLocation
import mil.nga.sf.Point
import org.junit.Test
import java.io.IOException


class Migration2To3Test: TestCase() {

}