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
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eisos.android.R;
import com.eisos.android.bluetooth.UARTLogAdapter;
import com.eisos.android.bluetooth.UARTManager;
import com.eisos.android.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.bluetooth.services.UARTService;
import com.eisos.android.database.profiles.Profile;
import com.eisos.android.database.profiles.ProfileViewModel;
import com.eisos.android.dialogs.OverwriteProfileDialog;
import com.eisos.android.dialogs.ReqMtuDialog;
import com.eisos.android.dialogs.ReqPhyDialog;
import com.eisos.android.frags.threads.SendingThread;
import com.eisos.android.profiles.ProfileActivity;
import com.eisos.android.utils.CustomLogger;
import com.eisos.android.utils.Parser;
import com.eisos.android.utils.Preferences;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;

import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.response.PhyResult;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.LogContract;

public class DeviceInstanceFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    private Toolbar deviceToolbar;
    private TextView tvSelectedProfile, tvSendingInfo;
    private ImageView imgAddProfile, imgSaveProfile, imgResetProfile;
    private TextView tvChannel1, tvChannel2, tvChannel3, tvChannel4, tvBrightness, noEntries;
    private TextView tvChannel1Perc, tvChannel2Perc, tvChannel3Perc, tvChannel4Perc, tvChannelBrightPerc;
    private NestedScrollView scrollView;
    private ListView listView;
    private Spinner spinnerLogFilter;
    private LinearLayout terminalContent;
    private SeekBar sbChannel1, sbChannel2, sbChannel3, sbChannel4, sbBrightness;
    private SharedPreferences sharedPrefs;
    private static final int CHANNEL_BYTE_LENGTH = 5;
    private boolean firstReceive;
    private ArrayList<String> orderList;
    private Handler handler;
    private Runnable task;
    private String tabTitle;
    private boolean START = true;
    private static final int LOG_REQUEST_ID = 1;
    private static final int LOG_SCROLL_NULL = -1;
    private static final int LOG_SCROLLED_TO_BOTTOM = -2;
    private static final String[] LOG_PROJECTION = {LogContract.Log._ID, LogContract.Log.TIME, LogContract.Log.LEVEL, LogContract.Log.DATA};
    private UARTManager mManager;
    /**
     * The adapter used to populate the list with log entries.
     */
    private CursorAdapter mLogAdapter;
    /**
     * The log session created to log events related with the target device.
     */
    private ILogSession mLogSession;
    /**
     * The last list view position.
     */
    private int mLogScrollPosition;
    private String logFilter;
    private BluetoothDevice mDevice;
    private BleMulticonnectProfileService.LocalBinder mBinder;
    private boolean controlGlobally;
    private String deviceName, deviceAddress;
    private int preferredPhy = PhyRequest.PHY_LE_1M_MASK;
    private int preferredOption = PhyRequest.PHY_OPTION_NO_PREFERRED;
    public static final int REQ_MTU = 10;
    public static final int REQ_PHY = 11;
    public static final String EXTRA_MTU = "com.eisos.android.bluetooth.EXTRA_MTU";
    public static final String EXTRA_PHY = "com.eisos.android.bluetooth.EXTRA_PHY";
    public static final String EXTRA_PHY_OPTIONS = "com.eisos.android.bluetooth.EXTRA_PHY_OPTIONS";
    private Profile activeProfile;
    private SendingThread sendingThread;

    private DeviceInstanceFragment(BluetoothDevice device, BleMulticonnectProfileService.LocalBinder service) {
        mDevice = device;
        deviceName = mDevice.getName();
        deviceAddress = mDevice.getAddress();
        tabTitle = deviceName + "\n" + deviceAddress;
        mBinder = service;
        BleMulticonnectProfileService.TmpDeviceSettingsHelper settings = mBinder.getDeviceSettings(device);
        if(settings != null) {
            preferredPhy = settings.getPreferredPhy();
            preferredOption = settings.getPreferredOption();
        }
        mManager = mBinder.getUARTManager(device);
        mLogSession = mBinder.getLogSession(device);
    }

    private DeviceInstanceFragment(String name, String address, ILogSession session) {
        this.deviceName = name;
        this.deviceAddress = address;
        this.tabTitle = name + "\n" + address;
        this.mLogSession = session;
    }

    public static DeviceInstanceFragment newInstance(BluetoothDevice device, BleMulticonnectProfileService.LocalBinder service) {
        DeviceInstanceFragment instance = new DeviceInstanceFragment(device, service);
        return instance;
    }

    public static DeviceInstanceFragment newDemoInstance(String name, String address, ILogSession session) {
        DeviceInstanceFragment instance = new DeviceInstanceFragment(name, address, session);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_instance, container, false);
        firstReceive = true;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        controlGlobally = sharedPrefs.getBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter(UARTService.BROADCAST_UART_RX));
        orderList = new ArrayList<>();
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Create the log adapter, initially with null cursor
        mLogAdapter = new UARTLogAdapter(requireContext());
        this.listView.setAdapter(mLogAdapter);
    }

    /**
     * Log filter for the terminal
     *
     * @param filter The filter which shall be applied
     */
    public void filterLog(String filter) {
        switch (filter) {
            case "DEBUG":
                logFilter = null;
                break;
            case "VERBOSE":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.INFO + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.VERBOSE
                        + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.APPLICATION;
                break;
            case "INFO": logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.INFO
                    + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.APPLICATION
                    + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.WARNING
                    + " OR " + LogContract.Log.LEVEL + "=" + LogContract.Log.Level.ERROR;
                break;
            case "APP":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.APPLICATION;
                break;
            case "WARNING":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.WARNING;
                break;
            case "ERROR":
                logFilter = LogContract.Log.LEVEL + "=" + LogContract.Log.Level.ERROR;
                break;
        }
        if (mLogSession != null) {
            getLoaderManager().restartLoader(LOG_REQUEST_ID, null, DeviceInstanceFragment.this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
        } catch (final IllegalArgumentException e) {
            // do nothing, we were not connected to the sensor
        }
        super.onDestroy();
    }

    public String getTabTitle() {
        return this.tabTitle;
    }

    public String getDeviceAddress() {
        return this.deviceAddress;
    }

    private void initView(View view) {
        deviceToolbar = view.findViewById(R.id.device_toolbar);
        if(!deviceAddress.equals(ScanFragment.DEMO_ADDRESS)) {
            deviceToolbar.inflateMenu(R.menu.device_menu);
            deviceToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    menuItemClicked(item);
                    return true;
                }
            });
        }
        tvSelectedProfile = view.findViewById(R.id.tv_selected_profile);
        imgAddProfile = view.findViewById(R.id.img_add_profile);
        imgAddProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfileActivity();
            }
        });
        imgSaveProfile = view.findViewById(R.id.img_save_profile);
        OverwriteProfileDialog dialog = new OverwriteProfileDialog(this);
        imgSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activeProfile != null) {
                    dialog.show(getChildFragmentManager(), "OverwriteProfileDialog");
                }
            }
        });
        imgResetProfile = view.findViewById(R.id.img_reset_profile);
        imgResetProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetProfile();
            }
        });

        if(activeProfile == null) {
            imgSaveProfile.setVisibility(View.INVISIBLE);
            imgResetProfile.setVisibility(View.INVISIBLE);
        } else {
            tvSelectedProfile.setText(getString(R.string.profileSelected) + " " + activeProfile.getName());
        }
        tvChannel1 = view.findViewById(R.id.tv_channel1);
        tvChannel2 = view.findViewById(R.id.tv_channel2);
        tvChannel3 = view.findViewById(R.id.tv_channel3);
        tvChannel4 = view.findViewById(R.id.tv_channel4);
        tvBrightness = view.findViewById(R.id.tv_brightness);
        tvChannel1Perc = view.findViewById(R.id.tv_channel1_perc);
        tvChannel2Perc = view.findViewById(R.id.tv_channel2_perc);
        tvChannel3Perc = view.findViewById(R.id.tv_channel3_perc);
        tvChannel4Perc = view.findViewById(R.id.tv_channel4_perc);
        tvChannelBrightPerc = view.findViewById(R.id.tv_channelBright_perc);
        tvSendingInfo = view.findViewById(R.id.tv_sending_info);
        if(controlGlobally) {
            tvSendingInfo.setVisibility(View.VISIBLE);
        } else {
            tvSendingInfo.setVisibility(View.GONE);
        }

        initSeekBars(view);

        scrollView = view.findViewById(R.id.channelContainer);
        listView = view.findViewById(R.id.terminal);
        terminalContent = view.findViewById(R.id.terminal_content);
        terminalContent.setVisibility(View.GONE);
        noEntries = view.findViewById(R.id.tv_no_entries);
        handler = new Handler();
        int option = sharedPrefs.getInt(Preferences.PREF_CHANGE_VIEW, 0);
        if (option == 0) {
            scrollView.setVisibility(View.VISIBLE);
            terminalContent.setVisibility(View.GONE);
        } else if (option == 1) {
            scrollView.setVisibility(View.GONE);
            terminalContent.setVisibility(View.VISIBLE);
        }
        spinnerLogFilter = view.findViewById(R.id.spinner_log_filter);
        spinnerLogFilter.setSelection(2);
        spinnerLogFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterLog((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (mBinder != null) {
            onDeviceConnected();
        } else {
            onDemoDeviceConnected();

        }
    }

    public void menuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.deviceMenuItem_req_mtu:
                ReqMtuDialog mtuDialog = new ReqMtuDialog();
                mtuDialog.setTargetFragment(this, REQ_MTU);
                mtuDialog.show(getFragmentManager(), "RequestMtuDialog");
                break;
            case R.id.deviceMenuItem_read_mtu:
                getMtu();
                break;
            case R.id.deviceMenuItem_read_rssi:
                readRssi();
                break;
            case R.id.devicemenuItem_set_phy:
                ReqPhyDialog phyDialog = new ReqPhyDialog();
                phyDialog.setTargetFragment(this, REQ_PHY);
                phyDialog.show(getFragmentManager(), "ReqPhyDialog");
                break;
            case R.id.deviceMenuItem_read_phy:
                readPhy();
                break;
        }
    }

    private void openProfileActivity() {
        Intent intent = new Intent(getActivity(), ProfileActivity.class);
        intent.setAction(ProfileActivity.ACTION_ADD_PROFILE);
        intent.putExtra(ProfileActivity.EXTRA_PROFILE_CH1, Parser.convertChannelToOriginalValue(sbChannel1.getProgress()));
        intent.putExtra(ProfileActivity.EXTRA_PROFILE_CH2, Parser.convertChannelToOriginalValue(sbChannel2.getProgress()));
        intent.putExtra(ProfileActivity.EXTRA_PROFILE_CH3, Parser.convertChannelToOriginalValue(sbChannel3.getProgress()));
        intent.putExtra(ProfileActivity.EXTRA_PROFILE_CH4, Parser.convertChannelToOriginalValue(sbChannel4.getProgress()));
        intent.putExtra(ProfileActivity.EXTRA_PROFILE_CHB, Parser.convertChannelToOriginalValue(sbBrightness.getProgress()));
        getActivity().startActivity(intent);
    }

    public void setSelectedProfile(Profile profile) {
        activeProfile = profile;
        tvSelectedProfile.setText(getString(R.string.profileSelected) + " " + profile.getName());
        imgSaveProfile.setVisibility(View.VISIBLE);
        imgResetProfile.setVisibility(View.VISIBLE);
    }

    /**
     * Saves the currently position of the channel and brightness knobs to the
     * selected profile.
     */
    public void saveProfile() {
        ProfileViewModel viewModel = new ProfileViewModel(getActivity().getApplication());
        Profile profile = activeProfile;
        profile.setChannel1(Parser.convertChannelToOriginalValue(sbChannel1.getProgress()));
        profile.setChannel2(Parser.convertChannelToOriginalValue(sbChannel2.getProgress()));
        profile.setChannel3(Parser.convertChannelToOriginalValue(sbChannel3.getProgress()));
        profile.setChannel4(Parser.convertChannelToOriginalValue(sbChannel4.getProgress()));
        profile.setBrightness(Parser.convertChannelToOriginalValue(sbBrightness.getProgress()));
        viewModel.update(profile);
        Snackbar snackbar = Snackbar.make(listView, getString(R.string.profileSavedSuccess), Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.greenSuccess));
        snackbar.show();
    }

    /**
     * Resets the selected profile to it's original saved values.
     */
    private void resetProfile() {
        if(activeProfile != null) {
            sbChannel1.setProgress(Parser.convertChannelToPercent(activeProfile.getChannel1()));
            sbChannel2.setProgress(Parser.convertChannelToPercent(activeProfile.getChannel2()));
            sbChannel3.setProgress(Parser.convertChannelToPercent(activeProfile.getChannel3()));
            sbChannel4.setProgress(Parser.convertChannelToPercent(activeProfile.getChannel4()));
            sbBrightness.setProgress(Parser.convertChannelToPercent(activeProfile.getBrightness()));
        }
    }

    /**
     * Formats the values of the channels and returns a hexadecimal string
     * @param channels All the LED channels
     * @return A correct string which can be send to an EV-Board
     */
    private String formatOutput(Integer... channels) {
        int channelNr = 0;
        StringBuilder sb = new StringBuilder();
        for (int i : channels) {
            if (channelNr < 10) {
                sb.append("0401000" + Integer.toHexString(channelNr) + Parser.convertToHexValue(i));
            } else if (channelNr < 100) {
                sb.append("040100" + Integer.toHexString(channelNr) + Parser.convertToHexValue(i));
            }
            channelNr++;
        }
        return sb.toString();
    }

    private void initSeekBars(View view) {
        sbBrightness = view.findViewById(R.id.sb_brightness);
        sbChannel1 = view.findViewById(R.id.sb_channel1);
        sbChannel2 = view.findViewById(R.id.sb_channel2);
        sbChannel3 = view.findViewById(R.id.sb_channel3);
        sbChannel4 = view.findViewById(R.id.sb_channel4);
        setSeekBarChangeListener();

        if (START) {
            enableSeekBars(false);
            START = false;
        }
    }

    private void setSeekBarChangeListener() {
        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvChannelBrightPerc.setText(String.valueOf(progress) + " %");
                tvChannel1Perc.setText(String.valueOf(Parser.convertSeekBarValue(sbChannel1.getProgress(), progress)) + " %");
                tvChannel2Perc.setText(String.valueOf(Parser.convertSeekBarValue(sbChannel2.getProgress(), progress)) + " %");
                tvChannel3Perc.setText(String.valueOf(Parser.convertSeekBarValue(sbChannel3.getProgress(), progress)) + " %");
                tvChannel4Perc.setText(String.valueOf(Parser.convertSeekBarValue(sbChannel4.getProgress(), progress)) + " %");
                if (!firstReceive) {
                    sendChannelInfo();
                    if (controlGlobally) {
                        controlAllChannels(sbBrightness.getProgress(), sbChannel1.getProgress(), sbChannel2.getProgress(), sbChannel3.getProgress(), sbChannel4.getProgress());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sbChannel1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvChannel1Perc.setText(String.valueOf(Parser.convertSeekBarValue(progress, sbBrightness.getProgress())) + " %");
                if (!firstReceive) {
                    sendChannelInfo();
                    if (controlGlobally) {
                        controlAllChannels(sbBrightness.getProgress(), sbChannel1.getProgress(), sbChannel2.getProgress(), sbChannel3.getProgress(), sbChannel4.getProgress());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sbChannel2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvChannel2Perc.setText(String.valueOf(Parser.convertSeekBarValue(progress, sbBrightness.getProgress())) + " %");
                if (!firstReceive) {
                    sendChannelInfo();
                    if (controlGlobally) {
                        controlAllChannels(sbBrightness.getProgress(), sbChannel1.getProgress(), sbChannel2.getProgress(), sbChannel3.getProgress(), sbChannel4.getProgress());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sbChannel3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvChannel3Perc.setText(String.valueOf(Parser.convertSeekBarValue(progress, sbBrightness.getProgress())) + " %");
                if (!firstReceive) {
                    sendChannelInfo();
                    if (controlGlobally) {
                        controlAllChannels(sbBrightness.getProgress(), sbChannel1.getProgress(), sbChannel2.getProgress(), sbChannel3.getProgress(), sbChannel4.getProgress());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sbChannel4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvChannel4Perc.setText(String.valueOf(Parser.convertSeekBarValue(progress, sbBrightness.getProgress())) + " %");
                if (!firstReceive) {
                    sendChannelInfo();
                    if (controlGlobally) {
                        controlAllChannels(sbBrightness.getProgress(), sbChannel1.getProgress(), sbChannel2.getProgress(), sbChannel3.getProgress(), sbChannel4.getProgress());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Gets called when user wants to control all connected
     * devices at once
     * @param brightness
     * @param channel1
     * @param channel2
     * @param channel3
     * @param channel4
     */
    private void controlAllChannels(int brightness, int channel1, int channel2, int channel3, int channel4) {
        for (DeviceInstanceFragment dif : ControlFragment.getFragment().getDeviceInstances()) {
            if (!dif.equals(this)) {
                dif.setChannelProgress(brightness, channel1, channel2, channel3, channel4);
                dif.sendChannelInfo();
            }
        }
    }

    public void setChannelProgress(int brightness, int channel1, int channel2, int channel3, int channel4) {
        sbBrightness.setProgress(brightness);
        sbChannel1.setProgress(channel1);
        sbChannel2.setProgress(channel2);
        sbChannel3.setProgress(channel3);
        sbChannel4.setProgress(channel4);
    }

    /**
     * Adds all the collected orders to an ArrayList
     */
    public void sendChannelInfo() {
        String s = formatOutput(sbBrightness.getProgress(), sbChannel1.getProgress(), sbChannel2.getProgress(), sbChannel3.getProgress(), sbChannel4.getProgress());
        orderList.add(s);
    }

    /**
     * Schedules the timing when new data will be send over
     * to the connected device
     */
    private void startSendingTask() {
        sendingThread = new SendingThread(orderList, mManager);
        sendingThread.start();
    }

    /**
     * Ends the SendingThread and disables the SeekBars
     */
    private void endSendingTask() {
        if (task != null) {
            sendingThread.stopThread();
            enableSeekBars(false);
        }
    }

    /**
     * Receives the answers of {@link UARTManager.DataHandler#onDataReceived} method
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(UARTService.EXTRA_DEVICE);
            if (mDevice != null && mDevice.getAddress().equals(device.getAddress())) {
                String tmp = intent.getStringExtra(UARTService.EXTRA_DATA);

                // Format String to hex byte and save them into byte array
                byte[] bytes = new byte[CHANNEL_BYTE_LENGTH * 5];
                int index = 0;
                for(int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) Integer.parseInt(tmp.substring(index, index+2), 16);
                    index += 2;
                }

                // Control if byte length is n*5 byte Payload
                if (bytes.length / 5 != CHANNEL_BYTE_LENGTH) {
                    Toast.makeText(getActivity(), getString(R.string.payloadError), Toast.LENGTH_SHORT).show();
                    ControlFragment.getFragment().onDeviceDisconnected(device);
                    Log.d("SERVICE_WE", "WRONG PAYLOAD LENGTH");
                    return;
                }

                ArrayList<Integer> channelValues = getInputValues(bytes);

                // Only set seek bar values the first time the device connects
                if (firstReceive) {
                    sbBrightness.setProgress(Parser.convertChannelToPercent(channelValues.get(0)));
                    sbChannel1.setProgress(Parser.convertChannelToPercent(channelValues.get(1)));
                    sbChannel2.setProgress(Parser.convertChannelToPercent(channelValues.get(2)));
                    sbChannel3.setProgress(Parser.convertChannelToPercent(channelValues.get(3)));
                    sbChannel4.setProgress(Parser.convertChannelToPercent(channelValues.get(4)));
                    firstReceive = false;
                    enableSeekBars(true);
                }
            }
        }
    };

    /**
     * Catches all the results of the Dialogs
     * @param requestCode The operation which is requested
     * @param resultCode The result of the operation
     * @param data Optional data of the result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_MTU && resultCode == Activity.RESULT_OK) { // MTU_REQ
            int mtu = data.getIntExtra(EXTRA_MTU, 29);
            requestMtu(mtu);
        }
        else if (requestCode == REQ_PHY && resultCode == Activity.RESULT_OK) { // PHY_REQ
            preferredPhy = data.getIntExtra(EXTRA_PHY, PhyResult.PHY_LE_1M);
            int txPhy = preferredPhy;
            int rxPhy = preferredPhy;
            preferredOption = data.getIntExtra(EXTRA_PHY_OPTIONS, PhyRequest.PHY_OPTION_NO_PREFERRED);
            setPhy(txPhy, rxPhy, preferredOption);
        }
    }

    public void requestMtu(int mtu) {
        mManager.setMtu(mtu).enqueue();
    }

    public void getMtu() {
        int mtu = mManager.readMtu();
        mManager.log(android.util.Log.INFO, "MTU read: " + mtu);
    }

    public void readRssi() {
        mManager.getRssi().enqueue();
    }

    public void setPhy(int txPhy, int rxPhy, int phyOptions) {
        mManager.setPhy(txPhy, rxPhy, phyOptions).enqueue();
    }

    public void readPhy() {
        mManager.getPhy().enqueue();
    }

    public int getPreferredPhy() {
        return this.preferredPhy;
    }

    public int getPreferredOption() {
        return this.preferredOption;
    }

    /**
     * @param bytes The byte array of the received channel values
     * @return The ArrayList with all transformed Integer values for the Seek bar
     */
    private ArrayList<Integer> getInputValues(byte[] bytes) {
        ArrayList<Integer> list = new ArrayList<>();
        int end = 5;
        // Run over array and split it into the channel values à 5 bytes
        for (int i = 0; i <= bytes.length; ) {
            byte[] b = Arrays.copyOfRange(bytes, i, end);
            int channelValue = Parser.getPWMValue(b);
            list.add(channelValue);
            i += 5;
            end += 5;
        }
        return list;
    }

    /**
     * Shows the settings for the demo device
     */
    public void onDemoDeviceConnected() {
        startSendingTask();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                enableSeekBars(true);
                firstReceive = false;
            }
        }, 1000);
    }

    /**
     * Shows the settings for the device
     */
    public void onDeviceConnected() {
        startSendingTask();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                enableSeekBars(true);
                firstReceive = false;
            }
        }, 3000);
    }

    /**
     * Enables or disables all the seek bars
     *
     * @param value The boolean variable
     */
    public void enableSeekBars(boolean value) {
        sbBrightness.setEnabled(value);
        sbChannel1.setEnabled(value);
        sbChannel2.setEnabled(value);
        sbChannel3.setEnabled(value);
        sbChannel4.setEnabled(value);
    }

    public void onDeviceDisconnected() {
        firstReceive = true;
        endSendingTask();
        if (mLogSession != null && mLogSession instanceof LocalLogSession) {
            LocalLogSession session = (LocalLogSession) mLogSession;
            session.delete();
            Log.d(CustomLogger.TAG, "LocalLogSession deleted");
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        switch (id) {
            case LOG_REQUEST_ID: {
                return new CursorLoader(requireContext(), mLogSession.getSessionEntriesUri(), LOG_PROJECTION, logFilter, null, LogContract.Log.TIME);
            }
        }
        throw new UnsupportedOperationException("Could not create loader with ID " + id);
    }

    @Override
    public void onLoadFinished(@NonNull final Loader<Cursor> loader, final Cursor data) {
        // Here we have to restore the old saved scroll position, or scroll to the bottom if before adding new events it was scrolled to the bottom.
        final ListView list = this.listView;
        final int position = mLogScrollPosition;
        final boolean scrolledToBottom = position == LOG_SCROLLED_TO_BOTTOM || (list.getCount() > 0 && list.getLastVisiblePosition() == list.getCount() - 1);

        mLogAdapter.swapCursor(data);
        if (mLogAdapter.isEmpty()) {
            listView.setVisibility(View.GONE);
            noEntries.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            noEntries.setVisibility(View.GONE);
        }

        if (position > LOG_SCROLL_NULL) {
            list.setSelectionFromTop(position, 0);
        } else {
            if (scrolledToBottom)
                list.setSelection(list.getCount() - 1);
        }
        mLogScrollPosition = LOG_SCROLL_NULL;
        scrollToBottom();
    }

    @Override
    public void onLoaderReset(@NonNull final Loader<Cursor> loader) {
        mLogAdapter.swapCursor(null);
    }

    private void scrollToBottom() {
        this.listView.post(new Runnable() {
            @Override
            public void run() {
                listView.smoothScrollToPosition(mLogAdapter.getCount() - 1);
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.PREF_CHANGE_VIEW)) {
            int option = sharedPreferences.getInt(Preferences.PREF_CHANGE_VIEW, 0);
            if (option == 0) {
                scrollView.setVisibility(View.VISIBLE);
                terminalContent.setVisibility(View.GONE);
            } else if (option == 1) {
                scrollView.setVisibility(View.GONE);
                terminalContent.setVisibility(View.VISIBLE);
            }
        } else if (key.equals(Preferences.PREF_GLOBAL_CONTROL)) {
            boolean checked = sharedPreferences.getBoolean(Preferences.PREF_GLOBAL_CONTROL, false);
            if (checked) {
                controlGlobally = true;
                tvSendingInfo.setVisibility(View.VISIBLE);
            } else {
                controlGlobally = false;
                tvSendingInfo.setVisibility(View.GONE);
            }
        }
    }
}
