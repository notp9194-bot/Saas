package com.breathe.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** One layout reused for Inhale / Hold / Exhale — only title, icon, timer & animation change. */
public class BreathingActivity extends AppCompatActivity {
    private static final String[] NAMES = {"Breathe In", "Hold", "Breathe Out", "Hold"};
    private static final String[] SUBS = {"Slowly...", "Don't breathe...", "Slowly...", "Stay relaxed..."};
    private static final int[] ICONS = {R.drawable.ic_lungs, R.drawable.ic_pause, R.drawable.ic_wind, R.drawable.ic_pause};

    private int exIdx; private int[] p;
    private int phase = 0, cycle = 0;
    private long phaseMs = 0, totalMs = 0, last, totalDur;
    private int cycles = 4;
    private boolean paused = false, finished = false, discard = false, resumed = false;

    private TextView title, sub, count; private CircularProgressView ring;
    private View inner; private ImageView icon, pauseBtn; private ProgressBar bar;
    private SoundPlayer sound;
    private Vibrator vibrator;
    private final Handler h = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = SystemClock.uptimeMillis();
            long dt = Math.min(now - last, 100); last = now;
            if (!paused) { phaseMs += dt; totalMs += dt; }
            long dur = p[phase] * 1000L;
            if (phaseMs >= dur) { if (!advance()) return; dur = p[phase] * 1000L; }
            float f = Math.min(1f, phaseMs / (float) dur);
            ring.setProgress(f);
            count.setText(String.valueOf((int) Math.ceil((dur - phaseMs) / 1000.0)));
            float s = phase == 0 ? 1f + 0.15f * f : phase == 1 ? 1.15f : phase == 2 ? 1.15f - 0.15f * f : 1f;
            inner.setScaleX(s); inner.setScaleY(s);
            bar.setProgress((int) (1000 * Math.min(1f, totalMs / (float) totalDur)));
            h.postDelayed(this, 40);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_breathing);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        exIdx = getIntent().getIntExtra("ex", 0);
        // resume: explicit request, or system recreated us (rotation / process death)
        Prefs.ResumeState rs = (getIntent().getBooleanExtra("resume", false) || b != null) ? Prefs.getResume(this) : null;
        if (rs != null) {
            exIdx = rs.ex; p = rs.pattern; cycles = rs.cycles;
            phase = rs.phase; cycle = rs.cycle; phaseMs = rs.phaseMs; totalMs = rs.totalMs;
            resumed = true;
        } else {
            int[] ip = getIntent().getIntArrayExtra("pattern");
            p = (ip != null && ip.length == 4) ? ip : Prefs.getPattern(this, exIdx);
            cycles = getIntent().getIntExtra("cycles", Prefs.getCycles(this, exIdx));
        }
        int sum = 0; for (int v : p) sum += v;
        totalDur = sum * 1000L * cycles;

        title = findViewById(R.id.tv_title); sub = findViewById(R.id.tv_sub); count = findViewById(R.id.tv_count);
        ring = findViewById(R.id.ring); inner = findViewById(R.id.inner); icon = findViewById(R.id.icon);
        pauseBtn = findViewById(R.id.btn_pause); bar = findViewById(R.id.bar);

        StringBuilder sb = new StringBuilder();
        for (int v : p) if (v > 0) { if (sb.length() > 0) sb.append(" - "); sb.append(v); }
        ((TextView) findViewById(R.id.tv_pattern)).setText(sb);

        pauseBtn.setOnClickListener(v -> setPaused(!paused));
        findViewById(R.id.btn_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmExit(); }
        });

        sound = new SoundPlayer();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        applyPhase(resumed);
        if (resumed) setPaused(true);   // let the user take a breath before continuing
        last = SystemClock.uptimeMillis();
        h.post(tick);
    }

    /** Back / top-left arrow: confirm before throwing away a running session. */
    private void confirmExit() {
        if (finished) return;
        if (totalMs < 3000) { discardAndFinish(); return; }   // barely started: just leave
        final boolean wasPaused = paused;
        setPaused(true);
        new AlertDialog.Builder(this)
                .setTitle("End session?")
                .setMessage("Your progress for this session won't be saved.")
                .setPositiveButton("End Session", (d, w) -> discardAndFinish())
                .setNegativeButton("Continue", (d, w) -> setPaused(wasPaused))
                .setOnCancelListener(d -> setPaused(wasPaused))
                .show();
    }

    private void discardAndFinish() {
        discard = true;
        Prefs.clearResume(this);
        finish();
    }

    private void setPaused(boolean v) {
        paused = v;
        pauseBtn.setImageResource(v ? R.drawable.ic_play : R.drawable.ic_pause);
    }

    private void applyPhase() { applyPhase(false); }

    private void applyPhase(boolean silent) {
        title.setText(NAMES[phase]); sub.setText(SUBS[phase]); icon.setImageResource(ICONS[phase]);
        ring.setProgress(0f);
        fadeIn(title, 0); fadeIn(sub, 80); fadeIn(icon, 40);
        icon.setScaleX(0.7f); icon.setScaleY(0.7f);
        icon.animate().scaleX(1f).scaleY(1f).setDuration(350).start();
        if (silent) return;
        if (Prefs.isSound(this) && sound != null) sound.play(phase);
        if (Prefs.isVibration(this) && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void fadeIn(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(12f);
        v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(350).start();
    }

    /** @return false when the session finished. */
    private boolean advance() {
        phaseMs = 0;
        do {
            phase++;
            if (phase == 4) {
                phase = 0; cycle++;
                if (cycle >= cycles) { complete(); return false; }
            }
        } while (p[phase] == 0);
        applyPhase();
        return true;
    }

    private void complete() {
        if (finished) return;
        finished = true;
        int sec = (int) Math.round(totalMs / 1000.0);
        Prefs.clearResume(this);
        Prefs.addSession(this, Exercise.ALL[exIdx].session, sec);
        Intent i = new Intent(this, CompleteActivity.class);
        i.putExtra("ex", exIdx); i.putExtra("sec", sec);
        i.putExtra("pattern", p.clone()); i.putExtra("cycles", cycles);
        startActivity(i);
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override public void finish() {
        super.finish();
        if (!finished) overridePendingTransition(R.anim.fade_in, R.anim.slide_down_out);
    }

    @Override protected void onPause() {
        super.onPause();
        if (finished || discard) return;
        setPaused(true);
        if (totalMs > 3000) {
            Prefs.ResumeState r = new Prefs.ResumeState();
            r.ex = exIdx; r.pattern = p; r.cycles = cycles; r.phase = phase; r.cycle = cycle;
            r.phaseMs = phaseMs; r.totalMs = totalMs;
            Prefs.saveResume(this, r);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacksAndMessages(null);
        if (sound != null) sound.release();
    }
}
