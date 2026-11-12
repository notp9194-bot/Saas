package com.breathe.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/** Continuously falling, swaying and spinning confetti. */
public class ConfettiView extends View {
    private static final int[] COLORS = {
            Color.parseColor("#7C4DFF"), Color.parseColor("#FF6FB5"),
            Color.parseColor("#FFB347"), Color.parseColor("#B9A4F5"), Color.parseColor("#5B2FD6")};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random(11);
    private float[] x, y, rot, size, vy, spin, ph;
    private int[] col;
    private float density = 1f;
    private ValueAnimator anim;
    private long last;

    public ConfettiView(Context c) { this(c, null); }
    public ConfettiView(Context c, AttributeSet a) { super(c, a); }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        density = getResources().getDisplayMetrics().density;
        int n = 36;
        x = new float[n]; y = new float[n]; rot = new float[n]; size = new float[n];
        vy = new float[n]; spin = new float[n]; ph = new float[n]; col = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = rnd.nextFloat() * w;
            y[i] = rnd.nextFloat() * h;
            rot[i] = rnd.nextFloat() * 180f;
            size[i] = (5 + rnd.nextFloat() * 6) * density;
            vy[i] = (60 + rnd.nextFloat() * 90) * density;   // px per second
            spin[i] = -120f + rnd.nextFloat() * 240f;        // deg per second
            ph[i] = rnd.nextFloat() * 6.28f;
            col[i] = COLORS[rnd.nextInt(COLORS.length)];
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        last = SystemClock.uptimeMillis();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> step());
        anim.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    private void step() {
        if (x == null) return;
        long now = SystemClock.uptimeMillis();
        float dt = Math.min((now - last) / 1000f, 0.05f);
        last = now;
        int h = getHeight(), w = getWidth();
        for (int i = 0; i < x.length; i++) {
            y[i] += vy[i] * dt;
            rot[i] += spin[i] * dt;
            ph[i] += dt * 2f;
            if (y[i] > h) { y[i] = -size[i] * 2; x[i] = rnd.nextFloat() * w; }
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (x == null) return;
        for (int i = 0; i < x.length; i++) {
            float px = x[i] + (float) Math.sin(ph[i]) * 12f * density;
            paint.setColor(col[i]);
            canvas.save();
            canvas.rotate(rot[i], px, y[i]);
            canvas.drawRect(px, y[i], px + size[i], y[i] + size[i] * 1.8f, paint);
            canvas.restore();
        }
    }
}
