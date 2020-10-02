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
import android.content.res.AssetManager;
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

public class CreateProfileFragment extends Fragment {

    public static final int PICK_IMAGE = 1;
    public static final int PICK_IMAGE_CAMERA = 2;
    public static final int REQ_PERMISSIONS = 10;
    private EditText etName, etDescription, etPanelType, etChannel1, etChannel2, etChannel3, etChannel4, etBrightness;
    private TextView tvChangeImage;
    private ImageView profileImage;
    private Button btnCreate;
    private ProfileAdapter profileAdapter;
    private Application application;
    private ProfileActivity profileActivity;
    private AssetManager assetManager;
    private Uri imageUri;
    private File storageDir;
    private File imageFile;

    public CreateProfileFragment(Application application, ProfileAdapter profileAdapter) {
        this.application = application;
        this.profileAdapter = profileAdapter;
        assetManager = application.getResources().getAssets();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_profile, container, false);
        profileActivity = (ProfileActivity) getActivity();
        profileImage = view.findViewById(R.id.profile_image);
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
        etDescription = view.findViewById(R.id.et_description);
        etPanelType = view.findViewById(R.id.et_panel_type);
        etChannel1 = view.findViewById(R.id.et_channel1);
        etChannel2 = view.findViewById(R.id.et_channel2);
        etChannel3 = view.findViewById(R.id.et_channel3);
        etChannel4 = view.findViewById(R.id.et_channel4);
        etBrightness = view.findViewById(R.id.et_brightness);

        btnCreate = view.findViewById(R.id.btn_create);
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateClicked();
            }
        });

        Bundle bundle = getArguments();
        if(bundle != null) {
            etChannel1.setText(String.valueOf(bundle.getInt(ProfileActivity.EXTRA_PROFILE_CH1)));
            etChannel2.setText(String.valueOf(bundle.getInt(ProfileActivity.EXTRA_PROFILE_CH2)));
            etChannel3.setText(String.valueOf(bundle.getInt(ProfileActivity.EXTRA_PROFILE_CH3)));
            etChannel4.setText(String.valueOf(bundle.getInt(ProfileActivity.EXTRA_PROFILE_CH4)));
            etBrightness.setText(String.valueOf(bundle.getInt(ProfileActivity.EXTRA_PROFILE_CHB)));
        }
        return view;
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

    private void openChooseImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.chooseImageTitle));
        String[] options = {getString(R.string.chooseImageOptionGallery),
                getString(R.string.chooseImageOptionCamera)};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    imageFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (which == 0) { // Choose image from gallery
                    Intent intent = new Intent();
                    intent.setType("image/jpeg");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    if(imageFile != null) {
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
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private File createImageFile() throws IOException{
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
                profileImage.setImageBitmap(bitmap);
                fous.flush();
                fous.close();
                Log.d(CustomLogger.TAG, "IMAGE SUCCESSFULLY SAVED");
            } catch (IOException e) {
                Log.d(CustomLogger.TAG, "Error Activity Result: " + e.getMessage());
            }
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

    private void onCreateClicked() {
        if (checkInput()) {
            int position = profileAdapter.getItemCount()+1;
            Profile profile = new Profile(etName.getText().toString(), null, position,
                    Integer.valueOf(etChannel1.getText().toString()), Integer.valueOf(etChannel2.getText().toString()),
                    Integer.valueOf(etChannel3.getText().toString()), Integer.valueOf(etChannel4.getText().toString()),
                    Integer.valueOf(etBrightness.getText().toString()));
            if(imageUri != null) {
                profile.setImagePath(imageUri.toString());
            }
            String desc = etDescription.getText().toString();
            if (desc.trim().isEmpty()) {
                desc = getString(R.string.descriptionPlaceholder);
            }
            profile.setDescription(desc);

            String panel = etPanelType.getText().toString();
            if(panel.trim().isEmpty()) {
                panel = getString(R.string.panelPlaceholder);
            }
            profile.setPanelType(panel);
            profileAdapter.createProfile(profile);
            profileActivity.onProfileCreated();
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