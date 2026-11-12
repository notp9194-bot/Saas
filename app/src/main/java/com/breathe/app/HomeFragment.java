package com.breathe.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    private View root, resumeCard;
    private TextView resumeTitle, resumeSub, streak, weekTotal;
    private WeekChartView chart;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        root = inf.inflate(R.layout.fragment_home, c, false);
        String full = Prefs.getName(requireContext()).trim();
        String first = full.isEmpty() ? "there" : full.split(" ")[0];
        ((TextView) root.findViewById(R.id.tv_name)).setText(first + " 👋");

        resumeCard = root.findViewById(R.id.resume_card);
        resumeTitle = root.findViewById(R.id.resume_title);
        resumeSub = root.findViewById(R.id.resume_sub);
        streak = root.findViewById(R.id.tv_streak);
        weekTotal = root.findViewById(R.id.tv_week_total);
        chart = root.findViewById(R.id.week_chart);

        root.findViewById(R.id.quick_start).setOnClickListener(x -> quickStart(0));

        int[] ids = {R.id.card0, R.id.card1, R.id.card2, R.id.card3};
        for (int i = 0; i < ids.length; i++) {
            final int idx = i;
            Exercise e = Exercise.ALL[i];
            View card = root.findViewById(ids[i]);
            card.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), e.cardColor)));
            ImageView icon = card.findViewById(R.id.ex_icon);
            icon.setImageResource(e.icon);
            if (e.iconTint != 0)
                icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), e.iconTint)));
            ((TextView) card.findViewById(R.id.ex_title)).setText(e.title);
            ((TextView) card.findViewById(R.id.ex_label)).setText(e.label);
            card.setOnClickListener(x -> SetupSheet.show(requireActivity(), idx));
        }
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        // resume card
        final Prefs.ResumeState rs = Prefs.getResume(requireContext());
        if (rs != null) {
            long leftMs = Math.max(0, rs.totalDur() - rs.totalMs);
            resumeTitle.setText("Resume " + Exercise.ALL[rs.ex].session);
            resumeSub.setText(Prefs.formatDuration((int) Math.round(leftMs / 1000.0)) + " left");
            resumeCard.setVisibility(View.VISIBLE);
            resumeCard.setOnClickListener(x -> {
                Intent i = new Intent(requireContext(), BreathingActivity.class);
                i.putExtra("resume", true);
                startActivity(i);
                requireActivity().overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out);
            });
            root.findViewById(R.id.resume_discard).setOnClickListener(x -> {
                Prefs.clearResume(requireContext());
                resumeCard.setVisibility(View.GONE);
            });
        } else {
            resumeCard.setVisibility(View.GONE);
        }

        // streak + weekly chart
        int st = Prefs.streak(requireContext());
        streak.setText(st + (st == 1 ? " day" : " days"));
        int[] week = Prefs.weekSeconds(requireContext());
        int total = 0; for (int v : week) total += v;
        weekTotal.setText(total == 0 ? "0 min" : Prefs.formatDuration(total));
        chart.setData(week);
    }

    /** One tap: start with the last saved pattern for this exercise. */
    private void quickStart(int idx) {
        Intent i = new Intent(requireContext(), BreathingActivity.class);
        i.putExtra("ex", idx)
         .putExtra("pattern", Prefs.getPattern(requireContext(), idx))
         .putExtra("cycles", Prefs.getCycles(requireContext(), idx));
        startActivity(i);
        requireActivity().overridePendingTransition(R.anim.slide_up_in, R.anim.fade_out);
    }
}
