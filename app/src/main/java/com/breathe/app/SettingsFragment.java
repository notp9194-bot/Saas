package com.breathe.app;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class SettingsFragment extends Fragment {
    private TextView pName, reminderValue;
    private SwitchCompat notifSwitch;
    private boolean suppress = false;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) enableReminder();
                else {
                    setSwitch(notifSwitch, false);
                    Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_settings, c, false);
        pName = v.findViewById(R.id.p_name);
        pName.setText(Prefs.getName(requireContext()));
        ((TextView) v.findViewById(R.id.p_email)).setText(Prefs.getEmail(requireContext()));
        v.findViewById(R.id.profile).setOnClickListener(x -> editName());

        // keep the toggle honest if the permission was revoked in system settings
        boolean notifOn = Prefs.isNotif(requireContext()) && hasNotifPermission();
        if (!notifOn && Prefs.isNotif(requireContext())) { Prefs.setNotif(requireContext(), false); Reminders.cancel(requireContext()); }

        LinearLayout rows = v.findViewById(R.id.rows);
        View nRow = addSwitch(rows, R.drawable.ic_bell, "Daily Reminder", notifOn, (btn, on) -> {
            if (suppress) return;
            if (on) {
                if (hasNotifPermission()) enableReminder();
                else notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Prefs.setNotif(requireContext(), false);
                Reminders.cancel(requireContext());
            }
        });
        notifSwitch = nRow.findViewById(R.id.r_switch);

        View tRow = addRow(rows, R.drawable.ic_clock, "Reminder Time", fmtTime(Prefs.reminderHour(requireContext()), Prefs.reminderMinute(requireContext())), this::pickTime);
        reminderValue = tRow.findViewById(R.id.r_value);

        addSwitch(rows, R.drawable.ic_volume, "Sound", Prefs.isSound(requireContext()),
                (btn, on) -> Prefs.setSound(requireContext(), on));
        addSwitch(rows, R.drawable.ic_pause, "Vibration", Prefs.isVibration(requireContext()),
                (btn, on) -> Prefs.setVibration(requireContext(), on));
        addRow(rows, R.drawable.ic_theme, "Theme", new String[]{"System", "Light", "Dark"}[Prefs.getTheme(requireContext())], this::pickTheme);
        addRow(rows, R.drawable.ic_info, "About App", null,
                () -> msg("About App", "Breathing Exercise v1.0\nBreathe Better, Live Healthier."));
        addRow(rows, R.drawable.ic_lock, "Privacy Policy", null,
                () -> msg("Privacy Policy", "All your data is stored only on this device. Passwords are saved as salted hashes, never in plain text."));
        addRow(rows, R.drawable.ic_star, "Rate App", null, this::rate);
        addRow(rows, R.drawable.ic_share, "Share App", null, this::share);

        v.findViewById(R.id.btn_logout).setOnClickListener(x ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Log Out")
                        .setMessage("You'll be logged out. Your history stays saved on this device.")
                        .setPositiveButton("Log Out", (d, w) -> {
                            Auth.logout(requireContext());
                            Reminders.cancel(requireContext());
                            Prefs.setNotif(requireContext(), false);
                            Intent i = new Intent(requireContext(), AuthActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                        })
                        .setNegativeButton("Cancel", null).show());
        return v;
    }

    // ---------- reminder ----------
    private boolean hasNotifPermission() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void enableReminder() {
        Prefs.setNotif(requireContext(), true);
        Reminders.schedule(requireContext(), false);
        Toast.makeText(requireContext(), "Daily reminder set for "
                + fmtTime(Prefs.reminderHour(requireContext()), Prefs.reminderMinute(requireContext())), Toast.LENGTH_SHORT).show();
    }

    private void pickTime() {
        new TimePickerDialog(requireContext(), (view, h, m) -> {
            Prefs.setReminder(requireContext(), h, m);
            reminderValue.setText(fmtTime(h, m));
            if (Prefs.isNotif(requireContext())) Reminders.schedule(requireContext(), false);
        }, Prefs.reminderHour(requireContext()), Prefs.reminderMinute(requireContext()),
                DateFormat.is24HourFormat(requireContext())).show();
    }

    private String fmtTime(int h, int m) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m);
        return DateFormat.getTimeFormat(requireContext()).format(c.getTime());
    }

    private void setSwitch(SwitchCompat s, boolean on) {
        suppress = true; s.setChecked(on); suppress = false;
    }

    // ---------- rows ----------
    private View baseRow(LinearLayout parent, int icon, String title) {
        View r = LayoutInflater.from(requireContext()).inflate(R.layout.item_setting_row, parent, false);
        ((ImageView) r.findViewById(R.id.r_icon)).setImageResource(icon);
        ((TextView) r.findViewById(R.id.r_title)).setText(title);
        parent.addView(r);
        return r;
    }

    private View addRow(LinearLayout parent, int icon, String title, String value, Runnable action) {
        View r = baseRow(parent, icon, title);
        if (value != null) {
            TextView t = r.findViewById(R.id.r_value);
            t.setText(value); t.setVisibility(View.VISIBLE);
        }
        r.setOnClickListener(x -> action.run());
        return r;
    }

    private View addSwitch(LinearLayout parent, int icon, String title, boolean on, CompoundButton.OnCheckedChangeListener l) {
        View r = baseRow(parent, icon, title);
        r.findViewById(R.id.r_arrow).setVisibility(View.GONE);
        SwitchCompat sw = r.findViewById(R.id.r_switch);
        sw.setVisibility(View.VISIBLE);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(l);
        r.setOnClickListener(x -> sw.toggle());
        return r;
    }

    // ---------- dialogs / actions ----------
    private void pickTheme() {
        final String[] names = {"System default", "Light", "Dark"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Theme")
                .setSingleChoiceItems(names, Prefs.getTheme(requireContext()), (d, which) -> {
                    Prefs.setTheme(requireContext(), which);
                    d.dismiss();
                    AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(requireContext()));
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void editName() {
        if (Auth.isGuest(requireContext())) {
            Toast.makeText(requireContext(), "Log out and create an account to edit your profile", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText et = new EditText(requireContext());
        et.setText(Prefs.getName(requireContext()));
        et.setSelectAllOnFocus(true);
        new AlertDialog.Builder(requireContext())
                .setTitle("Your name").setView(et)
                .setPositiveButton("Save", (d, w) -> {
                    String n = et.getText().toString().trim();
                    if (!n.isEmpty()) { Auth.setName(requireContext(), n); pName.setText(n); }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void msg(String t, String m) {
        new AlertDialog.Builder(requireContext()).setTitle(t).setMessage(m)
                .setPositiveButton("OK", null).show();
    }

    private void rate() {
        String pkg = requireContext().getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }

    private void share() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "Try the Breathing Exercise app: https://play.google.com/store/apps/details?id="
                + requireContext().getPackageName());
        startActivity(Intent.createChooser(i, "Share App"));
    }
}
