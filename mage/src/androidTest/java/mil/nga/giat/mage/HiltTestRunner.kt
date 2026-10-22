package mil.nga.giat.mage

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Swaps in HiltTestApplication for instrumented tests, so MageApplication's own onCreate() never
 * runs. Tests supply whatever session/data-source state they need via @HiltAndroidTest and
 * @BindValue instead.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        WorkManagerTestInitHelper.initializeTestWorkManager(targetContext, Configuration.Builder().build())
    }
}
