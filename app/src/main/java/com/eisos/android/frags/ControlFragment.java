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

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.eisos.android.MainActivity;
import com.eisos.android.R;
import com.eisos.android.adapter.DeviceInstancePagerAdapter;
import com.eisos.android.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.bluetooth.services.UARTService;
import com.eisos.android.customLayout.CustomViewPager;
import com.eisos.android.customLayout.ScanListItem;
import com.eisos.android.utils.Preferences;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import no.nordicsemi.android.log.ILogSession;


public class ControlFragment extends Fragment {

    public static final String TAG = "ControlFragment";
    private static ControlFragment controlFragment;
    private CustomViewPager viewPager;
    private DeviceInstancePagerAdapter adapter;
    private FragmentManager fmManager;
    private TabLayout tlDevices;
    private TextView tvNoData;
    private Toolbar toolbar;
    private BleMulticonnectProfileService.LocalBinder mService;
    private SharedPreferences sharedPrefs;
    private ArrayList<DeviceInstanceFragment> mFragments;
    private final static int PAGE_LIMIT = ScanFragment.MAX_ALLOWED_CONS;
    private boolean isPaused = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        controlFragment = this;
        mFragments = new ArrayList<>();
        fmManager = getActivity().getSupportFragmentManager();
        adapter = new DeviceInstancePagerAdapter(mFragments, getChildFragmentManager());
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setVisibility(View.GONE);
        viewPager.setAdapter(adapter);
        tlDevices = view.findViewById(R.id.tL_devices);
        tlDevices.setSelectedTabIndicatorColor(getResources().getColor(R.color.colorPrimary));
        tlDevices.setupWithViewPager(viewPager);
        tlDevices.setVisibility(View.GONE);
        tvNoData = view.findViewById(R.id.tv_noData);
        toolbar = getActivity().findViewById(R.id.customToolbar);
        int option = sharedPrefs.getInt(Preferences.PREF_CHANGE_VIEW, 0);
        if(option == 0) {
            viewPager.setSwipingLocked(true);
        }
        return view;
    }

    public ArrayList<DeviceInstanceFragment> getDeviceInstances() {
        return this.mFragments;
    }

    public static ControlFragment getFragment() {
        return controlFragment;
    }

    public void setService(BleMulticonnectProfileService.LocalBinder service) {
        this.mService = service;
    }

    /**
     * Adds a new Fragment to the PageAdapter. The method gets called out of the service
     * {@link UARTService#onDeviceConnected(BluetoothDevice)}
     * when a device has connected
     *
     * @param device The bluetooth device which you want to connect to
     */
    public void onDeviceConnected(BluetoothDevice device) {
        toolbar.getMenu().findItem(R.id.menu_disconnect).setVisible(true);
        toolbar.getMenu().findItem(R.id.menu_switch_view).setVisible(true);
        boolean checked = sharedPrefs.getBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
        if(checked) {
            MenuItem item = toolbar.getMenu().findItem(R.id.menu_sendGlobally);
            if(item != null) {
                item.setChecked(true);
            }
        }
        if(!isPaused) {
            // If called in onPause() -> App will crash
            MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_control);
        }
        tlDevices.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
        DeviceInstanceFragment fragment = DeviceInstanceFragment.newInstance(device, mService);
        mFragments.add(fragment);
        adapter.notifyDataSetChanged();
        viewPager.setCurrentItem(mFragments.indexOf(fragment), true);
        ScanFragment.getFragment().onDeviceConnected(device);
        if(isPaused) {
            new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(),
                    device.getName() + " " + getString(R.string.deviceReconnected), Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Gets called out of the service
     * {@link UARTService#onDeviceDisconnected(BluetoothDevice, int)}
     * when a device has disconnected
     */
    public void onDeviceDisconnected(BluetoothDevice device) {
        ScanFragment.getFragment().onDeviceDisconnected(device);
        if (adapter.getCount() > 0) {
            for(DeviceInstanceFragment f : mFragments) {
                if (f.getDeviceAddress().equals(device.getAddress())) {
                    mFragments.remove(f);
                    f.onDeviceDisconnected();
                    break;
                }
            }
            adapter = new DeviceInstancePagerAdapter(mFragments, fmManager);
            viewPager.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }

        if (adapter.getCount() == 0) {
            tlDevices.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
            if(!isPaused) {
                // If called in onPause() -> App will crash
                MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_scan);
            }
            toolbar.getMenu().setGroupVisible(R.id.menu_group_one, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
    }

    /**
     * Adds a new Fragment to the PageAdapter
     *
     * @param name    The name of the demo device
     * @param address The address of the demo device
     */
    public void onDemoDeviceConnected(String name, String address, ILogSession session) {
        toolbar.getMenu().findItem(R.id.menu_disconnect).setVisible(true);
        toolbar.getMenu().findItem(R.id.menu_switch_view).setVisible(true);
        boolean checked = sharedPrefs.getBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
        if(checked) {
            MenuItem item = toolbar.getMenu().findItem(R.id.menu_sendGlobally);
            if(item != null) {
                item.setChecked(true);
            }
        }
        MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_control);
        tlDevices.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
        DeviceInstanceFragment fragment = DeviceInstanceFragment.newDemoInstance(name,
                address, session);
        mFragments.add(fragment);
        adapter.notifyDataSetChanged();
        viewPager.setCurrentItem(mFragments.indexOf(fragment), true);
    }

    /**
     * Gets called when the demo device has disconnected
     */
    public void onDemoDeviceDisconnected() {
        if (adapter.getCount() > 0) {
            DeviceInstanceFragment frag  = mFragments.remove(getSelectedTabPosition());
            frag.onDeviceDisconnected();
            adapter = new DeviceInstancePagerAdapter(mFragments, fmManager);
            viewPager.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }

        if (adapter.getCount() == 0) {
            tlDevices.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
            MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_scan);
            toolbar.getMenu().setGroupVisible(R.id.menu_group_one, false);
        }
        ScanFragment.getFragment().onDemoDeviceDisconnected(ScanFragment.DEMO_ADDRESS);
    }

    /**
     * Disconnects the device selected in the tab
     */
    public void disconnect(BluetoothDevice device) {
        if (mService != null) {
            mService.disconnect(device);
        }
    }

    public int getSelectedTabPosition() {
        return tlDevices.getSelectedTabPosition();
    }

    /**
     * Searches the right index position of the selected tab
     * in the list of the scanned items
     *
     * @return The index of the item in the scanning list
     */
    public int getTabIndexOfScanListItem() {
        String address = mFragments.get(getSelectedTabPosition()).getDeviceAddress();
        ArrayList<ScanListItem> scannedItems = ScanFragment.getFragment().getScannedItems();
        for (int i = 0; i < scannedItems.size(); i++) {
            if (address.contains(scannedItems.get(i).getDeviceAddress())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Switches between the standard and the terminal view
     */
    public void switchView() {
        int option = sharedPrefs.getInt(Preferences.PREF_CHANGE_VIEW, 0);
        MenuItem item = toolbar.getMenu().findItem(R.id.menu_switch_view);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (option == 0) {
            editor.putInt(Preferences.PREF_CHANGE_VIEW, 1);
            editor.commit();
            item.setIcon(R.drawable.ic_channel_white);
            item.setTitle(getString(R.string.menuShowViewChannels));
            viewPager.setSwipingLocked(false);
        } else if (option == 1) {
            editor.putInt(Preferences.PREF_CHANGE_VIEW, 0);
            editor.commit();
            item.setTitle(getString(R.string.menuShowViewTerminal));
            item.setIcon(R.drawable.ic_terminal);
            viewPager.setSwipingLocked(true);
        }
    }

    public CustomViewPager getViewPager() {
        return this.viewPager;
    }

    /**
     * Allows to globally control all connected devices at once
     */
    public void controlAllDevices() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        MenuItem item = toolbar.getMenu().findItem(R.id.menu_sendGlobally);
        if(item.isChecked()) {
            item.setChecked(false);
            editor.putBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
            // If not all devices shall be controlled simultaneously
            // off screen limit is 1 (slightly less resources get used)
            viewPager.setOffscreenPageLimit(1);
        } else {
            item.setChecked(true);
            editor.putBoolean(Preferences.PREF_GLOBAL_CONTROL, true);
            // Needs to be set to be able to control all views
            // When offscreen limit to low --> devices out of
            // page limit can't be controlled globally
            viewPager.setOffscreenPageLimit(PAGE_LIMIT);
        }
        editor.commit();
    }

    public void selectTab(@NonNull String address) {
        for (DeviceInstanceFragment f : mFragments) {
            if (f.getDeviceAddress().contains(address)) {
                viewPager.setCurrentItem(mFragments.indexOf(f), true);
                break;
            }
        }
    }

}
