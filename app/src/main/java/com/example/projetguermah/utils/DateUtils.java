package com.example.projetguermah.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    public static String formatDate(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(date);
    }

    public static String formatDateShort(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("MMM dd", Locale.getDefault()).format(date);
    }

    public static String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}