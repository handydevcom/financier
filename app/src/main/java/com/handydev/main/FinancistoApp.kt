package com.handydev.main

import com.handydev.financier.MyEventBusIndex
import com.handydev.financier.app.FinancistoApp
import org.greenrobot.eventbus.EventBus

class FinancistoApp: FinancistoApp() {
    override fun onCreate() {
        super.onCreate()
        val eventBus: EventBus = EventBus.builder().addIndex(MyEventBusIndex()).build()
        EventBus.builder().addIndex(MyEventBusIndex()).installDefaultEventBus()
    }
}