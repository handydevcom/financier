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

import com.handydev.financier.R;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.export.ImportExportAsyncTask;
import com.handydev.financier.export.ImportExportAsyncTaskListener;
import com.handydev.financier.export.ImportExportException;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 1/20/14
 * Time: 11:58 PM
 */
public class DropboxListFilesTask extends ImportExportAsyncTask {

    public DropboxListFilesTask(final Activity context, ProgressDialog dialog) {
        super(context, dialog);
        setShowResultMessage(false);
        setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted(Object result) {
                EventBus.getDefault().post(new DropboxFileList((String[]) result));
            }
        });
    }

    @Override
    protected Object work(Context context, DatabaseAdapter db, String... params) throws Exception {
        try {
            Dropbox dropbox = new Dropbox(context);
            List<String> files = dropbox.listFiles();
            return files.toArray(new String[files.size()]);
        } catch (Exception e) {
            throw new ImportExportException(R.string.dropbox_error);
        }
    }

    @Override
    protected String getSuccessMessage(Object result) {
        return null;
    }

}
