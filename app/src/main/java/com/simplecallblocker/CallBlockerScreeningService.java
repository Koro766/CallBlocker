package com.simplecallblocker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Android Telecom CallScreeningService.
 * Intercepts incoming calls and dynamically checks contacts in real-time,
 * instantly blocking callers not saved in contacts.
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
            Log.d(TAG, "Caller is in contacts. Allowing call.");
            respondAllow(callDetails);
        } else {
            Log.d(TAG, "Caller is NOT in contacts. Blocking call instantly!");
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
                .setSkipNotification(true)    // Suppress system ringing and heads-up
                .setSkipCallLog(!keepInCallLog); // Optionally record in system dialer call log

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
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    /**
     * Dynamically verifies whether the phone number is saved in the user's contacts.
     * Uses a multi-strategy lookup (PhoneLookup, stripped digits, national suffix, and direct phone table matching)
     * so contacts added at any moment are recognized immediately.
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

        // Strategy 1: Real-time query to PhoneLookup with original number
        if (queryPhoneLookup(context, phoneNumber)) {
            return true;
        }

        // Strategy 2: Query with stripped formatting digits
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        if (!cleaned.isEmpty() && !cleaned.equals(phoneNumber)) {
            if (queryPhoneLookup(context, cleaned)) {
                return true;
            }
        }

        // Strategy 3: Query with national suffix (last 10 digits) if formatted with international country code
        if (cleaned.length() > 10) {
            String suffix = cleaned.substring(cleaned.length() - 10);
            if (queryPhoneLookup(context, suffix)) {
                return true;
            }
        }

        // Strategy 4: Fallback direct scan on CommonDataKinds.Phone with PhoneNumberUtils.compare
        return queryCommonDataKindsPhone(context, phoneNumber);
    }

    private static boolean queryPhoneLookup(Context context, String numberToSearch) {
        if (numberToSearch == null || numberToSearch.trim().isEmpty()) {
            return false;
        }

        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(numberToSearch)
        );

        String[] projection = new String[]{
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                Log.d(TAG, "Match found in PhoneLookup: " + contactName + " (" + numberToSearch + ")");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying PhoneLookup for " + numberToSearch, e);
        }
        return false;
    }

    private static boolean queryCommonDataKindsPhone(Context context, String incomingNumber) {
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
        )) {
            if (cursor != null) {
                int numberCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    String savedNumber = cursor.getString(numberCol);
                    if (savedNumber != null && PhoneNumberUtils.compare(context, incomingNumber, savedNumber)) {
                        String name = cursor.getString(nameCol);
                        Log.d(TAG, "Match found in CommonDataKinds.Phone: " + name + " (" + savedNumber + ")");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning CommonDataKinds.Phone", e);
        }
        return false;
    }
}
