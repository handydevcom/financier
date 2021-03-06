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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.handydev.financier.service.FinancierService;

public class PackageReplaceReceiver extends BroadcastReceiver {

	private static final String PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
        String dataString = intent.getDataString();
		if (PACKAGE_REPLACED.equals(action)) {
            Log.d("PackageReplaceReceiver", "Received " + dataString);
            if ("package:com.handydev".equals(dataString)) {
                Log.d("PackageReplaceReceiver", "Re-scheduling all transactions");
                requestScheduleAll(context);
                requestScheduleAutoBackup(context);
            }
		}
	}

    protected void requestScheduleAll(Context context) {
        Intent serviceIntent = new Intent(FinancierService.ACTION_SCHEDULE_ALL, null, context, FinancierService.class);
        FinancierService.enqueueWork(context, serviceIntent);
    }

    protected void requestScheduleAutoBackup(Context context) {
        Intent serviceIntent = new Intent(FinancierService.ACTION_SCHEDULE_AUTO_BACKUP, null, context, FinancierService.class);
        FinancierService.enqueueWork(context, serviceIntent);
    }
    
}
