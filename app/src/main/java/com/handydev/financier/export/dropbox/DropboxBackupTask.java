/*
 * Copyright (c) 2014 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.handydev.financier.export.dropbox;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import com.handydev.financier.backup.DatabaseExport;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.export.ImportExportAsyncTask;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/9/11 2:23 AM
 */
public class DropboxBackupTask extends ImportExportAsyncTask {

    public DropboxBackupTask(Activity mainActivity, ProgressDialog dialog) {
        super(mainActivity, dialog);
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        DatabaseExport export = new DatabaseExport(context, db.db(), true);
        String backupFileName = export.export();
        doForceUploadToDropbox(context, backupFileName);
        return backupFileName;
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return String.valueOf(result);
    }

}
