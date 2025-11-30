package com.example.SmartAirGroup2;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChildLogsActivity extends AppCompatActivity {

    private String currentChildId = "andy6688";
    private FirebaseDatabase db;
    private DatabaseReference childrenRef;
    private LinearLayout logsContainer;

    private String currentFilter = "all";
    // "all", "pef", "controller", "rescue"

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_logs);

        //try to get childId from intent from caller
        String intentChild = getIntent() == null ? null : getIntent().getStringExtra("childId");
        if (intentChild != null && !intentChild.isEmpty()) currentChildId = intentChild;

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        childrenRef = db.getReference("categories").child("users").child("children");

        logsContainer = findViewById(R.id.logsContainer);

        Button addInhalorButton = findViewById(R.id.addInhalor);
        Button addPEFButton = findViewById(R.id.addPEF);
        Button addMedicineButton = findViewById(R.id.addMedicine);

        Button filterAll = findViewById(R.id.filterAll);
        Button filterPEF = findViewById(R.id.filterPEF);
        Button filterController = findViewById(R.id.filterController);
        Button filterRescue = findViewById(R.id.filterRescue);

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
            currentFilter = "controller";
            loadLogs();
        });
        filterRescue.setOnClickListener(v -> {
            currentFilter = "rescue";
            loadLogs();
        });

        addInhalorButton.setOnClickListener(v -> showMedicationDialog("Log Inhaler Use", false));
        addMedicineButton.setOnClickListener(v -> showMedicationDialog("Log Controller Medicine Use", true));
        addPEFButton.setOnClickListener(v -> showPEFDialog());

        loadLogs();
    }

    private void loadLogs() {
        logsContainer.removeAllViews();

        DatabaseReference logsRoot = childrenRef.child(currentChildId).child("logs");

        logsRoot.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot rootSnap) {
                List<LogItem> items = new ArrayList<>();

                DataSnapshot pefSnap = rootSnap.child("PEF_log");
                for (DataSnapshot s : pefSnap.getChildren()) {
                    String nodeKey = s.getKey();
                    Map<String, Object> map = snapshotToMap(s);
                    long ts = getTimestampFromMap(map);
                    items.add(new LogItem("pef", ts, map, "PEF_log", nodeKey));
                }

                DataSnapshot ctrlSnap = rootSnap.child("controller_log");
                for (DataSnapshot s : ctrlSnap.getChildren()) {
                    String nodeKey = s.getKey();
                    Map<String, Object> map = snapshotToMap(s);
                    long ts = getTimestampFromMap(map);
                    items.add(new LogItem("controller", ts, map, "controller_log", nodeKey));
                }

                DataSnapshot rescueSnap = rootSnap.child("rescue_log");
                for (DataSnapshot s : rescueSnap.getChildren()) {
                    String nodeKey = s.getKey();
                    Map<String, Object> map = snapshotToMap(s);
                    long ts = getTimestampFromMap(map);
                    items.add(new LogItem("rescue", ts, map, "rescue_log", nodeKey));
                }

                Collections.sort(items, (a, b) -> Long.compare(a.timestamp, b.timestamp));

                logsContainer.removeAllViews();
                for (LogItem item : items) {
                    if (!"all".equals(currentFilter) && !item.category.equalsIgnoreCase(currentFilter)) continue;
                    addLogItemToView(item);
                }

                if (items.isEmpty()) {
                    TextView empty = new TextView(ChildLogsActivity.this);
                    empty.setText("No logs yet. Use the + buttons below to add PEF, inhaler, or medicine logs.");
                    empty.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                    logsContainer.addView(empty);
                }

                updateDailyPEF();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChildLogsActivity.this,
                        "Failed to load logs: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class LogItem {
        String category;
        //"pef", "controller", "rescue"

        long timestamp;
        Map<String, Object> data;
        String groupKey;
        //"PEF_log" / "controller_log" / "rescue_log"
        String nodeKey;
        //"PEF1", "controller3"
        LogItem(String c, long t, Map<String, Object> d, String g, String n) { category = c; timestamp = t; data = d; groupKey = g; nodeKey = n; }
    }

    private Map<String, Object> snapshotToMap(DataSnapshot entry) {
        Object v = entry.getValue();
        if (v instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) v;
            return map;
        } else {
            return new HashMap<>();
        }
    }

    private long getTimestampFromMap(Map<String, Object> map) {
        if (map == null) return 0;
        Object t = map.get("timestamp");
        if (t == null) {
            Object dt = map.get("dateTime");
            if (dt != null) {
                try {
                    String s = dt.toString();
                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    Date d = fmt.parse(s);
                    if (d != null) return d.getTime();
                } catch (Exception ignored) { }
            }
            return 0;
        } else {
            try {
                return Long.parseLong(String.valueOf(t));
            } catch (Exception e) { return 0; }
        }
    }

    private void addLogItemToView(final LogItem item) {
        Map<String, Object> map = item.data;
        String headerText = item.category.toUpperCase();
        String dateText = "";
        long ts = item.timestamp;
        if (ts > 0) {
            Date date = new Date(ts);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateText = fmt.format(date);
        } else {
            dateText = safeToString(map.get("dateTime"), "");
        }

        LinearLayout itemLayout = new LinearLayout(ChildLogsActivity.this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(10);
        itemLayout.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(6), 0, dpToPx(6));
        itemLayout.setLayoutParams(lp);

        LinearLayout headerRow = new LinearLayout(ChildLogsActivity.this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView header = new TextView(ChildLogsActivity.this);
        header.setText(headerText + (dateText.isEmpty() ? "" : " — " + dateText));
        header.setTextSize(16f);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.setLayoutParams(headerLp);
        headerRow.addView(header);

        TextView deleteBtn = new TextView(ChildLogsActivity.this);
        deleteBtn.setText("✖");
        deleteBtn.setTextSize(16f);
        deleteBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        deleteBtn.setClickable(true);
        deleteBtn.setFocusable(true);
        deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        deleteBtn.setOnClickListener(v -> confirmAndDelete(item));
        headerRow.addView(deleteBtn);

        itemLayout.addView(headerRow);

        if ("pef".equalsIgnoreCase(item.category)) {
            String val = safeToString(map.get("value"), "—");
            TextView v1 = new TextView(ChildLogsActivity.this);
            v1.setText("PEF: " + val);
            itemLayout.addView(v1);

            String pre = safeToString(map.get("preMedVal"), "");
            String post = safeToString(map.get("postMedVal"), "");
            if (!pre.isEmpty()) {
                TextView preV = new TextView(ChildLogsActivity.this);
                preV.setText("Pre-med value: " + pre);
                itemLayout.addView(preV);
            }
            if (!post.isEmpty()) {
                TextView postV = new TextView(ChildLogsActivity.this);
                postV.setText("Post-med value: " + post);
                itemLayout.addView(postV);
            }

            String note = safeToString(map.get("note"), "");
            if (!note.isEmpty()) {
                TextView noteV = new TextView(ChildLogsActivity.this);
                noteV.setText("Note: " + note);
                itemLayout.addView(noteV);
            }
        } else if ("controller".equalsIgnoreCase(item.category)) {
            String med = safeToString(map.get("medication"), "");
            String dose = safeToString(map.get("dose"), "");
            String units = safeToString(map.get("units"), "");
            String note = safeToString(map.get("note"), "");
            String pre = safeToString(map.get("preDose"), "");
            String post = safeToString(map.get("postDose"), "");
            if (!med.isEmpty()) {
                TextView medV = new TextView(ChildLogsActivity.this);
                medV.setText("Medication: " + med);
                itemLayout.addView(medV);
            }
            if (!dose.isEmpty() || !units.isEmpty()) {
                TextView doseV = new TextView(ChildLogsActivity.this);
                doseV.setText("Dose: " + dose + (units.isEmpty() ? "" : " " + units));
                itemLayout.addView(doseV);
            }
            if (!pre.isEmpty()) {
                TextView preV = new TextView(ChildLogsActivity.this);
                preV.setText("Before: " + pre);
                itemLayout.addView(preV);
            }
            if (!post.isEmpty()) {
                TextView postV = new TextView(ChildLogsActivity.this);
                postV.setText("After: " + post);
                itemLayout.addView(postV);
            }
            if (!note.isEmpty()) {
                TextView noteV = new TextView(ChildLogsActivity.this);
                noteV.setText("Note: " + note);
                itemLayout.addView(noteV);
            }
        } else if ("rescue".equalsIgnoreCase(item.category)) {
            String med = safeToString(map.get("medication"), "");
            String dose = safeToString(map.get("dose"), "");
            String units = safeToString(map.get("units"), "");
            String note = safeToString(map.get("note"), "");
            String pre = safeToString(map.get("preDose"), "");
            String post = safeToString(map.get("postDose"), "");
            if (!med.isEmpty()) {
                TextView medV = new TextView(ChildLogsActivity.this);
                medV.setText("Medication: " + med);
                itemLayout.addView(medV);
            }
            if (!dose.isEmpty() || !units.isEmpty()) {
                TextView doseV = new TextView(ChildLogsActivity.this);
                doseV.setText("Dose: " + dose + (units.isEmpty() ? "" : " " + units));
                itemLayout.addView(doseV);
            }
            if (!pre.isEmpty()) {
                TextView preV = new TextView(ChildLogsActivity.this);
                preV.setText("Before: " + pre);
                itemLayout.addView(preV);
            }
            if (!post.isEmpty()) {
                TextView postV = new TextView(ChildLogsActivity.this);
                postV.setText("After: " + post);
                itemLayout.addView(postV);
            }
            if (!note.isEmpty()) {
                TextView noteV = new TextView(ChildLogsActivity.this);
                noteV.setText("Note: " + note);
                itemLayout.addView(noteV);
            }
        } else {
            for (Map.Entry<String,Object> e : map.entrySet()) {
                TextView tv = new TextView(ChildLogsActivity.this);
                tv.setText(e.getKey() + ": " + String.valueOf(e.getValue()));
                itemLayout.addView(tv);
            }
        }

        TextView divider = new TextView(ChildLogsActivity.this);
        divider.setText("────────────────────────────");
        divider.setPadding(0, dpToPx(6), 0, 0);
        itemLayout.addView(divider);

        logsContainer.addView(itemLayout, 0);
    }

    private void confirmAndDelete(final LogItem item) {
        new AlertDialog.Builder(ChildLogsActivity.this)
                .setTitle("Delete Log")
                .setMessage("Are you sure you want to delete this log?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DatabaseReference nodeRef = childrenRef.child(currentChildId).child("logs").child(item.groupKey).child(item.nodeKey);
                    nodeRef.removeValue((err, ref) -> {
                        if (err == null) {
                            Toast.makeText(ChildLogsActivity.this, "Log deleted", Toast.LENGTH_SHORT).show();
                            loadLogs();
                            if ("PEF_log".equals(item.groupKey)) updateDailyPEF();
                        } else {
                            Toast.makeText(ChildLogsActivity.this, "Failed to delete: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private String safeToString(Object o, String def) {
        if (o == null) return def == null ? "" : def;
        String s = String.valueOf(o);
        return s.equals("null") ? (def == null ? "" : def) : s;
    }

    private void showPEFDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(12);
        layout.setPadding(pad, pad, pad, pad);

        TextView peakLabel = new TextView(this);
        peakLabel.setText("Peak Flow (L/min):");
        layout.addView(peakLabel);

        EditText peakFlowInput = new EditText(this);
        peakFlowInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        peakFlowInput.setHint("e.g. 320");
        layout.addView(peakFlowInput);

        TextView preMedLabel = new TextView(this);
        preMedLabel.setText("Pre-med value (optional):");
        layout.addView(preMedLabel);
        EditText preMedInput = new EditText(this);
        preMedInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        preMedInput.setHint("e.g. 300 (optional)");
        layout.addView(preMedInput);

        TextView postMedLabel = new TextView(this);
        postMedLabel.setText("Post-med value (optional):");
        layout.addView(postMedLabel);
        EditText postMedInput = new EditText(this);
        postMedInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        postMedInput.setHint("e.g. 320 (optional)");
        layout.addView(postMedInput);

        TextView notesLabel = new TextView(this);
        notesLabel.setText("Notes (optional):");
        layout.addView(notesLabel);

        EditText notesInput = new EditText(this);
        notesInput.setHint("e.g. taken after exercise");
        layout.addView(notesInput);

        TextView timeLabel = new TextView(this);
        timeLabel.setText("Date & Time:");
        layout.addView(timeLabel);

        EditText timeInput = new EditText(this);
        timeInput.setFocusable(false);
        timeInput.setClickable(true);
        timeInput.setHint("Tap to pick date and time");
        layout.addView(timeInput);

        final Calendar calendar = Calendar.getInstance();
        setDateTimeEditTextFromCalendar(timeInput, calendar);

        timeInput.setOnClickListener(v -> pickDateTime(calendar, timeInput));

        new AlertDialog.Builder(this)
                .setTitle("Record Peak Flow")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String peak = peakFlowInput.getText().toString().trim();
                    String pre = preMedInput.getText().toString().trim();
                    String post = postMedInput.getText().toString().trim();
                    String notes = notesInput.getText().toString().trim();
                    String dateTime = timeInput.getText().toString().trim();

                    if (peak.isEmpty()) {
                        Toast.makeText(this, "Please enter peak flow.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int peakVal;
                    try {
                        peakVal = Integer.parseInt(peak);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Peak flow must be a number.", Toast.LENGTH_SHORT).show();
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

    private void showMedicationDialog(String title, boolean askForType) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(12);
        layout.setPadding(pad, pad, pad, pad);

        TextView beforeLabel = new TextView(this);
        beforeLabel.setText("How you felt before (optional):");
        layout.addView(beforeLabel);

        EditText beforeInput = new EditText(this);
        beforeInput.setHint("e.g. breathless, OK");
        layout.addView(beforeInput);

        TextView afterLabel = new TextView(this);
        afterLabel.setText("How you felt after (optional):");
        layout.addView(afterLabel);

        EditText afterInput = new EditText(this);
        afterInput.setHint("e.g. improved, same");
        layout.addView(afterInput);

        EditText typeInput = null;
        if (askForType) {
            TextView typeLabel = new TextView(this);
            typeLabel.setText("Type of Medicine:");
            layout.addView(typeLabel);

            typeInput = new EditText(this);
            typeInput.setHint("e.g. Salbutamol, Fluticasone");
            layout.addView(typeInput);
        }

        TextView doseLabel = new TextView(this);
        doseLabel.setText("Dosage (optional):");
        layout.addView(doseLabel);

        EditText doseInput = new EditText(this);
        doseInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        doseInput.setHint("e.g. 2");
        layout.addView(doseInput);

        TextView unitsLabel = new TextView(this);
        unitsLabel.setText("Units (optional):");
        layout.addView(unitsLabel);

        EditText unitsInput = new EditText(this);
        unitsInput.setHint("e.g. puffs, mcg, mg");
        layout.addView(unitsInput);

        TextView notesLabel = new TextView(this);
        notesLabel.setText("Notes (optional):");
        layout.addView(notesLabel);

        EditText notesInput = new EditText(this);
        notesInput.setHint("e.g. used before exercise");
        layout.addView(notesInput);

        TextView timeLabel = new TextView(this);
        timeLabel.setText("Date & Time of Use:");
        layout.addView(timeLabel);

        EditText timeInput = new EditText(this);
        timeInput.setFocusable(false);
        timeInput.setClickable(true);
        timeInput.setHint("Tap to pick date and time");
        layout.addView(timeInput);

        final Calendar calendar = Calendar.getInstance();
        setDateTimeEditTextFromCalendar(timeInput, calendar);

        timeInput.setOnClickListener(v -> pickDateTime(calendar, timeInput));

        EditText finalTypeInput = typeInput;

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String before = beforeInput.getText().toString().trim();
                    String after = afterInput.getText().toString().trim();
                    String type;
                    if (askForType) {
                        type = finalTypeInput.getText().toString().trim();
                        if (type.isEmpty()) {
                            Toast.makeText(this, "Please enter medicine type.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this, "Dosage must be a number.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(ChildLogsActivity.this, onCompleteToast, Toast.LENGTH_LONG).show();
                        loadLogs();
                        if ("PEF_log".equals(groupKey)) updateDailyPEF();
                    } else {
                        Toast.makeText(ChildLogsActivity.this, "Failed saving log: " + err.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChildLogsActivity.this, "Failed to read existing logs: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

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
                        Toast.makeText(ChildLogsActivity.this, "Failed to update dailyPEF: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // ignore
            }
        });
    }

    private void pickDateTime(final Calendar calendar, final EditText targetEditText) {
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            TimePickerDialog tp = new TimePickerDialog(ChildLogsActivity.this,
                    (timeView, hourOfDay, minute1) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute1);
                        setDateTimeEditTextFromCalendar(targetEditText, calendar);
                    }, hour, minute, true);
            tp.show();

        }, y, m, d);

        dp.show();
    }

    private void setDateTimeEditTextFromCalendar(EditText et, Calendar cal) {
        Date date = cal.getTime();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        et.setText(fmt.format(date));
    }
}
