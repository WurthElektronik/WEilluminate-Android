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

package com.eisos.android.database.profiles;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class Profile {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "position")
    public int position;

    @ColumnInfo(name = "image_path")
    public String imagePath;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "panelType")
    public String panelType;

    @ColumnInfo(name = "channel1")
    public int channel1;

    @ColumnInfo(name = "channel2")
    public int channel2;

    @ColumnInfo(name = "channel3")
    public int channel3;

    @ColumnInfo(name = "channel4")
    public int channel4;

    @ColumnInfo(name = "brightness")
    public int brightness;

    public Profile(String name, String imagePath, int position, int channel1,
                   int channel2, int channel3, int channel4, int brightness) {
        this.name = name;
        this.channel1 = channel1;
        this.channel2 = channel2;
        this.channel3 = channel3;
        this.channel4 = channel4;
        this.brightness = brightness;
        this.imagePath = imagePath;
        setDescription("No description available");
        setPosition(position);
    }

    /**
     * Sets the position of the item in the profile list.
     * @param position
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     *
     * @return The position of the item in the profile list.
     */
    public int getPosition() {
        return position;
    }

    public int getId() {
        return id;
    }

    public void setImagePath(String path) {
        this.imagePath = path;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String text) {
        this.description = text;
    }

    public String getDescription() {
        return description;
    }

    public void setPanelType(String panelType) {
        this.panelType = panelType;
    }

    public String getPanelType() {
        return panelType;
    }

    public void setChannel1(int channel1) {
        this.channel1 = channel1;
    }

    public int getChannel1() {
        return channel1;
    }

    public void setChannel2(int channel2) {
        this.channel2 = channel2;
    }

    public int getChannel2() {
        return channel2;
    }

    public void setChannel3(int channel3) {
        this.channel3 = channel3;
    }

    public int getChannel3() {
        return channel3;
    }

    public void setChannel4(int channel4) {
        this.channel4 = channel4;
    }

    public int getChannel4() {
        return channel4;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getBrightness() {
        return brightness;
    }
}
