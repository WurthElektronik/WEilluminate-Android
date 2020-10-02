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

import android.app.Application;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eisos.android.R;
import com.eisos.android.database.profiles.Profile;
import com.eisos.android.database.profiles.ProfileViewModel;
import com.eisos.android.dialogs.DeleteProfileDialog;
import com.eisos.android.profiles.ProfileActivity;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileHolder> {

    private ProfileViewModel profileViewModel;
    private Application application;
    private FragmentManager fm;
    private List<Profile> profiles;
    private ProfileActivity profileActivity;
    private ProfileAdapter profileAdapter;

    public ProfileAdapter(Application application, ProfileActivity profileActivity,
                          FragmentManager fm) {
        this.profileAdapter = this;
        this.application = application;
        this.fm = fm;
        this.profileActivity = profileActivity;
        this.profileViewModel = new ProfileViewModel(application);
        profiles = profileViewModel.getAll();
    }

    @NonNull
    @Override
    public ProfileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View listItem = LayoutInflater.from(profileActivity)
                .inflate(R.layout.profile_card_option1, parent, false);
        return new ProfileHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileHolder holder, int position) {
        final Profile profile = profiles.get(position);
        // Load profile image
        new ProfileImageLoaderTask(profile, holder.profileImg, position).execute();
        holder.profileName.setText(profile.getName());
        holder.profileDesc.setText(profile.getDescription());
        holder.profilePanel.setText(application.getString(R.string.ledPanelType) + ": " + profile.getPanelType());
        holder.profileRGBW.setText(application.getString(R.string.colorChannel1) + ": "
                + profile.getChannel1() + ", " + application.getString(R.string.colorChannel2) + ": "
                + profile.getChannel2() + ",\n" + application.getString(R.string.colorChannel3) + ": "
                + profile.getChannel3() + ", " + application.getString(R.string.colorChannel4) + ": "
                + profile.getChannel4() + ",\n" + application.getString(R.string.brightness) + ": "
                + profile.getBrightness());
        holder.tvSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileActivity.onProfileSelected(profile);
            }
        });
        holder.tvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileActivity.openEditProfileScreen(position);
            }
        });
        holder.tvDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteProfileDialog dialog = new DeleteProfileDialog(profileAdapter, position,
                        profile, profileActivity);
                dialog.show(fm, "DeleteProfileDialog");
            }
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public List<Profile> getProfiles() {
        return this.profiles;
    }

    public void createProfile(Profile profile) {
        profileViewModel.insert(profile);
        profileViewModel.updateList();
        profiles = profileViewModel.getAll();
        notifyItemInserted(profiles.size()-1);
    }

    public void updateProfile(int position, Profile profile) {
        profileViewModel.update(profile);
        profileViewModel.updateList();
        profiles = profileViewModel.getAll();
        notifyItemChanged(position);
    }

    public void deleteProfile(int position, Profile profile) {
        profileViewModel.delete(profile);
        profileViewModel.updateList();
        profiles = profileViewModel.getAll();
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    class ProfileHolder extends RecyclerView.ViewHolder {

        public ImageView profileImg;
        public TextView profileName, profileDesc, profilePanel, profileRGBW, tvSelect, tvEdit, tvDelete;

        public ProfileHolder(@NonNull View itemView) {
            super(itemView);
            this.profileImg = itemView.findViewById(R.id.profile_image);
            this.profileName = itemView.findViewById(R.id.profile_name);
            this.profileDesc = itemView.findViewById(R.id.profile_description);
            this.profilePanel = itemView.findViewById(R.id.profile_panel);
            this.profileRGBW = itemView.findViewById(R.id.profile_rgbw_value);
            this.tvSelect = itemView.findViewById(R.id.tv_select);
            this.tvEdit = itemView.findViewById(R.id.tv_edit);
            this.tvDelete = itemView.findViewById(R.id.tv_delete);
        }
    }

    private class ProfileImageLoaderTask extends AsyncTask<Void, Void, Void> {

        private Profile profile;
        private ImageView imageView;
        private Uri imageUri;
        private int index;

        public ProfileImageLoaderTask(Profile profile, ImageView imageView, int index) {
            this.profile = profile;
            this.imageView = imageView;
            this.index = index;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                imageUri = Uri.parse(profile.getImagePath());
            } catch (NullPointerException e) {
                imageUri = null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            imageView.setImageURI(imageUri);
            if(imageView.getDrawable() == null) {
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }
}
