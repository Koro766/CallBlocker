package com.simplecallblocker;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages persisted state, app visibility, and settings for Call Blocker.
 */
public class BlockerPreferences {
    private static final String TAG = "BlockerPreferences";
    private static final String PREF_NAME = "call_blocker_prefs";
    private static final String KEY_BLOCKER_ENABLED = "blocker_enabled";
    private static final String KEY_BLOCKED_CALLS = "blocked_calls_json";
    private static final String KEY_KEEP_IN_CALL_LOG = "keep_in_call_log";
    private static final String KEY_NOTIFY_ON_BLOCK = "notify_on_block";
    private static final String KEY_SHOW_PERSISTENT_NOTIF = "show_persistent_notif";
    private static final String KEY_APP_HIDDEN = "key_app_hidden";

    public static final String ACTION_STATE_CHANGED = "com.simplecallblocker.ACTION_STATE_CHANGED";
    public static final String ACTION_TOGGLE_BLOCKER = "com.simplecallblocker.ACTION_TOGGLE_BLOCKER";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isBlockerEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_BLOCKER_ENABLED, true);
    }

    public static void setBlockerEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_BLOCKER_ENABLED, enabled).apply();
    }

    public static boolean shouldKeepInCallLog(Context context) {
        return getPrefs(context).getBoolean(KEY_KEEP_IN_CALL_LOG, true);
    }

    public static boolean shouldNotifyOnBlock(Context context) {
        return getPrefs(context).getBoolean(KEY_NOTIFY_ON_BLOCK, true);
    }

    public static boolean isPersistentNotificationEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_SHOW_PERSISTENT_NOTIF, true);
    }

    /**
     * Checks if the app is currently hidden from the phone's app launcher.
     */
    public static boolean isAppHidden(Context context) {
        try {
            ComponentName alias = new ComponentName(context, "com.simplecallblocker.LauncherAlias");
            int state = context.getPackageManager().getComponentEnabledSetting(alias);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Exception e) {
            return getPrefs(context).getBoolean(KEY_APP_HIDDEN, false);
        }
    }

    /**
     * Hides or reveals the app icon in the phone's launcher.
     * When hidden, the app can still be opened by holding the Quick Settings Tile or tapping the notification.
     */
    public static void setAppHidden(Context context, boolean hide) {
        getPrefs(context).edit().putBoolean(KEY_APP_HIDDEN, hide).apply();
        try {
            ComponentName alias = new ComponentName(context, "com.simplecallblocker.LauncherAlias");
            int newState = hide ?
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

            context.getPackageManager().setComponentEnabledSetting(
                    alias,
                    newState,
                    PackageManager.DONT_KILL_APP
            );
            Log.i(TAG, "Launcher icon visibility updated: hidden = " + hide);
        } catch (Exception e) {
            Log.e(TAG, "Error changing launcher icon visibility", e);
        }
    }

    /**
     * Dynamically queries the contact provider to get the current total number of saved contacts.
     */
    public static int getContactCount(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }

        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts._ID},
                null,
                null,
                null
        )) {
            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying contact count", e);
        }
        return 0;
    }

    public static synchronized List<BlockedCallItem> getBlockedCalls(Context context) {
        List<BlockedCallItem> list = new ArrayList<>();
        String jsonStr = getPrefs(context).getString(KEY_BLOCKED_CALLS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(BlockedCallItem.fromJson(obj));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public static synchronized void addBlockedCall(Context context, String phoneNumber) {
        List<BlockedCallItem> current = getBlockedCalls(context);
        current.add(0, new BlockedCallItem(phoneNumber, System.currentTimeMillis()));

        // Keep last 100 entries max
        if (current.size() > 100) {
            current = current.subList(0, 100);
        }

        JSONArray arr = new JSONArray();
        for (BlockedCallItem item : current) {
            arr.put(item.toJson());
        }

        getPrefs(context).edit().putString(KEY_BLOCKED_CALLS, arr.toString()).apply();
    }

    public static synchronized void clearBlockedCalls(Context context) {
        getPrefs(context).edit().remove(KEY_BLOCKED_CALLS).apply();
    }
}
