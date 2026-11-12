package com.breathe.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Patterns;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Local on-device accounts. Passwords are never stored in plain text
 * (PBKDF2-HMAC-SHA256 + random salt). Swap this class for Firebase Auth later
 * without touching the UI.
 */
public class Auth {
    public static final String GUEST = "guest";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("breathe_auth", Context.MODE_PRIVATE);
    }

    private static JSONObject accounts(Context c) {
        try { return new JSONObject(sp(c).getString("accounts", "{}")); }
        catch (JSONException e) { return new JSONObject(); }
    }

    private static byte[] hash(String pw, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 10000, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    /** @return error message, or null on success (user is logged in). */
    public static String register(Context c, String name, String email, String pw) {
        name = name.trim(); email = email.trim().toLowerCase();
        if (name.isEmpty()) return "Please enter your name.";
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Enter a valid email address.";
        if (pw.length() < 6) return "Password must be at least 6 characters.";
        JSONObject all = accounts(c);
        if (all.has(email)) return "An account with this email already exists.";
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            JSONObject a = new JSONObject();
            a.put("n", name);
            a.put("s", Base64.encodeToString(salt, Base64.NO_WRAP));
            a.put("h", Base64.encodeToString(hash(pw, salt), Base64.NO_WRAP));
            all.put(email, a);
            sp(c).edit().putString("accounts", all.toString()).putString("current", email).apply();
            return null;
        } catch (Exception e) {
            return "Could not create account. Try again.";
        }
    }

    /** @return error message, or null on success. */
    public static String login(Context c, String email, String pw) {
        email = email.trim().toLowerCase();
        if (email.isEmpty() || pw.isEmpty()) return "Enter your email and password.";
        JSONObject a = accounts(c).optJSONObject(email);
        if (a == null) return "No account found for this email.";
        try {
            byte[] salt = Base64.decode(a.getString("s"), Base64.NO_WRAP);
            byte[] expected = Base64.decode(a.getString("h"), Base64.NO_WRAP);
            if (!MessageDigest.isEqual(expected, hash(pw, salt))) return "Incorrect password.";
            sp(c).edit().putString("current", email).apply();
            return null;
        } catch (Exception e) {
            return "Login failed. Try again.";
        }
    }

    public static void guest(Context c) { sp(c).edit().putString("current", GUEST).apply(); }
    public static void logout(Context c) { sp(c).edit().remove("current").apply(); }

    public static boolean isLoggedIn(Context c) { return sp(c).getString("current", null) != null; }
    public static boolean isGuest(Context c) { return GUEST.equals(sp(c).getString("current", null)); }
    public static String email(Context c) { return sp(c).getString("current", ""); }

    public static String name(Context c) {
        if (isGuest(c)) return "Guest";
        JSONObject a = accounts(c).optJSONObject(email(c));
        return a == null ? "" : a.optString("n", "");
    }

    public static void setName(Context c, String name) {
        if (isGuest(c)) return;
        try {
            JSONObject all = accounts(c);
            JSONObject a = all.optJSONObject(email(c));
            if (a == null) return;
            a.put("n", name);
            sp(c).edit().putString("accounts", all.toString()).apply();
        } catch (JSONException ignored) { }
    }
}
