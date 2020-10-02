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
package com.eisos.android.bluetooth.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.eisos.android.MainActivity;
import com.eisos.android.R;
import com.eisos.android.bluetooth.ServerManager;
import com.eisos.android.bluetooth.UARTManager;
import com.eisos.android.frags.ControlFragment;
import com.eisos.android.frags.ScanFragment;
import com.eisos.android.utils.CustomLogger;

import no.nordicsemi.android.ble.callback.FailCallback;
import no.nordicsemi.android.ble.observer.ConnectionObserver;
import no.nordicsemi.android.ble.observer.ServerObserver;

public class UARTService extends BleMulticonnectProfileService implements ServerObserver {

    public static final String BROADCAST_UART_TX = "com.eisos.android.ble.BROADCAST_UART_TX";
    public static final String BROADCAST_UART_RX = "com.eisos.android.ble.BROADCAST_UART_RX";
    /**
     * A broadcast message with this action is triggered when a message is received
     * from the UART device.
     */
    public static final String EXTRA_DATA = "com.eisos.android.ble.EXTRA_DATA";
    public static final String CHANNEL_ID = "com.eisos.android.ble.ForegroundServiceChannel";
    private SharedPreferences sharedPrefs;
    private NotificationManager manager;
    private ServerManager serverManager;
    private Handler mHandler;
    private long connStart, connEnd;
    private int minConnTime = 3000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler = new Handler(getMainLooper());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected UARTManager initializeManager() {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        UARTManager manager = new UARTManager(this);
        manager.useServer(serverManager);
        return manager;
    }

    @Override
    protected void onServiceCreated() {
        super.onServiceCreated();
        serverManager = new ServerManager(getApplicationContext());
        serverManager.setServerObserver(this);
    }

    @Override
    protected void onServiceStopped() {
        // Close the GATT server. If it hasn't been opened this method does nothing
        serverManager.close();
        serverManager = null;
        super.onServiceStopped();
    }

    @Override
    public void onServerReady() {
        // This will start reconnecting to devices that will previously connected.
        super.onBluetoothEnabled();
    }

    @Override
    public void onDeviceConnectedToServer(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnectedFromServer(@NonNull BluetoothDevice device) {

    }

    @Override
    protected void onBluetoothEnabled() {
        // First, open the server. onServerReady() will be called when all services were added.
        serverManager.open();
    }

    @Override
    protected void onBluetoothDisabled() {
        super.onBluetoothDisabled();
        // Close the GATT server
        serverManager.close();
    }

    @Override
    protected void onRebind() {
        deleteBackgroundNotificationChannel();
    }

    @Override
    protected void onUnbind() {
        createBackgroundNotificationChannel();
    }

    /**
     * Create Notification Service
     */
    private void createBackgroundNotificationChannel() {
        createChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.serviceContentTitle))
                .setContentText(getString(R.string.serviceContentText))
                .setSmallIcon(R.drawable.ic_alert)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    /**
     * Delete the Notification Channel
     */
    private void deleteBackgroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager != null) {
                manager.deleteNotificationChannel(CHANNEL_ID);
            }
        }
    }

    /**
     * Create Notification Channel
     */
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "WEilluminate Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null, null);

            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        super.onDeviceConnected(device);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ControlFragment.getFragment().onDeviceConnected(device);
            }
        });
        connStart = System.currentTimeMillis();
        Log.d(CustomLogger.TAG, "DEVICE CONNECTED");
    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        switch (reason) {
            case FailCallback.REASON_TIMEOUT:
                showToast(device.getAddress() + "\n" + getString(R.string.connectionTimedOut));
                break;
            case FailCallback.REASON_REQUEST_FAILED:
                showToast(device.getAddress() + "\n" + getString(R.string.errorRequestFailed));
                break;
            case FailCallback.REASON_DEVICE_NOT_SUPPORTED:
                showToast(getString(R.string.deviceNotSupported) + "\n" + device.getAddress());
                ControlFragment.getFragment().onDeviceDisconnected(device);
                break;
            case FailCallback.REASON_BLUETOOTH_DISABLED:
                ScanFragment.getFragment().onDeviceDisconnected(device);
                showToast(getString(R.string.bluetoothNotEnabled));
                break;
        }
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device, int reason) {
        super.onDeviceDisconnected(device, reason);
        switch (reason) {
        case ConnectionObserver.REASON_SUCCESS:
            closeTerminalInstance(device);
            Log.d(CustomLogger.TAG, "DEVICE DISCONNECTED");
            break;
        case ConnectionObserver.REASON_TIMEOUT:
            closeTerminalInstance(device);
            showToast(device.getName() + " " + getString(R.string.deviceDisconnected));
            Log.d(CustomLogger.TAG, "DEVICE DISCONNECTED BY TIMEOUT");
            break;
        case ConnectionObserver.REASON_TERMINATE_LOCAL_HOST:
            closeTerminalInstance(device);
            Log.d(CustomLogger.TAG, "DEVICE DISCONNECTED BY LOCAL HOST");
            break;
        case ConnectionObserver.REASON_TERMINATE_PEER_USER:
            closeTerminalInstance(device);
            Log.d(CustomLogger.TAG, "DEVICE DISCONNECTED BY PEER USER");
            break;
        case ConnectionObserver.REASON_LINK_LOSS:
            onDeviceLinkLoss(device);
            Log.d(CustomLogger.TAG, "DEVICE CONNECTION LOST, TRY TO RECONNECT...");
            break;
        case ConnectionObserver.REASON_UNKNOWN:
            closeTerminalInstance(device);
            Log.d(CustomLogger.TAG, "DEVICE DISCONNECTED BY UNKNOWN ERROR");
            break;
    }
    // Measure time between connect and disconnect. If the connection lasts shorter than minConnTime
    // it can be assumed, that the device does not support a MTU bigger than 21. The app is therefore
    // not usable with this device.
    connEnd = System.currentTimeMillis();
    long result = (connEnd - connStart);
    if(result <= minConnTime) {
        showLongToast(getString(R.string.mtuNotSupported));
        Log.d(CustomLogger.TAG, "Connection time of " + device.getAddress() + "= " + result/1000 + " sec.");
    }
}

    private void closeTerminalInstance(BluetoothDevice device) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ControlFragment.getFragment().onDeviceDisconnected(device);
            }
        });
    }

    private void onDeviceLinkLoss(BluetoothDevice device) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ControlFragment.getFragment().onDeviceDisconnected(device);
                showToast(getString(R.string.errorDeviceConnectionLost) +
                        " " + device.getAddress() + "!");
            }
        });
    }
}
