package com.example.SmartAirGroup2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ProviderReportGenerator {

    private Context context;
    private Activity activity;
    private String uname;
    private String name;
    private DatabaseReference database;
    private PdfDocument pendingPdfDocument;

    public static final int CREATE_PDF_REQUEST_CODE = 2001;

    public ProviderReportGenerator(Context context, Activity activity, String uname, String name) {
        this.context = context;
        this.activity = activity;
        this.uname = uname;
        this.name = name;
        this.database = FirebaseDatabase.getInstance().getReference();
    }

    public void generateThreeMonthReport() {
        queryLogsAndGeneratePDF(3);
    }

    public void generateSixMonthReport() {
        queryLogsAndGeneratePDF(6);
    }

    private void queryLogsAndGeneratePDF(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);
        long startTime = cal.getTimeInMillis();

        // Calculate total days in timeframe
        int totalDays = months * 30; // Approximate

        final ReportData reportData = new ReportData();
        reportData.months = months;
        reportData.totalDays = totalDays;

        final CountDownLatch latch = new CountDownLatch(2);

        // Query rescue_log for rescue frequency (count of entries)
        DatabaseReference rescueRef = database
                .child("categories/users/children")
                .child(uname)
                .child("data/logs/rescue_log");

        rescueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int rescueCount = 0;
                Map<String, Integer> monthlyRescue = new HashMap<>();

                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    String dateTime = logSnapshot.child("dateTime").getValue(String.class);

                    if (dateTime != null) {
                        long logTime = convertDateTimeToLong(dateTime);

                        if (logTime >= startTime) {
                            rescueCount++;

                            // Extract month for breakdown
                            String month = dateTime.substring(0, 7);
                            monthlyRescue.put(month,
                                    monthlyRescue.getOrDefault(month, 0) + 1);
                        }
                    }
                }

                reportData.rescueFrequency = rescueCount;
                reportData.monthlyRescue = monthlyRescue;
                latch.countDown();
                checkAndGeneratePDF(latch, reportData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error loading rescue data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Query controller_log for adherence (count of unique days)
        DatabaseReference controllerRef = database
                .child("categories/users/children")
                .child(uname)
                .child("data/logs/controller_log");

        controllerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> uniqueDays = new HashSet<>();
                Map<String, Integer> monthlyAdherence = new HashMap<>();

                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    String dateTime = logSnapshot.child("dateTime").getValue(String.class);

                    if (dateTime != null) {
                        long logTime = convertDateTimeToLong(dateTime);

                        if (logTime >= startTime) {
                            // Extract just the date (yyyy-MM-dd)
                            String date = dateTime.substring(0, 10);
                            uniqueDays.add(date);

                            // Extract month for breakdown
                            String month = dateTime.substring(0, 7);

                            // Count unique days per month
                            if (!monthlyAdherence.containsKey(month)) {
                                monthlyAdherence.put(month, 0);
                            }
                        }
                    }
                }

                // Calculate adherence percentage
                int daysWithController = uniqueDays.size();
                double adherencePercentage = (daysWithController / (double) totalDays) * 100;

                reportData.controllerDays = daysWithController;
                reportData.adherencePercentage = adherencePercentage;
                reportData.monthlyAdherence = monthlyAdherence;

                latch.countDown();
                checkAndGeneratePDF(latch, reportData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error loading controller data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndGeneratePDF(CountDownLatch latch, ReportData reportData) {
        if (latch.getCount() == 0) {
            // Both queries completed, generate PDF on main thread
            activity.runOnUiThread(() -> createPDF(reportData));
        }
    }

    private void createPDF(ReportData data) {
        try {
            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);

            final int[] pageNumber = {1};
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber[0]).create();
            final PdfDocument.Page[] page = {pdfDocument.startPage(pageInfo)};
            final Canvas[] canvas = {page[0].getCanvas()};
            final int[] y = {60};
            int lineSpacing = 22;

            // Helper to create a new page
            Runnable newPage = () -> {
                pageNumber[0]++;
                PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber[0]).create();
                PdfDocument.Page newPg = pdfDocument.startPage(newPageInfo);
                page[0] = newPg;
                canvas[0] = page[0].getCanvas();
                y[0] = 60;
                paint.setTextSize(14);
            };

            java.util.function.Consumer<String> drawLine = (text) -> {
                if (y[0] > 780) {
                    pageNumber[0]++;
                    PdfDocument.PageInfo newPageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber[0]).create();
                    page[0] = pdfDocument.startPage(newPageInfo);
                    canvas[0] = page[0].getCanvas();
                    y[0] = 60;
                    paint.setTextSize(14);
                }
                canvas[0].drawText(text, 50, y[0], paint);
                y[0] += lineSpacing;
            };

            // ---------- Write content ----------
            paint.setTextSize(20);
            drawLine.accept("Provider Report - " + name);
            y[0] += 10;

            paint.setTextSize(14);
            drawLine.accept("Report Period: " + data.months + " months");
            y[0] += 10;

            // Rescue
            paint.setTextSize(16);
            drawLine.accept("Rescue Medication Use");
            paint.setTextSize(14);
            drawLine.accept("- Total rescue events: " + data.rescueFrequency);
            drawLine.accept("- Average per month: " +
                    String.format(Locale.getDefault(), "%.1f", data.rescueFrequency / (double) data.months));
            y[0] += 10;

            // Controller
            paint.setTextSize(16);
            drawLine.accept("Controller Medication Adherence");
            paint.setTextSize(14);
            drawLine.accept("- Days with controller use: " + data.controllerDays + " / " + data.totalDays);
            drawLine.accept("- Adherence rate: " +
                    String.format(Locale.getDefault(), "%.1f%%", data.adherencePercentage));
            y[0] += 10;

            // Monthly breakdown
            paint.setTextSize(16);
            drawLine.accept("Monthly Breakdown");
            paint.setTextSize(14);
            List<String> sortedMonths = new ArrayList<>(data.monthlyRescue.keySet());
            Collections.sort(sortedMonths);
            for (String month : sortedMonths) {
                int rescueCount = data.monthlyRescue.getOrDefault(month, 0);
                drawLine.accept(month + " - Rescue events: " + rescueCount);
            }

            // Finish last page
            pdfDocument.finishPage(page[0]);

            // Store PDF and show save dialog
            this.pendingPdfDocument = pdfDocument;
            String fileName = "Provider_Report_" +
                    name.replaceAll("[^a-zA-Z0-9]", "_") +
                    "_" + data.months + "mo_" +
                    new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".pdf";

            showSaveAsDialog("application/pdf", fileName, CREATE_PDF_REQUEST_CODE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "PDF generation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (pendingPdfDocument != null) {
                pendingPdfDocument.close();
                pendingPdfDocument = null;
            }
        }
    }



    private void showSaveAsDialog(String mimeType, String suggestedName, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        activity.startActivityForResult(intent, requestCode);
    }

    // Call this from your Fragment's onActivityResult
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREATE_PDF_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null && pendingPdfDocument != null) {
                savePdfToUri(uri);
            }
        }
    }

    private void savePdfToUri(Uri uri) {
        if (pendingPdfDocument == null) {
            Toast.makeText(context, "PDF document is null", Toast.LENGTH_SHORT).show();
            return;
        }

        OutputStream out = null;
        try {
            out = context.getContentResolver().openOutputStream(uri);
            if (out != null) {
                pendingPdfDocument.writeTo(out); // Write raw bytes
                Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Failed to open output stream", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {}

            pendingPdfDocument.close(); // Close after writing
            pendingPdfDocument = null;
        }
    }



    private long convertDateTimeToLong(String dateTimeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = sdf.parse(dateTimeStr);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Data class to hold report information
    private static class ReportData {
        int months;
        int totalDays;
        int rescueFrequency;
        int controllerDays;
        double adherencePercentage;
        Map<String, Integer> monthlyRescue = new HashMap<>();
        Map<String, Integer> monthlyAdherence = new HashMap<>();
    }
}