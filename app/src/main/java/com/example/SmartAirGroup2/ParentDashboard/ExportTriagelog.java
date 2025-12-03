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
 * A fragment that displays a child's triage history and provides functionality to export
 * selected triage logs to PDF or CSV formats.
 * <p>
 * This class retrieves triage data from Firebase for a specific child, identified by their
 * username. The logs are displayed as a list of selectable cards. Users can filter the
 * displayed logs based on criteria such as red flags, date ranges, and specific triggers
 * by navigating to the {@link TriageFilterFragment}.
 * <p>
 * Selected triage logs can be exported into a PDF or CSV file, which is saved to the
 * device's storage using the Android Storage Access Framework.
 *

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

    /**
     * Initiates the Android Storage Access Framework to display a "Save As" dialog.
     * This allows the user to choose a location and a name for the file to be saved.
     * The method constructs an {@link Intent} with the {@code ACTION_CREATE_DOCUMENT} action,
     * which is used to create a new file. The result of this action is handled in
     * the {@link #onActivityResult(int, int, Intent)} method.
     *
     * @param mimeType      The MIME type of the file to be created (e.g., "application/pdf" or "text/csv").
     * @param suggestedName The default file name to be suggested to the user in the dialog.
     * @param requestCode   The integer request code to identify this request when the result is returned
     *                      in {@code onActivityResult}. This is used to differentiate between, for example,
     *                      a PDF save request and a CSV save request.
     */
    private void showSaveAsDialog(String mimeType, String suggestedName, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);
        startActivityForResult(intent, requestCode);
    }

    /**
     * Updates the UI to show or hide a "Clear Filters" button.
     * This method checks if any filters (red flag, date range, or triggers) are currently
     * active. If at least one filter is set, it dynamically creates and displays a
     * "Clear Filters" button in the designated container. Clicking this button will
     * reset all filter variables to their default state (null or empty) and then
     * trigger a reload of the triage list to show all entries.
     * If no filters are active, it ensures the container for the button is empty,
     * effectively hiding the button.
     */
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

    /**
     * Fetches triage data from the Firebase Realtime Database for the current child.
     * It dynamically populates the UI with cards representing each triage log entry.
     * The method first checks if the fragment is attached to a context and if its view has been created
     * to prevent crashes. It then establishes a connection to the specific child's triage data node in Firebase
     * using the child's username (`uname`).
     * A {@link ValueEventListener} is used to retrieve the data once. On successful data retrieval
     * ({@code onDataChange}), the existing views in the triage container are cleared. If no triage data exists,
     * a single card indicating "No triages recorded" is displayed. Otherwise, it iterates through each
     * triage entry.
     * For each entry, it applies the active filters (red flags, date range, triggers) using the
     * {@code passesFilter} method. If an entry passes the filter criteria, a new card is created and added
     * to the UI using the {@code addTriageCard} method.
     * If there's a database error ({@code onCancelled}), an error message is logged.
     */
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

    /**
     * Converts a Firebase data object, which may be a String or a Map, into a
     * comma-separated string.
     * This is a utility method for handling Firebase data that represents red flags.
     * The data can come in two formats: a simple string or a map where keys are red flag names
     * and values are booleans indicating if they were triggered.
     *
     * @param value The object retrieved from Firebase. Expected to be a String or a Map.
     * @return A formatted string. If the input is a Map, it returns a comma-separated list
     *         of keys whose corresponding value is {@code true}. If the input is a String,
     *         it returns the string itself. If the input is null or another type, it returns
     *         an empty string or the result of {@code toString()}.
     */
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

    /**
     * Checks if a given triage log entry matches the current filter criteria.

     * If any filter is not set (i.e., is null or empty), it is ignored. The method
     * short-circuits and returns {@code false} as soon as a filter condition is not met.
     * Note: the parameters {@code guidance}, {@code response}, and {@code PEF} are included
     * for future filter expansions but are not currently used.
     *
     * @param redflags The string of red flags associated with the triage log.
     * @param time     The timestamp of the triage log (e.g., "yyyy/MM/dd HH:mm:ss").
     * @param guidance The guidance given in the triage log (currently unused for filtering).
     * @param response The response recorded in the triage log (currently unused for filtering).
     * @param PEF      The Peak Expiratory Flow value (currently unused for filtering).
     * @return {@code true} if the triage log entry passes all active filters, {@code false} otherwise.
     */
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

    /**
     * Dynamically creates and adds a {@link CardView} to the UI for a single triage record.
     * Each card displays details such as red flags, time, guidance, response, and PEF.
     * It also includes a {@link Switch} to allow the user to select or deselect the triage
     * record for export. If the provided 'id' is empty, it displays a message card
     *
     * @param id       The unique identifier for the triage record from Firebase. Used for selection.
     * @param redflags A string representing the red flags identified in the triage.
     * @param time     The timestamp of when the triage was recorded.
     * @param guidance The guidance provided based on the triage results.
     * @param response The user's response to the triage questions.
     * @param PEF      The Peak Expiratory Flow value recorded, if any.
     */
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

    /**
     * Creates and configures a TextView to display a single line of triage information.
     * This helper method simplifies the creation of text views used within each triage card.
     * It sets a standard text size, color, and padding for a consistent look and feel.
     *
     * @param text The string content to be displayed in the TextView.
     * @return A configured TextView instance ready to be added to a layout.
     */
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

    /**
     * Exports the selected triage logs to a CSV file at the specified URI.
     * <p>
     * This method queries the Firebase database for the triage data of the current child.
     * It iterates through the retrieved data, filtering for logs that have been selected by the user
     * (i.e., their IDs are present in the {@code selectedTriages} map).
     * <p>
     * For each selected log, it constructs a CSV row containing the red flags, timestamp,
     * guidance, user response, and PEF value. The resulting CSV string, including a header row,
     * is then written to the output stream provided by the content resolver for the given URI.
     * <p>
     * Toasts are displayed to indicate the success or failure of the export operation.
     * In case of failure, the error message is logged or shown in a toast.
     *
     * @param uri The URI of the file where the CSV data will be saved. This is obtained from
     *            the Storage Access Framework's file picker dialog.
     */ // ───────────────────────────────
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

    /**
     * Exports the selected triage logs to a PDF file at the specified URI.
     * <p>
     * This method retrieves triage data for the current child from Firebase. It filters
     * this data to include only the triages that have been selected by the user
     * (identified by their keys in the {@code selectedTriages} map).
     * <p>
     * It then creates a new PDF document and draws the details of each selected triage
     * onto a page. The details include red flags, time, guidance, response, and PEF.
     * Finally, it writes the generated PDF to the {@link OutputStream} provided by the
     * content resolver for the given {@link Uri}.
     * <p>
     * Toasts are displayed to the user to indicate the success or failure of the export
     * operation.
     *
     * @param uri The URI of the file where the PDF will be saved. This is typically
     *            obtained from the Storage Access Framework's file picker.
     */
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
