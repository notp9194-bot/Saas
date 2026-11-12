package com.breathe.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CompleteActivity extends AppCompatActivity {
    @Override public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down_out);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_complete);
        final int ex = getIntent().getIntExtra("ex", 0);
        int sec = getIntent().getIntExtra("sec", 60);
        ((TextView) findViewById(R.id.tv_duration)).setText(Prefs.formatDuration(sec));
        int st = Math.max(1, Prefs.streak(this));
        ((TextView) findViewById(R.id.tv_streak)).setText(st + (st == 1 ? " day" : " days"));

        findViewById(R.id.btn_done).setOnClickListener(v -> finish());
        findViewById(R.id.btn_again).setOnClickListener(v -> {
            Intent i = new Intent(this, BreathingActivity.class);
            i.putExtra("ex", ex);
            int[] pat = getIntent().getIntArrayExtra("pattern");
            if (pat != null) i.putExtra("pattern", pat);
            i.putExtra("cycles", getIntent().getIntExtra("cycles", Prefs.getCycles(this, ex)));
            startActivity(i);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
}
