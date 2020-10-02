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

package com.eisos.android.frags.threads;


import android.os.Handler;

import com.eisos.android.bluetooth.UARTManager;

import java.util.ArrayList;

/**
 * This class handles the sending of commands.
 */
public class SendingThread extends Thread {

    private static final int SEND_INTERVAL = 250; // in ms
    private Handler handler;
    private Runnable task;
    private ArrayList<String> orderList;
    private UARTManager mManager;
    private boolean stopThread;

    public SendingThread(ArrayList orderList, UARTManager manager) {
        handler = new Handler();
        this.orderList = orderList;
        this.mManager = manager;
    }

    public void stopThread() {
        stopThread = true;
    }

    @Override
    public void run() {
        task = new Runnable() {
            @Override
            public void run() {
                if(stopThread) {
                    return;
                }

                // Check if a message is in the queue, if true send the last command of the list
                // to the connected device otherwise do nothing
                if (!orderList.isEmpty()) {
                    String command = orderList.get(orderList.size() - 1);
                    if (mManager != null) {
                        mManager.send(command);
                    }
                    orderList.clear();
                }
                handler.postDelayed(this, SEND_INTERVAL);
            }
        };
        handler.post(task);
    }
}
