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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ProviderAdherenceActivity
 *
 * Read-only version of the controller adherence screen for providers.
 * This activity reuses the same layout as ParentAdherenceActivity
 * (activity_parent_adherence.xml), but disables all editing and hides
 * the save button.
 *
 * Purpose:
 *   - Allow providers to see a summary of a child's planned controller
 *     schedule and their recent adherence without changing the plan.
 *
 * Behavior:
 *   - Receives "childUname" (and optionally "childName") via Intent extras.
 *   - Loads ControllerSchedule from:
 *       categories/users/children/{childUname}/controller_schedule
 *     and populates:
 *       - et_medication
 *       - et_times_per_day
 *       - weekday CheckBoxes
 *     All of these fields are set to read-only (setEnabled(false)).
 *
 *   - Hides the "Save schedule" button so providers cannot modify
 *     the parent's controller plan.
 *
 *   - Computes last 7 days controller adherence using the same
 *     AdherenceCalculator as the parent side, by reading:
 *       categories/users/children/{childUname}/logs/controller_log
 *     and showing:
 *       - tv_overall_adherence_value (percentage)
 *       - tv_overall_adherence_level (Good / Needs attention / Poor).
 *
 * Navigation:
 *   - Opened from the provider dashboard adherence card, subject to
 *     the provider's "controllerAdherence" permission.
 *   - AppBar back button finishes the activity and returns to the
 *     provider dashboard.
 */

public class ProviderAdherenceActivity extends AppCompatActivity {

    private String childUname;

    private TextView tvAdherenceValue;
    private TextView tvAdherenceLevel;

    private EditText etMedication;
    private EditText etTimesPerDay;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_adherence);

        childUname = getIntent().getStringExtra("childUname");
        if (childUname == null) {
            childUname = "test_child";
        }

        Toolbar toolbar = findViewById(R.id.toolbar_adherence);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Controller Adherence");
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

        Button btnSave = findViewById(R.id.btn_save_schedule);
        if (btnSave != null) {
            btnSave.setVisibility(Button.GONE);
        }

        etMedication.setEnabled(false);
        etTimesPerDay.setEnabled(false);
        cbMon.setEnabled(false);
        cbTue.setEnabled(false);
        cbWed.setEnabled(false);
        cbThu.setEnabled(false);
        cbFri.setEnabled(false);
        cbSat.setEnabled(false);
        cbSun.setEnabled(false);

        loadScheduleIntoForm();

        calculateAndShowAdherence();
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
                .child(childUname);
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
                Log.e("ProvAdherence", "loadSchedule failed", error.toException());
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
                        List<ControllerLog> logs = new ArrayList<ControllerLog>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Long ts = child.child("timestamp").getValue(Long.class);
                            String med = child.child("medication").getValue(String.class);
                            if (ts == null) continue;
                            logs.add(new ControllerLog(ts.longValue(), med));
                        }

                        long endMillis = System.currentTimeMillis();
                        long startMillis = endMillis - 6L * 24L * 60L* 60L * 1000L;

                        AdherenceCalculator.AdherenceResult result =
                                AdherenceCalculator.calculate(schedule, logs, startMillis, endMillis);

                        double p = result.overallPercent;

                        String level;
                        if (p >= 80.0) {
                            level = "Good";
                        } else if (p >= 50.0) {
                            level = "Needs attention";
                        } else {
                            level = "Poor";
                        }

                        String valueText = String.format(Locale.getDefault(), "%.0f%%", p);
                        tvAdherenceValue.setText(valueText);
                        tvAdherenceLevel.setText(level);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ProvAdherence", "loadLogs failed", error.toException());
                        tvAdherenceValue.setText("--");
                        tvAdherenceLevel.setText("");
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProvAdherence", "loadSchedule failed", error.toException());
                tvAdherenceValue.setText("--");
                tvAdherenceLevel.setText("");
            }
        });
    }
}
