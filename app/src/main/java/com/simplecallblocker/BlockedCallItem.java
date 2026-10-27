package com.simplecallblocker;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents an incoming call blocked by the app.
 */
public class BlockedCallItem {
    private final String phoneNumber;
    private final long timestamp;

    public BlockedCallItem(String phoneNumber, long timestamp) {
        this.phoneNumber = phoneNumber;
        this.timestamp = timestamp;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("phoneNumber", phoneNumber);
            obj.put("timestamp", timestamp);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static BlockedCallItem fromJson(JSONObject obj) {
        String number = obj.optString("phoneNumber", "Unknown");
        long time = obj.optLong("timestamp", System.currentTimeMillis());
        return new BlockedCallItem(number, time);
    }
}
