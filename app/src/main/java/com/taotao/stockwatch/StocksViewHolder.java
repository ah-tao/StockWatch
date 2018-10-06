package com.taotao.stockwatch;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class StocksViewHolder extends RecyclerView.ViewHolder {

    public TextView symbol;
    public TextView price;
    public TextView change;
    public TextView company;


    public StocksViewHolder(@NonNull View itemView) {
        super(itemView);
        symbol = itemView.findViewById(R.id.symbolText);
        price = itemView.findViewById(R.id.priceText);
        change = itemView.findViewById(R.id.changeText);
        company = itemView.findViewById(R.id.companyText);
    }
}
