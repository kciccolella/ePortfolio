package com.zybooks.ciccolella_weight_trackingapp;

import com.jjoe64.graphview.DefaultLabelFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Custom label formatter for GraphView's X-Axis, displaying dates (MM-dd) instead of full dates.
 */
public class DateAsXAxisLabelFormatter extends DefaultLabelFormatter {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");  // Format to show only Month and Day

    @Override
    public String formatLabel(double value, boolean isValueX) {
        if (isValueX) {
            // Convert the xValue (which is a timestamp or index) to a date string
            return dateFormat.format(new Date((long) value));  // Convert xValue (timestamp) to Month-Day format
        } else {
            return super.formatLabel(value, isValueX); // Return y-axis labels normally
        }
    }
}