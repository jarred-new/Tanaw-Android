package com.jarredapps.tanaw;

import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
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
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class TVPlayer extends AppCompatActivity {
    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private TextView channelNameText;
    private ImageButton backButton;
    private LinearLayout bottomBar;
    private ImageButton pipButton;
    private SwitchCompat dataSaverSwitch;
    private String streamUrl;
    private String channelName;
    private String channelId;
    private boolean fullscreen;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanaw_Player);
        DynamicColors.applyIfAvailable(this);
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvplayer);

        playerView = findViewById(R.id.playerView);
        channelNameText = findViewById(R.id.channelNameText);
        backButton = findViewById(R.id.backButton);
        bottomBar = findViewById(R.id.bottomBar);
        pipButton = findViewById(R.id.pipButton);
        dataSaverSwitch = findViewById(R.id.dataSaverSwitch);

        streamUrl = getIntent().getStringExtra(PrefHelper.urlsIntent);
        channelName = getIntent().getStringExtra(PrefHelper.channelNameIntent);
        channelId = getIntent().getStringExtra(PrefHelper.channelIdIntent);
        if (channelName == null || channelName.trim().isEmpty()) {
            channelName = "Unknown Channel";
        }
        channelNameText.setText(channelName);
        channelNameText.setSelected(true);

        boolean dataSaverEnabled = getPreferences(MODE_PRIVATE)
                .getBoolean(PrefHelper.dataSaver, false);
        dataSaverSwitch.setChecked(dataSaverEnabled);
        dataSaverSwitch.setOnCheckedChangeListener((button, enabled) -> {
            getPreferences(MODE_PRIVATE).edit().putBoolean(PrefHelper.dataSaver, enabled).apply();
            applyDataSaver(enabled);
            Toast.makeText(this, enabled
                    ? "Data saver on: video quality limited to 360p"
                    : "Data saver off: automatic video quality", Toast.LENGTH_SHORT).show();
        });

        if (streamUrl == null || streamUrl.trim().isEmpty()) {
            showErrorDialog("Invalid Stream",
                    "The channel does not contain a valid stream URL.", true);
            return;
        }
        streamUrl = streamUrl.trim();
        fullscreen = false;

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fullscreen) {
                    playerView.setFullscreenButtonState(false);
                } else {
                    confirmStop();
                }
            }
        });
        backButton.setOnClickListener(v -> confirmStop());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            pipButton.setVisibility(View.VISIBLE);
            pipButton.setOnClickListener(v -> enterPictureInPictureMode(
                    new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build()));
        } else {
            pipButton.setVisibility(View.GONE);
        }
    }

    private void confirmStop() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Are you sure to stop?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (streamUrl != null && !streamUrl.isEmpty()) initializePlayer();
    }

    private void initializePlayer() {
        if (player != null) return;

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Tanaw/1.0")
                .setAllowCrossProtocolRedirects(true);
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpFactory);
        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

        trackSelector = new DefaultTrackSelector(this);
        applyDataSaver(dataSaverSwitch.isChecked());
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                showPlaybackError(error);
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    TVInfoToast.showInfo(TVPlayer.this, channelId, channelName,
                            streamUrl, Toast.LENGTH_LONG);
                }
            }
        });
        playerView.setFullscreenButtonClickListener(isFullScreen -> {
            if (isFullScreen) enterFullScreen(); else exitFullScreen();
        });

        MediaItem.Builder mediaBuilder = new MediaItem.Builder().setUri(Uri.parse(streamUrl));
        if (streamUrl.toLowerCase(Locale.US).contains(".m3u8")) {
            mediaBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
        }
        player.setMediaItem(mediaBuilder.build());
        player.prepare();
        player.play();
    }

    private void applyDataSaver(boolean enabled) {
        if (trackSelector == null) return;
        DefaultTrackSelector.Parameters.Builder parameters = trackSelector.buildUponParameters();
        if (enabled) {
            // Prefer a lower-resolution rendition when the stream provides adaptive tracks.
            parameters.setMaxVideoSize(640, 360).setMaxVideoBitrate(1_000_000);
        } else {
            parameters.setMaxVideoSize(Integer.MAX_VALUE, Integer.MAX_VALUE)
                    .setMaxVideoBitrate(Integer.MAX_VALUE);
        }
        trackSelector.setParameters(parameters);
    }

    private void showPlaybackError(PlaybackException error) {
        StringBuilder message = new StringBuilder("Unable to play this channel.\n\nError: ")
                .append(error.getErrorCodeName());
        if (error.getCause() != null) {
            message.append("\n\nCause: ").append(error.getCause().getClass().getSimpleName());
            if (error.getCause().getMessage() != null && !error.getCause().getMessage().isEmpty()) {
                message.append("\n").append(error.getCause().getMessage());
            }
        }
        showErrorDialog("Playback Error", message.toString(), false);
    }

    private void showErrorDialog(String title, String message, boolean finishActivity) {
        if (isFinishing() || isDestroyed()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (finishActivity) finish();
                })
                .setOnCancelListener(dialog -> {
                    if (finishActivity) finish();
                })
                .show();
    }

    private void enterFullScreen() {
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        fullscreen = true;
        TVInfoToast.showInfo(TVPlayer.this, channelId, channelName, streamUrl, Toast.LENGTH_LONG);
    }

    private void exitFullScreen() {
        if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .show(WindowInsetsCompat.Type.systemBars());
        fullscreen = false;
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
            trackSelector = null;
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
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
            Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            playerView.hideController();
            bottomBar.setVisibility(View.GONE);
        } else {
            playerView.showController();
            bottomBar.setVisibility(View.VISIBLE);
        }
    }
}
