package com.breathe.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {
    private boolean signup = false;
    private TextView title, subtitle, error, primary, toggle;
    private EditText name, email, pass, confirm;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_auth);
        title = findViewById(R.id.tv_title); subtitle = findViewById(R.id.tv_subtitle);
        error = findViewById(R.id.tv_error); primary = findViewById(R.id.btn_primary);
        toggle = findViewById(R.id.tv_switch);
        name = findViewById(R.id.et_name); email = findViewById(R.id.et_email);
        pass = findViewById(R.id.et_pass); confirm = findViewById(R.id.et_confirm);

        primary.setOnClickListener(v -> submit());
        toggle.setOnClickListener(v -> { signup = !signup; render(); });
        findViewById(R.id.btn_guest).setOnClickListener(v -> { Auth.guest(this); goMain(); });
        render();
    }

    private void render() {
        error.setVisibility(View.GONE);
        name.setVisibility(signup ? View.VISIBLE : View.GONE);
        confirm.setVisibility(signup ? View.VISIBLE : View.GONE);
        title.setText(signup ? "Create Account" : "Welcome Back");
        subtitle.setText(signup ? "Start your breathing journey today" : "Log in to continue your breathing journey");
        primary.setText(signup ? "Sign Up" : "Log In");
        toggle.setText(signup ? "Already have an account? Log in" : "New here? Create account");
    }

    private void submit() {
        String e = email.getText().toString(), p = pass.getText().toString(), err;
        if (signup) {
            if (!p.equals(confirm.getText().toString())) { showError("Passwords do not match."); return; }
            err = Auth.register(this, name.getText().toString(), e, p);
        } else {
            err = Auth.login(this, e, p);
        }
        if (err != null) showError(err); else goMain();
    }

    private void showError(String m) { error.setText(m); error.setVisibility(View.VISIBLE); }

    private void goMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
