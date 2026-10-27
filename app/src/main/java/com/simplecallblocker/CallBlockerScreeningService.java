package com.simplecallblocker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Android Telecom CallScreeningService.
 * Intercepts incoming calls and instantly blocks those that are not saved in contacts.
 */
public class CallBlockerScreeningService extends CallScreeningService {
    private static final String TAG = "CallBlockerService";

    @Override
    public void onScreenCall(Call.Details callDetails) {
        if (callDetails == null) return;

        // Only intercept incoming calls
        if (callDetails.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            respondAllow(callDetails);
            return;
        }

        // Check if blocking is toggled ON
        if (!BlockerPreferences.isBlockerEnabled(this)) {
            Log.d(TAG, "Call Blocker is toggled OFF. Allowing call.");
            respondAllow(callDetails);
            return;
        }

        Uri handle = callDetails.getHandle();
        String rawNumber = (handle != null) ? handle.getSchemeSpecificPart() : null;

        Log.d(TAG, "Screening incoming call from: " + (rawNumber != null ? rawNumber : "Unknown/Private"));

        // If no caller ID / private number, it cannot be in contacts
        boolean inContacts = false;
        if (rawNumber != null && !rawNumber.trim().isEmpty()) {
            inContacts = isNumberInContacts(this, rawNumber);
        }

        if (inContacts) {
            Log.d(TAG, "Number is in contacts. Allowing call.");
            respondAllow(callDetails);
        } else {
            Log.d(TAG, "Number is NOT in contacts. Blocking call instantly!");
            respondBlock(callDetails, rawNumber);
        }
    }

    private void respondAllow(Call.Details callDetails) {
        CallResponse response = new CallResponse.Builder().build();
        respondToCall(callDetails, response);
    }

    private void respondBlock(Call.Details callDetails, String rawNumber) {
        boolean keepInCallLog = BlockerPreferences.shouldKeepInCallLog(this);

        CallResponse.Builder responseBuilder = new CallResponse.Builder()
                .setDisallowCall(true)        // Block call from connecting
                .setRejectCall(true)          // Disconnect/reject instantly
                .setSkipNotification(true)    // Suppress system ringing and incoming call heads-up
                .setSkipCallLog(!keepInCallLog); // Optionally keep or hide from system dialer history

        respondToCall(callDetails, responseBuilder.build());

        String displayNum = (rawNumber != null && !rawNumber.trim().isEmpty()) ? rawNumber : "Private / Unknown";

        // Record in app's blocked call history
        BlockerPreferences.addBlockedCall(this, displayNum);

        // Notify user of blocked call if enabled
        if (BlockerPreferences.shouldNotifyOnBlock(this)) {
            NotificationHelper.showBlockedCallNotification(this, displayNum);
        }

        // Notify UI to refresh blocked call list
        Intent intent = new Intent(BlockerPreferences.ACTION_STATE_CHANGED);
        sendBroadcast(intent);
    }

    /**
     * Checks if the phone number belongs to any contact in the user's address book.
     */
    public static boolean isNumberInContacts(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CONTACTS permission is not granted! Cannot verify contact.");
            return false;
        }

        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
        );

        String[] projection = new String[]{
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                Log.d(TAG, "Match found in contacts: " + contactName);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error looking up contact for number " + phoneNumber, e);
        }

        return false;
    }
}
