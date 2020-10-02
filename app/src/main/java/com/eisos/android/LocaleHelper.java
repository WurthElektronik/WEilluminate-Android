/*
 * __          ________        _  _____
 * \ \        / /  ____|      (_)/ ____|
 *  \ \  /\  / /| |__      ___ _| (___   ___  ___
 *   \ \/  \/ / |  __|    / _ \ |\___ \ / _ \/ __|
 *    \  /\  /  | |____  |  __/ |____) | (_) \__ \
 *     \/  \/   |______|  \___|_|_____/ \___/|___/
 *
 * Copyright Wuerth Elektronik eiSos 2019
 *
 */

package com.eisos.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import androidx.preference.PreferenceManager;

import com.eisos.android.utils.Preferences;

import java.util.Locale;

public class LocaleHelper {

    /**
     * Applies the selected language to the system
     * @param context The context of the application
     * @param lang The locale language code
     */
    public static void applyLanguage(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = context.getResources().getConfiguration();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, dm);
    }

    /**
     * Returns a locale language code like "en" or "de" for the given option.
     * The available options may change in the future.
     * @param option The selected option of the language dialog
     * @return The locale language otherwise null
     */
    public static String getLocaleCode(int option) {
        switch (option) {
            case 0: return Locale.ENGLISH.getLanguage();
            case 1: return Locale.GERMAN.getLanguage();
        }
        return null;
    }

    /**
     * Returns the selected option of the local language code
     * @param langCode The code of the locale language
     * @return The index of the selected language otherwise -1
     */
    public static int getItemPosition(String langCode) {
        switch (langCode) {
            case "en": return 0;
            case "de": return 1;
        }
        return -1;
    }

    /**
     * Returns the locale language code of the option saved in the SharedPreferences
     * @param context The context of the application
     * @return The locale language code
     */
    public static String getSavedLocaleCode(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String langCode = sharedPrefs.getString(Preferences.PREF_LANGUAGE, Locale.ENGLISH.getLanguage());
        return langCode;
    }
}
