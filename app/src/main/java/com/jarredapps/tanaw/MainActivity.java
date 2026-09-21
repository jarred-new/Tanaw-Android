package com.jarredapps.tanaw;

//import android.R;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.*;
import android.widget.*;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {
    private FloatingActionButton _fab;
    private TextView txtStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        _fab = findViewById(R.id._fab);
        txtStatus = findViewById(R.id.txtStatus);
        
        _fab.setOnClickListener(v -> showPlaylistDialog());
    }
    
    private void showPlaylistDialog() {
        final LinearLayout layout = new LinearLayout(this);
    
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 0);
    
        final TextInputLayout inputLayout = new TextInputLayout(
                this,
                null,
                com.google.android.material.R.attr.textInputOutlinedStyle
        );
    
        inputLayout.setHint("M3U Playlist URL");
    
        final TextInputEditText input = new TextInputEditText(this);
    
        input.setSingleLine(true);
        input.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_URI
        );
    
        inputLayout.addView(input);
        layout.addView(inputLayout);
    
        new MaterialAlertDialogBuilder(this)
                        .setTitle("Add Playlist")
                        .setView(layout)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Add", (dialog, which) -> {
                            String url = input.getText()
                                .toString()
                                .trim();
    
                            if (url.isEmpty()) {
        
                                inputLayout.setError(
                                        "Enter a playlist URL"
                                );
        
                                return;
                            }
        
                            inputLayout.setError(null);
                            
                            //loadPlaylist(url);
                        }).show();
    }
    
    /*private void loadPlaylist(String url) {
        
    }*/ 
}