package com.example.b07project.utils;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import com.example.b07project.models.PEFReading;
import com.example.b07project.models.Report;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.models.SymptomCheckIn;
import com.example.b07project.models.TriageSession;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.PEFRepository;
import com.example.b07project.repository.RescueInhalerRepository;
import com.example.b07project.repository.SymptomCheckInRepository;
import com.example.b07project.repository.TriageRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    private TriageRepository triageRepository;
    private PEFRepository pefRepository;
    private SymptomCheckInRepository symptomRepository;
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
        this.triageRepository = new TriageRepository();
        this.pefRepository = new PEFRepository();
        this.symptomRepository = new SymptomCheckInRepository();
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    public void generateReport(String userId, int days, boolean includeTriage, boolean includeRescue, boolean includeController, boolean includeSymptoms, boolean includeZones, boolean includeDailyLogs, boolean includeTriggerChart, ReportCallback callback) {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1));
        Date startDate = calendar.getTime();
        generateReport(userId, startDate, endDate, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, callback);
    }

    public void generateReport(String userId, Date startDate, Date endDate, boolean includeTriage, boolean includeRescue, boolean includeController, boolean includeSymptoms, boolean includeZones, boolean includeDailyLogs, boolean includeTriggerChart, ReportCallback callback) {
        Log.d(TAG, "generateReport: Starting for user " + userId + " from " + startDate + " to " + endDate);
        long diff = endDate.getTime() - startDate.getTime();
        int days = (int) (diff / (24 * 60 * 60 * 1000)) + 1;
        if (days < 1) days = 1;
        Log.d(TAG, "generateReport: Calculated days: " + days);

        // Fetch data from all repositories
        int finalDays = days;
        rescueRepository.getLogsForUserInDateRange(userId, startDate, endDate, new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> rescueLogs) {
                Log.d(TAG, "generateReport: Rescue logs fetched: " + rescueLogs.size());
                controllerRepository.getLogsForUserInDateRange(userId, startDate, endDate, new ControllerMedicineRepository.LoadCallback() {
                    @Override
                    public void onSuccess(List<ControllerMedicineLog> controllerLogs) {
                        Log.d(TAG, "generateReport: Controller logs fetched: " + controllerLogs.size());
                        fetchOptionalData(userId, startDate, endDate, finalDays, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, rescueLogs, controllerLogs, callback);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "generateReport: Controller fetch failed: " + error);
                        callback.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "generateReport: Rescue fetch failed: " + error);
                callback.onFailure(error);
            }
        });
    }

    private void fetchOptionalData(String userId, Date startDate, Date endDate, int days, boolean includeTriage, boolean includeRescue, boolean includeController, boolean includeSymptoms, boolean includeZones, boolean includeDailyLogs, boolean includeTriggerChart,
                                   List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                   ReportCallback callback) {
        Log.d(TAG, "fetchOptionalData: Fetching triage sessions...");
        triageRepository.getTriageSessions(userId, new TriageRepository.LoadCallback<List<TriageSession>>() {
            @Override
            public void onSuccess(List<TriageSession> allTriageSessions) {
                Log.d(TAG, "fetchOptionalData: Triage sessions fetched: " + allTriageSessions.size());
                fetchPEF(userId, startDate, endDate, days, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, rescueLogs, controllerLogs, allTriageSessions, callback);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "fetchOptionalData: Triage fetch failed: " + error);
                fetchPEF(userId, startDate, endDate, days, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, rescueLogs, controllerLogs, new ArrayList<>(), callback);
            }
        });
    }

    private void fetchPEF(String userId, Date startDate, Date endDate, int days, boolean includeTriage, boolean includeRescue, boolean includeController, boolean includeSymptoms, boolean includeZones, boolean includeDailyLogs, boolean includeTriggerChart,
                          List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                          List<TriageSession> triageSessions, ReportCallback callback) {
        Log.d(TAG, "fetchPEF: Fetching PEF readings...");
        pefRepository.getPEFReadingsForUser(userId, new PEFRepository.LoadCallback<List<PEFReading>>() {
            @Override
            public void onSuccess(List<PEFReading> allPefReadings) {
                Log.d(TAG, "fetchPEF: PEF readings fetched: " + allPefReadings.size());
                fetchSymptoms(userId, startDate, endDate, days, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, rescueLogs, controllerLogs, triageSessions, allPefReadings, callback);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "fetchPEF: PEF fetch failed: " + error);
                fetchSymptoms(userId, startDate, endDate, days, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, rescueLogs, controllerLogs, triageSessions, new ArrayList<>(), callback);
            }
        });
    }

    private void fetchSymptoms(String userId, Date startDate, Date endDate, int days, boolean includeTriage, boolean includeRescue, boolean includeController, boolean includeSymptoms, boolean includeZones, boolean includeDailyLogs, boolean includeTriggerChart,
                               List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                               List<TriageSession> triageSessions, List<PEFReading> pefReadings,
                               ReportCallback callback) {
        Log.d(TAG, "fetchSymptoms: Fetching symptoms...");
        symptomRepository.getCheckInsForUser(userId, new SymptomCheckInRepository.LoadCallback() {
            @Override
            public void onSuccess(List<SymptomCheckIn> allSymptomCheckIns) {
                Log.d(TAG, "fetchSymptoms: Symptoms fetched: " + allSymptomCheckIns.size());
                finalizeReport(userId, startDate, endDate, days, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, rescueLogs, controllerLogs, triageSessions, pefReadings, allSymptomCheckIns, callback);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "fetchSymptoms: Symptom fetch failed: " + error);
                finalizeReport(userId, startDate, endDate, days, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, rescueLogs, controllerLogs, triageSessions, pefReadings, new ArrayList<>(), callback);
            }
        });
    }

    private void finalizeReport(String userId, Date startDate, Date endDate, int days, boolean includeTriage, boolean includeRescue, boolean includeController, boolean includeSymptoms, boolean includeZones, boolean includeDailyLogs, boolean includeTriggerChart,
                                List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                List<TriageSession> allTriageSessions, List<PEFReading> allPefReadings,
                                List<SymptomCheckIn> allSymptomCheckIns, ReportCallback callback) {
        Log.d(TAG, "finalizeReport: Finalizing report...");
        // Filter data by date range
        List<TriageSession> triageSessions = filterByDate(allTriageSessions, startDate, endDate);
        List<PEFReading> pefReadings = filterByDate(allPefReadings, startDate, endDate);
        List<SymptomCheckIn> symptomCheckIns = filterByDate(allSymptomCheckIns, startDate, endDate);

        Report report = createReport(userId, days, includeTriage, includeRescue, includeController, includeSymptoms, includeZones, includeDailyLogs, includeTriggerChart, startDate, endDate, 
            rescueLogs, controllerLogs, triageSessions, pefReadings, symptomCheckIns);
        Log.d(TAG, "finalizeReport: Report created, calling onSuccess");
        callback.onSuccess(report);
    }

    private <T> List<T> filterByDate(List<T> items, Date startDate, Date endDate) {
        List<T> filtered = new ArrayList<>();
        long start = normalizeDate(startDate);
        long end = normalizeDate(endDate) + (24 * 60 * 60 * 1000) - 1; // End of day

        for (T item : items) {
            Date date = null;
            if (item instanceof TriageSession) {
                date = ((TriageSession) item).getStartTime();
            } else if (item instanceof PEFReading) {
                date = ((PEFReading) item).getTimestamp();
            } else if (item instanceof SymptomCheckIn) {
                date = ((SymptomCheckIn) item).getDate();
            }

            if (date != null) {
                long time = date.getTime();
                if (time >= start && time <= end) {
                    filtered.add(item);
                }
            }
        }
        return filtered;
    }

    private Report createReport(String userId, int days, boolean includeTriage, boolean includeRescue, boolean includeController, boolean includeSymptoms, boolean includeZones, boolean includeDailyLogs, boolean includeTriggerChart, Date startDate, Date endDate,
                                List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                List<TriageSession> triageSessions, List<PEFReading> pefReadings,
                                List<SymptomCheckIn> symptomCheckIns) {
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

        // Calculate Controller Adherence (% of days with at least one dose)
        Map<Long, Boolean> controllerDays = new HashMap<>();
        for (ControllerMedicineLog log : controllerLogs) {
            controllerDays.put(normalizeDate(log.getTimestamp()), true);
        }
        double controllerAdherence = (controllerDays.size() / (double) days) * 100.0;

        // Calculate Symptom Burden (days with symptom level > 1)
        Map<Long, Boolean> symptomDays = new HashMap<>();
        for (SymptomCheckIn checkIn : symptomCheckIns) {
            if (checkIn.getSymptomLevel() > 1) {
                symptomDays.put(normalizeDate(checkIn.getDate()), true);
            }
        }
        int symptomBurdenDays = symptomDays.size();

        // Calculate Zone Distribution
        int green = 0, yellow = 0, red = 0;
        for (PEFReading reading : pefReadings) {
            if ("green".equalsIgnoreCase(reading.getZone())) green++;
            else if ("yellow".equalsIgnoreCase(reading.getZone())) yellow++;
            else if ("red".equalsIgnoreCase(reading.getZone())) red++;
        }

        // Count Triage Incidents
        int triageIncidents = 0;
        for (TriageSession session : triageSessions) {
            if ("emergency".equalsIgnoreCase(session.getDecision()) || session.isEscalated()) {
                triageIncidents++;
            }
        }

        return new Report(
                userId,
                days,
                System.currentTimeMillis(),
                startDate.getTime(),
                endDate.getTime(),
                totalRescue,
                totalController,
                avgRescue,
                avgController,
                controllerAdherence,
                symptomBurdenDays,
                green,
                yellow,
                red,
                triageIncidents,
                includeTriage,
                includeRescue,
                includeController,
                includeSymptoms,
                includeZones,
                includeDailyLogs,
                includeTriggerChart
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
                        fetchOptionalDataForPdf(userId, startDate, endDate, report, rescueLogs, controllerLogs, action);
                    }

                    @Override
                    public void onFailure(String error) {
                        dismissLoading();
                        Toast.makeText(context, "Failed to load controller data", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                dismissLoading();
                Toast.makeText(context, "Failed to load rescue data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOptionalDataForPdf(String userId, Date startDate, Date endDate, Report report,
                                         List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                         ReportAction action) {
        triageRepository.getTriageSessions(userId, new TriageRepository.LoadCallback<List<TriageSession>>() {
            @Override
            public void onSuccess(List<TriageSession> allTriageSessions) {
                fetchPEFForPdf(userId, startDate, endDate, report, rescueLogs, controllerLogs, allTriageSessions, action);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Triage fetch failed: " + error);
                fetchPEFForPdf(userId, startDate, endDate, report, rescueLogs, controllerLogs, new ArrayList<>(), action);
            }
        });
    }

    private void fetchPEFForPdf(String userId, Date startDate, Date endDate, Report report,
                                List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                List<TriageSession> triageSessions, ReportAction action) {
        pefRepository.getPEFReadingsForUser(userId, new PEFRepository.LoadCallback<List<PEFReading>>() {
            @Override
            public void onSuccess(List<PEFReading> allPefReadings) {
                fetchSymptomsForPdf(userId, startDate, endDate, report, rescueLogs, controllerLogs, triageSessions, allPefReadings, action);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "PEF fetch failed: " + error);
                fetchSymptomsForPdf(userId, startDate, endDate, report, rescueLogs, controllerLogs, triageSessions, new ArrayList<>(), action);
            }
        });
    }

    private void fetchSymptomsForPdf(String userId, Date startDate, Date endDate, Report report,
                                     List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                     List<TriageSession> triageSessions, List<PEFReading> pefReadings,
                                     ReportAction action) {
        symptomRepository.getCheckInsForUser(userId, new SymptomCheckInRepository.LoadCallback() {
            @Override
            public void onSuccess(List<SymptomCheckIn> allSymptomCheckIns) {
                finalizePdfGeneration(startDate, endDate, report, rescueLogs, controllerLogs, triageSessions, pefReadings, allSymptomCheckIns, action);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Symptom fetch failed: " + error);
                finalizePdfGeneration(startDate, endDate, report, rescueLogs, controllerLogs, triageSessions, pefReadings, new ArrayList<>(), action);
            }
        });
    }

    private void finalizePdfGeneration(Date startDate, Date endDate, Report report,
                                       List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                       List<TriageSession> allTriageSessions, List<PEFReading> allPefReadings,
                                       List<SymptomCheckIn> allSymptomCheckIns, ReportAction action) {
        List<TriageSession> triageSessions = filterByDate(allTriageSessions, startDate, endDate);
        List<PEFReading> pefReadings = filterByDate(allPefReadings, startDate, endDate);
        List<SymptomCheckIn> symptomCheckIns = filterByDate(allSymptomCheckIns, startDate, endDate);

        generatePDF(report, rescueLogs, controllerLogs, triageSessions, pefReadings, symptomCheckIns, action);
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

    private void generatePDF(Report report, List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                             List<TriageSession> triageSessions, List<PEFReading> pefReadings, List<SymptomCheckIn> symptomCheckIns,
                             ReportAction action) {
        Log.d(TAG, "generatePDF: Starting generation");
        
        String htmlContent = buildReportHtml(report, rescueLogs, controllerLogs, triageSessions, pefReadings, symptomCheckIns);
        Log.d(TAG, "generatePDF: HTML generated, length: " + htmlContent.length());

        String fileName = "AsthmaReport_" + report.getDays() + "days_" + System.currentTimeMillis() + ".pdf";
        // Use cache dir for temporary storage before sharing/moving
        File file = new File(context.getCacheDir(), fileName);
        Log.d(TAG, "generatePDF: Output file path: " + file.getAbsolutePath());
        renderHtmlToPdf(htmlContent, file, action);
    }

    private String buildReportHtml(Report report, List<RescueInhalerLog> rescueLogs, List<ControllerMedicineLog> controllerLogs,
                                   List<TriageSession> triageSessions, List<PEFReading> pefReadings, List<SymptomCheckIn> symptomCheckIns) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String dateRange = sdf.format(new Date(report.getStartDate())) + " - " + sdf.format(new Date(report.getEndDate()));
        String patientName = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "Patient";
        
        StringBuilder metricsHtml = new StringBuilder("<div class='metrics-grid'>");
        if (report.isIncludeRescue()) {
            metricsHtml.append("<div class='card'><h3>Avg Rescue/Day</h3><div class='val'>")
                       .append(String.format(Locale.US, "%.1f", report.getAvgRescuePerDay()))
                       .append("</div></div>");
        }
        if (report.isIncludeController()) {
            metricsHtml.append("<div class='card'><h3>Controller Adherence</h3><div class='val'>")
                       .append(String.format(Locale.US, "%.0f%%", report.getControllerAdherence()))
                       .append("</div></div>");
        }
        if (report.isIncludeSymptoms()) {
            metricsHtml.append("<div class='card'><h3>Symptom Burden</h3><div class='val'>")
                       .append(report.getSymptomBurdenDays())
                       .append(" days</div></div>");
        }
        metricsHtml.append("</div>");

        StringBuilder chartsHtml = new StringBuilder("<div class='charts'>");
        if (report.isIncludeRescue()) {
            chartsHtml.append("<div class='chart-box'><h2>Rescue Inhaler Trends</h2><canvas id='rescueChart'></canvas></div>");
        }
        if (report.isIncludeController()) {
            chartsHtml.append("<div class='chart-box'><h2>Controller Medicine Trends</h2><canvas id='controllerChart'></canvas></div>");
        }
        if (report.isIncludeZones()) {
            chartsHtml.append("<div class='chart-box'><h2>Zone Distribution</h2><div class='pie-container'><canvas id='zoneChart'></canvas></div></div>");
        }
        if (report.isIncludeTriggerChart()) {
            chartsHtml.append("<div class='chart-box'><h2>Trigger Trends</h2><canvas id='triggerChart'></canvas></div>");
        }
        chartsHtml.append("</div>");

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
                currentMonth = monthName;
                currentMonthStart = i;
            }

            String rStyle = rCount > 4 ? "color: #dc3545; font-weight: bold;" : "";
            
            rows.append("<tr>")
                .append("<td>").append(rowDateFmt.format(calendar.getTime())).append("</td>");
            
            if (report.isIncludeRescue()) {
                rows.append("<td style='").append(rStyle).append("'>").append(rCount).append("</td>");
            }
            if (report.isIncludeController()) {
                rows.append("<td>").append(cCount).append("</td>");
            }
            
            rows.append("</tr>");
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Triage Incidents Table
        StringBuilder triageRows = new StringBuilder();
        SimpleDateFormat triageDateFmt = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        boolean hasTriageIncidents = false;
        
        if (report.isIncludeTriage()) {
            for (TriageSession session : triageSessions) {
                if ("emergency".equalsIgnoreCase(session.getDecision()) || session.isEscalated()) {
                    hasTriageIncidents = true;
                    String reason = session.getEscalationReason() != null ? session.getEscalationReason() : session.getDecision();
                    triageRows.append("<tr>")
                        .append("<td>").append(triageDateFmt.format(session.getStartTime())).append("</td>")
                        .append("<td>").append(reason).append("</td>")
                        .append("<td>").append(session.getUserResponse() != null ? session.getUserResponse() : "N/A").append("</td>")
                        .append("</tr>");
                }
            }
        }
        
        String triageTableHtml = "";
        if (hasTriageIncidents) {
            triageTableHtml = "<div class='chart-box triage-incidents'><h2>Notable Triage Incidents</h2>" +
                    "<table><thead><tr><th>Date/Time</th><th>Reason</th><th>Outcome</th></tr></thead>" +
                    "<tbody>" + triageRows.toString() + "</tbody></table></div>";
        }

        // Daily Logs Table
        StringBuilder dailyLogsHtml = new StringBuilder();
        if (report.isIncludeDailyLogs()) {
            SimpleDateFormat logDateFmt = new SimpleDateFormat("MMM d", Locale.getDefault());
            SimpleDateFormat logTimeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            dailyLogsHtml.append("<div class='chart-box daily-logs'><h2>Daily Symptom Logs</h2>")
                         .append("<table><thead><tr><th>Date</th><th>Time</th><th>Level</th><th>Triggers</th><th>Notes</th></tr></thead><tbody>");
            
            List<SymptomCheckIn> sortedCheckIns = new ArrayList<>(symptomCheckIns);
            Collections.sort(sortedCheckIns, (a, b) -> b.getDate().compareTo(a.getDate()));
            
            for (SymptomCheckIn checkIn : sortedCheckIns) {
                String triggers = checkIn.getTriggers() != null ? TextUtils.join(", ", checkIn.getTriggers()) : "-";
                String notes = checkIn.getNotes() != null ? checkIn.getNotes() : "";
                
                dailyLogsHtml.append("<tr>")
                             .append("<td>").append(logDateFmt.format(checkIn.getDate())).append("</td>")
                             .append("<td>").append(logTimeFmt.format(checkIn.getDate())).append("</td>")
                             .append("<td>").append(checkIn.getSymptomLevel()).append("</td>")
                             .append("<td>").append(triggers).append("</td>")
                             .append("<td>").append(notes).append("</td>")
                             .append("</tr>");
            }
            dailyLogsHtml.append("</tbody></table></div>");
        }

        // Month ranges JS
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

        // Zone Data for Pie Chart
        int green = report.getGreenZoneCount();
        int yellow = report.getYellowZoneCount();
        int red = report.getRedZoneCount();
        String zoneDataJs = "[" + green + "," + yellow + "," + red + "]";

        // Trigger Chart Data
        String triggerChartJs = "";
        if (report.isIncludeTriggerChart()) {
            java.util.Set<String> uniqueTriggers = new java.util.HashSet<>();
            for (SymptomCheckIn checkIn : symptomCheckIns) {
                if (checkIn.getTriggers() != null) {
                    uniqueTriggers.addAll(checkIn.getTriggers());
                }
            }
            
            Map<String, int[]> triggerCounts = new HashMap<>();
            for (String trigger : uniqueTriggers) {
                triggerCounts.put(trigger, new int[report.getDays()]);
            }
            
            long startMillis = normalizeDate(new Date(report.getStartDate()));
            for (SymptomCheckIn checkIn : symptomCheckIns) {
                long checkInDate = normalizeDate(checkIn.getDate());
                int dayIndex = (int) ((checkInDate - startMillis) / (24 * 60 * 60 * 1000));
                
                if (dayIndex >= 0 && dayIndex < report.getDays() && checkIn.getTriggers() != null) {
                    for (String trigger : checkIn.getTriggers()) {
                        if (triggerCounts.containsKey(trigger)) {
                            triggerCounts.get(trigger)[dayIndex]++;
                        }
                    }
                }
            }
            
            StringBuilder datasets = new StringBuilder("[");
            String[] colors = {"#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40", "#C9CBCF"};
            int colorIdx = 0;
            
            for (Map.Entry<String, int[]> entry : triggerCounts.entrySet()) {
                datasets.append("{label:'").append(entry.getKey()).append("',data:[");
                for (int i = 0; i < entry.getValue().length; i++) {
                    datasets.append(entry.getValue()[i]).append(i < entry.getValue().length - 1 ? "," : "");
                }
                datasets.append("],borderColor:'").append(colors[colorIdx % colors.length]).append("',fill:false,tension:0.1},");
                colorIdx++;
            }
            if (datasets.length() > 1) datasets.setLength(datasets.length() - 1);
            datasets.append("]");
            
            triggerChartJs = "if (document.getElementById('triggerChart')) new Chart(document.getElementById('triggerChart'), { type: 'line', data: { labels: " + labelsJs + ", datasets: " + datasets + " }, options: commonOptions, plugins: [monthLabelPlugin] });";
        }

        int tickFontSize = report.getDays() > 30 ? 8 : 10;
        
        StringBuilder tableHeader = new StringBuilder("<tr><th>Date</th>");
        if (report.isIncludeRescue()) tableHeader.append("<th>Rescue Puffs</th>");
        if (report.isIncludeController()) tableHeader.append("<th>Controller Doses</th>");
        tableHeader.append("</tr>");

        String dailyBreakdownHtml = "";
        if (report.isIncludeRescue() || report.isIncludeController()) {
            dailyBreakdownHtml = "<div class='chart-box daily-breakdown'>" +
                "<h2>Daily Breakdown</h2>" +
                "<table><thead>" + tableHeader.toString() + "</thead>" +
                "<tbody>" + rows.toString() + "</tbody></table>" +
                "</div>";
        }

        return "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<script src='https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js'></script>" +
                "<style>" +
                "body { font-family: Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 40px 40px 0 40px; color: #333; }" +
                ".header { text-align: center; margin-bottom: 40px; border-bottom: 3px solid #4F46E5; padding-bottom: 20px; }" +
                ".header h1 { color: #4F46E5; margin: 0; font-size: 28px; }" +
                ".meta { color: #666; margin-top: 10px; font-size: 14px; }" +
                ".metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 40px; }" +
                ".card { background: #F3F4F6; padding: 15px; border-radius: 10px; text-align: center; }" +
                ".card h3 { margin: 0 0 5px; font-size: 11px; text-transform: uppercase; color: #6B7280; letter-spacing: 1px; }" +
                ".card .val { font-size: 20px; font-weight: bold; color: #111827; }" +
                ".charts { margin-bottom: 40px; }" +
                ".chart-box { margin-bottom: 30px; background: white; border: 1px solid #E5E7EB; border-radius: 8px; padding: 15px; page-break-inside: avoid; }" +
                ".chart-box h2 { font-size: 18px; color: #111827; border-left: 4px solid #4F46E5; padding-left: 10px; margin-bottom: 15px; }" +
                "canvas { width: 100% !important; height: 250px !important; }" +
                ".pie-container { height: 300px !important; display: flex; justify-content: center; }" +
                ".pie-container canvas { width: auto !important; height: 100% !important; }" +
                "table { width: 100%; border-collapse: collapse; font-size: 13px; }" +
                "th { text-align: left; padding: 12px; background: #F9FAFB; color: #6B7280; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; border-bottom: 1px solid #E5E7EB; }" +
                "td { padding: 12px; border-bottom: 1px solid #E5E7EB; }" +
                "tr:nth-child(even) { background: #F9FAFB; }" +
                "</style></head><body>" +
                "<div class='header'>" +
                "<h1>Asthma Control Report</h1>" +
                "<div class='meta'>" + patientName + " &bull; " + dateRange + "</div>" +
                "</div>" +
                metricsHtml.toString() +
                chartsHtml.toString() +
                triageTableHtml +
                dailyLogsHtml.toString() +
                dailyBreakdownHtml +
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
                "  if (document.getElementById('rescueChart')) new Chart(document.getElementById('rescueChart'), { type: 'bar', data: { labels: " + labelsJs + ", datasets: [{ label: 'Puffs', data: " + rescueJs + ", backgroundColor: '#3B82F6', borderRadius: 4 }] }, options: commonOptions, plugins: [monthLabelPlugin] });" +
                "  if (document.getElementById('controllerChart')) new Chart(document.getElementById('controllerChart'), { type: 'bar', data: { labels: " + labelsJs + ", datasets: [{ label: 'Doses', data: " + controllerJs + ", backgroundColor: '#10B981', borderRadius: 4 }] }, options: commonOptions, plugins: [monthLabelPlugin] });" +
                "  if (document.getElementById('zoneChart')) new Chart(document.getElementById('zoneChart'), { type: 'pie', data: { labels: ['Green', 'Yellow', 'Red'], datasets: [{ data: " + zoneDataJs + ", backgroundColor: ['#4CAF50', '#FFC107', '#F44336'] }] }, options: { animation: false, responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } } });" +
                triggerChartJs +
                "  setTimeout(applyPagination, 100);" +
                "} catch(e) {" +
                "  console.error('Chart Init Error:', e);" +
                "}" +
                "function applyPagination() {" +
                "  const density = window.devicePixelRatio || 1;" +
                "  const pageHeightPx = 1754;" +
                "  const pageHeight = pageHeightPx / density;" +
                "  const elements = Array.from(document.querySelectorAll('.header, .metrics-grid, .chart-box:not(.daily-breakdown):not(.daily-logs), .daily-breakdown h2, .daily-logs h2, tr'));" +
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
                "      const spacerHeight = nextPageStart - top + 40;" +
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
                "      } else if (el.tagName === 'H2' && (el.closest('.daily-breakdown') || el.closest('.daily-logs'))) {" +
                "        const container = el.closest('.daily-breakdown') || el.closest('.daily-logs');" +
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
                            generatePdfPages(view, pageWidth, pageHeight, finalContentHeight, outputFile, action);
                        }, 250); // Short delay for resize to take effect
                    }, 1500); // 1.5 seconds delay for JS charts
                }
            });
            
            Log.d(TAG, "renderHtmlToPdf: Loading HTML data");
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        });
    }

    private void generatePdfPages(WebView view, int pageWidth, int pageHeight, int contentHeight, File outputFile, ReportAction action) {
        Log.d(TAG, "generatePdfPages: Starting PDF capture. Content Height: " + contentHeight);
        PdfDocument document = new PdfDocument();
        int totalPages = (int) Math.ceil((double) contentHeight / pageHeight);
        Log.d(TAG, "generatePdfPages: Total pages to generate: " + totalPages);
        
        generatePageRecursive(document, view, 0, totalPages, pageWidth, pageHeight, outputFile, action);
    }

    private void generatePageRecursive(PdfDocument document, WebView view, int pageIndex, int totalPages, int pageWidth, int pageHeight, File outputFile, ReportAction action) {
        if (pageIndex >= totalPages) {
            savePdfInBackground(document, outputFile, action);
            return;
        }

        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            canvas.save();
            canvas.translate(0, -pageIndex * pageHeight);
            canvas.clipRect(0, pageIndex * pageHeight, pageWidth, (pageIndex + 1) * pageHeight);
            
            view.draw(canvas);
            canvas.restore();
            
            document.finishPage(page);
        } catch (Exception e) {
            Log.e(TAG, "Error drawing page " + pageIndex, e);
        }

        // Schedule next page with a small delay to let UI thread breathe
        new Handler(Looper.getMainLooper()).postDelayed(() -> 
            generatePageRecursive(document, view, pageIndex + 1, totalPages, pageWidth, pageHeight, outputFile, action),
            10 // 10ms delay between pages
        );
    }

    private void savePdfInBackground(PdfDocument document, File outputFile, ReportAction action) {
        Log.d(TAG, "savePdfInBackground: Saving PDF to disk...");
        new Thread(() -> {
            try {
                FileOutputStream fos = new FileOutputStream(outputFile);
                document.writeTo(fos);
                fos.close();
                Log.d(TAG, "savePdfInBackground: Document written successfully");
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (action == ReportAction.SHARE) {
                        sharePDF(outputFile);
                    } else {
                        saveToPublicDocuments(outputFile, outputFile.getName());
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Error saving PDF", e);
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
                );
            } finally {
                document.close();
                Log.d(TAG, "savePdfInBackground: Document closed");
                new Handler(Looper.getMainLooper()).post(this::dismissLoading);
            }
        }).start();
    }

    private void saveToPublicDocuments(File tempFile, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

            ContentResolver resolver = context.getContentResolver();
            Uri uri = null;

            try {
                uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    try (OutputStream out = resolver.openOutputStream(uri);
                         FileInputStream in = new FileInputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        Toast.makeText(context, "Report saved to Documents", Toast.LENGTH_LONG).show();
                    }
                } else {
                    throw new IOException("Failed to create MediaStore entry");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error saving to public documents", e);
                Toast.makeText(context, "Error saving to Documents folder", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Legacy storage
            File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!publicDir.exists()) {
                publicDir.mkdirs();
            }
            File destFile = new File(publicDir, fileName);
            try (FileInputStream in = new FileInputStream(tempFile);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                Toast.makeText(context, "Report saved to Documents", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.e(TAG, "Error saving to legacy public documents", e);
                Toast.makeText(context, "Error saving to Documents folder", Toast.LENGTH_SHORT).show();
            }
        }
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
