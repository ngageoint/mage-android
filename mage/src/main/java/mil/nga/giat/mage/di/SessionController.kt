package mil.nga.giat.mage.di

/**
 * The subset of MageApplication's behavior that other app components need to
 * trigger directly. Injected as this interface so instrumented tests running under
 * HiltTestApplication can substitute a fake via @BindValue instead.
 */
interface SessionController {
    fun onLogin()
    fun onLogout(clearTokenInformationAndSendLogoutRequest: Boolean)
    fun shouldReportLocation(): Boolean
    fun startLocationTrackingService()
    fun stopLocationTrackingService()
    fun recreateLocationService()
}
