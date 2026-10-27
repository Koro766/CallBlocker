package com.simplecallblocker;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/**
 * Handles toggling the call blocker from notification action buttons.
 */
public class ToggleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean currentState = BlockerPreferences.isBlockerEnabled(context);
        boolean newState = !currentState;
        BlockerPreferences.setBlockerEnabled(context, newState);

        // Update notification UI immediately
        NotificationHelper.updateStatusNotification(context);

        // Update Quick Settings Tile if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(
                        context,
                        new ComponentName(context, CallBlockerTileService.class)
                );
            } catch (Exception ignored) {
            }
        }

        // Notify active activity if visible
        Intent stateChangedIntent = new Intent(BlockerPreferences.ACTION_STATE_CHANGED);
        context.sendBroadcast(stateChangedIntent);

        // Feedback toast
        String message = newState ?
                context.getString(R.string.blocking_active) :
                context.getString(R.string.blocking_paused);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
