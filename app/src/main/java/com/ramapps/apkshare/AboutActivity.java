package com.ramapps.apkshare;

import static com.ramapps.apkshare.GlobalVariables.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = "AboutActivity";
    private AppBarLayout appBarLayout;
    private MaterialButton btnGithub;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        init();
        addListeners();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.about), (v, insets) -> {
            Insets displayCutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            findViewById(R.id.aboutNestedScrollView).setPadding(
                    displayCutouts.left,
                    findViewById(R.id.aboutNestedScrollView).getPaddingTop(),
                    displayCutouts.right,
                    findViewById(R.id.aboutNestedScrollView).getPaddingBottom());
            toolbar.setPadding(
                    displayCutouts.left,
                    toolbar.getPaddingTop(),
                    displayCutouts.right,
                    toolbar.getPaddingBottom());
            return insets;
        });
    }

    private void addListeners() {
        btnGithub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RAM-tech400/APK-Share")));
            }
        });
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });
    }

    private void init() {
        appBarLayout = findViewById(R.id.aboutAppBarLayout);
        toolbar = findViewById(R.id.aboutToolbar);
        btnGithub = findViewById(R.id.aboutButtonGithub);
    }
}