package com.handydev.financier.app;

import android.content.Context;
import android.content.res.Configuration;

import androidx.multidex.MultiDexApplication;

import com.handydev.financier.FinancierApplication;
import com.handydev.financier.utils.MyPreferences;
import com.handydev.main.googledrive.GoogleDriveClient;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;
import org.greenrobot.eventbus.EventBus;

@EApplication
public class FinancierApp extends FinancierApplication {

    public EventBus bus;

    public static GoogleDriveClient driveClient;

    @AfterInject
    public void init() {
        bus = EventBus.getDefault();
        driveClient = new GoogleDriveClient(getApplicationContext());
        //bus.register(driveClient);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MyPreferences.switchLocale(this);
    }
}
