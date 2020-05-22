package mil.nga.giat.mage.login

interface LoginListener {
    fun onLoginComplete(userChanged: Boolean? = false)
}