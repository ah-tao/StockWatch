package com.taotao.stockwatch;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class NameDownloader extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "NameDownloader";
    private MainActivity mainActivity;
    private HashMap<String, String> fullSet= new HashMap<>();

    private static final String DATA_URL = "https://api.iextrading.com/1.0/ref-data/symbols";

    public NameDownloader(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPostExecute(Void v) {
        super.onPostExecute(v);
        mainActivity.fetchFullSet(fullSet);
        Log.d(TAG, "onPostExecute: Full Set of Stocks Download");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        StringBuilder builder = new StringBuilder();
        try {
            URL url = new URL(DATA_URL);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            Log.d(TAG, "doInBackground: Response Code" + connection.getResponseCode()
                    + ", " + connection.getResponseMessage());

            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }

            Log.d(TAG, "doInBackground: " + builder.toString());

        } catch (Exception e) {
            Log.e(TAG, "doInBackground: ", e);
        }

        if (builder.length() > 0) {
            parseJSON(builder.toString());
        }

        return null;
    }

    private void parseJSON(String data) {
        Log.d(TAG, "parseJSON: Parse JSON");
        try {
            JsonReader reader = new JsonReader(new StringReader(data));
            if (reader.hasNext()) {
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    String symbol = "";
                    String name = "";
                    while (reader.hasNext()) {
                        String key = reader.nextName();
                        switch (key) {
                            case "symbol":
                                symbol = reader.nextString();
                                break;
                            case "name":
                                name = reader.nextString();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                    reader.endObject();
                    fullSet.put(symbol, name);
                }
                reader.endArray();
            }
            reader.close();
            Log.d(TAG, "parseJSON: Parse Finish");
        } catch (IOException e) {
            Log.e(TAG, "parseJSON: ", e);
        }
    }
}
