package com.simplecallblocker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages persisted state and settings for the call blocker.
 */
public class BlockerPreferences {
    private static final String PREF_NAME = "call_blocker_prefs";
    private static final String KEY_BLOCKER_ENABLED = "blocker_enabled";
    private static final String KEY_BLOCKED_CALLS = "blocked_calls_json";
    private static final String KEY_KEEP_IN_CALL_LOG = "keep_in_call_log";
    private static final String KEY_NOTIFY_ON_BLOCK = "notify_on_block";
    private static final String KEY_SHOW_PERSISTENT_NOTIF = "show_persistent_notif";

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
