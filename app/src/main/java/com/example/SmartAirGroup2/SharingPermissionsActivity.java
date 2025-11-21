package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SharingPermissionsActivity extends AppCompatActivity {
    private String childId;
    private FirebaseDatabase db;
    DatabaseReference permRef;
    private Switch rescueLogSwitch, controllerAdherenceSwitch, shareSymptomsSwitch,
            shareTriggersSwitch, sharePefSwitch, shareTriageSwitch,
            shareChartsSwitch, shareInventorySwitch;

    private Map<String, Switch> permissionSwitches = new HashMap<>();
    private boolean suppressToggleListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing_permissions);

        childId = getIntent().getStringExtra("childId");  // <-- ASSIGN IT HERE

        if (childId == null) {
            Toast.makeText(this, "Error: Child ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");

        rescueLogSwitch = findViewById(R.id.rescuelogswitch);
        controllerAdherenceSwitch = findViewById(R.id.controlleradherenceswitch);
        shareSymptomsSwitch = findViewById(R.id.sharesymptomsswitch);
        shareTriggersSwitch = findViewById(R.id.sharetriggersswitch);
        sharePefSwitch = findViewById(R.id.sharepefswitch);
        shareTriageSwitch = findViewById(R.id.sharetriageswitch);
        shareChartsSwitch = findViewById(R.id.sharechartsswitch);
        shareInventorySwitch = findViewById(R.id.shareinventoryswitch);

        permissionSwitches.put("rescueLog", rescueLogSwitch);
        permissionSwitches.put("controllerAdherence", controllerAdherenceSwitch);
        permissionSwitches.put("symptoms", shareSymptomsSwitch);
        permissionSwitches.put("triggers", shareTriggersSwitch);
        permissionSwitches.put("pef", sharePefSwitch);
        permissionSwitches.put("triage", shareTriageSwitch);
        permissionSwitches.put("charts", shareChartsSwitch);
        permissionSwitches.put("inventory", shareInventorySwitch);

        permRef = db.getReference("categories/users/children")
                .child(childId)
                .child("shareToProviderPermissions");


        permRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();

                for (String key : permissionSwitches.keySet()) {
                    boolean value = false;

                    if (snapshot.child(key).exists()) {
                        value = snapshot.child(key).getValue(Boolean.class);
                    }

                    permissionSwitches.get(key).setChecked(value);
                }
            }
        });

        for (Map.Entry<String, Switch> entry : permissionSwitches.entrySet()) {
            String key = entry.getKey();
            Switch sw = entry.getValue();

            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                permRef.child(key).setValue(isChecked);
            });
        }
    }
}