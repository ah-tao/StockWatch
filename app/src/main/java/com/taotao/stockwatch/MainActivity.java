package com.taotao.stockwatch;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {

    // INTERNET permission and ACCESS_NETWORK_STATE permission in the Manifest file first
    // To run more than 1 async task, need THREAD_POOL_EXECUTOR

    private static final String TAG = "MainActivity";
    private static final String URL_HEAD = "http://www.marketwatch.com/investing/stock/";

    // full set of stocks contains stocks symbol and name
    private HashMap<String, String> fullSet = new HashMap<>();
    // temporary list to store all stocks in database
    private List<String[]> dbList = new ArrayList<>();
    // a list of user stocks have full info
    private List<Stock> stockList = new ArrayList<>();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private StocksAdapter adapter;
    private DBHandler databaseHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new StocksAdapter(stockList, this);

        // set recycler view
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // start task to download the full set of stocks from web

        new NameDownloader(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // load database into a temporary list
        databaseHandler = new DBHandler(this);
        dbList = databaseHandler.loadStocks();
        Log.d(TAG, "onCreate: database load");

        // set swipe refresh Layout
        swipeRefreshLayout = findViewById(R.id.swiper);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                                    @Override
                                                    public void onRefresh() { doRefresh();
                                                    }
                                                });

    }

    @Override
    protected void onResume() {
        // check network first
        if (networkCheck()) {
            stockList.clear();
            for (int i=0; i<dbList.size(); i++) {
                new StockDownloader(this)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dbList.get(i)[0]);
            }
        } else {
            noNetworkDialog();
            stockList.clear();
            for (int i=0; i<dbList.size(); i++) {
                String[] s = dbList.get(i);
                Stock stock = new Stock(s[0], s[1]);
                stock.setPrice(0.00);
                stock.setChange(0.00);
                stock.setChangePercent(0.00);
                stockList.add(stock);
            }
            Collections.sort(stockList);
            adapter.notifyDataSetChanged();
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        databaseHandler.shutDown();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addMenu:
                // check network connection first
                if (!networkCheck()) {
                    noNetworkDialog();
                    return true;
                }
                // check if the full set map is downloaded
                if (fullSet.isEmpty()) {
                    new NameDownloader(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                // pop a dialog to add a new stock
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                final EditText editText = new EditText(this);
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                editText.setGravity(Gravity.CENTER_HORIZONTAL);

                builder.setTitle("Stock Selection");
                builder.setMessage("Please enter a Stock Symbol:");
                builder.setView(editText);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String symbol = editText.getText().toString();
                        if (!symbol.isEmpty()) {
                            // find symbol in the set
                            List<String[]> list = new ArrayList<>();
                            for (Map.Entry<String, String> entry : fullSet.entrySet()) {
                                if (entry.getKey().contains(symbol)) {
                                    String[] s = new String[2];
                                    s[0] = entry.getKey();
                                    s[1] = entry.getValue();
                                    list.add(s);
                                }
                            }
                            if (list.size() == 0) {
                                symbolNotFoundDialog(symbol);
                            } else if (list.size() == 1){
                                // add the stock to list
                                addStock(symbol);
                            } else {
                                // make a dialog to list all Stocks
                                listStockSelectionDialog(list);
                            }
                        }
                    }
                });
                builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        int pos = recyclerView.getChildLayoutPosition(v);
        Stock stock = stockList.get(pos);
        // open browser
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(URL_HEAD + stock.getSymbol()));
        startActivity(intent);
        // Toast.makeText(v.getContext(), stock.getSymbol(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onLongClick(View v) {
        final int pos = recyclerView.getChildLayoutPosition(v);
        final Stock stock = stockList.get(pos);

        // pop a dialog to confirm delete
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.outline_delete_forever_black_48);
        builder.setTitle("Delete Stock");
        builder.setMessage("Delete Stock Symbol '" + stock.getSymbol() + "'?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // delete stock from Database
                databaseHandler.deleteStock(stock.getSymbol());
                // remove stock from list
                stockList.remove(pos);
                Collections.sort(stockList);
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        builder.create().show();

        return false;
    }

    public void fetchFullSet(HashMap<String, String> data) {
        fullSet.clear();
        fullSet.putAll(data);
    }

    public void updateStockList(Stock stock) {
        stockList.add(stock);
        Collections.sort(stockList);
        adapter.notifyDataSetChanged();
    }

    private void addStock(String symbol) {
        // make sure new stock not in the list
        for (int i=0; i<stockList.size(); i++) {
            if (stockList.get(i).getSymbol().equals(symbol)) {
                duplicateStockDialog(symbol);
                return;
            }
        }
        StockDownloader task = new StockDownloader(MainActivity.this);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, symbol);
        try {
            Stock stock = task.get();
            databaseHandler.addStock(stock);
            Log.d(TAG, "addStock: stock added into database\n" + stock.toString());
        } catch (Exception e) {
            Log.e(TAG, "addStock: ", e);
        }
    }

    private void doRefresh() {
        Log.d(TAG, "doRefresh: Refresh");
        dbList.clear();
        dbList.addAll(databaseHandler.loadStocks());

        // check network first
        if (!networkCheck()) {
            noNetworkDialog();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // refresh data
        stockList.clear();
        for (String[] s : dbList) {
            new StockDownloader(this)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, s[0]);
        }

        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "Refresh", Toast.LENGTH_SHORT).show();
    }

    private void noNetworkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Network Connection");
        builder.setMessage("Stocks cannot be added without a network connection.");
        builder.create().show();
    }

    private void duplicateStockDialog(String symbol) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.outline_warning_black_48);
        builder.setTitle("Duplicate Stock");
        builder.setMessage("Stock symbol '" + symbol + "' is already displayed.");
        builder.create().show();
    }

    private void symbolNotFoundDialog(String symbol) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Symbol Not Found");
        builder.setMessage("Data for Stock symbol '" + symbol + "' not found.");
        builder.create().show();
    }

    private void listStockSelectionDialog(final List<String[]> list) {

        final CharSequence[] sArray = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            String[] s = list.get(i);
            sArray[i] = s[0] + " - " + s[1];
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make a selection");

        builder.setItems(sArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String symbol = list.get(which)[0];
                addStock(symbol);
            }
        });

        builder.setNegativeButton("Nevermind", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        AlertDialog dialog = builder.create();

        dialog.show();
    }

    private boolean networkCheck() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            return manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                    manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;
        }
        return false;
    }
}
