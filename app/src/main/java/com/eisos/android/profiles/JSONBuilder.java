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

package com.eisos.android.profiles;

import com.eisos.android.database.profiles.Profile;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONBuilder {

    public static final String PROFILE_NAME = "profile_name";
    public static final String PROFILE_DESC = "profile_desc";
    public static final String PROFILE_PANEL = "profile_panel";
    public static final String PROFILE_CHANNEL1 = "profile_channel1";
    public static final String PROFILE_CHANNEL2 = "profile_channel2";
    public static final String PROFILE_CHANNEL3 = "profile_channel3";
    public static final String PROFILE_CHANNEL4 = "profile_channel4";
    public static final String PROFILE_BRIGHTNESS = "profile_brightness";
    public static final String FILE_NAME = "WEilluminate_profile_export.json";
    public static final String FOLDER_NAME = "WEilluminate";
    public static final String FILE_IDENTIFIER = "id";
    public static final String FILE_IDENTIFIER_EXTRA = "com.eisos.android.profile.json";

    /**
     * Adds a file identifier to the beginning of the JSON file.
     * This method should be called before any other method.
     * @return the file identifier as JSONObject
     */
    public static JSONObject buildFileIdentifier() {
        try {
            JSONObject obj = new JSONObject();
            obj.put(FILE_IDENTIFIER, FILE_IDENTIFIER_EXTRA);
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Builds a profile entry as JSONObject.
     * @param profile the profile which should be saved
     * @return the profile as JSONObject
     */
    public static JSONObject buildProfileEntry(Profile profile) {
        try {
            JSONObject obj = new JSONObject();
            obj.put(PROFILE_NAME, profile.getName());
            obj.put(PROFILE_DESC, profile.getDescription());
            obj.put(PROFILE_PANEL, profile.getPanelType());
            obj.put(PROFILE_CHANNEL1, profile.getChannel1());
            obj.put(PROFILE_CHANNEL2, profile.getChannel2());
            obj.put(PROFILE_CHANNEL3, profile.getChannel3());
            obj.put(PROFILE_CHANNEL4, profile.getChannel4());
            obj.put(PROFILE_BRIGHTNESS, profile.getBrightness());
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converts a JSONObject to a Profile.class object.
     * @param obj the JSONObject which to convert
     * @param position the position of the JSONObject in the list
     * @return the JSONObject converted to a profile
     */
    public static Profile convertJSONToProfile(JSONObject obj, int position) {
        try {
            String name = obj.getString(PROFILE_NAME);
            String desc = obj.getString(PROFILE_DESC);
            String panel = obj.getString(PROFILE_PANEL);
            int channel1 = obj.getInt(PROFILE_CHANNEL1);
            int channel2 = obj.getInt(PROFILE_CHANNEL2);
            int channel3 = obj.getInt(PROFILE_CHANNEL3);
            int channel4 = obj.getInt(PROFILE_CHANNEL4);
            int brightness = obj.getInt(PROFILE_BRIGHTNESS);
            Profile profile = new Profile(name, null, position,
                    channel1, channel2, channel3, channel4, brightness);
            profile.setDescription(desc);
            profile.setPanelType(panel);
            return profile;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
