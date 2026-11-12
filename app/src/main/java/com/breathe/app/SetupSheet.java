package com.breathe.app;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/** Bottom sheet to tune breathing pattern and number of cycles before starting. */
public class SetupSheet {
    private static final String[] LABELS = {"Inhale (sec)", "Hold (sec)", "Exhale (sec)", "Hold after exhale (sec)"};
    private static final int[] MIN = {2, 0, 2, 0};
    private static final int[] MAX = {10, 10, 12, 10};

    public static void show(final Activity a, final int exIdx) {
        final Exercise ex = Exercise.ALL[exIdx];
        final int[] pat = Prefs.getPattern(a, exIdx);
        final int[] cyc = {Prefs.getCycles(a, exIdx)};

        final BottomSheetDialog d = new BottomSheetDialog(a);
        final View v = LayoutInflater.from(a).inflate(R.layout.dialog_setup, null);
        ((TextView) v.findViewById(R.id.s_title)).setText(ex.title);
        final LinearLayout box = v.findViewById(R.id.s_steppers);
        final TextView est = v.findViewById(R.id.s_total);

        final Runnable[] rebuild = new Runnable[1];
        final Runnable updateEst = () -> {
            int sum = 0; for (int x : pat) sum += x;
            est.setText("Total ≈ " + Prefs.formatDuration(sum * cyc[0]));
        };
        rebuild[0] = () -> {
            box.removeAllViews();
            for (int i = 0; i < 4; i++) {
                final int idx = i;
                addStepper(a, box, LABELS[i], MIN[i], MAX[i], pat[i], val -> { pat[idx] = val; updateEst.run(); });
            }
            addStepper(a, box, "Cycles", 1, 12, cyc[0], val -> { cyc[0] = val; updateEst.run(); });
            updateEst.run();
        };
        rebuild[0].run();

        v.findViewById(R.id.s_reset).setOnClickListener(x -> {
            System.arraycopy(ex.pattern, 0, pat, 0, 4);
            cyc[0] = 4;
            rebuild[0].run();
        });
        v.findViewById(R.id.s_start).setOnClickListener(x -> {
            Prefs.setPattern(a, exIdx, pat);
            Prefs.setCycles(a, exIdx, cyc[0]);
            Intent i = new Intent(a, BreathingActivity.class);
            i.putExtra("ex", exIdx).putExtra("pattern", pat.clone()).putExtra("cycles", cyc[0]);
            d.dismiss();
            a.startActivity(i);
            a.overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out);
        });
        d.setContentView(v);
        d.show();
    }

    private interface OnValue { void on(int v); }

    private static void addStepper(Activity a, LinearLayout parent, String label, final int min, final int max, int value, final OnValue cb) {
        View row = LayoutInflater.from(a).inflate(R.layout.item_stepper, parent, false);
        ((TextView) row.findViewById(R.id.st_label)).setText(label);
        final TextView val = row.findViewById(R.id.st_value);
        final int[] cur = {value};
        val.setText(String.valueOf(value));
        row.findViewById(R.id.st_minus).setOnClickListener(x -> {
            if (cur[0] > min) { cur[0]--; val.setText(String.valueOf(cur[0])); cb.on(cur[0]); }
        });
        row.findViewById(R.id.st_plus).setOnClickListener(x -> {
            if (cur[0] < max) { cur[0]++; val.setText(String.valueOf(cur[0])); cb.on(cur[0]); }
        });
        parent.addView(row);
    }
}
