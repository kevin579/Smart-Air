package com.example.SmartAirGroup2;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
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

public class ChildLogsActivity extends AppCompatActivity {

    private String currentChildId = "andy6688";
    private FirebaseDatabase db;
    private DatabaseReference childrenRef;
    private LinearLayout logsContainer;

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_logs);

        currentChildId = getIntent().getStringExtra("childId");  // <-- ASSIGN IT HERE

        if (currentChildId == null) {
            Toast.makeText(this, "Error: Child ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        childrenRef = db.getReference("categories").child("users").child("children");

        logsContainer = findViewById(R.id.logsContainer);

        Button addInhalorButton = findViewById(R.id.addInhalor);
        Button addPEFButton = findViewById(R.id.addPEF);
        Button addMedicineButton = findViewById(R.id.addMedicine);

        // Inhaler: askForType = false (no medicine type)
        addInhalorButton.setOnClickListener(v -> showMedicationDialog("Log Inhaler Use", false));
        // Controller medicine: askForType = true
        addMedicineButton.setOnClickListener(v -> showMedicationDialog("Log Controller Medicine Use", true));
        addPEFButton.setOnClickListener(v -> showPEFDialog());

        // start listening and populate UI
        loadLogs();
    }

    /**
     * Load logs ordered by 'timestamp' and display newest-first (top).
     */
    private void loadLogs() {
        DatabaseReference logsRef = childrenRef.child(currentChildId).child("logs");
        // orderByChild("timestamp") requires that logs have "timestamp" property
        logsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // clear existing views
                logsContainer.removeAllViews();

                // collect snapshots into a list then reverse to show newest first
                List<DataSnapshot> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    items.add(child);
                }
                Collections.reverse(items); // newest-first

                for (DataSnapshot ds : items) {
                    // each ds represents a log object (Map)
                    Object val = ds.getValue();
                    if (!(val instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) val;

                    String type = safeToString(map.get("type"), "log");
                    // allow either 'timestamp' (long) or 'dateTime' (string) display
                    String dateDisplay = "";
                    if (map.get("timestamp") != null) {
                        try {
                            long ts = Long.parseLong(String.valueOf(map.get("timestamp")));
                            Date date = new Date(ts);
                            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            dateDisplay = fmt.format(date);
                        } catch (Exception ignored) { dateDisplay = safeToString(map.get("dateTime"), ""); }
                    } else {
                        dateDisplay = safeToString(map.get("dateTime"), "");
                    }

                    // create a container for this log item
                    LinearLayout itemLayout = new LinearLayout(ChildLogsActivity.this);
                    itemLayout.setOrientation(LinearLayout.VERTICAL);
                    int pad = dpToPx(10);
                    itemLayout.setPadding(pad, pad, pad, pad);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, dpToPx(6), 0, dpToPx(6));
                    itemLayout.setLayoutParams(lp);

                    // Header: type + date
                    TextView header = new TextView(ChildLogsActivity.this);
                    header.setText(type.toUpperCase() + (dateDisplay.isEmpty() ? "" : " — " + dateDisplay));
                    header.setTextSize(16f);
                    header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
                    itemLayout.addView(header);

                    // Details: different depending on type
                    if ("pef".equalsIgnoreCase(type)) {
                        // show value and note
                        Object valueObj = map.get("value");
                        String valueStr = valueObj == null ? "—" : String.valueOf(valueObj);
                        TextView line1 = new TextView(ChildLogsActivity.this);
                        line1.setText("PEF: " + valueStr);
                        itemLayout.addView(line1);

                        String note = safeToString(map.get("note"), "");
                        if (!note.isEmpty()) {
                            TextView noteView = new TextView(ChildLogsActivity.this);
                            noteView.setText("Note: " + note);
                            itemLayout.addView(noteView);
                        }
                    } else if ("medication".equalsIgnoreCase(type) || "inhaler".equalsIgnoreCase(type)) {
                        String med = safeToString(map.get("medication"), (String) map.get("medication"));
                        String dose = safeToString(map.get("dose"), "");
                        String dosageNum = map.get("dosage") == null ? "" : String.valueOf(map.get("dosage"));
                        String units = safeToString(map.get("units"), "");
                        String pre = safeToString(map.get("preDose"), "");
                        String post = safeToString(map.get("postDose"), "");
                        String note = safeToString(map.get("note"), "");

                        if (!med.isEmpty()) {
                            TextView medView = new TextView(ChildLogsActivity.this);
                            medView.setText("Medication: " + med);
                            itemLayout.addView(medView);
                        }

                        if (!dose.isEmpty() || !dosageNum.isEmpty() || !units.isEmpty()) {
                            TextView doseView = new TextView(ChildLogsActivity.this);
                            String dText = !dose.isEmpty() ? dose : (dosageNum.isEmpty() ? "—" : (dosageNum + (units.isEmpty() ? "" : " " + units)));
                            doseView.setText("Dose: " + dText);
                            itemLayout.addView(doseView);
                        }

                        if (!pre.isEmpty()) {
                            TextView preView = new TextView(ChildLogsActivity.this);
                            preView.setText("Before: " + pre);
                            itemLayout.addView(preView);
                        }
                        if (!post.isEmpty()) {
                            TextView postView = new TextView(ChildLogsActivity.this);
                            postView.setText("After: " + post);
                            itemLayout.addView(postView);
                        }
                        if (!note.isEmpty()) {
                            TextView noteView = new TextView(ChildLogsActivity.this);
                            noteView.setText("Note: " + note);
                            itemLayout.addView(noteView);
                        }
                    } else {
                        // Generic fallback: show whole map as text (safe)
                        for (Map.Entry<String, Object> e : map.entrySet()) {
                            TextView tv = new TextView(ChildLogsActivity.this);
                            tv.setText(e.getKey() + ": " + String.valueOf(e.getValue()));
                            itemLayout.addView(tv);
                        }
                    }

                    // add a divider (simple)
                    TextView divider = new TextView(ChildLogsActivity.this);
                    divider.setText("────────────────────────────");
                    divider.setPadding(0, dpToPx(6), 0, 0);
                    itemLayout.addView(divider);

                    // finally add item to container
                    logsContainer.addView(itemLayout);
                }

                // If there are no logs, show a friendly message
                if (items.isEmpty()) {
                    TextView empty = new TextView(ChildLogsActivity.this);
                    empty.setText("No logs yet. Use the buttons below to add PEF, inhaler, or medicine logs.");
                    empty.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
                    logsContainer.addView(empty);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChildLogsActivity.this,
                        "Failed to load logs: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safeToString(Object o, String def) {
        if (o == null) return def == null ? "" : def;
        String s = String.valueOf(o);
        return s.equals("null") ? (def == null ? "" : def) : s;
    }

    /********* existing dialog and save code follows (unchanged except for adding timestamp) *********/

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

        final Calendar calendar = Calendar.getInstance(); // initialized to now
        setDateTimeEditTextFromCalendar(timeInput, calendar);

        timeInput.setOnClickListener(v -> pickDateTime(calendar, timeInput));

        new AlertDialog.Builder(this)
                .setTitle("Record Peak Flow")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String peak = peakFlowInput.getText().toString().trim();
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

                    Map<String, Object> pefLog = new HashMap<>();
                    pefLog.put("type", "pef");
                    pefLog.put("timestamp", timestamp);                    // <-- added timestamp
                    pefLog.put("dateTime", dateTime);
                    pefLog.put("value", peakVal);
                    pefLog.put("note", notes.isEmpty() ? "" : notes);
                    pefLog.put("preDose", null);
                    pefLog.put("postDose", null);

                    DatabaseReference childrenRef = db.getReference("categories").child("users").child("children");
                    childrenRef.child(currentChildId).child("logs").push().setValue(pefLog,
                            (databaseError, databaseReference) -> {
                                if (databaseError == null) {
                                    Toast.makeText(this,
                                            "Saved PEF: " + peakVal + " at " + dateTime,
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this,
                                            "Failed saving PEF: " + databaseError.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
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

        EditText finalTypeInput = typeInput; // for lambda capture

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

                    Map<String, Object> medLog = new HashMap<>();
                    medLog.put("type", askForType ? "medication" : "inhaler");
                    medLog.put("timestamp", timestamp);                    // <-- added timestamp
                    medLog.put("dateTime", dateTime);
                    medLog.put("medication", askForType ? type : "Inhaler");
                    medLog.put("dosage", doseVal == null ? null : doseVal);
                    medLog.put("dose", (doseVal == null ? "" : doseVal) + (units.isEmpty() ? "" : (" " + units)).trim());
                    medLog.put("units", units.isEmpty() ? "" : units);
                    medLog.put("preDose", before.isEmpty() ? "" : before);
                    medLog.put("postDose", after.isEmpty() ? "" : after);
                    medLog.put("note", notes.isEmpty() ? "" : notes);

                    DatabaseReference childrenRef = db.getReference("categories").child("users").child("children");
                    childrenRef.child(currentChildId).child("logs").push().setValue(medLog,
                            (databaseError, databaseReference) -> {
                                if (databaseError == null) {
                                    Toast.makeText(ChildLogsActivity.this, title + " saved", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(ChildLogsActivity.this,
                                            "Failed saving log: " + databaseError.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
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
