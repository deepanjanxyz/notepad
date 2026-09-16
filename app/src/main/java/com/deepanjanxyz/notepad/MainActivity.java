package com.deepanjanxyz.notepad;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Main screen: shows the staggered-grid notes list with search, multi-select
 * deletion, and an optional biometric / device-credential lock gate.
 */
public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteListener {
    private static final String KEY_AUTHENTICATED = "is_authenticated";
    private static final String KEYSTORE_KEY_NAME = "elite_memo_lock_key";
    private static final int REQUEST_CONFIRM_CREDENTIAL = 1001;

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private DatabaseHelper dbHelper;
    private ArrayList<Note> noteList;
    private TextView emptyView;
    private Menu mainMenu;
    private boolean isSelectionMode = false;
    private boolean isAuthenticated = false;

    /** Sets up the activity, restoring the lock state across recreation and showing the biometric gate when enabled. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyUserTheme();
        super.onCreate(savedInstanceState);

        // Survive rotation / config changes without re-prompting for the lock
        if (savedInstanceState != null) {
            isAuthenticated = savedInstanceState.getBoolean(KEY_AUTHENTICATED, false);
        }

        if (isLockEnabled() && !isAuthenticated) {
            setContentView(new View(this));
            showBiometricPrompt();
        } else {
            initUI();
        }
    }

    /** Persists the authenticated state so a config change does not re-trigger the lock. */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_AUTHENTICATED, isAuthenticated);
    }

    /** Inflates the main layout and wires up the toolbar, notes list, empty view and add-note button. */
    private void initUI() {
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        noteList = new ArrayList<>();

        setSupportActionBar(findViewById(R.id.toolbar));
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new NoteAdapter(this, noteList, this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, NoteEditorActivity.class)));
        loadNotes("");
    }

    /** Returns whether the user has enabled the app lock in settings. */
    private boolean isLockEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("pref_lock", false);
    }

    /** Unlocks the app through a crypto-bound BiometricPrompt, falling back to the device credential gate. */
    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            /** Closes the locked activity when authentication cannot continue. */
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MainActivity.this, errString, Toast.LENGTH_SHORT).show();
                finish();
            }
            /** Unlocks and shows the main screen after a successful biometric authentication. */
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Success is cryptographically bound: it was authorized through a
                // Keystore key that itself requires user authentication, so simply
                // hooking this callback is not enough to bypass the lock
                isAuthenticated = true;
                initUI();
            }
        });

        // Bind the unlock to a Keystore key (crypto-bound authentication) instead
        // of relying only on the success callback
        BiometricPrompt.CryptoObject cryptoObject = createCryptoObject();

        if (cryptoObject != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On Android 11+ both biometric and device credential unlock work with
            // a CryptoObject, and the key allows either authenticator
            biometricPrompt.authenticate(
                    buildPromptInfo(BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL),
                    cryptoObject);
        } else if (cryptoObject != null && BiometricManager.from(this)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS) {
            // Android 10 and below with a strong biometric enrolled
            biometricPrompt.authenticate(
                    buildPromptInfo(BiometricManager.Authenticators.BIOMETRIC_STRONG),
                    cryptoObject);
        } else {
            // No crypto-capable biometric available: gate with the device
            // credential via KeyguardManager
            confirmDeviceCredential();
        }
    }

    /** Builds the prompt shown to the user for the given set of allowed authenticators. */
    private BiometricPrompt.PromptInfo buildPromptInfo(int authenticators) {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Elite Memo Security")
                .setSubtitle("Unlock to access your notes")
                .setAllowedAuthenticators(authenticators)
                .build();
    }

    /**
     * Creates a {@link BiometricPrompt.CryptoObject} backed by an AES key in
     * the Android Keystore that requires user authentication to use, so the
     * biometric unlock is cryptographically enforced. Returns null if the
     * Keystore is unavailable on this device.
     */
    private BiometricPrompt.CryptoObject createCryptoObject() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_KEY_NAME, null);
            if (secretKey == null) {
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEYSTORE_KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .setUserAuthenticationRequired(true)
                        .setInvalidatedByBiometricEnrollment(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Let the device credential unlock the key as well on Android 11+
                    builder.setUserAuthenticationParameters(0,
                            KeyProperties.AUTH_BIOMETRIC_STRONG | KeyProperties.AUTH_DEVICE_CREDENTIAL);
                }
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                keyGenerator.init(builder.build());
                keyGenerator.generateKey();
                secretKey = (SecretKey) keyStore.getKey(KEYSTORE_KEY_NAME, null);
            }
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return new BiometricPrompt.CryptoObject(cipher);
        } catch (Exception e) {
            // Keystore unavailable on this device; fall back to the device credential gate
            return null;
        }
    }

    /** Gates access with the system device credential (PIN/pattern/password) on devices without a crypto-capable biometric. */
    @SuppressWarnings("deprecation")
    private void confirmDeviceCredential() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "Elite Memo Security", "Unlock to access your notes");
            startActivityForResult(intent, REQUEST_CONFIRM_CREDENTIAL);
        } else {
            // No screen lock is configured, so there is nothing to
            // authenticate with. Never silently grant access in that case:
            // close the app and ask the user to set up a screen lock first.
            Toast.makeText(this, "Set a screen lock (PIN, pattern or password) to use App Lock", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /** Handles the result of the device credential confirmation flow. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            if (resultCode == RESULT_OK) {
                isAuthenticated = true;
                initUI();
            } else {
                finish();
            }
        }
    }

    /** Opens the tapped note in the editor. */
    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", note.getContent());
        startActivity(intent);
    }

    /** Reacts to multi-select mode changes by swapping the visible menu items and title. */
    @Override
    public void onSelectionModeChange(boolean selectionMode, int count) {
        this.isSelectionMode = selectionMode;
        if (mainMenu != null) {
            mainMenu.findItem(R.id.action_delete_selected).setVisible(selectionMode);
            mainMenu.findItem(R.id.action_search).setVisible(!selectionMode);
            mainMenu.findItem(R.id.action_settings).setVisible(!selectionMode);
            if (selectionMode) getSupportActionBar().setTitle(count + " Selected");
            else getSupportActionBar().setTitle("Elite Memo Pro");
        }
    }

    /** Exits selection mode on back press instead of leaving the activity. */
    @Override
    public void onBackPressed() {
        if (isSelectionMode && adapter != null) adapter.clearSelection();
        else super.onBackPressed();
    }

    /** Inflates the main menu and wires up live search. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.mainMenu = menu;
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search notes...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /** Applies the submitted query to the note list. */
            @Override public boolean onQueryTextSubmit(String query) { loadNotes(query); return false; }
            /** Filters the note list as the search text changes. */
            @Override public boolean onQueryTextChange(String newText) { loadNotes(newText); return false; }
        });
        // Reset the list when the search view is closed so the user is not
        // stuck looking at stale filtered results
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            /** Restores the full note list when the search view is closed. */
            @Override public boolean onClose() { loadNotes(""); return false; }
        });
        return true;
    }

    /** Handles settings navigation and multi-select deletion from the toolbar. */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
        else if (id == R.id.action_delete_selected) { showDeleteConfirmation(); return true; }
        return super.onOptionsItemSelected(item);
    }

    /** Asks for confirmation, then deletes every currently selected note. */
    private void showDeleteConfirmation() {
        List<Note> selected = adapter.getSelectedNotes();
        new AlertDialog.Builder(this)
            .setTitle("Delete Notes?")
            .setMessage("Delete " + selected.size() + " notes?")
            .setPositiveButton("Delete", (dialog, which) -> {
                for (Note note : selected) dbHelper.deleteNote(note.getId());
                adapter.clearSelection();
                loadNotes("");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /** Applies the theme chosen in settings (dark, light or follow system). */
    private void applyUserTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("pref_theme", "system");
        if (theme.equals("dark")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (theme.equals("light")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /** Reloads the notes when the list becomes visible again. */
    @Override protected void onResume() {
        super.onResume();
        if (isAuthenticated || !isLockEnabled()) loadNotes("");
    }

    /** Loads all notes, or only those matching {@code query}, into the list and toggles the empty view. */
    private void loadNotes(String query) {
        if (noteList == null) return;
        noteList.clear();
        Cursor cursor = null;
        try {
            cursor = (query == null || query.isEmpty()) ? dbHelper.getAllNotes() : dbHelper.searchNotes(query);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    noteList.add(new Note(
                        cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DATE))
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            // Always close the cursor, including when the result set is empty
            if (cursor != null) cursor.close();
        }
        if (noteList.isEmpty()) { recyclerView.setVisibility(View.GONE); emptyView.setVisibility(View.VISIBLE); }
        else { recyclerView.setVisibility(View.VISIBLE); emptyView.setVisibility(View.GONE); }
        adapter.notifyDataSetChanged();
    }
}
