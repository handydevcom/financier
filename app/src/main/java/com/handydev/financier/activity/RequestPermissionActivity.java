package com.handydev.financier.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;

import com.handydev.financier.BuildConfig;
import com.handydev.financier.R;
import com.handydev.financier.utils.MyPreferences;

import static android.os.Build.VERSION.SDK_INT;
import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

@EActivity(R.layout.activity_request_permissions)
public class RequestPermissionActivity extends AppCompatActivity {

    @Extra("requestedPermission")
    String requestedPermission;

    @ViewById(R.id.toggleWriteStorageWrap)
    ViewGroup toggleWriteStorageWrap;

    @ViewById(R.id.toggleWriteStorage)
    SwitchCompat toggleWriteStorage;

    @ViewById(R.id.toggleGetAccountsWrap)
    ViewGroup toggleGetAccountsWrap;

    @ViewById(R.id.toggleGetAccounts)
    SwitchCompat toggleGetAccounts;

    @ViewById(R.id.toggleCameraWrap)
    ViewGroup toggleCameraWrap;

    @ViewById(R.id.toggleCamera)
    SwitchCompat toggleCamera;

    @ViewById(R.id.toggleSmsWrap)
    ViewGroup toggleSmsWrap;

    @ViewById(R.id.toggleSms)
    SwitchCompat toggleSms;

    ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(SDK_INT >= 30) {
            activityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            checkAllFilesPermission();
                        }
                    });
        }
    }

    @AfterViews
    public void initViews() {
        checkPermissions();
        if (BuildConfig.FLAVOR.equals("googleplay")) {
            toggleSmsWrap.setVisibility(View.GONE);
        }
    }

    private void checkAllFilesPermission() {
        enableToggle(!Environment.isExternalStorageManager(), toggleWriteStorage, toggleWriteStorageWrap);
    }

    private void checkPermissions() {
        if(SDK_INT >= 30) {
            checkAllFilesPermission();
        } else {
            disableToggleIfGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, toggleWriteStorage, toggleWriteStorageWrap);
        }
        disableToggleIfGranted(Manifest.permission.GET_ACCOUNTS, toggleGetAccounts, toggleGetAccountsWrap);
        disableToggleIfGranted(Manifest.permission.CAMERA, toggleCamera, toggleCameraWrap);
        if(!BuildConfig.FLAVOR.equals("googleplay")) {
            disableToggleIfGranted(Manifest.permission.RECEIVE_SMS, toggleSms, toggleSmsWrap);
        }
    }

    private void disableToggleIfGranted(String permission, CompoundButton toggleButton, ViewGroup wrapLayout) {
        if (isGranted(permission)) {
            enableToggle(false, toggleButton, wrapLayout);
        } else if (permission.equals(requestedPermission)) {
            wrapLayout.setBackgroundResource(R.drawable.highlight_border);
        }
    }

    private void enableToggle(Boolean enable, CompoundButton toggleButton, ViewGroup wrapLayout) {
        toggleButton.setChecked(!enable);
        toggleButton.setEnabled(enable);
        wrapLayout.setBackgroundResource(0);
    }

    @Click(R.id.toggleWriteStorage)
    public void onGrantWriteStorage() {
        if(SDK_INT >= 30){
            Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
            activityResultLauncher.launch(intent);
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, toggleWriteStorage);
        }
    }

    @Click(R.id.toggleGetAccounts)
    public void onGrantGetAccounts() {
        requestPermission(Manifest.permission.GET_ACCOUNTS, toggleGetAccounts);
    }

    @Click(R.id.toggleCamera)
    public void onGrantCamera() {
        requestPermission(Manifest.permission.CAMERA, toggleCamera);
    }

    @Click(R.id.toggleSms)
    public void onGrantSms() {
        if (!BuildConfig.FLAVOR.equals("googleplay")) {
            requestPermission(Manifest.permission.RECEIVE_SMS, toggleSms);
        }
    }

    private void requestPermission(String permission, CompoundButton toggleButton) {
        toggleButton.setChecked(false);
        ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkPermissions();
    }

}
