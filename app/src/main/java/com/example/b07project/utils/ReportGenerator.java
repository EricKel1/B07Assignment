package com.example.b07project.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.example.b07project.R;
import com.example.b07project.models.ControllerMedicineLog;
import com.example.b07project.models.Report;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.RescueInhalerRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportGenerator {

    private static final String TAG = "reportdebugging";
    private Context context;
    private RescueInhalerRepository rescueRepository;
    private ControllerMedicineRepository controllerRepository;
    private SimpleDateFormat dateFormat;
    private static final float TINY_AXIS_TEXT_DP = 2.5f;
    private AlertDialog progressDialog;

    public enum ReportAction {
        SHARE,
        DOWNLOAD
    }

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

    public void shareReport(Report report) {
        processReport(report, ReportAction.SHARE);
    }

    public void downloadReport(Report report) {
        processReport(report, ReportAction.DOWNLOAD);
    }

    private void processReport(Report report, ReportAction action) {
        showLoading();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Date startDate = new Date(report.getStartDate());
        Date endDate = new Date(report.getEndDate());

        rescueRepository.getLogsForUserInDateRange(userId, startDate, endDate, new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> rescueLogs) {
                controllerRepository.getLogsForUserInDateRange(userId, startDate, endDate, new ControllerMedicineRepository.LoadCallback() {
                    @Override
                    public void onSuccess(List<ControllerMedicineLog> controllerLogs) {
                        generatePDF(report, rescueLogs, controllerLogs, action);
                    }

                    @Override
                    public void onFailure(String error) {
                        dismissLoading();
                        Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                dismissLoading();
                Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_loading, null);
        builder.setView(view);
        builder.setCancelable(false);
        progressDialog = builder.create();
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        progressDialog.show();
    }

    private void dismissLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void generatePDF(Report report, List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs, ReportAction action) {
        Log.d(TAG, "generatePDF: Starting generation");
        
        String htmlContent = buildReportHtml(report, rescueLogs, controllerLogs);
        Log.d(TAG, "generatePDF: HTML generated, length: " + htmlContent.length());

        String fileName = "AsthmaReport_" + report.getDays() + "days_" + System.currentTimeMillis() + ".pdf";
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        Log.d(TAG, "generatePDF: Output file path: " + file.getAbsolutePath());
        renderHtmlToPdf(htmlContent, file, action);
    }

    private String buildReportHtml(Report report, List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String dateRange = sdf.format(new Date(report.getStartDate())) + " - " + sdf.format(new Date(report.getEndDate()));
        String patientName = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "Patient";
        
        StringBuilder rows = new StringBuilder();
        Map<Long, Integer> rescueDaily = aggregateLogsByDay(rescueLogs, true);
        Map<Long, Integer> controllerDaily = aggregateLogsByDay(controllerLogs, false);
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(report.getStartDate()));
        SimpleDateFormat rowDateFmt = new SimpleDateFormat("MMM d", Locale.getDefault());
        SimpleDateFormat chartLabelFmt = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM", Locale.getDefault());

        List<String> chartLabels = new ArrayList<>();
        List<Integer> rescueData = new ArrayList<>();
        List<Integer> controllerData = new ArrayList<>();
        
        // For month grouping
        List<String> monthLabels = new ArrayList<>();
        String currentMonth = "";
        int currentMonthStart = 0;

        for(int i=0; i<report.getDays(); i++) {
            long key = normalizeDate(calendar.getTime());
            int rCount = rescueDaily.getOrDefault(key, 0);
            int cCount = controllerDaily.getOrDefault(key, 0);
            
            chartLabels.add("'" + chartLabelFmt.format(calendar.getTime()) + "'");
            rescueData.add(rCount);
            controllerData.add(cCount);

            String monthName = monthFmt.format(calendar.getTime());
            if (!monthName.equals(currentMonth)) {
                if (!currentMonth.isEmpty()) {
                    // Add previous month
                    // We need to know start index and end index (i-1)
                    // But Chart.js doesn't support multi-level axis easily without plugins.
                    // We will use a simpler approach: just label the days, and maybe add month to the first day of month?
                    // Or use the user's request: "label the months in the x-axis similarly to the attached image"
                    // The attached image has a second x-axis for months.
                }
                currentMonth = monthName;
                currentMonthStart = i;
            }

            String rStyle = rCount > 4 ? "color: #dc3545; font-weight: bold;" : "";
            
            rows.append("<tr>")
                .append("<td>").append(rowDateFmt.format(calendar.getTime())).append("</td>")
                .append("<td style='").append(rStyle).append("'>").append(rCount).append("</td>")
                .append("<td>").append(cCount).append("</td>")
                .append("</tr>");
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Re-loop to build the month ranges for the JS
        // We will inject a custom plugin script into the HTML to draw the month labels
        StringBuilder monthRangesJs = new StringBuilder("[");
        calendar.setTime(new Date(report.getStartDate()));
        currentMonth = "";
        currentMonthStart = 0;
        
        for(int i=0; i<report.getDays(); i++) {
            String monthName = monthFmt.format(calendar.getTime());
            if (!monthName.equals(currentMonth)) {
                if (!currentMonth.isEmpty()) {
                    monthRangesJs.append("{label:'").append(currentMonth).append("', start:").append(currentMonthStart).append(", end:").append(i-1).append("},");
                }
                currentMonth = monthName;
                currentMonthStart = i;
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        // Add last month
        monthRangesJs.append("{label:'").append(currentMonth).append("', start:").append(currentMonthStart).append(", end:").append(report.getDays()-1).append("}");
        monthRangesJs.append("]");

        StringBuilder labelsJs = new StringBuilder("[");
        StringBuilder rescueJs = new StringBuilder("[");
        StringBuilder controllerJs = new StringBuilder("[");
        
        for (int i = 0; i < chartLabels.size(); i++) {
            labelsJs.append(chartLabels.get(i));
            rescueJs.append(rescueData.get(i));
            controllerJs.append(controllerData.get(i));
            if (i < chartLabels.size() - 1) {
                labelsJs.append(",");
                rescueJs.append(",");
                controllerJs.append(",");
            }
        }
        labelsJs.append("]");
        rescueJs.append("]");
        controllerJs.append("]");

        // Determine font size based on density
        int tickFontSize = report.getDays() > 30 ? 8 : 10;

        return "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                // Use UMD build to ensure global 'Chart' variable is available
                "<script src='https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js'></script>" +
                "<style>" +
                "body { font-family: Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 40px 40px 0 40px; color: #333; }" +
                ".header { text-align: center; margin-bottom: 40px; border-bottom: 3px solid #4F46E5; padding-bottom: 20px; }" +
                ".header h1 { color: #4F46E5; margin: 0; font-size: 28px; }" +
                ".meta { color: #666; margin-top: 10px; font-size: 14px; }" +
                ".metrics { display: flex; gap: 20px; margin-bottom: 40px; }" +
                ".card { flex: 1; background: #F3F4F6; padding: 20px; border-radius: 10px; text-align: center; }" +
                ".card h3 { margin: 0 0 10px; font-size: 12px; text-transform: uppercase; color: #6B7280; letter-spacing: 1px; }" +
                ".card .val { font-size: 24px; font-weight: bold; color: #111827; }" +
                ".charts { margin-bottom: 40px; }" +
                ".chart-box { margin-bottom: 30px; background: white; border: 1px solid #E5E7EB; border-radius: 8px; padding: 15px; }" +
                ".chart-box h2 { font-size: 18px; color: #111827; border-left: 4px solid #4F46E5; padding-left: 10px; margin-bottom: 15px; }" +
                "canvas { width: 100% !important; height: 250px !important; }" +
                "table { width: 100%; border-collapse: collapse; font-size: 13px; }" +
                "th { text-align: left; padding: 12px; background: #F9FAFB; color: #6B7280; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; border-bottom: 1px solid #E5E7EB; }" +
                "td { padding: 12px; border-bottom: 1px solid #E5E7EB; }" +
                "tr:nth-child(even) { background: #F9FAFB; }" +
                "</style></head><body>" +
                "<div class='header'>" +
                "<h1>Asthma Control Report</h1>" +
                "<div class='meta'>" + patientName + " &bull; " + dateRange + "</div>" +
                "</div>" +
                "<div class='metrics'>" +
                "<div class='card'><h3>Total Rescue</h3><div class='val'>" + report.getTotalRescueUses() + "</div></div>" +
                "<div class='card'><h3>Total Controller</h3><div class='val'>" + report.getTotalControllerDoses() + "</div></div>" +
                "<div class='card'><h3>Avg Rescue/Day</h3><div class='val'>" + String.format(Locale.US, "%.1f", report.getAvgRescuePerDay()) + "</div></div>" +
                "</div>" +
                "<div class='charts'>" +
                "<div class='chart-box'><h2>Rescue Inhaler Trends</h2><canvas id='rescueChart'></canvas></div>" +
                "<div class='chart-box'><h2>Controller Medicine Trends</h2><canvas id='controllerChart'></canvas></div>" +
                "</div>" +
                "<div class='chart-box daily-breakdown'>" +
                "<h2>Daily Breakdown</h2>" +
                "<table><thead><tr><th>Date</th><th>Rescue Puffs</th><th>Controller Doses</th></tr></thead>" +
                "<tbody>" + rows.toString() + "</tbody></table>" +
                "</div>" +
                "<script>" +
                "try {" +
                "  const monthRanges = " + monthRangesJs.toString() + ";" +
                "  const monthLabelPlugin = {" +
                "    id: 'monthLabels'," +
                "    afterDraw: (chart) => {" +
                "      try {" +
                "        const ctx = chart.ctx;" +
                "        const xAxis = chart.scales.x;" +
                "        if (!xAxis) return;" +
                "        const yBottom = xAxis.bottom;" +
                "        ctx.save();" +
                "        ctx.font = '12px Arial';" +
                "        ctx.fillStyle = '#666';" +
                "        ctx.textAlign = 'center';" +
                "        monthRanges.forEach(range => {" +
                "          const startX = xAxis.getPixelForValue(range.start);" +
                "          const endX = xAxis.getPixelForValue(range.end);" +
                "          const width = endX - startX;" +
                "          const x = startX + (width / 2);" +
                "          ctx.fillText(range.label, x, yBottom + 25);" +
                "          if (range.end < " + (report.getDays() - 1) + ") {" +
                "             const lineX = (xAxis.getPixelForValue(range.end) + xAxis.getPixelForValue(range.end + 1)) / 2;" +
                "             ctx.beginPath();" +
                "             ctx.moveTo(lineX, yBottom);" +
                "             ctx.lineTo(lineX, yBottom + 30);" +
                "             ctx.strokeStyle = '#ddd';" +
                "             ctx.stroke();" +
                "          }" +
                "        });" +
                "        ctx.restore();" +
                "      } catch(e) { console.error('Plugin error:', e); }" +
                "    }" +
                "  };" +
                "  if (typeof Chart === 'undefined') { throw new Error('Chart.js library not loaded. Check internet connection.'); }" +
                "  const commonOptions = {" +
                "    animation: false," +
                "    responsive: true," +
                "    maintainAspectRatio: false," +
                "    layout: { padding: { bottom: 30 } }," +
                "    scales: {" +
                "      y: { beginAtZero: true, ticks: { stepSize: 1 } }," +
                "      x: { grid: { display: false }, ticks: { font: { size: " + tickFontSize + " } } }" +
                "    }," +
                "    plugins: { legend: { display: false } }" +
                "  };" +
                "  new Chart(document.getElementById('rescueChart'), { type: 'bar', data: { labels: " + labelsJs + ", datasets: [{ label: 'Puffs', data: " + rescueJs + ", backgroundColor: '#3B82F6', borderRadius: 4 }] }, options: commonOptions, plugins: [monthLabelPlugin] });" +
                "  new Chart(document.getElementById('controllerChart'), { type: 'bar', data: { labels: " + labelsJs + ", datasets: [{ label: 'Doses', data: " + controllerJs + ", backgroundColor: '#10B981', borderRadius: 4 }] }, options: commonOptions, plugins: [monthLabelPlugin] });" +
                "  setTimeout(applyPagination, 100);" +
                "} catch(e) {" +
                "  console.error('Chart Init Error:', e);" +
                "}" +
                "function applyPagination() {" +
                "  const density = window.devicePixelRatio || 1;" +
                "  const pageHeightPx = 1754;" +
                "  const pageHeight = pageHeightPx / density;" +
                "  const elements = Array.from(document.querySelectorAll('.header, .metrics, .chart-box:not(.daily-breakdown), .daily-breakdown h2, tr'));" +
                "  for (let i = 0; i < elements.length; i++) {" +
                "    const el = elements[i];" +
                "    if (el.offsetParent === null) continue;" +
                "    if (el.tagName === 'TH' || el.closest('thead')) continue;" +
                "    const rect = el.getBoundingClientRect();" +
                "    const top = rect.top + window.scrollY;" +
                "    const height = rect.height;" +
                "    const startPage = Math.floor(top / pageHeight);" +
                "    const endPage = Math.floor((top + height - 1) / pageHeight);" +
                "    if (startPage !== endPage) {" +
                "      const nextPageStart = (startPage + 1) * pageHeight;" +
                "      const spacerHeight = nextPageStart - top;" +
                "      if (el.tagName === 'TR') {" +
                "        const spacerRow = document.createElement('tr');" +
                "        spacerRow.style.height = spacerHeight + 'px';" +
                "        spacerRow.style.border = 'none';" +
                "        spacerRow.style.background = 'transparent';" +
                "        const cell = document.createElement('td');" +
                "        cell.colSpan = 3;" +
                "        cell.style.border = 'none';" +
                "        spacerRow.appendChild(cell);" +
                "        el.parentNode.insertBefore(spacerRow, el);" +
                "      } else if (el.tagName === 'H2' && el.closest('.daily-breakdown')) {" +
                "        const container = el.closest('.daily-breakdown');" +
                "        const style = window.getComputedStyle(container);" +
                "        const currentMargin = parseFloat(style.marginTop) || 0;" +
                "        container.style.marginTop = (currentMargin + spacerHeight) + 'px';" +
                "      } else {" +
                "        const style = window.getComputedStyle(el);" +
                "        const currentMargin = parseFloat(style.marginTop) || 0;" +
                "        el.style.marginTop = (currentMargin + spacerHeight) + 'px';" +
                "      }" +
                "    }" +
                "  }" +
                "}" +
                "</script>" +
                "</body></html>";
    }

    private void renderHtmlToPdf(String html, File outputFile, ReportAction action) {
        Log.d(TAG, "renderHtmlToPdf: Initializing WebView on Main Thread");
        new Handler(Looper.getMainLooper()).post(() -> {
            WebView webView = new WebView(context);
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // Crucial for off-screen
            Log.d(TAG, "renderHtmlToPdf: WebView created, JS enabled");
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "onPageFinished: Web page finished loading");
                    
                    // Use higher resolution for better quality (approx 2x A4 at 72dpi)
                    // A4 at ~150dpi: 1240 x 1754
                    int pageHeight = 1754;
                    int pageWidth = 1240;

                    // Measure and Layout IMMEDIATELY to trigger rendering pipeline
                    view.measure(View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
                                 View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    
                    // Initial layout to ensure rendering starts
                    view.layout(0, 0, pageWidth, Math.max(view.getMeasuredHeight(), pageHeight));
                    Log.d(TAG, "onPageFinished: Initial layout applied. Waiting for render...");

                    // Delay to allow the WebView to rasterize the new layout and JS charts
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // Re-measure the true content height after JS has run and layout settled
                        int contentHeightVal = (int) (view.getContentHeight() * context.getResources().getDisplayMetrics().density);
                        int measuredHeight = view.getMeasuredHeight();
                        
                        Log.d(TAG, "onPageFinished: Delayed Measure - ContentHeight*Density: " + contentHeightVal + ", Measured: " + measuredHeight);

                        // Use the content height, but ensure it's at least the page height
                        int calculatedHeight = contentHeightVal > 0 ? contentHeightVal : measuredHeight;
                        if (calculatedHeight <= 0) calculatedHeight = pageHeight;
                        
                        final int finalContentHeight = calculatedHeight;

                        // Force layout to the exact content height to ensure all content is rendered
                        view.layout(0, 0, pageWidth, finalContentHeight);
                        
                        // Small delay to allow the WebView to update its internal viewport/buffers after resize
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Log.d(TAG, "onPageFinished: Starting PDF capture. Final Height: " + finalContentHeight);
                            PdfDocument document = new PdfDocument();
                            
                            int totalPages = (int) Math.ceil((double) finalContentHeight / pageHeight);
                            Log.d(TAG, "onPageFinished: Total pages to generate: " + totalPages);
                            
                            for (int i = 0; i < totalPages; i++) {
                                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
                                PdfDocument.Page page = document.startPage(pageInfo);
                                Canvas canvas = page.getCanvas();
                                
                                // Translate the canvas to draw the correct portion of the WebView
                                canvas.save();
                                canvas.translate(0, -i * pageHeight);
                                // Clip to ensure we don't draw garbage from outside the page bounds (though translate handles most)
                                canvas.clipRect(0, i * pageHeight, pageWidth, (i + 1) * pageHeight);
                                
                                view.draw(canvas);
                                canvas.restore();
                                
                                document.finishPage(page);
                            }
                            
                            try {
                                Log.d(TAG, "onPageFinished: Writing document to file: " + outputFile.getAbsolutePath());
                                FileOutputStream fos = new FileOutputStream(outputFile);
                                document.writeTo(fos);
                                fos.close();
                                Log.d(TAG, "onPageFinished: Document written successfully");
                                if (action == ReportAction.SHARE) {
                                    sharePDF(outputFile);
                                } else {
                                    Toast.makeText(context, "Report saved to Documents", Toast.LENGTH_LONG).show();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "onPageFinished: Error writing PDF", e);
                                e.printStackTrace();
                                Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show();
                            } finally {
                                document.close();
                                Log.d(TAG, "onPageFinished: Document closed");
                                dismissLoading();
                            }
                        }, 250); // Short delay for resize to take effect
                    }, 1500); // 1.5 seconds delay for JS charts
                }
            });
            
            Log.d(TAG, "renderHtmlToPdf: Loading HTML data");
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        });
    }

    private void sharePDF(File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share Report"));
    }

    private Map<Long, Integer> aggregateLogsByDay(List<?> logs, boolean isRescue) {
        Map<Long, Integer> counts = new HashMap<>();
        if (isRescue) {
            for (Object obj : logs) {
                RescueInhalerLog log = (RescueInhalerLog) obj;
                if (log.getTimestamp() != null) {
                    long key = normalizeDate(log.getTimestamp());
                    counts.put(key, counts.getOrDefault(key, 0) + log.getDoseCount());
                }
            }
        } else {
            for (Object obj : logs) {
                ControllerMedicineLog log = (ControllerMedicineLog) obj;
                if (log.getTimestamp() != null) {
                    long key = normalizeDate(log.getTimestamp());
                    counts.put(key, counts.getOrDefault(key, 0) + log.getDoseCount());
                }
            }
        }
        return counts;
    }

    private long normalizeDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
