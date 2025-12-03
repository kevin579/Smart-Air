package com.example.SmartAirGroup2.ParentDashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import com.example.SmartAirGroup2.Helpers.AlertAdapter;
import com.example.SmartAirGroup2.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


/**
 * A {@link Fragment} that displays a list of alerts for a parent user.
 * It fetches health status and inventory data for all of the parent's associated children from
 * the Firebase Realtime Database. The alerts are categorized into "critical" (e.g., PEF red zone)
 * and "normal" (e.g., low medicine). The final list is sorted and displayed in a RecyclerView.
 */
public class AlertCenterFragment extends Fragment{
    /**
     * The RecyclerView used to display the list of alerts.
     */
    private RecyclerView Recycler_Alerts;
    /**
     * The adapter that binds the {@link #List_alert} to the {@link #Recycler_Alerts}.
     */
    private AlertAdapter Alert_Adapter;
    /**
     * The final, combined list of alerts that is passed to the adapter.
     */
    private List<Alert> List_alert = new ArrayList<>();
    /**
     * A temporary list to hold high-priority alerts (e.g., PEF safety alerts).
     */
    private List<Alert> criticalAlerts = new ArrayList<>(); // for pefZone alerts
    /**
     * A temporary list to hold standard-priority alerts (e.g., inventory warnings).
     */
    private List<Alert> normalAlerts   = new ArrayList<>(); // other

    /**
     * The instance of the Firebase Realtime Database.
     */
    private FirebaseDatabase db;
    /**
     * A reference to the Firebase database.
     */
    private DatabaseReference statusRef;

    /**
     * The username of the parent whose alerts are being displayed.
     */
    private String parentUname;



    /**
     * Default constructor required for fragment instantiation.
     */
    public AlertCenterFragment(){
        // Empty constructor
    }

    /**
     * Called to do initial creation of a fragment.
     * Initializes the fragment and retrieves the parent's username from either SharedPreferences
     * or fragment arguments.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     * this is the state.
     */
    @Override
    public void onCreate(@NonNull Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("APP_DATA", Context.MODE_PRIVATE);
            String storedParent = prefs.getString("parentUname", null);
            if (storedParent != null && !storedParent.trim().isEmpty()) {
                parentUname = storedParent;
            }
        }

        if (getArguments() != null) {
            String argParent = getArguments().getString("parentUname");
            if (argParent != null && !argParent.trim().isEmpty()) {
                parentUname = argParent;
            }
        }
    }



    /**
     * Called to have the fragment instantiate its user interface view.
     * Inflates the fragment's layout, sets up the Toolbar, initializes the RecyclerView and its
     * adapter, and triggers the process to load alerts from Firebase.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_alert_center, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbarAlert);

        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_ios_new_24);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
            }
        });

        //  get RecyclerView reference
        Recycler_Alerts = view.findViewById(R.id.recyclerAlerts);

        //  telling RecyclerView：using a vertical linear layout to put the list of alerts
        Recycler_Alerts.setLayoutManager(new LinearLayoutManager(getContext()));

        //  bind Adapter to RecyclerView
        Alert_Adapter = new AlertAdapter(List_alert);
        Recycler_Alerts.setAdapter(Alert_Adapter);

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        DatabaseReference childrenRef  = db.getReference("categories/users/parents")
                .child(parentUname)
                .child("children");

        loadAlertsForAllChildren(childrenRef);

        return view;
    }

    /**
     * Asynchronously fetches the list of children for the parent. For each child, it initiates
     * another fetch to get their detailed status and inventory data. It handles the complexity
     * of multiple nested asynchronous calls to ensure all data is processed before updating the UI.
     *
     * @param childrenRef A DatabaseReference pointing to the list of children for the current parent.
     */
    private void loadAlertsForAllChildren(DatabaseReference childrenRef) {
        List_alert.clear();
        criticalAlerts.clear();
        normalAlerts.clear();

        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Alert_Adapter.notifyDataSetChanged();
                    return;
                }

                int totalChildren = (int) snapshot.getChildrenCount();
                if (totalChildren == 0) {
                    Alert_Adapter.notifyDataSetChanged();
                    return;
                }

                final int[] done = {0};

                for (DataSnapshot childSnap : snapshot.getChildren()) {
                    String childUname = childSnap.getValue(String.class);
                    if (childUname == null || childUname.trim().isEmpty()) {
                        if (++done[0] == totalChildren) {
                            finishLoading();
                        }
                        continue;
                    }

                    DatabaseReference childRef = db.getReference("categories/users/children")
                            .child(childUname);

                    childRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot childData) {
                            String displayName = childData.child("name").getValue(String.class);
                            if (displayName == null || displayName.trim().isEmpty()) {
                                displayName = childUname;
                            }

                            DataSnapshot statusSnap = childData.child("status");
                            DataSnapshot inventorySnap = childData.child("inventory");
                            parseStatusForChild(displayName, statusSnap, inventorySnap);

                            if (++done[0] == totalChildren) {
                                finishLoading();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (++done[0] == totalChildren) {
                                finishLoading();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }


    /**
     * Parses the DataSnapshot for a single child to find alert-worthy conditions.
     * It checks for PEF red zone status and low or expired medicine, creating Alert objects and
     * adding them to the appropriate {@link #criticalAlerts} or {@link #normalAlerts} list.
     *
     * @param childDisplayName The display name of the child.
     * @param statusSnap       The DataSnapshot of the child's 'status' node.
     * @param inventorySnap    The DataSnapshot of the child's 'inventory' node.
     */
    private void parseStatusForChild(String childDisplayName,
                                     DataSnapshot statusSnap,
                                     DataSnapshot inventorySnap) {
        if (statusSnap == null || !statusSnap.exists()) return;

        //  PEF alert
        Integer pefZone = statusSnap.child("pefZone").getValue(Integer.class);
        if (pefZone != null && pefZone == 2) {

            criticalAlerts.add(new Alert(
                    "PEF Safety Alert",
                    childDisplayName + " is in the red PEF zone.",
                    0L
            ));
        }

        // medicine alerts
        DataSnapshot statusInvSnap = statusSnap.child("inventory");
        if (statusInvSnap == null || !statusInvSnap.exists()) {
            return;
        }
        if (inventorySnap == null || !inventorySnap.exists()) {
            // if inventory doesn't exist, just reurn
            return;
        }

        for (DataSnapshot medSnap : statusInvSnap.getChildren()) {
            String medName = medSnap.getKey();
            if (medName == null) continue;

            boolean checkLow = false;
            boolean checkExpired = false;

            for (DataSnapshot snapIndex : medSnap.getChildren()) {
                Integer code = snapIndex.getValue(Integer.class);
                if (code == null) {
                    continue;
                }
                if (code == 1) {
                    checkLow = true;
                }
                if (code == 2) {
                    checkExpired = true;
                }
            }

            long alertTime = getMedicineTime(inventorySnap, medName);

            if (checkLow) {
                normalAlerts.add(new Alert(
                        "Medicine Low",
                        childDisplayName + ":" + medName + " is running low.",
                        alertTime
                ));
            }

            if (checkExpired) {
                normalAlerts.add(new Alert(
                        "Medicine Expired",
                        childDisplayName + ":" + medName + " has expired.",
                        alertTime
                ));
            }
        }
    }


    /**
     * Retrieves the 'lastUpdated' timestamp for a specific medicine from the inventory snapshot.
     * This is used to time-sort medicine-related alerts.
     *
     * @param inventorySnap The DataSnapshot of the child's full 'inventory' node.
     * @param medName       The name of the medicine to look for.
     * @return The timestamp of the last update, or the current system time as a fallback.
     */
    private long getMedicineTime(DataSnapshot inventorySnap, String medName) {
        if (inventorySnap == null) {
            return System.currentTimeMillis();
        }

        DataSnapshot medInv = inventorySnap.child(medName);
        if (!medInv.exists()) {
            return System.currentTimeMillis();
        }

        Long timestamp = medInv.child("lastUpdated").getValue(Long.class);
        if (timestamp != null) {
            return timestamp;
        }

        return System.currentTimeMillis();
    }


    /**
     * Called after all child data has been processed. It sorts the normal alerts chronologically,
     * combines the critical and normal alerts (with critical ones first), and finally notifies the
     * adapter to refresh the RecyclerView.
     */
    private void finishLoading() {
            // Sort the alerts in chronological order from new to old
        java.util.Collections.sort(normalAlerts,
                (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        List_alert.clear();
        // put PEF（critical） first
        List_alert.addAll(criticalAlerts);
        // then put medicine alerts
        List_alert.addAll(normalAlerts);

        // Refresh
        Alert_Adapter.notifyDataSetChanged();
    }

}







