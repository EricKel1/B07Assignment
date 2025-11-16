package com.example.b07project;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class InhalerTechniqueActivity extends AppCompatActivity {

    private WebView webViewVideo;
    private Button btnStartTimer, btnClose;
    private TextView tvTimerDisplay;
    private CountDownTimer practiceTimer;
    private static final String VIDEO_ID = "2i9_DelNqs4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_technique);

        initializeViews();
        setupYouTubeVideo();
        setupListeners();
    }

    private void initializeViews() {
        webViewVideo = findViewById(R.id.webViewVideo);
        btnStartTimer = findViewById(R.id.btnStartTimer);
        btnClose = findViewById(R.id.btnClose);
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupYouTubeVideo() {
        WebSettings webSettings = webViewVideo.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webViewVideo.setWebChromeClient(new WebChromeClient());

        // YouTube iframe embed HTML
        String videoHtml = "<html><body style='margin:0;padding:0;'>"
                + "<iframe width='100%' height='100%' "
                + "src='https://www.youtube.com/embed/" + VIDEO_ID + "?autoplay=0&rel=0' "
                + "frameborder='0' "
                + "allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' "
                + "allowfullscreen></iframe>"
                + "</body></html>";

        webViewVideo.loadData(videoHtml, "text/html", "utf-8");
    }

    private void setupListeners() {
        btnStartTimer.setOnClickListener(v -> startPracticeTimer());
        btnClose.setOnClickListener(v -> finish());
    }

    private void startPracticeTimer() {
        if (practiceTimer != null) {
            practiceTimer.cancel();
        }

        btnStartTimer.setEnabled(false);
        btnStartTimer.setText("Hold your breath...");

        practiceTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                tvTimerDisplay.setText(String.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                tvTimerDisplay.setText("✓");
                Toast.makeText(InhalerTechniqueActivity.this, 
                    "Great job! You can breathe out now.", 
                    Toast.LENGTH_LONG).show();
                
                // Reset after 2 seconds
                tvTimerDisplay.postDelayed(() -> {
                    tvTimerDisplay.setText("10");
                    btnStartTimer.setEnabled(true);
                    btnStartTimer.setText("Start 10-Second Timer");
                }, 2000);
            }
        };

        practiceTimer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (practiceTimer != null) {
            practiceTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (practiceTimer != null) {
            practiceTimer.cancel();
        }
        if (webViewVideo != null) {
            webViewVideo.destroy();
        }
    }
}
