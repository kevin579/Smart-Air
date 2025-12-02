package com.example.SmartAirGroup2.ChildDashboard;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChildLogsFragment serves as the main screen for viewing and logging a child's
 * asthma-related history (PEF, Controller, Rescue).
 *
 * This fragment is responsible for:
 * 1. Handling initial launch checks (Terms Acceptance).
 * 2. Retrieving the target child's ID via Fragment arguments.
 * 3. Initializing the Firebase Realtime Database connection for the child's logs.
 * 4. Managing filter states and launching logging dialogs.
 * 5. Displaying the log history loaded via {@code loadLogs()}.
 */
public class ChildLogsFragment extends Fragment {

    // ─────────────────────────────────────────────────────────────────
    // FIELDS / UI STATE
    // ─────────────────────────────────────────────────────────────────
    /**
     * The Firebase key for the child passed by intent when called from child dashboard
     * It is a string that matches the child's username
     */
    private String currentChildId;

    /**
     * Firebase instance to access realtime DB.
     */
    private FirebaseDatabase db;
    /**
     * Reference to categories/users/children in the database
     */
    private DatabaseReference childrenRef;

    /**
     * Container LinearLayout that holds the created log views.
     * Is filled by the loadLogs() method.
     */
    private LinearLayout logsContainer;

    /**
     * Current filter for log view.
     * Can be one of: "all", "pef", "controller", "rescue".
     * Defaults to "all".
     */
    private String currentFilter = "all";
    private Toolbar toolbar;


    // ─────────────────────────────────────────────────────────────────
    // FACTORY METHOD (Replaces Intent usage for Fragments)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Factory method to create a new instance of this fragment and pass arguments.
     *
     * @param childId The unique ID/username of the child whose logs are being viewed.
     * @return A new instance of ChildLogsFragment.
     */
    public static ChildLogsFragment newInstance(String childId) {
        ChildLogsFragment fragment = new ChildLogsFragment();
        Bundle args = new Bundle();
        args.putString("childId", childId);
        fragment.setArguments(args);
        return fragment;
    }


    // ─────────────────────────────────────────────────────────────────
    // UTILITIES
    // ─────────────────────────────────────────────────────────────────
    /**
     * Convert dp units to pixels with respect to display density.
     *
     * @param dp dp value
     * @return px value (int)
     */
    private int dpToPx(int dp) {
        // getResources() is available inside a Fragment
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    // Assumed placeholder method for navigation (as loadFragment was in the Activity)
    private void loadFragment(Fragment fragment) {
        // Use getParentFragmentManager() as the Fragment Manager for the hosting Activity
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);

        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Placeholder class for a data log item retrieved from Firebase.
     * Includes the unique key, category, timestamp, and a data map.
     */
    private static class LogItem {
        public String key;
        public String category;
        public long timestamp;

        // Added for deletion/unique reference in Firebase
        public String groupKey;
        public String nodeKey;


        public Map<String, Object> data;

        // Placeholder constructor
        public LogItem(String key, String category, long timestamp, String groupKey, String nodeKey, Map<String, Object> data) {
            this.key = key;
            this.category = category;
            this.timestamp = timestamp;
            this.groupKey = groupKey;
            this.nodeKey = nodeKey;
            this.data = data;
        }
    }


    // ─────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Called to do initial creation of the fragment.
     * Retrieves the child ID from the arguments bundle.
     *
     * @param savedInstanceState saved state from system, can be null
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve arguments passed via newInstance
        if (getArguments() != null) {
            currentChildId = getArguments().getString("childUname");
        }
    }


    /**
     * onCreateView - inflates the layout and initializes UI, Firebase references, and click listeners.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Inflate Layout (Replaces setContentView)
        View view = inflater.inflate(R.layout.fragment_child_logs, container, false); // Assuming layout works for fragment

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        toolbar.setTitle("My logs");
        // Handle back navigation (up button)
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());



        if (currentChildId == null) {
            // CRITICAL: Cannot load logs without a child ID.
            Toast.makeText(requireContext(), "Error: Child ID is missing for log access.", Toast.LENGTH_LONG).show();
            // Cannot call finish() in a Fragment, rely on the host to handle
            return view; // Return the view but prevent further setup/loading
        }
        // --- End Guard Clause ---

        // Initialize Firebase references
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        childrenRef = db.getReference("categories").child("users").child("children");

        // 2. Locate Views (Using the inflated 'view')
        logsContainer = view.findViewById(R.id.logsContainer);

        Button addInhalorButton = view.findViewById(R.id.addInhalor);
        Button addPEFButton = view.findViewById(R.id.addPEF);
        Button addMedicineButton = view.findViewById(R.id.addMedicine);

        Button filterAll = view.findViewById(R.id.filterAll);
        Button filterPEF = view.findViewById(R.id.filterPEF);
        Button filterController = view.findViewById(R.id.filterController);
        Button filterRescue = view.findViewById(R.id.filterRescue);

        // 3. Set up listeners
        //filter button actions
        filterAll.setOnClickListener(v -> {
            currentFilter = "all";
            loadLogs();
        });
        filterPEF.setOnClickListener(v -> {
            currentFilter = "pef";
            loadLogs();
        });
        filterController.setOnClickListener(v -> {
            currentFilter = "medication";
            loadLogs();
        });
        filterRescue.setOnClickListener(v -> {
            currentFilter = "inhaler";
            loadLogs();
        });

        addInhalorButton.setOnClickListener(v -> showMedicationDialog("Log Inhaler Use", false));
        addMedicineButton.setOnClickListener(v -> showMedicationDialog("Log Controller Medicine Use", true));
        addPEFButton.setOnClickListener(v -> showPEFDialog());

        // 4. Initial Load
        loadLogs();

        return view;
    }

    /**
     * String conversion for display. Returns default if null or literal "null".
     *
     * @param o   object to stringify
     * @param def default string if o is null or "null"
     * @return string
     */
    private String safeToString(Object o, String def) {
        if (o == null) return def == null ? "" : def;
        String s = String.valueOf(o);
        return s.equals("null") ? (def == null ? "" : def) : s;
    }

    /**
     * Helper method to set a date/time formatted string into an EditText based on a Calendar instance.
     *
     * @param editText The EditText view to update.
     * @param calendar The Calendar instance containing the date/time.
     */
    private void setDateTimeEditTextFromCalendar(EditText editText, Calendar calendar) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        editText.setText(fmt.format(calendar.getTime()));
    }

    /**
     * Helper method to launch the date and time pickers sequentially.
     *
     * @param calendar The Calendar instance to modify.
     * @param editText The EditText view to update.
     */
    private void pickDateTime(final Calendar calendar, final EditText editText) {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(requireContext(), (view2, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                setDateTimeEditTextFromCalendar(editText, calendar);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Utility to safely extract Map<String, Object> from a DataSnapshot.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> snapshotToMap(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    /**
     * Utility to extract the timestamp from a log entry map.
     */
    private long getTimestampFromMap(Map<String, Object> map) {
        Object tsObj = map.get("timestamp");
        if (tsObj instanceof Long) {
            return (Long) tsObj;
        }
        // Handle other types if necessary
        return 0;
    }

    /**
     * Placeholder method for Daily PEF update.
     * @see #updateDailyPEF() for implementation details.
     */
    private void updateDailyPEF() {
        DatabaseReference pefRef = childrenRef.child(currentChildId).child("logs").child("PEF_log");
        pefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Integer> todayVals = new ArrayList<>();

                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                long startOfDay = c.getTimeInMillis();
                c.add(Calendar.DAY_OF_MONTH, 1);
                long startOfNextDay = c.getTimeInMillis();

                for (DataSnapshot entry : snapshot.getChildren()) {
                    Map<String, Object> map = snapshotToMap(entry);
                    long ts = getTimestampFromMap(map);
                    if (ts >= startOfDay && ts < startOfNextDay) {
                        Object v = map.get("value");
                        if (v != null) {
                            try {
                                int val = Integer.parseInt(String.valueOf(v));
                                todayVals.add(val);
                            } catch (Exception ignored) { }
                        }
                    }
                }

                DatabaseReference dataRef = childrenRef.child(currentChildId).child("data").child("dailyPEF");
                dataRef.setValue(todayVals, (err, ref) -> {
                    if (err != null) {
                        Toast.makeText(requireContext(), "Failed to update dailyPEF: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // ignore
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // LOG ENTRY MANAGEMENT
    // ─────────────────────────────────────────────────────────────────

    /**
     * confirmAndDelete - shows an AlertDialog for confirming deletion of a specific log item.
     * On confirmation, it removes the node from Firebase and refreshes the log list.
     *
     * @param item The LogItem to be deleted.
     */
    private void confirmAndDelete(final LogItem item) {
        new AlertDialog.Builder(requireContext()) // CONTEXT FIX
                .setTitle("Delete Log")
                .setMessage("Are you sure you want to delete this log?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DatabaseReference nodeRef = childrenRef.child(currentChildId).child("logs").child(item.groupKey).child(item.nodeKey);
                    nodeRef.removeValue((err, ref) -> {
                        if (err == null) {
                            Toast.makeText(requireContext(), "Log deleted", Toast.LENGTH_SHORT).show(); // CONTEXT FIX
                            loadLogs();
                            if ("PEF_log".equals(item.groupKey)) updateDailyPEF();
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete: " + err.getMessage(), Toast.LENGTH_LONG).show(); // CONTEXT FIX
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    // ─────────────────────────────────────────────────────────────────
    // DIALOGS (ADD ENTRIES)
    // ─────────────────────────────────────────────────────────────────

    /**
     * showPEFDialog - builds and displays an AlertDialog to record a peak flow entry.
     * On Save: validates fields, builds a map and calls saveEntryWithSequentialKey for "PEF_log".
     */
    private void showPEFDialog() {
        LinearLayout layout = new LinearLayout(requireContext()); // CONTEXT FIX
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(12);
        layout.setPadding(pad, pad, pad, pad);

        TextView peakLabel = new TextView(requireContext()); // CONTEXT FIX
        peakLabel.setText("Peak Flow (L/min):");
        layout.addView(peakLabel);

        EditText peakFlowInput = new EditText(requireContext()); // CONTEXT FIX
        peakFlowInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        peakFlowInput.setHint("e.g. 320");
        layout.addView(peakFlowInput);

        TextView preMedLabel = new TextView(requireContext()); // CONTEXT FIX
        preMedLabel.setText("Pre-med value (optional):");
        layout.addView(preMedLabel);
        EditText preMedInput = new EditText(requireContext()); // CONTEXT FIX
        preMedInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        preMedInput.setHint("e.g. 300 (optional)");
        layout.addView(preMedInput);

        TextView postMedLabel = new TextView(requireContext()); // CONTEXT FIX
        postMedLabel.setText("Post-med value (optional):");
        layout.addView(postMedLabel);
        EditText postMedInput = new EditText(requireContext()); // CONTEXT FIX
        postMedInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        postMedInput.setHint("e.g. 320 (optional)");
        layout.addView(postMedInput);

        TextView notesLabel = new TextView(requireContext()); // CONTEXT FIX
        notesLabel.setText("Notes (optional):");
        layout.addView(notesLabel);

        EditText notesInput = new EditText(requireContext()); // CONTEXT FIX
        notesInput.setHint("e.g. taken after exercise");
        layout.addView(notesInput);

        TextView timeLabel = new TextView(requireContext()); // CONTEXT FIX
        timeLabel.setText("Date & Time:");
        layout.addView(timeLabel);

        EditText timeInput = new EditText(requireContext()); // CONTEXT FIX
        timeInput.setFocusable(false);
        timeInput.setClickable(true);
        timeInput.setHint("Tap to pick date and time");
        layout.addView(timeInput);

        final Calendar calendar = Calendar.getInstance();
        setDateTimeEditTextFromCalendar(timeInput, calendar);

        timeInput.setOnClickListener(v -> pickDateTime(calendar, timeInput));

        new AlertDialog.Builder(requireContext()) // CONTEXT FIX
                .setTitle("Record Peak Flow")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String peak = peakFlowInput.getText().toString().trim();
                    String pre = preMedInput.getText().toString().trim();
                    String post = postMedInput.getText().toString().trim();
                    String notes = notesInput.getText().toString().trim();
                    String dateTime = timeInput.getText().toString().trim();

                    if (peak.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter peak flow.", Toast.LENGTH_SHORT).show(); // CONTEXT FIX
                        return;
                    }

                    int peakVal;
                    try {
                        peakVal = Integer.parseInt(peak);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Peak flow must be a number.", Toast.LENGTH_SHORT).show(); // CONTEXT FIX
                        return;
                    }

                    long timestamp = calendar.getTimeInMillis();

                    Map<String, Object> pefEntry = new HashMap<>();
                    pefEntry.put("type", "pef");
                    pefEntry.put("timestamp", timestamp);
                    pefEntry.put("dateTime", dateTime);
                    pefEntry.put("value", peakVal);
                    pefEntry.put("note", notes.isEmpty() ? "" : notes);
                    pefEntry.put("preMedVal", pre.isEmpty() ? "" : pre);
                    pefEntry.put("postMedVal", post.isEmpty() ? "" : post);

                    DatabaseReference logsRoot = childrenRef.child(currentChildId).child("logs");
                    saveEntryWithSequentialKey(logsRoot, "PEF_log", "PEF", pefEntry,
                            "Saved PEF: " + peakVal);
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * showMedicationDialog - builds and displays a medication/inhaler logging dialog.
     *
     * @param title      dialog title
     * @param askForType if true, ask user for medicine name and save under controller_log,
     * if false, treat as rescue/inhaler entry and save under rescue_log
     */
    private void showMedicationDialog(String title, boolean askForType) {
        LinearLayout layout = new LinearLayout(requireContext()); // CONTEXT FIX
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(12);
        layout.setPadding(pad, pad, pad, pad);

        TextView beforeLabel = new TextView(requireContext()); // CONTEXT FIX
        beforeLabel.setText("How you felt before (optional):");
        layout.addView(beforeLabel);

        EditText beforeInput = new EditText(requireContext()); // CONTEXT FIX
        beforeInput.setHint("e.g. breathless, OK");
        layout.addView(beforeInput);

        TextView afterLabel = new TextView(requireContext()); // CONTEXT FIX
        afterLabel.setText("How you felt after (optional):");
        layout.addView(afterLabel);

        EditText afterInput = new EditText(requireContext()); // CONTEXT FIX
        afterInput.setHint("e.g. improved, same");
        layout.addView(afterInput);

        EditText typeInput = null;
        if (askForType) {
            TextView typeLabel = new TextView(requireContext()); // CONTEXT FIX
            typeLabel.setText("Type of Medicine:");
            layout.addView(typeLabel);

            typeInput = new EditText(requireContext()); // CONTEXT FIX
            typeInput.setHint("e.g. Salbutamol, Fluticasone");
            layout.addView(typeInput);
        }

        TextView doseLabel = new TextView(requireContext()); // CONTEXT FIX
        doseLabel.setText("Dosage (optional):");
        layout.addView(doseLabel);

        EditText doseInput = new EditText(requireContext()); // CONTEXT FIX
        doseInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        doseInput.setHint("e.g. 2");
        layout.addView(doseInput);

        TextView unitsLabel = new TextView(requireContext()); // CONTEXT FIX
        unitsLabel.setText("Units (optional):");
        layout.addView(unitsLabel);

        EditText unitsInput = new EditText(requireContext()); // CONTEXT FIX
        unitsInput.setHint("e.g. puffs, mcg, mg");
        layout.addView(unitsInput);

        TextView notesLabel = new TextView(requireContext()); // CONTEXT FIX
        notesLabel.setText("Notes (optional):");
        layout.addView(notesLabel);

        EditText notesInput = new EditText(requireContext()); // CONTEXT FIX
        notesInput.setHint("e.g. used before exercise");
        layout.addView(notesInput);

        TextView timeLabel = new TextView(requireContext()); // CONTEXT FIX
        timeLabel.setText("Date & Time of Use:");
        layout.addView(timeLabel);

        EditText timeInput = new EditText(requireContext()); // CONTEXT FIX
        timeInput.setFocusable(false);
        timeInput.setClickable(true);
        timeInput.setHint("Tap to pick date and time");
        layout.addView(timeInput);

        final Calendar calendar = Calendar.getInstance();
        setDateTimeEditTextFromCalendar(timeInput, calendar);

        timeInput.setOnClickListener(v -> pickDateTime(calendar, timeInput));

        EditText finalTypeInput = typeInput;

        new AlertDialog.Builder(requireContext()) // CONTEXT FIX
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String before = beforeInput.getText().toString().trim();
                    String after = afterInput.getText().toString().trim();
                    String type;
                    if (askForType) {
                        type = finalTypeInput.getText().toString().trim();
                        if (type.isEmpty()) {
                            Toast.makeText(requireContext(), "Please enter medicine type.", Toast.LENGTH_SHORT).show(); // CONTEXT FIX
                            return;
                        }
                    } else {
                        type = "Inhaler";
                    }

                    String doseStr = doseInput.getText().toString().trim();
                    Double doseVal = null;
                    if (!doseStr.isEmpty()) {
                        try {
                            doseVal = Double.parseDouble(doseStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Dosage must be a number.", Toast.LENGTH_SHORT).show(); // CONTEXT FIX
                            return;
                        }
                    }

                    String units = unitsInput.getText().toString().trim();
                    String notes = notesInput.getText().toString().trim();
                    String dateTime = timeInput.getText().toString().trim();
                    long timestamp = calendar.getTimeInMillis();

                    Map<String, Object> medEntry = new HashMap<>();
                    medEntry.put("type", askForType ? "medication" : "inhaler");
                    medEntry.put("timestamp", timestamp);
                    medEntry.put("dateTime", dateTime);
                    medEntry.put("medication", askForType ? type : "Inhaler");
                    medEntry.put("dosage", doseVal == null ? null : doseVal);
                    medEntry.put("dose", (doseVal == null ? "" : doseVal) + (units.isEmpty() ? "" : (" " + units)).trim());
                    medEntry.put("units", units.isEmpty() ? "" : units);
                    medEntry.put("preDose", before.isEmpty() ? "" : before);
                    medEntry.put("postDose", after.isEmpty() ? "" : after);
                    medEntry.put("note", notes.isEmpty() ? "" : notes);

                    DatabaseReference logsRoot = childrenRef.child(currentChildId).child("logs");
                    String groupKey = askForType ? "controller_log" : "rescue_log";
                    String keyPrefix = askForType ? "controller" : "rescue";

                    saveEntryWithSequentialKey(logsRoot, groupKey, keyPrefix, medEntry, title + " saved");
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Save an entry under a given groupKey using sequential key.
     * It scans existing keys under the group to find the highest index at the suffix, increments it,
     * and writes the new node
     *
     * After saving, onCompleteToast is shown and loadLogs() is called to refresh the UI.
     *
     * @param logsRoot       reference to children/{childId}/logs
     * @param groupKey       "PEF_log" / "controller_log" / "rescue_log"
     * @param keyPrefix      node prefix (e.g., "PEF", "controller", "rescue")
     * @param entryMap       data to write
     * @param onCompleteToast toast message shown on successful save
     */
    private void saveEntryWithSequentialKey(final DatabaseReference logsRoot,
                                            final String groupKey,
                                            final String keyPrefix,
                                            final Map<String, Object> entryMap,
                                            final String onCompleteToast) {
        DatabaseReference groupRef = logsRoot.child(groupKey);
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int maxIndex = 0;
                Pattern p = Pattern.compile("(\\d+)$");
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key == null) continue;
                    Matcher m = p.matcher(key);
                    if (m.find()) {
                        try {
                            int val = Integer.parseInt(m.group(1));
                            if (val > maxIndex) maxIndex = val;
                        } catch (NumberFormatException ignored) {}
                    }
                }
                int next = maxIndex + 1;
                String newKey = keyPrefix + next;

                groupRef.child(newKey).setValue(entryMap, (err, ref) -> {
                    if (err == null) {
                        Toast.makeText(requireContext(), onCompleteToast, Toast.LENGTH_LONG).show(); // CONTEXT FIX
                        loadLogs();
                        if ("PEF_log".equals(groupKey)) updateDailyPEF();
                    } else {
                        Toast.makeText(requireContext(), "Failed saving log: " + err.getMessage(), Toast.LENGTH_LONG).show(); // CONTEXT FIX
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to read existing logs: " + error.getMessage(), Toast.LENGTH_LONG).show(); // CONTEXT FIX
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // LOG DISPLAY METHOD
    // ─────────────────────────────────────────────────────────────────

    /**
     * Dynamically creates a UI element (LinearLayout) to display the details of a single log item.
     *
     * @param item The log item data (PEF, Controller, or Rescue) to display.
     */
    private void addLogItemToView(final LogItem item) {
        Map<String, Object> map = item.data;
        String headerText = item.category.toUpperCase();
        String dateText = "";
        long ts = item.timestamp;

        // Date formatting logic
        if (ts > 0) {
            Date date = new Date(ts);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateText = fmt.format(date);
        } else {
            dateText = safeToString(map.get("dateTime"), "");
        }

        // --- View Creation (Context fix: ChildLogsActivity.this -> requireContext()) ---
        LinearLayout itemLayout = new LinearLayout(requireContext());
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(10);
        itemLayout.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(6), 0, dpToPx(6));
        itemLayout.setLayoutParams(lp);

        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView header = new TextView(requireContext());
        header.setText(headerText + (dateText.isEmpty() ? "" : " — " + dateText));
        header.setTextSize(16f);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.setLayoutParams(headerLp);
        headerRow.addView(header);

        TextView deleteBtn = new TextView(requireContext());
        deleteBtn.setText("✖");
        deleteBtn.setTextSize(16f);
        deleteBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        deleteBtn.setClickable(true);
        deleteBtn.setFocusable(true);
        deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        deleteBtn.setOnClickListener(v -> confirmAndDelete(item));
        headerRow.addView(deleteBtn);

        itemLayout.addView(headerRow);

        // PEF Log Details
        if ("pef".equalsIgnoreCase(item.category)) {
            String val = safeToString(map.get("value"), "—");
            TextView v1 = new TextView(requireContext());
            v1.setText("PEF: " + val);
            itemLayout.addView(v1);

            String pre = safeToString(map.get("preMedVal"), "");
            String post = safeToString(map.get("postMedVal"), "");
            if (!pre.isEmpty()) {
                TextView preV = new TextView(requireContext());
                preV.setText("Pre-med value: " + pre);
                itemLayout.addView(preV);
            }
            if (!post.isEmpty()) {
                TextView postV = new TextView(requireContext());
                postV.setText("Post-med value: " + post);
                itemLayout.addView(postV);
            }

            String note = safeToString(map.get("note"), "");
            if (!note.isEmpty()) {
                TextView noteV = new TextView(requireContext());
                noteV.setText("Note: " + note);
                itemLayout.addView(noteV);
            }
        }
        // Controller Log Details
        else if ("controller".equalsIgnoreCase(item.category)) {
            String med = safeToString(map.get("medication"), "");
            String dose = safeToString(map.get("dose"), "");
            String units = safeToString(map.get("units"), "");
            String note = safeToString(map.get("note"), "");
            String pre = safeToString(map.get("preDose"), "");
            String post = safeToString(map.get("postDose"), "");
            if (!med.isEmpty()) {
                TextView medV = new TextView(requireContext());
                medV.setText("Medication: " + med);
                itemLayout.addView(medV);
            }
            if (!dose.isEmpty() || !units.isEmpty()) {
                TextView doseV = new TextView(requireContext());
                doseV.setText("Dose: " + dose + (units.isEmpty() ? "" : " " + units));
                itemLayout.addView(doseV);
            }
            if (!pre.isEmpty()) {
                TextView preV = new TextView(requireContext());
                preV.setText("Before: " + pre);
                itemLayout.addView(preV);
            }
            if (!post.isEmpty()) {
                TextView postV = new TextView(requireContext());
                postV.setText("After: " + post);
                itemLayout.addView(postV);
            }
            if (!note.isEmpty()) {
                TextView noteV = new TextView(requireContext());
                noteV.setText("Note: " + note);
                itemLayout.addView(noteV);
            }
        }
        // Rescue Log Details
        else if ("rescue".equalsIgnoreCase(item.category)) {
            String med = safeToString(map.get("medication"), "");
            String dose = safeToString(map.get("dose"), "");
            String units = safeToString(map.get("units"), "");
            String note = safeToString(map.get("note"), "");
            String pre = safeToString(map.get("preDose"), "");
            String post = safeToString(map.get("postDose"), "");
            if (!med.isEmpty()) {
                TextView medV = new TextView(requireContext());
                medV.setText("Medication: " + med);
                itemLayout.addView(medV);
            }
            if (!dose.isEmpty() || !units.isEmpty()) {
                TextView doseV = new TextView(requireContext());
                doseV.setText("Dose: " + dose + (units.isEmpty() ? "" : " " + units));
                itemLayout.addView(doseV);
            }
            if (!pre.isEmpty()) {
                TextView preV = new TextView(requireContext());
                preV.setText("Before: " + pre);
                itemLayout.addView(preV);
            }
            if (!post.isEmpty()) {
                TextView postV = new TextView(requireContext());
                postV.setText("After: " + post);
                itemLayout.addView(postV);
            }
            if (!note.isEmpty()) {
                TextView noteV = new TextView(requireContext());
                noteV.setText("Note: " + note);
                itemLayout.addView(noteV);
            }
        }
        // Generic Log Details (Fallback)
        else {
            for (Map.Entry<String,Object> e : map.entrySet()) {
                TextView tv = new TextView(requireContext());
                tv.setText(e.getKey() + ": " + String.valueOf(e.getValue()));
                itemLayout.addView(tv);
            }
        }

        TextView divider = new TextView(requireContext());
        divider.setText("────────────────────────────");
        divider.setPadding(0, dpToPx(6), 0, 0);
        itemLayout.addView(divider);

        // Add the new item to the container at the top
        logsContainer.addView(itemLayout, 0);
    }


    /**
     * Assumed placeholder method from original activity to load Firebase data.
     * Fetches logs from Firebase, filters them based on {@code currentFilter},
     * sorts them, clears the UI, and calls {@code addLogItemToView} for each.
     */
    private void loadLogs() {
        if (currentChildId == null) {
            Toast.makeText(requireContext(), "Cannot load logs: Child ID is missing.", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. Clear existing views before loading new ones
        if (logsContainer != null) {
            logsContainer.removeAllViews();
        }

        // 2. Fetch data from Firebase
        DatabaseReference childLogsRef = childrenRef.child(currentChildId).child("logs");

        childLogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LogItem> items = new ArrayList<>();

                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    String groupKey = groupSnapshot.getKey(); // e.g., PEF_log, rescue_log

                    for (DataSnapshot logSnapshot : groupSnapshot.getChildren()) {
                        try {
                            String nodeKey = logSnapshot.getKey();

                            @SuppressWarnings("unchecked")
                            Map<String, Object> logMap = (Map<String, Object>) logSnapshot.getValue();

                            if (logMap != null) {
                                String category = safeToString(logMap.get("type"), "unknown"); // Changed from category to type based on PEF/Med dialogs

                                // Check filter criteria
                                if (currentFilter.equals("all") || currentFilter.equalsIgnoreCase(category)) {
                                    long timestamp = 0;
                                    Object tsObj = logMap.get("timestamp");
                                    if (tsObj instanceof Long) {
                                        timestamp = (Long) tsObj;
                                    }

                                    // Create a copy for display data
                                    Map<String, Object> displayData = new HashMap<>(logMap);
                                    displayData.remove("type");
                                    displayData.remove("timestamp");

                                    items.add(new LogItem(nodeKey, category, timestamp, groupKey, nodeKey, displayData));
                                }
                            }
                        } catch (Exception e) {
                            // Log parsing error
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error parsing log data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }

                // 3. Sort logs by timestamp (newest first)
                Collections.sort(items, new Comparator<LogItem>() {
                    @Override
                    public int compare(LogItem o1, LogItem o2) {
                        // Descending order (newest first)
                        return Long.compare(o2.timestamp, o1.timestamp);
                    }
                });

                // 4. Populate UI or show empty message
                if (items.isEmpty()) {
                    // FIX: Changed ChildLogsActivity.this to requireContext()
                    TextView empty = new TextView(requireContext());
                    empty.setText("No logs yet. Use the + buttons below to add PEF, inhaler, or medicine logs.");
                    empty.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                    logsContainer.addView(empty);
                } else {
                    for (LogItem item : items) {
                        addLogItemToView(item);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load logs: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }



    // ───────────────────────────────
    // MENU HANDLING
    // ───────────────────────────────
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return MenuHelper.handleMenuSelection(item, this) || super.onOptionsItemSelected(item);
    }

}