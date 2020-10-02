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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.eisos.android.R;
import com.eisos.android.frags.DeviceInstanceFragment;

public class OverwriteProfileDialog extends DialogFragment {

    private DeviceInstanceFragment deviceInstanceFragment;

    public OverwriteProfileDialog(DeviceInstanceFragment instance) {
        this.deviceInstanceFragment = instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);
        builder.setIcon(R.drawable.ic_alert);
        builder.setTitle(getString(R.string.overwriteProfileTitle));
        builder.setMessage(getString(R.string.overwriteProfileMsg));
        builder.setPositiveButton(R.string.dialogBtnOk, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deviceInstanceFragment.saveProfile();
                dismiss();
            }
        });
        builder.setNegativeButton(R.string.dialogBtnCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        return builder.create();
    }
}
