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

package com.eisos.android.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.eisos.android.database.favourites.FavouriteDao;
import com.eisos.android.database.favourites.FavouriteDevice;
import com.eisos.android.database.profiles.Profile;
import com.eisos.android.database.profiles.ProfileDao;

@Database(entities = {FavouriteDevice.class, Profile.class}, version = 1)
public abstract class WEilluminateDB extends RoomDatabase {
    private static WEilluminateDB instance;

    public abstract FavouriteDao favouriteDao();

    public abstract ProfileDao profileDao();

    public static synchronized WEilluminateDB getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    WEilluminateDB.class, "weilluminate-db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
