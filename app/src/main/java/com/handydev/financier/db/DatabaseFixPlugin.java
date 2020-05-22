package com.handydev.financier.db;

import android.content.ContentValues;

import com.handydev.orb.Plugin;

import static com.handydev.financier.db.DatabaseHelper.LOCATIONS_TABLE;

public class DatabaseFixPlugin implements Plugin {

    @Override
    public void withContentValues(String tableName, ContentValues values) {
        if (LOCATIONS_TABLE.equals(tableName)) {
            // since there is no easy way to drop a column in SQLite
            values.put("name", values.getAsString("title"));
        }
    }

}
