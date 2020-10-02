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

package com.eisos.android.utils;

import android.content.res.Configuration;
import android.content.res.Resources;

import com.eisos.android.R;

import java.util.Locale;

import static com.eisos.android.MainActivity.getActivity;

public class LocaleManager {

    public static String updateResources(int checkedItem) {
        String[] availableOptions = getActivity().getResources().getStringArray(R.array.languageOptions);
        String option = "";
        String langCode = "";
        switch (checkedItem) {
            case 0: langCode = "en";
                option = availableOptions[0];
                break;
            case 1: langCode = "de";
                option = availableOptions[1];
                break;
        }

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Resources res = getActivity().getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.locale = locale;
        getActivity().createConfigurationContext(config);
        return option;
    }
}
