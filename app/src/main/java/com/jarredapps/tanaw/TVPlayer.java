package com.jarredapps.tanaw;

import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

    private String streamUrl;
    private String channelName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        
        setTheme(R.style.Theme_Tanaw_Player);

        DynamicColors.applyIfAvailable(this);
        
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tvplayer);

        playerView = findViewById(R.id.playerView);

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
        
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(TVPlayer.this)
                .setTitle("Are you sure to stop?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    finish();
                })
                .show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
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