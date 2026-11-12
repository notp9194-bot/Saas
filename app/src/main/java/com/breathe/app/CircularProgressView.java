package com.breathe.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 0f;

    public CircularProgressView(Context c) { this(c, null); }
    public CircularProgressView(Context c, AttributeSet a) {
        super(c, a);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(getResources().getDisplayMetrics().density * 14f);
        paint.setColor(getResources().getColor(R.color.purple_primary));
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(paint.getStrokeWidth());
        track.setColor(getResources().getColor(R.color.ring_track));
    }

    public void setProgress(float p) { progress = p; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        float half = paint.getStrokeWidth() / 2f;
        rect.set(half, half, getWidth() - half, getHeight() - half);
        canvas.drawArc(rect, 0f, 360f, false, track);
        canvas.drawArc(rect, -90f, 360f * progress, false, paint);
    }
}
