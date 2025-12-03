package com.ramapps.apkshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = "AboutActivity";
    private AppBarLayout appBarLayout;
    private MaterialButton btnGithub, buttonEmailToDeveloper, buttonCheckUpdate;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        initViews();
        addListeners();
    }

    private void addListeners() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.about), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets displayCutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            int leftInset = Math.max(systemBars.left, displayCutouts.left);
            int rightInset = Math.max(systemBars.right, displayCutouts.right);
            findViewById(R.id.aboutNestedScrollView).setPadding(
                    leftInset,
                    findViewById(R.id.aboutNestedScrollView).getPaddingTop(),
                    rightInset,
                    findViewById(R.id.aboutNestedScrollView).getPaddingBottom());
            toolbar.setPadding(
                    leftInset,
                    toolbar.getPaddingTop(),
                    rightInset,
                    toolbar.getPaddingBottom());
            return insets;
        });
        btnGithub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RAM-tech400/APK-Share")));
            }
        });
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });
        buttonEmailToDeveloper.setOnClickListener((v) -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"newram098@gmail.com"});
            emailIntent.setData(Uri.parse("mailto:"));
            startActivity(Intent.createChooser(emailIntent, getString(R.string.compose_email_via)));
        });
    }

    private void initViews() {
        appBarLayout = findViewById(R.id.aboutAppBarLayout);
        toolbar = findViewById(R.id.aboutToolbar);
        btnGithub = findViewById(R.id.aboutButtonGithub);
        buttonEmailToDeveloper = findViewById(R.id.aboutButtonEmailToDeveloper);
        buttonCheckUpdate = findViewById(R.id.aboutButtonCheckUpdates);
    }
}