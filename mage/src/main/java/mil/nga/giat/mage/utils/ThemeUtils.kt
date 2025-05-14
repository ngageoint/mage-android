package mil.nga.giat.mage.utils

import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    enum class MageThemeModes(val code: Int) {
        LIGHT(1), DARK(2), AUTO(0);

        companion object {
            fun fromCode(code: Int): MageThemeModes? {
                return entries.firstOrNull { it.code == code }
            }
        }
    }

    fun updateUiWithDayNightTheme(themeCode: Int) {
        val mageTheme = MageThemeModes.fromCode(themeCode)

        val systemThemeMode = when(mageTheme) {
            MageThemeModes.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            MageThemeModes.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            MageThemeModes.AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }

        AppCompatDelegate.setDefaultNightMode(systemThemeMode)
    }

}