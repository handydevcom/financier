/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.handydev.financier.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import java.util.Date;
import com.handydev.financier.R;
import com.handydev.financier.activity.AbstractTransactionActivity;
import com.handydev.financier.activity.AccountWidget;
import com.handydev.financier.activity.MassOpActivity;
import com.handydev.financier.backup.DatabaseExport;
import com.handydev.financier.blotter.BlotterFilter;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.export.Export;
import com.handydev.financier.filter.WhereFilter;
import com.handydev.financier.model.Transaction;
import com.handydev.financier.model.TransactionInfo;
import com.handydev.financier.model.TransactionStatus;
import com.handydev.financier.recur.NotificationOptions;
import static com.handydev.financier.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static com.handydev.financier.service.SmsReceiver.SMS_TRANSACTION_BODY;
import static com.handydev.financier.service.SmsReceiver.SMS_TRANSACTION_NUMBER;
import com.handydev.financier.utils.MyPreferences;
import static com.handydev.financier.utils.MyPreferences.getSmsTransactionStatus;
import static com.handydev.financier.utils.MyPreferences.shouldSaveSmsToTransactionNote;

public class FinancierService extends JobIntentService {

    private static final String TAG = "FinancierService";
    public static final int JOB_ID = 1000;

    public static final String ACTION_SCHEDULE_ALL = "com.handydev.financier.SCHEDULE_ALL";
    public static final String ACTION_SCHEDULE_ONE = "com.handydev.financier.SCHEDULE_ONE";
    public static final String ACTION_SCHEDULE_AUTO_BACKUP = "com.handydev.financier.ACTION_SCHEDULE_AUTO_BACKUP";
    public static final String ACTION_AUTO_BACKUP = "com.handydev.financier.ACTION_AUTO_BACKUP";
    public static final String ACTION_NEW_TRANSACTION_SMS = "com.handydev.financier.NEW_TRANSACTON_SMS";

    private static final int RESTORED_NOTIFICATION_ID = 0;

    private DatabaseAdapter db;
    private RecurrenceScheduler scheduler;
    private SmsTransactionProcessor smsProcessor;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FinancierService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
        scheduler = new RecurrenceScheduler(db);
        smsProcessor = new SmsTransactionProcessor(db);
    }

    @Override
    public void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_SCHEDULE_ALL:
                    scheduleAll();
                    break;
                case ACTION_SCHEDULE_ONE:
                    scheduleOne(intent);
                    break;
                case ACTION_SCHEDULE_AUTO_BACKUP:
                    scheduleNextAutoBackup(this);
                    break;
                case ACTION_AUTO_BACKUP:
                    doAutoBackup();
                    break;
                case ACTION_NEW_TRANSACTION_SMS:
                    processSmsTransaction(intent);
                    break;
            }
        }
    }

    private void processSmsTransaction(Intent intent) {
        String number = intent.getStringExtra(SMS_TRANSACTION_NUMBER);
        String body = intent.getStringExtra(SMS_TRANSACTION_BODY);
        if (number != null && body != null) {
            Transaction t = smsProcessor.createTransactionBySms(number, body, getSmsTransactionStatus(this),
                shouldSaveSmsToTransactionNote(this));
            if (t != null) {
                TransactionInfo transactionInfo = db.getTransactionInfo(t.id);
                if (transactionInfo != null) {
                    Notification notification = createSmsTransactionNotification(transactionInfo, number);
                    notifyUser(notification, (int) t.id);
                    AccountWidget.updateWidgets(this);
                } else {
                    Log.e("Financier", "Transaction info does not exist for "+t.id);
                }
            }
        }
    }

    private void scheduleAll() {
        int restoredTransactionsCount = scheduler.scheduleAll(this);
        if (restoredTransactionsCount > 0) {
            notifyUser(createRestoredNotification(restoredTransactionsCount), RESTORED_NOTIFICATION_ID);
        }
    }

    private void scheduleOne(Intent intent) {
        long scheduledTransactionId = intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1);
        if (scheduledTransactionId > 0) {
            TransactionInfo transaction = scheduler.scheduleOne(this, scheduledTransactionId);
            if (transaction != null) {
                notifyUser(transaction);
                AccountWidget.updateWidgets(this);
            }
        }
    }

    private void doAutoBackup() {
        try {
            try {
                long t0 = System.currentTimeMillis();
                Log.e(TAG, "Auto-backup started at " + new Date());
                DatabaseExport export = new DatabaseExport(this, db.db(), true);
                String fileName = export.export();
                boolean successful = true;
                if (MyPreferences.isDropboxUploadAutoBackups(this)) {
                    try {
                        Export.uploadBackupFileToDropbox(this, fileName);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to upload auto-backup to Dropbox", e);
                        MyPreferences.notifyAutobackupFailed(this, e);
                        successful = false;
                    }
                }
                if (MyPreferences.isGoogleDriveUploadAutoBackups(this)) {
                    try {
                        Export.uploadBackupFileToGoogleDrive(this, fileName);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to upload auto-backup to Google Drive", e);
                        MyPreferences.notifyAutobackupFailed(this, e);
                        successful = false;
                    }
                }
                Log.e(TAG, "Auto-backup completed in " + (System.currentTimeMillis() - t0) + "ms");
                if (successful) {
                    MyPreferences.notifyAutobackupSucceeded(this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Auto-backup unsuccessful", e);
                MyPreferences.notifyAutobackupFailed(this, e);
            }
        } finally {
            scheduleNextAutoBackup(this);
        }
    }

    private void notifyUser(TransactionInfo transaction) {
        Notification notification = createScheduledNotification(transaction);
        notifyUser(notification, (int) transaction.id);
    }

    private void notifyUser(Notification notification, int id) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    private Notification createRestoredNotification(int count) {
        long when = System.currentTimeMillis();
        String text = getString(R.string.scheduled_transactions_have_been_restored, count);
        String contentTitle = getString(R.string.scheduled_transactions_restored);

        Intent notificationIntent = new Intent(this, MassOpActivity.class);
        WhereFilter filter = new WhereFilter("");
        filter.eq(BlotterFilter.STATUS, TransactionStatus.RS.name());
        filter.toIntent(notificationIntent);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, "restored")
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.notification_icon_transaction)
                .setWhen(when)
                .setTicker(text)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();
    }

    private Notification createSmsTransactionNotification(TransactionInfo t, String number) {
        String tickerText = getString(R.string.new_sms_transaction_text, number);
        String contentTitle = getString(R.string.new_sms_transaction_title, number);
        String text = t.getNotificationContentText(this);

        return generateNotification(t, tickerText, contentTitle, text);
    }

    private Notification createScheduledNotification(TransactionInfo t) {
        String tickerText = t.getNotificationTickerText(this);
        String contentTitle = t.getNotificationContentTitle(this);
        String text = t.getNotificationContentText(this);

        return generateNotification(t, tickerText, contentTitle, text);
    }

    private Notification generateNotification(TransactionInfo t, String tickerText, String contentTitle, String text) {
        Intent notificationIntent = new Intent(this, t.getActivity());
        notificationIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, t.id);
        PendingIntent contentIntent = PendingIntent.getActivity(this, (int) t.id, notificationIntent, FLAG_CANCEL_CURRENT); /* https://stackoverflow.com/a/3730394/365675 */

        Notification notification = new NotificationCompat.Builder(this, "transactions")
                .setContentIntent(contentIntent)
                .setSmallIcon(t.getNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setTicker(tickerText)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true)
                .build();

        applyNotificationOptions(notification, t.notificationOptions);

        return notification;
    }

    private void applyNotificationOptions(Notification notification, String notificationOptions) {
        if (notificationOptions == null) {
            notification.defaults = Notification.DEFAULT_ALL;
        } else {
            NotificationOptions options = NotificationOptions.parse(notificationOptions);
            options.apply(notification);
        }
    }

}
