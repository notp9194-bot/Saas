package com.breathe.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** 7 bars (last 7 days, today on the right) showing practice time per day. */
public class WeekChartView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final int cToday, cBar, cEmpty, cTextToday, cText;
    private final float d;
    private int[] sec = new int[7];
    private final String[] labels = new String[7];

    public WeekChartView(Context c) { this(c, null); }
    public WeekChartView(Context c, AttributeSet a) {
        super(c, a);
        d = getResources().getDisplayMetrics().density;
        cToday = ContextCompat.getColor(c, R.color.purple_primary);
        cBar = ContextCompat.getColor(c, R.color.purple_light);
        cEmpty = ContextCompat.getColor(c, R.color.lavender);
        cTextToday = ContextCompat.getColor(c, R.color.text_dark);
        cText = ContextCompat.getColor(c, R.color.text_gray);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(12 * d);
        buildLabels();
    }

    private void buildLabels() {
        SimpleDateFormat f = new SimpleDateFormat("EEE", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            String s = f.format(cal.getTime());
            labels[i] = s.isEmpty() ? "" : s.substring(0, 1).toUpperCase();
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    public void setData(int[] seconds) { sec = seconds; buildLabels(); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float labelH = 22 * d, chartH = h - labelH;
        float slot = w / 7f, barW = Math.min(26 * d, slot * 0.55f);
        int max = 60;
        for (int v : sec) max = Math.max(max, v);
        for (int i = 0; i < 7; i++) {
            float cx = slot * (i + 0.5f);
            float bh = sec[i] == 0 ? 6 * d : Math.max(10 * d, chartH * sec[i] / (float) max);
            bar.setColor(sec[i] == 0 ? cEmpty : (i == 6 ? cToday : cBar));
            r.set(cx - barW / 2, chartH - bh, cx + barW / 2, chartH);
            canvas.drawRoundRect(r, barW / 2, barW / 2, bar);
            text.setColor(i == 6 ? cTextToday : cText);
            text.setFakeBoldText(i == 6);
            canvas.drawText(labels[i], cx, h - 4 * d, text);
        }
    }
}
