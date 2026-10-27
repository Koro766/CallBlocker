package com.simplecallblocker;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

/**
 * Quick Settings Tile service that provides a quick toggle in the
 * notification dropdown shade just like Wi-Fi and Bluetooth.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class CallBlockerTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean currentState = BlockerPreferences.isBlockerEnabled(this);
        boolean newState = !currentState;
        BlockerPreferences.setBlockerEnabled(this, newState);

        updateTileState();
        NotificationHelper.updateStatusNotification(this);

        // Notify active activity if visible
        Intent stateChangedIntent = new Intent(BlockerPreferences.ACTION_STATE_CHANGED);
        sendBroadcast(stateChangedIntent);
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isEnabled = BlockerPreferences.isBlockerEnabled(this);

        tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(
                this,
                isEnabled ? R.drawable.ic_shield : R.drawable.ic_shield_off
        ));
        tile.setLabel(getString(R.string.tile_label));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(isEnabled ?
                    getString(R.string.blocking_active) :
                    getString(R.string.blocking_paused));
        }

        tile.updateTile();
    }
}
