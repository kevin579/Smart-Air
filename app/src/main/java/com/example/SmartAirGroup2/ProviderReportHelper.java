package com.example.SmartAirGroup2;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ProviderReportHelper {

    private final String uname;
    private final String name;
    private final DatabaseReference db;

    public ProviderReportHelper(String uname, String name) {
        this.uname = uname;
        this.name = name;
        this.db = FirebaseDatabase.getInstance().getReference();
    }

    // ============================================================
    // 1. Count Unique Symptom Days (asynchronous)
    // ============================================================
    public void countUniqueSymptomDays(String startDate, Consumer<Integer> callback) {
        long start = convertDateToLong(startDate);
        Set<String> days = new HashSet<>();

        DatabaseReference ref = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("data")
                .child("symptoms");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String date = snap.child("time").getValue(String.class);
                    if (date != null && convertDateToLong(date) >= start) {
                        days.add(date);
                    }
                }
                callback.accept(days.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProviderReportHelper", "Error fetching symptoms: " + error.getMessage());
                callback.accept(0);
            }
        });
    }

    // ============================================================
    // 2. controllerAdherence percentage (asynchronous)
    // ============================================================
    // Adherence % = (days with logged controller dose / planned dose days) × 100
    public void controllerAdherence(String startDate, Consumer<Double> callback) {
        long start = convertDateToLong(startDate);
        long totalDays = computeDaysBetween(startDate, today());

        if (totalDays == 0) {
            callback.accept(0.0);
            return;
        }

        DatabaseReference ref = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("logs")
                .child("controller_log");

        DatabaseReference med = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("controller_schedule");

        // First, fetch the medication schedule
        med.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot medSnapshot) {
                String scheduledMedication = medSnapshot.child("medication").getValue(String.class);

                // Count days per week from daysOfWeek
                int daysPerWeek = 0;
                DataSnapshot daysOfWeek = medSnapshot.child("daysOfWeek");
                if (daysOfWeek.exists()) {
                    for (DataSnapshot day : daysOfWeek.getChildren()) {
                        Boolean isActive = day.getValue(Boolean.class);
                        if (isActive != null && isActive) {
                            daysPerWeek++;
                        }
                    }
                }

                if (daysPerWeek == 0 || scheduledMedication == null) {
                    callback.accept(0.0);
                    return;
                }

                int finalDaysPerWeek = daysPerWeek;

                // Now fetch controller log entries
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Set<String> uniqueDays = new HashSet<>();

                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String dateTime = snap.child("dateTime").getValue(String.class);
                            String logMedication = snap.child("medication").getValue(String.class);

                            if (dateTime != null && convertDateTimeToLong(dateTime) >= start
                                    && logMedication != null && logMedication.equals(scheduledMedication)) {
                                // Extract date (first 10 characters: yyyy/MM/dd)
                                uniqueDays.add(dateTime.substring(0, 10));
                            }
                        }

                        // Scale by daysPerWeek/7
                        double expectedDays = totalDays * (finalDaysPerWeek / 7.0);
                        double adherencePercent = expectedDays > 0 ? (uniqueDays.size() * 100.0 / expectedDays) : 0.0;

                        callback.accept(adherencePercent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.accept(0.0);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.accept(0.0);
            }
        });
    }

    // ============================================================
    // 3. Rescue Frequency (asynchronous)
    // ============================================================
    public void countRescueFrequency(String startDate, Consumer<Double> callback) {
        long start = convertDateToLong(startDate);
        long totalDays = computeDaysBetween(startDate, today());

        if (totalDays == 0) {
            callback.accept(0.0);
            return;
        }

        DatabaseReference ref = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("logs")
                .child("rescue_log");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String dt = snap.child("dateTime").getValue(String.class);
                    if (dt != null && convertDateTimeToLong(dt) >= start) {
                        count++;
                    }
                }
                callback.accept((double) count / totalDays);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProviderReportHelper", "Error fetching rescue logs: " + error.getMessage());
                callback.accept(0.0);
            }
        });
    }

    // ============================================================
    // 4. Average PEF (asynchronous)
    // ============================================================
    public void averagePEF(String startDate, Consumer<Double> callback) {
        long start = convertDateToLong(startDate);

        DatabaseReference ref = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("logs")
                .child("PEF_log");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double sum = 0;
                int count = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String dt = snap.child("dateTime").getValue(String.class);
                    Double pef = snap.child("value").getValue(Double.class);
                    if (pef != null && dt != null && convertDateTimeToLong(dt) >= start) {
                        sum += pef;
                        count++;
                    }
                }
                callback.accept(count == 0 ? 0.0 : sum / count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProviderReportHelper", "Error fetching PEF: " + error.getMessage());
                callback.accept(0.0);
            }
        });
    }

    // ============================================================
    // 5. Triage Incidents (asynchronous)
    // ============================================================
    public void triageIncidents(String startDate, Consumer<Map<String, TriageIncident>> callback) {
        long start = convertDateToLong(startDate);
        Map<String, TriageIncident> incidents = new HashMap<>();

        DatabaseReference ref = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("data")
                .child("triages");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot root) {
                for (DataSnapshot snap : root.getChildren()) {
                    String date = snap.child("time").getValue(String.class);
                    if (date == null || convertDateToLong2(date) < start) continue;

                    TriageIncident t = new TriageIncident();
                    t.pef = snap.child("PEF").getValue(String.class);
                    t.guidance = snap.child("guidance").getValue(String.class);
                    t.response = snap.child("response").getValue(String.class);
                    t.time = snap.child("time").getValue(String.class);;

                    DataSnapshot red = snap.child("redflags");
                    if (red.exists()) {
                        t.redflags.put("Chest Pulling or Retraction", getBool(red, "Chest Pulling or Retraction"));
                        t.redflags.put("Grey or Blue lips", getBool(red, "Grey or Blue lips"));
                        t.redflags.put("Grey or Blue nails", getBool(red, "Grey or Blue nails"));
                        t.redflags.put("trouble breathing", getBool(red, "trouble breathing"));
                        t.redflags.put("trouble speaking", getBool(red, "trouble speaking"));
                    }
                    incidents.put(snap.getKey(), t);
                }
                callback.accept(incidents);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProviderReportHelper", "Error fetching triages: " + error.getMessage());
                callback.accept(incidents);
            }
        });
    }

    public void checkPermission(Consumer<Boolean> callback) {
        DatabaseReference ref = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("shareToProviderPermissions")
                .child("triage");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean hasPermission = snapshot.getValue(Boolean.class);
                callback.accept(hasPermission != null && hasPermission);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.accept(false); // default to false if error
            }
        });
    }



    private boolean getBool(DataSnapshot snap, String key) {
        Boolean v = snap.child(key).getValue(Boolean.class);
        return v != null && v;
    }

    // ============================================================
    // Triage Data Class
    // ============================================================
    public static class TriageIncident {
        public String pef;
        public String guidance;
        public String response;
        public String time;
        public Map<String, Boolean> redflags = new HashMap<>();
    }

    // ============================================================
    // Date Utilities
    // ============================================================
    long convertDateToLong(String date) {
        try {
            return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    .parse(date).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    long convertDateToLong2(String date) {
        if (date == null) return 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
            sdf.setLenient(false);
            return sdf.parse(date).getTime();
        } catch (Exception e) {
            Log.e("ProviderReportHelper", "Failed to parse date: " + date, e);
            return 0;
        }
    }


    private long convertDateTimeToLong(String dt) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .parse(dt).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    private long computeDaysBetween(String start, String end) {
        long s = convertDateToLong(start);
        long e = convertDateToLong(end);
        return (e - s) / (1000 * 60 * 60 * 24);
    }

    public String today() {
        return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(new Date());
    }

    public String reverseDate(String date, int monthsBack) {
        try {
            // Same format as your today() method
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date parsed = format.parse(date);

            Calendar cal = Calendar.getInstance();
            cal.setTime(parsed);

            // Subtract N months
            cal.add(Calendar.MONTH, -monthsBack);

            return format.format(cal.getTime());

        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    // ============================================================
    // 6. Compute Daily Average PEF Values (asynchronous)
    // ============================================================
    public void dailyAveragePEF(String startDate, Consumer<Map<String, Double>> callback) {
        long start = convertDateToLong(startDate);

        DatabaseReference ref = db.child("categories")
                .child("users")
                .child("children")
                .child(uname)
                .child("logs")
                .child("PEF_log");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Double> dailySum = new HashMap<>();
                Map<String, Integer> dailyCount = new HashMap<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    String dt = snap.child("dateTime").getValue(String.class);
                    Double pef = snap.child("value").getValue(Double.class);

                    if (dt != null && pef != null && convertDateTimeToLong(dt) >= start) {
                        String dateOnly = dt.substring(0, 10).replace("-", "/"); // yyyy/MM/dd

                        dailySum.put(dateOnly, dailySum.getOrDefault(dateOnly, 0.0) + pef);
                        dailyCount.put(dateOnly, dailyCount.getOrDefault(dateOnly, 0) + 1);
                    }
                }

                Map<String, Double> dailyAvg = new HashMap<>();
                for (String date : dailySum.keySet()) {
                    dailyAvg.put(date, dailySum.get(date) / dailyCount.get(date));
                }

                callback.accept(dailyAvg);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ProviderReportHelper", "Error fetching PEF logs: " + error.getMessage());
                callback.accept(new HashMap<>());
            }
        });
    }


}
