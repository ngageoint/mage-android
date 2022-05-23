package mil.nga.giat.mage.observation.edit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

class CaptureAudio : ActivityResultContract<Void?, Uri?>() {
   override fun createIntent(context: Context, input: Void?): Intent =
      Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

   override fun parseResult(resultCode: Int, intent: Intent?) =
      intent.takeIf { resultCode == Activity.RESULT_OK }?.data
}