package com.handydev.financier.app;

import android.content.Context;
import android.content.res.Configuration;

import com.handydev.financier.FinancierApplication;
import com.handydev.financier.utils.MyPreferences;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;
import org.greenrobot.eventbus.EventBus;

@EApplication
public class FinancierApp extends FinancierApplication {

    public EventBus bus;

    @AfterInject
    public void init() {
        bus = EventBus.getDefault();
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
