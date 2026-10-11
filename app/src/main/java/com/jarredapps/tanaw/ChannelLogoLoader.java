package com.jarredapps.tanaw;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ChannelLogoLoader {
    private static final String TAG = "ChannelLogoLoader";
    private static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024;
    private static final int LOGO_MAX_DIMENSION_PX = 512;
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> bitmapCache =
            new LruCache<String, Bitmap>(
                    (int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };

    private ChannelLogoLoader() {}

    static void load(String logoUrl, ImageView imageView) {
        String normalizedUrl = logoUrl == null ? "" : logoUrl.trim();
        imageView.setTag(normalizedUrl);
        imageView.setImageResource(R.drawable.ic_channel);
        if (normalizedUrl.isEmpty()) {
            return;
        }

        Bitmap cachedBitmap = bitmapCache.get(normalizedUrl);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        executor.execute(() -> {
            Bitmap bitmap = downloadBitmap(normalizedUrl);
            if (bitmap == null) {
                return;
            }
            bitmapCache.put(normalizedUrl, bitmap);
            mainHandler.post(() -> {
                if (normalizedUrl.equals(imageView.getTag())) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap downloadBitmap(String logoUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(logoUrl);
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                Log.w(TAG, "Unsupported channel logo URL protocol: " + protocol);
                return null;
            }

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            int responseCode = connection.getResponseCode();
            if (responseCode < HttpURLConnection.HTTP_OK
                    || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                Log.w(TAG, "Channel logo request failed with HTTP " + responseCode
                        + ": " + logoUrl);
                return null;
            }

            byte[] imageData;
            try (InputStream input = connection.getInputStream();
                    ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int totalBytes = 0;
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > MAX_IMAGE_BYTES) {
                        Log.w(TAG, "Channel logo exceeds the size limit: " + logoUrl);
                        return null;
                    }
                    output.write(buffer, 0, bytesRead);
                }
                imageData = output.toByteArray();
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(imageData, 0, imageData.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.w(TAG, "Channel logo is not a supported image: " + logoUrl);
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateSampleSize(bounds);
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    imageData, 0, imageData.length, options);
            if (bitmap == null) {
                Log.w(TAG, "Unable to decode channel logo: " + logoUrl);
            }
            return bitmap;
        } catch (IOException e) {
            Log.w(TAG, "Unable to load channel logo: " + logoUrl, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static int calculateSampleSize(BitmapFactory.Options bounds) {
        int sampleSize = 1;
        while (bounds.outWidth / sampleSize > LOGO_MAX_DIMENSION_PX
                || bounds.outHeight / sampleSize > LOGO_MAX_DIMENSION_PX) {
            sampleSize *= 2;
        }
        return sampleSize;
    }
}
