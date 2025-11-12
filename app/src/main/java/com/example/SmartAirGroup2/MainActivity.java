package com.example.SmartAirGroup2;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        DatabaseReference myRef = db.getReference("categories/users/parents");

//        myRef.setValue("B07 Demo!");
//        Parent p = new Parent("kevin579","kevin","likevi579@gmail.com","12345678","parent");
//        p.addChild("Randy");
//        p.addChild("Andy");
//        myRef.child(p.getUname()).setValue(p).addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
//
//            } else {
//                Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
//            }
//        });

        if (savedInstanceState == null) {
            loadFragment(new ParentDashboardFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}