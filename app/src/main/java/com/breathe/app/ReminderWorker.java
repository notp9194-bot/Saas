package com.breathe.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;

public class ReminderWorker extends Worker {
    private static final String CHANNEL = "reminders";
    private static final String[] MESSAGES = {
            "Take 2 minutes to breathe and reset.",
            "A calm minute is waiting for you.",
            "Time for your breathing exercise.",
            "Pause, inhale, exhale. You've got this!"};

    public ReminderWorker(@NonNull Context ctx, @NonNull WorkerParameters p) { super(ctx, p); }

    @NonNull @Override public Result doWork() {
        Context c = getApplicationContext();
        if (!Prefs.isNotif(c)) return Result.success();   // turned off meanwhile: stop the chain
        try {
            if (Auth.isLoggedIn(c) && !Prefs.practicedToday(c) && canPost(c)) notifyUser(c);
        } finally {
            Reminders.schedule(c, true);
        }
        return Result.success();
    }

    private boolean canPost(Context c) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void notifyUser(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT));

        Intent open = new Intent(c, SplashActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String msg = MESSAGES[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % MESSAGES.length];
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_lotus)
                .setContentTitle("Time to breathe")
                .setContentText(msg)
                .setContentIntent(pi)
                .setAutoCancel(true);
        nm.notify(1001, b.build());
    }
}
