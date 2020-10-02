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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.eisos.android.utils.Preferences;

public class SplashActivity extends AppCompatActivity {

    private static int DELAY = 1000;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean firstStart = sharedPrefs.getBoolean(Preferences.PREF_FIRST_START, true);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(firstStart) {
                    final Intent intent = new Intent(SplashActivity.this, PolicyActivity.class);
                    startActivity(intent);
                } else {
                    final Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                finish();
            }
        }, DELAY);
    }
}
