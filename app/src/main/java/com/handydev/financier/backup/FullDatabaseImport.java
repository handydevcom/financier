/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package com.handydev.financier.backup;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.service.RecurrenceScheduler;
import com.handydev.financier.utils.CurrencyCache;
import com.handydev.financier.utils.IntegrityFix;

public abstract class FullDatabaseImport {

    protected final Context context;
    protected final DatabaseAdapter dbAdapter;
    protected final SQLiteDatabase db;

    public FullDatabaseImport(Context context, DatabaseAdapter dbAdapter) {
        this.context = context;
        this.dbAdapter = dbAdapter;
        this.db = dbAdapter.db();
    }

    public void importDatabase() throws IOException {
        db.beginTransaction();
        try {
            cleanDatabase();
            restoreDatabase();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        new IntegrityFix(dbAdapter).fix();
        CurrencyCache.initialize(dbAdapter);
        scheduleAll();
    }

    protected abstract void restoreDatabase() throws IOException;

    private void cleanDatabase() {
        for (String tableName : tablesToClean()) {
            db.execSQL("delete from " + tableName);
        }
    }

    protected List<String> tablesToClean() {
        List<String> list = new ArrayList<>(Arrays.asList(Backup.BACKUP_TABLES));
        list.add("running_balance");
        return list;
    }

    private void scheduleAll() {
        RecurrenceScheduler scheduler = new RecurrenceScheduler(dbAdapter);
        scheduler.scheduleAll(context);
    }

}
