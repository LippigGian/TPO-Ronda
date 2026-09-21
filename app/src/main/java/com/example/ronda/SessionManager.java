package com.example.ronda;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public final class SessionManager {
    private static final String PREFERENCES_NAME = "ronda_secure_session";
    private static final String LEGACY_PREFERENCES_NAME = "ronda_session";
    private static final String TOKEN_KEY = "access_token";
    private static final String USER_ID_KEY = "user_id";
    private static final String USER_NOMBRE_KEY = "user_nombre";
    private final SharedPreferences preferences;

    @SuppressWarnings("deprecation")
    public SessionManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            Context appContext = context.getApplicationContext();
            preferences = EncryptedSharedPreferences.create(
                    appContext,
                    PREFERENCES_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            migrateLegacyToken(appContext);
        } catch (GeneralSecurityException | IOException error) {
            throw new IllegalStateException("No se pudo inicializar el almacenamiento seguro", error);
        }
    }

    public void saveToken(String token) {
        preferences.edit().putString(TOKEN_KEY, token).apply();
    }

    public String getToken() {
        return preferences.getString(TOKEN_KEY, null);
    }

    public void clearToken() {
        preferences.edit().remove(TOKEN_KEY).remove(USER_ID_KEY).remove(USER_NOMBRE_KEY).apply();
    }

    /** Guarda los datos del usuario logueado (vienen en la respuesta de login/OTP). **/
    public void saveUser(long id, String nombreUsuario) {
        preferences.edit()
                .putLong(USER_ID_KEY, id)
                .putString(USER_NOMBRE_KEY, nombreUsuario)
                .apply();
    }

    public long getUserId() {
        return preferences.getLong(USER_ID_KEY, -1L);
    }

    public String getUserNombre() {
        return preferences.getString(USER_NOMBRE_KEY, null);
    }

    // Migra la sesión previa y elimina la copia que estaba guardada sin cifrado.
    private void migrateLegacyToken(Context context) {
        SharedPreferences legacyPreferences = context.getSharedPreferences(
                LEGACY_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        String legacyToken = legacyPreferences.getString(TOKEN_KEY, null);

        if (legacyToken != null && getToken() == null) {
            saveToken(legacyToken);
        }
        legacyPreferences.edit().clear().apply();
    }
}
