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
import com.example.b07project.repository.RescueInhalerRepository;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.models.PEFReading;
import com.example.b07project.models.PersonalBest;
import com.example.b07project.repository.PEFRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private BarChart rescueChart;
    private BarChart controllerChart;
    private LineChart pefChart;
    private RescueInhalerRepository rescueRepository;
    private ControllerMedicineRepository controllerRepository;
    private PEFRepository pefRepository;
    private SimpleDateFormat dayFormat;
    private String userId;

    public static StatisticsFragment newInstance(String userId) {
        StatisticsFragment fragment = new StatisticsFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        rescueChart = view.findViewById(R.id.rescueChart);
        controllerChart = view.findViewById(R.id.controllerChart);
        pefChart = view.findViewById(R.id.pefChart);

        rescueRepository = new RescueInhalerRepository();
        controllerRepository = new ControllerMedicineRepository();
        pefRepository = new PEFRepository();
        dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        if (getArguments() != null && getArguments().getString("userId") != null) {
            userId = getArguments().getString("userId");
        } else {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
    }

    private void loadStatistics() {
        if (userId == null) return;

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

        // Load PEF data
        pefRepository.getPEFReadingsForUser(userId, new PEFRepository.LoadCallback<List<PEFReading>>() {
            @Override
            public void onSuccess(List<PEFReading> readings) {
                // Filter for last 7 days
                List<PEFReading> recentReadings = new ArrayList<>();
                for (PEFReading reading : readings) {
                    if (reading.getTimestamp() != null && 
                        reading.getTimestamp().after(startDate) && 
                        reading.getTimestamp().before(endDate)) {
                        recentReadings.add(reading);
                    }
                }
                // Sort by timestamp ascending for the chart
                Collections.sort(recentReadings, (r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()));
                
                // Get Personal Best to draw zones
                pefRepository.getPersonalBest(userId, new PEFRepository.LoadCallback<PersonalBest>() {
                    @Override
                    public void onSuccess(PersonalBest pb) {
                        setupPEFChart(recentReadings, startDate, pb);
                    }

                    @Override
                    public void onFailure(String error) {
                        setupPEFChart(recentReadings, startDate, null);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("StatisticsFragment", "Failed to load PEF readings: " + error);
            }
        });
    }

    private void setupPEFChart(List<PEFReading> readings, Date startDate, PersonalBest pb) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        
        // Create labels for the last 7 days
        for (int i = 0; i < 7; i++) {
            labels.add(dayFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Map readings to days (0-6)
        // Note: This is a simplified mapping. If there are multiple readings per day, 
        // we might want to average them or show max. For now, let's show all points relative to their day index.
        // A better approach for LineChart is to use exact timestamps, but for consistency with the BarCharts,
        // let's map to the day index (0.0 to 6.99).
        
        long startTime = startDate.getTime();
        long dayMillis = 24 * 60 * 60 * 1000L;

        for (PEFReading reading : readings) {
            long diff = reading.getTimestamp().getTime() - startTime;
            float xPos = (float) diff / dayMillis;
            if (xPos >= 0 && xPos < 7) {
                entries.add(new Entry(xPos, reading.getValue()));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "PEF Values");
        dataSet.setColor(Color.parseColor("#1976D2"));
        dataSet.setCircleColor(Color.parseColor("#1976D2"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E3F2FD"));

        LineData lineData = new LineData(dataSet);
        pefChart.setData(lineData);

        // Configure Axis
        XAxis xAxis = pefChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(6f);

        YAxis leftAxis = pefChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        
        // Add Limit Lines for Zones if PB exists
        if (pb != null && pb.getValue() > 0) {
            float greenLimit = pb.getValue() * 0.8f;
            float yellowLimit = pb.getValue() * 0.5f;

            LimitLine llGreen = new LimitLine(greenLimit, "Green Zone (>80%)");
            llGreen.setLineWidth(2f);
            llGreen.setLineColor(Color.parseColor("#4CAF50"));
            llGreen.setTextColor(Color.parseColor("#4CAF50"));
            llGreen.setTextSize(10f);

            LimitLine llYellow = new LimitLine(yellowLimit, "Yellow Zone (50-80%)");
            llYellow.setLineWidth(2f);
            llYellow.setLineColor(Color.parseColor("#FFC107"));
            llYellow.setTextColor(Color.parseColor("#FFC107"));
            llYellow.setTextSize(10f);
            llYellow.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);

            leftAxis.addLimitLine(llGreen);
            leftAxis.addLimitLine(llYellow);
            leftAxis.setAxisMaximum(Math.max(pb.getValue() * 1.1f, 800f)); // Ensure PB is visible
        }

        pefChart.getAxisRight().setEnabled(false);
        pefChart.getDescription().setEnabled(false);
        pefChart.getLegend().setEnabled(false);
        pefChart.setTouchEnabled(true);
        pefChart.setDragEnabled(true);
        pefChart.setScaleEnabled(true);
        pefChart.setPinchZoom(true);

        pefChart.invalidate();
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
