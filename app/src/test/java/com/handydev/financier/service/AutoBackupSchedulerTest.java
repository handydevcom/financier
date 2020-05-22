package com.handydev.financier.service;

import java.util.Date;

import com.handydev.financier.test.DateTime;

import static com.handydev.financier.test.DateTime.date;
import static org.junit.Assert.*;

public class AutoBackupSchedulerTest {

    public void test_should_schedule_auto_backup_at_specified_time() {
        assertEquals(date(2011, 12, 16).at(6, 0, 0, 0).asDate(), scheduleAt(date(2011, 12, 16).at(0, 0, 0, 0)));
        assertEquals(date(2011, 12, 17).at(6, 0, 0, 0).asDate(), scheduleAt(date(2011, 12, 16).at(8, 0, 0, 0)));
        assertEquals(date(2012, 1, 1).at(6, 0, 0, 0).asDate(), scheduleAt(date(2011, 12, 31).at(18, 0, 0, 0)));
    }

    private Date scheduleAt(DateTime now) {
        return new DailyAutoBackupScheduler(6, 0, now.asLong()).getScheduledTime();
    }

}
