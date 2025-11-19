package com.example.b07project.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.example.b07project.models.ControllerMedicineLog;
import com.example.b07project.models.Report;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.RescueInhalerRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.renderer.YAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportGenerator {

    private Context context;
    private RescueInhalerRepository rescueRepository;
    private ControllerMedicineRepository controllerRepository;
    private SimpleDateFormat dateFormat;
    private static final float TINY_AXIS_TEXT_DP = 4f;

    public interface ReportCallback {
        void onSuccess(Report report);
        void onFailure(String error);
    }

    public ReportGenerator(Context context) {
        this.context = context;
        this.rescueRepository = new RescueInhalerRepository();
        this.controllerRepository = new ControllerMedicineRepository();
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    public void generateReport(String userId, int days, ReportCallback callback) {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1));
        Date startDate = calendar.getTime();

        // Fetch data for both rescue and controller
        rescueRepository.getLogsForUserInDateRange(userId, startDate, endDate, new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> rescueLogs) {
                controllerRepository.getLogsForUserInDateRange(userId, startDate, endDate, new ControllerMedicineRepository.LoadCallback() {
                    @Override
                    public void onSuccess(List<ControllerMedicineLog> controllerLogs) {
                        Report report = createReport(userId, days, startDate, endDate, rescueLogs, controllerLogs);
                        callback.onSuccess(report);
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    private Report createReport(String userId, int days, Date startDate, Date endDate,
                                List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs) {
        int totalRescue = 0;
        for (RescueInhalerLog log : rescueLogs) {
            totalRescue += log.getDoseCount();
        }

        int totalController = 0;
        for (ControllerMedicineLog log : controllerLogs) {
            totalController += log.getDoseCount();
        }

        double avgRescue = totalRescue / (double) days;
        double avgController = totalController / (double) days;

        return new Report(
                userId,
                days,
                System.currentTimeMillis(),
                startDate.getTime(),
                endDate.getTime(),
                totalRescue,
                totalController,
                avgRescue,
                avgController
        );
    }

    public void viewReport(Report report) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Date startDate = new Date(report.getStartDate());
        Date endDate = new Date(report.getEndDate());

        rescueRepository.getLogsForUserInDateRange(userId, startDate, endDate, new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> rescueLogs) {
                controllerRepository.getLogsForUserInDateRange(userId, startDate, endDate, new ControllerMedicineRepository.LoadCallback() {
                    @Override
                    public void onSuccess(List<ControllerMedicineLog> controllerLogs) {
                        generatePDF(report, rescueLogs, controllerLogs);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generatePDF(Report report, List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Title
        paint.setTextSize(24f);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("Medication Usage Report", 40, 60, paint);

        // Date range
        paint.setTextSize(14f);
        paint.setFakeBoldText(false);
        paint.setColor(Color.GRAY);
        canvas.drawText("Period: " + dateFormat.format(new Date(report.getStartDate())) + 
                       " - " + dateFormat.format(new Date(report.getEndDate())), 40, 90, paint);

        // Patient name
        String patientName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        canvas.drawText("Patient: " + patientName, 40, 115, paint);

        int yPosition = 160;

        // Rescue Inhaler Section
        paint.setTextSize(18f);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        canvas.drawText("Rescue Inhaler", 40, yPosition, paint);
        yPosition += 30;

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Total uses: " + report.getTotalRescueUses(), 40, yPosition, paint);
        yPosition += 20;
        canvas.drawText("Average per day: " + String.format("%.2f", report.getAvgRescuePerDay()), 40, yPosition, paint);
        yPosition += 40;

        // Draw rescue chart
        Bitmap rescueChartBitmap = createChartBitmap(rescueLogs, new Date(report.getStartDate()), report.getDays(), true);
        if (rescueChartBitmap != null) {
            canvas.drawBitmap(rescueChartBitmap, 40, yPosition, paint);
            yPosition += rescueChartBitmap.getHeight() + 40;
        }

        // Controller Medicine Section
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("Controller Medicine", 40, yPosition, paint);
        yPosition += 30;

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Total doses: " + report.getTotalControllerDoses(), 40, yPosition, paint);
        yPosition += 20;
        canvas.drawText("Average per day: " + String.format("%.2f", report.getAvgControllerPerDay()), 40, yPosition, paint);
        yPosition += 40;

        // Draw controller chart
        Bitmap controllerChartBitmap = createChartBitmap(controllerLogs, new Date(report.getStartDate()), report.getDays(), false);
        if (controllerChartBitmap != null) {
            canvas.drawBitmap(controllerChartBitmap, 40, yPosition, paint);
        }

        document.finishPage(page);

        // Save PDF
        String fileName = "report_" + report.getDays() + "days_" + System.currentTimeMillis() + ".pdf";
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            sharePDF(file);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createChartBitmap(List<?> logs, Date startDate, int totalDays, boolean isRescue) {
        final int chartWidth = 480;
        final int chartHeight = 180;
        final int bitmapHeight = 220; // extra space for month labels

        BarChart chart = new BarChart(context);
        chart.setLayoutParams(new android.view.ViewGroup.LayoutParams(chartWidth, chartHeight));
        chart.measure(
            View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
        );
        chart.layout(0, 0, chartWidth, chartHeight);
        chart.setXAxisRenderer(new TinyXAxisRenderer(chart.getViewPortHandler(), chart.getXAxis(), chart.getTransformer(YAxis.AxisDependency.LEFT), TINY_AXIS_TEXT_DP));
        chart.setRendererLeftYAxis(new TinyYAxisRenderer(chart.getViewPortHandler(), chart.getAxisLeft(), chart.getTransformer(YAxis.AxisDependency.LEFT), TINY_AXIS_TEXT_DP));

        // Aggregate data by day
        Map<String, Integer> dailyCounts = new HashMap<>();
        Map<Date, Integer> dateToCount = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault()); // Just the day number

        if (isRescue) {
            for (Object obj : logs) {
                RescueInhalerLog log = (RescueInhalerLog) obj;
                if (log.getTimestamp() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(log.getTimestamp());
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date dayDate = cal.getTime();
                    dateToCount.put(dayDate, dateToCount.getOrDefault(dayDate, 0) + log.getDoseCount());
                }
            }
        } else {
            for (Object obj : logs) {
                ControllerMedicineLog log = (ControllerMedicineLog) obj;
                if (log.getTimestamp() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(log.getTimestamp());
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date dayDate = cal.getTime();
                    dateToCount.put(dayDate, dateToCount.getOrDefault(dayDate, 0) + log.getDoseCount());
                }
            }
        }

        // Create bar entries based on report period
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        for (int i = 0; i < totalDays; i++) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date currentDate = calendar.getTime();
            
            String dayLabel = dayFormat.format(currentDate);
            labels.add(dayLabel);
            
            int count = dateToCount.getOrDefault(currentDate, 0);
            entries.add(new BarEntry(i, count));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#1976D2"));
        dataSet.setDrawValues(false); // Hide values on top of bars

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        chart.setData(barData);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setFitBars(true);

        // X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(6f);
        xAxis.setLabelCount(totalDays, false);

        // Y Axis
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setTextSize(6f);
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();

        Bitmap bitmap = Bitmap.createBitmap(chartWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas bitmapCanvas = new Canvas(bitmap);
        chart.draw(bitmapCanvas);

        // Draw month labels below
        Paint monthPaint = new Paint();
        monthPaint.setColor(Color.BLACK);
        monthPaint.setTextSize(14f);
        monthPaint.setAntiAlias(true);
        
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        calendar.setTime(startDate);
        
        String currentMonth = "";
        int monthStartIndex = 0;
        
        for (int i = 0; i < totalDays; i++) {
            String monthName = monthFormat.format(calendar.getTime());
            
            if (!monthName.equals(currentMonth)) {
                // Draw previous month label if exists
                if (!currentMonth.isEmpty() && i > 0) {
                    float monthStartX = 50 + (monthStartIndex * (380f / totalDays));
                    float monthEndX = 50 + (i * (380f / totalDays));
                    float monthCenterX = (monthStartX + monthEndX) / 2;
                    bitmapCanvas.drawText(currentMonth, monthCenterX - (monthPaint.measureText(currentMonth) / 2), 205, monthPaint);
                }
                currentMonth = monthName;
                monthStartIndex = i;
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Draw the last month label
        if (!currentMonth.isEmpty()) {
            float monthStartX = 50 + (monthStartIndex * (380f / totalDays));
            float monthEndX = 430;
            float monthCenterX = (monthStartX + monthEndX) / 2;
            bitmapCanvas.drawText(currentMonth, monthCenterX - (monthPaint.measureText(currentMonth) / 2), 205, monthPaint);
        }

        return bitmap;
    }

    private int drawSimpleBarChart(Canvas canvas, List<RescueInhalerLog> logs, int startY, String type) {
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#1976D2"));

        Map<String, Integer> dailyData = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());

        for (RescueInhalerLog log : logs) {
            if (log.getTimestamp() != null) {
                String day = dayFormat.format(log.getTimestamp());
                dailyData.put(day, dailyData.getOrDefault(day, 0) + log.getDoseCount());
            }
        }

        int barWidth = 15;
        int spacing = 20;
        int xPosition = 40;
        int maxHeight = 100;

        // Find max value for scaling
        int maxValue = dailyData.values().stream().max(Integer::compareTo).orElse(1);

        for (Map.Entry<String, Integer> entry : dailyData.entrySet()) {
            int barHeight = (int) ((entry.getValue() / (double) maxValue) * maxHeight);
            canvas.drawRect(xPosition, startY + maxHeight - barHeight, xPosition + barWidth, startY + maxHeight, paint);
            
            // Draw value on top
            paint.setTextSize(10f);
            paint.setColor(Color.BLACK);
            canvas.drawText(String.valueOf(entry.getValue()), xPosition, startY + maxHeight - barHeight - 5, paint);
            paint.setColor(Color.parseColor("#1976D2"));
            
            xPosition += barWidth + spacing;
            if (xPosition > 500) break; // Don't overflow page
        }

        return startY + maxHeight + 20;
    }

    private void sharePDF(File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share Report"));
    }

    private static class TinyXAxisRenderer extends XAxisRenderer {
        private final float textSizeDp;

        TinyXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans, float textSizeDp) {
            super(viewPortHandler, xAxis, trans);
            this.textSizeDp = textSizeDp;
        }

        @Override
        protected void drawLabel(Canvas c, String formattedLabel, float x, float y, MPPointF anchor, float angleDegrees) {
            float originalSize = mAxisLabelPaint.getTextSize();
            mAxisLabelPaint.setTextSize(Utils.convertDpToPixel(textSizeDp));
            super.drawLabel(c, formattedLabel, x, y, anchor, angleDegrees);
            mAxisLabelPaint.setTextSize(originalSize);
        }
    }

    private static class TinyYAxisRenderer extends YAxisRenderer {
        private final float textSizeDp;

        TinyYAxisRenderer(ViewPortHandler viewPortHandler, YAxis yAxis, Transformer trans, float textSizeDp) {
            super(viewPortHandler, yAxis, trans);
            this.textSizeDp = textSizeDp;
        }

        @Override
        public void renderAxisLabels(Canvas c) {
            float originalSize = mAxisLabelPaint.getTextSize();
            mAxisLabelPaint.setTextSize(Utils.convertDpToPixel(textSizeDp));
            super.renderAxisLabels(c);
            mAxisLabelPaint.setTextSize(originalSize);
        }
    }
}
