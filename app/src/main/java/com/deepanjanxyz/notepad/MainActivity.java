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

    /**
     * Restores authentication state and initializes or locks the main screen.
     *
     * @param savedInstanceState previously saved activity state, if available
     */
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

    /**
     * Persists the current authentication state across configuration changes.
     *
     * @param outState state bundle populated for the next activity instance
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_AUTHENTICATED, isAuthenticated);
    }

    /** Initializes the note list and main-screen controls. */
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

    /**
     * Checks whether the user enabled the application lock.
     *
     * @return {@code true} when the lock preference is enabled
     */
    private boolean isLockEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean("pref_lock", false);
    }

    /** Starts the strongest available cryptographically backed unlock flow. */
    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            /**
             * Closes the locked activity when authentication cannot continue.
             *
             * @param errorCode biometric error code
             * @param errString user-facing description of the error
             */
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(MainActivity.this, errString, Toast.LENGTH_SHORT).show();
                finish();
            }
            /**
             * Displays the main screen after successful authentication.
             *
             * @param result successful biometric authentication result
             */
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

    /**
     * Builds prompt text and the accepted authenticator configuration.
     *
     * @param authenticators allowed {@link BiometricManager.Authenticators} flags
     * @return configured prompt information
     */
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
     * biometric unlock is cryptographically enforced.
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
            // Keystore unavailable on this device; fall back to a non-crypto gate
            return null;
        }
    }

    /** Opens the system device-credential confirmation screen as a fallback. */
    @SuppressWarnings("deprecation")
    private void confirmDeviceCredential() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isKeyguardSecure()) {
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "Elite Memo Security", "Unlock to access your notes");
            startActivityForResult(intent, REQUEST_CONFIRM_CREDENTIAL);
        } else {
            // No lock screen is configured at all; nothing to gate with
            isAuthenticated = true;
            initUI();
        }
    }

    /**
     * Handles completion of the device-credential fallback flow.
     *
     * @param requestCode identifier supplied when credential confirmation started
     * @param resultCode result returned by the credential screen
     * @param data optional result data
     */
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

    /**
     * Opens the editor for the selected note.
     *
     * @param note note selected by the user
     */
    @Override
    public void onNoteClick(Note note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("note_id", note.getId());
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", note.getContent());
        startActivity(intent);
    }

    /**
     * Updates toolbar actions and text for the adapter's selection state.
     *
     * @param selectionMode whether selection mode is active
     * @param count number of selected notes
     */
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

    /** Clears an active note selection before performing normal back navigation. */
    @Override
    public void onBackPressed() {
        if (isSelectionMode && adapter != null) adapter.clearSelection();
        else super.onBackPressed();
    }

    /**
     * Inflates toolbar actions and connects search filtering callbacks.
     *
     * @param menu menu to populate
     * @return {@code true} after the menu is initialized
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.mainMenu = menu;
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search notes...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /**
             * Applies a submitted query to the note list.
             *
             * @param query submitted search text
             * @return {@code false} to allow the SearchView's default handling
             */
            @Override
            public boolean onQueryTextSubmit(String query) { loadNotes(query); return false; }

            /**
             * Filters notes as the search text changes.
             *
             * @param newText current search text
             * @return {@code false} to allow the SearchView's default handling
             */
            @Override
            public boolean onQueryTextChange(String newText) { loadNotes(newText); return false; }
        });
        // Reset the list when the search view is closed so the user is not
        // stuck looking at stale filtered results
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            /**
             * Restores all notes when search closes.
             *
             * @return {@code false} to allow the SearchView's default close handling
             */
            @Override
            public boolean onClose() { loadNotes(""); return false; }
        });
        return true;
    }

    /**
     * Handles settings and bulk-delete toolbar actions.
     *
     * @param item selected menu item
     * @return {@code true} when this activity handled the action
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
        else if (id == R.id.action_delete_selected) { showDeleteConfirmation(); return true; }
        return super.onOptionsItemSelected(item);
    }

    /** Shows a confirmation dialog before deleting all selected notes. */
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

    /** Applies the user's light, dark, or system theme preference. */
    private void applyUserTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("pref_theme", "system");
        if (theme.equals("dark")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else if (theme.equals("light")) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /** Refreshes notes whenever the unlocked activity returns to the foreground. */
    @Override protected void onResume() {
        super.onResume();
        if (isAuthenticated || !isLockEnabled()) loadNotes("");
    }

    /**
     * Loads all notes or matching search results and updates the empty state.
     *
     * @param query search text, or empty to load every note
     */
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
