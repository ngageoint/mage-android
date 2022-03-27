package mil.nga.giat.mage.profile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.request.target.Target

import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import mil.nga.giat.mage.R
import mil.nga.giat.mage.databinding.AttachmentViewerBinding
import mil.nga.giat.mage.glide.GlideApp
import mil.nga.giat.mage.glide.model.Avatar
import mil.nga.giat.mage.sdk.datastore.user.User
import mil.nga.giat.mage.sdk.datastore.user.UserHelper

class ProfilePictureViewerActivity : AppCompatActivity() {

    companion object {
        private val LOG_NAME = ProfilePictureViewerActivity::class.java.name

        private const val USER_ID_EXTRA = "USER_ID"

        fun intent(context: Context, user: User): Intent {
            val intent = Intent(context, ProfilePictureViewerActivity::class.java)
            intent.putExtra(USER_ID_EXTRA, user.id)
            return intent
        }
    }

    private lateinit var binding: AttachmentViewerBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        require(intent.hasExtra(USER_ID_EXTRA)) {"USER_ID_EXTRA is required to launch ProfilePictureViewerActivity"}

        binding = AttachmentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        binding.progress.visibility = View.VISIBLE

        try {
            val userID = intent.getLongExtra(USER_ID_EXTRA, -1)
            val user = UserHelper.getInstance(applicationContext).read(userID)
            this.title = user.displayName

            GlideApp.with(this)
                .load(Avatar.forUser(user))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        binding.progress.visibility = View.GONE
                        binding.noContent.visibility = View.VISIBLE
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        binding.progress.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.image)
        } catch (e: Exception) {
            Log.e(LOG_NAME, "Could not set title.", e)
            binding.progress.visibility = View.GONE
            showErrorDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
            .setTitle("Error Downloading Avatar")
            .setMessage("MAGE could not download this users avatar.  Please try again later.")
            .setPositiveButton(android.R.string.ok) { _, _ ->  finish()}
            .create()
            .show()
    }
}
