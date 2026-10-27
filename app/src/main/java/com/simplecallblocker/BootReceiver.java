package com.simplecallblocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Ensures the persistent toggle notification is restored when the phone reboots.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (BlockerPreferences.isPersistentNotificationEnabled(context)) {
                NotificationHelper.updateStatusNotification(context);
            }
        }
    }
}
