package com.example.SmartAirGroup2.ParentDashboard;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
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
 * A fragment that displays a child's symptom history, allows for filtering, and provides
 * options to export selected symptoms to PDF or CSV formats.
 *
 * <p>This fragment retrieves symptom data for a specific child from the Firebase Realtime Database.
 * The data is displayed in a series of cards. Users can filter this list by clicking a filter
 * button, which opens the {@link SymptomFilterFragment}. Active filters are displayed, and a
 * "Clear Filters" button is provided to reset the view.</p>
 *
 * <p>Each symptom card has a switch, allowing the user to select one or more symptoms for export.
 * The "Export to PDF" and "Export to CSV" buttons trigger the Android Storage Access Framework
 * to let the user choose a location and name for the exported file. The actual export process
 * then writes the selected symptom data to the chosen file URI.</p>
 *
 * <p>The fragment receives the child's name and username as arguments upon creation, which are
 * used to fetch the correct data and to set the toolbar title.</p>
 *
 * @see SymptomFilterFragment
 * @see MenuHelper
 */
public class ExportSymptoms extends Fragment {

    // ───────────────────────────────
    // UI COMPONENTS
    // ───────────────────────────────
    private Toolbar toolbar;
    private CardView cardFilter;

    private String name, uname;
    private String filterSymptom, filterStartDate, filterEndDate;
    private List<String> filterTriggers;

    private Button exportPDF, exportCSV;
    private View view;
    private static final int CREATE_PDF_REQUEST_CODE = 2001;
    private static final int CREATE_CSV_REQUEST_CODE = 2002;

    // ───────────────────────────────
    // LIFECYCLE METHODS
    // ───────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve parent username passed as argument
        if (getArguments() != null) {
            name = getArguments().getString("childName");
            uname = getArguments().getString("childUname");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_export_symptoms, container, false);
        exportPDF = view.findViewById(R.id.PdfButton);
        exportCSV = view.findViewById(R.id.CsvButton);

        exportPDF.setOnClickListener(v -> {
            if (selectedSymptoms.isEmpty()) {
                Toast.makeText(getContext(), "No symptoms selected to export", Toast.LENGTH_SHORT).show();
                return;
            }
            // Launch Save As dialog for PDF
            showSaveAsDialog("application/pdf", "symptoms_export.pdf", CREATE_PDF_REQUEST_CODE);
        });

        exportCSV.setOnClickListener(v -> {
            if (selectedSymptoms.isEmpty()) {
                Toast.makeText(getContext(), "No symptoms selected to export", Toast.LENGTH_SHORT).show();
                return;
            }
            // Launch Save As dialog for CSV
            showSaveAsDialog("text/csv", "symptoms_export.csv", CREATE_CSV_REQUEST_CODE);
        });


        // Toolbar setup
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        toolbar.setTitle(name +"'s Symptom History");

        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // UI references
        cardFilter = view.findViewById(R.id.cardFilter);

        getParentFragmentManager().setFragmentResultListener("symptomFilter", this, (requestKey, bundle) -> {

            filterSymptom = bundle.getString("filter_symptom", null);
            filterStartDate = bundle.getString("filter_start_date", null);
            filterEndDate = bundle.getString("filter_end_date", null);
            filterTriggers = bundle.getStringArrayList("filter_triggers");

            //If there are filters found, add the clear button.
            updateClearFilterButton();

            loadSymptoms(); // reload with filters applied
        });

        loadSymptoms();

        cardFilter.setOnClickListener(v -> {
            SymptomFilterFragment filterFrag = new SymptomFilterFragment();
            loadFragment(filterFrag);
        });

        return view;
    }

    private void showSaveAsDialog(String mimeType, String suggestedName, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        startActivityForResult(intent, requestCode);
    }



    /**
     * Updates the UI by showing or hiding a "Clear Filters" button depending
     * on whether filters are currently active.
     */
    private void updateClearFilterButton() {
        LinearLayout filterActionContainer = view.findViewById(R.id.filterActionContainer);

        // Clear existing button if already added
        filterActionContainer.removeAllViews();

        // Check if ANY filter is active
        boolean hasFilters =
                (filterSymptom != null && !filterSymptom.isEmpty()) ||
                        (filterStartDate != null && !filterStartDate.isEmpty()) ||
                        (filterEndDate != null && !filterEndDate.isEmpty()) ||
                        (filterTriggers != null && !filterTriggers.isEmpty());

        if (!hasFilters) return; // no filters → no button shown

        // Create button dynamically
        Button clearBtn = new Button(requireContext());
        clearBtn.setText("Clear Filters");
        clearBtn.setAllCaps(false);
        clearBtn.setPadding(20, 20, 20, 20);

        clearBtn.setOnClickListener(v -> {
            filterSymptom = null;
            filterStartDate = null;
            filterEndDate = null;
            if (filterTriggers != null) filterTriggers.clear();

            updateClearFilterButton(); // remove button again
            loadSymptoms(); // reload full list
        });


        filterActionContainer.addView(clearBtn);
    }

    /**
     * Loads symptom entries from Firebase and applies active filters before displaying.
     * Clears existing UI items before reloading.
     */
    private void loadSymptoms() {
        if (!isAdded()) return;

        if (view == null) return;

        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/symptoms");

        // Get container BEFORE database listener
        LinearLayout symptomContainer = view.findViewById(R.id.symptomContainer);

        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // 🔥 Clear existing cards before adding new ones
                symptomContainer.removeAllViews();

                // If database is empty show placeholder
                if (!snapshot.exists()) {
                    addSymptomCard("","No symptoms recorded", "", "", "");
                    return;
                }

                // Loop through data
                for (DataSnapshot symptomSnapshot : snapshot.getChildren()) {

                    String id = symptomSnapshot.getKey(); // Firebase ID

                    String symptom = symptomSnapshot.child("symptom").getValue(String.class);
                    String time = symptomSnapshot.child("time").getValue(String.class);
                    String triggers = symptomSnapshot.child("triggers").getValue(String.class);
                    String author = symptomSnapshot.child("type").getValue(String.class);


                    if (!passesFilter(symptom, time, triggers)) continue;

                    addSymptomCard(id, symptom, time, triggers, author);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SYMPTOM_ERROR", error.getMessage());
            }
        });
    }

    /**
     * Determines whether a symptom entry should be shown based on:
     * - Text match
     * - Date range
     * - Trigger selections
     *
     * @return true if entry passes filters, false otherwise
     */
    private boolean passesFilter(String symptom, String time, String triggers) {

        if (filterSymptom != null && !filterSymptom.trim().isEmpty()) {
            if (!symptom.toLowerCase().contains(filterSymptom.toLowerCase())) {
                return false;
            }
        }

        try {
            // Extract ONLY yyyy/MM/dd portion from stored timestamp
            String dateOnly = time.length() >= 10 ? time.substring(0, 10) : time;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date entryDate = sdf.parse(dateOnly);

            // --- HANDLE FILTER CONDITIONS ---
            Date startDate = null;
            Date endDate = null;

            // If start date is valid, parse it; otherwise leave null (means no restriction)
            if (filterStartDate != null  && !filterStartDate.isEmpty()) {
                startDate = sdf.parse(filterStartDate);
            }

            // If end date is valid parse it; otherwise assume today
            if (filterEndDate != null && !filterEndDate.isEmpty()) {
                endDate = sdf.parse(filterEndDate);
            }

            // If startDate exists and entryDate is BEFORE it → exclude
            if (startDate != null && entryDate.before(startDate)) {
                return false;
            }

            // If entryDate is AFTER end date → exclude
            if (entryDate.after(endDate)) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Fail-safe: don't filter out data if parsing failed
        }

        if (filterTriggers != null && !filterTriggers.isEmpty()) {

            String entryTriggersLower = triggers.toLowerCase();
            boolean foundMatch = false;

            for (String trigger : filterTriggers) {
                if (entryTriggersLower.contains(trigger.toLowerCase())) {
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) return false;
        }

        return true;
    }

    /**
     * Dynamically builds and displays a card containing symptom info.
     * Includes delete functionality if the card represents a real database entry.
     *
     * @param symptomId Firebase ID used to delete card
     */

    // At the top of the class
    private final Map<String, String> selectedSymptoms = new HashMap<>();
    @SuppressLint("ResourceType")
    private void addSymptomCard(String symptomId, String symptom, String time, String triggers, String author) {

        if (!isAdded() || getContext() == null) return;
        Context ctx = requireContext();

        // ----- Card container -----
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

        // Touch ripple effect
        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            Drawable selectable = ContextCompat.getDrawable(ctx, outValue.resourceId);
            if (selectable != null) cardView.setForeground(selectable);
        }

        // ----- Inner layout -----
        LinearLayout outerLayout = new LinearLayout(ctx);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        outerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // ======= Title Row =======
        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(ctx);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleView.setText(symptom);
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);

        if (symptomId.equals("")){
            topRow.addView(titleView);
            outerLayout.addView(topRow);
            cardView.addView(outerLayout);
            LinearLayout container = requireView().findViewById(R.id.symptomContainer);
            container.addView(cardView);
            return;
        }

        Switch select = new Switch(ctx);
        select.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(60), dpToPx(20)));
        String symptomKey = symptomId; // unique ID for this symptom

        select.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedSymptoms.put(symptomKey, symptom); // store symptom text
                Toast.makeText(getContext(), "Selected a Symptom", Toast.LENGTH_SHORT).show();
            } else {
                selectedSymptoms.remove(symptomKey);
                Toast.makeText(getContext(), "Deselected a Symptom", Toast.LENGTH_SHORT).show();
            }
        });

        topRow.addView(titleView);
        topRow.addView(select);

        // ======= Info Labels =======
        TextView timeView = buildInfoText("Time: " + time);
        TextView triggerView = buildInfoText("Triggers: " + triggers);
        TextView authorView = buildInfoText("Entered by: " + author);

        outerLayout.addView(topRow);
        outerLayout.addView(timeView);
        outerLayout.addView(triggerView);
        outerLayout.addView(authorView);

        cardView.addView(outerLayout);

        // ----- Add card to layout -----
        LinearLayout container = requireView().findViewById(R.id.symptomContainer);
        container.addView(cardView);
    }

    private void exportCsvToUri(Uri uri) {
        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/symptoms");

        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("Symptom,Time,Triggers,Entered By\n"); // CSV header

                for (DataSnapshot symptomSnapshot : snapshot.getChildren()) {
                    String id = symptomSnapshot.getKey();
                    if (!selectedSymptoms.containsKey(id)) continue; // only export selected

                    String symptom = symptomSnapshot.child("symptom").getValue(String.class);
                    String time = symptomSnapshot.child("time").getValue(String.class);
                    String triggers = symptomSnapshot.child("triggers").getValue(String.class);
                    String author = symptomSnapshot.child("type").getValue(String.class);

                    csvBuilder.append("\"").append(symptom).append("\",")
                            .append("\"").append(time).append("\",")
                            .append("\"").append(triggers).append("\",")
                            .append("\"").append(author).append("\"\n");
                }

                try {
                    OutputStream out = requireContext().getContentResolver().openOutputStream(uri);
                    if (out != null) {
                        out.write(csvBuilder.toString().getBytes());
                        out.close();
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
        DatabaseReference statusRef = FirebaseDatabase.getInstance()
                .getReference("categories/users/children")
                .child(uname)
                .child("data/symptoms");

        statusRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                canvas.drawText("Child Symptom Report:", 50, y, paint);
                y += 30;

                for (DataSnapshot symptomSnapshot : snapshot.getChildren()) {
                    String id = symptomSnapshot.getKey();
                    if (!selectedSymptoms.containsKey(id)) continue;

                    String symptom = symptomSnapshot.child("symptom").getValue(String.class);
                    String time = symptomSnapshot.child("time").getValue(String.class);
                    String triggers = symptomSnapshot.child("triggers").getValue(String.class);
                    String author = symptomSnapshot.child("type").getValue(String.class);

                    canvas.drawText("- Symptom: " + symptom, 50, y, paint);
                    y += 20;
                    canvas.drawText("  Time: " + time, 50, y, paint);
                    y += 20;
                    canvas.drawText("  Triggers: " + triggers, 50, y, paint);
                    y += 20;
                    canvas.drawText("  Entered by: " + author, 50, y, paint);
                    y += 30;
                }

                pdfDocument.finishPage(page);

                try {
                    OutputStream out = requireContext().getContentResolver().openOutputStream(uri);
                    if (out != null) {
                        pdfDocument.writeTo(out);
                        out.close();
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;

        Uri fileUri = data.getData();

        if (requestCode == CREATE_PDF_REQUEST_CODE) {
            exportPdfToUri(fileUri);
        } else if (requestCode == CREATE_CSV_REQUEST_CODE) {
            exportCsvToUri(fileUri);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenu(menu, inflater, requireContext());
        MenuHelper.setupNotification(this,menu,inflater);
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
    /**
     * Utility method for fragment navigation inside the same activity.
     */
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}