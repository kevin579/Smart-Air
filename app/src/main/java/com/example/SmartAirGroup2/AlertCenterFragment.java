package com.example.SmartAirGroup2;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



public class AlertCenterFragment extends Fragment{
    private RecyclerView Recycler_Alerts;
    private AlertAdapter Alert_Adapter;
    private List<Alert> List_alert = new ArrayList<>();
    private List<Alert> criticalAlerts = new ArrayList<>(); // for pefZone alerts
    private List<Alert> normalAlerts   = new ArrayList<>(); // other

    private FirebaseDatabase db;
    private DatabaseReference statusRef;
    private String childName = "Andy"; // testing
    private String childUname = "andy6688"; // testing
    private String parentUname = "kevin579"; // testing
    @Nullable
    private String childFilterUname = null;



    public AlertCenterFragment(){
        // Empty constructor
    }

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






