package com.simplecallblocker;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_ROLE = 101;
    private static final int REQUEST_CODE_CONTACTS = 102;
    private static final int REQUEST_CODE_NOTIF = 103;

    private LinearLayout layoutStatusBg;
    private ImageView imgStatusIcon;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private SwitchMaterial switchBlocker;

    // Permission views
    private ImageView iconPermScreening;
    private MaterialButton btnGrantScreening;
    private ImageView iconPermContacts;
    private MaterialButton btnGrantContacts;
    private ImageView iconPermNotification;
    private MaterialButton btnGrantNotification;

    // History views
    private TextView tvHistoryTitle;
    private TextView tvEmptyHistory;
    private TextView btnClearHistory;
    private RecyclerView rvBlockedCalls;
    private BlockedCallsAdapter adapter;

    private boolean isReceiverRegistered = false;

    // Broadcast receiver to listen for toggle events from Notification or QS Tile
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshStatusCard();
            loadHistory();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            bindViews();
            setupListeners();
            setupRecyclerView();

            // Ensure notification channels are registered and notification is displayed
            NotificationHelper.createNotificationChannels(this);
            NotificationHelper.updateStatusNotification(this);
            Log.i(TAG, "MainActivity initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Startup warning: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (!isReceiverRegistered) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.registerReceiver(
                            this,
                            stateReceiver,
                            new IntentFilter(BlockerPreferences.ACTION_STATE_CHANGED),
                            ContextCompat.RECEIVER_NOT_EXPORTED
                    );
                } else {
                    registerReceiver(stateReceiver, new IntentFilter(BlockerPreferences.ACTION_STATE_CHANGED));
                }
                isReceiverRegistered = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver", e);
        }

        refreshStatusCard();
        checkPermissions();
        loadHistory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(stateReceiver);
            } catch (Exception ignored) {
            }
            isReceiverRegistered = false;
        }
    }

    private void bindViews() {
        layoutStatusBg = findViewById(R.id.layoutStatusBg);
        imgStatusIcon = findViewById(R.id.imgStatusIcon);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        switchBlocker = findViewById(R.id.switchBlocker);

        iconPermScreening = findViewById(R.id.iconPermScreening);
        btnGrantScreening = findViewById(R.id.btnGrantScreening);
        iconPermContacts = findViewById(R.id.iconPermContacts);
        btnGrantContacts = findViewById(R.id.btnGrantContacts);
        iconPermNotification = findViewById(R.id.iconPermNotification);
        btnGrantNotification = findViewById(R.id.btnGrantNotification);

        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        rvBlockedCalls = findViewById(R.id.rvBlockedCalls);
    }

    private void setupListeners() {
        setupSwitchListener();

        btnGrantScreening.setOnClickListener(v -> requestCallScreeningRole());
        btnGrantContacts.setOnClickListener(v -> requestContactsPermission());
        btnGrantNotification.setOnClickListener(v -> requestNotificationPermission());

        btnClearHistory.setOnClickListener(v -> {
            BlockerPreferences.clearBlockedCalls(this);
            loadHistory();
            Toast.makeText(this, "Call history cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSwitchListener() {
        switchBlocker.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked != BlockerPreferences.isBlockerEnabled(this)) {
                BlockerPreferences.setBlockerEnabled(this, isChecked);
                refreshStatusCard();
                NotificationHelper.updateStatusNotification(this);
                updateTileService();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new BlockedCallsAdapter();
        rvBlockedCalls.setLayoutManager(new LinearLayoutManager(this));
        rvBlockedCalls.setAdapter(adapter);
    }

    private void refreshStatusCard() {
        try {
            boolean isEnabled = BlockerPreferences.isBlockerEnabled(this);
            switchBlocker.setOnCheckedChangeListener(null);
            switchBlocker.setChecked(isEnabled);
            setupSwitchListener();

            if (isEnabled) {
                layoutStatusBg.setBackgroundResource(R.drawable.bg_status_active);
                imgStatusIcon.setImageResource(R.drawable.ic_shield);
                tvStatusTitle.setText(R.string.blocking_active);
                tvStatusDesc.setText(R.string.status_active_desc);
            } else {
                layoutStatusBg.setBackgroundResource(R.drawable.bg_status_inactive);
                imgStatusIcon.setImageResource(R.drawable.ic_shield_off);
                tvStatusTitle.setText(R.string.blocking_paused);
                tvStatusDesc.setText(R.string.status_paused_desc);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing status card", e);
        }
    }

    private void checkPermissions() {
        try {
            // 1. Call Screening Role
            boolean hasScreeningRole = isCallScreeningRoleHeld();
            if (hasScreeningRole) {
                iconPermScreening.setImageResource(R.drawable.ic_check);
                btnGrantScreening.setText(R.string.btn_granted);
                btnGrantScreening.setEnabled(false);
            } else {
                iconPermScreening.setImageResource(R.drawable.ic_warning);
                btnGrantScreening.setText(R.string.btn_grant);
                btnGrantScreening.setEnabled(true);
            }

            // 2. Contacts Permission
            boolean hasContactsPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED;
            if (hasContactsPerm) {
                iconPermContacts.setImageResource(R.drawable.ic_check);
                btnGrantContacts.setText(R.string.btn_granted);
                btnGrantContacts.setEnabled(false);
            } else {
                iconPermContacts.setImageResource(R.drawable.ic_warning);
                btnGrantContacts.setText(R.string.btn_grant);
                btnGrantContacts.setEnabled(true);
            }

            // 3. Notification Permission
            boolean hasNotifPerm = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasNotifPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
            }
            if (hasNotifPerm) {
                iconPermNotification.setImageResource(R.drawable.ic_check);
                btnGrantNotification.setText(R.string.btn_granted);
                btnGrantNotification.setEnabled(false);
            } else {
                iconPermNotification.setImageResource(R.drawable.ic_warning);
                btnGrantNotification.setText(R.string.btn_grant);
                btnGrantNotification.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
        }
    }

    private boolean isCallScreeningRoleHeld() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking role held", e);
        }
        return false;
    }

    private void requestCallScreeningRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
                    startActivityForResult(intent, REQUEST_CODE_ROLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting role", e);
            Toast.makeText(this, "Please set Call Blocker as default in Android Settings > Apps > Default Apps", Toast.LENGTH_LONG).show();
        }
    }

    private void requestContactsPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_CONTACTS},
                REQUEST_CODE_CONTACTS
        );
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_NOTIF
            );
        }
    }

    private void updateTileService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(
                        this,
                        new ComponentName(this, CallBlockerTileService.class)
                );
            } catch (Exception ignored) {
            }
        }
    }

    private void loadHistory() {
        try {
            List<BlockedCallItem> blockedCalls = BlockerPreferences.getBlockedCalls(this);
            if (blockedCalls == null || blockedCalls.isEmpty()) {
                tvEmptyHistory.setVisibility(View.VISIBLE);
                rvBlockedCalls.setVisibility(View.GONE);
                btnClearHistory.setVisibility(View.GONE);
                tvHistoryTitle.setText(R.string.title_history);
            } else {
                tvEmptyHistory.setVisibility(View.GONE);
                rvBlockedCalls.setVisibility(View.VISIBLE);
                btnClearHistory.setVisibility(View.VISIBLE);
                tvHistoryTitle.setText(getString(R.string.title_history) + " (" + blockedCalls.size() + ")");
                adapter.updateData(blockedCalls);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading history", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ROLE) {
            checkPermissions();
            if (isCallScreeningRoleHeld()) {
                Toast.makeText(this, "Call screening enabled successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
        if (requestCode == REQUEST_CODE_NOTIF) {
            NotificationHelper.updateStatusNotification(this);
        }
    }
}
