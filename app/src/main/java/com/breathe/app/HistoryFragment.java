package com.breathe.app;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    private static final int[] DAYS = {0, 7, 30};

    private LinearLayout list;
    private View empty;
    private TextView[] chips;
    private TextView chipDay;
    private int filterDays = 0;      // 0 = all
    private long dayStart = 0;       // 0 = no single-day filter, else local midnight of picked day

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_history, c, false);
        list = v.findViewById(R.id.list);
        empty = v.findViewById(R.id.empty);
        chipDay = v.findViewById(R.id.chip_day);
        chips = new TextView[]{v.findViewById(R.id.chip_all), v.findViewById(R.id.chip_7), v.findViewById(R.id.chip_30)};
        for (int i = 0; i < chips.length; i++) {
            final int d = DAYS[i];
            chips[i].setOnClickListener(x -> { filterDays = d; dayStart = 0; render(); });
        }
        chipDay.setOnClickListener(x -> { dayStart = 0; render(); });   // tap to clear the date filter
        v.findViewById(R.id.btn_cal).setOnClickListener(x -> pickDate());
        render();
        return v;
    }

    @Override public void onResume() { super.onResume(); if (list != null) render(); }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        if (dayStart != 0) c.setTimeInMillis(dayStart);
        DatePickerDialog dlg = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            Calendar p = Calendar.getInstance();
            p.set(y, m, d, 0, 0, 0);
            p.set(Calendar.MILLISECOND, 0);
            dayStart = p.getTimeInMillis();
            render();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMaxDate(System.currentTimeMillis());
        dlg.show();
    }

    private void style(TextView t, boolean selected) {
        t.setBackgroundResource(selected ? R.drawable.bg_purple_gradient : R.drawable.bg_icon_container);
        t.setTextColor(selected ? Color.WHITE : ContextCompat.getColor(requireContext(), R.color.text_dark));
    }

    private void render() {
        boolean dayMode = dayStart != 0;
        for (int i = 0; i < chips.length; i++) style(chips[i], !dayMode && DAYS[i] == filterDays);
        chipDay.setVisibility(dayMode ? View.VISIBLE : View.GONE);
        if (dayMode) {
            chipDay.setText(new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(dayStart)) + "  ✕");
            style(chipDay, true);
        }

        long from, to;
        if (dayMode) {
            Calendar e = Calendar.getInstance();
            e.setTimeInMillis(dayStart);
            e.add(Calendar.DAY_OF_YEAR, 1);
            from = dayStart; to = e.getTimeInMillis();
        } else {
            from = filterDays == 0 ? 0 : System.currentTimeMillis() - filterDays * 86400000L;
            to = Long.MAX_VALUE;
        }

        list.removeAllViews();
        List<Prefs.Session> all = Prefs.getSessions(requireContext());
        int shown = 0;
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (Prefs.Session s : all) {
            if (s.ts < from || s.ts >= to) continue;
            if (shown > 0) {
                View div = new View(requireContext());
                div.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.divider));
                list.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
            }
            View row = inf.inflate(R.layout.item_history, list, false);
            ((TextView) row.findViewById(R.id.h_title)).setText(s.title);
            ((TextView) row.findViewById(R.id.h_dur)).setText(Prefs.formatDuration(s.seconds));
            ((TextView) row.findViewById(R.id.h_time)).setText(formatTime(s.ts));
            list.addView(row);
            shown++;
        }
        empty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        list.setVisibility(shown == 0 ? View.GONE : View.VISIBLE);
    }

    private String formatTime(long ts) {
        Calendar now = Calendar.getInstance(), t = Calendar.getInstance();
        t.setTimeInMillis(ts);
        String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(ts));
        if (sameDay(now, t)) return "Today, " + time;
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(now, t)) return "Yesterday, " + time;
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(ts));
    }

    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
