package com.breathe.app;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {
    private View[] items;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        items = new View[]{findViewById(R.id.nav_home), findViewById(R.id.nav_history), findViewById(R.id.nav_settings)};
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            items[i].setOnClickListener(v -> select(idx));
        }
        if (Prefs.isNotif(this)) Reminders.schedule(this, false);
        select(0);
    }

    private void select(int i) {
        for (int k = 0; k < items.length; k++) items[k].setSelected(k == i);
        Fragment f = i == 0 ? new HomeFragment() : i == 1 ? new HistoryFragment() : new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out).replace(R.id.container, f).commit();
    }
}
