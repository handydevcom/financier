/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.handydev.financier.activity;

import com.handydev.financier.service.FinancierService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.handydev.financier.service.RecurrenceScheduler;

public class ScheduledAlarmReceiver extends PackageReplaceReceiver {

    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String SCHEDULED_BACKUP = "com.handydev.financier.SCHEDULED_BACKUP";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ScheduledAlarmReceiver", "Received " + intent.getAction());
        String action = intent.getAction();
        if (BOOT_COMPLETED.equals(action)) {
            requestScheduleAll(context);
            requestScheduleAutoBackup(context);
        } else if (SCHEDULED_BACKUP.equals(action)) {
            requestAutoBackup(context);
        } else {
            requestScheduleOne(context, intent);
        }
    }

    private void requestScheduleOne(Context context, Intent intent) {
        Intent serviceIntent = new Intent(FinancierService.ACTION_SCHEDULE_ONE, null, context, FinancierService.class);
        serviceIntent.putExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1));
        FinancierService.enqueueWork(context, serviceIntent);
    }

    private void requestAutoBackup(Context context) {
        Intent serviceIntent = new Intent(FinancierService.ACTION_AUTO_BACKUP, null, context, FinancierService.class);
        FinancierService.enqueueWork(context, serviceIntent);
    }

}
