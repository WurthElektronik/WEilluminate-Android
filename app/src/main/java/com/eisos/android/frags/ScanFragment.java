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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.eisos.android.MainActivity;
import com.eisos.android.R;
import com.eisos.android.adapter.ItemAdapter;
import com.eisos.android.bluetooth.UARTLocalLogContentProvider;
import com.eisos.android.bluetooth.services.BleMulticonnectProfileService;
import com.eisos.android.bluetooth.services.UARTService;
import com.eisos.android.customLayout.ScanListItem;
import com.eisos.android.database.favourites.FavouriteDevice;
import com.eisos.android.database.favourites.FavouriteViewModel;
import com.eisos.android.dialogs.ReqPhyStartDialog;
import com.eisos.android.utils.CustomLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.response.PhyResult;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LocalLogSession;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScanFragment<E extends BleMulticonnectProfileService.LocalBinder> extends Fragment implements OnDeviceSelectedListener {

    public static final String TAG = "ScanFragment";
    private SwipeRefreshLayout srLayout;
    private RecyclerView recyclerView;
    private Spinner sortSpinner;
    private ItemAdapter itemAdapter;
    private BluetoothLeScannerCompat bleScanner;
    private TextView tvScan;
    private ProgressBar progressBar;
    private SharedPreferences sharedPrefs;
    private final static ParcelUuid mUuid = ParcelUuid.fromString("6E400001-C352-11E5-953D-0002A5D5C51B");
    private ScanListItem demoDevice;
    public static final String DEMO_ADDRESS = "-";
    public static final String DEMO_NAME = "Demo Device";
    private static ScanFragment scanFragment;
    private ScanCallback scanCallback, scanCallback2;
    private ScanSettings settings, settings2;
    private boolean isScanning = true;
    protected static final int REQUEST_ENABLE_BT = 1;
    private static String SORT_FILTER;
    private static final String sortByDefault = "Default";
    private static final String sortByName = "Sort by Name";
    private static final String sortByAddress = "Sort by Address";
    private static final String sortByRSSI = "Sort by RSSI";
    private Intent service;
    private E mBinder;
    private List<BluetoothDevice> mManagedDevices;
    public final static int MAX_ALLOWED_CONS = 5;
    public static final int REQ_PHY = 11;
    public static final String EXTRA_PHY = "com.eisos.android_terminal.bluetooth.EXTRA_PHY";
    public static final String EXTRA_PHY_OPTIONS = "com.eisos.android_terminal.bluetooth.EXTRA_PHY_OPTIONS";
    private List<ScanListItem> connectedDevices;
    private FavouriteViewModel mFavModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        scanFragment = this;
        mManagedDevices = new ArrayList<>();
        connectedDevices = new ArrayList<>();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        itemAdapter = new ItemAdapter(getActivity().getApplicationContext(), this);
        SORT_FILTER = sortByDefault;
        mFavModel = new ViewModelProvider(this).get(FavouriteViewModel.class);
        initView(view);
        // Check if device supports Ble
        checkBleSupport();
        /*
         * In comparison to BleProfileServiceReadyActivity this activity always starts the service when started.
         * Connecting to a device is done by calling mBinder.connect(BluetoothDevice) method, not startService(...) like there.
         */
        // Start Service
        service = new Intent(getActivity(), UARTService.class);
        getActivity().startService(service);
        bindActivityToService(getActivity());
        scan();
        return view;
    }

    protected void showBLEDialog() {
        final Intent enableBLE = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBLE, REQUEST_ENABLE_BT);
    }

    @SuppressLint("MissingPermission")
    protected boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    // Check if smartphone supports ble
    protected void checkBleSupport() {
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(),
                    getString(R.string.no_ble), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == getActivity().RESULT_OK) {
                if (service != null) {
                    getActivity().stopService(service);
                }
                // Check if device supports Ble
                checkBleSupport();
                /*
                 * In comparison to BleProfileServiceReadyActivity this activity always starts the service when started.
                 * Connecting to a device is done by calling mBinder.connect(BluetoothDevice) method, not startService(...) like there.
                 */
                // Start Service
                service = new Intent(getActivity(), UARTService.class);
                getActivity().startService(service);
                bindActivityToService(getActivity());
                scan();
            } else {
                new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(), getString(R.string.bluetoothNotEnabled), Toast.LENGTH_LONG).show());
            }
        } else if (requestCode == REQ_PHY) {
            if (resultCode == getActivity().RESULT_OK) {
                int preferredPhy = data.getIntExtra(EXTRA_PHY, PhyResult.PHY_LE_1M);
                int preferredOption = data.getIntExtra(EXTRA_PHY_OPTIONS, PhyRequest.PHY_OPTION_NO_PREFERRED);
                ScanListItem item = (ScanListItem) data.getSerializableExtra(ReqPhyStartDialog.EXTRA_DEVICE);
                mBinder.setDeviceSettings(item.getDevice(), false, preferredPhy, preferredOption);
                connectDevice(item);
            }
        }
    }

    /**
     * Service connection which gets started at the app start.
     * Holds the connection to the service.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            final E bleService = mBinder = (E) service;
            ControlFragment.getFragment().setService(mBinder);
            mManagedDevices.addAll(bleService.getManagedDevices());

            // and notify user if device is connected
            for (final BluetoothDevice device : mManagedDevices) {
                if (bleService.isConnected(device)) {
                    onDeviceConnected(device);
                }
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBinder = null;
            Log.d(CustomLogger.TAG, "Service disconnected");
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        bindActivityToService(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        stopScan();
        unbindActivityFromService(getActivity());
    }

    private void initView(View view) {
        srLayout = view.findViewById(R.id.refreshLayout);
        srLayout.setColorSchemeColors(getActivity().getResources().getColor(R.color.colorPrimary));
        srLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                scan();
                srLayout.setRefreshing(false);
            }
        });

        recyclerView = view.findViewById(R.id.recyclerView);
        // Disable animation for notifyItemChanged()
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        recyclerView.setAdapter(itemAdapter);

        tvScan = view.findViewById(R.id.tv_scan);
        tvScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning) {
                    stopScan();
                } else {
                    scan();
                }
            }
        });

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        sortSpinner = view.findViewById(R.id.spinner_sort);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sortOption = (String) parent.getItemAtPosition(position);
                sortList(sortOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public static ScanFragment getFragment() {
        return scanFragment;
    }

    public void bindActivityToService(Activity activity) {
        if (service != null) {
            activity.bindService(service, mServiceConnection, 0);
        }
    }

    public void unbindActivityFromService(Activity activity) {
        if (service != null) {
            activity.unbindService(mServiceConnection);
        }
    }

    /**
     * Start scan and add items to ArrayList and Layout
     */
    public void scan() {
        if (isScanning) {
            stopScan();
        }

        // Clear list with items
        itemAdapter.clearDevices();
        addDemoDevice();
        addConnectedDevices();

        if (!isBLEEnabled()) {
            showBLEDialog();
            return;
        }

        String[] permissions = MainActivity.getActivity().getPermissions();
        if (ContextCompat.checkSelfPermission(getActivity(), permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), permissions, 1);
        }

        progressBar.setVisibility(View.VISIBLE);
        addFavouriteDevices();
        tvScan.setText(R.string.stopScanning);
        initScan();
        isScanning = true;
        Log.d(CustomLogger.TAG, "SCAN STARTED");
    }

    private void addFavouriteDevices() {
        List<FavouriteDevice> devices = mFavModel.getAll();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bAdapter = bluetoothManager.getAdapter();

        if (devices != null) {
            for (FavouriteDevice device : devices) {
                BluetoothDevice bleDevice = bAdapter.getRemoteDevice(device.getDeviceAddress());
                // Check if device is connected
                // if true than add old ScanListItem to list
                if(connectedDevices.size() > 0) {
                    boolean found = false;
                    for (ScanListItem item : connectedDevices) {
                        if (item.getDeviceAddress().equals(bleDevice.getAddress())) {
                            itemAdapter.addListItem(item);
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        ScanListItem listItem = new ScanListItem(getContext(), bleDevice);
                        listItem.setDeviceName(device.getDeviceName());
                        itemAdapter.addListItem(listItem);
                    }
                } else {
                    ScanListItem listItem = new ScanListItem(getContext(), bleDevice);
                    listItem.setDeviceName(device.getDeviceName());
                    itemAdapter.addListItem(listItem);
                }
            }
        }
    }

    public void addDeviceToFavourites(ScanListItem item) {
        List<FavouriteDevice> devices = mFavModel.getAll();
        boolean inList = false;
        for (FavouriteDevice device : devices) {
            if (device.getDeviceAddress().equals(item.getDeviceAddress())) {
                inList = true;
                break;
            }
        }

        if (!inList) {
            mFavModel.insert(new FavouriteDevice(item.getDeviceName(), item.getDeviceAddress()));
            mFavModel.updateList();
            Log.d(CustomLogger.TAG, "Device " + item.getDeviceAddress() + " added to favourites");
        }
    }

    public void deleteDeviceFromFavourites(ScanListItem item) {
        List<FavouriteDevice> devices = mFavModel.getAll();
        FavouriteDevice deviceDelete = null;
        for (FavouriteDevice device : devices) {
            if (device.getDeviceAddress().equals(item.getDeviceAddress())) {
                deviceDelete = device;
                break;
            }
        }

        if (deviceDelete != null) {
            mFavModel.delete(deviceDelete);
            mFavModel.updateList();
            Log.d(CustomLogger.TAG, "Device " + deviceDelete.getDeviceAddress() + " deleted from favourites");
        }
    }

    public boolean isDeviceFavourite(ScanListItem item) {
        List<FavouriteDevice> devices = mFavModel.getAll();
        for (FavouriteDevice device : devices) {
            if (device.getDeviceAddress().equals(item.getDeviceAddress())) {
                return true;
            }
        }
        return false;
    }

    private void addConnectedDevices() {
        if (connectedDevices.size() > 0) {
            for (ScanListItem item : connectedDevices) {
                if (!isDeviceFavourite(item)) {
                    itemAdapter.addListItem(item);
                }
            }

            // Sort the ArrayList dependent of the sort filter
            sortList(SORT_FILTER);
        }
    }

    public void stopScan() {
        tvScan.setText(R.string.scan);
        progressBar.setVisibility(View.GONE);
        isScanning = false;
        if (scanCallback != null && scanCallback2 != null) {
            bleScanner.stopScan(scanCallback);
            bleScanner.stopScan(scanCallback2);
            itemAdapter.setAllItemsNotUpdating(true);
            scanCallback = null;
            scanCallback2 = null;
            Log.d(CustomLogger.TAG, "SCAN STOPPED");
        }
    }

    /**
     * Initializes the scan settings
     */
    private void initScan() {

        settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1000L)
                .setUseHardwareBatchingIfSupported(false)
                .build();
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
        scanCallback = new ScanCallback() {

            @Override
            public void onBatchScanResults(@NonNull List<ScanResult> results) {
                addDevices(results);
            }
        };

        // TODO It seems that the Callback gets not triggered under Android 10
        settings2 = new ScanSettings.Builder()
                .setLegacy(false)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                .setReportDelay(0L) // Show results directly
                .setUseHardwareBatchingIfSupported(false)
                .build();
        scanCallback2 = new ScanCallback() {

            @Override
            @SuppressLint("MissingPermission")
            public void onScanResult(int callbackType, @NonNull ScanResult result) {
                final ScanListItem scanListItem = new ScanListItem(getActivity(), result.getDevice());
                deviceNotAvailable(scanListItem);
                Log.d(CustomLogger.TAG, "MATCH_LOST");
            }

        };
        bleScanner = BluetoothLeScannerCompat.getScanner();

        bleScanner.startScan(filters, settings, scanCallback);
        bleScanner.startScan(filters, settings2, scanCallback2);
    }

    /**
     * Adds a fake device to the layout for testing purposes
     */
    private void addDemoDevice() {
        if (!connectedDevices.contains(demoDevice)) {
            demoDevice = new ScanListItem(getActivity().getApplicationContext(), DEMO_NAME, DEMO_ADDRESS);
            itemAdapter.addListItem(demoDevice);
        }
    }

    /**
     * Adds the scanned devices to the ArrayList.
     * If a device is already in the list, update the rssi value
     *
     * @param results the ScanResults which shall be added
     */
    private void addDevices(List<ScanResult> results) {
        for (ScanResult sr : results) {
            boolean inList = false;
            ScanListItem device = new ScanListItem(getActivity().getApplicationContext(), sr.getDevice());
            for (ScanListItem item : itemAdapter.getItems()) {
                if (item.getDeviceAddress().equals(device.getDeviceAddress())) {
                    item.setItemUpdateStatus(true);
                    item.setRSSI(sr.getRssi());
                    inList = true;
                    itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                    break;
                }
            }

            if (!inList) {
                device.setRSSI(sr.getRssi());
                itemAdapter.addListItem(device);
            }
        }

        // Sort the ArrayList dependent of the sort filter
        if (SORT_FILTER != sortByDefault) {
            sortList(SORT_FILTER);
        }
    }

    /**
     * Disables a device in the displayed ListView
     *
     * @param device The DeviceItem which is not available anymore
     */
    private void deviceNotAvailable(ScanListItem device) {
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(device.getDeviceAddress())) {
                item.setSignalOutOfRange();
                item.setItemUpdateStatus(false);
                Log.d(CustomLogger.TAG, "DEVICE SIGNAL LOST");
                itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                return;
            }
        }
    }

    /**
     * Simulates a ble connection with a fake device
     *
     * @param item the DemoDevice
     */
    public void connectDemoDevice(ScanListItem item) {
        stopScan();
        if (item.getConnectionState() == ScanListItem.CONNECTED) {
            MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_control);
            ControlFragment.getFragment().selectTab(item.getDeviceAddress());
            return;
        } else if (item.getConnectionState() == ScanListItem.CONNECTING) {
            return;
        }

        if (mBinder == null || mBinder.getManagedDevices().size() < MAX_ALLOWED_CONS) {
            ILogSession logSession = Logger.newSession(getActivity().getApplicationContext(), null, item.getDeviceAddress(), item.getDeviceName());
            // If nRF Logger is not installed we may want to use local logger
            if (logSession == null) {
                logSession = LocalLogSession.newSession(getActivity().getApplicationContext(), UARTLocalLogContentProvider.AUTHORITY_URI, item.getDeviceAddress(), item.getDeviceName());
                Log.d(CustomLogger.TAG, "LocalLogSession created");
            }
            final ILogSession logger = logSession;
            item.setConnectionState(ScanListItem.CONNECTING);
            itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
            Logger.logEntry(logger, LogContract.Log.Level.INFO, "Demo device connecting...");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    item.setConnectionState(ScanListItem.CONNECTED);
                    itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                    Logger.logEntry(logger, LogContract.Log.Level.INFO, "Demo device connected");
                    MainActivity.getActivity().getMenu().setGroupVisible(R.id.menu_group_one, true);
                    connectedDevices.add(item);
                    ControlFragment.getFragment().onDemoDeviceConnected(item.getDeviceName(), item.getDeviceAddress(), logger);
                }
            }, 250);
        } else {
            new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(), "You can max. connect to " + MAX_ALLOWED_CONS + " devices", Toast.LENGTH_SHORT).show());
            item.setConnectionState(ScanListItem.DISCONNECTED);
            itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
        }
    }

    /**
     * Builds a connection to a bluetooth device
     *
     * @param item The Device you want to connect to
     */
    public void connectDevice(ScanListItem item) {
        stopScan();
        if (item.getConnectionState() == ScanListItem.CONNECTED) {
            MainActivity.getActivity().getBtmNavView().setSelectedItemId(R.id.item_control);
            ControlFragment.getFragment().selectTab(item.getDeviceAddress());
            return;
        } else if (item.getConnectionState() == ScanListItem.CONNECTING) {
            return;
        }

        final ScanListItem device = item;
        device.setConnectionState(ScanListItem.CONNECTING);
        itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(device));
        if (mBinder != null && mBinder.getManagedDevices().size() < MAX_ALLOWED_CONS) {
            onDeviceSelected(item.getDevice());
        } else {
            new Handler().post(() -> Toast.makeText(getActivity().getApplicationContext(), "You can max. connect to " + MAX_ALLOWED_CONS + " devices", Toast.LENGTH_SHORT).show());
            device.setConnectionState(ScanListItem.DISCONNECTED);
            itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(device));
        }
    }

    public void onAutoConnectClicked(ScanListItem item) {
        mBinder.setDeviceSettings(item.getDevice(), true);
        connectDevice(item);
    }

    public void onPreferredPhyClicked(ScanListItem item) {
        ReqPhyStartDialog dialog = new ReqPhyStartDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ReqPhyStartDialog.EXTRA_DEVICE, item);
        dialog.setArguments(bundle);
        dialog.setTargetFragment(this, REQ_PHY);
        dialog.show(getFragmentManager(), "ReqPhyStartDialog");
    }

    /**
     * Gets called out of the service {@link UARTService#onDeviceConnected(BluetoothDevice)}
     * when a device has connected
     */
    public void onDeviceConnected(BluetoothDevice device) {
        MainActivity.getActivity().getMenu().setGroupVisible(R.id.menu_group_one, true);
        setDeviceConnectionState(device.getAddress(), ScanListItem.CONNECTED);
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(device.getAddress())) {
                connectedDevices.add(item);
                item.setMenuVisibility(false);
            }
        }
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        ILogSession logSession = Logger.newSession(getActivity().getApplicationContext(), null, device.getAddress(), device.getName());
        // If nRF Logger is not installed we may want to use local logger
        if (logSession == null) {
            logSession = LocalLogSession.newSession(getActivity().getApplicationContext(), UARTLocalLogContentProvider.AUTHORITY_URI, device.getAddress(), device.getName());
            Log.d(CustomLogger.TAG, "LocalLogSession created");
        }
        mBinder.connect(device, logSession);
    }

    /**
     * @return A list with the connected ScanListItems
     */
    public ArrayList<ScanListItem> getConnectedListItems() {
        ArrayList<ScanListItem> connectedItems = new ArrayList<>();
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getConnectionState() == ScanListItem.CONNECTED) {
                connectedItems.add(item);
            }
        }
        return connectedItems;
    }

    public ArrayList<ScanListItem> getScannedItems() {
        return this.itemAdapter.getItems();
    }

    /**
     * @return A list with the connected bluetooth devices
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (mBinder != null) {
            return mBinder.getConnectedDevices();
        }
        return null;
    }


    /**
     * Gets called out of the ControlFragment {@link ControlFragment#onDeviceDisconnected(BluetoothDevice)}
     * when a device has disconnected
     */
    public void onDeviceDisconnected(BluetoothDevice device) {
        for (ScanListItem item : itemAdapter.getItems()) {
            // Check if device != null because of demo device
            if (item.getDevice() != null && item.getDevice().getAddress().equals(device.getAddress())) {
                connectedDevices.remove(item);
                item.setConnectionState(ScanListItem.DISCONNECTED);
                item.setMenuVisibility(true);
                itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                break;
            }
        }
    }

    /**
     * Sets the connection state of the demo device
     * to not connected
     *
     * @param address The address of the demo device
     */
    public void onDemoDeviceDisconnected(String address) {
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(address)) {
                item.setConnectionState(ScanListItem.DISCONNECTED);
                itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
                break;
            }
        }
    }

    public E getService() {
        return this.mBinder;
    }

    /**
     * Set the current state of a device in the scanning list
     *
     * @param state The current state of the device in the list
     */
    public void setDeviceConnectionState(String address, int state) {
        for (ScanListItem item : itemAdapter.getItems()) {
            if (item.getDeviceAddress().equals(address)) {
                item.setConnectionState(state);
                itemAdapter.notifyItemChanged(itemAdapter.getItems().indexOf(item));
            }
        }
    }

    /**
     * Sorts the ArrayList with the detected devices
     *
     * @param sortOption The sorting parameter by which the ArrayList will be sorted
     */
    private void sortList(String sortOption) {
        switch (sortOption) {
            case sortByDefault:
                sortByDefault();
                break;
            case sortByName:
                sortByName();
                break;
            case sortByAddress:
                sortByAddress();
                break;
            case sortByRSSI:
                sortByRssi();
                break;
        }
    }

    private void sortByDefault() {
        SORT_FILTER = sortByDefault;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return String.valueOf(o1.getID()).compareTo(String.valueOf(o2.getID()));
            }
        });
        itemAdapter.notifyDataSetChanged();
    }

    private void sortByName() {
        SORT_FILTER = sortByName;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return o1.getDeviceName().compareTo(o2.getDeviceName());
            }
        });
        itemAdapter.notifyDataSetChanged();
    }

    private void sortByAddress() {
        SORT_FILTER = sortByAddress;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return o1.getDeviceAddress().compareTo(o2.getDeviceAddress());
            }
        });
        itemAdapter.notifyDataSetChanged();
    }

    private void sortByRssi() {
        SORT_FILTER = sortByRSSI;
        Collections.sort(itemAdapter.getItems(), new Comparator<ScanListItem>() {
            @Override
            public int compare(ScanListItem o1, ScanListItem o2) {
                return String.valueOf(o1.getRssi()).compareTo(String.valueOf(o2.getRssi()));
            }
        });
        itemAdapter.notifyDataSetChanged();
    }
}
