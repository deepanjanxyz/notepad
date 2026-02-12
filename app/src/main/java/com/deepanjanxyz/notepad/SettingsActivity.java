package com.deepanjanxyz.notepad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // থিম সেট করা হচ্ছে
        setTheme(R.style.Theme_EliteMemoPro_Settings);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final String PREF_THEME = "pref_theme";
        private static final String PREF_LOCK = "pref_lock";
        private static final String PREF_PIN = "pref_pin";
        private static final String PREF_BACKUP = "pref_backup";
        private static final String PREF_RESTORE = "pref_restore";
        private static final String PREF_ABOUT = "pref_about";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            ListPreference themePref = findPreference(PREF_THEME);
            if (themePref != null) {
                themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String mode = (String) newValue;
                    applyTheme(mode);
                    requireActivity().recreate();
                    return true;
                });
            }

            SwitchPreferenceCompat lockPref = findPreference(PREF_LOCK);
            if (lockPref != null) {
                lockPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled) checkBiometricOrSetPin();
                    return true;
                });
            }

            Preference backupPref = findPreference(PREF_BACKUP);
            if (backupPref != null) {
                backupPref.setOnPreferenceClickListener(p -> {
                    exportDatabase();
                    return true;
                });
            }

            Preference restorePref = findPreference(PREF_RESTORE);
            if (restorePref != null) {
                restorePref.setOnPreferenceClickListener(p -> {
                    restoreDatabase();
                    return true;
                });
            }

            Preference aboutPref = findPreference(PREF_ABOUT);
            if (aboutPref != null) {
                String version = getAppVersion();
                aboutPref.setSummary("Version " + version + "\nDeveloped by Deepanjan");
                aboutPref.setOnPreferenceClickListener(p -> {
                    openGitHub();
                    return true;
                });
            }
        }

        private void applyTheme(String mode) {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            prefs.edit().putString(PREF_THEME, mode).apply();
        }

        private void checkBiometricOrSetPin() {
            BiometricManager bm = BiometricManager.from(requireContext());
            int can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            if (can == BiometricManager.BIOMETRIC_SUCCESS) {
                showBiometricPromptForSetup();
            } else {
                EditTextPreference pinPref = findPreference(PREF_PIN);
                if (pinPref != null) pinPref.setVisible(true);
            }
        }

        private void showBiometricPromptForSetup() {
            BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()), new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    Toast.makeText(getContext(), "Biometric enabled", Toast.LENGTH_SHORT).show();
                }
            });
            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder().setTitle("Enable App Lock").setNegativeButtonText("Cancel").build();
            prompt.authenticate(info);
        }

        private void exportDatabase() {
            File db = requireContext().getDatabasePath("EliteMemoPro.db");
            File backup = new File(requireContext().getExternalFilesDir(null), "EliteMemoPro_backup.db");
            try (FileInputStream in = new FileInputStream(db); FileOutputStream out = new FileOutputStream(backup)) {
                in.getChannel().transferTo(0, in.getChannel().size(), out.getChannel());
                Toast.makeText(getContext(), "Backup saved!", Toast.LENGTH_LONG).show();
            } catch (IOException e) { e.printStackTrace(); }
        }

        private void restoreDatabase() {
            File backup = new File(requireContext().getExternalFilesDir(null), "EliteMemoPro_backup.db");
            File db = requireContext().getDatabasePath("EliteMemoPro.db");
            try (FileInputStream in = new FileInputStream(backup); FileOutputStream out = new FileOutputStream(db)) {
                in.getChannel().transferTo(0, in.getChannel().size(), out.getChannel());
                Toast.makeText(getContext(), "Restored! Restart app.", Toast.LENGTH_LONG).show();
            } catch (IOException e) { e.printStackTrace(); }
        }

        private String getAppVersion() {
            try { return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName; }
            catch (Exception e) { return "1.0"; }
        }

        private void openGitHub() {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deepanjanxyz")));
        }
    }
}
