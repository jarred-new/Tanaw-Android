package com.jarredapps.tanaw;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Rational;
import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class TVPlayer extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView channelNameText;
    private ImageButton backButton;
    private LinearLayout bottomBar;
    private ImageButton pipButton;

    private String streamUrl;
    private String channelName;
    private String channelId;
    
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
        pipButton = findViewById(R.id.pipButton);

        streamUrl = getIntent().getStringExtra(
            PrefHelper.urlsIntent
        );

        channelName = getIntent().getStringExtra(
            PrefHelper.channelNameIntent
        );
        
        channelId = getIntent().getStringExtra(
            PrefHelper.channelIdIntent
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

        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        if (fullscreen == true) {
                            //exitFullScreen();
                            playerView.setFullscreenButtonState(false);
                        } else {
                            new MaterialAlertDialogBuilder(TVPlayer.this)
                                    .setTitle("Are you sure to stop?")
                                    .setNegativeButton("No", null)
                                    .setPositiveButton(
                                            "Yes",
                                            (dialog, which) -> {
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
        
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && Build.VERSION.SDK_INT >= 26) {
            pipButton.setVisibility(View.VISIBLE);
            pipButton.setOnClickListener(v -> {
                Rational aspectRatio = new Rational(16, 9); // Set your video aspect ratio
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build();
                enterPictureInPictureMode(params);
            });
        }
        else {
            pipButton.setVisibility(View.GONE);
        }
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
                    
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        switch (state) {
                            case Player.STATE_READY:
                                TVInfoToast.showInfo(
                                    TVPlayer.this,
                                    channelId,
                                    channelName,
                                    streamUrl,
                                    Toast.LENGTH_LONG
                                );
                                break;
                        }
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
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Hide the Action Bar/Toolbar
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }

        // Hide Status Bar and Navigation Bars using WindowInsetsController
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        this.fullscreen = true;
        
        TVInfoToast.showInfo(
            TVPlayer.this,
            channelId,
            channelName,
            streamUrl,
            Toast.LENGTH_LONG
        );
    }

    private void exitFullScreen() {
        // Return orientation to portrait
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        // Show Action Bar/Toolbar
        if (bottomBar != null) {
            bottomBar.setVisibility(View.VISIBLE);
        }

        // Restore Status Bar and Navigation Bars
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());

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
    
    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        // Get Orientation
        int orientation = config.orientation;
    }
    
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            // Hide controllers and extra UI elements
            playerView.hideController();
            bottomBar.setVisibility(View.GONE);
        } else {
            // Show controllers back when user returns to full screen
            playerView.showController();
            bottomBar.setVisibility(View.VISIBLE);
        }
    }
}