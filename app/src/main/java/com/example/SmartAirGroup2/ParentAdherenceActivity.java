package com.example.SmartAirGroup2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ParentAdherenceActivity
 *
 * Screen for parents to review their child's controller adherence and
 * configure the planned controller schedule.
 *
 * Layout:
 *   Uses activity_parent_adherence.xml which contains:
 *     - Overall adherence card:
 *         - tv_overall_adherence_value : last 7 days adherence in %
 *         - tv_overall_adherence_level : qualitative label
 *           ("Good", "Needs attention", "Poor")
 *     - Schedule card:
 *         - et_medication    : editable label for controller medication
 *         - et_times_per_day : planned doses per active day
 *         - cb_mon ... cb_sun: which weekdays are active
 *         - btn_save_schedule: saves the schedule to Firebase
 *
 * Data flow:
 *   1) Receives child identifiers via Intent extras:
 *        - "childUname" (required for Firebase path)
 *        - "childName"  (optional, for display if needed)
 *
 *   2) Loads existing schedule from:
 *        categories/users/children/{childUname}/controller_schedule
 *      and populates the schedule card fields.
 *
 *   3) When "Save schedule" is pressed:
 *        - Builds a ControllerSchedule instance from the form.
 *        - Writes it back to controller_schedule in Firebase.
 *        - Recomputes adherence to reflect the updated plan.
 *
 *   4) For adherence calculation:
 *        - Reads all controller_log entries for the child.
 *        - Calls AdherenceCalculator.calculate(...) with:
 *             - schedule
 *             - logs
 *             - last 7 days time window
 *        - Displays overallPercent and a qualitative level.
 *
 * Navigation:
 *   - Opened from the parent dashboard adherence card.
 *   - Shows an AppBar back button that simply finishes the activity.
 */

public class ParentAdherenceActivity extends AppCompatActivity {

    private String childUsername;

    private TextView tvAdherenceValue;
    private TextView tvAdherenceLevel;

    private EditText etMedication;
    private EditText etTimesPerDay;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private Button btnSaveSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_adherence);

        childUsername = getIntent().getStringExtra("childUsername");
        if (childUsername == null) {
            childUsername = "andy6687";
        }

        Toolbar toolbar = findViewById(R.id.toolbar_adherence);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvAdherenceValue = findViewById(R.id.tv_overall_adherence_value);
        tvAdherenceLevel = findViewById(R.id.tv_overall_adherence_level);

        etMedication = findViewById(R.id.et_medication);
        etTimesPerDay = findViewById(R.id.et_times_per_day);

        cbMon = findViewById(R.id.cb_mon);
        cbTue = findViewById(R.id.cb_tue);
        cbWed = findViewById(R.id.cb_wed);
        cbThu = findViewById(R.id.cb_thu);
        cbFri = findViewById(R.id.cb_fri);
        cbSat = findViewById(R.id.cb_sat);
        cbSun = findViewById(R.id.cb_sun);
        btnSaveSchedule = findViewById(R.id.btn_save_schedule);

        loadScheduleIntoForm();

        calculateAndShowAdherence();

        btnSaveSchedule.setOnClickListener(view -> saveSchedule());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private DatabaseReference getChildRef() {
        return FirebaseDatabase.getInstance()
                .getReference("categories")
                .child("users")
                .child("children")
                .child(childUsername);
    }

    private void loadScheduleIntoForm() {
        DatabaseReference scheduleRef = getChildRef().child("controller_schedule");

        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ControllerSchedule schedule = snapshot.getValue(ControllerSchedule.class);
                if (schedule == null) {
                    return;
                }

                etMedication.setText(schedule.medication);
                etTimesPerDay.setText(String.valueOf(schedule.timesPerDay));

                if (schedule.daysOfWeek != null) {
                    cbMon.setChecked(Boolean.TRUE.equals(schedule.daysOfWeek.get("MON")));
                    cbTue.setChecked(Boolean.TRUE.equals(schedule.daysOfWeek.get("TUE")));
                    cbWed.setChecked(Boolean.TRUE.equals(schedule.daysOfWeek.get("WED")));
                    cbThu.setChecked(Boolean.TRUE.equals(schedule.daysOfWeek.get("THU")));
                    cbFri.setChecked(Boolean.TRUE.equals(schedule.daysOfWeek.get("FRI")));
                    cbSat.setChecked(Boolean.TRUE.equals(schedule.daysOfWeek.get("SAT")));
                    cbSun.setChecked(Boolean.TRUE.equals(schedule.daysOfWeek.get("SUN")));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Adherence", "loadSchedule failed", error.toException());
            }
        });
    }

    private void saveSchedule() {
        String med = etMedication.getText().toString().trim();
        if (med.isEmpty()) {
            med = "Controller";
        }

        int times = 1;
        try {
            times = Integer.parseInt(etTimesPerDay.getText().toString());
        } catch (NumberFormatException e) {
            times = 1;
        }

        Map<String, Boolean> days = new HashMap<>();
        days.put("MON", cbMon.isChecked());
        days.put("TUE", cbTue.isChecked());
        days.put("WED", cbWed.isChecked());
        days.put("THU", cbThu.isChecked());
        days.put("FRI", cbFri.isChecked());
        days.put("SAT", cbSat.isChecked());
        days.put("SUN", cbSun.isChecked());

        ControllerSchedule schedule = new ControllerSchedule(med, times, days);

        getChildRef().child("controller_schedule").setValue(schedule)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show();
                        calculateAndShowAdherence();
                    } else {
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateAndShowAdherence() {
        DatabaseReference logRef = getChildRef()
                .child("logs")
                .child("controller_log");

        DatabaseReference scheduleRef = getChildRef()
                .child("controller_schedule");

        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ControllerSchedule schedule = snapshot.getValue(ControllerSchedule.class);

                logRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ControllerLog> logs = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Long ts = child.child("timestamp").getValue(Long.class);
                            String med = child.child("medication").getValue(String.class);
                            if (ts == null) continue;
                            logs.add(new ControllerLog(ts, med));
                        }

                        long endMillis = System.currentTimeMillis();
                        long startMillis = endMillis -6L * 24L * 60L * 60L * 1000L;

                        AdherenceCalculator.AdherenceResult result =AdherenceCalculator.calculate(schedule, logs, startMillis, endMillis);

                        double p = result.overallPercent;

                        String level;
                        if (p >= 80.0) {
                            level = "Good";
                        } else if (p >= 50.0) {
                            level ="Needs attention";
                        } else {
                            level = "Poor";
                        }

                        String valueText = String.format(Locale.getDefault(), "%.0f%%", p);
                        tvAdherenceValue.setText(valueText);
                        tvAdherenceLevel.setText(level);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Adherence", "loadLogs failed", error.toException());
                        tvAdherenceValue.setText("--");
                        tvAdherenceLevel.setText("");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Adherence", "loadSchedule failed", error.toException());
                tvAdherenceValue.setText("--");
                tvAdherenceLevel.setText("");
            }
        });
    }
}
