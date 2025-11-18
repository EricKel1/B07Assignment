package com.example.b07project.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.b07project.R;
import com.example.b07project.models.ControllerMedicineLog;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.RescueInhalerRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private BarChart rescueChart;
    private BarChart controllerChart;
    private RescueInhalerRepository rescueRepository;
    private ControllerMedicineRepository controllerRepository;
    private SimpleDateFormat dayFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        rescueChart = view.findViewById(R.id.rescueChart);
        controllerChart = view.findViewById(R.id.controllerChart);

        rescueRepository = new RescueInhalerRepository();
        controllerRepository = new ControllerMedicineRepository();
        dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
    }

    private void loadStatistics() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        android.util.Log.d("RescueServiceChart", "=== LOADING STATISTICS ===");
        android.util.Log.d("RescueServiceChart", "userId: " + userId);

        // Get date range (last 7 days)
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -6); // 6 days back + today = 7 days
        Date startDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        android.util.Log.d("RescueServiceChart", "Date range: " + sdf.format(startDate) + " to " + sdf.format(endDate));
        android.util.Log.d("RescueServiceChart", "Querying rescue inhaler logs...");

        // Load rescue inhaler data
        rescueRepository.getLogsForUserInDateRange(userId, startDate, endDate, new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> logs) {
                android.util.Log.d("RescueServiceChart", "Rescue query SUCCESS: Found " + logs.size() + " logs");
                for (RescueInhalerLog log : logs) {
                    if (log.getTimestamp() != null) {
                        android.util.Log.d("RescueServiceChart", "  - Log: " + sdf.format(log.getTimestamp()) + ", doses: " + log.getDoseCount());
                    }
                }
                setupRescueChart(logs, startDate);
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("RescueServiceChart", "Rescue query FAILED: " + error);
            }
        });

        // Load controller medicine data
        controllerRepository.getLogsForUserInDateRange(userId, startDate, endDate, new ControllerMedicineRepository.LoadCallback() {
            @Override
            public void onSuccess(List<ControllerMedicineLog> logs) {
                setupControllerChart(logs, startDate);
            }

            @Override
            public void onFailure(String error) {
                // Show error
            }
        });
    }

    private void setupRescueChart(List<RescueInhalerLog> logs, Date startDate) {
        android.util.Log.d("RescueServiceChart", "=== SETTING UP RESCUE CHART ===");
        Map<String, Integer> dailyCounts = aggregateByDay(logs, startDate);
        android.util.Log.d("RescueServiceChart", "Daily counts aggregated: " + dailyCounts.toString());
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        for (int i = 0; i < 7; i++) {
            String dayKey = dayFormat.format(calendar.getTime());
            labels.add(dayKey);
            int count = dailyCounts.getOrDefault(dayKey, 0);
            entries.add(new BarEntry(i, count));
            android.util.Log.d("RescueServiceChart", "Day " + i + " (" + dayKey + "): count=" + count);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#1976D2")); // Blue color
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        android.util.Log.d("RescueServiceChart", "Configuring rescue chart with " + entries.size() + " entries");
        configureChart(rescueChart, barData, labels);
        android.util.Log.d("RescueServiceChart", "Rescue chart setup complete!");
    }

    private void setupControllerChart(List<ControllerMedicineLog> logs, Date startDate) {
        Map<String, Integer> dailyCounts = aggregateControllerByDay(logs, startDate);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        for (int i = 0; i < 7; i++) {
            String dayKey = dayFormat.format(calendar.getTime());
            labels.add(dayKey);
            int count = dailyCounts.getOrDefault(dayKey, 0);
            entries.add(new BarEntry(i, count));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#1976D2")); // Blue color
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        configureChart(controllerChart, barData, labels);
    }

    private void configureChart(BarChart chart, BarData barData, List<String> labels) {
        chart.setData(barData);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setFitBars(true);
        chart.animateY(500);

        // X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);

        // Y Axis
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setTextSize(10f);
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();
    }

    private Map<String, Integer> aggregateByDay(List<RescueInhalerLog> logs, Date startDate) {
        Map<String, Integer> dailyCounts = new HashMap<>();

        for (RescueInhalerLog log : logs) {
            if (log.getTimestamp() != null) {
                String dayKey = dayFormat.format(log.getTimestamp());
                dailyCounts.put(dayKey, dailyCounts.getOrDefault(dayKey, 0) + log.getDoseCount());
            }
        }

        return dailyCounts;
    }

    private Map<String, Integer> aggregateControllerByDay(List<ControllerMedicineLog> logs, Date startDate) {
        Map<String, Integer> dailyCounts = new HashMap<>();

        for (ControllerMedicineLog log : logs) {
            if (log.getTimestamp() != null) {
                String dayKey = dayFormat.format(log.getTimestamp());
                dailyCounts.put(dayKey, dailyCounts.getOrDefault(dayKey, 0) + log.getDoseCount());
            }
        }

        return dailyCounts;
    }
}
