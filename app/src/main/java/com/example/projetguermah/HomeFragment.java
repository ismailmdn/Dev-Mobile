package com.example.projetguermah;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Find chart view
        BarChart barChart = root.findViewById(R.id.barChart);

        // Prepare sample data
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 100));
        entries.add(new BarEntry(1f, 80));
        entries.add(new BarEntry(2f, 120));

        // Create dataset
        BarDataSet dataSet = new BarDataSet(entries, "Expenses");
        dataSet.setColor(getResources().getColor(R.color.teal_700)); // Optional color

        // Set data to chart
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // Styling
        XAxis xAxis = barChart.getXAxis();
        xAxis.setDrawLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate(); // Refresh

        return root;
    }
}
