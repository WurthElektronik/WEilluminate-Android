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

package com.eisos.android.customLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class CustomViewPager extends ViewPager {

    private boolean swipingLocked;

    public CustomViewPager(@NonNull Context context, AttributeSet set) {
        super(context, set);
        swipingLocked = false;
    }

    public void setSwipingLocked(boolean value) {
        this.swipingLocked = !value;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return swipingLocked && super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return swipingLocked && super.onTouchEvent(ev);
    }

    @Override
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        return swipingLocked && super.executeKeyEvent(event);
    }
}
