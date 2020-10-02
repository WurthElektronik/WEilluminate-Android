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
package com.eisos.android.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.eisos.android.frags.DeviceInstanceFragment;

import java.util.ArrayList;

public class DeviceInstancePagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<DeviceInstanceFragment> mFragments;

    public DeviceInstancePagerAdapter(ArrayList<DeviceInstanceFragment> fragments, FragmentManager fm) {
        super(fm);
        this.mFragments = fragments;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragments.get(position).getTabTitle();
    }

    @Override
    public DeviceInstanceFragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }
}
