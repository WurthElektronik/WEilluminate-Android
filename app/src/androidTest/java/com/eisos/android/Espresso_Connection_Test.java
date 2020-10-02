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

package com.eisos.android;

import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/**
 * Before starting the tests the app should have been started at least one time,
 * bluetooth should be enabled and the location permission granted
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class Espresso_Connection_Test {

    @Rule
    public ActivityTestRule<SplashActivity> activityRule =
            new ActivityTestRule<>(SplashActivity.class);

    @Test
    public void demoDevice_Connection_Disconnection() {
        // Waiting for SplashActivity to end
        sleep(1000);
        onView(withText("Demo Device")).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        // Set value channel1
        onView(withId(R.id.sb_channel1)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_LEFT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel1_perc)).check(matches(withText("0 %")));
        sleep(1000);
        // Set value channel2
        onView(withId(R.id.sb_channel2)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel2_perc)).check(matches(withText("25 %")));
        sleep(1000);
        // Set value channel3
        onView(withId(R.id.sb_channel3)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_RIGHT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel3_perc)).check(matches(withText("50 %")));
        sleep(1000);
        // Set value channel4
        onView(withId(R.id.sb_channel4)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_LEFT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel4_perc)).check(matches(withText("0 %")));
        sleep(1000);
        // Set value channelBrightness
        onView(withId(R.id.sb_brightness)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_RIGHT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channelBright_perc)).check(matches(withText("100 %")));
        onView(withId(R.id.tv_channel1_perc)).check(matches(withText("0 %")));
        onView(withId(R.id.tv_channel2_perc)).check(matches(withText("50 %")));
        onView(withId(R.id.tv_channel3_perc)).check(matches(withText("100 %")));
        onView(withId(R.id.tv_channel1_perc)).check(matches(withText("0 %")));
        sleep(2000);
        onView(withId(R.id.menu_switch_view)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.menu_switch_view)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.menu_disconnect)).check(matches(isDisplayed())).perform(click());
        sleep(1000);
    }

    /**
     * Only works if device name = Prot3
     */
    @Test
    public void proteus3_Connection_Disconnection() {
        // Waiting for SplashActivity to end and Proteus3 to be scanned
        sleep(3000);
        onView(withText("Prot3")).check(matches(isDisplayed())).perform(click());
        sleep(4500); // Wait for device answer
        // Set value channel1
        onView(withId(R.id.sb_channel1)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_LEFT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel1_perc)).check(matches(withText("0 %")));
        sleep(1000);
        // Set value channel2
        onView(withId(R.id.sb_channel2)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel2_perc)).check(matches(withText("25 %")));
        sleep(1000);
        // Set value channel3
        onView(withId(R.id.sb_channel3)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_RIGHT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel3_perc)).check(matches(withText("50 %")));
        sleep(1000);
        // Set value channel4
        onView(withId(R.id.sb_channel4)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_LEFT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channel4_perc)).check(matches(withText("0 %")));
        sleep(1000);
        // Set value channelBrightness
        onView(withId(R.id.sb_brightness)).perform(new GeneralClickAction(Tap.SINGLE,
                GeneralLocation.CENTER_RIGHT, Press.FINGER, 0, 0));
        onView(withId(R.id.tv_channelBright_perc)).check(matches(withText("100 %")));
        onView(withId(R.id.tv_channel1_perc)).check(matches(withText("0 %")));
        onView(withId(R.id.tv_channel2_perc)).check(matches(withText("50 %")));
        onView(withId(R.id.tv_channel3_perc)).check(matches(withText("100 %")));
        onView(withId(R.id.tv_channel1_perc)).check(matches(withText("0 %")));
        sleep(2000);
        onView(withId(R.id.menu_switch_view)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.menu_switch_view)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
        onView(withId(R.id.menu_disconnect)).check(matches(isDisplayed())).perform(click());
        sleep(2000);
    }


    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
