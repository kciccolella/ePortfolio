package com.zybooks.ciccolella_weight_trackingapp;

public class WeightEntry {
    private long id;           // Add an id field
    private String date;
    private double weight;

    public WeightEntry(long id, String date, double weight) {
        this.id = id;
        this.date = date;
        this.weight = weight;
    }

    public long getId() {
        return id; // Getter for id
    }

    public String getDate() {
        return date;
    }

    public double getWeight() {
        return weight;
    }

    // Compare based on the date (for sorting purposes)
    public int compareTo(WeightEntry other) {
        return this.date.compareTo(other.date);  // Change to handle oldest to newest or vice versa
    }
}