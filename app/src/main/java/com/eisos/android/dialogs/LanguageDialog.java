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

package com.eisos.android.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.LocaleHelper;
import com.eisos.android.R;
import com.eisos.android.frags.InfoFragment;
import com.eisos.android.utils.Preferences;

import java.util.Locale;

public class LanguageDialog extends DialogFragment {

    private SharedPreferences sharedPrefs;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String langCode = sharedPrefs.getString(Preferences.PREF_LANGUAGE, Locale.ENGLISH.getLanguage());
        int checkedItem = LocaleHelper.getItemPosition(langCode);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogLanguage);
        builder.setTitle(R.string.languageDialogTitle)
                .setSingleChoiceItems(R.array.languageOptions, checkedItem, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        String langCode = LocaleHelper.getLocaleCode(which);
                        editor.putString(Preferences.PREF_LANGUAGE, langCode);
                        editor.commit();
                        getTargetFragment().onActivityResult(InfoFragment.REQ_DIALOG, Activity.RESULT_OK, null);
                        dialog.dismiss();
                    }
                });
        return builder.create();
    }
}
