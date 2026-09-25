package com.jarredapps.tanaw;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import android.view.Menu;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView txtStatus;
    private GridView channelGrid;
    private FloatingActionButton _fab;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    
    private final ArrayList<Channel> channels =
            new ArrayList<>();

    private ChannelAdapter channelAdapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    
    private SharedPreferences preferences;
    
    private Intent intentPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanaw);
        DynamicColors.applyIfAvailable(this);
        EdgeToEdge.enable(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            return insets;
        });
        
        
        txtStatus = findViewById(R.id.txtStatus);
        searchView = findViewById(R.id.searchView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        channelGrid = findViewById(R.id.channelGrid);
        _fab = findViewById(R.id._fab);
        
        intentPlayer = new Intent();
        
        preferences = getSharedPreferences(
            PrefHelper.prefName,
            MODE_PRIVATE
        );
        
        String savedUrl = preferences.getString(
                PrefHelper.urls,
                ""
        );
        
        if (!savedUrl.isEmpty()) {
            loadPlaylist(savedUrl);
        }

        // GridView adapter
        channelAdapter = new ChannelAdapter();
        channelGrid.setAdapter(channelAdapter);

        // Add playlist
        _fab.setOnClickListener(
                v -> showPlaylistDialog()
        );

        // Channel click and long click
        channelGrid.setOnItemClickListener(
                (parent, view, position, id) -> {

                    Channel channel =
                            channelAdapter.getItem(position);

                    /*Toast.makeText(
                            MainActivity.this,
                            channel.name,
                            Toast.LENGTH_SHORT
                    ).show();*/

                    new MaterialAlertDialogBuilder(this)
                        .setTitle(channel.name)
                        .setMessage(
                            "Channel Number: " + String.valueOf(id) + "\n" +
                            "Name: " + channel.name + "\n" +
                            "Url: " + channel.url + "\n"
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Play",
                                (dialog, which) -> {
                                    // Open Media3 player here.
                                    if (intentPlayer != null) {
                                        intentPlayer.setClass(this, TVPlayer.class);
                                        intentPlayer.putExtra(
                                            PrefHelper.urlsIntent, channel.url
                                        );
                                        intentPlayer.putExtra(
                                            PrefHelper.channelNameIntent, channel.name
                                        );
                                        intentPlayer.putExtra(
                                            PrefHelper.channelIdIntent, String.valueOf(id)
                                        );
                                        startActivity(intentPlayer);
                                    }
                                }
                        )
                        .show();
                }
        );
        
        channelGrid.setOnItemLongClickListener((parent, view, position, id) -> {

                    Channel channel =
                            channelAdapter.getItem(position);
        
                    if (channel != null) {
                        showChannelPopupMenu(view, channel);
                    }
        
                    return true;
                }
        );
        
        // Refresh to reload channels
        swipeRefreshLayout.setOnRefreshListener(() -> {
            String urlRefresh = preferences.getString(
                PrefHelper.urls,
                ""
            );
            
            if (!savedUrl.isEmpty()) {
                loadPlaylist(urlRefresh);
            }
            else {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(
                    MainActivity.this,
                    "No Channels or Playlists were added yet!",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });
        
        // Search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                channelAdapter.getFilter().filter(query);
                return true;
            }
        
            @Override
            public boolean onQueryTextChange(String newText) {
                channelAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }
    
    // --------------------------------------------------
    // Playlist dialog
    // --------------------------------------------------

    private void showPlaylistDialog() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40,
                10,
                40,
                0
        );

        TextInputLayout inputLayout =
                new TextInputLayout(
                        this,
                        null,
                        com.google.android.material.R.attr
                                .textInputOutlinedStyle
                );

        inputLayout.setHint(
                "M3U Playlist URL"
        );

        TextInputEditText input =
                new TextInputEditText(this);

        input.setSingleLine(true);

        input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_URI
        );

        inputLayout.addView(input);

        layout.addView(inputLayout);

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Add Playlist")
                        .setView(layout)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Add",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            androidx.appcompat.app.AlertDialog
                                    .BUTTON_POSITIVE
                    ).setOnClickListener(v -> {

                        String url =
                                input.getText()
                                        .toString()
                                        .trim();

                        if (url.isEmpty()) {

                            inputLayout.setError(
                                    "Enter a playlist URL"
                            );

                            return;
                        }

                        if (!url.startsWith("http://") &&
                                !url.startsWith("https://")) {

                            inputLayout.setError(
                                    "Enter a valid HTTP/HTTPS URL"
                            );

                            return;
                        }

                        inputLayout.setError(null);

                        dialog.dismiss();

                        loadPlaylist(url);
                    });
                }
        );

        dialog.show();
    }

    // --------------------------------------------------
    // Load playlist
    // --------------------------------------------------

    private void loadPlaylist(
            String playlistUrl
    ) {

        txtStatus.setText(
                "Loading playlist..."
        );

        channelGrid.setVisibility(View.INVISIBLE);
        _fab.setEnabled(false);
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        executor.execute(() -> {

            PlaylistResult playlist =
                    downloadPlaylist(playlistUrl);

            runOnUiThread(() -> {

                _fab.setEnabled(true);
                channelGrid.setVisibility(View.VISIBLE);

                if (playlist.error != null) {

                    txtStatus.setText(
                            "Failed to load playlist"
                    );

                    Toast.makeText(
                            MainActivity.this,
                            playlist.error,
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                channels.clear();
                channels.addAll(playlist.channels);
                
                channelAdapter.setChannels(channels);

                txtStatus.setText(
                        channels.size() +
                        " channels loaded"
                );
                
                preferences.edit().putString(PrefHelper.urls, playlistUrl).apply();
            });
        });
    }

    // --------------------------------------------------
    // Download and parse M3U
    // --------------------------------------------------

    private PlaylistResult downloadPlaylist(
            String playlistUrl
    ) {

        ArrayList<Channel> result =
                new ArrayList<>();

        HttpURLConnection connection = null;

        try {

            URL url =
                    new URL(playlistUrl);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    30000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "Tanaw IPTV Player"
            );

            connection.setRequestProperty(
                    "Accept",
                    "*/*"
            );

            connection.setInstanceFollowRedirects(
                    true
            );

            connection.connect();

            int responseCode =
                    connection.getResponseCode();

            if (responseCode < 200 ||
                    responseCode >= 300) {

                return new PlaylistResult(
                        result,
                        "HTTP " + responseCode
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection
                                            .getInputStream()
                            )
                    );

            String line;

            String channelName = null;
            String channelLogo = null;

            while ((line =
                    reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // Channel information
                if (line.startsWith("#EXTINF")) {

                    channelName =
                            parseChannelName(line);

                    channelLogo =
                            parseAttribute(
                                    line,
                                    "tvg-logo"
                            );

                    continue;
                }

                // Ignore other M3U tags
                if (line.startsWith("#")) {
                    continue;
                }

                // This line is the stream URL
                if (channelName != null) {

                    String streamUrl = line;

                    result.add(
                            new Channel(
                                    channelName,
                                    channelLogo,
                                    streamUrl
                            )
                    );

                    channelName = null;
                    channelLogo = null;
                }
            }

            reader.close();

            return new PlaylistResult(
                    result,
                    null
            );

        } catch (Exception e) {

            String message =
                    e.getMessage();

            if (message == null ||
                    message.isEmpty()) {

                message =
                        e.getClass()
                                .getSimpleName();
            }

            return new PlaylistResult(
                    result,
                    message
            );

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // --------------------------------------------------
    // Parse channel name
    // --------------------------------------------------

    private String parseChannelName(
            String line
    ) {

        int comma =
                line.indexOf(',');

        if (comma >= 0 &&
                comma + 1 < line.length()) {

            String name =
                    line.substring(
                            comma + 1
                    ).trim();

            if (!name.isEmpty()) {
                return name;
            }
        }

        return "Unknown Channel";
    }

    // --------------------------------------------------
    // Parse M3U attribute
    // --------------------------------------------------

    private String parseAttribute(
            String line,
            String attribute
    ) {

        String search =
                attribute + "=\"";

        int start =
                line.indexOf(search);

        if (start == -1) {
            return "";
        }

        start += search.length();

        int end =
                line.indexOf(
                        "\"",
                        start
                );

        if (end == -1) {
            return "";
        }

        return line.substring(
                start,
                end
        );
    }
    
    private void showChannelPopupMenu(
        View anchor,
        Channel channel
    ) {
    
        PopupMenu popupMenu =
                new PopupMenu(this, anchor);
    
        Menu menu = popupMenu.getMenu();
    
        menu.add(
                Menu.NONE,
                1,
                Menu.NONE,
                "Play"
        );
    
        menu.add(
                Menu.NONE,
                2,
                Menu.NONE,
                "Record"
        );
    
        menu.add(
                Menu.NONE,
                3,
                Menu.NONE,
                "Add to Favorites"
        );
    
        menu.add(
                Menu.NONE,
                4,
                Menu.NONE,
                "Channel Info"
        );
    
        menu.add(
                Menu.NONE,
                5,
                Menu.NONE,
                "Copy Stream URL"
        );
    
        menu.add(
                Menu.NONE,
                6,
                Menu.NONE,
                "Share"
        );
    
        menu.add(
                Menu.NONE,
                7,
                Menu.NONE,
                "Remove Channel"
        );
    
        popupMenu.setOnMenuItemClickListener(
                item -> {
                    switch (item.getItemId()) {
    
                        case 1:
                            playChannel(channel);
                            return true;
    
                        case 2:
                            recordChannel(channel);
                            return true;
    
                        case 3:
                            toggleFavorite(channel);
                            return true;
    
                        case 4:
                            showChannelInfo(channel);
                            return true;
    
                        case 5:
                            copyStreamUrl(channel);
                            return true;
    
                        case 6:
                            shareChannel(channel);
                            return true;
    
                        case 7:
                            removeChannel(channel);
                            return true;
    
                        default:
                            return false;
                    }
                }
        );
    
        popupMenu.show();
    }

    // --------------------------------------------------
    // Channel adapter
    // --------------------------------------------------

    private class ChannelAdapter extends ArrayAdapter<Channel> implements android.widget.Filterable {
    
        private final ArrayList<Channel> allChannels;
        private final ArrayList<Channel> filteredChannels;
    
        private final android.widget.Filter filter =
                new android.widget.Filter() {
    
            @Override
            protected FilterResults performFiltering(
                    CharSequence constraint) {
    
                ArrayList<Channel> filtered =
                        new ArrayList<>();
    
                if (constraint == null ||
                        constraint.length() == 0) {
    
                    filtered.addAll(allChannels);
    
                } else {
    
                    String query =
                            constraint.toString()
                                    .toLowerCase()
                                    .trim();
    
                    for (Channel channel : allChannels) {
    
                        if (channel.name
                                .toLowerCase()
                                .contains(query)) {
    
                            filtered.add(channel);
                        }
                    }
                }
    
                FilterResults results =
                        new FilterResults();
    
                results.values = filtered;
                results.count = filtered.size();
    
                return results;
            }
    
            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(
                    CharSequence constraint,
                    FilterResults results) {
    
                filteredChannels.clear();
    
                if (results.values != null) {
    
                    filteredChannels.addAll(
                            (ArrayList<Channel>)
                                    results.values
                    );
                }
    
                notifyDataSetChanged();
            }
        };
    
        ChannelAdapter() {
    
            super(
                    MainActivity.this,
                    android.R.layout.simple_list_item_1,
                    new ArrayList<>()
            );
    
            allChannels = new ArrayList<>();
    
            filteredChannels = new ArrayList<>();
    
            allChannels.addAll(channels);
            filteredChannels.addAll(channels);
        }
    
        @Override
        public int getCount() {
            return filteredChannels.size();
        }
    
        @Override
        public Channel getItem(int position) {
            return filteredChannels.get(position);
        }
    
        @Override
        public long getItemId(int position) {
            return position;
        }
    
        @Override
        public View getView(
                int position,
                View convertView,
                android.view.ViewGroup parent) {
    
            TextView textView;
    
            if (convertView == null) {
    
                textView =
                        new TextView(MainActivity.this);
    
                GridView.LayoutParams params =
                        new GridView.LayoutParams(
                                GridView.LayoutParams.MATCH_PARENT,
                                120
                        );
    
                textView.setLayoutParams(params);
    
                textView.setGravity(
                        Gravity.CENTER
                );
    
                textView.setPadding(
                        12,
                        12,
                        12,
                        12
                );
    
                textView.setTextColor(
                        android.graphics.Color.WHITE
                );
    
                textView.setTextSize(15);
    
                textView.setBackgroundColor(
                        android.graphics.Color.rgb(
                                35,
                                35,
                                35
                        )
                );
    
            } else {
    
                textView =
                        (TextView) convertView;
            }
    
            Channel channel =
                    getItem(position);
    
            textView.setText(
                    channel.name
            );
    
            return textView;
        }
    
        @Override
        public android.widget.Filter getFilter() {
            return filter;
        }
    
        void setChannels(
                ArrayList<Channel> newChannels) {
    
            allChannels.clear();
    
            allChannels.addAll(
                    newChannels
            );
    
            filteredChannels.clear();
    
            filteredChannels.addAll(
                    newChannels
            );
    
            notifyDataSetChanged();
        }
    }
    // --------------------------------------------------
    // Channel model
    // --------------------------------------------------

    private static class Channel {

        String name;
        String logo;
        String url;

        Channel(
                String name,
                String logo,
                String url
        ) {

            this.name = name;
            this.logo = logo;
            this.url = url;
        }
    }

    // --------------------------------------------------
    // Playlist result
    // --------------------------------------------------

    private static class PlaylistResult {

        ArrayList<Channel> channels;
        String error;

        PlaylistResult(
                ArrayList<Channel> channels,
                String error
        ) {

            this.channels = channels;
            this.error = error;
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        executor.shutdownNow();
    }
}