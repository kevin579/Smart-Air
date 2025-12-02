package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ProviderRescueLogsFragment
 *
 * Read-only fragment for provider users that shows ONLY rescue (inhaler) logs for a given child.
 * - No add / edit / delete controls.
 * - Only displays logs where type == "inhaler" or "rescue".
 *
 * Usage: ProviderRescueLogsFragment.newInstance(childId)
 */
public class ProviderRescueLogsFragment extends Fragment {

    private String currentChildId;
    private FirebaseDatabase db;
    private DatabaseReference childrenRef;
    private LinearLayout logsContainer;
    private Toolbar toolbar;

    public static ProviderRescueLogsFragment newInstance(String childId) {
        ProviderRescueLogsFragment frag = new ProviderRescueLogsFragment();
        Bundle args = new Bundle();
        args.putString("childUname", childId);
        frag.setArguments(args);
        return frag;
    }

    private static class LogItem {
        public String key;
        public String category;
        public long timestamp;
        public String groupKey;
        public String nodeKey;
        public Map<String, Object> data;

        public LogItem(String key, String category, long timestamp, String groupKey, String nodeKey, Map<String, Object> data) {
            this.key = key;
            this.category = category;
            this.timestamp = timestamp;
            this.groupKey = groupKey;
            this.nodeKey = nodeKey;
            this.data = data;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentChildId = getArguments().getString("childUname");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_provider_rescue_logs, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        toolbar.setTitle("Provider — Rescue Logs");
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (currentChildId == null) {
            Toast.makeText(requireContext(), "Error: Child ID missing.", Toast.LENGTH_LONG).show();
            return view;
        }

        // Initialize Firebase
        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        childrenRef = db.getReference("categories").child("users").child("children");

        logsContainer = view.findViewById(R.id.logsContainer);

        // load only rescue logs (read-only)
        loadRescueLogs();

        return view;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String safeToString(Object o, String def) {
        if (o == null) return def == null ? "" : def;
        String s = String.valueOf(o);
        return s.equals("null") ? (def == null ? "" : def) : s;
    }

    private long getTimestampFromMap(Map<String, Object> map) {
        Object tsObj = map.get("timestamp");
        if (tsObj instanceof Long) return (Long) tsObj;
        if (tsObj instanceof Integer) return ((Integer) tsObj).longValue();
        if (tsObj instanceof Double) return ((Double) tsObj).longValue();
        return 0;
    }

    private void addLogItemToView(final LogItem item) {
        Map<String, Object> map = item.data;
        String headerText = item.category != null ? item.category.toUpperCase() : "RESCUE";
        String dateText = "";
        long ts = item.timestamp;

        if (ts > 0) {
            Date date = new Date(ts);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            dateText = fmt.format(date);
        } else {
            dateText = safeToString(map.get("dateTime"), "");
        }

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

        itemLayout.addView(headerRow);

        // Display rescue-specific details (medication, dose, before/after, note)
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

        TextView divider = new TextView(requireContext());
        divider.setText("────────────────────────────");
        divider.setPadding(0, dpToPx(6), 0, 0);
        itemLayout.addView(divider);

        logsContainer.addView(itemLayout, 0);
    }

    /**
     * Loads logs and filters to rescue/inhaler only.
     * Read-only: no delete/save actions.
     */
    private void loadRescueLogs() {
        if (currentChildId == null) {
            Toast.makeText(requireContext(), "Cannot load logs: Child ID missing.", Toast.LENGTH_LONG).show();
            return;
        }

        if (logsContainer != null) logsContainer.removeAllViews();

        DatabaseReference childLogsRef = childrenRef.child(currentChildId).child("logs");
        childLogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LogItem> items = new ArrayList<>();

                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    String groupKey = groupSnapshot.getKey(); // e.g., PEF_log, rescue_log, controller_log

                    for (DataSnapshot logSnapshot : groupSnapshot.getChildren()) {
                        try {
                            String nodeKey = logSnapshot.getKey();

                            @SuppressWarnings("unchecked")
                            Map<String, Object> logMap = (Map<String, Object>) logSnapshot.getValue();

                            if (logMap != null) {
                                String type = safeToString(logMap.get("type"), "").toLowerCase();

                                // Accept both explicit "rescue"/"inhaler" or group key rescue_log
                                boolean isRescue = "rescue".equalsIgnoreCase(type)
                                        || "inhaler".equalsIgnoreCase(type)
                                        || (groupKey != null && groupKey.toLowerCase().contains("rescue"));

                                if (!isRescue) continue; // skip non-rescue logs

                                long timestamp = getTimestampFromMap(logMap);

                                Map<String, Object> displayData = new HashMap<>(logMap);
                                displayData.remove("type");
                                displayData.remove("timestamp");

                                items.add(new LogItem(nodeKey, "rescue", timestamp, groupKey, nodeKey, displayData));
                            }
                        } catch (Exception e) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error parsing log: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }

                // sort newest first
                Collections.sort(items, new Comparator<LogItem>() {
                    @Override
                    public int compare(LogItem o1, LogItem o2) {
                        return Long.compare(o2.timestamp, o1.timestamp);
                    }
                });

                if (items.isEmpty()) {
                    TextView empty = new TextView(requireContext());
                    empty.setText("No rescue logs available for this child.");
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }
}
