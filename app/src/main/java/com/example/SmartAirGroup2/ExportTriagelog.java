package com.example.SmartAirGroup2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment to export triage logs to PDF / CSV with filtering support.
 */
public class ExportTriagelog extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardFilter;

    private String name, uname;
    private String filterRedflag, filterStartDate, filterEndDate;
    private List<String> filterTriggers;

    private Button exportPDF, exportCSV;
    private View view;
    private static final int CREATE_PDF_REQUEST_CODE = 2001;
    private static final int CREATE_CSV_REQUEST_CODE = 2002;

    // Stores selected triages for export
    private final Map<String, String> selectedTriages = new HashMap<>();

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_export_triagelog, container, false);

        exportPDF = view.findViewById(R.id.PdfButton);
        exportCSV = view.findViewById(R.id.CsvButton);

        exportPDF.setOnClickListener(v -> {
            if (selectedTriages.isEmpty()) {
                Toast.makeText(getContext(), "No triages selected to export", Toast.LENGTH_SHORT).show();
                return;
            }
            showSaveAsDialog("application/pdf", "triages_export.pdf", CREATE_PDF_REQUEST_CODE);
        });

        exportCSV.setOnClickListener(v -> {
            if (selectedTriages.isEmpty()) {
                Toast.makeText(getContext(), "No triages selected to export", Toast.LENGTH_SHORT).show();
                return;
            }
            showSaveAsDialog("text/csv", "triages_export.csv", CREATE_CSV_REQUEST_CODE);
        });

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        toolbar.setTitle(name + "'s Triage History");
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        cardFilter = view.findViewById(R.id.cardFilter);
        cardFilter.setOnClickListener(v -> loadFragment(new TriageFilterFragment()));

        getParentFragmentManager().setFragmentResultListener("triageFilter", this,
                (requestKey, bundle) -> {
                    filterRedflag = bundle.getString("filter_redflag", null);
                    filterStartDate = bundle.getString("filter_start_date", null);
                    filterEndDate = bundle.getString("filter_end_date", null);
                    filterTriggers = bundle.getStringArrayList("filter_triggers");

                    updateClearFilterButton();
                    loadTriages();
                });

        loadTriages();
        return view;
    }

    private void showSaveAsDialog(String mimeType, String suggestedName, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        startActivityForResult(intent, requestCode);
    }

    private void updateClearFilterButton() {
        LinearLayout filterActionContainer = view.findViewById(R.id.filterActionContainer);
        filterActionContainer.removeAllViews();

        boolean hasFilters =
                (filterRedflag != null && !filterRedflag.isEmpty()) ||
                        (filterStartDate != null && !filterStartDate.isEmpty()) ||
                        (filterEndDate != null && !filterEndDate.isEmpty()) ||
                        (filterTriggers != null && !filterTriggers.isEmpty());

        if (!hasFilters) return;

        Button clearBtn = new Button(requireContext());
        clearBtn.setText("Clear Filters");
        clearBtn.setAllCaps(false);
        clearBtn.setPadding(20, 20, 20, 20);
        clearBtn.setOnClickListener(v -> {
            filterRedflag = null;
            filterStartDate = null;
            filterEndDate = null;
            if (filterTriggers != null) filterTriggers.clear();

            updateClearFilterButton();
            loadTriages();
        });

        filterActionContainer.addView(clearBtn);
    }

    private void loadTriages() {
        if (!isAdded() || view == null) return;

        DatabaseReference triageRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/triages");

        LinearLayout triageContainer = view.findViewById(R.id.triageContainer);

        triageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                triageContainer.removeAllViews();

                if (!snapshot.exists()) {
                    addTriageCard("", "No triages recorded", "", "", "", "");
                    return;
                }

                for (DataSnapshot triageSnapshot : snapshot.getChildren()) {
                    String id = triageSnapshot.getKey();

                    Object redflagsObj = triageSnapshot.child("redflags").getValue();
                    String redflags = mapToString(redflagsObj);
                    String time = triageSnapshot.child("time").getValue(String.class);
                    String guidance = triageSnapshot.child("guidance").getValue(String.class);
                    String response = triageSnapshot.child("response").getValue(String.class);
                    String PEF = triageSnapshot.child("PEF").getValue(String.class);

                    if (!passesFilter(redflags, time, response, guidance, PEF)) continue;

                    addTriageCard(id, redflags, time, guidance, response, PEF);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TRIAGE_ERROR", error.getMessage());
            }
        });
    }

    private String mapToString(Object value) {
        if (value == null) return "";
        if (value instanceof String) return (String) value;
        if (value instanceof Map) {
            StringBuilder sb = new StringBuilder();
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if ((boolean) entry.getValue()) sb.append(entry.getKey()).append(", ");
            }
            return sb.toString().trim();
        }
        return value.toString();
    }

    private boolean passesFilter(String redflags, String time, String guidance, String response, String PEF) {
        if (filterRedflag != null && !filterRedflag.trim().isEmpty()) {
            if (!redflags.toLowerCase().contains(filterRedflag.toLowerCase())) return false;
        }

        try {
            String dateOnly = time.length() >= 10 ? time.substring(0, 10) : time;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date entryDate = sdf.parse(dateOnly);

            Date startDate = (filterStartDate != null && !filterStartDate.isEmpty()) ? sdf.parse(filterStartDate) : null;
            Date endDate = (filterEndDate != null && !filterEndDate.isEmpty()) ? sdf.parse(filterEndDate) : null;

            if (startDate != null && entryDate.before(startDate)) return false;
            if (endDate != null && entryDate.after(endDate)) return false;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (filterTriggers != null && !filterTriggers.isEmpty()) {
            boolean found = false;
            for (String trigger : filterTriggers) {
                if (redflags.toLowerCase().contains(trigger.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    @SuppressLint("ResourceType")
    private void addTriageCard(String id, String redflags, String time, String guidance, String response, String PEF) {
        if (!isAdded() || getContext() == null) return;
        Context ctx = requireContext();

        CardView cardView = new CardView(ctx);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        cardView.setLayoutParams(cardParams);
        cardView.setCardBackgroundColor(0xFFC8E6C9);
        cardView.setRadius(dpToPx(8));
        cardView.setCardElevation(4);

        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            Drawable selectable = ContextCompat.getDrawable(ctx, outValue.resourceId);
            if (selectable != null) cardView.setForeground(selectable);
        }

        LinearLayout outerLayout = new LinearLayout(ctx);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        outerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(ctx);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleView.setText(redflags);
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);

        if (id.equals("")) {
            topRow.addView(titleView);
            outerLayout.addView(topRow);
            cardView.addView(outerLayout);
            ((LinearLayout) view.findViewById(R.id.triageContainer)).addView(cardView);
            return;
        }

        Switch select = new Switch(ctx);
        select.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(60), dpToPx(20)));
        String triageKey = id;

        select.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedTriages.put(triageKey, redflags);
                Toast.makeText(getContext(), "Selected a Triage", Toast.LENGTH_SHORT).show();
            } else {
                selectedTriages.remove(triageKey);
                Toast.makeText(getContext(), "Deselected a Triage", Toast.LENGTH_SHORT).show();
            }
        });

        topRow.addView(titleView);
        topRow.addView(select);

        outerLayout.addView(topRow);
        outerLayout.addView(buildInfoText("Time: " + time));
        outerLayout.addView(buildInfoText("Guidance: " + guidance));
        outerLayout.addView(buildInfoText("Response: " + response));
        outerLayout.addView(buildInfoText("PEF: " + PEF));

        cardView.addView(outerLayout);
        ((LinearLayout) view.findViewById(R.id.triageContainer)).addView(cardView);
    }

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
    // EXPORT LOGIC
    // ───────────────────────────────
    private void exportCsvToUri(Uri uri) {
        DatabaseReference triageRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/triages");

        triageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("Redflags,Time,Guidance,Response,PEF\n");

                for (DataSnapshot triageSnapshot : snapshot.getChildren()) {
                    String id = triageSnapshot.getKey();
                    if (!selectedTriages.containsKey(id)) continue;

                    Object redflagsObj = triageSnapshot.child("redflags").getValue();
                    String redflags = mapToString(redflagsObj);
                    String time = triageSnapshot.child("time").getValue(String.class);
                    String guidance = triageSnapshot.child("guidance").getValue(String.class);
                    String response = triageSnapshot.child("response").getValue(String.class);
                    String PEF = triageSnapshot.child("PEF").getValue(String.class);

                    csvBuilder.append("\"").append(redflags).append("\",")
                            .append("\"").append(time).append("\",")
                            .append("\"").append(guidance).append("\",")
                            .append("\"").append(response).append("\",")
                            .append("\"").append(PEF).append("\"\n");
                }

                try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        out.write(csvBuilder.toString().getBytes());
                        Toast.makeText(getContext(), "CSV exported!", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Export failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exportPdfToUri(Uri uri) {
        DatabaseReference triageRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/triages");

        triageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PdfDocument pdfDocument = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();
                paint.setColor(Color.BLACK);
                paint.setTextSize(16);

                int y = 50;
                canvas.drawText("Child Triage Report:", 50, y, paint);
                y += 30;

                for (DataSnapshot triageSnapshot : snapshot.getChildren()) {
                    String id = triageSnapshot.getKey();
                    if (!selectedTriages.containsKey(id)) continue;

                    Object redflagsObj = triageSnapshot.child("redflags").getValue();
                    String redflags = mapToString(redflagsObj);
                    String time = triageSnapshot.child("time").getValue(String.class);
                    String guidance = triageSnapshot.child("guidance").getValue(String.class);
                    String response = triageSnapshot.child("response").getValue(String.class);
                    String PEF = triageSnapshot.child("PEF").getValue(String.class);

                    canvas.drawText("- Redflags: " + redflags, 50, y, paint);
                    y += 20;
                    canvas.drawText("  Time: " + time, 50, y, paint);
                    y += 20;
                    canvas.drawText("  Guidance: " + guidance, 50, y, paint);
                    y += 20;
                    canvas.drawText("  Response: " + response, 50, y, paint);
                    y += 20;
                    canvas.drawText("  PEF: " + PEF, 50, y, paint);
                    y += 30;
                }

                pdfDocument.finishPage(page);

                try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        pdfDocument.writeTo(out);
                        Toast.makeText(getContext(), "PDF exported!", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    pdfDocument.close();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Export failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ───────────────────────────────
    // MENU & NAVIGATION
    // ───────────────────────────────
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;

        Uri fileUri = data.getData();
        if (requestCode == CREATE_PDF_REQUEST_CODE) exportPdfToUri(fileUri);
        else if (requestCode == CREATE_CSV_REQUEST_CODE) exportCsvToUri(fileUri);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
