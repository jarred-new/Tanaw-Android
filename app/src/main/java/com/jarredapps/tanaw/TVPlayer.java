package com.jarredapps.tanaw;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.color.DynamicColors;

import java.util.Locale;

public class TVPlayer extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView channelNameText;
    private ImageButton backButton;
    private LinearLayout bottomBar;

    private String streamUrl;
    private String channelName;
    private boolean fullscreen;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        
        setTheme(R.style.Theme_Tanaw_Player);

        DynamicColors.applyIfAvailable(this);
        
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tvplayer);

        playerView = findViewById(R.id.playerView);
        channelNameText = findViewById(R.id.channelNameText);
        backButton = findViewById(R.id.backButton);
        bottomBar = findViewById(R.id.bottomBar);

        streamUrl = getIntent().getStringExtra(
                PrefHelper.urlsIntent
        );

        channelName = getIntent().getStringExtra(
                PrefHelper.channelNameIntent
        );

        if (channelName == null ||
                channelName.trim().isEmpty()) {

            channelName = "Unknown Channel";
        }
        
        channelNameText.setText(channelName);

        /*
         * Check stream URL.
         */
        if (streamUrl == null ||
                streamUrl.trim().isEmpty()) {

            showErrorDialog(
                    "Invalid Stream",
                    "The channel does not contain a valid stream URL.",
                    true
            );

            return;
        }

        streamUrl = streamUrl.trim();
        
        this.fullscreen = false;
        
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (fullscreen == true) {
                    exitFullScreen();
                }
                else {
                   new MaterialAlertDialogBuilder(TVPlayer.this)
                        .setTitle("Are you sure to stop?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", (dialog, which) -> {
                            finish();
                        })
                        .show();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        
        backButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(TVPlayer.this)
                .setTitle("Are you sure to stop?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    finish();
                })
                .show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (streamUrl != null &&
                !streamUrl.isEmpty()) {

            initializePlayer();
        }
    }

    private void initializePlayer() {

        if (player != null) {
            return;
        }

        /*
         * HTTP data source.
         *
         * IPTV providers commonly require
         * a User-Agent.
         */
        DefaultHttpDataSource.Factory httpFactory =
                new DefaultHttpDataSource.Factory()
                        .setUserAgent(
                                "Tanaw/1.0"
                        )
                        .setAllowCrossProtocolRedirects(
                                true
                        );

        /*
         * Default data source.
         */
        DefaultDataSource.Factory dataSourceFactory =
                new DefaultDataSource.Factory(
                        this,
                        httpFactory
                );

        /*
         * Media source factory.
         */
        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(
                        dataSourceFactory
                );

        /*
         * Create ExoPlayer.
         */
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(
                        mediaSourceFactory
                )
                .build();

        /*
         * Attach PlayerView.
         */
        playerView.setPlayer(player);

        /*
         * Player listener.
         */
        player.addListener(
                new Player.Listener() {

                    @Override
                    public void onPlayerError(
                            PlaybackException error
                    ) {

                        showPlaybackError(error);
                    }
                }
        );
        
        playerView.setFullscreenButtonClickListener(isFullScreen -> {
            if (isFullScreen) {
                enterFullScreen();
            } else {
                exitFullScreen();
            }
        });

        /*
         * Build MediaItem.
         */
        MediaItem.Builder mediaBuilder =
                new MediaItem.Builder()
                        .setUri(
                                Uri.parse(streamUrl)
                        );

        /*
         * Detect HLS.
         */
        String lowerUrl =
                streamUrl.toLowerCase(
                        Locale.US
                );

        if (lowerUrl.contains(".m3u8")) {

            mediaBuilder.setMimeType(
                    MimeTypes.APPLICATION_M3U8
            );
        }

        MediaItem mediaItem =
                mediaBuilder.build();

        /*
         * Start playback.
         */
        player.setMediaItem(mediaItem);

        player.prepare();

        player.play();
    }

    private void showPlaybackError(
            PlaybackException error
    ) {

        StringBuilder message =
                new StringBuilder();

        message.append(
                "Unable to play this channel."
        );

        /*
         * Media3 error code.
         */
        message.append("\n\n");

        message.append(
                "Error: "
        );

        message.append(
                error.getErrorCodeName()
        );

        /*
         * Underlying exception.
         */
        if (error.getCause() != null) {

            Throwable cause =
                    error.getCause();

            message.append("\n\n");

            message.append(
                    "Cause: "
            );

            message.append(
                    cause.getClass()
                            .getSimpleName()
            );

            if (cause.getMessage() != null &&
                    !cause.getMessage().isEmpty()) {

                message.append("\n");

                message.append(
                        cause.getMessage()
                );
            }
        }

        showErrorDialog(
                "Playback Error",
                message.toString(),
                false
        );
    }

    private void showErrorDialog(
            String title,
            String message,
            boolean finishActivity
    ) {

        /*
         * Make sure the Activity is still alive.
         */
        if (isFinishing() ||
                isDestroyed()) {

            return;
        }

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(
                        this
                );

        builder.setTitle(title);

        builder.setMessage(message);

        builder.setPositiveButton(
                "OK",
                (dialog, which) -> {

                    dialog.dismiss();

                    if (finishActivity) {
                        finish();
                    }
                }
        );

        builder.setOnCancelListener(
                dialog -> {

                    if (finishActivity) {
                        finish();
                    }
                }
        );

        builder.show();
    }
    
   private void enterFullScreen() {
        // Change orientation to landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Hide the Action Bar/Toolbar
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }

        // Hide Status Bar and Navigation Bars using WindowInsetsController
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // Force PlayerView to take up the entire screen layout
        ViewGroup.LayoutParams params = playerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playerView.setLayoutParams(params);
        
        this.fullscreen = true;
    }

    private void exitFullScreen() {
        // Return orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Show Action Bar/Toolbar
        if (bottomBar != null) {
            bottomBar.setVisibility(View.VISIBLE);
        }

        // Restore Status Bar and Navigation Bars
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());

        // Reset PlayerView to its original height (e.g., 250dp or original layout params)
        ViewGroup.LayoutParams params = playerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = (int) (250 * getResources().getDisplayMetrics().density); // Example: 250dp height
        playerView.setLayoutParams(params);
        
        this.fullscreen = false;
    }

    @Override
    protected void onStop() {

        super.onStop();

        releasePlayer();
    }

    private void releasePlayer() {

        if (player != null) {

            playerView.setPlayer(null);

            player.release();

            player = null;
        }
    }

    @Override
    protected void onDestroy() {

        releasePlayer();

        super.onDestroy();
    }
    
}