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
package com.eisos.android.frags;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eisos.android.ImprintActivity;
import com.eisos.android.LocaleHelper;
import com.eisos.android.PolicyActivity;
import com.eisos.android.R;
import com.eisos.android.WhatsNewActivity;
import com.eisos.android.dialogs.LanguageDialog;
import com.eisos.android.utils.LocaleManager;
import com.eisos.android.utils.Preferences;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class InfoFragment extends Fragment {

    public static final int REQ_DIALOG = 1;
    public static final String TAG = "InfoFragment";
    private LinearLayout llPolicy, llImprint, llWhatsNew, llLanguage, llSensors, llManuals, llSourceCode;;
    private TextView tvVersion, tvLanguage;
    private SharedPreferences sharedPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        llPolicy = view.findViewById(R.id.ll_policy);
        llPolicy.setOnClickListener((View v) -> onPolicyClicked());

        llImprint = view.findViewById(R.id.ll_imprint);
        llImprint.setOnClickListener((View v) -> onImprintClicked());

        llWhatsNew = view.findViewById(R.id.ll_whats_new);
        llWhatsNew.setOnClickListener((View v) -> onWhatsNewClicked());

        llLanguage = view.findViewById(R.id.ll_language);
        llLanguage.setOnClickListener((View v) -> onLanguageClicked());

        llSensors = view.findViewById(R.id.ll_sensors);
        llSensors.setOnClickListener((View v) -> onSensorInfoClicked());

        llManuals = view.findViewById(R.id.ll_userManual);
        llManuals.setOnClickListener((View v) -> onUserManualsClicked());

        llSourceCode = view.findViewById(R.id.ll_source_code);
        llSourceCode.setOnClickListener((View v) -> onSourceCodeClicked());

        tvVersion = view.findViewById(R.id.tv_version);
        tvLanguage = view.findViewById(R.id.tv_language);

        String langCode = sharedPrefs.getString(Preferences.PREF_LANGUAGE, Locale.ENGLISH.getLanguage());
        int checkedItem = LocaleHelper.getItemPosition(langCode);
        switch (checkedItem) {
            case 0: tvLanguage.setText(getResources().getStringArray(R.array.languageOptions)[0]);
                break;
            case 1: tvLanguage.setText(getResources().getStringArray(R.array.languageOptions)[1]);
                break;
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String version = "";
        try {
            version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException nf) {
            nf.printStackTrace();
        }
        tvVersion.setText(version);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_DIALOG && resultCode == Activity.RESULT_OK) {
            String langCode = sharedPrefs.getString(Preferences.PREF_LANGUAGE, Locale.ENGLISH.getLanguage());
            int checkedItem = LocaleHelper.getItemPosition(langCode);
            setTextFieldOption(checkedItem);
            Snackbar snackbar = Snackbar.make(llSourceCode,
                    getResources().getString(R.string.languageAfterRestart), Snackbar.LENGTH_LONG);
            (snackbar.getView()).getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            snackbar.getView().setBackgroundColor(getResources().getColor(R.color.greenSuccess));
            snackbar.show();
        }
    }

    private void setTextFieldOption(int checkedItem) {
        String option = LocaleManager.updateResources(checkedItem);
        tvLanguage.setText(option);
    }

    /**
     * onClickListener for the language
     */
    private void onLanguageClicked() {
        LanguageDialog dialog = new LanguageDialog();
        dialog.setTargetFragment(this, REQ_DIALOG);
        dialog.show(getParentFragmentManager(), "LanguageDialog");
    }

    /**
     * onClickListener for the policy
     */
    private void onPolicyClicked() {
        Intent intent = new Intent(getActivity(), PolicyActivity.class);
        getActivity().startActivity(intent);
    }

    /**
     * onClickListener for the imprint
     */
    private void onImprintClicked() {
        Intent intent = new Intent(getActivity(), ImprintActivity.class);
        getActivity().startActivity(intent);
    }

    /**
     * onClickListener for the what's new section
     */
    private void onWhatsNewClicked() {
        Intent intent = new Intent(getActivity(), WhatsNewActivity.class);
        getActivity().startActivity(intent);
    }

    /**
     * Links to the wireless connectivity page
     */
    private void onSensorInfoClicked() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.we-online.de/web/en/electronic_components/" +
                "produkte_pb/produktinnovationen/wirelessconnectivitylandingpage.php"));
        startActivity(browserIntent);
    }

    /**
     * Links to the wco manuals
     */
    private void onUserManualsClicked() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.we-online.com/web/en/electronic_components/" +
                "produkte_pb/service_pbs/wco/handbuecher/wco_handbuecher.php"));
        startActivity(browserIntent);
    }

    /**
     * Links to the source code of the app (GitHub.com)
     */
    private void onSourceCodeClicked() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/WE-eiSmart/WEilluminate"));
        startActivity(browserIntent);
    }
}
