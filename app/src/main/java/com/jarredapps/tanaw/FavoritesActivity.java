package com.jarredapps.tanaw;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class FavoritesActivity extends AppCompatActivity {
    private TextView txtStatus;
    private GridView channelGrid;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;
    private final ArrayList<Channel> channels = new ArrayList<>();
    private ChannelAdapter channelAdapter;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Tanaw);
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.favoritesLayout), (v, insets) -> {
            return insets;
        });

        txtStatus = findViewById(R.id.txtStatus_Favorites);
        searchView = findViewById(R.id.searchView_Favorites);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout_Favorites);
        channelGrid = findViewById(R.id.channelGrid_Favorites);
        preferences = getSharedPreferences(PrefHelper.prefName, MODE_PRIVATE);

        findViewById(R.id.backButton_Favorites).setOnClickListener(v -> finish());

        channelAdapter = new ChannelAdapter();
        channelGrid.setAdapter(channelAdapter);
        channelGrid.setOnItemClickListener((parent, view, position, id) -> {
            Channel channel = channelAdapter.getItem(position);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(channel.name)
                    .setMessage("Name: " + channel.name + "\nUrl: " + channel.url)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Play", (dialog, which) -> playChannel(channel, position))
                    .show();
        });
        channelGrid.setOnItemLongClickListener((parent, view, position, id) -> {
            removeFavorite(channelAdapter.getItem(position));
            return true;
        });

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
        swipeRefreshLayout.setOnRefreshListener(this::loadFavorites);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        String json = preferences.getString(PrefHelper.favorites, "");
        Type type = new TypeToken<ArrayList<Channel>>() {}.getType();
        ArrayList<Channel> savedChannels = json.isEmpty()
                ? new ArrayList<>()
                : new Gson().fromJson(json, type);

        channels.clear();
        if (savedChannels != null) {
            channels.addAll(savedChannels);
        }
        channelAdapter.setChannels(channels);
        txtStatus.setText(channels.size() + (channels.size() == 1
                ? " favorite channel"
                : " favorite channels"));
        swipeRefreshLayout.setRefreshing(false);
    }

    private void playChannel(Channel channel, int position) {
        Intent intent = new Intent(this, TVPlayer.class);
        intent.putExtra(PrefHelper.urlsIntent, channel.url);
        intent.putExtra(PrefHelper.channelNameIntent, channel.name);
        intent.putExtra(PrefHelper.channelIdIntent, String.valueOf(position + 1));
        startActivity(intent);
    }

    private void removeFavorite(Channel channel) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove favorite?")
                .setMessage("Remove \"" + channel.name + "\" from Favorites?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> {
                    channels.remove(channel);
                    preferences.edit()
                            .putString(PrefHelper.favorites, new Gson().toJson(channels))
                            .apply();
                    channelAdapter.setChannels(channels);
                    txtStatus.setText(channels.size() + (channels.size() == 1
                            ? " favorite channel"
                            : " favorite channels"));
                })
                .show();
    }

    private static class Channel {
        String name;
        String logo;
        String url;
    }

    private class ChannelAdapter extends ArrayAdapter<Channel> {
        private final ArrayList<Channel> allChannels = new ArrayList<>();
        private final ArrayList<Channel> filteredChannels = new ArrayList<>();

        ChannelAdapter() {
            super(FavoritesActivity.this, android.R.layout.simple_list_item_1);
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
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            TextView textView = convertView instanceof TextView
                    ? (TextView) convertView
                    : new TextView(FavoritesActivity.this);
            if (convertView == null) {
                GridView.LayoutParams params = new GridView.LayoutParams(
                        GridView.LayoutParams.MATCH_PARENT, 120);
                textView.setLayoutParams(params);
                textView.setGravity(Gravity.CENTER);
                textView.setPadding(12, 12, 12, 12);
                textView.setTextColor(android.graphics.Color.WHITE);
                textView.setTextSize(15);
                textView.setBackgroundColor(android.graphics.Color.rgb(35, 35, 35));
            }
            textView.setText(getItem(position).name);
            return textView;
        }

        @Override
        public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    ArrayList<Channel> results = new ArrayList<>();
                    String query = constraint == null ? "" : constraint.toString().trim();
                    for (Channel channel : allChannels) {
                        if (query.isEmpty() || channel.name.toLowerCase().contains(query.toLowerCase())) {
                            results.add(channel);
                        }
                    }
                    FilterResults filterResults = new FilterResults();
                    filterResults.values = results;
                    filterResults.count = results.size();
                    return filterResults;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredChannels.clear();
                    filteredChannels.addAll((ArrayList<Channel>) results.values);
                    notifyDataSetChanged();
                }
            };
        }

        void setChannels(ArrayList<Channel> newChannels) {
            allChannels.clear();
            allChannels.addAll(newChannels);
            filteredChannels.clear();
            filteredChannels.addAll(newChannels);
            notifyDataSetChanged();
        }
    }
}