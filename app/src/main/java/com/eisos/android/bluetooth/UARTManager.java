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
package com.eisos.android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eisos.android.R;
import com.eisos.android.bluetooth.interfaces.UARTManagerObserver;
import com.eisos.android.bluetooth.services.UARTService;
import com.eisos.android.utils.Parser;

import java.util.UUID;

import no.nordicsemi.android.ble.MtuRequest;
import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.ble.ReadRssiRequest;
import no.nordicsemi.android.ble.WriteRequest;
import no.nordicsemi.android.log.LogContract;

public class UARTManager extends LoggableBleManager {

    // Wuerth Electronic UUID's
    private final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-C352-11E5-953D-0002A5D5C51B");
    private final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-C352-11E5-953D-0002A5D5C51B");
    private final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-C352-11E5-953D-0002A5D5C51B");
    private final static byte AMBER_RF_HEADER_TYPE_DATA = 0x01;

    private BluetoothGattCharacteristic mRXCharacteristic, mTXCharacteristic;
    /**
     * A flag indicating whether Long Write can be used. It's set to false if the UART RX
     * characteristic has only PROPERTY_WRITE_NO_RESPONSE property and no PROPERTY_WRITE.
     * If you set it to false here, it will never use Long Write.
     * <p>
     * change this flag if you don't want to use Long Write even with Write Request.
     */
    private boolean mUseLongWrite = true;
    // Server characteristics
    private BluetoothGattCharacteristic serverCharacteristic;

    public UARTManager(final Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new UARTMangerGattCallback();
    }

    /**
     * BluetoothGatt callbacks for connection/disconnection, service discovery,
     * receiving indication, etc.
     */
    private class UARTMangerGattCallback extends BleManagerGattCallback {

        @Override
        protected void onServerReady(@NonNull BluetoothGattServer server) {
            // Obtain your server attributes.
            serverCharacteristic = server
                    .getService(UART_SERVICE_UUID)
                    .getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
        }

        @Override
        protected void initialize() {
            setNotificationCallback(mTXCharacteristic)
                    .with((device, data) -> {
                        String hex = Parser.bytesToHex(data.getValue());
                        log(LogContract.Log.Level.APPLICATION, "\"" + hex + "\" received");
                        new DataHandler().onDataReceived(device, hex);
                    });
            enableNotifications(mTXCharacteristic).enqueue();

        }

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(UART_SERVICE_UUID);
            if (service != null) {
                mRXCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID);
                mTXCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);
            }

            boolean writeRequest = false;
            boolean writeCommand = false;
            if (mRXCharacteristic != null) {
                final int rxProperties = mRXCharacteristic.getProperties();
                writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
                writeCommand = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;

                // Set the WRITE REQUEST type when the characteristic supports it.
                // This will allow to send long write (also if the characteristic support it).
                // In case there is no WRITE REQUEST property, this manager will divide texts
                // longer then MTU-3 bytes into up to MTU-3 bytes chunks.
                if (writeRequest)
                    mRXCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                else
                    mUseLongWrite = false;
            }

            return mRXCharacteristic != null && mTXCharacteristic != null && (writeRequest || writeCommand);
        }

        @Override
        protected void onDeviceDisconnected() {
            serverCharacteristic = null;
            mRXCharacteristic = null;
            mTXCharacteristic = null;
            mUseLongWrite = true;
        }
    }

    /**
     * Handles the request for data receiving and sending
     */
    public class DataHandler implements UARTManagerObserver {

        @Override
        public void onDataReceived(BluetoothDevice device, String data) {
            if (AMBER_RF_HEADER_TYPE_DATA == (byte)Integer.parseInt(data.substring(0, 2), 16)) {
                final Intent broadcast = new Intent(UARTService.BROADCAST_UART_RX);
                broadcast.putExtra(UARTService.EXTRA_DEVICE, device);
                broadcast.putExtra(UARTService.EXTRA_DATA, data.substring(2));
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
            }
        }

        @Override
        public void onDataSent(BluetoothDevice device, String data) {
            if (AMBER_RF_HEADER_TYPE_DATA == (byte)Integer.parseInt(data.substring(0, 2), 16)) {
                final Intent broadcast = new Intent(UARTService.BROADCAST_UART_TX);
                broadcast.putExtra(UARTService.EXTRA_DEVICE, device);
                broadcast.putExtra(UARTService.EXTRA_DATA, data.substring(2));
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(broadcast);
            }
        }
    }

    public PhyRequest setPhy(int txPhy, int rxPhy, int phyOptions) {
        return setPreferredPhy(txPhy, rxPhy, phyOptions);
    }

    public PhyRequest getPhy() {
        return readPhy();
    }

    public MtuRequest setMtu(int mMtu) {
        return requestMtu(mMtu);
    }

    public int readMtu() {
        return getMtu();
    }

    public ReadRssiRequest getRssi() {
        return readRssi();
    }

    /**
     * Sends the given text to RX characteristic.
     *
     * @param text the text to be sent
     */
    public void send(final String text) {
        // Are we connected?
        if (mRXCharacteristic == null)
            return;

        if (!TextUtils.isEmpty(text)) {
            WriteRequest request = null;

            try {
                request = writeCharacteristic(mRXCharacteristic, Parser.parseHexBinary(String.format("%02X", AMBER_RF_HEADER_TYPE_DATA) + text))
                        .with((device, data) -> {
                                log(LogContract.Log.Level.APPLICATION,
                                        "\"" + Parser.bytesToHex(data.getValue()) + "\" sent");
                            });
            } catch (IllegalArgumentException e) {
                new Handler().post(() -> Toast.makeText(getContext(), R.string.hexParsingError, Toast.LENGTH_SHORT).show());
                return;
            }

            if (!mUseLongWrite) {
                // This will automatically split the long data into MTU-3-byte long packets.
                request.split();
            }
            request.enqueue();
        }
    }
}
