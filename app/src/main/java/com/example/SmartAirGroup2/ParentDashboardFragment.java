package com.example.SmartAirGroup2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ParentDashboardFragment extends Fragment {

    private Toolbar toolbar;
    private CardView  cardAddChild;
    private LinearLayout contentContainer;

    private FirebaseDatabase db;
    private DatabaseReference childrenRef;

    private String uname = "kevin579";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_parent_dashboard, container, false);

        // Setup toolbar
        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        // Tell Android this fragment has its own menu
        setHasOptionsMenu(true);

        // Initialize cards
        contentContainer = view.findViewById(R.id.contentContainer);
        cardAddChild = view.findViewById(R.id.cardAddChild);


        db = FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/");
        childrenRef = db.getReference("categories/users/parents/" + uname + "/children");
        loadChildrenFromDatabase();


        cardAddChild.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Action")
                    .setMessage("Does your child already have an account?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        LinkChildFragment linkFrag = new LinkChildFragment();
                        Bundle args = new Bundle();
                        args.putString("parentUname", uname);
                        linkFrag.setArguments(args);

                        loadFragment(linkFrag);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        AddChildFragment addFrag = new AddChildFragment();
                        Bundle args = new Bundle();
                        args.putString("parentUname", uname);
                        addFrag.setArguments(args);

                        loadFragment(addFrag);
                        dialog.dismiss();
                    })
                    .show();
//
        });

        return view;
    }

    private void loadChildrenFromDatabase() {
        childrenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (contentContainer == null || getContext() == null) return;

                contentContainer.removeAllViews();
                int totalChildren = (int) snapshot.getChildrenCount();

                if (totalChildren == 0) {
                    // No children at all → just show add button
                    contentContainer.addView(cardAddChild);
                    Toast.makeText(getContext(), "No children linked yet", Toast.LENGTH_SHORT).show();
                    return;
                }

                // A counter to track when we finish loading all children
                final int[] loadedChildren = {0};

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    String childUname = childSnapshot.getValue(String.class);

                    if (childUname == null || childUname.isEmpty()) {
                        loadedChildren[0]++;
                        if (loadedChildren[0] == totalChildren)
                            contentContainer.addView(cardAddChild);
                        continue;
                    }

                    DatabaseReference childRef = FirebaseDatabase.getInstance()
                            .getReference("categories/users/children")
                            .child(childUname);

                    childRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot childData) {
                            if (childData.exists()) {
                                User child = childData.getValue(User.class);
                                if (child != null) {
                                    String displayName = (child.getName() != null && !child.getName().isEmpty())
                                            ? child.getName()
                                            : child.getUname();
                                    addChildCard(displayName, child.getUname());
                                }
                            }

                            loadedChildren[0]++;
                            if (loadedChildren[0] == totalChildren) {
                                // Now all children are loaded — add the Add Child card last
                                contentContainer.addView(cardAddChild);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadedChildren[0]++;
                            if (loadedChildren[0] == totalChildren)
                                contentContainer.addView(cardAddChild);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load children: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }




    @SuppressLint("ResourceType")
    private void addChildCard(String childName, String childKey) {
        // defensive: make sure fragment currently attached
        if (!isAdded() || getContext() == null) return;

        Context ctx = requireContext();

        // Create CardView programmatically
        CardView cardView = new CardView(ctx);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16)); // 16dp bottom margin
        cardView.setLayoutParams(cardParams);

        // background color — use Color int or ContextCompat
        cardView.setCardBackgroundColor(0xFFC8E6C9); // Light green
        cardView.setRadius(dpToPx(8));
        cardView.setCardElevation(0);

        cardView.setClickable(true);
        cardView.setFocusable(true);

        // Resolve selectableItemBackground attribute properly and set as foreground if available
        TypedValue outValue = new TypedValue();
        boolean resolved = ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        if (resolved) {
            Drawable selectable = ContextCompat.getDrawable(ctx, outValue.resourceId);
            // setForeground is available on FrameLayout (CardView extends FrameLayout)
            if (selectable != null) cardView.setForeground(selectable);
        }

        // Create inner LinearLayout
        LinearLayout innerLayout = new LinearLayout(ctx);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        // Use FrameLayout.LayoutParams or LinearLayout.LayoutParams when adding innerLayout to CardView?
        // We'll set innerLayout's own LayoutParams after creation:
        FrameLayout.LayoutParams innerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        innerLayout.setLayoutParams(innerParams);
        innerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        innerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Create TextView for child name
        TextView textView = new TextView(ctx);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        textView.setLayoutParams(textParams);
        textView.setText(childName);
        textView.setTextSize(18);

        // Create ImageView for delete icon
        ImageView imageView = new ImageView(ctx);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                dpToPx(24),
                dpToPx(24)
        );
        imgParams.setMarginEnd(dpToPx(12));
        imageView.setLayoutParams(imgParams);
        imageView.setImageResource(android.R.drawable.ic_delete);

        // Add views to layout
        innerLayout.addView(textView);
        innerLayout.addView(imageView);

        // Add inner layout to card view
        cardView.addView(innerLayout);

        // Set click listener for the card
        cardView.setOnClickListener(v -> {
            if (!isAdded()) return;
            Toast.makeText(ctx, "Clicked: " + childName, Toast.LENGTH_SHORT).show();
        });

        // Set click listener for delete icon
        imageView.setOnClickListener(v -> {
            if (!isAdded()) return;
            // Simple confirmation dialog before deleting
            new AlertDialog.Builder(requireContext())
                    .setTitle("Remove Child")
                    .setMessage("Are you sure you want to unlink " + childName + "?")
                    .setPositiveButton("Yes", (d, w) -> {
                        // Remove the child from parent's children list
                        if (childrenRef != null) {
                            childrenRef.child(childKey).removeValue();
                            Toast.makeText(ctx, "Child removed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // Add card to container
        // Make sure we don't add the same view twice:
        if (cardView.getParent() == null) {
            contentContainer.addView(cardView);
        }
    }


    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu, menu);

        // Tint icons white
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setTint(getResources().getColor(android.R.color.white));
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_notifications) {
            // Navigate to notifications fragment
            loadFragment(new HomeFragment());
            return true;
        } else if (id == R.id.action_settings) {
            // Handle settings click
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}