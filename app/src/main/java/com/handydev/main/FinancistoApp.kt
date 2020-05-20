package com.handydev.main

import com.handydev.financisto.MyEventBusIndex
import com.handydev.financisto.app.FinancistoApp
import org.greenrobot.eventbus.EventBus

class FinancistoApp: FinancistoApp() {
    override fun onCreate() {
        super.onCreate()
        val eventBus: EventBus = EventBus.builder().addIndex(MyEventBusIndex()).build()
        EventBus.builder().addIndex(MyEventBusIndex()).installDefaultEventBus()
    }
}