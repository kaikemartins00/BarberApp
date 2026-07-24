package com.kaikeMartins.barberapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "barber_app";
    private static final String TOKEN_KEY = "jwt_token";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        preferences.edit().putString(TOKEN_KEY, token).apply();
    }

    public String getToken() {
        return preferences.getString(TOKEN_KEY, null);
    }

    public void logout() {
        preferences.edit().clear().apply();
    }
}