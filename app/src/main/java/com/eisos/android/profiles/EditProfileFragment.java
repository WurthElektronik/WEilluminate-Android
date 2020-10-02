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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.eisos.android.BuildConfig;
import com.eisos.android.R;
import com.eisos.android.adapter.ProfileAdapter;
import com.eisos.android.database.profiles.Profile;
import com.eisos.android.utils.CustomLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditProfileFragment extends Fragment {

    public static final int PICK_IMAGE = 1;
    public static final int PICK_IMAGE_CAMERA = 2;
    public static final int REQ_PERMISSIONS = 10;
    private Application application;
    private ProfileAdapter profileAdapter;
    private ProfileActivity profileActivity;
    private EditText etName, etDescription, etPanelType, etChannel1, etChannel2, etChannel3, etChannel4, etBrightness;
    private TextView tvChangeImage;
    private ImageView profileImage;
    private Button btnEdit;
    private int position;
    private Profile profile;
    private Uri imageUri;
    private File imageFile;
    private File storageDir;
    private boolean deleteImage = false;

    public EditProfileFragment(Application application, ProfileAdapter profileAdapter) {
        this.application = application;
        this.profileAdapter = profileAdapter;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_profile, container, false);
        profileActivity = (ProfileActivity) getActivity();
        position = getArguments().getInt("position");
        profile = profileAdapter.getProfiles().get(position);

        profileImage = view.findViewById(R.id.profile_image);
        try {
            // If Uri.parse() is no valid uri -> no image has been set,
            // therefore set placeholder image
            imageUri = Uri.parse(profile.getImagePath());
            profileImage.setImageURI(imageUri);
        } catch (NullPointerException e) {
            profileImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            profileImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        tvChangeImage = view.findViewById(R.id.tv_change_image);
        tvChangeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(getContext(),
                                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
                    openChooseImageDialog();
                } else {
                    requestPermissions();
                }
            }
        });
        etName = view.findViewById(R.id.et_profile_name);
        etName.setText(profile.getName());
        etDescription = view.findViewById(R.id.et_description);
        String desc = profile.getDescription();
        if(desc.equals(getString(R.string.descriptionPlaceholder))) {
            etDescription.setHint(desc);
        } else {
            etDescription.setText(desc);
        }
        etPanelType = view.findViewById(R.id.et_panel_type);
        String panel = profile.getPanelType();
        if(panel.equals(getString(R.string.panelPlaceholder))) {
            etPanelType.setHint(panel);
        } else {
            etPanelType.setText(panel);
        }
        etChannel1 = view.findViewById(R.id.et_channel1);
        etChannel1.setText(String.valueOf(profile.getChannel1()));
        etChannel2 = view.findViewById(R.id.et_channel2);
        etChannel2.setText(String.valueOf(profile.getChannel2()));
        etChannel3 = view.findViewById(R.id.et_channel3);
        etChannel3.setText(String.valueOf(profile.getChannel3()));
        etChannel4 = view.findViewById(R.id.et_channel4);
        etChannel4.setText(String.valueOf(profile.getChannel4()));
        etBrightness = view.findViewById(R.id.et_brightness);
        etBrightness.setText(String.valueOf(profile.getBrightness()));

        btnEdit = view.findViewById(R.id.btn_create);
        btnEdit.setText(getString(R.string.editProfileBtn));
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditClicked();
            }
        });
        return view;
    }

    private void onEditClicked() {
        if (checkInput()) {
            if(deleteImage) {
                profile.setImagePath(null);
                if(imageUri != null) {
                    getActivity().getContentResolver().delete(imageUri, null, null);
                }
            } else {
                if(imageUri != null) {
                    profile.setImagePath(imageUri.toString());
                }
            }
            profile.setName(etName.getText().toString());
            String desc = etDescription.getText().toString();
            if(desc.trim().isEmpty()) {
                desc = getString(R.string.descriptionPlaceholder);
            }
            profile.setDescription(desc);
            String panel = etPanelType.getText().toString();
            if(panel.trim().isEmpty()) {
                panel = getString(R.string.panelPlaceholder);
            }
            profile.setPanelType(panel);
            profile.setChannel1(Integer.valueOf(etChannel1.getText().toString()));
            profile.setChannel2(Integer.valueOf(etChannel2.getText().toString()));
            profile.setChannel3(Integer.valueOf(etChannel3.getText().toString()));
            profile.setChannel4(Integer.valueOf(etChannel4.getText().toString()));
            profile.setBrightness(Integer.valueOf(etBrightness.getText().toString()));
            profileAdapter.updateProfile(position, profile);
            profileActivity.onProfileEdited();
        }
    }

    private void openChooseImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.chooseImageTitle));
        String[] options = {getString(R.string.chooseImageOptionGallery),
                getString(R.string.chooseImageOptionCamera), getString(R.string.chooseImageOptionRemove)};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    imageFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                deleteImage = false;
                if (which == 0) { // Choose image from gallery
                    Intent intent = new Intent();
                    intent.setType("image/jpeg");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    if (imageFile != null) {
                        imageUri = FileProvider.getUriForFile(getActivity(),
                                BuildConfig.APPLICATION_ID + ".provider",
                                imageFile);
                        startActivityForResult(Intent.createChooser(intent,
                                getString(R.string.chooseImageActivityTitle)), PICK_IMAGE);
                    }
                } else if (which == 1) { // Choose image from camera
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    if(imageFile != null) {
                        imageUri = FileProvider.getUriForFile(getActivity(),
                                BuildConfig.APPLICATION_ID + ".provider",
                                imageFile);
                        startActivityForResult(intent, PICK_IMAGE_CAMERA);
                    }
                } else if (which == 2) { // Remove image
                    deleteImage = true;
                    profileImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    profileImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Creates a temporary image file to save the taken image by the camera.
     * @return the new created file
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if(!storageDir.exists()) {
            storageDir.mkdirs();
        }
        imageFile = File.createTempFile(timeStamp, ".jpg", storageDir);
        return imageFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            Uri selectedImg = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImg);
                FileOutputStream fout = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fout);
                fout.flush();
                fout.close();
                profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                profileImage.setImageBitmap(bitmap);
                Log.d(CustomLogger.TAG, "IMAGE SUCCESSFULLY LOADED");
            } catch (IOException e) {
                Log.d(CustomLogger.TAG, "Error Activity Result: " + e.getMessage());
            }
        } else if (requestCode == PICK_IMAGE_CAMERA && resultCode == Activity.RESULT_OK) {
            Log.d(CustomLogger.TAG, "IMAGE SUCCESSFULLY TAKEN");
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            try {
                FileOutputStream fous = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fous);
                profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                profileImage.setImageBitmap(bitmap);
                fous.flush();
                fous.close();
                Log.d(CustomLogger.TAG, "IMAGE SUCCESSFULLY SAVED");
            } catch (IOException e) {
                Log.d(CustomLogger.TAG, "Error Activity Result: " + e.getMessage());
            }
        }
    }


    /**
     * Requests the READ_EXTERNAL_STORAGE and CAMERA permissions
     */
    private void requestPermissions() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};
        if (ContextCompat.checkSelfPermission(getActivity(), permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), permissions, REQ_PERMISSIONS);
        } else if (ContextCompat.checkSelfPermission(getActivity(), permissions[1]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), permissions, REQ_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    openChooseImageDialog();
                }
                break;
        }
    }

    private boolean checkInput() {
        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError(application.getString(R.string.fieldRequired));
            etName.requestFocus();
            return false;
        } else if (etChannel1.getText().toString().isEmpty()) {
            etChannel1.setError(application.getString(R.string.fieldRequired));
            etChannel1.requestFocus();
            return false;
        } else if (Integer.parseInt(etChannel1.getText().toString()) < 0 || Integer.parseInt(etChannel1.getText().toString()) > 255) {
            etChannel1.setError(application.getString(R.string.wrongValue));
            etChannel1.requestFocus();
            return false;
        } else if (etChannel2.getText().toString().isEmpty()) {
            etChannel2.setError(application.getString(R.string.fieldRequired));
            etChannel2.requestFocus();
            return false;
        } else if (Integer.parseInt(etChannel2.getText().toString()) < 0 || Integer.parseInt(etChannel2.getText().toString()) > 255) {
            etChannel2.setError(application.getString(R.string.wrongValue));
            etChannel2.requestFocus();
            return false;
        } else if (etChannel3.getText().toString().isEmpty()) {
            etChannel3.setError(application.getString(R.string.fieldRequired));
            etChannel3.requestFocus();
            return false;
        } else if (Integer.parseInt(etChannel3.getText().toString()) < 0 || Integer.parseInt(etChannel3.getText().toString()) > 255) {
            etChannel3.setError(application.getString(R.string.wrongValue));
            etChannel3.requestFocus();
            return false;
        } else if (etChannel4.getText().toString().isEmpty()) {
            etChannel4.setError(application.getString(R.string.fieldRequired));
            etChannel4.requestFocus();
            return false;
        } else if (Integer.parseInt(etChannel4.getText().toString()) < 0 || Integer.parseInt(etChannel4.getText().toString()) > 255) {
            etChannel4.setError(application.getString(R.string.wrongValue));
            etChannel4.requestFocus();
            return false;
        } else if (etBrightness.getText().toString().isEmpty()) {
            etBrightness.setError(application.getString(R.string.fieldRequired));
            etBrightness.requestFocus();
            return false;
        } else if (Integer.parseInt(etBrightness.getText().toString()) < 0 || Integer.parseInt(etBrightness.getText().toString()) > 255) {
            etBrightness.setError(application.getString(R.string.wrongValue));
            etBrightness.requestFocus();
            return false;
        }
        return true;
    }
}
