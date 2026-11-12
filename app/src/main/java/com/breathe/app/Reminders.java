package com.breathe.app;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Reminders {
    private static final String WORK = "daily_reminder";

    /** @param fromWorker true when called by the worker itself (chain next run after it). */
    public static void schedule(Context c, boolean fromWorker) {
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, Prefs.reminderHour(c));
        t.set(Calendar.MINUTE, Prefs.reminderMinute(c));
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        long now = System.currentTimeMillis();
        if (t.getTimeInMillis() <= now + 1000) t.add(Calendar.DAY_OF_YEAR, 1);
        long delay = t.getTimeInMillis() - now;

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance(c).enqueueUniqueWork(WORK,
                fromWorker ? ExistingWorkPolicy.APPEND_OR_REPLACE : ExistingWorkPolicy.REPLACE, req);
    }

    public static void cancel(Context c) {
        WorkManager.getInstance(c).cancelUniqueWork(WORK);
    }
}
