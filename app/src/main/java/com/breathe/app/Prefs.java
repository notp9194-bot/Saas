package com.breathe.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class Prefs {
    public static class Session { public String title; public int seconds; public long ts; }

    private static final long DAY = 86400000L;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("breathe_prefs", Context.MODE_PRIVATE);
    }

    // ---- profile (comes from Auth) ----
    public static String getName(Context c) { return Auth.name(c); }
    public static String getEmail(Context c) { return Auth.isGuest(c) ? "Guest mode" : Auth.email(c); }

    // ---- settings ----
    public static boolean isNotif(Context c) { return sp(c).getBoolean("notif", false); }
    public static void setNotif(Context c, boolean v) { sp(c).edit().putBoolean("notif", v).apply(); }
    public static boolean isSound(Context c) { return sp(c).getBoolean("sound", true); }
    public static void setSound(Context c, boolean v) { sp(c).edit().putBoolean("sound", v).apply(); }
    public static boolean isVibration(Context c) { return sp(c).getBoolean("vibration", true); }
    public static void setVibration(Context c, boolean v) { sp(c).edit().putBoolean("vibration", v).apply(); }
    public static int reminderHour(Context c) { return sp(c).getInt("rem_h", 20); }
    public static int reminderMinute(Context c) { return sp(c).getInt("rem_m", 0); }
    public static void setReminder(Context c, int h, int m) {
        sp(c).edit().putInt("rem_h", h).putInt("rem_m", m).apply();
    }

    /** 0 = system, 1 = light (default), 2 = dark. */
    public static int getTheme(Context c) { return sp(c).getInt("theme", 1); }
    public static void setTheme(Context c, int t) { sp(c).edit().putInt("theme", t).apply(); }
    public static int nightMode(Context c) {
        int t = getTheme(c);
        return t == 2 ? AppCompatDelegate.MODE_NIGHT_YES
                : t == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    // ---- history (per account) ----
    private static String historyKey(Context c) { return "history_" + Auth.email(c); }

    public static void addSession(Context c, String title, int seconds) {
        try {
            JSONArray a = new JSONArray(sp(c).getString(historyKey(c), "[]"));
            JSONObject o = new JSONObject();
            o.put("t", title); o.put("s", seconds); o.put("ts", System.currentTimeMillis());
            a.put(o);
            sp(c).edit().putString(historyKey(c), a.toString()).apply();
        } catch (JSONException ignored) { }
    }

    /** Newest first. */
    public static List<Session> getSessions(Context c) {
        List<Session> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString(historyKey(c), "[]"));
            for (int i = a.length() - 1; i >= 0; i--) {
                JSONObject o = a.getJSONObject(i);
                Session s = new Session();
                s.title = o.getString("t"); s.seconds = o.getInt("s"); s.ts = o.getLong("ts");
                list.add(s);
            }
        } catch (JSONException ignored) { }
        return list;
    }

    private static long dayOf(long ts) {
        return (ts + TimeZone.getDefault().getOffset(ts)) / DAY;
    }

    public static boolean practicedToday(Context c) {
        long today = dayOf(System.currentTimeMillis());
        for (Session s : getSessions(c)) if (dayOf(s.ts) == today) return true;
        return false;
    }

    public static int streak(Context c) {
        Set<Long> days = new HashSet<>();
        for (Session s : getSessions(c)) days.add(dayOf(s.ts));
        long d = dayOf(System.currentTimeMillis());
        if (!days.contains(d)) d--;
        int n = 0;
        while (days.contains(d)) { n++; d--; }
        return n;
    }

    // ---- exercise customization ----
    public static int[] getPattern(Context c, int ex) {
        String def = Exercise.ALL[ex].pattern[0] + "," + Exercise.ALL[ex].pattern[1] + ","
                + Exercise.ALL[ex].pattern[2] + "," + Exercise.ALL[ex].pattern[3];
        return parsePattern(sp(c).getString("pattern_" + ex, def), Exercise.ALL[ex].pattern);
    }
    public static void setPattern(Context c, int ex, int[] p) {
        sp(c).edit().putString("pattern_" + ex, joinPattern(p)).apply();
    }
    public static int getCycles(Context c, int ex) { return sp(c).getInt("cycles_" + ex, 4); }
    public static void setCycles(Context c, int ex, int n) { sp(c).edit().putInt("cycles_" + ex, n).apply(); }

    static String joinPattern(int[] p) { return p[0] + "," + p[1] + "," + p[2] + "," + p[3]; }
    static int[] parsePattern(String s, int[] fallback) {
        try {
            String[] t = s.split(",");
            int[] r = new int[4];
            for (int i = 0; i < 4; i++) r[i] = Integer.parseInt(t[i].trim());
            return r;
        } catch (Exception e) { return fallback.clone(); }
    }

    // ---- resume an interrupted session ----
    public static class ResumeState {
        public int ex, cycles, phase, cycle; public int[] pattern; public long phaseMs, totalMs, ts;
        public long totalDur() { int s = 0; for (int v : pattern) s += v; return s * 1000L * cycles; }
    }

    private static String resumeKey(Context c) { return "resume_" + Auth.email(c); }

    public static void saveResume(Context c, ResumeState r) {
        try {
            JSONObject o = new JSONObject();
            o.put("ex", r.ex); o.put("p", joinPattern(r.pattern)); o.put("cy", r.cycles);
            o.put("ph", r.phase); o.put("c", r.cycle); o.put("pm", r.phaseMs); o.put("tm", r.totalMs);
            o.put("ts", System.currentTimeMillis());
            sp(c).edit().putString(resumeKey(c), o.toString()).apply();
        } catch (JSONException ignored) { }
    }

    /** Null when nothing to resume (or the saved session is older than 12 hours). */
    public static ResumeState getResume(Context c) {
        String raw = sp(c).getString(resumeKey(c), null);
        if (raw == null) return null;
        try {
            JSONObject o = new JSONObject(raw);
            ResumeState r = new ResumeState();
            r.ex = o.getInt("ex"); r.pattern = parsePattern(o.getString("p"), Exercise.ALL[r.ex].pattern);
            r.cycles = o.getInt("cy"); r.phase = o.getInt("ph"); r.cycle = o.getInt("c");
            r.phaseMs = o.getLong("pm"); r.totalMs = o.getLong("tm"); r.ts = o.getLong("ts");
            if (System.currentTimeMillis() - r.ts > 12 * 3600000L) { clearResume(c); return null; }
            return r;
        } catch (Exception e) { clearResume(c); return null; }
    }

    public static void clearResume(Context c) { sp(c).edit().remove(resumeKey(c)).apply(); }

    // ---- weekly stats ----
    /** Seconds practiced per day for the last 7 days; index 6 = today. */
    public static int[] weekSeconds(Context c) {
        int[] r = new int[7];
        long today = dayOf(System.currentTimeMillis());
        for (Session s : getSessions(c)) {
            int idx = 6 - (int) (today - dayOf(s.ts));
            if (idx >= 0 && idx < 7) r[idx] += s.seconds;
        }
        return r;
    }

    /** 45s -> "45 sec", 120s -> "2 min", 64s -> "1 min 4 sec". */
    public static String formatDuration(int sec) {
        if (sec < 60) return Math.max(1, sec) + " sec";
        int m = sec / 60, s = sec % 60;
        return s == 0 ? m + " min" : m + " min " + s + " sec";
    }
}
