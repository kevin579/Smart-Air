package com.example.SmartAirGroup2.ProviderDashboard;

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

import com.example.SmartAirGroup2.Helpers.MenuHelper;
import com.example.SmartAirGroup2.R;
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
 * A read-only fragment for provider users that displays rescue (inhaler) logs for a specific child.
 * <p>
 * This fragment fetches and displays logs identified as "rescue" or "inhaler" from Firebase.
 * It does not offer any controls for adding, editing, or deleting logs.
 * <p>
 * To use this fragment, create an instance using the {@link #newInstance(String)} factory method,
 * passing the unique ID of the child.
 */
public class ProviderRescueLogsFragment extends Fragment {

    /** The ID of the child whose rescue logs are being displayed. */
    private String currentChildId;
    /** The Firebase Database instance. */
    private FirebaseDatabase db;
    /** The database reference to the "children" node in Firebase. */
    private DatabaseReference childrenRef;
    /** The LinearLayout container that holds the log entry views. */
    private LinearLayout logsContainer;
    /** The toolbar for the fragment. */
    private Toolbar toolbar;

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param childId The unique identifier for the child whose logs should be displayed.
     * @return A new instance of ProviderRescueLogsFragment.
     */
    public static ProviderRescueLogsFragment newInstance(String childId) {
        ProviderRescueLogsFragment frag = new ProviderRescueLogsFragment();
        Bundle args = new Bundle();
        args.putString("childUname", childId);
        frag.setArguments(args);
        return frag;
    }

    /**
     * A private inner class to encapsulate the data for a single log entry.
     */
    private static class LogItem {
        /** The unique key of the log entry in Firebase. */
        public String key;
        /** The category of the log (e.g., "rescue"). */
        public String category;
        /** The timestamp of when the log was created. */
        public long timestamp;
        /** The parent key in Firebase (e.g., "rescue_log"). */
        public String groupKey;
        /** The specific node key for this log entry. */
        public String nodeKey;
        /** The map containing the detailed data for the log entry. */
        public Map<String, Object> data;

        /**
         * Constructs a new LogItem.
         *
         * @param key       The unique key of the log entry.
         * @param category  The category of the log.
         * @param timestamp The timestamp of the log.
         * @param groupKey  The parent key in Firebase.
         * @param nodeKey   The specific node key.
         * @param data      The detailed data map.
         */
        public LogItem(String key, String category, long timestamp, String groupKey, String nodeKey, Map<String, Object> data) {
            this.key = key;
            this.category = category;
            this.timestamp = timestamp;
            this.groupKey = groupKey;
            this.nodeKey = nodeKey;
            this.data = data;
        }
    }

    /**
     * Called when the fragment is first created.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentChildId = getArguments().getString("childUname");
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
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

    /**
     * Converts a value from density-independent pixels (dp) to pixels (px).
     *
     * @param dp The value in dp.
     * @return The equivalent value in pixels.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Safely converts an object to its string representation.
     * Returns a default value if the object is null or its string representation is "null".
     *
     * @param o   The object to convert.
     * @param def The default string to return.
     * @return The string representation or the default value.
     */
    private String safeToString(Object o, String def) {
        if (o == null) return def == null ? "" : def;
        String s = String.valueOf(o);
        return s.equals("null") ? (def == null ? "" : def) : s;
    }

    /**
     * Extracts a timestamp (as a long) from a map, handling various numeric types.
     *
     * @param map The map containing the timestamp.
     * @return The timestamp as a long, or 0 if not found or invalid.
     */
    private long getTimestampFromMap(Map<String, Object> map) {
        Object tsObj = map.get("timestamp");
        if (tsObj instanceof Long) return (Long) tsObj;
        if (tsObj instanceof Integer) return ((Integer) tsObj).longValue();
        if (tsObj instanceof Double) return ((Double) tsObj).longValue();
        return 0;
    }

    /**
     * Creates and adds a view representing a single log item to the logs container.
     *
     * @param item The LogItem to display.
     */
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
     * Fetches rescue/inhaler logs from Firebase for the current child.
     * This is a read-only operation. The method filters logs to include only those
     * related to rescue medication, sorts them by date (newest first), and displays them.
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

    /**
     * Initialize the contents of the Fragment's standard options menu.
     *
     * @param menu The options menu in which you place your items.
     * @param inflater The MenuInflater to inflate the menu.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuHelper.setupMenuWithoutAlerts(menu, inflater, requireContext());
        super.onCreateOptionsMenu(menu, inflater);
    }
}
