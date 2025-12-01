package com.example.SmartAirGroup2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class ProviderReport extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS & CONSTANTS
    // ───────────────────────────────
    private String name, uname, startDate;
    private Button export3Months, export6Months;
    private View view;
    private Toolbar toolbar;

    private ProviderReportHelper helper;

    private static final int CREATE_PDF_REQUEST_CODE = 2001;

    // ───────────────────────────────
    // LIFECYCLE METHODS
    // ───────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
        helper = new ProviderReportHelper(uname, name);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        view = inflater.inflate(R.layout.fragment_provider_report, container, false);

        export3Months = view.findViewById(R.id.ThreeMonthsButton);
        export6Months = view.findViewById(R.id.SixMonthsButton);

        export3Months.setOnClickListener(v -> {
            startDate = helper.reverseDate(helper.today(), 3);
            showSaveAsDialog("application/pdf", "Provider_Report_3_Months.pdf", CREATE_PDF_REQUEST_CODE);
        });

        export6Months.setOnClickListener(v -> {
            startDate = helper.reverseDate(helper.today(), 6);
            showSaveAsDialog("application/pdf", "Provider_Report_6_Months.pdf", CREATE_PDF_REQUEST_CODE);
        });

        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(name +"'s Provider Report");

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    // ───────────────────────────────
    // DOCUMENT EXPORT
    // ───────────────────────────────

    private void showSaveAsDialog(String mimeType, String suggestedName, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        startActivityForResult(intent, requestCode);
    }

    private void exportPdfToUri(Uri uri) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(16);
        int[] y = {50};

        canvas.drawText("Provider Report:", 50, y[0], paint);
        y[0] += 50;

        // Step 1-4: Rescue, Controller, Symptom, Average PEF (same as before)
        helper.countRescueFrequency(startDate, rescueFreq -> {
            canvas.drawText("Rescue Frequency: " + String.format("%.2f", rescueFreq) + " per day", 50, y[0], paint);
            y[0] += 30;

            helper.controllerAdherence(startDate, adherence -> {
                canvas.drawText("Controller Adherence: " + String.format("%.2f", adherence) + "%", 50, y[0], paint);

                // Draw pie chart below the text
                drawAdherencePieChart(canvas, paint, adherence, 400, y[0] - 70, 100);

                y[0] += 30; // space for pie chart

                helper.countUniqueSymptomDays(startDate, symptomDays -> {
                    canvas.drawText("Symptom Burden: " + symptomDays + " days", 50, y[0], paint);
                    y[0] += 30;

                    helper.averagePEF(startDate, avgPEF -> {
                        canvas.drawText("Average PEF: " + String.format("%.2f", avgPEF), 50, y[0], paint);
                        y[0] += 50;

                        // Step 5: Plot PEF graph
                        helper.dailyAveragePEF(startDate, dailyAvgMap -> {
                            plotPEFWithPB(canvas, paint, startDate, 60, y[0], 500, 200, () -> {
                                y[0] += 300;

                                // NOW check triage AFTER graph is done
                                helper.checkPermission(hasPermission -> {
                                    if (!hasPermission) {
                                        canvas.drawText("Triage Incidents: no permission", 50, y[0], paint);
                                        y[0] += 30;
                                        finishPdf(pdfDocument, page, uri);
                                    } else {
                                        helper.triageIncidents(startDate, triages -> {
                                            if (triages.isEmpty()) {
                                                canvas.drawText("Triage Incidents: none", 50, y[0], paint);
                                                y[0] += 30;
                                            } else {
                                                canvas.drawText("Triage Incidents:", 50, y[0], paint);
                                                y[0] += 30;
                                                for (Map.Entry<String, ProviderReportHelper.TriageIncident> entry : triages.entrySet()) {
                                                    ProviderReportHelper.TriageIncident t = entry.getValue();
                                                    canvas.drawText("Time: " + t.time, 70, y[0], paint); y[0] += 20;
                                                    if (t.pef != null) { canvas.drawText("PEF: " + t.pef, 70, y[0], paint); y[0] += 20; }
                                                    if (t.guidance != null) { canvas.drawText("Guidance: " + t.guidance, 70, y[0], paint); y[0] += 20; }
                                                    if (t.response != null) { canvas.drawText("Response: " + t.response, 70, y[0], paint); y[0] += 20; }
                                                    if (!t.redflags.isEmpty()) {
                                                        canvas.drawText("Red Flags:", 70, y[0], paint); y[0] += 20;
                                                        for (Map.Entry<String, Boolean> flag : t.redflags.entrySet()) {
                                                            if (flag.getValue()) { canvas.drawText(" - " + flag.getKey(), 90, y[0], paint); y[0] += 20; }
                                                        }
                                                    }
                                                    y[0] += 10;
                                                }
                                            }
                                            finishPdf(pdfDocument, page, uri);
                                        });
                                    }
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    // New helper to fetch PB and then plot PEF
    private void plotPEFWithPB(Canvas canvas, Paint paint, String startDate, int x, int y, int width, int height, Runnable callback) {
        DatabaseReference pbRef = FirebaseDatabase.getInstance().getReference()
                .child("categories").child("users").child("children").child(uname).child("data").child("pb");

        pbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double PB = snapshot.getValue(Double.class);
                if (PB == null || PB <= 0) PB = 1.0; // default PB if missing

                // Fetch daily average PEF
                Double finalPB = PB;
                helper.dailyAveragePEF(startDate, dailyAvgMap -> {
                    drawPEFGraphWithPB(canvas, paint, dailyAvgMap, finalPB, startDate, x, y, width, height);
                    if (callback != null) callback.run();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.run();
            }
        });
    }

    // Draw line chart with PB-based coloring
    private void drawPEFGraphWithPB(Canvas canvas, Paint paint, Map<String, Double> dailyAvgMap, double PB,
                                    String startDate, int x, int y, int width, int height) {
        // Legend at top
        int legendX = x;
        int legendY = y;
        int blockSize = 20;
        int spacing = 10;
        Paint blockPaint = new Paint();

        // GREEN >= 80% PB
        blockPaint.setColor(Color.GREEN);
        canvas.drawRect(legendX, legendY, legendX + blockSize, legendY + blockSize, blockPaint);
        canvas.drawText(">= 80% PB", legendX + blockSize + spacing, legendY + blockSize - 5, paint);

        // YELLOW 50-80% PB
        blockPaint.setColor(Color.YELLOW);
        canvas.drawRect(legendX + 120, legendY, legendX + 120 + blockSize, legendY + blockSize, blockPaint);
        canvas.drawText("50% - 80% PB", legendX + 120 + blockSize + spacing, legendY + blockSize - 5, paint);

        // RED < 50% PB
        blockPaint.setColor(Color.RED);
        canvas.drawRect(legendX + 280, legendY, legendX + 280 + blockSize, legendY + blockSize, blockPaint);
        canvas.drawText("< 50% PB", legendX + 280 + blockSize + spacing, legendY + blockSize - 5, paint);

        // Move y down past legend
        y += blockSize + spacing * 2;

        // Prepare data arrays
        long startMillis = helper.convertDateToLong(startDate);
        long endMillis = helper.convertDateToLong(helper.today());
        int totalDays = (int) ((endMillis - startMillis) / (1000*60*60*24)) + 1;

        double[] values = new double[totalDays];
        String[] dates = new String[totalDays];
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        for (int i = 0; i < totalDays; i++) {
            String dayStr = sdf.format(cal.getTime());
            dates[i] = dayStr;
            values[i] = dailyAvgMap.getOrDefault(dayStr, Double.NaN);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Y-axis scaling relative to PB
        double graphMin = 0;
        double maxVal = PB * 1.1; // max 10% above PB
        for (double v : values) {
            if (!Double.isNaN(v) && v > maxVal) maxVal = v * 1.1;
        }

        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2);

        // Draw axes
        canvas.drawLine(x, y + height, x + width, y + height, axisPaint); // X-axis
        canvas.drawLine(x, y, x, y + height, axisPaint); // Y-axis

        // Draw horizontal PB guide lines
        Paint guidePaint = new Paint();
        guidePaint.setColor(Color.GRAY);
        guidePaint.setStrokeWidth(1);
        guidePaint.setStyle(Paint.Style.STROKE);

        // 0.5 PB line
        float halfPBY = y + height - (float) ((0.5 * PB - graphMin) / (maxVal - graphMin) * height);
        canvas.drawLine(x, halfPBY, x + width, halfPBY, guidePaint);
        canvas.drawText("0.5 PB", x - 50, halfPBY + 5, paint);

        // 0.8 PB line
        float eightyPBY = y + height - (float) ((0.8 * PB - graphMin) / (maxVal - graphMin) * height);
        canvas.drawLine(x, eightyPBY, x + width, eightyPBY, guidePaint);
        canvas.drawText("0.8 PB", x - 50, eightyPBY + 5, paint);

        // Plot points
        if (!dailyAvgMap.isEmpty()){
            Paint linePaint = new Paint();
            linePaint.setStrokeWidth(3);

            for (int i = 0; i < totalDays; i++) {
                if (Double.isNaN(values[i])) continue;

                float px = x + (i * (float) width / (totalDays - 1));
                float py = y + height - (float) ((values[i] - graphMin) / (maxVal - graphMin) * height);

                // Color by PB thresholds
                if (values[i] >= 0.8 * PB) linePaint.setColor(Color.GREEN);
                else if (values[i] >= 0.5 * PB) linePaint.setColor(Color.YELLOW);
                else linePaint.setColor(Color.RED);

                canvas.drawCircle(px, py, 6, linePaint);

                if (i > 0 && !Double.isNaN(values[i-1])) {
                    float prevPy = y + height - (float) ((values[i-1] - graphMin) / (maxVal - graphMin) * height);
                    float prevPx = x + ((i-1) * (float) width / (totalDays - 1));
                    canvas.drawLine(prevPx, prevPy, px, py, linePaint);
                }
            }
        }
        else{
            paint.setColor(Color.RED);
            paint.setTextSize(16);
            canvas.drawText("No PEF data available.", x + 120, y + height/2, paint);
        }

        // X-axis label
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        canvas.drawText("PEF zone over time", x + 200, y - 5, paint);

        // Optional X-axis tick labels every month
        int tickStep = Math.max(1, totalDays / 3);
        for (int i = 0; i < totalDays; i += tickStep) {
            canvas.drawText(dates[i], x + (i * (float) width / (totalDays - 1)), y + height + 15, paint);
        }
        paint.setTextSize(16);
        paint.setColor(Color.BLACK);
    }

    private void drawAdherencePieChart(Canvas canvas, Paint paint, Double adherencePercent, int x, int y, int size) {
        Paint piePaint = new Paint();
        piePaint.setStyle(Paint.Style.FILL);

        // GREEN slice: adherence achieved
        piePaint.setColor(Color.BLUE);
        float sweepAngle = (float) (adherencePercent / 100f * 360f);
        canvas.drawArc(x, y, x + size, y + size, -90, sweepAngle, true, piePaint);

        // RED slice: missed adherence
        piePaint.setColor(Color.CYAN);
        canvas.drawArc(x, y, x + size, y + size, -90 + sweepAngle, 360 - sweepAngle, true, piePaint);

        // Draw outline
        piePaint.setStyle(Paint.Style.STROKE);
        piePaint.setColor(Color.BLACK);
        piePaint.setStrokeWidth(2);
        canvas.drawOval(x, y, x + size, y + size, piePaint);

        // Draw text in the middle with 2 decimal places
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(14);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format("%.2f%%", adherencePercent), x + size / 2f, y + size / 2f + 5, textPaint);

        // Add legend below the pie chart
        int legendY = y + size + 15;
        int blockSize = 12;

        // Green legend
        piePaint.setStyle(Paint.Style.FILL);
        piePaint.setColor(Color.BLUE);
        canvas.drawRect(x, legendY, x + blockSize, legendY + blockSize, piePaint);
        paint.setTextSize(10);
        paint.setColor(Color.BLACK);
        canvas.drawText("Adherence", x + blockSize + 5, legendY + blockSize - 2, paint);

        // CYAN legend
        legendY += blockSize + 5;
        piePaint.setColor(Color.CYAN);
        canvas.drawRect(x, legendY, x + blockSize, legendY + blockSize, piePaint);
        canvas.drawText("Missed", x + blockSize + 5, legendY + blockSize - 2, paint);

        // RESET paint properties to original state
        paint.setTextSize(16);
        paint.setColor(Color.BLACK);
    }

    // Utility method to finish PDF
    private void finishPdf(PdfDocument pdfDocument, PdfDocument.Page page, Uri uri) {
        pdfDocument.finishPage(page);
        try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
            if (out != null) {
                pdfDocument.writeTo(out);
                Toast.makeText(getContext(), "PDF exported!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pdfDocument.close();
        }
    }



    // ───────────────────────────────
    // HELPERS
    // ───────────────────────────────

    private TextView buildInfoText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(Color.DKGRAY);
        tv.setPadding(0, dpToPx(4), 0, 0);
        return tv;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ───────────────────────────────
    // MENU HANDLING
    // ───────────────────────────────

    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null)
            return;

        Uri fileUri = data.getData();

        if (requestCode == CREATE_PDF_REQUEST_CODE) {
            exportPdfToUri(fileUri);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (MenuHelper.handleMenuSelection(item, this)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ───────────────────────────────
    // FRAGMENT NAVIGATION
    // ───────────────────────────────

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
