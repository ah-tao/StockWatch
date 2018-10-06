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

import javax.net.ssl.HttpsURLConnection;

public class StockDownloader extends AsyncTask<String, Void, Stock> {

    private static final String TAG = "StockDownloader";

    private MainActivity mainActivity;

    private static final String DATA_URL_HEAD = "https://api.iextrading.com/1.0/stock/";
    private static final String DATA_URL_TAIL = "/quote?displayPercent=true";

    public StockDownloader(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected Stock doInBackground(String... strings) {
        String stockURL = DATA_URL_HEAD + strings[0] + DATA_URL_TAIL;
        Log.d(TAG, "doInBackground: " + stockURL);

        StringBuilder builder = new StringBuilder();
        try {
            URL url = new URL(stockURL);

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            Log.d(TAG, "doInBackground: Response Code: " + connection.getResponseCode() + ", "
                    + connection.getResponseMessage());

            // if response code is 404

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

        return parseJSON(builder.toString());
    }

    @Override
    protected void onPostExecute(Stock stock) {
        mainActivity.updateStockList(stock);
        // super.onPostExecute(stock);
    }

    private Stock parseJSON(String data) {
        Log.d(TAG, "parseJSON: Parse JSON");
        Stock stock = new Stock();
        try {
            JsonReader reader = new JsonReader(new StringReader(data));
            if (reader.hasNext()) {
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    switch (key) {
                        case "symbol":
                            stock.setSymbol(reader.nextString());
                            break;
                        case "companyName":
                            stock.setCompany(reader.nextString());
                            break;
                        case "latestPrice":
                            stock.setPrice(reader.nextDouble());
                            break;
                        case "change":
                            stock.setChange(reader.nextDouble());
                            break;
                        case "changePercent":
                            stock.setChangePercent(reader.nextDouble());
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                reader.endObject();
            }
            reader.close();
            Log.d(TAG, "parseJSON: Parse Finish");
        } catch (IOException e) {
            Log.e(TAG, "parseJSON: ", e);
        }
        return stock;
    }
}
