package com.example.SmartAirGroup2;

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
    private FirebaseDatabase db;
    private DatabaseReference statusRef;
    private String childName = "Andy"; // testing
    private String childUname = "andy6688"; // testing


    public AlertCenterFragment(){
        // Empty constructor
    }

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // If parameters were passed through setArguments when creating a Fragment elsewhere, they will be received here
        if (getArguments() != null) {
            String argUname = getArguments().getString("childUname");
            String argName  = getArguments().getString("childName");

            // Use the parameters if available, and keep the default values if not (andy/Andy)
            if (argUname != null && !argUname.trim().isEmpty()) {
                childUname = argUname;
            }
            if (argName != null && !argName.trim().isEmpty()) {
                childName = argName;
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
        statusRef = db.getReference("categories/users/children")
                .child(childUname)
                .child("status");

        getAlertsFromFirebase();

        return view;
    }
    private void getAlertsFromFirebase(){
        if(statusRef == null){
            return;
        }

        statusRef.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List_alert.clear();
                long now = System.currentTimeMillis();
                DataSnapshot invSnap = snapshot.child("inventory");

                Integer pefZone = snapshot.child("pefZone").getValue(int.class);
                if (pefZone != null && pefZone == 2) {
                    Alert new_alert = new Alert("PEF Safety Alert",
                            childName + " is in the red PEF zone.",
                            now);
                    List_alert.add(new_alert);
                }

                for (DataSnapshot medSnap : invSnap.getChildren()) {
                    String med_name = medSnap.getKey();
                    boolean check_Low = false;      // check if 1
                    boolean check_Expired = false;  // check if 2

                    for(DataSnapshot snap_index: medSnap.getChildren()){
                        Integer code = snap_index.getValue(Integer.class);

                        if(code == null){
                            continue;
                        }
                        if(code == 1){
                            check_Low = true;
                        }
                        if(code == 2){
                            check_Expired = true;
                        }
                    }
                    if(check_Low){
                        Alert new_alert = new Alert("Medicine Low",
                                childName+ ":" + med_name + " is running low.",
                                now);
                        List_alert.add(new_alert);
                    }
                    if(check_Expired){
                        Alert new_alert = new Alert("Medicine Expired",
                                childName+ ":" + med_name + " has expired.",
                                now);
                        List_alert.add(new_alert);
                    }

                }

                Alert_Adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }

        });
    }
}






