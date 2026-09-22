package com.jarredapps.tanaw;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

public class SearchDialogFragment extends DialogFragment {

    private SearchDialogListener listener;

    // Interface to pass data back to the Activity/Fragment
    public interface SearchDialogListener {
        void onSearchSubmitted(String query);
    }

    // Setter for the listener
    public void setSearchDialogListener(SearchDialogListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the custom layout we created earlier
        View view = inflater.inflate(R.layout.dialog_search, container, false);

        SearchBar searchBar = view.findViewById(R.id.dialog_search_bar);
        SearchView searchView = view.findViewById(R.id.dialog_search_view);

        // Connect the SearchBar to the SearchView
        searchView.setupWithSearchBar(searchBar);

        // Handle text submission
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            String query = searchView.getText().toString();
            
            if (listener != null) {
                listener.onSearchSubmitted(query);
            }

            // Close the SearchView and dismiss the DialogFragment
            searchView.hide();
            dismiss();
            return false;
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Optional: Force the dialog window to match the width of the screen
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}