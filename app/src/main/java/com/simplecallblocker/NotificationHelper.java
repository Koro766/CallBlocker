package com.simplecallblocker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Handles creation and updating of persistent toggle notifications
 * and blocked call alert notifications.
 */
public class NotificationHelper {
    public static final String CHANNEL_STATUS_ID = "channel_blocker_status";
    public static final String CHANNEL_ALERTS_ID = "channel_blocker_alerts";
    public static final int NOTIFICATION_STATUS_ID = 1001;
    public static final int NOTIFICATION_ALERT_BASE_ID = 2000;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm == null) return;

            // Channel 1: Ongoing toggle control notification
            NotificationChannel statusChannel = new NotificationChannel(
                    CHANNEL_STATUS_ID,
                    context.getString(R.string.channel_status_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            statusChannel.setDescription(context.getString(R.string.channel_status_desc));
            statusChannel.setShowBadge(false);
            nm.createNotificationChannel(statusChannel);

            // Channel 2: Alerts when a call is blocked
            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS_ID,
                    context.getString(R.string.channel_alerts_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            alertChannel.setDescription(context.getString(R.string.channel_alerts_desc));
            nm.createNotificationChannel(alertChannel);
        }
    }

    public static void updateStatusNotification(Context context) {
        if (!BlockerPreferences.isPersistentNotificationEnabled(context)) {
            cancelStatusNotification(context);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        createNotificationChannels(context);

        boolean isEnabled = BlockerPreferences.isBlockerEnabled(context);

        // Tap notification to open MainActivity
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Tap action button to toggle blocker ON / OFF
        Intent toggleIntent = new Intent(context, ToggleReceiver.class);
        toggleIntent.setAction(BlockerPreferences.ACTION_TOGGLE_BLOCKER);
        PendingIntent togglePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title = isEnabled ? context.getString(R.string.blocking_active) : context.getString(R.string.blocking_paused);
        String text = isEnabled ? context.getString(R.string.status_active_desc) : context.getString(R.string.status_paused_desc);
        String actionTitle = isEnabled ? context.getString(R.string.action_turn_off) : context.getString(R.string.action_turn_on);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_STATUS_ID)
                .setSmallIcon(isEnabled ? R.drawable.ic_shield : R.drawable.ic_shield_off)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openAppPendingIntent)
                .addAction(
                        isEnabled ? R.drawable.ic_shield_off : R.drawable.ic_shield,
                        actionTitle,
                        togglePendingIntent
                );

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_STATUS_ID, builder.build());
        }
    }

    public static void showBlockedCallNotification(Context context, String phoneNumber) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        createNotificationChannels(context);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String message = context.getString(R.string.notif_blocked_text, phoneNumber);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
                .setSmallIcon(R.drawable.ic_phone_blocked)
                .setContentTitle(context.getString(R.string.notif_blocked_title))
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openAppPendingIntent);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int notificationId = (int) (NOTIFICATION_ALERT_BASE_ID + (System.currentTimeMillis() % 10000));
            nm.notify(notificationId, builder.build());
        }
    }

    public static void cancelStatusNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_STATUS_ID);
        }
    }
}
