package com.taotao.stockwatch;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DecimalFormat;
import java.util.List;

public class StocksAdapter extends RecyclerView.Adapter<StocksViewHolder> {

    private static final String TAG = "StocksAdapter";
    private List<Stock> stockList;
    private MainActivity mainActivity;
    private final String TRIANGLE_UP = "\u25B2";
    private final String TRIANGLE_DOWN = "\u25BC";

    public StocksAdapter(List<Stock> stockList, MainActivity mainActivity) {
        this.stockList = stockList;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public StocksViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Log.d(TAG, "onCreateViewHolder: Making New");
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.stock_view, viewGroup, false);

        view.setOnClickListener(mainActivity);
        view.setOnLongClickListener(mainActivity);

        return new StocksViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StocksViewHolder stocksViewHolder, int i) {
        Stock stock = stockList.get(i);
        stocksViewHolder.symbol.setText(stock.getSymbol());
        stocksViewHolder.company.setText(stock.getCompany());
        // format price and change to string type
        DecimalFormat decimalFormat = new DecimalFormat("#0.00");
        String priceStr = decimalFormat.format(stock.getPrice());
        stocksViewHolder.price.setText(priceStr);

        String changeStr = " " + decimalFormat.format(stock.getChange()) + " ("
                + decimalFormat.format(stock.getChangePercent()) + "%)";
        if (stock.getChange() < 0) {
            changeStr = TRIANGLE_DOWN + changeStr;
            setColor(stocksViewHolder, Color.RED);
        } else if (stock.getChange() > 0) {
            changeStr = TRIANGLE_UP + changeStr;
            setColor(stocksViewHolder, Color.GREEN);
        } else {
            changeStr = " " + changeStr;
            setColor(stocksViewHolder, Color.BLACK);
        }
        stocksViewHolder.change.setText(changeStr);
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    private void setColor(StocksViewHolder holder, int color) {
        holder.symbol.setTextColor(color);
        holder.company.setTextColor(color);
        holder.price.setTextColor(color);
        holder.change.setTextColor(color);
    }
}