package com.taotao.stockwatch;

import android.support.annotation.NonNull;

import java.io.Serializable;

class Stock implements Serializable, Comparable<Stock> {
    private String symbol;
    private String company;
    private double price;
    private double change;
    private double changePercent;

    public Stock() {
        this.symbol = "*";
    }

    public Stock(String symbol, String company) {
        this.symbol = symbol;
        this.company = company;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "symbol='" + symbol + '\'' +
                ", company='" + company + '\'' +
                ", price=" + price +
                ", change=" + change +
                ", changePercent=" + changePercent +
                '}';
    }

    @Override
    public int compareTo(@NonNull Stock that) {
        return this.getSymbol().compareTo(that.getSymbol());
    }

}
