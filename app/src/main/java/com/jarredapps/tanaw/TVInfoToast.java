package com.jarredapps.tanaw;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

class TVInfoToast {

    public static void showInfo(
        Context context,
        int id,
        String name,
        String url,
        int duration
    ) {
        // Instantiate the layout inflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        // Inflate the XML layout file
        View layout = inflater.inflate(R.layout.tvinfo, null);

        // Bind the views from the inflated XML layout
        final TextView channelnumber = layout.findViewById(R.id.channelnumber);
        final TextView channelname = layout.findViewById(R.id.channelname);
        final TextView channelurl = layout.findViewById(R.id.channelurl);
        
        // Modify content dynamically
        channelnumber.setText(String.valueOf(id));
        channelname.setText(name);
        channelurl.setText(url);
        
        // Initialize and configure the native Toast object
        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(duration); // e.g., Toast.LENGTH_SHORT or Toast.LENGTH_LONG
        toast.setView(layout); // Pass the custom view to the toast
        
        // Display it
        toast.show();
    }

}
