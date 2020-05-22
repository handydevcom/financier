/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.handydev.financier.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.handydev.financier.R;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.model.Transaction;
import com.handydev.financier.model.TransactionStatus;
import com.handydev.main.protocol.IOTransactionDeleteListener;

public class BlotterOperations {

    private static final int EDIT_TRANSACTION_REQUEST = 2;
	private static final int EDIT_TRANSFER_REQUEST = 4;

    private final Activity activity;
    private final DatabaseAdapter db;
    private final Transaction originalTransaction;
    private final Transaction targetTransaction;
    private final IOTransactionDeleteListener listener;

    private boolean newFromTemplate = false;

    public BlotterOperations(Activity activity, DatabaseAdapter db, long transactionId, IOTransactionDeleteListener listener) {
        this.activity = activity;
        this.db = db;
        this.originalTransaction = db.getTransaction(transactionId);
        this.listener = listener;
        if (this.originalTransaction.isSplitChild()) {
            this.targetTransaction = db.getTransaction(this.originalTransaction.parentId);
        } else {
            this.targetTransaction = this.originalTransaction;
        }
    }

    public BlotterOperations asNewFromTemplate() {
        newFromTemplate = true;
        return this;
    }

    public void editTransaction() {
        if (targetTransaction.isTransfer()) {
            startEditTransactionActivity(TransferActivity.class, EDIT_TRANSFER_REQUEST);
        } else {
            startEditTransactionActivity(TransactionActivity.class, EDIT_TRANSACTION_REQUEST);
        }
    }

    private void startEditTransactionActivity(Class<? extends Activity> activityClass, int requestCode) {
        Intent intent = new Intent(activity, activityClass);
        intent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, targetTransaction.id);
        intent.putExtra(AbstractTransactionActivity.DUPLICATE_EXTRA, false);
        intent.putExtra(AbstractTransactionActivity.NEW_FROM_TEMPLATE_EXTRA, newFromTemplate);
        activity.startActivityForResult(intent, requestCode);
    }

    public void deleteTransaction() {
        int titleId = targetTransaction.isTemplate() ? R.string.delete_template_confirm
                : (originalTransaction.isSplitChild() ? R.string.delete_transaction_parent_confirm : R.string.delete_transaction_confirm);
        new AlertDialog.Builder(activity)
                .setMessage(titleId)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        long transactionIdToDelete = targetTransaction.id;
                        db.deleteTransaction(transactionIdToDelete);
                        listener.afterDeletingTransaction(transactionIdToDelete);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public long duplicateTransaction(int multiplier) {
        long newId;
		if (multiplier > 1) {
			newId = db.duplicateTransactionWithMultiplier(targetTransaction.id, multiplier);
		} else {
			newId = db.duplicateTransaction(targetTransaction.id);
		}
        return newId;
    }

    public void duplicateAsTemplate() {
        db.duplicateTransactionAsTemplate(targetTransaction.id);
    }

    public void clearTransaction() {
        db.updateTransactionStatus(targetTransaction.id, TransactionStatus.CL);
    }

    public void reconcileTransaction() {
        db.updateTransactionStatus(targetTransaction.id, TransactionStatus.RC);
    }

}
