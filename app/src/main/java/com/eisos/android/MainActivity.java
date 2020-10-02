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
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.eisos.android;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.eisos.android.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.bluetooth.services.UARTService;
import com.eisos.android.customLayout.ScanListItem;
import com.eisos.android.frags.ControlFragment;
import com.eisos.android.frags.InfoFragment;
import com.eisos.android.frags.ScanFragment;
import com.eisos.android.profiles.ProfileActivity;
import com.eisos.android.utils.Preferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefs;
    private long mBackPressed;
    private static final int TIME_INTERVAL = 1000;
    private static Toast mExitToast;
    private BottomNavigationView btmNavView;
    private FragmentManager fm;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private LinearLayout toolbarLogo;
    private InfoFragment infoFragment;
    private ScanFragment scanFragment;
    private ControlFragment controlFragment;
    private Fragment activeFrag;
    private static MainActivity activity;
    private Menu menu;
    private String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.FOREGROUND_SERVICE};
    private static final int REQUEST_CODE = 1;
    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedPrefs.getBoolean(Preferences.PREF_FIRST_START, true)) {
            LocaleHelper.applyLanguage(this, Locale.getDefault().getLanguage());
        } else {
            String langCode = LocaleHelper.getSavedLocaleCode(getApplicationContext());
            LocaleHelper.applyLanguage(this, langCode);
        }
        activity = this;
        setContentView(R.layout.activity_main);

        btmNavView = findViewById(R.id.btm_nav);
        toolbar = findViewById(R.id.customToolbar);
        toolbar.getOverflowIcon().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        toolbar.setTitle("");
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        setSupportActionBar(toolbar);
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.searchToolbarTitle);
        toolbarLogo = toolbar.findViewById(R.id.toolbar_logo);
        toolbarLogo.setVisibility(View.GONE);

        infoFragment = new InfoFragment();
        scanFragment = new ScanFragment();
        controlFragment = new ControlFragment();

        fm = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragContainer, infoFragment, InfoFragment.TAG).hide(infoFragment);
        ft.add(R.id.fragContainer, scanFragment, ScanFragment.TAG);
        ft.add(R.id.fragContainer, controlFragment, ControlFragment.TAG).hide(controlFragment);
        ft.commit();
        activeFrag = scanFragment;

        // set Listener of navigation bar
        btmNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.item_info:
                        stopScan();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.hide(activeFrag);
                        ft.show(infoFragment);
                        ft.commit();
                        activeFrag = infoFragment;

                        toolbarLogo.setVisibility(View.VISIBLE);
                        toolbarTitle.setVisibility(View.GONE);
                        toolbar.getMenu().setGroupVisible(R.id.menu_group_one, false);
                        return true;
                    case R.id.item_scan:
                        FragmentTransaction ft2 = fm.beginTransaction();
                        ft2.hide(activeFrag);
                        ft2.show(scanFragment);
                        ft2.commit();
                        activeFrag = scanFragment;

                        toolbarLogo.setVisibility(View.GONE);
                        toolbarTitle.setVisibility(View.VISIBLE);
                        toolbarTitle.setText(R.string.searchToolbarTitle);
                        toolbar.getMenu().setGroupVisible(R.id.menu_group_one, false);
                        return true;
                    case R.id.item_control:
                        stopScan();
                        FragmentTransaction ft3 = fm.beginTransaction();
                        ft3.hide(activeFrag);
                        ft3.show(controlFragment);
                        ft3.commit();
                        activeFrag = controlFragment;

                        toolbarTitle.setVisibility(View.GONE);
                        toolbarLogo.setVisibility(View.VISIBLE);
                        if(ScanFragment.getFragment().getConnectedListItems().size() > 0) {
                            toolbar.getMenu().setGroupVisible(R.id.menu_group_one, true);
                        }
                        return true;
                }
                return false;
            }
        });
        showContent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        this.menu = menu;
        menu.setGroupVisible(R.id.menu_group_one, false);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int option = sharedPrefs.getInt(Preferences.PREF_CHANGE_VIEW, 0);
        MenuItem item = MainActivity.getActivity().getMenu().findItem(R.id.menu_switch_view);
        if (option == 0) {
            item.setTitle(getString(R.string.menuShowViewTerminal));
            item.setIcon(R.drawable.ic_terminal);
            controlFragment.getViewPager().setSwipingLocked(true);
        } else if (option == 1) {
            item.setIcon(R.drawable.ic_channel_white);
            item.setTitle(getString(R.string.menuShowViewChannels));
            controlFragment.getViewPager().setSwipingLocked(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                ScanListItem listItem = ((ScanListItem) ScanFragment.getFragment()
                        .getScannedItems().get(controlFragment.getTabIndexOfScanListItem()));
                if (listItem.getDeviceAddress().equals(ScanFragment.DEMO_ADDRESS)) {
                    controlFragment.onDemoDeviceDisconnected();
                } else {
                    BluetoothDevice device = listItem.getDevice();
                    controlFragment.disconnect(device);
                }
                break;
            case R.id.menu_switch_view:
                controlFragment.switchView();
                break;
            case R.id.deviceMenuItem_profiles:
                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_sendGlobally:
                controlFragment.controlAllDevices();
                break;
        }
        return true;
    }

    public String[] getPermissions() {
        return this.permissions;
    }

    public BottomNavigationView getBtmNavView() {
        return this.btmNavView;
    }

    public static MainActivity getActivity() {
        return activity;
    }

    public Menu getMenu() {
        return this.menu;
    }

    /**
     * Stops the ble scan in {@link ScanFragment#stopScan()}
     */
    public void stopScan() {
        ScanFragment.getFragment().stopScan();
    }

    private void showContent() {
        // select scan menu
        btmNavView.setSelectedItemId(R.id.item_scan);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ScanFragment.getFragment().scan();
                    if (snackbar != null) {
                        snackbar.dismiss();
                    }
                } else {
                    ScanFragment.getFragment().stopScan();
                    if(snackbar == null) {
                        snackbar = Snackbar.make(findViewById(R.id.fragContainer), R.string.permissionNotGranted, Snackbar.LENGTH_INDEFINITE);
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbar.getView().getLayoutParams();
                        params.gravity = Gravity.TOP;
                        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                        params.width = FrameLayout.LayoutParams.MATCH_PARENT;
                        snackbar.getView().setLayoutParams(params);
                        snackbar.setAction(R.string.settingsTitle, this::openSettings);
                        snackbar.setActionTextColor(getResources().getColor(android.R.color.holo_orange_light));
                        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    }
                    snackbar.show();
                }
                return;
            }
        }
    }

    /**
     * Opens the settings of the app to allow permissions
     * @param v The View where to open the settings
     */
    public void openSettings(View v) {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + activity.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            if (mExitToast != null) {
                mExitToast.cancel();
            }
            finish();
            return;
        }
        else if(getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            mExitToast = Toast.makeText(getApplicationContext(), R.string.doubleTabToExit, Toast.LENGTH_SHORT);
            mExitToast.show();
        }
        mBackPressed = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        List<BluetoothDevice> deviceList = ScanFragment.getFragment().getConnectedDevices();
        if (deviceList != null && deviceList.size() > 0) {
            BleMulticonnectProfileService.LocalBinder service = ScanFragment.getFragment().getService();
            for (BluetoothDevice device : deviceList) {
                if (service.isConnected(device)) {
                    service.disconnect(device);
                }
            }
        }
        Intent intent = new Intent(MainActivity.this, UARTService.class);
        stopService(intent);
        super.onDestroy();
    }
}
