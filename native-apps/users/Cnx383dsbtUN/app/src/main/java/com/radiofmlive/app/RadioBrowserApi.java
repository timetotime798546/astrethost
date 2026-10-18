package com.radiofmlive.app;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class RadioBrowserApi {

    public interface DiscoveryCallback {
        void onSuccess(List<RadioStation> stations);
        void onError(String message);
    }

    private static final String[] MIRRORS = new String[] {
            "https://all.api.radio-browser.info",
            "https://de1.api.radio-browser.info",
            "https://nl1.api.radio-browser.info"
    };

    public static void discoverStations(final String name, final String country, final String language, final String genre, final DiscoveryCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<RadioStation> result = null;
                String lastError = "Unable to connect to radio directory service.";

                for (int m = 0; m < MIRRORS.length; m++) {
                    String base = MIRRORS[m];
                    try {
                        StringBuilder urlBuilder = new StringBuilder(base);
                        urlBuilder.append("/json/stations/search?limit=35&hidebroken=true&order=clickcount&reverse=true");

                        if (name != null && !name.trim().isEmpty()) {
                            urlBuilder.append("&name=").append(URLEncoder.encode(name.trim(), "UTF-8"));
                        }
                        if (country != null && !country.trim().isEmpty()) {
                            urlBuilder.append("&country=").append(URLEncoder.encode(country.trim(), "UTF-8"));
                        }
                        if (language != null && !language.trim().isEmpty()) {
                            urlBuilder.append("&language=").append(URLEncoder.encode(language.trim(), "UTF-8"));
                        }
                        if (genre != null && !genre.trim().isEmpty() && !genre.equalsIgnoreCase("All Genres")) {
                            urlBuilder.append("&tag=").append(URLEncoder.encode(genre.trim(), "UTF-8"));
                        }

                        URL url = new URL(urlBuilder.toString());
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(8000);
                        conn.setReadTimeout(10000);
                        conn.setRequestProperty("User-Agent", "RadioFMLive/1.1");
                        conn.setRequestProperty("Accept", "application/json");

                        int code = conn.getResponseCode();
                        if (code >= 200 && code < 300) {
                            InputStream is = conn.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                            StringBuilder jsonBuilder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                jsonBuilder.append(line);
                            }
                            reader.close();
                            is.close();
                            conn.disconnect();

                            result = parseRadioStations(jsonBuilder.toString());
                            if (result != null) {
                                break;
                            }
                        } else {
                            conn.disconnect();
                        }
                    } catch (Exception e) {
                        lastError = e.getMessage() != null ? e.getMessage() : "Network error during discovery";
                    }
                }

                final List<RadioStation> finalResult = result;
                final String finalErr = lastError;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (finalResult != null) {
                            callback.onSuccess(finalResult);
                        } else {
                            callback.onError(finalErr);
                        }
                    }
                });
            }
        }).start();
    }

    private static List<RadioStation> parseRadioStations(String jsonString) {
        try {
            JSONArray arr = new JSONArray(jsonString);
            List<RadioStation> list = new ArrayList<RadioStation>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String uuid = obj.optString("stationuuid", "");
                String name = obj.optString("name", "").trim();
                String streamUrl = obj.optString("url_resolved", "").trim();
                if (streamUrl.isEmpty()) {
                    streamUrl = obj.optString("url", "").trim();
                }

                // Verify valid HTTP/HTTPS audio stream URL
                if (streamUrl.isEmpty() || (!streamUrl.startsWith("http://") && !streamUrl.startsWith("https://"))) {
                    continue;
                }

                String tags = obj.optString("tags", "Radio");
                if (tags.length() > 30) {
                    tags = tags.substring(0, 30);
                }
                if (tags.trim().isEmpty()) {
                    tags = "Music & Talk";
                }

                String language = obj.optString("language", "Global");
                if (language.trim().isEmpty()) {
                    language = "Global";
                }

                String country = obj.optString("country", "Global");
                if (country.trim().isEmpty()) {
                    country = "Worldwide";
                }

                // Generate synthetic FM display frequency (87.5 - 107.9 FM) from UUID hash for authentic UI badge
                int hash = Math.abs(uuid.hashCode() % 204);
                double freqVal = 87.5 + (hash * 0.1);
                String freq = String.format(java.util.Locale.US, "%.1f FM", freqVal);

                if (uuid.isEmpty()) {
                    uuid = "discovered_" + Math.abs(streamUrl.hashCode());
                }

                RadioStation st = new RadioStation(uuid, name, streamUrl, tags, language, freq, country);
                list.add(st);
            }

            return list;
        } catch (Exception e) {
            return null;
        }
    }
}