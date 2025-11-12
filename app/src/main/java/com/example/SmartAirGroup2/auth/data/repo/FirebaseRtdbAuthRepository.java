package com.example.SmartAirGroup2.auth.data.repo;

import com.google.firebase.database.*;
import com.google.android.gms.tasks.Tasks;
import android.util.Log;

public class FirebaseRtdbAuthRepository implements AuthRepository {
    private final DatabaseReference usersRoot =
            FirebaseDatabase.getInstance("https://smart-air-group2-default-rtdb.firebaseio.com/")
                    .getReference("categories")
                    .child("users");

    private static final String[] GROUPS = new String[] {
            "children", "parents", "providers"
    };

    private static String refactory(String s) {
        if(s==null) {
            return "";
        }
        return s.trim().toLowerCase();
    }

    private DataSnapshot findByEmailInGroup(String group, String emailNorm) throws Exception {
        DatabaseReference ref = usersRoot.child(group);

        DataSnapshot snap = Tasks.await(ref.orderByChild("email").equalTo(emailNorm).get());

        if (!snap.exists() || snap.getChildrenCount() == 0) {
            snap = Tasks.await(ref.orderByChild("children/email").equalTo(emailNorm).get());
            Log.d("RTDB", "[deep] group=" + group + ", exists=" + snap.exists() + ", count=" + snap.getChildrenCount());
        }

        if (!snap.exists() || snap.getChildrenCount() == 0) return null;
        return snap.getChildren().iterator().next();
    }

    @Override
    public ProfileCheck Check_if_User_exist(String email) throws Exception {
        String new_email = refactory(email);
        if (new_email.trim().isEmpty()) throw new Exception("invalid-email");

        for (int i = 0; i < GROUPS.length; i++) {
            String group = GROUPS[i];
            if (findByEmailInGroup(group, new_email) != null) {
                return new ProfileCheck(new_email, true);
            }
        }
        return new ProfileCheck(new_email, false);
    }

    @Override
    public boolean CheckPassword(String email, String password) throws Exception {
        String new_email = refactory(email);
        if (new_email.trim().isEmpty()) throw new Exception("Invalid Email");
        if (password == null || password.trim().isEmpty()) throw new Exception("Invalid Password");

        for (int i = 0; i < GROUPS.length; i++) {
            String group = GROUPS[i];
            DataSnapshot hit = findByEmailInGroup(group, new_email);
            if (hit != null) {
                String dbPwd = String.valueOf(hit.child("password").getValue());
                if (dbPwd == null) dbPwd = getString(hit, "children/password");
                return dbPwd != null && dbPwd.equals(password);
            }
        }
        return false;
    }


    private static String getString(DataSnapshot node, String path) {
        DataSnapshot s = node;
        for (String p : path.split("/")) s = s.child(p);
        Object v = s.getValue();
        return v == null ? null : String.valueOf(v);
    }
}
